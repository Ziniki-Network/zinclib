package org.zincapi.peer.ssl;

import java.nio.channels.SelectionKey;

public interface ZincSSLConnectSelected {
	void canConnect(SelectionKey next) throws Exception;
}
