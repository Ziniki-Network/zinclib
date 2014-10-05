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
	private final int seq;
	private boolean sentSomething = false;
	private boolean unsubscribed;
	private final Set<ConcreteMulticastResponse> multicasters = new HashSet<ConcreteMulticastResponse>();

	public ConcreteResponse(ConcreteConnection oc, int seq) {
		this.oc = oc;
		this.seq = seq;
	}

	@Override
	public Connection getConnection() {
		return oc;
	}

	@Override
	public void send(Payload payload) throws JSONException {
		if (unsubscribed)
			return;
		
		try {
			JSONObject msg = new JSONObject();
			msg.put("subscription", seq);
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
