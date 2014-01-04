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

package pt.minha.models.local.nio;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.util.HashSet;
import java.util.Set;

import pt.minha.kernel.simulation.Event;
import pt.minha.models.fake.java.nio.channels.SelectableChannel;
import pt.minha.models.fake.java.nio.channels.SelectionKey;
import pt.minha.models.fake.java.nio.channels.Selector;
import pt.minha.models.fake.java.nio.channels.spi.AbstractSelectableChannel;
import pt.minha.models.fake.java.nio.channels.spi.AbstractSelector;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.local.lang.SimulationThread;

public class SelectorImpl extends AbstractSelector {
	private SelectorProvider provider;
	private Set<SelectionKey> keys, selectedKeys, canceled;
	private Set<SelectionKeyImpl> ready;
	private BlockingHelper selectors = new BlockingHelper() {
		@Override
		public boolean isReady() {
			return !ready.isEmpty();
		}
	};
	private boolean wakeup;
		
	protected SelectorImpl(SelectorProvider provider) {
		this.provider = provider;
		keys = new HashSet<SelectionKey>();
		selectedKeys = new HashSet<SelectionKey>();
		ready = new HashSet<SelectionKeyImpl>();
		canceled = new HashSet<SelectionKey>();
	}

	@Override
	public SelectorProvider provider() {
		return provider;
	}

	@Override
	public Set<SelectionKey> keys() {
		return keys;
	}

	@Override
	public Set<SelectionKey> selectedKeys() {
		return selectedKeys;
	}

	@Override
	public int selectNow() throws IOException {
		return select(-1);
	}

	@Override
	public int select(long timeout) throws IOException {
		try {			
			SimulationThread.stopTime(0);
			
			Set<SelectionKeyImpl> previous = ready;
			ready = new HashSet<SelectorImpl.SelectionKeyImpl>();
			
			for(SelectionKeyImpl key: previous)
				key.retest();
			
			SimulationThread current = SimulationThread.currentSimulationThread();
			
			if (current.getInterruptedStatus(false))
				wakeup = true;
			
			long deadline = current.getTimeline().getTime() + timeout*1000000;
			
			while((timeout==0 || deadline>current.getTimeline().getTime()) && !wakeup && !selectors.isReady()) {
				selectors.queue(SimulationThread.currentSimulationThread().getWakeup());
				if (timeout>0)
					SimulationThread.currentSimulationThread().getWakeup().schedule(deadline-current.getTimeline().getTime());
				if (SimulationThread.currentSimulationThread().pause(true, false))
					wakeup = true;
				selectors.cancel(SimulationThread.currentSimulationThread().getWakeup());
			}
			
			wakeup = false;
			ready.removeAll(canceled);
			keys.removeAll(canceled);
			selectedKeys.removeAll(canceled);
			
			int oldsize = selectedKeys.size();
			selectedKeys.addAll(ready);
			
			return selectedKeys.size()-oldsize;
		} finally {
			SimulationThread.startTime(0);
		}
	}

	@Override
	public int select() throws IOException {
		return select(0);
	}

	@Override
	public Selector wakeup() {
		wakeup = true;
		selectors.wakeup();
		return this;
	}

	public SelectionKey register(AbstractSelectableChannel cb, int operation, Object attachment) {
		SelectionKeyImpl key = new SelectionKeyImpl(cb);
		key.interestOps(operation);
		key.attach(attachment);
		keys.add(key);
		return key;
	}
	
	private void selected(SelectionKeyImpl key) {
		ready.add(key);
		selectors.wakeup();
	}
	private void canceled(SelectionKeyImpl key) {
		canceled.add(key);
	}
	
	class SelectionKeyImpl extends SelectionKey {
		private Event wakeup;
		private int interest, ready;
		private Object attachment;
		private AbstractSelectableChannel channel;
		private boolean cancelled;
		
		public SelectionKeyImpl(AbstractSelectableChannel channel) {
			wakeup = new Event(SimulationThread.currentSimulationThread().getTimeline()) {
				@Override
				public void run() {
					retest();
				}
			};
			this.channel = channel;
		}
		
		void retest() {
			ready=0;
			test(OP_READ);
			test(OP_WRITE);
			test(OP_CONNECT);
			test(OP_ACCEPT);
			if (ready!=0)
				selected(this);			
		}

		@Override
		public Selector selector() {
			return SelectorImpl.this;
		}

		@Override
		public int interestOps() {
			checkCancel();
			
			return interest;
		}

		private void test(int op) {
			if ((interest & op)==0)
				return;
			if (channel.helperFor(op).isReady())
				ready |= op;
			channel.helperFor(op).queue(wakeup);
		}
		
		private void update(int op, int ops) {
			if ((interest & op)==0 && (ops & op)!=0) {
				if (channel.helperFor(op).isReady())
					ready |= op;
				channel.helperFor(op).queue(wakeup);
			}
			if ((interest & op)!=0 && (ops &op)==0)
				channel.helperFor(op).cancel(wakeup);
		}
		
		@Override
		public SelectionKey interestOps(int ops) {
			checkCancel();
			
			update(OP_READ, ops);
			update(OP_WRITE, ops);
			update(OP_CONNECT, ops);
			update(OP_ACCEPT, ops);
			interest = ops;
			if (ready!=0)
				selected(SelectionKeyImpl.this);
			return this;
		}

		@Override
		public int readyOps() {
			checkCancel();
			
			return ready;
		}

		@Override
		public Object attach(Object attachment) {
			Object previous = this.attachment;
			this.attachment = attachment;
			return previous;
		}

		@Override
		public Object attachment() {
			return attachment;
		}

		@Override
		public SelectableChannel channel() {
			return channel;
		}

		@Override
		public void cancel() {
			interestOps(0);
			cancelled = true;
			canceled(SelectionKeyImpl.this);
		}
		
		private void checkCancel() {
			if (cancelled)
				throw new CancelledKeyException();
		}
		
		@Override
		public boolean isValid() {
			return !cancelled;
		}

		@Override
		public boolean isReadable() {
			return (ready&OP_READ)!=0;
		}

		@Override
		public boolean isWritable() {
			return (ready&OP_WRITE)!=0;
		}

		@Override
		public boolean isAcceptable() {
			return (ready&OP_ACCEPT)!=0;
		}

		@Override
		public boolean isConnectable() {
			return (ready&OP_CONNECT)!=0;
		}
		
		public String toString() {
			return "SK("+interest+", "+ready+", "+channel+")";
		}
	}
}