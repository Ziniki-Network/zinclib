package org.zincapi.concrete;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.OutgoingConnection;
import org.zincapi.Response;

public class ConcreteResponse implements Response {
	private final OutgoingConnection oc;
	private final int seq;

	public ConcreteResponse(OutgoingConnection oc, int seq) {
		this.oc = oc;
		this.seq = seq;
	}

	@Override
	public void send(JSONObject payload) throws JSONException {
		JSONObject msg = new JSONObject();
		msg.put("subscription", seq);
		msg.put("payload", payload);
		oc.sendTextMessage(msg.toString());
	}

}
