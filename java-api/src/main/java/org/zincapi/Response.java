package org.zincapi;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface Response {

	void send(JSONObject jsonObject) throws JSONException;

}
