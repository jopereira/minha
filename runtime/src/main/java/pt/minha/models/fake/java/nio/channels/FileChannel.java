package pt.minha.models.fake.java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

public abstract class FileChannel extends AbstractInterruptibleChannel implements ByteChannel, GatheringByteChannel, ReadableByteChannel, ScatteringByteChannel, WritableByteChannel {
	public abstract FileLock tryLock(long offet, long len, boolean shared); 
	public abstract long position() throws IOException;
	public abstract int write(ByteBuffer src, long position) throws IOException;
	public abstract void force(boolean metaData) throws IOException;
}
