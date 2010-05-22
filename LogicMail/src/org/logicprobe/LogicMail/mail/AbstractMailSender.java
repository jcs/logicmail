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

import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.EventListenerList;

/**
 * Provides the base implementation for asynchronous
 * mail transmission.
 * 
 * <p>Most methods of this class that could talk to a server
 * are designed to return immediately.  The results should
 * be delivered later via an event sent to the appropriate
 * listener.  Since servers can give notifications independent
 * from specific requests, and since commands can fail,
 * clients should not expect an exact match between requests
 * and events.
 */
public abstract class AbstractMailSender {
	private EventListenerList listenerList = new EventListenerList();
	
	protected AbstractMailSender() {
	}
	
	/**
	 * Shutdown the mail sender.
	 * <p>
	 * Stops any threads used to manage stateful network connections.
	 * </p>
	 * 
	 * @param wait If true, wait for all pending requests to finish.
	 */
	public abstract void shutdown(boolean wait);
	
    /**
     * Requests the transmission of a message.
     * 
     * <p>Successful completion is indicated by a call to
     * {@link MailSenderListener#messageSent(MessageSentEvent)}.
     * 
     * @param envelope The envelope of the message to send.
     * @param message The message to send.
     */
	public abstract void requestSendMessage(MessageEnvelope envelope, Message message);

    /**
     * Adds a <tt>MailSenderListener</tt> to the mail sender.
     * 
     * @param l The <tt>MailSenderListener</tt> to be added.
     */
    public void addMailSenderListener(MailSenderListener l) {
        listenerList.add(MailSenderListener.class, l);
    }

    /**
     * Removes a <tt>MailSenderListener</tt> from the mail sender.
     * @param l The <tt>MailSenderListener</tt> to be removed.
     */
    public void removeMailSenderListener(MailSenderListener l) {
        listenerList.remove(MailSenderListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MailSenderListener</tt>s
     * that have been added to this mail sender.
     * 
     * @return All the <tt>MailSenderListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MailSenderListener[] getMailStoreListeners() {
        return (MailSenderListener[])listenerList.getListeners(MailSenderListener.class);
    }
    
    /**
     * Notifies all registered <tt>MailSenderListener</tt>s that
     * a message has sent.
     * 
     * @param envelope The envelope of the message that was sent.
     * @param message The message that was sent.
     * @param messageSource The raw source for the message that was sent.
     */
    protected void fireMessageSent(MessageEnvelope envelope, Message message, String messageSource) {
        Object[] listeners = listenerList.getListeners(MailSenderListener.class);
        MessageSentEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageSentEvent(this, envelope, message, messageSource);
            }
            ((MailSenderListener)listeners[i]).messageSent(e);
        }
    }
    
    /**
     * Notifies all registered <tt>MailSenderListener</tt>s that
     * there was an error when trying to send a message.
     * 
     * @param envelope The envelope of the message that could not be sent.
     * @param message The message that could not be sent.
     * @param exception 
     */
    protected void fireMessageSendFailed(MessageEnvelope envelope, Message message, Throwable exception) {
        Object[] listeners = listenerList.getListeners(MailSenderListener.class);
        MessageSentEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MessageSentEvent(this, envelope, message, exception);
            }
            ((MailSenderListener)listeners[i]).messageSendFailed(e);
        }
    }
}
