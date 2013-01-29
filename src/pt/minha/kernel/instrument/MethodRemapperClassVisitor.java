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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodRemapperClassVisitor extends ClassVisitor {
	private Translation trans;
	
	public MethodRemapperClassVisitor(ClassVisitor arg0, Translation trans) {
		super(Opcodes.ASM4, arg0);
		this.trans = trans;
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new RemapMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
	}

	private class RemapMethodVisitor extends MethodVisitor {
		public RemapMethodVisitor(MethodVisitor arg0) {
			super(Opcodes.ASM4, arg0);
		}

		/**
		 * Remapped methods go here. This happens either because we don't need to remap the whole
		 * class (e.g. nanoTime), because we can't override a final method (e.g. wait/notify) or
		 * cannot wrap the entire class (i.e. throwables).
		 */
		public void visitMethodInsn (int opcode, String owner, String name, String desc) {
			if (owner.equals("java/lang/Class") && name.equals("forName") && trans.isUsingFakes())
				mv.visitMethodInsn(opcode, ClassConfig.fake_prefix+owner+"Fake", name, desc);
			else if (owner.equals("java/lang/Class") && name.equals("getResourceAsStream") && trans.isUsingMoved())				
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, ClassConfig.fake_prefix+owner+"Fake", "_fake_"+name, "(Ljava/lang/Class;"+desc.substring(1));
			else if (owner.equals("java/lang/ClassLoader") && name.equals("getResourceAsStream") && trans.isUsingMoved())
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, ClassConfig.fake_prefix+owner+"Fake", "_fake_"+name, "(Ljava/lang/ClassLoader;"+desc.substring(1));
			else if (name.equals("printStackTrace") && trans.isUsingMoved() && (desc.equals("(L"+ClassConfig.moved_prefix+"java/io/PrintStream;)V") || desc.contains("(L"+ClassConfig.moved_prefix+"java/io/PrintWriter;)V")))
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, ClassConfig.fake_prefix+"java/lang/ThrowableFake", "_fake_"+name, "(Ljava/lang/Object;"+desc.substring(1));
			else if (owner.equals("java/lang/Object") && trans.isSynchronized() && (name.equals("wait") || name.equals("notify") || name.equals("notifyAll")))
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, ClassConfig.fake_prefix+owner, "_fake_"+name, "(Ljava/lang/Object;"+desc.substring(1));
			else
				mv.visitMethodInsn(opcode, owner, name, desc);
		}
	}
}
