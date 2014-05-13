package org.zincapi.concrete;

import java.util.Map;
import java.util.TreeMap;

import org.zincapi.HandleRequest;
import org.zincapi.Requestor;
import org.zincapi.jsonapi.Payload;

public class ConcreteHandleRequest implements HandleRequest {
	private final ConcreteConnection conn;
	private final String method;
	private String resource;
	private final Map<String, Object> options = new TreeMap<String, Object>();
	private Payload payload;

	public ConcreteHandleRequest(ConcreteConnection conn, String method) {
		this.conn = conn;
		this.method = method;
	}
	
	public void setResource(String resource) {
		this.resource = resource;
	}

	public void setOption(String k, Object object) {
		options.put(k, object);
	}
	
	public void setPayload(Payload payload) {
		this.payload = payload;
	}
	
	@Override
	public boolean isSubscribe() {
		return method.equals("subscribe");
	}

	@Override
	public boolean isCreate() {
		return method.equals("create");
	}

	@Override
	public boolean isInvoke() {
		return method.equals("invoke");
	}
	
	@Override
	public String getResource() {
		return resource;
	}

	@Override
	public Requestor obtainRequestor() {
		return conn.newRequestor();
	}

	@Override
	public String getConnectionURI() {
		return conn.getURI();
	}

	@Override
	public Map<String, Object> options() {
		return options;
	}

	@Override
	public Payload getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "HandleRequest[" + method + "[" + resource + "]]";
	}
}
