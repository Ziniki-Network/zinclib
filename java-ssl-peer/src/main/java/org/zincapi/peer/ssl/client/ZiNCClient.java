package org.zincapi.peer.ssl.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Future;

import org.zincapi.Connection;
import org.zincapi.LifecycleHandler;
import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zincapi.concrete.ConcreteConnection;
import org.zincapi.peer.ssl.ZincSSLParticipant;
import org.zinutils.exceptions.UtilException;

public class ZiNCClient implements Zinc.Client {
	private final ZincSSL zinc;

	public ZiNCClient(Zinc z) {
		zinc = (ZincSSL) z;
	}
	
	@Override
	public Connection createConnection(URI url, LifecycleHandler lifecycleHandler) throws IOException {
		String host = url.getHost();
		int port = url.getPort();
		Future<ZincSSLParticipant> client = zinc.createClient(new InetSocketAddress(host, port));
		try {
			ZincSSLParticipant cli = client.get();
			return cli.conn;
		} catch (Exception ex) {
			if (ex.getCause() instanceof IOException)
				throw (IOException)ex.getCause();
			else
				throw UtilException.wrap(ex.getCause());
		}
	}

	@Override
	public Requestor requestor(Connection conn) {
		return ((ConcreteConnection) conn).newRequestor();
	}

}
