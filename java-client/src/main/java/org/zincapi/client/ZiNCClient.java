package org.zincapi.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.zincapi.Connection;
import org.zincapi.LifecycleHandler;
import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zincapi.concrete.ConcreteConnection;

public class ZiNCClient implements Zinc.Client {
	private final Zinc zinc;
	final List<ClientConnection> connections = new ArrayList<ClientConnection>();
	private final ClientBackgroundThread thr;
	private final Client<?, ?, ?> client;

	public ZiNCClient(Zinc z) {
		this.zinc = z;
		this.thr = new ClientBackgroundThread(this);
		this.thr.start();
		client = ClientFactory.getDefault().newClient();
	}
	
	public Connection createConnection(URI url, LifecycleHandler lifecycleHandler) throws IOException {
		ClientConnection ret = new ClientConnection(zinc, client, url);
		ret.addLifecycleHandler(lifecycleHandler);
		synchronized (connections) {
			connections.add(ret);
			connections.notify();
		}
		return ret;
	}

	@Override
	public Requestor requestor(Connection conn) {
		return ((ConcreteConnection) conn).newRequestor();
	}
}
