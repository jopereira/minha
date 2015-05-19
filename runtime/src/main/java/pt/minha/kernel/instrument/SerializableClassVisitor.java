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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import pt.minha.models.local.io.WrappedObjectInputStream;
import pt.minha.models.local.io.WrappedObjectOutputStream;

public class SerializableClassVisitor extends ClassVisitor implements Opcodes {
	private boolean isSerializable;
	private String name;
	private Translation trans; 
	
	public SerializableClassVisitor(ClassVisitor visitor, Translation trans) {
		super(Opcodes.ASM5, visitor);
		this.trans = trans;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		for(String intf: interfaces) { 
			if (intf.equals("java/io/Serializable")) {
				isSerializable = true;
				break;
			}
		}
		super.visit(version, access, name, signature, superName, interfaces);	
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		
		if (isSerializable && trans.isUsingMoved()) {
			if (name.equals("writeObject") && desc.equals("(L"+ClassConfig.fake_prefix+"java/io/ObjectOutputStream;)V"))
				makeWriteStub();
			if (name.equals("readObject") && desc.equals("(L"+ClassConfig.fake_prefix+"java/io/ObjectInputStream;)V"))
				makeReadStub();
		}
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
		
	private void makeWriteStub() {
		String wclz = WrappedObjectOutputStream.class.getCanonicalName().replace('.', '/');

		MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "writeObject", "(Ljava/io/ObjectOutputStream;)V", null, new String[] { "java/io/IOException" });
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, wclz);
		mv.visitFieldInsn(GETFIELD, wclz, "wrapper", "Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, ClassConfig.fake_prefix+"java/io/ObjectOutputStream");
		mv.visitMethodInsn(INVOKESPECIAL, name, "writeObject", "(L"+ClassConfig.fake_prefix+"java/io/ObjectOutputStream;)V");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+name+";", null, l0, l2, 0);
		mv.visitLocalVariable("stream", "Ljava/io/ObjectOutputStream;", null, l0, l2, 1);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
	}
	
	private void makeReadStub() {
		String wclz = WrappedObjectInputStream.class.getCanonicalName().replace('.', '/');
		
		MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "readObject", "(Ljava/io/ObjectInputStream;)V", null, new String[] { "java/io/IOException", "java/lang/ClassNotFoundException" });
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, wclz);
		mv.visitFieldInsn(GETFIELD, wclz, "wrapper", "Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, ClassConfig.fake_prefix+"java/io/ObjectInputStream");
		mv.visitMethodInsn(INVOKESPECIAL, name, "readObject", "(L"+ClassConfig.fake_prefix+"java/io/ObjectInputStream;)V");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitInsn(RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", "L"+name+";", null, l0, l2, 0);
		mv.visitLocalVariable("stream", "Ljava/io/ObjectInputStream;", null, l0, l2, 1);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
	}	
}
