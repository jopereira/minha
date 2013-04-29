package pt.minha.models.global;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultHolder {
	
	private static Logger logger = LoggerFactory.getLogger("pt.minha.API");

	private Object result;
	private Throwable exception;
	private boolean done;
	private boolean async;
	
	public synchronized void reportReturn(Object result) {
		this.result = result;
		done = true;
		notifyAll();
		if (async) ignore();
	}

	public synchronized void reportException(Throwable exception) {
		this.exception = exception;
		done = true;
		notifyAll();
		if (async) ignore();
	}
	
	public synchronized Object getResult() throws Throwable {
		while(!done)
			wait();
		if (exception != null)
			throw exception;
		return result;
	}
	
	public Object getFakeResult(Class<?> type) throws Throwable {
		ignore();
		
		if (!type.isPrimitive() || type.equals(Void.TYPE))
			return null;
		else if (type.equals(Boolean.TYPE))
			return Boolean.valueOf(false);
		else if (type.equals(Integer.TYPE))
			return Integer.valueOf(0);
		else if (type.equals(Long.TYPE))
			return Long.valueOf(0);
		else if (type.equals(Float.TYPE))
			return Float.valueOf(0f);
		else if (type.equals(Double.TYPE))
			return Double.valueOf(0f);
		else if (type.equals(Byte.TYPE))
			return Byte.valueOf((byte)0);
		return null;
	}

	private synchronized void ignore() {
		async = true;
		if (exception != null)
			logger.error("uncaught exception on entry/exit", exception);
		else if (done && result != null)
			logger.warn("ignored result on entry/exit", result);
	}
	
	public synchronized boolean isIgnored() {
		return async;
	}
	
	public synchronized boolean isComplete() {
		return done;
	}
}	