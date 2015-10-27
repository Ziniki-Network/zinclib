package org.zincapi.jsonapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.zincapi.ZincInvalidPayloadException;
import org.zincapi.ZincInvalidSideloadException;
import org.zincapi.ZincNotSingletonException;
import org.zincapi.ZincPayloadReadonlyException;
import org.zinutils.collections.ListMap;

public class Payload {
	private final boolean creating;

	private final String type;
	private final List<PayloadItem> items = new ArrayList<PayloadItem>();
	private final ListMap<String, PayloadItem> sideload = new ListMap<String, PayloadItem>();

	public Payload(JSONObject jsonObject) throws JSONException {
		this.creating = false;
		@SuppressWarnings("unchecked")
		Iterator<String> it = jsonObject.keys();
		String ty = null;
		while (it.hasNext()) {
			String s = it.next();
			if (s.equals("_main")) {
				if (ty != null)
					throw new ZincInvalidPayloadException();
				ty = jsonObject.getString(s);
			} else {
				JSONArray objs = jsonObject.getJSONArray(s);
				if (ty == null) {
					ty = s;
					for (int i=0;i<objs.length();i++)
						items.add(new PayloadItem(objs.getJSONObject(i)));
				} else {
					for (int i=0;i<objs.length();i++)
						sideload.add(s, new PayloadItem(objs.getJSONObject(i)));
				}
			}
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

	public PayloadItem sideload(String sltype) {
		if (!creating)
			throw new ZincPayloadReadonlyException();
		if (sltype == null || sltype.equals(type))
			throw new ZincInvalidSideloadException(sltype, type);
		PayloadItem ret = new PayloadItem(this, sltype);
		sideload.add(sltype, ret);
		return ret;
	}

	public Payload addJSONItem(JSONObject json) throws JSONException {
		PayloadItem toAdd = newItem();
		@SuppressWarnings("unchecked")
		Iterator<String> it = json.keys();
		while (it.hasNext()) {
			String s = it.next();
			toAdd.set(s, json.get(s));
		}
		return this;
	}

	public String getType() {
		return type;
	}

	public PayloadItem assertSingle(String ty) {
		if (!type.equals(ty) || items.size() != 1)
			throw new ZincNotSingletonException(ty, type, items.size());
		return items.iterator().next();
	}

	public JSONObject asJSONObject() throws JSONException {
		JSONObject ret = new JSONObject();
		if (!sideload.isEmpty())
			ret.put("_main", type);
		JSONArray mainArray = new JSONArray();
		ret.put(type, mainArray);
		for (PayloadItem pi : items)
			mainArray.put(pi.asJSONObject());
		for (String s : sideload) {
			JSONArray sideArray = new JSONArray();
			ret.put(s, sideArray);
			for (PayloadItem pi : sideload.get(s))
				sideArray.put(pi.asJSONObject());
		}
		return ret;
	}

	public Collection<PayloadItem> items() {
		return items;
	}
	
	public Collection<String> sideloads() {
		return sideload.keySet();
	}
	
	public Collection<PayloadItem> sideloaded(String oftype) {
		return sideload.get(oftype);
	}
}
