package org.zincapi.peer.ssl;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.peer.ssl.client.SSLFuture;
import org.zinutils.exceptions.UtilException;

public class ZincSSLParticipant implements ZincSSLReadSelected, ZincSSLWriteSelected, ZincSSLConnectSelected {
	public static class BufferEntry {
		private final byte[] len = new byte[4];
		private final byte[] bytes;
		private int offset;
		
		public BufferEntry(String s) {
			bytes = s.getBytes();
			len[0] = (byte) ((bytes.length>>24)&0xff);
			len[1] = (byte) ((bytes.length>>16)&0xff);
			len[2] = (byte) ((bytes.length>>8)&0xff);
			len[3] = (byte) ((bytes.length)&0xff);
			offset = -4;
		}
	}

	private final static Logger logger = LoggerFactory.getLogger("ZincSSL");
	private final ZincSSLPeerManager manager;
	private final SocketChannel sc;
	public final ZincSSLConnection conn;
	private final String role;
	private final List<BufferEntry> writeBuffer = new ArrayList<BufferEntry>();
	private boolean doClose = false;
    
    protected SSLEngine engine;

    protected ByteBuffer out;       // write side of clientEngine
    protected ByteBuffer in;        // read side of clientEngine
    protected ByteBuffer trans;     // "reliable" transport client->server
	protected ByteBuffer intrans;
	SSLFuture onComplete;

    enum MyStat {
    	DONE,
    	ISCLOSED,
    	NEEDMOREINPUT,
    	WRITEBLOCKED,
    	OUTBOUND,
    	INBOUND,
    	CLOSEOUT;

		public boolean keepGoing() {
			return this == OUTBOUND || this == INBOUND || this == CLOSEOUT;
		}
    }

    public ZincSSLParticipant(ZincSSLPeerManager mgr, SocketChannel sc, boolean serverMode) throws Exception {
		this.manager = mgr;
		this.sc = sc;
		sc.configureBlocking(false);
		this.conn = new ZincSSLConnection(mgr.zinc, this, serverMode);
		this.role = serverMode?"server":"client";

        engine = mgr.sslc.createSSLEngine();
        if (serverMode) {
	        engine.setUseClientMode(false);
	        engine.setNeedClientAuth(true);
        } else {
            engine.setUseClientMode(true);
        }
        
        SSLSession session = engine.getSession();
		int appBufferMax = session.getApplicationBufferSize();
		int netBufferMax = session.getPacketBufferSize();
		
		in = ByteBuffer.allocate(appBufferMax + 50);
		out = ByteBuffer.allocate(appBufferMax + 50);
		out.flip();
		trans = ByteBuffer.allocateDirect(netBufferMax);
		intrans = ByteBuffer.allocateDirect(netBufferMax);
    }

    // This is potentially running in a different thread
    public void write(String s) throws Exception {
    	writeBuffer.add(new BufferEntry(s));
    	manager.readyToWrite(this);
    }

    // This is potentially running in a different thread
	public void closeOutbound() {
		doClose = true;
		manager.readyToWrite(this);
	}

	protected void doComms(MyStat stat) throws Exception {
		// doComms should always be called in the "select" thread
		manager.assertSelectionThread();
		while (stat.keepGoing()) {
			if (stat == MyStat.CLOSEOUT) {
				engine.closeOutbound();
				stat = MyStat.OUTBOUND;
			}
    		if (stat == MyStat.OUTBOUND)
    			stat = wrap();
    		else if (stat == MyStat.INBOUND)
    			stat = unwrap();
    	}
    	if (stat == MyStat.ISCLOSED)
    		manager.close(sc);
    	if (stat == MyStat.WRITEBLOCKED) {
    		manager.blockedFromWriting(this);
    	}
	}
    
