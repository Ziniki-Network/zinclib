package org.zinclib.testjj;

import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zinutils.http.InlineServer;
import org.zinutils.serialization.Endpoint;

@RunWith(ZincJavaJavaTest.class)
public class SimpleSubcribeTests extends TestingResponseHandler {
	private final Endpoint addr;
	private final Zinc zincServer;

	public SimpleSubcribeTests(final InlineServer server, final Endpoint addr, final Zinc zinc) {
		this.addr = addr;
		this.zincServer = zinc;
	}

	@Test
	public void testWeCanSubscribeToASimpleResource() throws Exception {
		// set up server
		TestingResourceHandler resourceHandler = new TestingResourceHandler();
		resourceHandler.immediate(0, "hello");
		zincServer.handleResource("simple", resourceHandler);
		Zinc client = new Zinc();
		Requestor requestor = client.newRequestor(URI.create("http://"+addr+"/test"));
		assertNotNull(requestor);
		requestor.subscribe("simple", this).send();
		assertMessages("hello");
		throwErrors();
	}

	@Test
	public void testWeCanSubscribeToMultipleMessagesFromASimpleResource() throws Exception {
		// set up server
		TestingResourceHandler resourceHandler = new TestingResourceHandler();
		resourceHandler.immediate(0, "hello");
		resourceHandler.delayed(0, 10, "there");
		zincServer.handleResource("simple", resourceHandler);
		Zinc client = new Zinc();
		Requestor requestor = client.newRequestor(URI.create("http://"+addr+"/test"));
		assertNotNull(requestor);
		requestor.subscribe("simple", this).send();
		assertMessages("hello", "there");
		throwErrors();
	}


	@Test
	public void testWeCanCreateTwoIndependentChannelsToTheSameConnection() throws Exception {
		// set up server
		TokenResourceHandler resourceHandler = new TokenResourceHandler();
		zincServer.handleResource("setToken", resourceHandler);
		zincServer.handleResource("simple", resourceHandler);
		Zinc client = new Zinc();
		Requestor req1 = client.newRequestor(URI.create("http://"+addr+"/test"));
		assertNotNull(req1);
		Requestor req2 = client.newRequestor(URI.create("http://"+addr+"/test"));
		assertNotNull(req2);
		req1.invoke("setToken", null).setOption("token", "81").send();
		req2.invoke("setToken", null).setOption("token", "92").send();
		req1.subscribe("simple", this).send();
		req2.subscribe("simple", this).send();
		assertMessages("hello 81", "hello 92");
		throwErrors();
	}
}
