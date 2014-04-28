package org.zincapi;

public interface ConnectionHandler {
	void newConnection(HandleRequest request, String type, String uri);
}
