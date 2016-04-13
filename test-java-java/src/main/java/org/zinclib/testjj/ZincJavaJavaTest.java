package org.zinclib.testjj;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.Zinc;
import org.zincapi.inline.server.ZincServlet;
import org.zinutils.http.ISServletDefn;
import org.zinutils.http.InlineServer;
import org.zinutils.http.NotifyOnServerReady;
import org.zinutils.serialization.Endpoint;

public class ZincJavaJavaTest extends Runner {
	private final static Logger logger = LoggerFactory.getLogger("ZJJT");
	private final Constructor<?> ctor;
	private final Description suite;
	private final List<TestMethod> toTest = new ArrayList<TestMethod>();

	public ZincJavaJavaTest(Class<?> underTest) {
		try {
			ctor = underTest.getConstructor(InlineServer.class, Endpoint.class, Zinc.class);
		} catch (Exception ex) {
			throw new AssertionError("Cannot create " + underTest.getName() + " because it does not have a constructor for (InlineServer, Endpoint, Zinc)");
		}
		suite = Description.createSuiteDescription(underTest);
		String pattern = System.getProperty("org.zinclib.pattern");
		Pattern p = null;
		if (pattern != null)
			p = Pattern.compile(pattern);
		for (Method m : underTest.getMethods()) {
			if (m.getReturnType().equals(Void.TYPE) && m.getParameterCount() == 0 && m.getAnnotation(Test.class) != null && m.getAnnotation(Ignore.class) == null && (p == null || p.matcher(m.getName()).find())) {
				TestMethod e = new TestMethod(underTest, m);
				suite.addChild(e.getDescription());
				toTest.add(e);
			}
		}
	}
	
	protected void go(final Method method) throws Throwable
	{
		InlineServer server = new InlineServer(8480, "org.zincapi.inline.server.ZincServlet$ServerOnly");
		ISServletDefn servlet = server.getBaseServlet();
		servlet.initParam("org.atmosphere.cpr.sessionSupport", "true");
//		servlet.initParam("org.zincapi.server.init", "org.zincapi.chirpy.server.Main");
		servlet.setServletPath("/test");

		server.notify(new NotifyOnServerReady() {
			@Override
			public void serverReady(final InlineServer server, String scheme, final Endpoint addr) {
				Thread thr = new Thread() {
					public void run() {
						try {
							ZincServlet zs = (ZincServlet) server.servletFor("/test").getImpl();
							Zinc zinc = zs.getZinc();
							Object obj = ctor.newInstance(server, addr, zinc);
							method.invoke(obj);
						} catch (InvocationTargetException ite) {
							server.addFailure(ite.getCause());
						} catch (Exception e) {
							server.addFailure(e);
						}
						server.pleaseExit();
					}
				};
				thr.start();
			}
		});
		server.run();
		if (server.getFailure() != null)
			throw server.getFailure();
	}

	@Override
	public Description getDescription() {
		return suite;
	}

	@Override
	public void run(RunNotifier notifier) {
		for (TestMethod m : toTest) {
			Description d = m.getDescription();
			notifier.fireTestStarted(d);
			try {
				logger.error("Running test " + d);
				go(m.getMethod());
				if (m.expectsError() != null)
					Assert.fail("Expected exception: " + m.expectsError());
			} catch (Throwable ex) {
				if (!m.isExpected(ex))
					notifier.fireTestFailure(new Failure(d, ex));
			}
			notifier.fireTestFinished(d);
		}
	}
}
