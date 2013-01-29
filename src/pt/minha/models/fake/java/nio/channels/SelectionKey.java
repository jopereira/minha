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

public abstract class SelectionKey {

	public static final int OP_READ = 1;
	public static final int OP_WRITE = 4;
	public static final int OP_CONNECT = 8;
	public static final int OP_ACCEPT = 16;

	public abstract Object attach(Object attachment);

	public abstract Object attachment();

	public abstract Selector selector();

	public abstract SelectableChannel channel();

	public abstract int interestOps();

	public abstract SelectionKey interestOps(int ops);

	public abstract int readyOps();
	
	public abstract void cancel();
	
	public abstract boolean isValid();
	public abstract boolean isReadable();
	public abstract boolean isWritable();
	public abstract boolean isAcceptable();
	public abstract boolean isConnectable();

}
