package org.zinclib.testjj;

import org.zincapi.Connection;
import org.zincapi.LifecycleHandler;

public class TestingLifecycleHandler implements LifecycleHandler {
	public int connections;
	public int disconnections;
	public int errors;

	@Override
	public void onConnection(Connection conn) {
		System.out.println("Connection completed to " + conn);
	}
	
	@Override
	public void onError(Connection conn, Throwable ex) {
		System.out.println("Error detected on " + conn);
		errors++;
	}

	@Override
	public void onDisconnection(Connection conn) {
		System.out.println("Connection to " + conn + " terminated");
	}

}
