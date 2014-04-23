package org.zincapi.concrete;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.MulticastResponse;
import org.zincapi.Response;

public class ConcreteMulticastResponse implements MulticastResponse {
	private final Set<Response> responses = new HashSet<Response>();
	private boolean unsubscribed;

	@Override
	public void attachResponse(Response response) {
		if (response == null)
			return; // can't attach a non-existent response
		synchronized (responses) {
			responses.add(response);
		}
	}
	
	public void removeResponse(Response r) {
		responses.remove(r);
	}

	@Override
	public void send(JSONObject jsonObject) throws JSONException {
		if (unsubscribed)
			return;
		
		Set<Response> tmp;
		synchronized (responses) {
			tmp = new HashSet<Response>(responses);
		}
		for (Response r : tmp) {
			r.send(jsonObject);
		}
	}

	@Override
	public void unsubscribed() {
		this.unsubscribed = true;
	}
}
