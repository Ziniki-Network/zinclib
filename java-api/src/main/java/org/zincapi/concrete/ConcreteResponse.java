package org.zincapi.concrete;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.Connection;
import org.zincapi.Response;
import org.zincapi.ZincBrokenConnectionException;

public class ConcreteResponse implements Response {
	private final Connection oc;
	private final int seq;
	private boolean unsubscribed;
	private final Set<ConcreteMulticastResponse> multicasters = new HashSet<ConcreteMulticastResponse>();

	public ConcreteResponse(Connection oc, int seq) {
		this.oc = oc;
		this.seq = seq;
	}

	@Override
	public void send(JSONObject payload) throws JSONException {
		if (unsubscribed)
			return;
		
		try {
			JSONObject msg = new JSONObject();
			msg.put("subscription", seq);
			msg.put("payload", payload);
			oc.send(msg);
		} catch (ZincBrokenConnectionException ex) {
			unsubscribed();
		}
	}

	@Override
	public void unsubscribed() {
		unsubscribed = true;
		for (ConcreteMulticastResponse r : multicasters)
			r.removeResponse(this);
	}
}
