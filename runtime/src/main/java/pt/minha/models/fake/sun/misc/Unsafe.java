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

package pt.minha.models.fake.sun.misc;

import java.lang.reflect.Field;

public class Unsafe {
	private static Unsafe theUnsafe;
	private sun.misc.Unsafe theUnsafeUnsafe;
	
	static {
		theUnsafe = new Unsafe();
	}
	
	public static Unsafe getUnsafe() {
		return theUnsafe;
	}
	
	private Unsafe() {
		/*
		 * Based on: http://stackoverflow.com/questions/13003871/how-do-i-get-the-instance-of-sun-misc-unsafe
		 */
        try {
        	Field singleoneInstanceField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            theUnsafeUnsafe = (sun.misc.Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
	
    public void putObject(Object o, long offset, Object x){
    	theUnsafeUnsafe.putObject(o, offset, x);
    }

    public long objectFieldOffset(Field f) {
		return theUnsafeUnsafe.objectFieldOffset(f);
	}
	
	public int arrayBaseOffset(Class arrayClass){
		return theUnsafeUnsafe.arrayBaseOffset(arrayClass);
	}

	public int arrayIndexScale(Class arrayClass){
		return theUnsafeUnsafe.arrayIndexScale(arrayClass);
	}

    public boolean compareAndSwapInt(Object o, long f, int a, int b) {
		return theUnsafeUnsafe.compareAndSwapInt(o, f, a, b);
	}

    public boolean compareAndSwapLong(Object o, long offset, long expected, long x){
    	return theUnsafeUnsafe.compareAndSwapLong(o, offset, expected, x);
    }

    public boolean compareAndSwapObject(Object o, long f, Object a, Object b) {
		return theUnsafeUnsafe.compareAndSwapObject(o, f, a, b);
	}

	public Object getObjectVolatile(Object o, long offset){
    	return theUnsafeUnsafe.getObjectVolatile(o, offset);
	}

	public void putObjectVolatile(Object o, long offset, Object x){
		theUnsafeUnsafe.putObjectVolatile(o, offset, x);
	}

    public void putOrderedObject(Object o, long offset, Object x){
    	theUnsafeUnsafe.putOrderedObject(o, offset, x);
    }
}
