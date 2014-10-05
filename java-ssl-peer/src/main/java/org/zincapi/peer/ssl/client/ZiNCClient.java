package org.zincapi.peer.ssl.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

import org.zincapi.Connection;
import org.zincapi.Requestor;
import org.zincapi.Zinc;
import org.zincapi.concrete.ConcreteConnection;
import org.zincapi.concrete.ConcreteRequestor;
import org.zincapi.peer.ssl.ZincSSLParticipant;
import org.zinutils.exceptions.UtilException;
import org.zinutils.sync.Promise;

public class ZiNCClient implements Zinc.Client {
	private final ZincSSL zinc;

	public ZiNCClient(Zinc z) {
		zinc = (ZincSSL) z;
	}
	
	@Override
	public Connection createConnection(URI url) throws IOException {
		String host = url.getHost();
		int port = url.getPort();
		Promise<ZincSSLParticipant> client = zinc.createClient(new InetSocketAddress(host, port));
		try {
			ZincSSLParticipant cli = client.lazyget();
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
		return new ConcreteRequestor((ConcreteConnection) conn);
	}

}
