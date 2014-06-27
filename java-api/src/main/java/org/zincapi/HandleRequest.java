package org.zincapi;

import java.util.Map;

import org.zincapi.jsonapi.Payload;

public interface HandleRequest {

	boolean isSubscribe();

	boolean isCreate();
	
	boolean isInvoke();
	
	String getResource();

	String getResourceParameter(String param);

	Map<String, Object> options();

	Payload getPayload();

	Connection getConnection();

	String getConnectionURI();

	Requestor obtainRequestor();


}
