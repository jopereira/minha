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
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

public class SelectorProviderImpl extends SelectorProvider {

	@Override
	public DatagramChannel openDatagramChannel() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pipe openPipe() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractSelector openSelector() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerSocketChannel openServerSocketChannel() throws IOException {
		return new ServerSocketChannelImpl(this);
	}

	@Override
	public SocketChannel openSocketChannel() throws IOException {
		return new SocketChannelImpl(this);
	}
}