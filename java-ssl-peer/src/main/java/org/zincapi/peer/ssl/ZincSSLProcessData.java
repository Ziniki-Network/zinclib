package org.zincapi.peer.ssl;

public class ZincSSLProcessData implements Runnable {
	private final ZincSSLConnection conn;
	private final String data;

	public ZincSSLProcessData(ZincSSLConnection conn, String data) {
		this.conn = conn;
		this.data = data;
	}

	@Override
	public void run() {
		try {
			conn.ic.receiveTextMessage(data);
		} catch (Exception ex) {
			// TODO: should probably send back to other end
			ex.printStackTrace();
		}
	}

}
