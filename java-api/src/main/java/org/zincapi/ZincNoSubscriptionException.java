package org.zincapi;

@SuppressWarnings("serial")
public class ZincNoSubscriptionException extends ZincException {
	public ZincNoSubscriptionException() {
		super("There was no subscription specified");
	}
}
