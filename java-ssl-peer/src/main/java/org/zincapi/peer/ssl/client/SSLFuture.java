package org.zincapi.peer.ssl.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.zincapi.peer.ssl.ZincSSLParticipant;

public class SSLFuture implements Future<ZincSSLParticipant>{
	private boolean done;
	private ZincSSLParticipant ret;
	private Throwable ex;
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public ZincSSLParticipant get() throws InterruptedException, ExecutionException {
		synchronized (this) {
			while (!done)
				this.wait();
			if (ex != null)
				throw new ExecutionException(ex);
			return ret;
		}
	}

	@Override
	public ZincSSLParticipant get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		synchronized (this) {
			if (!done) {
				long mswait = unit.convert(timeout, TimeUnit.MILLISECONDS);
				this.wait(mswait);
			}
			if (ex != null)
				throw new ExecutionException(ex);
			return ret;
		}
	}

	public void completed(ZincSSLParticipant cli) {
		synchronized (this) {
			this.ret = cli;
			this.done = true;
			this.notifyAll();
		}
	}

	public void failed(Throwable ex) {
		synchronized (this) {
			this.ex = ex;
			this.done = true;
			this.notifyAll();
		}
	}

}
