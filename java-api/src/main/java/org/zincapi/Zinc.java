package org.zincapi;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.concrete.ConcreteHandleRequest;
import org.zincapi.concrete.ConcreteMakeRequest;
import org.zincapi.concrete.ConcreteMulticastResponse;
import org.zincapi.concrete.SegmentHandler;

public class Zinc {
	private final Client client; 
	private final Server server; 
	private final Map<String, Connection> conns = new TreeMap<String, Connection>();
	private ResourceHandler defaultHandler;
	private final Map<String, SegmentHandler> handlers = new TreeMap<String, SegmentHandler>();
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
		if (resource == null) {
			defaultHandler = handler;
			return;
		}
		String[] segments = resource.split("/");
		if (segments == null || segments.length == 0) {
			defaultHandler = handler;
			return;
		}
		SegmentHandler h = null;
		Map<String, SegmentHandler> map = handlers;
		for (String seg : segments) {
			String any = null;
			if (seg.charAt(0) == '{' && seg.charAt(seg.length()-1) == '}') {
				any = seg.substring(1, seg.length()-1);
				seg = "{}";
			}
			if (!map.containsKey(seg))
				map.put(seg, new SegmentHandler(any));
			h = map.get(seg);
			map = h.handlers;
		}
		h.setHandler(resource, handler);
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
		hr.setResource(resource);
		if (resource != null) { 
			String[] segments = resource.split("/");
			List<MatchState> curr = new ArrayList<MatchState>();
			curr.add(new MatchState(handlers));
			for (String seg : segments) {
				List<MatchState> toAdd = new ArrayList<MatchState>();
				for (MatchState k : curr) {
					SegmentHandler h1 = k.handlers.get(seg);
					SegmentHandler h2 = k.handlers.get("{}");
					if (h1 != null)
						toAdd.add(k.matched(null, h1));
					if (h2 != null)
						toAdd.add(k.matched(seg, h2));
				}
				curr = toAdd;
			}
			if (curr.size() > 1)
				throw new ZincMultipleMatchException(resource);
			if (curr.size() == 1) {
				MatchState sh = curr.get(0);
				sh.bindParametersTo(hr);
				return sh.handler();
			}
		}
		if (defaultHandler != null) {
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
