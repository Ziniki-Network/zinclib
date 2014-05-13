package org.zincapi.jsonapi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class PayloadItem {
	private final Map<String, Object> fields = new HashMap<String, Object>();

	PayloadItem(Payload payload, String type) {
		// TODO Auto-generated constructor stub
	}

	public PayloadItem(JSONObject object) throws JSONException {
		@SuppressWarnings("unchecked")
		Iterator<String> it = object.keys();
		while (it.hasNext()) {
			String s = it.next();
			fields.put(s, object.get(s));
		}
	}

	public void set(String field, Object val) {
		fields.put(field, val);
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
