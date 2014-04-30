package org.zincapi.concrete;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zincapi.Connection;
import org.zincapi.MakeRequest;
import org.zincapi.Requestor;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;
import org.zincapi.Zinc;
import org.zincapi.ZincInvalidSubscriptionException;
import org.zincapi.ZincNoSubscriptionException;

public abstract class ConcreteConnection implements Connection {
	private final static Logger logger = LoggerFactory.getLogger("Connection");
	protected final Zinc zinc;
	private int handle;
	private String remoteURI;
	private final Map<Integer, MakeRequest> mapping = new TreeMap<Integer, MakeRequest>();
	private final Map<Integer, Response> subscriptions = new HashMap<Integer, Response>();

	public ConcreteConnection(Zinc zinc) {
		this.zinc = zinc;
	}

	@Override
	public synchronized int nextHandle(MakeRequest r) {
		int ret = ++handle;
		mapping.put(ret, r);
		return ret;
	}
	
	protected void handleMessage(String s) {
		try {
			logger.info("Handling message " + s);
			JSONObject json = new JSONObject(s);
			handleMessage(json);
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}

	protected void handleMessage(JSONObject json) {
		try {
			if (json.has("request")) {
				JSONObject req = json.getJSONObject("request");
				String method = req.getString("method");
				if (method.equals("unsubscribe")) {
					if (!json.has("subscription"))
						throw new ZincNoSubscriptionException();
					int sub = json.getInt("subscription");
					if (!subscriptions.containsKey(sub))
						throw new ZincInvalidSubscriptionException(sub);
					Response deadResponse = subscriptions.remove(sub);
					deadResponse.unsubscribed();
					return;
				}
				String resource = null;
				if (req.has("resource"))
					resource = req.getString("resource");
				ConcreteHandleRequest hr = new ConcreteHandleRequest(this, method);
				ResourceHandler handler = zinc.getHandler(hr, resource);
				if (req.has("options")) {
					JSONObject opts = req.getJSONObject("options");
					@SuppressWarnings("unchecked")
					Iterator<String> keys = opts.keys();
					while (keys.hasNext()) {
						String k = keys.next();
						hr.setOption(k, opts.get(k));
					}
				}
				if (json.has("payload"))
					hr.setPayload(json.getJSONObject("payload"));
				try {
					// May need more than this if we have a "subscription"
					Response response = null;
					if (json.has("subscription")) {
						int sub = json.getInt("subscription");
						response = new ConcreteResponse(this, sub);
						subscriptions.put(sub, response);
					}
					handler.handle(hr, response);
				} catch (Exception ex) {
					ex.printStackTrace();
					// TODO: send a "500" exception back
				}
			} else {
				int sub = json.getInt("subscription");
				MakeRequest r;
				synchronized (this) {
					r = mapping.get(sub);
				}
				if (r != null)
					((ConcreteMakeRequest)r).handler.response(r, json.getJSONObject("payload"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void setURI(String address) {
		remoteURI = address;
	}

	public String getURI() {
		return remoteURI;
	}

	public Requestor newRequestor() {
		return zinc.getClient().requestor(this);
	}
}
