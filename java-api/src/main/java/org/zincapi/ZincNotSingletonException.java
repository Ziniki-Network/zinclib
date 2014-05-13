package org.zincapi;

@SuppressWarnings("serial")
public class ZincNotSingletonException extends ZincException {
	public ZincNotSingletonException(String want, String have, int count) {
		super(want.equals(have)?"The payload had " + count + " entries, not 1":"The payload was of type " + have + ", not " + want);
	}
}
