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

import pt.minha.models.fake.java.net.ServerSocket;
import pt.minha.models.fake.java.nio.channels.spi.AbstractSelectableChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;

public abstract class ServerSocketChannel extends AbstractSelectableChannel {
	protected ServerSocketChannel(SelectorProvider provider) {
		super(provider);
	}

	public static ServerSocketChannel open() throws IOException { return SelectorProvider.provider().openServerSocketChannel(); }


	@Override
	public int validOps() {
		return SelectionKey.OP_ACCEPT;
	}
	
	public abstract ServerSocket socket();
	
	public abstract SocketChannel accept() throws IOException;
}
