package org.zincapi;

@SuppressWarnings("serial")
public class ZincCannotSetPayloadException extends ZincException {

	public ZincCannotSetPayloadException() {
		super("Cannot set payload multiple times");
	}

}
