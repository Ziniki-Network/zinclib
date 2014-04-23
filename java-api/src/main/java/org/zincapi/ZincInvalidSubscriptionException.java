package org.zincapi;

@SuppressWarnings("serial")
public class ZincInvalidSubscriptionException extends ZincException {
	public ZincInvalidSubscriptionException(int handle) {
		super("There was no subscription associated with handle " + handle);
	}
}
