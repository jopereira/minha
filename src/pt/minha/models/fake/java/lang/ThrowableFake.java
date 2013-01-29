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

package pt.minha.models.fake.java.lang;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Support for Throwable.printStackTrace(). This is a lot of trouble, since we cannot wrap throwables
 * as they are generated by the VM and, as printStackTrace(Print*) references moved classes, it cannot
 * be called directly. The workaround is this ugly piece of code that breaks if any class defines or
 * redefines a method called printStackTrace with the same parameters. Hopefully, will be able to do
 * better than this.
 */
public class ThrowableFake {
	public static void _fake_printStackTrace(java.lang.Object pt, PrintStream ps) {
		// If this is not a throwable, we have a problem.
		if (!(pt instanceof Throwable)) {
			java.lang.System.err.println("Add "+pt.getClass().getCanonicalName()+" to nothrowable in RemapFakeClassVisitor");
			System.exit(1);
		} else {
			// Otherwise, fake it locally and avoid using the original code.
			Throwable t = (Throwable) pt;
			ps.println(t.toString());
			StackTraceElement[] st = t.getStackTrace();
			for(int i = 0; i<st.length; i++)
				ps.println("\tat "+st[i].toString());
			if (t.getCause()!=null) {
				ps.print("Caused by: ");
				_fake_printStackTrace(t.getCause(), ps);
			}		
		}
	}
	
	public static void _fake_printStackTrace(java.lang.Object pt, PrintWriter ps) {
		// If this is not a throwable, we have a problem.
		if (!(pt instanceof Throwable)) {
			java.lang.System.err.println("Add "+pt.getClass().getCanonicalName()+" to nothrowable in RemapFakeClassVisitor");
			System.exit(1);
		} else {
			// Otherwise, fake it locally and avoid using the original code.
			Throwable t = (Throwable) pt;
			ps.println(t.toString());
			StackTraceElement[] st = t.getStackTrace();
			for(int i = 0; i<st.length; i++)
				ps.println("\tat "+st[i].toString());
			if (t.getCause()!=null) {
				ps.print("Caused by: ");
				_fake_printStackTrace(t.getCause(), ps);
			}		
		}
	}
}
