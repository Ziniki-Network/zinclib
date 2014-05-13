package org.zincapi;

@SuppressWarnings("serial")
public class ZincPayloadReadonlyException extends ZincException {
	public ZincPayloadReadonlyException() {
		super("Cannot call create methods from read-only payload");
	}
}
