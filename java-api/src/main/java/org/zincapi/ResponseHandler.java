package org.zincapi;

import org.zincapi.jsonapi.Payload;

public interface ResponseHandler {

	public void response(MakeRequest req, Payload payload);
}
