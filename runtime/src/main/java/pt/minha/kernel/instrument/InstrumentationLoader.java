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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.RemappingClassAdapter;

import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.minha.kernel.instrument.ClassConfig.Action;

public class InstrumentationLoader extends ClassLoader {
	
	private ClassConfig cc;
	
	public InstrumentationLoader(ClassConfig cc) {
		this.cc = cc;
	}
	
	private Class<?> findLoadedOrGlobal(ClassConfig.Action act, String name) throws ClassNotFoundException {
		if (act.equals(ClassConfig.Action.invalid))
			throw new ClassCastException("class marked as invalid");

		/*
		 * Moved and faked classes arrive with the prefixed name. If it
		 * gets here without the prefix, then it should be handled as a
		 * global as the reference comes from a fake, load or global.
		 */
		if (act.equals(ClassConfig.Action.global) ||
			act.equals(ClassConfig.Action.fake) ||
			act.equals(ClassConfig.Action.moved))
			return super.loadClass(name);
					
		Class<?> claz = findLoadedClass(name);
		if (claz!=null)
			return claz;
		
		return null;
	}
	
	public void transform(Translation trans, ClassVisitor cw) throws IOException {

		ClassVisitor ca = cw;

		if (trans.getLogger().isInfoEnabled()) {
			trans.getLogger().info("transforming {}", trans);
		}

		// ------ This is the bytecode re-writting pipeline: -------
		// (order is: last in, first used)
		
		// Handle readObject/writeObject in Serializable classes
		ca = new SerializableClassVisitor(ca, trans);

		// Rewrite some special methods
		ca = new MethodRemapperClassVisitor(ca, trans);
			
		// Rewrite MONITORENTER/MONITORLEAVE to methods in fake.j.l.Object 
		ca = new FakeMonitorClassVisitor(ca, trans);
			
		// Rewrite synchronized methods to synchronized blocks
		ca = new SyncToMonitorClassVisitor(ca, trans);

		// Redirect references to fake.* and moved.* classes
		ca = new RemappingClassAdapter(ca, new ClassRemapper(cc, trans));
		
		// Prepare for changes done by other stages
		ca = new JSRInlinerClassVisitor(ca);
				
		// Update translation config from annotations (this is the first
		// stage in the pipeline!)
		ca = new AnnotatedClassVisitor(ca, trans); 
		// ---------------------------------------------------------------------------------------
		
		InputStream is = getResourceAsStream(trans.getFileName());
		ClassReader cr = new ClassReader(is);
		cr.accept(ca, ClassReader.SKIP_FRAMES);
	}

