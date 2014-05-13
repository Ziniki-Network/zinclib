package org.zincapi;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.concrete.ConcreteHandleRequest;
import org.zincapi.concrete.ConcreteMakeRequest;
import org.zincapi.concrete.ConcreteMulticastResponse;

public class Zinc {
	private final Client client; 
	private final Server server; 
	private final Map<String, Connection> conns = new TreeMap<String, Connection>();
	private ResourceHandler defaultHandler;
	private final Map<String, ResourceHandler> handlers = new TreeMap<String, ResourceHandler>();
	private final Map<String, ConcreteMulticastResponse> multicasts = new TreeMap<String, ConcreteMulticastResponse>();
	private final Set<ConnectionHandler> newConnectionListeners = new HashSet<ConnectionHandler>();
	private String idType = "client";
	private String idAddress;

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
	
	public void setIdentity(String type, String address) {
		if (!type.equals("client") && !type.equals("server"))
			throw new ZincException("Type " + type + " is not valid");
		idType = type;
		idAddress = address;
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
			mr.setOption("type", idType);
			mr.setOption("address", idAddress);
			try {
				mr.send();
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
		}
		return client.requestor(conn);
	}
	
	public void handleResource(String resource, ResourceHandler handler) {
		if (resource == null)
			defaultHandler = handler;
		// TODO: this should be broken up into a REST-like tree structure
		handlers.put(resource, handler);
	}

	public MulticastResponse getMulticastResponse(String name) {
		synchronized (multicasts ) {
			if (!multicasts.containsKey(name)) {
				multicasts.put(name, new ConcreteMulticastResponse());
			}
			return multicasts.get(name);
		}
	}

	public void addConnectionHandler(ConnectionHandler handler) {
		newConnectionListeners.add(handler);
	}
	
	public Set<ConnectionHandler> getConnectionHandlers() {
		return newConnectionListeners;
	}

	public IncomingConnection addConnection(OutgoingConnection c) {
		if (server == null)
			throw new ZincNoServerException();
		return server.addConnection(c);
	}
	
	public Client getClient() {
		return client;
	}
	
	public ResourceHandler getHandler(ConcreteHandleRequest hr, String resource) {
		if (resource != null) { 
			// TODO: This should really support REST-like endpoints
			// with {id} style syntax and nesting
			
			if (handlers.containsKey(resource)) {
				hr.setResource(resource);
				return handlers.get(resource);
			}
		}
		if (defaultHandler != null) {
			hr.setResource(resource);
			return defaultHandler;
		}
		throw new ZincNoResourceHandlerException(resource);
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
