package org.zincapi;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.jsonapi.Payload;

public interface Response {

	void send(Payload obj) throws JSONException;

	void unsubscribed();

	Connection getConnection();
}
