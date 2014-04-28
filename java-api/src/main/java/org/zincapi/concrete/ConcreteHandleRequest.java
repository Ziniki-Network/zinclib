package org.zincapi.concrete;

import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONObject;
import org.zincapi.HandleRequest;
import org.zincapi.Requestor;

public class ConcreteHandleRequest implements HandleRequest {
	private final ConcreteConnection conn;
	private final String method;
	private String resource;
	private final Map<String, Object> options = new TreeMap<String, Object>();
	private JSONObject payload;

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
	
	public void setPayload(JSONObject payload) {
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
	public Requestor obtainRequestor() {
		return conn.newRequestor();
	}

	@Override
	public String getConnectionURI() {
		return conn.getURI();
	}

	@Override
	public JSONObject getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "HandleRequest[" + method + "[" + resource + "]]";
	}
}
