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

package org.logicprobe.LogicMail.mail;

import java.io.IOException;

import net.rim.device.api.system.EventLogger;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.util.Queue;

/**
 * This class is responsible for managing the lifecycle of a mail
 * server network connection.  It also encapsulates the necessary
 * hooks for UI interactions.
 */
public abstract class AbstractMailConnectionHandler {
	private MailClient client;
	private ConnectionThread connectionThread;
	private int state;
	private Queue requestQueue;
	private MailConnectionHandlerListener listener;
	
	// TODO: Handle the state transitions for shutdown
	
	// The various states of a mail connection
	public static final int STATE_CLOSED   = 0;
	public static final int STATE_OPENING  = 1;
	public static final int STATE_OPENED   = 2;
	public static final int STATE_REQUESTS = 3;
	public static final int STATE_IDLE     = 4;
	public static final int STATE_CLOSING  = 5;

	protected AbstractMailConnectionHandler(MailClient client) {
		this.client = client;
		this.connectionThread = new ConnectionThread();
		this.state = STATE_CLOSED;
		this.requestQueue = new Queue();
		this.listener = null;
	}

	/**
	 * Start the mail connection handler thread.
	 */
	public void start() {
		if(!connectionThread.isAlive()) {
			connectionThread.start();
		}
    }

	/**
	 * Shutdown the mail connection handler thread.
	 * 
	 * @param wait If true, wait for all pending requests to finish.
	 */
	public void shutdown(boolean wait) {
		if(!connectionThread.isAlive()) {
			return;
		}
		if(wait) {
			Object element;
			synchronized(requestQueue) {
				element = requestQueue.element();
			}
			while(element != null) {
				synchronized(requestQueue) {
					element = requestQueue.element();
				}
				Thread.yield();
			}
		}
		connectionThread.shutdown();
		try {
			connectionThread.join();
		} catch (InterruptedException e) { }
	}

	/**
	 * Sets the listener for events from this class.
	 * The listener is the class that handles results from requests.
	 * 
	 * @param listener The listener
	 */
	public void setListener(MailConnectionHandlerListener listener) {
		this.listener = listener;
	}

	/**
	 * Gets the listener for events from this class.
	 * 
	 * @return The listener
	 */
	protected MailConnectionHandlerListener getListener() {
		return this.listener;
	}
	
	/**
	 * Add a request to the queue.
	 * 
	 * @param type Type of request
	 * @param params Parameters passed to the corresponding
	 *               AbstractMailStore.requestXXXX() method. 
	 */
	public void addRequest(int type, Object[] params) {
		if(type >= 0 && type <= 5) {
			synchronized(requestQueue) {
				requestQueue.add(new Object[] {new Integer(type), params});
				requestQueue.notifyAll();
			}
		}
	}
	
	/**
	 * Handles the CLOSED state.
	 */
	private void handleClosedConnection() {
		synchronized(requestQueue) {
			if(requestQueue.element() != null) {
				setConnectionState(STATE_OPENING);
			}
			else if(!connectionThread.isShutdown()) {
				try {
					requestQueue.wait();
				} catch (InterruptedException e) { }
			}
		}
	}
	
    /**
     * Handles the OPENING state.
     * Open a new connection, and authenticates with the server.
     * This method should typically establish a socket connection and
     * then send the protocol-specific login commands.
     * 
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
	private void handleOpeningConnection() throws IOException, MailException {
		if(checkLogin(client)) {
			if(client.open()) {
				setConnectionState(STATE_OPENED);
				return;
			}
			// Add in handling for failed connections and
			// for bad username/password.
		}
		// Unable to open, so transition to closing
        setConnectionState(STATE_CLOSING);
	}
    
	/**
	 * Handles the OPENED state.
	 * 
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
	 */
	private void handleOpenedConnection() throws IOException, MailException {
		synchronized(requestQueue) {
			if(requestQueue.element() != null) {
				setConnectionState(STATE_REQUESTS);
			}
			else {
				setConnectionState(STATE_IDLE);
			}
		}
	}

