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

package pt.minha.kernel.util;

import java.io.IOException;
import java.util.Properties;

public class PropertiesLoader {
	private final String filename;
	private final Properties properties;
	
	public PropertiesLoader(String filename) throws IOException {
		this.filename = filename;
        this.properties = new Properties();
        this.properties.load(this.getClass().getClassLoader().getResourceAsStream(this.filename));
	}
	
	public String getProperty(String key) throws Exception {
		String value = this.properties.getProperty(key);
		if ( null == value )
			throw new Exception("Missing key '"+key+"' on file '"+this.filename+"'");
		return value;
	}
	
	public String getProperty(String key, String def) throws Exception {
		String value = this.properties.getProperty(key);
		if ( null == value )
			return def;
		return value;
	}

}
