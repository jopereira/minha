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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileInputStream extends InputStream {
	private final java.io.FileInputStream impl;

	public FileInputStream(String name) throws FileNotFoundException {
		this.impl = new java.io.FileInputStream(name);
    }

	public FileInputStream(File file) throws FileNotFoundException {
		this(file.getAbsolutePath());
	}
	
	public int read() throws IOException {
		return this.impl.read();
	}

	public int read(byte b[]) throws IOException {
        return this.impl.read(b);
    }
	
	public int read(byte b[], int off, int len) throws IOException {
        return this.impl.read(b, off, len);
    }
	
	public void close() throws IOException {
		this.impl.close();
	}
}
