package pt.minha.models.global;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultHolder {
	
	private static Logger logger = LoggerFactory.getLogger("pt.minha.API");

	private Method method;
	private Object result;
	private Throwable exception;
	private boolean done, async, ignored;
	
	public ResultHolder(Method method) {
		this.method = method;
	}
	
	public synchronized void reportReturn(Object result) {
		this.result = result;
		done = true;
		notifyAll();
		if (ignored) setIgnored();
	}

	public synchronized void reportException(Throwable exception) {
		this.exception = exception;
		done = true;
		notifyAll();
		if (ignored) setIgnored();
	}
	
	public synchronized boolean isComplete() {
		return done;
	}
	
	public synchronized Object getResult() throws Throwable {
		while(!done)
			wait();
		if (exception != null)
			throw exception;
		return result;
	}
	
	public synchronized Object getFakeResult() throws Throwable {
		Class<?> type = method.getReturnType();
		
		async = true;
		
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
	
	public synchronized void setSync() {
		if (done)
			throw new IllegalArgumentException("cannot wait on finished invocation");
		async = false;
	}

	public synchronized void setIgnored() {
		ignored = true;
		if (exception != null)
			logger.error("uncaught exception on entry/exit {}", method, exception);
		else if (done && result != null)
			logger.warn("ignored result on entry/exit to {}", method);
	}
		
	public synchronized boolean isIgnored() {
		return async;
	}	
}