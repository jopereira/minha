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

import pt.minha.models.fake.java.net.DatagramSocket;
import pt.minha.models.fake.java.nio.channels.spi.AbstractSelectableChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;

public abstract class DatagramChannel extends AbstractSelectableChannel implements ByteChannel {

	protected DatagramChannel(SelectorProvider provider) {
		super(provider);
	}
	
	public static DatagramChannel open() throws IOException { return SelectorProvider.provider().openDatagramChannel(); }

	public abstract DatagramSocket socket();
	
	public abstract DatagramChannel connect(SocketAddress address);

	public abstract DatagramChannel disconnect();
	
	public abstract boolean isConnected();

	@Override
	public int validOps() {
		return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
	}
}
