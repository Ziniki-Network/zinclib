package org.zincapi.concrete;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.Connection;
import org.zincapi.MakeRequest;
import org.zincapi.ResponseHandler;
import org.zincapi.ZincCannotSetPayloadException;

public class ConcreteMakeRequest implements MakeRequest {
	private final Connection conn;
	final ResponseHandler handler;
	private final String method;
	private final Map<String,Object> opts = new HashMap<String,Object>();
	private String resource;
	private Integer subscriptionHandle;
	private JSONObject payload;

	public ConcreteMakeRequest(Connection conn, String method) {
		this(conn, method, null);
	}

	public ConcreteMakeRequest(Connection conn, String method, ResponseHandler handler) {
		this.conn = conn;
		this.method = method;
		this.handler = handler;
	}

	@Override
	public ResponseHandler getHandler() {
		return handler;
	}

	public void requireSubcription() {
		this.subscriptionHandle = conn.nextHandle(this);
	}
	
	public void setResource(String resource) {
		this.resource = resource;
	}

	@Override
	public void setOption(String opt, Object val) {
		opts.put(opt, val);
	}
	
	@Override
	public void setPayload(JSONObject payload) {
		if (this.payload != null)
			throw new ZincCannotSetPayloadException();
		this.payload = payload;
	}

	@Override
	public void send() throws JSONException {
		conn.send(asJSON());
	}

	private JSONObject asJSON() throws JSONException {
		JSONObject req = new JSONObject();
		req.put("method", method);
		if (resource != null)
			req.put("resource", resource);
		if (!opts.isEmpty()) {
			JSONObject options = new JSONObject();
			req.put("options", options);
			for (Entry<String, Object> e : opts.entrySet())
				options.put(e.getKey(), e.getValue());
		}
		JSONObject obj = new JSONObject();
		if (subscriptionHandle != null)
			obj.put("subscription", subscriptionHandle);
		obj.put("request", req);
		if (payload != null)
			obj.put("payload", payload);
		return obj;
	}
}
