/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2013, Universidade do Minho.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package pt.minha.models.fake.java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;

import pt.minha.models.fake.java.net.Socket;
import pt.minha.models.fake.java.nio.channels.spi.AbstractSelectableChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;

public abstract class SocketChannel extends AbstractSelectableChannel implements ByteChannel, GatheringByteChannel {

	protected SocketChannel(SelectorProvider provider) {
		super(provider);
	}
	
	public static SocketChannel open() throws IOException { return SelectorProvider.provider().openSocketChannel(); }

	public abstract Socket socket();

	public abstract boolean isConnected();

	public abstract boolean isConnectionPending();

	public abstract boolean connect(SocketAddress remote) throws IOException;
	
	public abstract boolean finishConnect() throws IOException;

	@Override
	public int validOps() {
		return SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE;
	}
}
