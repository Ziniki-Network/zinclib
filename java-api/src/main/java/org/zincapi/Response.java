package org.zincapi;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.jsonapi.Payload;

public interface Response {

	void send(String action, Payload payload) throws JSONException;

	void send(Payload payload) throws JSONException;

	void unsubscribed();

	Connection getConnection();
}
