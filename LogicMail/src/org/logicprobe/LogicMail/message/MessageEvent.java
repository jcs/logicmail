/*-
 * Copyright (c) 2007, Derek Konigsberg
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

package org.logicprobe.LogicMail.message;

import org.logicprobe.LogicMail.util.EventObject;

/**
 * Object for Message events
 */
public class MessageEvent extends EventObject {
    /** Message transformed to a Reply message */
    public static final int TO_REPLY = 1;
    /** Message transformed to a Reply All message */
    public static final int TO_REPLYALL = 2;
    /** Message transformed to a Forwarded message */
    public static final int TO_FORWARD = 3;
    
    private int action;
    private Message transformedMessage;
    
    /**
     * Creates a new instance of MessageEvent
     * @param source Source of the event
     * @param action Action for the event
     */
    public MessageEvent(Object source, int action) {
        super(source);
        this.action = action;
        this.transformedMessage = null;
    }
    
    /**
     * Creates a new instance of MessageEvent
     * @param source Source of the event
     * @param action Action for the event
     * @param transformedMessage The new message that caused this event.
     */
    public MessageEvent(Object source, int action, Message transformedMessage) {
        super(source);
        this.action = action;
        this.transformedMessage = transformedMessage;
    }

    /**
     * Gets the action for the event
     */
    public int getAction() {
        return action;
    }
    
    /**
     * Gets the new message that caused this event
     */
    public Message getTransformedMessage() {
        return transformedMessage;
    }
}
