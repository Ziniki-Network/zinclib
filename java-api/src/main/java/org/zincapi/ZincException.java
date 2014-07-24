package org.zincapi;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("serial")
public class ZincException extends RuntimeException {
	public ZincException(String msg) {
		super(msg);
	}

	public ZincException(String msg, Throwable ex) {
		super(msg, ex);
	}

	public static RuntimeException wrap(Throwable ex) {
		if (ex instanceof RuntimeException)
			return (RuntimeException)ex;
		else if (ex instanceof InvocationTargetException || ex instanceof ExecutionException)
			return wrap(ex.getCause());
		else
			return new ZincException("A checked exception was caught", ex);
	}
}
