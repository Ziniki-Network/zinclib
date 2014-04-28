package org.zincapi;

import org.codehaus.jettison.json.JSONObject;

public interface HandleRequest {

	boolean isSubscribe();

	boolean isCreate();

	JSONObject getPayload();

	String getConnectionURI();

	Requestor obtainRequestor();

}
