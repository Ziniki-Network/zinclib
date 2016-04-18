package org.zincapi.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.sync.SyncUtils;

public class ClientBackgroundThread extends Thread {
	protected final static Logger logger = LoggerFactory.getLogger("ZincCltThr");
	private final ZiNCClient cli;

	public ClientBackgroundThread(ZiNCClient cli) {
		super("ZincCltThr");
		this.cli = cli;
	}
	
	@Override
	public void run() {
		while (true) {
			for (ClientConnection c : cli.connections) {
				if (!c.isConnected() && !c.isConnecting())
					c.establish();
			}
			synchronized (cli) {
				SyncUtils.waitFor(cli, 5000);
			}
		}
	}
}
