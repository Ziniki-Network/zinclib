package org.zincapi;

@SuppressWarnings("serial")
public class ZincNoResourceParameterException extends ZincException {
	public ZincNoResourceParameterException(String param, String resource) {
		super("There is no parameter '" + param + "'in the resource: " + resource);
	}
}
