package org.zinclib.testjj;

import java.util.HashMap;
import java.util.Map;

import org.zincapi.Channel;
import org.zincapi.HandleRequest;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;
import org.zincapi.jsonapi.Payload;
import org.zincapi.jsonapi.PayloadItem;

public class TokenResourceHandler implements ResourceHandler {
	public class State {

		public String token;

	}

	private final Map<Channel, State> states = new HashMap<Channel, State>();
	
	@Override
	public void handle(final HandleRequest hr, final Response response) throws Exception {
		State ret;
		synchronized (states) {
			ret = states.get(hr.getChannel());
			if (ret == null) {
				ret = new State();
				states.put(hr.getChannel(), ret);
			}
		}
		if (hr.isInvoke() && hr.getResource().equals("setToken")) {
			String token = (String) hr.options().get("token");
			ret.token = token;
			return;
		}
		Payload p = new Payload("messages");
		PayloadItem item = p.newItem();
		item.set("text", "hello " + ret.token);
		response.send(p);
	}
}
