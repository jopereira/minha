/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2014, Universidade do Minho.
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
import java.util.Set;

import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;

public abstract class Selector {
	
	public static Selector open() throws IOException {
		return SelectorProvider.provider().openSelector();
	}

	public abstract Set<SelectionKey> keys();

	public abstract Set<SelectionKey> selectedKeys();

	public abstract int selectNow() throws IOException;
	
	public abstract int select(long timeout) throws IOException;
	
	public abstract int select() throws IOException;

	public abstract Selector wakeup();

	public abstract SelectorProvider provider();
	
	public abstract void close() throws IOException;
}
