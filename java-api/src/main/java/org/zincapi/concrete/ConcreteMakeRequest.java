package org.zincapi.concrete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.MakeRequest;
import org.zincapi.ResponseHandler;
import org.zincapi.ZincCannotSetPayloadException;
import org.zincapi.ZincException;
import org.zincapi.ZincNoSubscriptionException;
import org.zincapi.jsonapi.Payload;

public class ConcreteMakeRequest implements MakeRequest {
	private final static Logger logger = LoggerFactory.getLogger("ConcreteMakeRequest");
	private final ConcreteConnection conn;
	private final int channel;
	final ResponseHandler handler;
	private final String method;
	private final Map<String,Object> opts = new HashMap<String,Object>();
	private String resource;
	private Integer subscriptionHandle;
	private JSONObject payload;

	public ConcreteMakeRequest(ConcreteConnection conn, int channel, String method) {
		this(conn, channel, method, null);
	}

	public ConcreteMakeRequest(ConcreteConnection conn, int channel, String method, ResponseHandler handler) {
		this.conn = conn;
		this.channel = channel;
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
	public MakeRequest setOption(String opt, Object val) {
		opts.put(opt, val);
		return this;
	}
	
	@Override
	public MakeRequest setPayload(Payload payload) {
		try {
			if (this.payload != null)
				throw new ZincCannotSetPayloadException();
			this.payload = payload.asJSONObject();
			return this;
		} catch (Exception ex) {
			throw ZincException.wrap(ex);
		}
	}

	@Override
	public MakeRequest setPayload(String payload) {
		try {
			if (this.payload != null)
				throw new ZincCannotSetPayloadException();
			this.payload = new JSONObject(payload);
			return this;
		} catch (Exception ex) {
			throw ZincException.wrap(ex);
		}
	}

	@Override
	public void send() {
		try {
			JSONObject msg = asJSON();
			logger.info("Sending message " + msg);
			conn.send(msg);
		} catch (Exception ex) {
			throw ZincException.wrap(ex);
		}
	}
	
	@Override
	public void unsubscribe() {
		try {
			if (subscriptionHandle == null)
				throw new ZincNoSubscriptionException();
			JSONObject req = new JSONObject();
			req.put("method", "unsubscribe");
			JSONObject usmsg = new JSONObject();
			usmsg.put("subscription", subscriptionHandle);
			usmsg.put("request", req);
			conn.send(usmsg);
		} catch (Exception ex) {
			throw ZincException.wrap(ex);
		}
	}

	private JSONObject asJSON() throws JSONException {
		JSONObject req = new JSONObject();
		req.put("method", method);
		if (resource != null)
			req.put("resource", resource);
		if (channel > 0)
			req.put("channel", channel);
		if (!opts.isEmpty()) {
			JSONObject options = new JSONObject();
			req.put("options", options);
			for (Entry<String, Object> e : opts.entrySet()) {
				Object v = e.getValue();
				if (v instanceof List) {
					@SuppressWarnings("unchecked")
					List<String> sv = (List<String>)v;
					JSONArray a = new JSONArray();
					for (String s : sv)
						a.put(s);
					v = a;
				}
				options.put(e.getKey(), v);
			}
		}
		JSONObject obj = new JSONObject();
		int h;
		if (subscriptionHandle != null) {
			h = subscriptionHandle;
			obj.put("subscription", subscriptionHandle);
		}
		else {
			h = conn.nextHandle(this);
			obj.put("requestId", h);
		}
		conn.pend(h, null);
		obj.put("request", req);
		if (payload != null)
			obj.put("payload", payload);
		return obj;
	}
}
