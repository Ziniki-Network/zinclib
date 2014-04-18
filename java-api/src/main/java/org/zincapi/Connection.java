package org.zincapi;

import org.codehaus.jettison.json.JSONObject;

public interface Connection {
	int nextHandle(MakeRequest r);

	void send(JSONObject jsonObject);

	void close();
}
