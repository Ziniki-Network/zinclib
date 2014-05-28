package org.zincapi;

@SuppressWarnings("serial")
public class ZincMultipleMatchException extends ZincException {
	public ZincMultipleMatchException(String resource) {
		super("Multiple handlers matched resource " + resource);
	}
}
