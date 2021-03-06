package org.zincapi.concrete;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.Connection;
import org.zincapi.Response;
import org.zincapi.ZincBrokenConnectionException;
import org.zincapi.jsonapi.Payload;

public class ConcreteResponse implements Response {
	private final ConcreteConnection oc;
	private final boolean subscribe;
	private final int seq;
	private boolean sentSomething = false;
	private boolean unsubscribed;
	private final Set<ConcreteMulticastResponse> multicasters = new HashSet<ConcreteMulticastResponse>();

	public ConcreteResponse(ConcreteConnection oc, boolean subscribe, int seq) {
		this.oc = oc;
		this.subscribe = subscribe;
		this.seq = seq;
	}

	@Override
	public Connection getConnection() {
		return oc;
	}

	@Override
	public void send(Payload payload) throws JSONException {
		if (subscribe)
			send("replace", payload);
		else
			send(null, payload);
	}
	
	@Override
	public void send(String action, Payload payload) throws JSONException {
		if (unsubscribed)
			return;

		if (payload == null)
			action = "empty";
		
		// TODO: should we "validate" the action?  Or is anything OK?
		try {
			JSONObject msg = new JSONObject();
			if (subscribe)
				msg.put("subscription", seq);
			else
				msg.put("requestid", seq);
			if (action != null)
				msg.put("action", action);
			if (payload != null)
				msg.put("payload", payload.asJSONObject());
			oc.send(msg);
			sentSomething = true;
		} catch (ZincBrokenConnectionException ex) {
			unsubscribed();
		}
	}

	public void sendStatus(String idField, String status, Object error) throws JSONException {
		try {
			JSONObject msg = new JSONObject();
			msg.put(idField, seq);
			if (error != null)
				msg.put("error", error.toString());
			if (status != null)
				msg.put("status", status);
			oc.send(msg);
			sentSomething = true;
		} catch (ZincBrokenConnectionException ex) {
			unsubscribed();
		}
	}
	
	public boolean sent() {
		return sentSomething;
	}

	@Override
	public void unsubscribed() {
		unsubscribed = true;
		for (ConcreteMulticastResponse r : multicasters)
			r.removeResponse(this);
	}
}
