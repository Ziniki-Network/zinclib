package org.zincapi.concrete;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.OutgoingConnection;
import org.zincapi.Response;
import org.zincapi.ZincBrokenConnectionException;

public class ConcreteResponse implements Response {
	private final OutgoingConnection oc;
	private final int seq;
	private boolean unsubscribed;
	private final Set<ConcreteMulticastResponse> multicasters = new HashSet<ConcreteMulticastResponse>();

	public ConcreteResponse(OutgoingConnection oc, int seq) {
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
			oc.sendTextMessage(msg.toString());
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
