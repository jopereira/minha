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

package pt.minha.kernel.instrument;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ClassConfig {
	
	public static final String moved_prefix="pt/minha/models/moved/";
	public static final String fake_prefix="pt/minha/models/fake/";
	
	public enum Action {
		global(0), load(1), fake(2), moved(3), translate(4), invalid(5);
			
		public final int value;
	
		private Action(int value) {this.value = value;}
	};
	
	private Map<String, Action> classes = new HashMap<String, ClassConfig.Action>();
	private Map<String, Action> prefixes = new HashMap<String, ClassConfig.Action>();
	private Action defaultval;
	
	public ClassConfig(Properties props) {
		for(String s: props.stringPropertyNames()) {
			String k = s.replace('.', '/');
			Action v = Action.valueOf(props.getProperty(s).trim());
			if (k.equals("*"))
				defaultval=v;
			else if (k.endsWith("/*") || k.endsWith("$*"))
				prefixes.put(k.substring(0, k.length()-2), v);
			else if (s.endsWith("*"))
				prefixes.put(k.substring(0, k.length()-1), v);
			else
				classes.put(k, v);
		}
	}
	
	public Action get(String name) {
		Action s = classes.get(name);
		if (s!=null)
			return s;

		String str = name.replace('$', '/');
		int last = str.length();
		while(last>0) {
			str = str.substring(0, last);
			String key = name.substring(0, last);
			s = prefixes.get(key);
			if (s!=null)
				return s;			
			last = str.lastIndexOf('/');
		}
		
		return defaultval;
	}
}
