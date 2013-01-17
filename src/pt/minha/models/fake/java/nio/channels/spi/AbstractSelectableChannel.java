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

package pt.minha.models.fake.java.nio.channels.spi;

import pt.minha.models.fake.java.nio.channels.SelectableChannel;
import pt.minha.models.fake.java.nio.channels.SelectionKey;
import pt.minha.models.fake.java.nio.channels.Selector;

public abstract class AbstractSelectableChannel extends SelectableChannel {

	private SelectorProvider provider;
	private boolean blocking;

	public AbstractSelectableChannel(SelectorProvider provider) {
		this.provider = provider;
	}

	@Override
	public boolean isBlocking() {
		return blocking;
	}

	@Override
	public SelectableChannel configureBlocking(boolean mode) {
		blocking = mode;
		return this;
	}

	@Override
	public SelectorProvider provider() {
		return provider;
	}

	@Override
	public SelectionKey register(Selector selector, int operation,
			Object attachment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SelectionKey register(Selector selector, int operation) {
		// TODO Auto-generated method stub
		return super.register(selector, operation);
	}
}
