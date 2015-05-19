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

package pt.minha.models.fake.java.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

import pt.minha.models.local.io.ObjectOutputStreamImpl;
import pt.minha.models.local.io.OutputStreamInterface;

public class ObjectOutputStream extends OutputStream implements ObjectOutput {
	private DataOutputStream out;
	private ObjectOutputStreamImpl osi;

	public ObjectOutputStream(OutputStream os) throws IOException {
		super();
		
		this.out = new DataOutputStream(os);
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
	
	public void defaultWriteObject() throws IOException {
		osi.implDefaultWriteObject();
	}
	
	public void reset() throws IOException {
		osi.implReset();
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		out.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException {
		out.writeByte(v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		out.writeShort(v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		out.writeChar(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		out.writeLong(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		out.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		out.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		out.writeDouble(v);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		out.writeBytes(s);
	}

	@Override
	public void writeChars(String s) throws IOException {
		out.writeChars(s);
	}

	@Override
	public void writeUTF(String s) throws IOException {
		out.writeUTF(s);
	}

	@Override
	public void close() throws IOException {
        flush();
        osi.close();
        out.close();
	}
}
