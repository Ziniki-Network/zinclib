package org.zincapi.client;

import java.io.IOException;

import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.Socket;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.concrete.ConcreteConnection;

public class ClientConnection extends ConcreteConnection {
	private final Socket ws;

	public ClientConnection(Socket ws, Request request) throws IOException {
		this.ws = ws;
		ws
		.on(Event.MESSAGE, new Function<String>() {
			@Override
			public void on(String s) {
				handleResponse(s);
			}
		})
		// Handle Errors
		.on(Event.ERROR, new Function<Throwable>() {
			@Override
			public void on(Throwable t) {
				t.printStackTrace();
			}
		})
		.open(request);
	}
	
	@Override
	public synchronized void send(JSONObject obj) {
		try {
			ws.fire(obj.toString());
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
