package org.zincapi.peer.ssl;

import java.nio.channels.SelectionKey;

public interface ZincSSLReadSelected {
	void canRead(SelectionKey next) throws Exception;
}
