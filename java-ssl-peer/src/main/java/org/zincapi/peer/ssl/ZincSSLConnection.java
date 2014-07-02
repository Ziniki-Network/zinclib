package org.zincapi.peer.ssl;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.IncomingConnection;
import org.zincapi.OutgoingConnection;
import org.zincapi.concrete.ConcreteConnection;
import org.zincapi.peer.ssl.client.ZincSSL;
import org.zinutils.exceptions.UtilException;

public class ZincSSLConnection extends ConcreteConnection implements IncomingConnection, OutgoingConnection {
	private final ZincSSLParticipant sslconn;
	final IncomingConnection ic;

	public ZincSSLConnection(ZincSSL zinc, ZincSSLParticipant sslconn, boolean isServer) {
		super(zinc);
		this.sslconn = sslconn;
		if (isServer)
			ic = zinc.addConnection(this);
		else
			ic = this;
	}

	@Override
	public void send(JSONObject jsonObject) {
		sendTextMessage(jsonObject.toString());
	}

	@Override
	public void sendTextMessage(String msg) {
		try {
			sslconn.write(msg);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	@Override
	public void receiveTextMessage(String s) throws JSONException {
		super.handleMessage(s);
	}

	@Override
	public void close() {
		sslconn.closeOutbound();
	}
}
