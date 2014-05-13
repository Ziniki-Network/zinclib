package org.zincapi;


@SuppressWarnings("serial")
public class ZincInvalidPayloadException extends ZincException {
	public ZincInvalidPayloadException() {
		super("The provided payload was not valid JSONAPI");
	}

}
