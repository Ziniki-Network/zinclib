package org.zincapi.concrete;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.Connection;
import org.zincapi.MulticastResponse;
import org.zincapi.Response;
import org.zincapi.ZincException;
import org.zincapi.jsonapi.Payload;

public class ConcreteMulticastResponse implements MulticastResponse {
	private final Set<Response> responses = new HashSet<Response>();
	private boolean unsubscribed;

	@Override
	public Connection getConnection() {
		throw new ZincException("Cannot get a connection from a multicast response");
	}

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
	public void send(Payload payload) throws JSONException {
		send("replace", payload);
	}

	@Override
	public void send(String action, Payload payload) throws JSONException {
		if (unsubscribed)
			return;
		
		Set<Response> tmp;
		synchronized (responses) {
			tmp = new HashSet<Response>(responses);
		}
		for (Response r : tmp) {
			r.send(action, payload);
		}
	}

	@Override
	public void unsubscribed() {
		this.unsubscribed = true;
	}
}
