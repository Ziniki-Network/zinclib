package org.zincapi;

public interface ResourceHandler {

	void handle(HandleRequest hr, Response response) throws Exception;

}
