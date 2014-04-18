package org.zincapi;

@SuppressWarnings("serial")
public class ZincNoServerException extends ZincException {
	public ZincNoServerException() {
		super("There is no server library available");
	}
}
