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

package pt.minha.kernel.instrument;

import org.objectweb.asm.commons.Remapper;

public class ClassRemapper extends Remapper {
	private Translation trans;
	private ClassConfig cc;
	
	public ClassRemapper(ClassConfig cc, Translation trans) {
		this.trans = trans;
		this.cc = cc;
	}
	
	@Override
	public String map(String type) {
		ClassConfig.Action act = cc.get(type);
		
		if (act.equals(ClassConfig.Action.fake) && trans.isUsingFakes()) {
			trans.getLogger().debug("redirecting {} to fake", type);
			return ClassConfig.fake_prefix + type;
		} else if (act.equals(ClassConfig.Action.moved) && trans.isUsingMoved()) {
			trans.getLogger().debug("redirecting {} to moved", type);
			return ClassConfig.moved_prefix + type;
		} else
			return type;
	}
}