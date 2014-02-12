/*******************************************************************************
 * MINHA - Java Virtualized Environment
 * Copyright (c) 2011, Universidade do Minho
 * All rights reserved.
 * 
 * Contributors:
 *  - Jose Orlando Pereira <jop@di.uminho.pt>
 *  - Nuno Alexandre Carvalho <nuno@di.uminho.pt>
 *  - Joao Paulo Bordalo <jbordalo@di.uminho.pt> 
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
 ******************************************************************************/
package examples.thread;

/*
 * Thread synchronization.
 * 
 * MINHA command line:
 * java -cp lib/asm-all-3.3.1.jar:build/minha.jar sim.Runner examples.thread.Synchro
 * 
 */
public class Synchro
{
	public static void main(String[]args)throws InterruptedException
	{
		ThreadB b = new ThreadB();
		b.setName("ThreadB");
		b.start();
		synchronized(b)     //thread got lock
		{
			System.out.println("iam calling wait method");
			b.wait();
			System.out.println("I got notification");
		}
		System.out.println(b.total);
	}
}

class ThreadB extends Thread
{
	int total=0;
	public void run()
	{
		synchronized (this)//.thread got lock
		{
			System.out.println("iam starting calculation");
			for(int i=0;i<1000;i++)
			{
				total+=1;
			}
			System.out.println("iam giving notification call");
			notify();    //thread releases lock again
		}
	}
}