	public MyStat wrap() throws Exception {
		HandshakeStatus ehs = engine.getHandshakeStatus();
		if (ehs == HandshakeStatus.NOT_HANDSHAKING) {
			if (!writeBuffer.isEmpty()) {
				// send data
				BufferEntry bs = writeBuffer.get(0);
				out.compact();
				if (bs.offset < 0) {
					// write length to output
					int len = -bs.offset;
					if (len >= out.remaining())
						len  = out.remaining();
					out.put(bs.len, bs.len.length+bs.offset, len);
					bs.offset += len;
				}
				if (bs.offset >= 0) {
					int len = bs.bytes.length-bs.offset;
					if (len < out.remaining()) {
						writeBuffer.remove(0);
					} else {
						len = out.remaining();
						bs.offset += len;
					}
					out.put(bs.bytes, bs.offset, len);
				}
				out.flip();
			}
		}
		SSLEngineResult res = engine.wrap(out, trans);
		HandshakeStatus stat = runDelegatedTasks(res);
		trans.flip();
		while (trans.hasRemaining()) {
			int cnt = sc.write(trans);
			logger.debug(role + " wrote " + cnt + " bytes: " + trans);
			if (cnt == 0) // write blocked
				return MyStat.WRITEBLOCKED;
		}
        trans.compact();
        if (stat == HandshakeStatus.NEED_UNWRAP)
        	return MyStat.INBOUND;
        else if (stat == HandshakeStatus.NEED_WRAP)
        	return MyStat.OUTBOUND;
        else if (res.getStatus() == Status.CLOSED)
        	return MyStat.ISCLOSED;
        else if (writeBuffer.isEmpty() && !out.hasRemaining()) {
        	if (doClose)
        		return MyStat.CLOSEOUT;
        	else
        		return MyStat.DONE;
        } else
        	return MyStat.OUTBOUND;
	}
	
	public MyStat unwrap() throws Exception {
		int cnt = sc.read(intrans);
		logger.debug(role + " read " + cnt + " bytes: " + intrans);
		if (cnt < 0)
			return MyStat.ISCLOSED;
		if (cnt == 0)
			return MyStat.NEEDMOREINPUT;
		intrans.flip();
		while (intrans.hasRemaining()) {
			SSLEngineResult res = engine.unwrap(intrans, in);
			HandshakeStatus stat = runDelegatedTasks(res);
			if (stat == HandshakeStatus.NEED_WRAP) {
				intrans.compact();
				return MyStat.OUTBOUND;
			} else if (res.getStatus() == Status.BUFFER_UNDERFLOW)
				break;
		}
		intrans.compact();
        if (writeBuffer.isEmpty() && !out.hasRemaining())
        	return MyStat.DONE;
        else
        	return MyStat.OUTBOUND;
	}

    private HandshakeStatus runDelegatedTasks(SSLEngineResult result) throws Exception {
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception("handshake shouldn't need additional tasks");
            }
            return hsStatus;
        }
        return result.getHandshakeStatus();
    }

    private int lidx = 0;
    private byte[] length = new byte[4];
    private int widx = 0;
    private byte[] waiting = null;
	@Override
	public void canRead(SelectionKey next) throws Exception {
		doComms(MyStat.INBOUND);
		while (in.position() > 0) {
			logger.debug("received " + in);
			in.flip();
			logger.debug("flipped to " + in);
			if (waiting == null) {
				int want = length.length-lidx;
				int avail = in.remaining();
				if (want > avail) { // the full length is not here yet
					// read what we can
					in.get(length, lidx, avail);
					lidx += avail;
				} else {
					in.get(length, lidx, want);
					int alen = ((length[0]&0xff)<<24)|((length[1]&0xff)<<16)|((length[2]&0xff)<<8)|(length[3]&0xff);
					waiting = new byte[alen];
					widx = 0;
					lidx = 0; // for next time around
				}
			}
			if (waiting != null) {
				int want = waiting.length - widx;
				int avail = in.remaining();
				if (want > avail) { // the whole message is not here yet
					in.get(waiting, widx, avail);
					widx += avail;
				} else {
					// read what we need, and loop for the rest
					in.get(waiting, widx, want);
					String data = new String(waiting, 0, waiting.length);
					logger.debug("Read " + data);
					manager.submit(conn, data);
					in.compact();
					waiting = null;
					widx = 0;
					lidx = 0;
				}
			}
		}
	}

	@Override
	public void canWrite(SelectionKey next) throws Exception {
		doComms(MyStat.OUTBOUND);
	}

	public SelectionKey keyFor(Selector sel) {
		return sc.keyFor(sel); 
	}
	
	@Override
	public void canConnect(SelectionKey next) throws Exception {
		// This should only be called from the selection loop
		manager.assertSelectionThread();
		try {
			SocketChannel c = (SocketChannel) next.channel();
			if (!c.finishConnect())
				throw new UtilException("We were told the channel was ready to connect, but it wasn't");
			int ops = next.interestOps();
			ops &= ~SelectionKey.OP_CONNECT;
			ops |= SelectionKey.OP_READ;
			next.interestOps(ops);
			onComplete.completed(this);
			onComplete = null;
		} catch (Exception ex) {
			onComplete.failed(ex);
		}
	}
}
