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
import org.zincapi.Channel;
import org.zincapi.Connection;
import org.zincapi.LifecycleHandler;
import org.zincapi.MakeRequest;
import org.zincapi.Requestor;
import org.zincapi.ResourceHandler;
import org.zincapi.Response;
import org.zincapi.Zinc;
import org.zincapi.ZincInvalidSubscriptionException;
import org.zincapi.ZincNoResourceHandlerException;
import org.zincapi.ZincNoSubscriptionException;
import org.zincapi.jsonapi.Payload;
import org.zinutils.exceptions.UtilException;

public abstract class ConcreteConnection implements Connection {
	protected final static Logger logger = LoggerFactory.getLogger("Connection");
	protected final Zinc zinc;
	private int handle;
	private int nextChannel = 1;
	private String remoteURI;
	private final Map<Integer, MakeRequest> mapping = new TreeMap<Integer, MakeRequest>();
	private final Map<Integer, ConcreteChannel> channels = new HashMap<Integer, ConcreteChannel>();
	private final Map<Integer, Response> subscriptions = new HashMap<Integer, Response>();
	private final Map<Integer, Object> pendingPromises = new HashMap<Integer, Object>();
	protected final List<LifecycleHandler> lcHandlers = new ArrayList<LifecycleHandler>();

	public ConcreteConnection(Zinc zinc) {
		this.zinc = zinc;
	}

	public void addLifecycleHandler(LifecycleHandler handler) {
		if (handler == null)
			return;
		this.lcHandlers.add(handler);
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

	protected void handleMessage(final JSONObject json) {
		try {
			if (json.has("request")) {
				final Integer replyTo;
				final String idField;
				final ConcreteResponse response;
				// May need more than this if we have a "subscription"
				if (json.has("subscription")) {
					int sub = json.getInt("subscription");
					replyTo = sub;
					idField = "subscription";
					response = new ConcreteResponse(this, true, sub);
					subscriptions.put(sub, response);
				} else if (json.has("requestid")) {
					replyTo = json.getInt("requestid");
					idField = "requestid";
					response = new ConcreteResponse(this, false, replyTo);
				} else {
					replyTo = null;
					idField = null;
					response = null;
				}
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
					send(hb, true);
					return;
				}
				String resource = null;
				if (req.has("resource"))
					resource = req.getString("resource");
				int channel = 0;
				if (req.has("channel"))
					channel = req.getInt("channel");
				final ConcreteHandleRequest hr = new ConcreteHandleRequest(this, channel, method);
				final ResourceHandler handler = zinc.getHandler(hr, resource);
				if (handler == null) {
					logger.error("Request for " + resource + " received; there is no handler for that");
					ConcreteResponse rs = response;
					if (response == null)
						rs = new ConcreteResponse(ConcreteConnection.this, false, replyTo);
					rs.sendStatus(idField, null, "There is no handler for " + resource);
					return;
				}
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
				zinc.submit(new Runnable() {
					public void run() {
						Throwable err = null;
						try {
							handler.handle(hr, response);
						} catch (Exception ex) {
							err = UtilException.unwrap(ex);
							logger.error("Encountered exception", err);
						}
						ConcreteResponse rs2 = response;
						if (rs2 == null && replyTo != null)
							rs2 = new ConcreteResponse(ConcreteConnection.this, false, replyTo);
						if (rs2 != null && !rs2.sent())
							try {
								rs2.sendStatus(idField, null, err);
							} catch (Exception ex) {
								logger.error("Error sending status", ex);
							}
					}
				});
			} else {
				if (json.has("subscription")) {
					int sub = json.getInt("subscription");
					handlePromise(sub, json);
					final MakeRequest r;
					synchronized (this) {
						r = mapping.get(sub);
					}
					if (r != null) {
						zinc.submit(new Runnable() {
							@Override
							public void run() {
								try {
									if (json.has("payload")) {
										Payload payload = new Payload(json.getJSONObject("payload"));
										((ConcreteMakeRequest)r).handler.response(r, payload);
									} else if (json.has("error")) {
										((ConcreteMakeRequest)r).handler.error(r, json.getString("error"));
									}
								} catch (Exception ex) {
									logger.error("Error handling subscription", ex);
								}
							}
						});
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
		/* Promise<String> pp = */ pendingPromises.remove(sub);
		/*
		if (json.has("error"))
			pp.failed(new ZincException(json.getString("error")));
		else if (json.has("status"))
			pp.completed(json.getString("status"));
		else
			pp.completed(null);
			*/
	}

	public Channel getChannel(int channel) {
		if (!channels.containsKey(channel))
			channels.put(channel, new ConcreteChannel(this, channel));
		return channels.get(channel);
	}

	public void setURI(String address) {
		remoteURI = address;
	}

	public String getURI() {
		return remoteURI;
	}

	public Requestor newRequestor() {
		return new ConcreteRequestor(this, nextChannel++);
	}
	
	public void pend(int h, Object promise) {
		pendingPromises.put(h, promise);
	}

	public void send(JSONObject jsonObject) {
		send(jsonObject, false);
	}

	public abstract void send(JSONObject asJSON, boolean overridePending);
}
