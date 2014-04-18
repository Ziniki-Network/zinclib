package org.zincapi;

@SuppressWarnings("serial")
public class ZincNoClientException extends ZincException {
	public ZincNoClientException() {
		super("There is no client library available");
	}
}
