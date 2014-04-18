package org.zincapi.server;

import java.util.Map;
import java.util.TreeMap;

import org.zincapi.IncomingConnection;
import org.zincapi.OutgoingConnection;
import org.zincapi.ResourceHandler;
import org.zincapi.Zinc;
import org.zincapi.ZincNoResourceHandlerException;
import org.zincapi.concrete.ConcreteHandleRequest;

public class ZiNCServer implements Zinc.Server {
	private final Zinc zinc;
	private ResourceHandler defaultHandler;
	private final Map<String, ResourceHandler> handlers = new TreeMap<String, ResourceHandler>();

	public ZiNCServer(Zinc z) {
		zinc = z;
	}
	
	@Override
	public void handleResource(String resource, ResourceHandler handler) {
		if (resource == null)
			defaultHandler = handler;
		// TODO: this should be broken up into a REST-like tree structure
		handlers.put(resource, handler);
	}

	@Override
	public IncomingConnection addConnection(OutgoingConnection c) {
		return new IncomingServerConnection(zinc, this, c);
	}
	
	public ResourceHandler getHandler(ConcreteHandleRequest hr, String resource) {
		if (resource != null) { 
			// TODO: This should really support REST-like endpoints
			// with {id} style syntax and nesting
			
			// More urgent todo: this needs to support prefixes and hand the rest back to the request
			if (handlers.containsKey(resource)) {
				hr.setResource(""); // TODO: should be whatever is left over once the main body is matched
				return handlers.get(resource);
			}
		}
		if (defaultHandler != null) {
			hr.setResource(resource);
			return defaultHandler;
		}
		throw new ZincNoResourceHandlerException(resource);
	}
}
