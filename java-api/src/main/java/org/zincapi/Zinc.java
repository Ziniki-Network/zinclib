package org.zincapi;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.concrete.ConcreteMakeRequest;
import org.zincapi.concrete.ConcreteMulticastResponse;

public class Zinc {
	private final Client client; 
	private final Server server; 
	private final Map<String, Connection> conns = new TreeMap<String, Connection>();
	private final Map<String, ConcreteMulticastResponse> multicasts = new TreeMap<String, ConcreteMulticastResponse>();

	public Zinc() {
		{
			Client c = null;
			try {
				Class<?> tmp = Class.forName("org.zincapi.client.ZiNCClient");
				Constructor<?> ctor = tmp.getConstructor(Zinc.class);
				c = (Client) ctor.newInstance(this);
			} catch (Throwable ex) {
			}
			client = c;
		}
		{
			Server s = null;
			try {
				Class<?> tmp = Class.forName("org.zincapi.server.ZiNCServer");
				Constructor<?> ctor = tmp.getConstructor(Zinc.class);
				s = (Server) ctor.newInstance(this);
			} catch (Throwable ex) {
			}
			server = s;
		}
	}
	
	public Requestor newRequestor(URI uri) throws IOException {
		if (client == null)
			throw new ZincNoClientException();
		String url = uri.toString();
		Connection conn;
		if (conns.containsKey(url))
			conn = conns.get(url);
		else {
			conn = client.createConnection(url);
			conns.put(url, conn);
			ConcreteMakeRequest mr = new ConcreteMakeRequest(conn, "establish");
			mr.setOption("type", "client");
			mr.setOption("address", null);
			try {
				mr.send();
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
		}
		return client.requestor(conn);
	}
	
	public void handleResource(String resource, ResourceHandler handler) {
		if (server == null)
			throw new ZincNoServerException();
		server.handleResource(resource, handler);
	}

	public MulticastResponse getMulticastResponse(String name) {
		synchronized (multicasts ) {
			if (!multicasts.containsKey(name)) {
				multicasts.put(name, new ConcreteMulticastResponse());
			}
			return multicasts.get(name);
		}
	}

	public IncomingConnection addConnection(OutgoingConnection c) {
		if (server == null)
			throw new ZincNoServerException();
		return server.addConnection(c);
	}
	
	public void close() {
		for (Connection c : conns.values())
			c.close();
	}

	public interface Client {
		Connection createConnection(String url) throws IOException;
		Requestor requestor(Connection conn);
	}

	public interface Server {
		IncomingConnection addConnection(OutgoingConnection c);

		void handleResource(String resource, ResourceHandler handler);
	}
}
