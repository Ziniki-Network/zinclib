package org.zincapi.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.IncomingConnection;
import org.zincapi.OutgoingConnection;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;
import org.zincapi.Zinc;
import org.zincapi.ZincInvalidSubscriptionException;
import org.zincapi.ZincNoSubscriptionException;
import org.zincapi.concrete.ConcreteHandleRequest;
import org.zincapi.concrete.ConcreteResponse;

public class IncomingServerConnection implements IncomingConnection {
	private final ZiNCServer server;
	private final OutgoingConnection replyTo;
	private final Map<Integer, Response> subscriptions = new HashMap<Integer, Response>();
	boolean isEstablished = false;

	public IncomingServerConnection(Zinc zinc, ZiNCServer server, OutgoingConnection oc) {
		this.server = server;
		this.replyTo = oc;
	}

	@Override
	public void receiveTextMessage(String input) throws JSONException {
		JSONObject msg = new JSONObject(input);
		if (!isEstablished) {
			tryToEstablish(msg);
			isEstablished = true;
			return;
		}
		JSONObject req = msg.getJSONObject("request");
		String method = req.getString("method");
		if (method.equals("unsubscribe")) {
			if (!msg.has("subscription"))
				throw new ZincNoSubscriptionException();
			int sub = msg.getInt("subscription");
			if (!subscriptions.containsKey(sub))
				throw new ZincInvalidSubscriptionException(sub);
			Response deadResponse = subscriptions.remove(sub);
			deadResponse.unsubscribed();
			return;
		}
		String resource = null;
		if (req.has("resource"))
			resource = req.getString("resource");
		ConcreteHandleRequest hr = new ConcreteHandleRequest(method);
		ResourceHandler handler = server.getHandler(hr, resource);
		if (req.has("options")) {
			JSONObject opts = req.getJSONObject("options");
			@SuppressWarnings("unchecked")
			Iterator<String> keys = opts.keys();
			while (keys.hasNext()) {
				String k = keys.next();
				hr.setOption(k, opts.get(k));
			}
		}
		if (msg.has("payload"))
			hr.setPayload(msg.getJSONObject("payload"));
		try {
			// May need more than this if we have a "subscription"
			Response response = null;
			if (msg.has("subscription")) {
				int sub = msg.getInt("subscription");
				response = new ConcreteResponse(replyTo, sub);
				subscriptions.put(sub, response);
			}
			handler.handle(hr, response);
		} catch (Exception ex) {
			ex.printStackTrace();
			// TODO: send a "500" exception back
		}
	}

	private void tryToEstablish(JSONObject req) throws JSONException {
		JSONObject rq = req.getJSONObject("request");
		if (rq == null)
			throw new RuntimeException("No request");
		String m = rq.getString("method");
		if (!m.equals("establish"))
			throw new RuntimeException("Method was not establish");
	}
}
