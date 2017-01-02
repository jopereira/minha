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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FakeMonitorClassVisitor extends ClassVisitor {

	private Translation trans;

	public FakeMonitorClassVisitor(ClassVisitor arg0, Translation trans) {
		super(Opcodes.ASM5, arg0);
		this.trans = trans;
	}
	
	public void visit(int version, int access, String name, String signature, String supername, String[] interfaces) {
		if (trans.isSynchronized()) {
			if (supername.equals("java/lang/Object") && (access&Opcodes.ACC_INTERFACE)==0) {
				supername = ClassConfig.fake_prefix + supername;
				trans.getLogger().debug("reparenting for fake syncronization");
			}
		}
		super.visit(version, access, name, signature, supername, interfaces);
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (trans.isSynchronized())
			return new LocalMethodVisitor(name, desc, super.visitMethod(access, name, desc, signature, exceptions));
		else
			return super.visitMethod(access, name, desc, signature, exceptions);
	}

	private class LocalMethodVisitor extends MethodVisitor {
		private final String name;
		private final String desc;

		public LocalMethodVisitor(String name, String desc, MethodVisitor arg0) {
			super(Opcodes.ASM5, arg0);
			this.name = name;
			this.desc = desc;
		}

		public void visitInsn(int opcode) {
			if (opcode == Opcodes.MONITORENTER) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, ClassConfig.fake_prefix + "java/lang/Object", "_fake_enter", "(Ljava/lang/Object;)V", false);
			} else if (opcode == Opcodes.MONITOREXIT) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, ClassConfig.fake_prefix + "java/lang/Object", "_fake_leave", "(Ljava/lang/Object;)V", false);
			} else {
				super.visitInsn(opcode);
				return;
			}
			trans.getLogger().debug("redirecting monitor op in {}{}", this.name, this.desc);
		}
		
		public void visitTypeInsn (int opcode, String s) {
			if (opcode==Opcodes.NEW && s.equals("java/lang/Object")) {
				s = ClassConfig.fake_prefix + s;
				trans.getLogger().debug("redirecting explicit java.lang.Object new in {}{}", this.name, this.desc);
			}
			mv.visitTypeInsn(opcode, s);
		}

		public void visitMethodInsn (int opcode, String owner, String name, String desc, boolean itf) {
			if (opcode==Opcodes.INVOKESPECIAL && owner.equals("java/lang/Object") && name.equals("<init>")) {
				mv.visitMethodInsn(opcode, ClassConfig.fake_prefix + owner, name, desc, itf);
				trans.getLogger().debug("redirecting explicit java.lang.Object initializer in {}{}", this.name, this.desc);
			} else
				mv.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}
}
