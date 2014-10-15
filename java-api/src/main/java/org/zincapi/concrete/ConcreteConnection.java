package org.zincapi.concrete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONArray;
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
import org.zincapi.ZincException;
import org.zincapi.ZincInvalidSubscriptionException;
import org.zincapi.ZincNoResourceHandlerException;
import org.zincapi.ZincNoSubscriptionException;
import org.zincapi.jsonapi.Payload;
import org.zinutils.sync.Promise;

public abstract class ConcreteConnection implements Connection {
	private final static Logger logger = LoggerFactory.getLogger("Connection");
	protected final Zinc zinc;
	private int handle;
	private String remoteURI;
	private final Map<Integer, MakeRequest> mapping = new TreeMap<Integer, MakeRequest>();
	private final Map<Integer, Response> subscriptions = new HashMap<Integer, Response>();
	private final Map<Integer, Promise<String>> pendingPromises = new HashMap<Integer, Promise<String>>();

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
				} else if (method.equals("heartbeat")) {
					JSONObject hb = new JSONObject();
					hb.put("heartbeat", "");
					send(hb);
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
						Object v = opts.get(k);
						if (v instanceof JSONArray) {
							List<String> l = new ArrayList<String>();
							JSONArray a = (JSONArray) v;
							for (int i=0;i<a.length();i++)
								l.add(a.getString(i));
							v = l;
						}
						hr.setOption(k, v);
					}
				}
				if (json.has("payload"))
					hr.setPayload(new Payload(json.getJSONObject("payload")));
				Integer replyTo = null;
				String idField = null;
				Exception err = null;
				ConcreteResponse response = null;
				try {
					// May need more than this if we have a "subscription"
					if (json.has("subscription")) {
						int sub = json.getInt("subscription");
						replyTo = sub;
						response = new ConcreteResponse(this, sub);
						idField = "subscription";
						subscriptions.put(sub, response);
					} else if (json.has("requestid")) {
						replyTo = json.getInt("requestid");
						idField = "requestid";
					}
					handler.handle(hr, response);
				} catch (Exception ex) {
					err = ex;
					ex.printStackTrace();
				}
				if (response == null && replyTo != null)
					response = new ConcreteResponse(this, replyTo);
				if (response != null && !response.sent())
					response.sendStatus(idField, null, err);
			} else {
				if (json.has("subscription")) {
					int sub = json.getInt("subscription");
					handlePromise(sub, json);
					MakeRequest r;
					synchronized (this) {
						r = mapping.get(sub);
					}
					if (r != null) {
						if (json.has("payload")) {
							Payload payload = new Payload(json.getJSONObject("payload"));
							((ConcreteMakeRequest)r).handler.response(r, payload);
						}
					}
				} else if (json.has("requestid")) {
					handlePromise(json.getInt("requestid"), json);
				}
			}
		} catch (ZincNoResourceHandlerException ex) {
			logger.error(ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void handlePromise(int sub, JSONObject json) throws JSONException {
		if (!pendingPromises.containsKey(sub))
			return;
		Promise<String> pp = pendingPromises.remove(sub);
		if (json.has("error"))
			pp.failed(new ZincException(json.getString("error")));
		else if (json.has("status"))
			pp.completed(json.getString("status"));
		else
			pp.completed(null);
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
	
	public void pend(int h, Promise<String> ret) {
		pendingPromises.put(h, ret);
	}

	public abstract void send(JSONObject asJSON);
}