	public byte[] load(Translation trans, Action act) throws IOException {
		if (act.equals(ClassConfig.Action.load)) {		
			InputStream is = getResourceAsStream(trans.getFileName());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int n = is.read(buf);
			while(n>0) {
				baos.write(buf, 0, n);
				n = is.read(buf);
			}
			return baos.toByteArray();
		} else {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
				protected String getCommonSuperClass(String type1, String type2) {
					return InstrumentationLoader.this.getCommonSuperClass(type1, type2);
				}
			};
			transform(trans, cw);
			byte[] buf = cw.toByteArray();

			Logger logger =  trans.getLogger();
			if (logger.isDebugEnabled()) {
				try {
					Writer out = new CharArrayWriter();
					out.write('\n');
					ClassReader cr = new ClassReader(new ByteArrayInputStream(buf));
					cr.accept(new CheckClassAdapter(new TraceClassVisitor(new PrintWriter(out))), ClassReader.EXPAND_FRAMES);
					logger.trace("transformed bytecode: {}", out);
				} catch (Exception e) {
					logger.error("class validation error", e);
				}
			}

			return buf;
		}
	}
	
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		String effname = name.replace('.', '/');
		ClassConfig.Action act = cc.get(effname);

		Class<?> claz = findLoadedOrGlobal(act, name);
		if (claz != null)
			return claz;
				
		/*
		 * Set up translation configuration defaults. This depends on
		 * the class name prefix (i.e. fake or moved), properties file,
		 * and class file annotations.
		 */
		Translation trans = new Translation(effname, act);
		try {
			byte[] clsData = load(trans, act);

			if (trans.isGlobal())
				// If we discovered this from an annotation...
				return super.loadClass(name);
			else {
				try {
					String pname = name.substring(0,name.lastIndexOf('.'));
					if(getPackage(pname)==null)
						definePackage(pname, null, null, null, null, null, null, null);
				} catch (IndexOutOfBoundsException|IllegalArgumentException e) {
					//Ignore exceptions
				}
				return defineClass(name, clsData, 0, clsData.length);
			}
		} catch (Exception e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	public Class<?> forName(String name) throws ClassNotFoundException {
		Translation trans = new Translation("foobar", ClassConfig.Action.translate);
		ClassRemapper remapAll = new ClassRemapper(cc, trans);
		return loadClass(remapAll.map(name));
	}
	
	/*private static void checkAndDumpClass(byte[] buf) {
		try {
			ClassReader cr = new ClassReader(new ByteArrayInputStream(buf));
			cr.accept(new CheckClassAdapter(new TraceClassVisitor(new PrintWriter(System.out))), ClassReader.EXPAND_FRAMES);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}*/
	
	/*
	 * Rest of this file is based on code in ASM Tests by Eugene Kuleshov (v3.3)
	 * Copyright (c) 2002-2005 France Telecom.
	 * Modified and redistributed according to: http://asm.ow2.org/license.html.
	 * 
	 * Used as suggested in ASM mailing list thread: 
	 * http://mail-archive.ow2.org/asm/2011-08/msg00056.html
	 */
	protected String getCommonSuperClass(String type1, String type2) {
		ClassInfo ci1 = new ClassInfo(type1);
		ClassInfo ci2 = new ClassInfo(type2);

		if (ci1.isAssignableFrom(ci2))
			return type1;
		if (ci2.isAssignableFrom(ci1))
			return type2;
		
		if (ci1.isInterface() || ci2.isInterface())
			return "java/lang/Object";
	
		do {
			// Should never be null, because if ci1 were the Object class
			// or an interface, it would have been caught above.
			ci1 = ci1.getSuperclass();
		} while (!ci1.isAssignableFrom(ci2));

		return ci1.getType().getInternalName();
	}

	class ClassInfo {
		private Type type;
		private boolean isInterface;
		private String superClass;
		private String[] interfaces;

		public ClassInfo(String effname) {
			Class cls = null;
			
			String name = effname.replace('/', '.');
			ClassConfig.Action act = cc.get(effname);
			try {
				cls = findLoadedOrGlobal(act, name);
			} catch (ClassNotFoundException e) {
				// failover...
			}

			if (cls != null) {
				this.type = Type.getType(cls);
				this.isInterface = cls.isInterface();
				if (!isInterface && cls != Object.class)
					this.superClass = cls.getSuperclass().getName()
							.replace('.', '/');
				Class[] ifs = cls.getInterfaces();
				this.interfaces = new String[ifs.length];
				for (int i = 0; i < ifs.length; i++) {
					this.interfaces[i] = ifs[i].getName().replace('.', '/');
				}
				return;
			}

			// The class isn't loaded. Try to get the class file, and
			// extract the information from that.
			this.type = Type.getObjectType(effname);
			Translation trans = new Translation(effname, act);
			ClassVisitor ca = new ClassInfoVisitor();
			try {
				transform(trans, ca);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		Type getType() {
			return type;
		}

		ClassInfo getSuperclass() {
			if (superClass == null) {
				return null;
			}
			return new ClassInfo(superClass);
		}

		/**
		 * Same as {@link Class#getInterfaces()}
		 */
		ClassInfo[] getInterfaces() {
			if (interfaces == null) {
				return new ClassInfo[0];
			}
			ClassInfo[] result = new ClassInfo[interfaces.length];
			for (int i = 0; i < result.length; ++i) {
				result[i] = new ClassInfo(interfaces[i]);
			}
			return result;
		}

		/**
		 * Same as {@link Class#isInterface}
		 */
		boolean isInterface() {
			return isInterface;
		}

		private boolean implementsInterface(ClassInfo that) {
			for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
				ClassInfo[] interfaces = c.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					ClassInfo iface = interfaces[i];
					if (iface.type.equals(that.type)
							|| iface.implementsInterface(that)) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean isSubclassOf(ClassInfo that) {
			for (ClassInfo ci = this; ci != null; ci = ci.getSuperclass()) {
				if (ci.getSuperclass() != null
						&& ci.getSuperclass().type.equals(that.type)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Same as {@link Class#isAssignableFrom(Class)}
		 */
		boolean isAssignableFrom(ClassInfo that) {
			if (this == that
					|| that.isSubclassOf(this)
					|| that.implementsInterface(this)
					|| (that.isInterface() && getType().getDescriptor().equals("Ljava/lang/Object;"))) {
				return true;
			}

			return false;
		}

		private class ClassInfoVisitor extends ClassVisitor {

			public ClassInfoVisitor() {
				super(Opcodes.ASM5, new ClassWriter(0));
			}

			@Override
			public void visit(int version, int access, String name, String signature, String supername, String[] interfaces) {
				super.visit(version, access, name, signature, supername, interfaces);
				if (name.equals("java/lang/Object"))
					return;
				ClassInfo.this.interfaces = interfaces;
				ClassInfo.this.superClass = supername;
				ClassInfo.this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
			}
		}
	}
}