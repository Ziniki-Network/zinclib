package org.zincapi.peer.ssl;

import java.nio.channels.SelectionKey;

public interface ZincSSLAcceptSelected {
	void canAccept(SelectionKey next) throws Exception;
}
