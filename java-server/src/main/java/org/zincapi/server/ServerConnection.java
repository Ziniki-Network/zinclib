package org.zincapi.server;

import org.atmosphere.cpr.AtmosphereResponse;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.OutgoingConnection;
import org.zincapi.ZincBrokenConnectionException;
import org.zincapi.concrete.ConcreteConnection;

public class ServerConnection extends ConcreteConnection implements OutgoingConnection {
	private final AtmosphereResponse atmo;

	public ServerConnection(AtmosphereResponse atmo) {
		this.atmo = atmo;
	}

	@Override
	public void send(JSONObject jsonObject) {
		sendTextMessage(jsonObject.toString());
	}

	@Override
	public void sendTextMessage(String msg) {
		try {
			atmo.getAsyncIOWriter().write(atmo, msg);
		} catch (Exception ex) {
			throw new ZincBrokenConnectionException();
		}
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
}
