package org.zincapi;

@SuppressWarnings("serial")
public class ZincDuplicateResourceException extends ZincException {

	public ZincDuplicateResourceException(String resource) {
		super("Cannot specify handler for resource " + resource + " because that path is already handled");
	}

}
