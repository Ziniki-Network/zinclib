package org.zinclib.testjj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zinutils.http.InlineServer;
import org.zinutils.serialization.Endpoint;
import org.zinutils.sync.SyncUtils;

@RunWith(ZincJavaJavaTest.class)
public class SimpleConnectionTests {
	private static final Logger logger = LoggerFactory.getLogger("SimpleConnTests");
	private final Endpoint addr;

	public SimpleConnectionTests(final InlineServer server, final Endpoint addr, final Zinc zinc) {
		this.addr = addr;
	}

	@Test
	public void testWeCantConnectToSomethingThatDoesntExist() throws IOException {
		Zinc client = new Zinc();
		TestingLifecycleHandler lch = new TestingLifecycleHandler();
		Requestor requestor = client.newRequestor(URI.create("http://localhost:4844/test"), lch);
		assertNotNull(requestor);
		SyncUtils.sleep(500);
		logger.info("conns = " + lch.connections + " errors = " + lch.errors);
		assertEquals(0, lch.connections);
		assertTrue(lch.errors > 0);
	}

	@Test
	public void testWeCantConnectToTheWrongServlet() throws IOException {
		Zinc client = new Zinc();
		TestingLifecycleHandler lch = new TestingLifecycleHandler();
		Requestor requestor = client.newRequestor(URI.create("http://"+addr+"/foo"), lch);
		assertNotNull(requestor);
		SyncUtils.sleep(500);
		logger.info("conns = " + lch.connections + " errors = " + lch.errors);
		assertEquals(0, lch.connections);
		assertTrue(lch.errors > 0);
	}

	@Test
	public void testWeCanObtainARequestor() throws IOException {
		Zinc client = new Zinc();
		Requestor requestor = client.newRequestor(URI.create("http://"+addr+"/test"));
		assertNotNull(requestor);
	}
}
