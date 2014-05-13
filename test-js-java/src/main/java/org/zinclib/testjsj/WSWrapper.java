package org.zinclib.testjsj;

import java.io.IOException;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.Request.METHOD;
import org.atmosphere.wasync.Request.TRANSPORT;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class WSWrapper extends Thread {
	private ScriptableObject myThis;
	private String uri;
	static {
		// try and pull in netty
		try {
			Class.forName("org.jboss.netty.channel.Channel");
		} catch (Exception ex) {
			// no netty
		}
	}
	
	public WSWrapper(final ScriptableObject myThis, String uri) {
		this.myThis = myThis;
		this.uri = uri;
	}
	
	@SuppressWarnings("rawtypes")
	public void run() {
		System.out.println("yo " + uri);
		Client client = ClientFactory.getDefault().newClient();
		RequestBuilder request = client.newRequestBuilder()
				.method(METHOD.GET)
				.uri(uri)
				.transport(TRANSPORT.WEBSOCKET)
				.transport(TRANSPORT.LONG_POLLING);

		try {
			Socket ws = client.create();
			ws
			.on(Event.MESSAGE, new Function<String>() {
				@Override
				public void on(String s) {
					handleMessage(s);
				}
			})
			.on(Event.OPEN, new Function<String>() {
				@Override
				public void on(String s) {
					System.out.println("hello " + s);
					org.mozilla.javascript.Function f = (org.mozilla.javascript.Function) myThis.get("onopen");
					Context c = Context.enter();
					f.call(c, myThis, myThis, new Object[] { s });
					Context.exit();
				}
			})
			// Handle Errors
			.on(Event.ERROR, new Function<Throwable>() {
				@Override
				public void on(Throwable t) {
					org.mozilla.javascript.Function f = (org.mozilla.javascript.Function) myThis.get("onerror");
					Context c = Context.enter();
					f.call(c, myThis, myThis, new Object[] { t.toString() });
					Context.exit();
				}
			})
			.open(request.build());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("done");
	}

	protected void handleMessage(String s) {
		org.mozilla.javascript.Function f = (org.mozilla.javascript.Function) myThis.get("onmessage");
		Context c = Context.enter();
		
		org.mozilla.javascript.Function cr = (org.mozilla.javascript.Function) c.evaluateString(myThis, "MessageEvent", "", -1, null);
		Object msg = cr.call(c, myThis, myThis, new Object[] { s });
		f.call(c, myThis, myThis, new Object[] { msg });
		Context.exit();
	}

}