	/**
     * Handles the REQUESTS state to process any pending server requests.
     * 
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
	protected abstract void handlePendingRequests() throws IOException, MailException;
    
    /**
     * Handles the IDLE connection state which occurs after all
     * pending requests have been handled.  This method is called
     * at a periodic interval when no requests are pending.
     * 
     * @param idleTime The amount of time this connection has been idle.
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
	private void handleIdleConnection(long idleTime) throws IOException, MailException {
		// Do idle stuff
		
		synchronized(requestQueue) {
			if(requestQueue.element() != null) {
				setConnectionState(STATE_REQUESTS);
			}
			else if(connectionThread.isShutdown()) {
				setConnectionState(STATE_CLOSING);
			}
			else {
				try {
					requestQueue.wait();
				} catch (InterruptedException e) { }
			}
		}
		//System.err.println("-->idle (EXIT)");
	}
    
    /**
     * Handles the CLOSING state to close an existing connection.
     * This method should send protocol-specific logout commands and
     * then terminate the connection.
     * 
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
     */
	private void handleClosingConnection() throws IOException, MailException {
		//System.err.println("-->closing (ENTER)");
		client.close();
		setConnectionState(STATE_CLOSED);
		//System.err.println("-->closing (EXIT)");
	}

	/**
	 * Gets the current connection state.
	 * 
	 * @return Connection state
	 */
	public synchronized int getConnectionState() {
		return this.state;
	}
	
	/**
	 * Sets the current connection state.
	 * 
	 * @param state Connection state
	 */
	protected synchronized void setConnectionState(int state) {
		if(state >= STATE_CLOSED && state <= STATE_CLOSING) {
			this.state = state;
		}
	}
	
	/**
	 * Gets the request queue.
	 * Any use of the queue must be synchronized.
	 * 
	 * @return Request queue
	 */
	protected Queue getRequestQueue() {
		return this.requestQueue;
	}
	
//	/**
//	 * Show a status message.
//	 * 
//	 * @param message The message to show
//	 */
//	private void showStatus(String message) {
//		MailConnectionManager.getInstance().fireMailConnectionStatus(message);
//	}
	
	/**
	 * Check the login information, requesting a prompt if necessary.
	 * 
	 * @return True if the information was validated, false otherwise.
	 */
	private boolean checkLogin(MailClient client) {
        String username = client.getUsername();
        String password = client.getPassword();
        // If the username and password are not null,
        // but are empty, request login information.
        if((username != null && password != null) &&
           (username.trim().equals("") || password.trim().equals(""))) {

        	MailConnectionLoginEvent e = new MailConnectionLoginEvent(this, username, password);
    		MailConnectionManager.getInstance().fireMailConnectionLogin(e);
        	
           if(!e.isCanceled()) {
               client.setUsername(e.getUsername());
               client.setPassword(e.getPassword());
               return true;
           }
           else {
        	   return false;
           }
        }
        else {
        	return true;
        }
	}

	/**
	 * Thread for the standard mail connection lifecycle.
	 */
	private class ConnectionThread extends Thread {
		private boolean shutdown = false;
		private boolean shutdownComplete = false;
		
		public ConnectionThread() {
			super("ConnectionThread");
		}
		
		public void shutdown() {
			//System.err.println("-->shutdown (ENTER)");
			synchronized(this) {
				shutdown = true;
			}
			synchronized(requestQueue) {
				requestQueue.notifyAll();
			}
			//System.err.println("-->shutdown (EXIT)");
		}
		
		public boolean isShutdown() {
			boolean result;
			synchronized(this) {
				result = shutdown;
			}
			return result;
		}
		
        public void run() {
        	while(!shutdownComplete) {
        		switch(state) {
        		case STATE_CLOSED:
        			handleClosedConnection();
        			if(state == STATE_CLOSED && shutdown) {
        				shutdownComplete = true;
        			}
        			break;
        		case STATE_OPENING:
        			try {
        				handleOpeningConnection();
        			} catch (IOException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			} catch (MailException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			}
        			break;
        		case STATE_OPENED:
        			try {
        				handleOpenedConnection();
        			} catch (IOException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			} catch (MailException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			}
        			break;
        		case STATE_REQUESTS:
        			try {
        				handlePendingRequests();
        			} catch (IOException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			} catch (MailException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			}
        			break;
        		case STATE_IDLE:
        			try {
        				handleIdleConnection(1000);
        			} catch (IOException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			} catch (MailException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			}
        			break;
        		case STATE_CLOSING:
        			try {
        				handleClosingConnection();
        			} catch (IOException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			} catch (MailException e) {
        				EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);
        			} finally {
        				if(shutdown) {
        					shutdownComplete = true;
        				}
        			}
        			break;
        		}
        	}
        }
	}
}
