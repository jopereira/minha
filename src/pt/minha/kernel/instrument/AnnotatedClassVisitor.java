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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import pt.minha.api.Global;
import pt.minha.api.Local;

public class AnnotatedClassVisitor extends ClassVisitor implements Opcodes {

	private static final String localName = "L"+Local.class.getCanonicalName().replace('.', '/')+";";
	private static final String globalName = "L"+Global.class.getCanonicalName().replace('.', '/')+";";

	private Translation action;

	public AnnotatedClassVisitor(ClassVisitor visitor, Translation action) {
		super(Opcodes.ASM4, visitor);
		this.action = action;
	}	

	@Override
	public AnnotationVisitor visitAnnotation(String name, boolean runtime) {
		if (name.equals(localName))
			return new AnnotationVisitor(Opcodes.ASM4) {
				@Override
				public void visit(String name, Object value) {
					if (name.equals("synch"))
						action.setSynchronized((Boolean)value);
					else if (name.equals("useFakes"))
						action.setUsingFakes((Boolean)value);
					else if (name.equals("useMoved"))
						action.setUsingMoved((Boolean)value);
					super.visit(name, value);
				}
			};
		else if (name.equals(globalName))
			action.setGlobal(true);
		return super.visitAnnotation(name, runtime);
	}
}
