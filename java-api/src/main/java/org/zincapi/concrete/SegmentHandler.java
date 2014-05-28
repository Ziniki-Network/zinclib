package org.zincapi.concrete;

import java.util.Map;
import java.util.TreeMap;

import org.zincapi.ResourceHandler;
import org.zincapi.ZincDuplicateResourceException;

public class SegmentHandler {
	public final String handleAnyAs;
	public final Map<String, SegmentHandler> handlers = new TreeMap<String, SegmentHandler>();
	private ResourceHandler handler;

	public SegmentHandler(String handleAnyAs) {
		this.handleAnyAs = handleAnyAs;
	}

	public void setHandler(String resource, ResourceHandler handler) {
		if (this.handler != null)
			throw new ZincDuplicateResourceException(resource);
		this.handler = handler;
	}

	public ResourceHandler handler() {
		return handler;
	}
}
