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

import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * This singleton handles registration of listeners for
 * UI classes that need to interact with the mail connection
 * handlers.  It also provides a means for the mail
 * connection handlers to fire events handled by
 * those listeners.
 */
public class MailConnectionManager {
	private static MailConnectionManager instance = null;
    private EventListenerList listenerList = new EventListenerList();
	
	private MailConnectionManager() {
	}
	
    /**
     * Gets the connection manager instance.
     * @return Instance
     */
    public static synchronized MailConnectionManager getInstance() {
        if(instance == null) {
            instance = new MailConnectionManager();
        }
        return instance;
    }

    /**
     * Adds a <tt>MailConnectionListener</tt> to the mail store.
     * 
     * @param l The <tt>MailConnectionListener</tt> to be added.
     */
    public void addMailConnectionListener(MailConnectionListener l) {
        listenerList.add(MailConnectionListener.class, l);
    }

    /**
     * Removes a <tt>MailConnectionListener</tt> from the mail store.
     * @param l The <tt>MailConnectionListener</tt> to be removed.
     */
    public void removeMailConnectionListener(MailConnectionListener l) {
        listenerList.remove(MailConnectionListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MailConnectionListener</tt>s
     * that have been added to this mail store.
     * 
     * @return All the <tt>MailConnectionListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MailConnectionListener[] getMailConnectionListener() {
        return (MailConnectionListener[])listenerList.getListeners(MailConnectionListener.class);
    }

    /**
     * Notifies all registered <tt>MailConnectionListener</tt>s
     * of a connection state change.
     * 
     * @param connectionConfig The configuration for the connection that generated this event.
     * @param state The new connection state.
     */
    void fireMailConnectionStateChanged(ConnectionConfig connectionConfig, int state) {
    	Object[] listeners = listenerList.getListeners(MailConnectionListener.class);
    	MailConnectionStateEvent e = null;
    	for(int i=0; i<listeners.length; i++) {
    		if(e == null) {
    			e = new MailConnectionStateEvent(this, connectionConfig, state);
    		}
    		((MailConnectionListener)listeners[i]).mailConnectionStateChanged(e);
    	}
    }
    
    /**
     * Notifies all registered <tt>MailConnectionListener</tt>s
     * of an updated status message.
     * 
     * @param connectionConfig The configuration for the connection that generated this event.
     * @param message The status message.
     */
    void fireMailConnectionStatus(ConnectionConfig connectionConfig, String message) {
    	Object[] listeners = listenerList.getListeners(MailConnectionListener.class);
    	MailConnectionStatusEvent e = null;
    	for(int i=0; i<listeners.length; i++) {
    		if(e == null) {
    			e = new MailConnectionStatusEvent(this, connectionConfig, message);
    		}
    		((MailConnectionListener)listeners[i]).mailConnectionStatus(e);
    	}
    }

    /**
     * Notifies all registered <tt>MailConnectionListener</tt>s
     * of an updated status message with a progress percentage.
     * 
     * @param connectionConfig The configuration for the connection that generated this event.
     * @param message The status message.
     * @param progress The progress percentage.
     */
    void fireMailConnectionStatus(ConnectionConfig connectionConfig, String message, int progress) {
    	Object[] listeners = listenerList.getListeners(MailConnectionListener.class);
    	MailConnectionStatusEvent e = null;
    	for(int i=0; i<listeners.length; i++) {
    		if(e == null) {
    			e = new MailConnectionStatusEvent(
    					this, connectionConfig, message,
    					MailConnectionStatusEvent.PROGRESS_MEASURED, progress);
    		}
    		((MailConnectionListener)listeners[i]).mailConnectionStatus(e);
    	}
    }
    
    /**
     * Notifies all registered <tt>MailConnectionListener</tt>s
     * of an error with the mail connection.
     * 
     * @param connectionConfig The configuration for the connection that generated this event.
     * @param message The error message.
     */
    void fireMailConnectionError(ConnectionConfig connectionConfig, String message) {
    	Object[] listeners = listenerList.getListeners(MailConnectionListener.class);
    	MailConnectionStatusEvent e = null;
    	for(int i=0; i<listeners.length; i++) {
    		if(e == null) {
    			e = new MailConnectionStatusEvent(this, connectionConfig, message);
    		}
    		((MailConnectionListener)listeners[i]).mailConnectionError(e);
    	}
    }

    /**
     * Requests registered <tt>MailConnectionListener</tt>s
     * for handling of a login operation.
     * 
     * @param e Event data object
     */
    void fireMailConnectionLogin(MailConnectionLoginEvent e) {
    	Object[] listeners = listenerList.getListeners(MailConnectionListener.class);
    	for(int i=0; i<listeners.length; i++) {
    		((MailConnectionListener)listeners[i]).mailConnectionLogin(e);
    	}
    }
}
