package pt.minha.models.local.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

import pt.minha.models.fake.java.nio.channels.FileChannel;
import pt.minha.models.fake.java.nio.channels.FileLock;
import pt.minha.models.global.disk.Storage;

public class FileChannelImpl extends FileChannel {
	private java.nio.channels.FileChannel impl;
	private Storage storage;
	private long syncTarget;

	public FileChannelImpl(){}

	public FileChannelImpl(java.nio.channels.FileChannel fc){
		this.impl = fc;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return this.impl.read(dst);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return this.impl.write(src);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		return this.impl.write(srcs, offset, length);
	}

	@Override
	public long write(ByteBuffer[] srcs) throws IOException {
		return this.impl.write(srcs);
	}

	public int write(ByteBuffer src, long position) throws IOException {
		return this.impl.write(src,position);
	}

	@Override
	public void force(boolean metaData) throws IOException{
		this.impl.force(metaData);
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return this.impl.read(dsts, offset, length);
	}

	@Override
	public long read(ByteBuffer[] dsts) throws IOException {
		return this.impl.read(dsts);
	}

	@Override
	public FileLock tryLock(long offet, long len, boolean shared) {
		return new FileLock(this);
	}

	@Override
	protected void implCloseChannel() throws IOException {
		this.impl.close();
	}

	@Override
	public long position() throws IOException{
		return this.impl.position();
	}

}
