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
	private int retryCount;
	private boolean invalidLogin;
	
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
		this.retryCount = 0;
		this.invalidLogin = false;
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
		synchronized(requestQueue) {
			requestQueue.add(new Object[] {new Integer(type), params});
			requestQueue.notifyAll();
		}
	}
	
	/**
	 * Handles the CLOSED state.
	 */
	private void handleClosedConnection() {
		showStatus(null);
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
		showStatus("Opening connection...");
		if(checkLogin(client)) {
			if(client.open()) {
				invalidLogin = false;
				setConnectionState(STATE_OPENED);
				MailConnectionManager.getInstance().fireMailConnectionStateChanged(
						client.getConnectionConfig(),
						MailConnectionStateEvent.STATE_CONNECTED);
				return;
			}
			else {
				invalidLogin = true;
				return;
			}
		}
		// Unable to open, so transition to closing and clear the queue
        setConnectionState(STATE_CLOSING);
        synchronized(requestQueue) {
        	requestQueue.clear();
        }
	}
    
	/**
	 * Handles the OPENED state.
	 * 
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
	 */
	private void handleOpenedConnection() throws IOException, MailException {
		showStatus(null);
		retryCount = 0;
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
	private void handlePendingRequests() throws IOException, MailException {
		Queue requestQueue = getRequestQueue();
		Object element;
		synchronized(requestQueue) {
			element = requestQueue.element();
		}
		while(element != null) {
			synchronized (requestQueue) {
				requestQueue.remove();
			}
			Object[] request = (Object[])element;
			int type = ((Integer)request[0]).intValue();
			Object[] params = (Object[])request[1];
			
			// Handle the specific request
			showStatus("Processing requests...");
			handleRequest(type, params);
			
			synchronized(requestQueue) {
				element = requestQueue.element();
			}
		}
		setConnectionState(STATE_IDLE);
	}

	/**
	 * Handles a specific request during the REQUESTS state.
	 * Subclasses should implement this to dispatch requests for all
	 * the request types they know how to handle.
	 * 
	 * @param type Type identifier for the request.
	 * @param params Parameters for the request.
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
	 */
	protected abstract void handleRequest(int type, Object[] params) throws IOException, MailException;
	
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
		showStatus(null);
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
		showStatus("Closing connection...");
		try { client.close(); } catch (IOException e) {} catch (MailException e) {}
		setConnectionState(STATE_CLOSED);
		MailConnectionManager.getInstance().fireMailConnectionStateChanged(
				client.getConnectionConfig(),
				MailConnectionStateEvent.STATE_DISCONNECTED);
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
	
	/**
	 * Show a status message.
	 * 
	 * @param message The message to show
	 */
	private void showStatus(String message) {
		MailConnectionManager.getInstance().fireMailConnectionStatus(client.getConnectionConfig(), message);
	}
	
	/**
	 * Show an error message.
	 * 
	 * @param message The message to show
	 */
	private void showError(String message) {
		MailConnectionManager.getInstance().fireMailConnectionError(client.getConnectionConfig(), message);
	}
	
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
        if(invalidLogin || ((username != null && password != null) &&
           (username.trim().equals("") || password.trim().equals("")))) {

        	MailConnectionLoginEvent e = new MailConnectionLoginEvent(
        			this,
        			client.getConnectionConfig(),
        			username,
        			password);
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
	 * Handles <tt>IOException</tt>s that occur during a connection.
	 * These are typically due to network errors, and are handled
	 * in different ways depending on the connection state:
	 * <ul>
	 * <li><b>OPENING</b> - Connection failure, switch to CLOSED and clear the queue.</li>
	 * <li><b>CLOSING</b> - Disconnection failure, switch to CLOSED and clear the queue.</li>
	 * <li><b>OPENED/REQUESTS/IDLE</b> - Connection timeout or loss, switch to OPENING to attempt reconnection</li>
	 * </ul>
	 * 
	 * @param e Exception data.
	 */
	private void handleIOException(IOException e) {
		EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);

		int state = getConnectionState();
		
		if(state == STATE_OPENING || state == STATE_CLOSING || retryCount < 2) {
			// Switch to the CLOSING state and clear the request queue.
			synchronized (requestQueue) {
				setConnectionState(STATE_CLOSING);
				requestQueue.clear();
			}
		}
		else {
			retryCount++;
			setConnectionState(STATE_OPENING);
		}
		showError(e.getMessage());
	}
	
	/**
	 * Handles <tt>MailException</tt>s that occur during a connection.
	 * These are typically due to protocol errors, and can often be recovered
	 * from.  If they are recoverable, then they are not fatal to the connection.
	 * Otherwise, they result in a graceful logout and connection termination.
	 * 
	 * @param e Exception data.
	 */
	private void handleMailException(MailException e) {
		EventLogger.logEvent(AppInfo.GUID, e.toString().getBytes(), EventLogger.ERROR);

		if(e.isFatal()) {
			// Switch to the CLOSING state and clear the request queue.
			synchronized (requestQueue) {
				setConnectionState(STATE_CLOSING);
				requestQueue.clear();
			}
		}
		showError(e.getMessage());
	}
	
	/**
	 * Handles any other <tt>Throwable</tt> that occurs during a connection.
	 * These include any other unknown exception, and are usually caused by
	 * software bugs that occur during server response parsing.  Since it is
	 * impossible to know what their cause is, they are handled similarly
	 * to a fatal <tt>MailException</tt>.
	 * 
	 * @param t Throwable data.
	 */
	private void handleThrowable(Throwable t) {
		EventLogger.logEvent(AppInfo.GUID, t.toString().getBytes(), EventLogger.ERROR);

		// Switch to the CLOSING state and clear the request queue.
		synchronized (requestQueue) {
			setConnectionState(STATE_CLOSING);
			requestQueue.clear();
		}
		showError(t.getMessage());
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
			synchronized(this) {
				shutdown = true;
			}
			synchronized(requestQueue) {
				requestQueue.notifyAll();
			}
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
        		try {
	        		switch(state) {
	        		case STATE_CLOSED:
	        			handleClosedConnection();
	        			if(state == STATE_CLOSED && shutdown) {
	        				shutdownComplete = true;
	        			}
	        			break;
	        		case STATE_OPENING:
	    				handleOpeningConnection();
	        			break;
	        		case STATE_OPENED:
	    				handleOpenedConnection();
	        			break;
	        		case STATE_REQUESTS:
	    				handlePendingRequests();
	        			break;
	        		case STATE_IDLE:
	    				handleIdleConnection(1000);
	        			break;
	        		case STATE_CLOSING:
	    				handleClosingConnection();
	        			break;
	        		}
        		} catch (IOException e) {
        			handleIOException(e);
        		} catch (MailException e) {
        			handleMailException(e);
        		} catch (Throwable t) {
        			handleThrowable(t);
        		} finally {
        			if(state == STATE_CLOSING && shutdown) {
        				shutdownComplete = true;
        			}
        		}
        	}
        }
	}
}