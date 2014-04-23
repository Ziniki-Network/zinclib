package org.zincapi;

@SuppressWarnings("serial")
public class ZincBrokenConnectionException extends ZincException {
	public ZincBrokenConnectionException() {
		super("Connection broken");
	}
}
