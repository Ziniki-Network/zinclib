package org.zincapi;

public interface LifecycleHandler {
	public void onConnection(Connection conn);
	public void onReady(Connection conn);
	public void onError(Connection conn, Throwable ex);
	public void onDisconnection(Connection conn);
}
