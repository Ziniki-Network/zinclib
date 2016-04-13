package org.zincapi.client;

import org.zinutils.sync.SyncUtils;

public class ClientBackgroundThread extends Thread{
	private final ZiNCClient cli;

	public ClientBackgroundThread(ZiNCClient cli) {
		super("ZincCltThr");
		this.cli = cli;
	}
	
	@Override
	public void run() {
		while (true) {
			for (ClientConnection c : cli.connections) {
				if (!c.isConnected())
					c.establish();
			}
			synchronized (cli) {
				SyncUtils.waitFor(cli, 200);
				System.out.println("cli.connections = " + cli.connections.size());
			}
		}
	}
}
