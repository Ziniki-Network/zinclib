package org.zincapi.peer.ssl;

/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * -Redistribution of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the
 *  distribution.
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN
 * OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
 * FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

/**
 * A SSLEngine usage example which simplifies the presentation
 * by removing the I/O and multi-threading concerns.
 *
 * The demo creates two SSLEngines, simulating a client and server.
 * The "transport" layer consists two ByteBuffers:  think of them
 * as directly connected pipes.
 *
 * Note, this is a *very* simple example: real code will be much more
 * involved.  For example, different threading and I/O models could be
 * used, transport mechanisms could close unexpectedly, and so on.
 *
 * When this application runs, notice that several messages
 * (wrap/unwrap) pass before any application data is consumed or
 * produced.  (For more information, please see the SSL/TLS
 * specifications.)  There may several steps for a successful handshake,
 * so it's typical to see the following series of operations:
 *
 *      client          server          message
 *      ======          ======          =======
 *      wrap()          ...             ClientHello
 *      ...             unwrap()        ClientHello
 *      ...             wrap()          ServerHello/Certificate
 *      unwrap()        ...             ServerHello/Certificate
 *      wrap()          ...             ClientKeyExchange
 *      wrap()          ...             ChangeCipherSpec
 *      wrap()          ...             Finished
 *      ...             unwrap()        ClientKeyExchange
 *      ...             unwrap()        ChangeCipherSpec
 *      ...             unwrap()        Finished
 *      ...             wrap()          ChangeCipherSpec
 *      ...             wrap()          Finished
 *      unwrap()        ...             ChangeCipherSpec
 *      unwrap()        ...             Finished
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zincapi.ZincException;
import org.zincapi.peer.ssl.ZincSSLParticipant.MyStat;
import org.zincapi.peer.ssl.client.ZincSSL;
import org.zinutils.sync.Promise;
import org.zinutils.sync.SyncUtils;

public class ZincSSLPeerManager implements Runnable, ZincSSLAcceptSelected {
	final ZincSSL zinc;
	private final Selector sel;
	private final Set<ZincSSLParticipant> readyToWrite = new HashSet<ZincSSLParticipant>();
	private final Set<ZincSSLParticipant> blockedFromWriting = new HashSet<ZincSSLParticipant>();
	private Thread selectionThread;
	// TODO: what we actually want is a thread pool which is strictly ordered for a given destination,
	// but which can have multiple threads for different destinations.
	private final ExecutorService pool = Executors.newFixedThreadPool(1);
	private final List<Runnable> tasks = new ArrayList<Runnable>();
	final SSLContext sslc;
	private int port;

	// Just for testing ...
    public static void main(String args[]) throws Exception {
        ZincSSLPeerManager mgr = new ZincSSLPeerManager(4093);
        Thread thread = new Thread(mgr);
        thread.setDaemon(true);
		thread.start();
        
        Requestor newRequestor = mgr.zinc.newRequestor(new URI("http://localhost:4093/ziniki/replicator"));
        newRequestor.subscribe("/fred", null).send();
        SyncUtils.sleep(1000);
        mgr.pool.shutdown();
    }
    
    public ZincSSLPeerManager(int port) throws Exception {
    	this(Selector.open(), port);
    }

    public ZincSSLPeerManager(Selector sel, int port) throws Exception {
		String keyStoreFile = "/Users/gareth/Ziniki/Code/Ziniki/MockKernel/src/main/resources/ziniki.ks";
        String trustStoreFile = "/Users/gareth/Ziniki/Code/Ziniki/MockKernel/src/main/resources/ziniki.ks";
        String passwd = "password";

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");
        char[] passphrase = passwd.toCharArray();
        ks.load(new FileInputStream(keyStoreFile), passphrase);
        ts.load(new FileInputStream(trustStoreFile), passphrase);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);
        sslc = SSLContext.getInstance("TLS");
        sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        this.zinc = new ZincSSL(this);
		this.sel = sel;
		ServerSocketChannel listen = ServerSocketChannel.open();
        listen.bind(new InetSocketAddress(port));
        this.port = ((InetSocketAddress)listen.getLocalAddress()).getPort();
        listen.configureBlocking(false);
        listen.register(sel, SelectionKey.OP_ACCEPT, this);
    }

	public Zinc getZinc() {
		return zinc;
	}

	public int getListenPort() {
    	return port;
    }

	@Override
    public void run() {
		selectionThread = Thread.currentThread();
    	while (true) {
    		try {
    			sel.select();
				Iterator<SelectionKey> it = sel.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey next = it.next();
					Object selected = next.attachment();
					if (next.isValid() && next.isReadable())
						((ZincSSLReadSelected) selected).canRead(next);
					if (next.isValid() && next.isAcceptable())
						((ZincSSLAcceptSelected) selected).canAccept(next);
					if (next.isValid() && next.isConnectable())
						((ZincSSLConnectSelected) selected).canConnect(next);
					if (next.isValid() && next.isWritable()) {
						next.interestOps(next.interestOps() & ~SelectionKey.OP_WRITE);
						((ZincSSLWriteSelected) selected).canWrite(next);
					}
					it.remove();
				}
				synchronized (readyToWrite) {
					Iterator<ZincSSLParticipant> it2 = readyToWrite.iterator();
					while (it2.hasNext()) {
						it2.next().doComms(MyStat.OUTBOUND);
						it2.remove();
					}
				}
				synchronized (tasks) {
					Iterator<Runnable> task = tasks.iterator();
					while (task.hasNext()) {
						task.next().run();
						task.remove();
					}
				}
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}
    	}
    }

	// This is called by a specific participant when it finds it has more data to write
	// This can be called from any thread
	public void readyToWrite(ZincSSLParticipant p) {
		synchronized (readyToWrite) {
			readyToWrite.add(p);
		}
		sel.wakeup();
	}

	public void blockedFromWriting(ZincSSLParticipant p) {
		// This should only be called from doComms, when we are already in the selection thread
		assertSelectionThread();
		synchronized (blockedFromWriting) {
			blockedFromWriting.add(p);
		}
		SelectionKey sk = p.keyFor(sel);
		sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
	}

	@Override
	public void canAccept(SelectionKey sk) throws Exception {
		ServerSocketChannel lsnr = (ServerSocketChannel) sk.channel();
		SocketChannel sc = lsnr.accept();
		sc.configureBlocking(false);
		ZincSSLParticipant server = new ZincSSLParticipant(this, sc, true);
		sc.register(sel, SelectionKey.OP_READ, server);
	}

	public void assertSelectionThread() {
		if (Thread.currentThread() != selectionThread)
			throw new ZincException("Can only operate in the selection thread");
	}
	
	public void assertNotSelectionThread() {
		if (Thread.currentThread() == selectionThread)
			throw new ZincException("Cannot make this call in the selection thread");
	}

	public void submit(ZincSSLConnection conn, String data) {
		pool.submit(new ZincSSLProcessData(conn, data));
	}

	public void close(SocketChannel sc) throws IOException {
		SelectionKey sk = sc.keyFor(sel);
		if (sk != null)
			sk.cancel();
		sc.close();
	}

	public void runTask(Runnable runnable) {
		synchronized (tasks) {
			tasks.add(runnable);
		}
		sel.wakeup();
	}

	public void connectTo(SocketAddress addr, Promise<ZincSSLParticipant> promise) throws Exception {
        SocketChannel c = SocketChannel.open();
        c.configureBlocking(false);
        ZincSSLParticipant cli = new ZincSSLParticipant(this, c, false);
        boolean connected = c.connect(addr);
        if (connected) {
    		c.register(sel, SelectionKey.OP_READ, cli);
    		promise.completed(cli);
        } else {
        	cli.onComplete = promise;
        	c.register(sel, SelectionKey.OP_CONNECT, cli);
        }
	}
}