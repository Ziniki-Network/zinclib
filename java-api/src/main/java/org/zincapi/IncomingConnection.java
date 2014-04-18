package org.zincapi;

import org.codehaus.jettison.json.JSONException;

public interface IncomingConnection {
	public void receiveTextMessage(String s) throws JSONException;
}
