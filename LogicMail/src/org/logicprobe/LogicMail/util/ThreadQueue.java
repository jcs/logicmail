package org.logicprobe.LogicMail.util;

// TODO: Write rigorous tests for this class

public class ThreadQueue {
	private Queue runnableQueue;
	private ThreadQueueThread threadQueueThread;
	
	public ThreadQueue() {
		runnableQueue = new Queue();
	}
	
	public void shutdown(boolean wait) {
		// TODO: Implement thread queue shutdown
	}
	
	public void invokeLater(Runnable runnable) {
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
			}
			threadQueueThread = new ThreadQueueThread(runnable);
			threadQueueThread.start();
		}
		
	}
	
	private class ThreadQueueThread extends Thread {
		public ThreadQueueThread(Runnable runnable) {
			super(runnable);
		}

		public void run() {
			Thread.yield();
			super.run();
			synchronized(runnableQueue) {
				Object nextRunnableObject = runnableQueue.element();
				if(nextRunnableObject != null) {
					runnableQueue.remove();
					Runnable nextRunnable = (Runnable)nextRunnableObject;
					threadQueueThread = new ThreadQueueThread(nextRunnable);
					threadQueueThread.start();
				}
			}
		}
	}
}
