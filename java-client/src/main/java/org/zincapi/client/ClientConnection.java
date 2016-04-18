package org.zincapi.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request.METHOD;
import org.atmosphere.wasync.Request.TRANSPORT;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.Socket.STATUS;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.LifecycleHandler;
import org.zincapi.Zinc;
import org.zincapi.concrete.ConcreteConnection;
import org.zincapi.concrete.ConcreteMakeRequest;

public class ClientConnection extends ConcreteConnection {
	private final List<JSONObject> queue = new ArrayList<JSONObject>();
	private final Client<?,?,?> client;
	private final URI url;
	private Socket ws;
	private boolean connecting = false;
	private boolean pending = true;
	protected int pendingHandlers;
	private final String idType;
	private final String idAddress;

	public ClientConnection(Zinc zinc, Client<?,?,?> client, URI url, String idType, String idAddress) {
		super(zinc);
		this.client = client;
		this.url = url;
		this.idType = idType;
		this.idAddress = idAddress;
	}

	public boolean isConnected() {
		return ws != null && ws.status() == STATUS.OPEN;
	}

	public boolean isConnecting() {
		return connecting;
	}
	
	public void establish() {
		try {
			connecting = true;
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
					try {
						ConcreteMakeRequest mr = new ConcreteMakeRequest(ClientConnection.this, 0, "establish");
						mr.setOption("type", idType);
						mr.setOption("address", idAddress);
						ClientConnection.this.send(mr.getPayloadAsJson(), true);
						pendingHandlers = lcHandlers.size();
						for (LifecycleHandler lch : lcHandlers)
							lch.onConnection(ClientConnection.this);
						connecting = false;
					} catch (Throwable t) {
						logger.error("Error encountered", t);
					}
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
					ws.close();
					connecting = false;
					pending = true;
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
	public synchronized void reducePending() {
		logger.error("Reducing pending to " + (pendingHandlers-1));
		if (--pendingHandlers == 0)
			removePending();
	}
	
	private void removePending() {
		for (JSONObject msg : queue)
			send(msg, true);
		queue.clear();
		pending = false;
		for (LifecycleHandler lch : lcHandlers)
			lch.onReady(ClientConnection.this);
	}

	@Override
	public synchronized void send(JSONObject obj) {
		send(obj, false);
	}

	public void send(JSONObject obj, boolean overridePending) {
		try {
			logger.error("Sending msg " + obj + " to server with status " + (ws == null?"NULL":ws.status().toString()) + " and pending = " + pending + " (" + pendingHandlers + ")");
			if (ws != null && ws.status() == STATUS.OPEN && (overridePending || !pending ))
				ws.fire(obj.toString());
			else {
				queue.add(obj);
				logger.error("queue = " + queue);
			}
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
