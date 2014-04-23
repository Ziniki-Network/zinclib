package org.zincapi.client;

import java.io.IOException;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Request.METHOD;
import org.atmosphere.wasync.Request.TRANSPORT;
import org.atmosphere.wasync.RequestBuilder;
import org.zincapi.Connection;
import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zincapi.concrete.ConcreteRequestor;

public class ZiNCClient implements Zinc.Client {

	public ZiNCClient(Zinc z) {
	}
	
	@SuppressWarnings("rawtypes")
	public Connection createConnection(String url) throws IOException {
		Client client = ClientFactory.getDefault().newClient();
		RequestBuilder request = client.newRequestBuilder()
				.method(METHOD.GET)
				.uri(url)
				.transport(TRANSPORT.WEBSOCKET)
				.transport(TRANSPORT.LONG_POLLING);

		return new ClientConnection(client.create(), request.build());
	}

	@Override
	public Requestor requestor(Connection conn) {
		return new ConcreteRequestor(conn);
	}
}
