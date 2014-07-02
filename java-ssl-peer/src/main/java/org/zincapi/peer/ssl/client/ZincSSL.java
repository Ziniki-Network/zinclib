package org.zincapi.peer.ssl.client;

import java.net.SocketAddress;
import java.util.concurrent.Future;

import org.zincapi.Zinc;
import org.zincapi.peer.ssl.ZincSSLParticipant;
import org.zincapi.peer.ssl.ZincSSLPeerManager;
import org.zinutils.sync.Promise;

public class ZincSSL extends Zinc {
	private final ZincSSLPeerManager mgr;

	public ZincSSL(ZincSSLPeerManager mgr) throws Exception {
		super("org.zincapi.peer.ssl.client.ZiNCClient", "org.zincapi.server.ZiNCServer");
		this.mgr = mgr;
	}

	// This can be called from an arbitrary thread
	// In fact, it cannot be called from the selectionThread
	public Future<ZincSSLParticipant> createClient(final SocketAddress addr) {
		final Promise<ZincSSLParticipant> ret = new Promise<ZincSSLParticipant>();
		mgr.assertNotSelectionThread();
		mgr.runTask(new Runnable() {
			@Override
			public void run() {
				try {
					mgr.connectTo(addr, ret);
				} catch (Exception ex) {
					ret.failed(ex);
				}
			}
		});
//        ZincSSLParticipant cli = new ZincSSLParticipant(mgr, c, false);
//		c.register(mgr.sel, SelectionKey.OP_READ, cli);
		// TODO Auto-generated method stub
		return ret;
	}
}
