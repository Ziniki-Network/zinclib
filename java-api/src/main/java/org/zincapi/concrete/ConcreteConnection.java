package org.zincapi.concrete;

import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONObject;
import org.zincapi.Connection;
import org.zincapi.MakeRequest;

public abstract class ConcreteConnection implements Connection {
	private int handle;
	private final Map<Integer, MakeRequest> mapping = new TreeMap<Integer, MakeRequest>();

	@Override
	public synchronized int nextHandle(MakeRequest r) {
		int ret = ++handle;
		mapping.put(ret, r);
		return ret;
	}
	
	protected void handleResponse(String s) {
		try {
			JSONObject json = new JSONObject(s);
			int sub = json.getInt("subscription");
			MakeRequest r;
			synchronized (this) {
				r = mapping.get(sub);
			}
			if (r != null)
				((ConcreteMakeRequest)r).handler.response(r, json.getJSONObject("payload"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
