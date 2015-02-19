package org.zinclib.testjsj;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.zincapi.Zinc;
import org.zincapi.inline.server.ZincServlet;
import org.zinclib.testjj.TestingResourceHandler;
import org.zinutils.http.InlineServer;
import org.zinutils.http.NotifyOnServerReady;
import org.zinutils.rhino.RhinoEnvironment;
import org.zinutils.serialization.Endpoint;

@RunWith(ZincRhinoTest.class)
public class ReconnectionTests {
	private final InlineServer server;
	private final Endpoint addr;
	private final RhinoEnvironment rhino;
	private final TestingResourceHandler resourceHandler;

	public ReconnectionTests(final InlineServer server, final Endpoint addr, final Zinc zincServer, final RhinoEnvironment rhino) {
		this.server = server;
		this.addr = addr;
		this.rhino = rhino;
		rhino.jsdir("../js-client/zinc");
		rhino.jsdir("../js-client/vendor");
		rhino.loadResource("/rsvp.amd.js");
		rhino.loadResource("/atmosphere.js");
		rhino.loadResource("/zinc.js");

		resourceHandler = new TestingResourceHandler();
		resourceHandler.immediate(0, "hello");
		resourceHandler.immediate(1, "there");
		zincServer.handleResource("test", resourceHandler);
	}

	@Test
	public void testClientResubscribesAfterServerDeath() throws IOException {
		rhino.eval("zinc = requireModule('zinc')");
		rhino.eval("zinc.config.hbTimeout = 50");
		rhino.eval("reqPromise = zinc.newRequestor('http://"+addr+"/test')");
		rhino.eval("req = null");
		rhino.eval("messages = []");
		rhino.eval("reqPromise.then(function(rq) { req = rq; var s = req.subscribe('test', function (payload) { var msg = payload.messages[0].text; messages.push(msg); }); s.send(); })");

		rhino.wait("did not obtain a connection", 1000, "messages.length == 1.0");
		server.notify(new NotifyOnServerReady() {
			@Override
			public void serverReady(InlineServer inlineServer, String scheme, Endpoint addr) {
				ZincServlet zs = (ZincServlet) server.servletFor("/test").getImpl();
				Zinc zincServer = zs.getZinc();
				zincServer.handleResource("test", resourceHandler);
			}
		});
		server.restartServer();

		rhino.wait("did not reconnect", 1000, "messages.length == 2.0");
		assertEquals("hello", rhino.eval("messages[0]"));
		assertEquals("there", rhino.eval("messages[1]"));
	}
}
