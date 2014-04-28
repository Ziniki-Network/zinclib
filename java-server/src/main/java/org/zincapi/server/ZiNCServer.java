package org.zincapi.server;

import org.zincapi.IncomingConnection;
import org.zincapi.OutgoingConnection;
import org.zincapi.ResourceHandler;
import org.zincapi.Zinc;

public class ZiNCServer implements Zinc.Server {
	private final Zinc zinc;

	public ZiNCServer(Zinc z) {
		zinc = z;
	}
	
	@Override
	public void handleResource(String resource, ResourceHandler handler) {
	}

	@Override
	public IncomingConnection addConnection(OutgoingConnection c) {
		return new IncomingServerConnection(zinc, c);
	}
	
}
