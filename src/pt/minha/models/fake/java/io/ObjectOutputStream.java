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

package pt.minha.models.fake.java.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

import pt.minha.models.local.io.ObjectOutputStreamImpl;
import pt.minha.models.local.io.OutputStreamInterface;

public class ObjectOutputStream extends DataOutputStream implements ObjectOutput {
	private OutputStream out;
	private ObjectOutputStreamImpl osi;

	public ObjectOutputStream(OutputStream os) throws IOException {
		super(os);
		
		this.out = os;
		this.osi = new ObjectOutputStreamImpl(new OutputStreamInterface() {
			public void intfWrite(int b) throws IOException {
				out.write(b);
			}
		}, this);
	}

	@Override
	public void writeObject(Object o) throws IOException {
		osi.implWriteObject(o);
	}

	@Override
	public void write(int b) throws IOException {
		osi.implWrite(b);
	}
}
