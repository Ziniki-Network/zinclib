package org.zincapi.server;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.ConnectionHandler;
import org.zincapi.HandleRequest;
import org.zincapi.IncomingConnection;
import org.zincapi.OutgoingConnection;
import org.zincapi.Zinc;
import org.zincapi.concrete.ConcreteConnection;
import org.zincapi.concrete.ConcreteHandleRequest;

public class IncomingServerConnection extends ConcreteConnection implements IncomingConnection {
	private final OutgoingConnection replyTo;
	boolean isEstablished = false;

	public IncomingServerConnection(Zinc zinc, OutgoingConnection oc) {
		super(zinc);
		this.replyTo = oc;
	}

	@Override
	public void receiveTextMessage(String input) throws JSONException {
		JSONObject msg = new JSONObject(input);
		if (!isEstablished) {
			tryToEstablish(msg);
			isEstablished = true;
		}
		else
			super.handleMessage(msg);
	}

	private void tryToEstablish(JSONObject req) throws JSONException {
		JSONObject rq = req.getJSONObject("request");
		if (rq == null)
			throw new RuntimeException("No request");
		String m = rq.getString("method");
		if (!m.equals("establish"))
			throw new RuntimeException("Method was not establish");
		if (rq.has("options")) {
			JSONObject opts = rq.getJSONObject("options");
			if (opts.has("type") && opts.has("address")) {
				String type = opts.getString("type");
				String address = opts.getString("address");
				this.setURI(address);
				HandleRequest hr = new ConcreteHandleRequest(this, 0, m);
				for (ConnectionHandler h : zinc.getConnectionHandlers()) {
					h.newConnection(hr, type, address);
				}
			}
		}
	}

	@Override
	public void send(JSONObject jsonObject, boolean overridePending) {
		replyTo.sendTextMessage(jsonObject.toString());
	}

	@Override
	public void reducePending() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// Needs propagating to OutgoingConnection (or otherwise)
	}
}
