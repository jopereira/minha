package pt.minha.models.global.io;

public class Buffer {
	private static final int bufferSize = 32*1024;

	private final byte[] buffer = new byte[bufferSize];
	private int h, t, n;
	private boolean closed;

	public int push(byte[] data, int offset, int len) {
		if (closed)
			return -1;
		
		int done = 0;
				
		while(len-done>0 && n<bufferSize) {
			int op = len;
			if (op > bufferSize-n)
				op = bufferSize-n;
			if (op > bufferSize-h)
				op = bufferSize-h;
			System.arraycopy(data, offset+done, buffer, h, op);
			done += op;
			n += op;
			h = (h + op) % bufferSize;
		}
		
		return done;
	}
	
	public int pop(byte[] data, int offset, int len) {
		if (closed && n==0)
			return -1;
		
		int done = 0;

		while(len-done>0 && n>0) {
			int op = len;
			if (op > n)
				op = n;
			if (op > bufferSize-t)
				op = bufferSize-t;
			System.arraycopy(buffer, t, data, offset+done, op);
			done += op;
			n -= op;
			t = (t + op) % bufferSize;
		}
		
		return done;
	}
	
	public int getUsed() {
		return n;
	}

	public int getFree() {
		return bufferSize-n;
	}
	
	public void close(boolean linger) {
		if (!linger)
			n = 0;
		
		closed = true;
	}
	
	public boolean isClosing() {
		return closed;
	}

	public boolean isClosed() {
		return closed && n==0;
	}
}
