package org.zincapi;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.zincapi.concrete.ConcreteHandleRequest;
import org.zincapi.concrete.SegmentHandler;

public class MatchState {
	Map<String, SegmentHandler> handlers;
	private Map<String, String> params = new TreeMap<String, String>();
	private SegmentHandler matched;

	public MatchState(Map<String, SegmentHandler> handlers) {
		this.handlers = handlers;
	}

	public MatchState matched(String seg, SegmentHandler sh) {
		MatchState ret = new MatchState(sh.handlers);
		ret.matched = sh;
		ret.params.putAll(params);
		if (seg != null)
			ret.setParameter(sh.handleAnyAs, seg);
		return ret;
	}

	private void setParameter(String handleAnyAs, String seg) {
		params.put(handleAnyAs, seg);
	}
	
	public void bindParametersTo(ConcreteHandleRequest hr) {
		for (Entry<String, String> p : params.entrySet())
			hr.setResourceParameter(p.getKey(), p.getValue());
	}

	public ResourceHandler handler() {
		return matched.handler();
	}

}
