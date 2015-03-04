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

package pt.minha.models.fake.java.lang.management;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import pt.minha.models.local.lang.SimulationThread;

public class ManagementFactory {
	public static ThreadMXBean getThreadMXBean() {
		return new ThreadMXBean() {
			
			@Override
			public void setThreadCpuTimeEnabled(boolean enable) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setThreadContentionMonitoringEnabled(boolean enable) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void resetPeakThreadCount() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isThreadCpuTimeSupported() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isThreadCpuTimeEnabled() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isThreadContentionMonitoringSupported() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isThreadContentionMonitoringEnabled() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isSynchronizerUsageSupported() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isObjectMonitorUsageSupported() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isCurrentThreadCpuTimeSupported() {
				return true;
			}
			
			@Override
			public long getTotalStartedThreadCount() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public long getThreadUserTime(long id) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
					boolean lockedSynchronizers) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ThreadInfo getThreadInfo(long id, int maxDepth) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ThreadInfo[] getThreadInfo(long[] ids) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ThreadInfo getThreadInfo(long id) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public long getThreadCpuTime(long id) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int getThreadCount() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int getPeakThreadCount() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public int getDaemonThreadCount() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public long getCurrentThreadUserTime() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public long getCurrentThreadCpuTime() {
				try {
					SimulationThread.stopTime(0);
					return SimulationThread.currentSimulationThread().totalCPU;
				} finally {
					SimulationThread.startTime(0);
				}
			}
			
			@Override
			public long[] getAllThreadIds() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public long[] findMonitorDeadlockedThreads() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public long[] findDeadlockedThreads() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
					boolean lockedSynchronizers) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ObjectName getObjectName() {
				return null;
			}
		};
	}
	
	public static MBeanServer getPlatformMBeanServer() {
		// FIXME: should wrap and prefix mbeans with instance name
		return java.lang.management.ManagementFactory.getPlatformMBeanServer(); 
	}
	
	public static RuntimeMXBean getRuntimeMXBean() {
		// FIXME: should wrap and prefix mbeans with instance name
		return java.lang.management.ManagementFactory.getRuntimeMXBean(); 		
	}
	
	public static MemoryMXBean getMemoryMXBean() {
		// FIXME: should wrap and prefix mbeans with instance name
		return java.lang.management.ManagementFactory.getMemoryMXBean(); 		
	}
	
	public static OperatingSystemMXBean getOperatingSystemMXBean() {
		// FIXME: should wrap and prefix mbeans with instance name
		return java.lang.management.ManagementFactory.getOperatingSystemMXBean(); 		
	}
	
	public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
		// FIXME: should return beans
		return Collections.emptyList();
	}
}
