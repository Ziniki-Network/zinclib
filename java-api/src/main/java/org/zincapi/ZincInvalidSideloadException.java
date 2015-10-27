package org.zincapi;


@SuppressWarnings("serial")
public class ZincInvalidSideloadException extends ZincException {
	public ZincInvalidSideloadException(String sltype, String oftype) {
		super("Cannot sideload type " + sltype + " into payload of type " + oftype);
	}

}
