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

package pt.minha.models.fake.java.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

import pt.minha.models.local.io.InputStreamInterface;
import pt.minha.models.local.io.ObjectInputStreamImpl;

public class ObjectInputStream extends DataInputStream implements ObjectInput {
	private InputStream in;
	private ObjectInputStreamImpl isi;

	public ObjectInputStream(InputStream is) throws IOException {
		super(is);
		this.in = is;
		this.isi = new ObjectInputStreamImpl(new InputStreamInterface() {			
			public int intfRead() throws IOException {
				return in.read();
			}
		}, this);
	}
	

	@Override
	public Object readObject() throws ClassNotFoundException, IOException {
		return isi.implReadObject();
	}

	@Override
	public int read() throws IOException {
		return isi.implRead();
	}
	
	public void defaultReadObject() throws IOException, ClassNotFoundException {
		isi.implDefaultReadObject();
	}
}
