package org.zincapi.concrete;

import org.zincapi.MakeRequest;
import org.zincapi.Requestor;
import org.zincapi.ResponseHandler;

public class ConcreteRequestor implements Requestor {
	private final ConcreteConnection conn;
	private final int channel;

	public ConcreteRequestor(ConcreteConnection conn, int channel) {
		this.conn = conn;
		this.channel = channel;
	}

	@Override
	public MakeRequest subscribe(String resource, ResponseHandler handler) {
		ConcreteMakeRequest ret = new ConcreteMakeRequest(conn, channel, "subscribe", handler);
		ret.requireSubcription();
		ret.setResource(resource);
		return ret;
	}

	@Override
	public MakeRequest create(String resource, ResponseHandler handler) {
		return requestWithOptionalHandler("create", resource, handler);
	}

	@Override
	public MakeRequest invoke(String resource, ResponseHandler handler) {
		return requestWithOptionalHandler("invoke", resource, handler);
	}

	private MakeRequest requestWithOptionalHandler(String method, String resource, ResponseHandler handler) {
		ConcreteMakeRequest ret = new ConcreteMakeRequest(conn, channel, method, handler);
		if (handler != null)
			ret.requireSubcription();
		ret.setResource(resource);
		return ret;
	}
	
	
}
