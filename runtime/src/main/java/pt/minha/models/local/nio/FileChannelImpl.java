package pt.minha.models.local.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

import pt.minha.models.fake.java.nio.channels.FileChannel;
import pt.minha.models.fake.java.nio.channels.FileLock;

public class FileChannelImpl extends FileChannel {

	@Override
	public int read(ByteBuffer dst) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long write(ByteBuffer[] srcs) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long read(ByteBuffer[] dsts) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FileLock tryLock(long offet, long len, boolean shared) {
		return new FileLock(this);
	}

	@Override
	protected void implCloseChannel() throws IOException {
	}
}
