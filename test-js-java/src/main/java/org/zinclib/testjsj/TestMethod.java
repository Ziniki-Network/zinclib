package org.zinclib.testjsj;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.Test.None;
import org.junit.runner.Description;

public class TestMethod {
	private final Description description;
	private final Method method;
	private Test testAnnotation;

	public TestMethod(Class<?> underTest, Method method) {
		this.method = method;
		testAnnotation = method.getAnnotation(Test.class);
		description = Description.createTestDescription(underTest, method.getName());
	}

	public Description getDescription() {
		return description;
	}

	public Method getMethod() {
		return method;
	}

	public boolean isExpected(Throwable ex) {
		return testAnnotation.expected() != null && testAnnotation.expected().isInstance(ex);
	}

	public Class<? extends Throwable> expectsError() {
		if (testAnnotation.expected().equals(None.class))
			return null;
		return testAnnotation.expected();
	}
}
