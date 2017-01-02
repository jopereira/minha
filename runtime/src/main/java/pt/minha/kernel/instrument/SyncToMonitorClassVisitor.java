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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class SyncToMonitorClassVisitor extends ClassVisitor {
	public static final String PREFIX = "$sync_";

	private String clz;
	private boolean hasClinit;
	private int access;
	private Translation trans;

	public SyncToMonitorClassVisitor(ClassVisitor visitor, Translation trans) {
		super(Opcodes.ASM5, visitor);
		this.trans = trans;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (trans.isSynchronized()) {
			clz = name;
			this.access = access;
			if ((access&Opcodes.ACC_INTERFACE)==0) {
				trans.getLogger().debug("adding explicit class monitor object");
				FieldVisitor fv = visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "_fake_class", "L"+ClassConfig.fake_prefix+"java/lang/Object;", null, null);
				fv.visitEnd();
			}
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (trans.isSynchronized()) {
			if (name.equals("<clinit>")) {
				trans.getLogger().debug("redirecting existing static initializer");
				hasClinit = true;
				return new ClinitVisitor(super.visitMethod(access, name, desc, signature, exceptions));
			}
			
			if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
				trans.getLogger().debug("wrapping {}{} with {}{}", name, desc, PREFIX, name);
				makeStub(access & ~Opcodes.ACC_SYNCHRONIZED, name, desc, signature, exceptions);
			
				return super.visitMethod((access & ~(Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED) | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE), PREFIX+name, desc, signature, exceptions);
			}
		}
		return super.visitMethod(access, name, desc, signature, exceptions);
	}

	public void visitEnd() {
		if (trans.isSynchronized()) {
			if (!hasClinit && (access&Opcodes.ACC_INTERFACE)==0) {
				mkClinit();
				trans.getLogger().debug("adding fake static initializer");
			}
		}
		super.visitEnd();
	}

	private void mkClinit() {
		MethodVisitor mv = visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	private class ClinitVisitor extends MethodVisitor {
		public ClinitVisitor(MethodVisitor arg0) {
			super(Opcodes.ASM5, arg0);
		}

		public void visitCode() {
			super.visitCode();
			if ((access&Opcodes.ACC_INTERFACE)!=0)
				return;
			mv.visitTypeInsn(Opcodes.NEW, ClassConfig.fake_prefix+"java/lang/Object");
			mv.visitInsn(Opcodes.DUP);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ClassConfig.fake_prefix+"java/lang/Object", "<init>", "()V", false);
			mv.visitFieldInsn(Opcodes.PUTSTATIC, clz, "_fake_class", "L"+ClassConfig.fake_prefix+"java/lang/Object;");
		}		
	}
	
	public void makeStub(int access, String name, String desc, String signature, String[] exceptions) {
		Method m = new Method(name, desc);
		
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		mv.visitCode();
		
		Label begin = new Label();
		Label pre_invoke = new Label();
		Label pos_leave = new Label();
		Label in_catch = new Label();
		Label pre_rethrow = new Label();
		Label end = new Label();

		mv.visitTryCatchBlock(pre_invoke, pos_leave, in_catch, null);
		mv.visitTryCatchBlock(in_catch, pre_rethrow, in_catch, null);

		mv.visitLabel(begin);

		int offset;
		if ((access&Opcodes.ACC_STATIC)==0) {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			offset = 1;
		} else {
			mv.visitFieldInsn(Opcodes.GETSTATIC, clz, "_fake_class", "L"+ClassConfig.fake_prefix+"java/lang/Object;");
			offset = 0;
		}
		
		int length = 0;
		for(Type t: m.getArgumentTypes())
			length += t.getSize();
		
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.ASTORE, offset + length);
		mv.visitInsn(Opcodes.MONITORENTER);

		mv.visitLabel(pre_invoke);

		if ((access&Opcodes.ACC_STATIC)==0)
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		
		int i=offset;
		for(Type t: m.getArgumentTypes()) {
			// t.getOpcode() should work for long and double too... :-( 
			if (t.getClassName().equals("long"))
				mv.visitVarInsn(Opcodes.LLOAD, i);
			else if (t.getClassName().equals("double"))
				mv.visitVarInsn(Opcodes.DLOAD, i);
			else
				mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), i);
			i += t.getSize();
		}

		boolean itf = (access&Opcodes.ACC_INTERFACE)!=0;
		if ((access&Opcodes.ACC_STATIC)==0)
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clz, PREFIX+name, desc, itf);
		else
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, clz, PREFIX+name, desc, itf);

		mv.visitVarInsn(Opcodes.ALOAD, offset + length);
		mv.visitInsn(Opcodes.MONITOREXIT);

		mv.visitLabel(pos_leave);

		if (m.getReturnType().equals(Type.VOID_TYPE))
			mv.visitInsn(Opcodes.RETURN);
		else
			mv.visitInsn(m.getReturnType().getOpcode(Opcodes.IRETURN));

		mv.visitLabel(in_catch);
		
		mv.visitVarInsn(Opcodes.ALOAD, offset+length);
		mv.visitInsn(Opcodes.MONITOREXIT);

		mv.visitLabel(pre_rethrow);
		mv.visitInsn(Opcodes.ATHROW);

		mv.visitLabel(end);
		
		i=0;
		if ((access&Opcodes.ACC_STATIC)==0)
			mv.visitLocalVariable("this", "L"+clz+";", null, begin, end, i++);
		for(Type t: m.getArgumentTypes())
			mv.visitLocalVariable("arg"+i, t.getDescriptor(), null, begin, end, i++);

		mv.visitMaxs(0, 0);	
		mv.visitEnd();
	}
}
