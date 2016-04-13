package org.zincapi.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.Request.METHOD;
import org.atmosphere.wasync.Request.TRANSPORT;
import org.atmosphere.wasync.Socket.STATUS;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.LifecycleHandler;
import org.zincapi.Zinc;
import org.zincapi.concrete.ConcreteConnection;

public class ClientConnection extends ConcreteConnection {
	private final List<JSONObject> queue = new ArrayList<JSONObject>();
	private final Client<?,?,?> client;
	private final URI url;
	private Socket ws;

	public ClientConnection(Zinc zinc, Client<?,?,?> client, URI url) {
		super(zinc);
		this.client = client;
		this.url = url;
	}

	public boolean isConnected() {
		return ws != null && ws.status() == STATUS.OPEN;
	}
	
	public void establish() {
		try {
			ws = client.create();
			RequestBuilder<?> request = client.newRequestBuilder()
					.method(METHOD.GET)
					.uri(url.toASCIIString())
					.transport(TRANSPORT.WEBSOCKET)
					.transport(TRANSPORT.LONG_POLLING);
			ws
			.on(Event.OPEN, new Function<String>() {
				@Override
				public void on(String arg0) {
					System.out.println("onOpen called, #queue = " + queue.size());
					for (JSONObject msg : queue)
						send(msg);
				}
			})
			.on(Event.MESSAGE, new Function<String>() {
				@Override
				public void on(String s) {
					handleMessage(s);
				}
			})
			// Handle Errors
			.on(Event.ERROR, new Function<Throwable>() {
				@Override
				public void on(Throwable t) {
					System.out.println("#lch = " + lcHandlers.size());
					for (LifecycleHandler lch : lcHandlers)
						lch.onError(ClientConnection.this, t);
				}
			})
			.open(request.build());
		} catch (IOException e) {
			// I believe that errors here are also reported to the handler,
			// so we don't want to handle them twice ...
//			e.printStackTrace(System.out);
		}
	}
	
	@Override
	public synchronized void send(JSONObject obj) {
		try {
			if (ws != null && ws.status() == STATUS.OPEN)
				ws.fire(obj.toString());
			else
				queue.add(obj);
		} catch (IOException ex) {
			// has the connection broken?
			ex.printStackTrace();
		}
	}
	
	@Override
	public void close() {
		ws.close();
	}

}
