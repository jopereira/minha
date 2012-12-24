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

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

class SelectionKeyImpl extends AbstractSelectionKey {

	@Override
	public SelectableChannel channel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Selector selector() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int interestOps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SelectionKey interestOps(int ops) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int readyOps() {
		// TODO Auto-generated method stub
		return 0;
	}
}
