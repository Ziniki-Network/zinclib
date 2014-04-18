package org.zincapi;

@SuppressWarnings("serial")
public class ZincNoResourceHandlerException extends ZincException {
	public ZincNoResourceHandlerException(String resource) {
		super("There is no handler for resource " + resource);
	}
}
