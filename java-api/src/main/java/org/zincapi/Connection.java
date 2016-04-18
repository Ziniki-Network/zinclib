package org.zincapi;

public interface Connection {
	int nextHandle(MakeRequest r);

	void close();

	void reducePending();
}
