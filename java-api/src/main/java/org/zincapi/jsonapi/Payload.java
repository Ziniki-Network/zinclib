package org.zincapi.jsonapi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.ZincInvalidPayloadException;
import org.zincapi.ZincNotSingletonException;
import org.zincapi.ZincPayloadReadonlyException;

public class Payload {
	private final boolean creating;

	private final String type;
	private final Set<PayloadItem> items = new HashSet<PayloadItem>();

	public Payload(JSONObject jsonObject) throws JSONException {
		this.creating = false;
		@SuppressWarnings("unchecked")
		Iterator<String> it = jsonObject.keys();
		String ty = null;
		while (it.hasNext()) {
			String s = it.next();
			// TODO: handle meta, links and linked
			// else {
			ty = s;
			JSONArray objs = jsonObject.getJSONArray(s);
			for (int i=0;i<objs.length();i++)
				items.add(new PayloadItem(objs.getJSONObject(i)));
			// }
		}
		if (ty == null)
			throw new ZincInvalidPayloadException();
		this.type = ty;
	}
	
	public Payload(String type) {
		this.creating = true;
		this.type = type;
	}

	public PayloadItem newItem() {
		if (!creating)
			throw new ZincPayloadReadonlyException();
		PayloadItem ret = new PayloadItem(this, type);
		items.add(ret);
		return ret;
	}

	public PayloadItem assertSingle(String ty) {
		if (!type.equals(ty) || items.size() != 1)
			throw new ZincNotSingletonException(ty, type, items.size());
		return items.iterator().next();
	}

	public JSONObject asJSONObject() throws JSONException {
		JSONObject ret = new JSONObject();
		JSONArray mainArray = new JSONArray();
		ret.put(type, mainArray);
		for (PayloadItem pi : items)
			mainArray.put(pi.asJSONObject());
		return ret;
	}
}
