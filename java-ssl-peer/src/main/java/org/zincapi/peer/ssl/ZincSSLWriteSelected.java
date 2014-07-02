package org.zincapi.peer.ssl;

import java.nio.channels.SelectionKey;

public interface ZincSSLWriteSelected {
	void canWrite(SelectionKey next) throws Exception;
}
