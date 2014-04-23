package org.zincapi;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface MakeRequest {

	ResponseHandler getHandler();

	void setOption(String opt, Object val);

	void setPayload(JSONObject jsonObject);

	void send() throws JSONException;

	void unsubscribe() throws JSONException;
}
