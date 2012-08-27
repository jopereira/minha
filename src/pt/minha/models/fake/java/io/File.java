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

import java.net.URI;
import java.net.URISyntaxException;

public class File {
	private final String path;
	private final java.io.File impl;
	
	public static final char separatorChar     = java.io.File.separatorChar;
	public static final String separator       = java.io.File.separator;
	public static final char pathSeparatorChar = java.io.File.pathSeparatorChar;
	public static final String pathSeparator   = java.io.File.pathSeparator;
	
	public File(String path) {
		this.path = path;
		this.impl = new java.io.File(this.path);
	}
	
	public String getAbsolutePath() {
		return path;
	}
	
	public boolean isFile() {
		return this.impl.isFile();
	}

	public boolean isDirectory() {
		return this.impl.isDirectory();
	}
	
	public boolean delete() {
		return this.impl.delete();
	}
	
	public String[] list() {
		return this.impl.list();
	}
	
	public boolean exists() {
		return this.impl.exists();
	}
	
	public boolean mkdir() {
		return this.impl.mkdir();
	}
	
    public boolean mkdirs() {
    	return this.impl.mkdirs();
    }
    
    public URI toURI() {
    	try {
			return new URI("file", this.impl.getAbsolutePath(), null);
		} catch (URISyntaxException e) {
			// shouldn't happen
			return null;
		}
    }
}
