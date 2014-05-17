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
public class SimpleConnectionTests {
	private final Endpoint addr;
	private final RhinoEnvironment rhino;

	public SimpleConnectionTests(final InlineServer server, final Endpoint addr, final Zinc zinc, final RhinoEnvironment rhino) {
		this.addr = addr;
		this.rhino = rhino;
		rhino.jsdir("../js-client/zinc");
		rhino.jsdir("../js-client/vendor");
		rhino.loadResource("/rsvp.amd.js");
		rhino.loadResource("/atmosphere.js");
		rhino.loadResource("/zinc.js");
	}

//	@Test(expected=ConnectException.class)
//	public void testWeCantConnectToSomethingThatDoesntExist() throws IOException {
//		Zinc client = new Zinc();
//		client.newRequestor(URI.create("http://localhost:4844/test"));
//	}
//
//	@Test(expected=IOException.class)
//	public void testWeCantConnectToTheWrongServlet() throws IOException {
//		Zinc client = new Zinc();
//		client.newRequestor(URI.create("http://"+addr+"/foo"));
//	}
//
	@Test
	public void testWeCanObtainARequestor() throws IOException {
		rhino.eval("zinc = requireModule('zinc')");
		rhino.eval("req = zinc.newRequestor('http://"+addr+"/test')");
		rhino.eval("marker = []");
		rhino.eval("req.then(function(rq) { java.lang.System.out.println('Promise resolved with ' + rq); marker.push(rq); })");
		
		// allow timers etc. to work
		for (int i=0;i<10;i++) {
			if (rhino.eval("marker.length").equals(1.0))
				break;
			rhino.eval("Envjs.wait(100)");
		}
		assertEquals(1.0, rhino.eval("marker.length"));
		assertNotNull(rhino.eval("marker[0]"));
	}
}
