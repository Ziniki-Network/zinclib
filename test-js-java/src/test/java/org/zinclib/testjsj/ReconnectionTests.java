package org.zinclib.testjsj;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.zincapi.Zinc;
import org.zinutils.http.InlineServer;
import org.zinutils.rhino.RhinoEnvironment;
import org.zinutils.serialization.Endpoint;

@RunWith(ZincRhinoTest.class)
public class ReconnectionTests {
	private final Endpoint addr;
	private final RhinoEnvironment rhino;
	private final InlineServer server;

	public ReconnectionTests(final InlineServer server, final Endpoint addr, final Zinc zinc, final RhinoEnvironment rhino) {
		this.server = server;
		this.addr = addr;
		this.rhino = rhino;
		rhino.jsdir("../js-client/zinc");
		rhino.jsdir("../js-client/vendor");
		rhino.loadResource("/rsvp.amd.js");
		rhino.loadResource("/atmosphere.js");
		rhino.loadResource("/zinc.js");
	}

	@Test
	public void testClientResubscribesAfterServerDeath() throws IOException {
		rhino.eval("zinc = requireModule('zinc')");
		rhino.eval("zinc.config.hbTimeout = 50");
		rhino.eval("req = zinc.newRequestor('http://"+addr+"/test')");
		rhino.eval("marker = []");
		rhino.eval("req.then(function(rq) { marker.push(rq); var s = rq.subscribe('test', function (msg) { messages.push(msg); }); })");

		// TODO: make this rhino.wait("did not obtain a connection", 1000, "marker.length == 1.0")
		for (int i=0;i<10;i++) {
			if (rhino.eval("marker.length == 1.0").equals(true))
				break;
			rhino.eval("Envjs.wait(100)");
		}
		assertEquals("did not obtain a connection", 1.0, rhino.eval("marker.length"));
		
		server.restartServer();
		for (int i=0;i<100;i++) {
//			if (rhino.eval("marker.length").equals(1.0))
//				break;
			rhino.eval("Envjs.wait(100)");
		}
	}
}
