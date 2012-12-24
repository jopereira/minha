/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2012, Universidade do Minho.
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

package pt.minha.models.local.nio;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

class SocketChannelImpl extends SocketChannel {

	protected SocketChannelImpl(SelectorProvider provider) {
		super(provider);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Socket socket() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConnectionPending() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect(SocketAddress remote) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean finishConnect() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void implConfigureBlocking(boolean block) throws IOException {
		// TODO Auto-generated method stub
		
	}
}