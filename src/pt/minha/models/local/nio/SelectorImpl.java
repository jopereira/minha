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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

class SelectorImpl extends AbstractSelector {
	protected SelectorImpl(SelectorProvider provider) {
		super(provider);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void implCloseSelector() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected SelectionKey register(AbstractSelectableChannel ch, int ops,
			Object att) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SelectionKey> keys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SelectionKey> selectedKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int selectNow() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int select(long timeout) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int select() throws IOException {
		return select(0);
	}

	@Override
	public Selector wakeup() {
		// TODO Auto-generated method stub
		return null;
	}
}