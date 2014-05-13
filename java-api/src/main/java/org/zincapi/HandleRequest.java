package org.zincapi;

import java.util.Map;

import org.zincapi.jsonapi.Payload;

public interface HandleRequest {

	boolean isSubscribe();

	boolean isCreate();
	
	boolean isInvoke();
	
	String getResource();

	Map<String, Object> options();

	Payload getPayload();

	String getConnectionURI();

	Requestor obtainRequestor();
}
