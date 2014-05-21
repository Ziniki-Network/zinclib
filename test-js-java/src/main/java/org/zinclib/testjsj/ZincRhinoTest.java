package org.zinclib.testjsj;

import java.lang.reflect.Constructor;

import org.zincapi.Zinc;
import org.zincapi.inline.server.ZincServlet;
import org.zinutils.http.ISServletDefn;
import org.zinutils.http.InlineServer;
import org.zinutils.rhino.RhinoEnvironment;
import org.zinutils.rhino.ZinRhinoTest;
import org.zinutils.serialization.Endpoint;

public class ZincRhinoTest extends ZinRhinoTest {

	public ZincRhinoTest(Class<?> underTest) {
		super(underTest);
	}

	@Override
	protected void addServlets(InlineServer server) {
		ISServletDefn servlet = server.addServlet(null, "/test", "org.zincapi.inline.server.ZincServlet");
		servlet.initParam("org.atmosphere.cpr.sessionSupport", "true");
	}

	@Override
	protected Constructor<?> getConstructor(Class<?> underTest) throws Exception {
		return underTest.getConstructor(InlineServer.class, Endpoint.class, Zinc.class, RhinoEnvironment.class);
	}

	@Override
	protected Object createObject(InlineServer server, Endpoint addr, RhinoEnvironment rhino) throws Exception {
		ZincServlet zs = (ZincServlet) server.servletFor("/test").getImpl();
		Zinc zinc = zs.getZinc();
		return ctor.newInstance(server, addr, zinc, rhino);
	}
}
