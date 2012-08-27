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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class FileOutputStream extends OutputStream {
	private final java.io.FileOutputStream impl;
	
	public FileOutputStream(String name, boolean append) throws FileNotFoundException {
		this.impl = new java.io.FileOutputStream(name, append);
	}
	
	public FileOutputStream(String name) throws FileNotFoundException {
		this(name, false);
	}
	
	public FileOutputStream(File file, boolean append) throws FileNotFoundException {
		this(file.getAbsolutePath(), append);
	}

	public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

	public void write(int b) throws IOException {
		this.impl.write(b);
	}
	
	public void write(byte b[]) throws IOException {
        this.impl.write(b, 0, b.length);
    }
	
	public void write(byte b[], int off, int len) throws IOException {
		this.impl.write(b, off, len);
	}
	
	public void close() throws IOException {
		this.impl.close();
	}
}
