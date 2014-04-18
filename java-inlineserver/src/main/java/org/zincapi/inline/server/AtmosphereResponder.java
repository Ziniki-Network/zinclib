package org.zincapi.inline.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.IncomingConnection;
import org.zincapi.OutgoingConnection;
import org.zincapi.Zinc;
import org.zincapi.server.ServerConnection;

public class AtmosphereResponder implements AtmosphereHandler {
	final static Logger logger = LoggerFactory.getLogger("AtmosphereResponder");
	private final Zinc zinc;
	private final Map<HttpSession, ServerConnection> pending = new HashMap<HttpSession, ServerConnection>();
	private final Map<HttpSession, IncomingConnection> active = new HashMap<HttpSession, IncomingConnection>();
	private IncomingConnection conn;

	public AtmosphereResponder(Zinc zinc) {
		this.zinc = zinc;
	}

	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest request = resource.getRequest();
		HttpSession session = resource.session();
		if (session == null)
			throw new RuntimeException("Session must be non-null");
		if (request.getMethod().equalsIgnoreCase("get"))
		{
			// initiating the channel
			resource.suspend();
			synchronized (pending) {
				pending.put(session, new ServerConnection(resource.getResponse()));
			}
		}
		else if (request.getMethod().equalsIgnoreCase("post"))
		{
			String input = request.getReader().readLine().trim();
			logger.info("Handling input " + input);
			try {
				// I feel we may be too low-level here and this should be on Zinc/or the connection
				synchronized (pending) {
					if (pending.containsKey(session)) {
						OutgoingConnection c = pending.remove(session);
						IncomingConnection ic = zinc.addConnection(c);
						ic.receiveTextMessage(input); // this should be the "establish" method
						synchronized (active) {
							// only add it to active if the message was accepted and the connection was established
							active.put(session, ic);
						}
						return;
					}
				}
				synchronized (active) {
					if (!active.containsKey(session)) {
						logger.info("There is no connection for session " + session);
						return;
					}
					conn = active.get(session);
					conn.receiveTextMessage(input);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				// Should send back if interested
			}
		}
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent ev) throws IOException {
		if (ev.getResource().isCancelled()) {
			HttpSession session = ev.getResource().session();
			logger.info("Closing connection for " + session);
//			ObserverSender sender = conns.get(session);
//			if (sender != null) {
//				sender.close();
//				conns.remove(session);
//			}
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
