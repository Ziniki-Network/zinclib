package org.zincapi.concrete;

import java.util.Map;
import java.util.TreeMap;

import org.zincapi.Channel;
import org.zincapi.HandleRequest;
import org.zincapi.Requestor;
import org.zincapi.ZincNoResourceParameterException;
import org.zincapi.jsonapi.Payload;

public class ConcreteHandleRequest implements HandleRequest {
	private final ConcreteConnection conn;
	private final Channel channel;
	private final String method;
	private String resource;
	private final Map<String, String> parameters = new TreeMap<String, String>();
	private final Map<String, Object> options = new TreeMap<String, Object>();
	private Payload payload;

	public ConcreteHandleRequest(ConcreteConnection conn, int channel, String method) {
		this.conn = conn;
		this.channel = channel <= 0 ? null : conn.getChannel(channel);
		this.method = method;
	}
	
	public void setResource(String resource) {
		this.resource = resource;
	}

	public void setResourceParameter(String name, String segment) {
		this.parameters.put(name, segment);
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
	public String getResourceParameter(String param) {
		if (parameters.containsKey(param))
			return parameters.get(param);
		throw new ZincNoResourceParameterException(param, resource);
	}

	@Override
	public Requestor obtainRequestor() {
		return conn.newRequestor();
	}

	
	@Override
	public Channel getChannel() {
		return channel;
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
