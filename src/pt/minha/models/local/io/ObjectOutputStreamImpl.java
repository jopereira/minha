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

package pt.minha.models.local.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class ObjectOutputStreamImpl extends OutputStream {
	private OutputStreamInterface osi;
	private ObjectOutputStream oos;

	public ObjectOutputStreamImpl(OutputStreamInterface osi, Object wrapper) throws IOException {
		this.osi = osi;
		this.oos = new WrappedObjectOutputStream(this, wrapper);
	}
	
	public void implWrite(int b) throws IOException {
		oos.write(b);
	}

	public void implWriteObject(Object o) throws IOException {
		oos.writeObject(o);
	}

	@Override
	public void write(int b) throws IOException {
		osi.intfWrite(b);
	}

	public void implDefaultWriteObject() throws IOException {
		oos.defaultWriteObject();
	}
}
