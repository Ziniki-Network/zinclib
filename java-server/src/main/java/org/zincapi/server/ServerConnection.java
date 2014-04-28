package org.zincapi.server;

import org.atmosphere.cpr.AtmosphereResponse;
import org.zincapi.OutgoingConnection;
import org.zincapi.Zinc;
import org.zincapi.ZincBrokenConnectionException;

public class ServerConnection implements OutgoingConnection {
	private final AtmosphereResponse atmo;

	public ServerConnection(Zinc zinc, AtmosphereResponse atmo) {
		this.atmo = atmo;
	}

	@Override
	public void sendTextMessage(String msg) {
		try {
			atmo.getAsyncIOWriter().write(atmo, msg);
		} catch (Exception ex) {
			throw new ZincBrokenConnectionException();
		}
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}
}
