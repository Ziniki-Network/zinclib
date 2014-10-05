package org.zinclib.testjj;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zincapi.ZincException;
import org.zinutils.http.InlineServer;
import org.zinutils.serialization.Endpoint;

@RunWith(ZincJavaJavaTest.class)
public class SimpleConnectionTests {
	private final Endpoint addr;

	public SimpleConnectionTests(final InlineServer server, final Endpoint addr, final Zinc zinc) {
		this.addr = addr;
	}

	@Test(expected=ZincException.class)
	public void testWeCantConnectToSomethingThatDoesntExist() throws IOException {
		Zinc client = new Zinc();
		client.newRequestor(URI.create("http://localhost:4844/test"));
	}

	@Test(expected=ZincException.class)
	public void testWeCantConnectToTheWrongServlet() throws IOException {
		Zinc client = new Zinc();
		client.newRequestor(URI.create("http://"+addr+"/foo"));
	}

	@Test
	public void testWeCanObtainARequestor() throws IOException {
		Zinc client = new Zinc();
		Requestor requestor = client.newRequestor(URI.create("http://"+addr+"/test"));
		assertNotNull(requestor);
	}
}
