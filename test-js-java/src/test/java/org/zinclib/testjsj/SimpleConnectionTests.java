package org.zinclib.testjsj;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.zincapi.Zinc;
import org.zinutils.http.InlineServer;
import org.zinutils.serialization.Endpoint;
import org.zinutils.sync.SyncUtils;

@RunWith(ZincJavaScriptJavaTest.class)
public class SimpleConnectionTests {
	private final Endpoint addr;
	private final JSEnvironment jsenv;

	public SimpleConnectionTests(final InlineServer server, final Endpoint addr, final Zinc zinc, final JSEnvironment jsenv) {
		this.addr = addr;
		this.jsenv = jsenv;
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
		jsenv.eval("window.console['info'] = function(a) { java.lang.System.out.println(a); }");
		jsenv.eval("window.console['debug'] = function(a) { java.lang.System.out.println(a); }");
//		Object tmp1 = jsenv.eval("requirejs.load()");
//		System.out.println(tmp1);
		jsenv.eval("zinc = requireModule('zinc')");
		Object zinc = jsenv.eval("zinc");
		System.out.println(zinc);
		Object nr = jsenv.eval("req = zinc.newRequestor('http://"+addr+"/test')");
		System.out.println(nr);
		jsenv.eval("marker = []");
		Object req = jsenv.eval("req.then(function(rq) { java.lang.System.out.println('hello from rq callback - i.e. test worked'); marker.push(rq); })");
		System.out.println(req);
		
		// allow timers etc. to work
		jsenv.eval("Envjs.wait(2000)");
		System.out.println();
		assertEquals(1.0, jsenv.eval("marker.length"));
		assertNotNull(jsenv.eval("marker[0]"));
	}
}
