/*-
 * Copyright (c) 2008, Derek Konigsberg
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.logicprobe.LogicMail.util;

import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AppInfo;

// TODO: Write rigorous tests for this class

/**
 * Provides a work item queue for <tt>Runnable</tt> objects.
 * This is similar to a thread pool, except all work items
 * run in sequence.  Also, the thread is not kept alive
 * when there are no pending work items.
 */
public class ThreadQueue {
	private Queue runnableQueue;
	private ThreadQueueThread threadQueueThread;
	private boolean isShutdown;
	
	/**
	 * Instantiates a new thread queue.
	 */
	public ThreadQueue() {
		runnableQueue = new Queue();
	}
	
	/**
	 * Flushes any pending work items, and optionally
	 * waits for the thread to join.
	 * 
	 * @param wait True to wait for the thread to join.
	 */
	public void shutdown(boolean wait) {
		isShutdown = true;
		synchronized(runnableQueue) {
			runnableQueue.clear();
		}
		if(wait && threadQueueThread != null) {
			try {
				threadQueueThread.join();
			} catch (InterruptedException e) { }
			threadQueueThread = null;
		}
	}
	
	/**
	 * Puts the provided <tt>Runnable</tt> object on the
	 * work item queue.  Starts the worker thread if necessary.
	 * 
	 * @param runnable The <tt>Runnable</tt> object.
	 * @throws IllegalStateException Thrown if {@link #shutdown(boolean)} has been called.
	 */
	public void invokeLater(Runnable runnable) {
		if(isShutdown) {
			throw new IllegalStateException("Thread queue has been shutdown");
		}
		boolean queued = false;
		synchronized(runnableQueue) {
			if(threadQueueThread != null && threadQueueThread.isAlive()) {
				runnableQueue.add(runnable);
				queued = true;
			}
		}
		if(!queued) {
			if(threadQueueThread != null) {
				try {
					threadQueueThread.join();
				} catch (InterruptedException e) { }
				threadQueueThread = null;
			}
			threadQueueThread = new ThreadQueueThread();
			runnableQueue.add(runnable);
			threadQueueThread.start();
		}
	}
	
	/**
	 * Actual thread implementation used for the work item queue.
	 */
	private class ThreadQueueThread extends Thread {
		/**
		 * Instantiates a new thread queue thread.
		 */
		public ThreadQueueThread() {
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			while(true) {
				Runnable runnable;
				synchronized(runnableQueue) {
					if(runnableQueue.element() != null) {
						runnable = (Runnable)runnableQueue.remove();
					}
					else {
						return;
					}
				}
				Thread.yield();
				try {
					runnable.run();
				} catch (RuntimeException exp) {
	                EventLogger.logEvent(AppInfo.GUID,
	                        ("RuntimeException: " + exp.getMessage()).getBytes(),
	                        EventLogger.ERROR);
				}
			}
		}
	}
}
