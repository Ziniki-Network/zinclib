package org.zincapi;

import org.codehaus.jettison.json.JSONException;
import org.zincapi.jsonapi.Payload;

public interface MakeRequest {

	ResponseHandler getHandler();

	void setOption(String opt, Object val);

	void setPayload(Payload payload);

	void send() throws JSONException;

	void unsubscribe() throws JSONException;
}
