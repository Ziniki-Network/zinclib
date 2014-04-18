package org.zincapi;

import org.codehaus.jettison.json.JSONObject;

public interface ResponseHandler {

	public void response(MakeRequest req, JSONObject payload);
}
