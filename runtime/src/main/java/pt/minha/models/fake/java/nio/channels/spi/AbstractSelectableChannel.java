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

package pt.minha.models.fake.java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.IllegalSelectorException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import pt.minha.models.fake.java.nio.channels.SelectableChannel;
import pt.minha.models.fake.java.nio.channels.SelectionKey;
import pt.minha.models.fake.java.nio.channels.Selector;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.local.nio.SelectorImpl;

public abstract class AbstractSelectableChannel extends SelectableChannel {

	private SelectorProvider provider;
	private boolean blocking;
	private Map<Selector,SelectionKey> keys;

	public AbstractSelectableChannel(SelectorProvider provider) {
		this.provider = provider;
		keys = new HashMap<Selector,SelectionKey>();
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
	public SelectionKey register(Selector selector, int operation, Object attachment) throws IOException {
		if (isBlocking())
			throw new IllegalBlockingModeException();
		if (provider() != selector.provider())
			throw new IllegalSelectorException();
		SelectionKey key = ((SelectorImpl)selector).register(this, operation, attachment);
		keys.put(selector,key);
		return key;
	}
	
	@Override
	public SelectionKey keyFor(Selector selector) {
		return keys.get(selector);
	}
	
	@Override
	protected void implCloseChannel() throws IOException {
		for(SelectionKey key: keys.values())
			key.cancel();
	}
	
	public abstract BlockingHelper helperFor(int op);	
}
