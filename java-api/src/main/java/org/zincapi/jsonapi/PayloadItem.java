package org.zincapi.jsonapi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class PayloadItem {
	private final Map<String, Object> fields = new HashMap<String, Object>();
	private Payload payload;

	PayloadItem(Payload payload, String type) {
		this.payload = payload;
	}

	public PayloadItem(JSONObject object) throws JSONException {
		@SuppressWarnings("unchecked")
		Iterator<String> it = object.keys();
		while (it.hasNext()) {
			String s = it.next();
			fields.put(s, object.get(s));
		}
	}

	public Payload getPayload() {
		return payload;
	}

	public PayloadItem set(String field, Object val) {
		fields.put(field, val);
		return this;
	}

	public Set<String> keys() {
		return fields.keySet();
	}
	
	public Object get(String s) {
		return fields.get(s);
	}

	public String getString(String s) {
		return (String) fields.get(s);
	}

	public JSONObject asJSONObject() throws JSONException {
		JSONObject ret = new JSONObject();
		for (Entry<String, Object> f : fields.entrySet()) {
			ret.put(f.getKey(), f.getValue());
		}
		return ret;
	}

}
