/*-
 * Copyright (c) 2010, Derek Konigsberg
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

/**
 * Object for mail store request events. 
 */
public class RequestEvent extends MailStoreEvent {
    private int type;
    private Object[] params;
    private Throwable exception;
    
    public final static int TYPE_FOLDER_TREE = 0;
    public final static int TYPE_FOLDER_EXPUNGE = 1;
    public final static int TYPE_FOLDER_STATUS = 2;
    public final static int TYPE_FOLDER_MESSAGES_RANGE = 3;
    public final static int TYPE_FOLDER_MESSAGES_SET = 4;
    public final static int TYPE_FOLDER_MESSAGES_RECENT = 5;
    public final static int TYPE_MESSAGE = 6;
    public final static int TYPE_MESSAGE_PARTS = 7;
    public final static int TYPE_MESSAGE_DELETE = 8;
    public final static int TYPE_MESSAGE_UNDELETE = 9;
    public final static int TYPE_MESSAGE_ANSWERED = 10;
    public final static int TYPE_MESSAGE_APPEND = 11;
    public final static int TYPE_MESSAGE_COPY = 12;
    
    /**
     * Creates a new instance of MessageEvent.
     * 
     * @param source the source of the event
     * @param type the type of the event
     * @param params the parameters passed along with the request.
     * @param exception the exception that caused the request to fail, if applicable.
     */
    public RequestEvent(Object source, int type, Object[] params, Throwable exception) {
        super(source);
        this.type = type;
        this.params = params;
        this.exception = exception;
    }

    /**
     * Gets the type of this request event.
     * 
     * @return Event type.
     */
    public int getType() {
        return this.type;
    }
    
    /**
     * Gets the parameters passed along with the request.
     * 
     * @return the parameters passed along with the request
     */
    public Object[] getParameters() {
        return params;
    }
    
    /**
     * Gets the exception that caused the request to fail, if applicable.
     * 
     * @return the exception that caused the request to fail, if applicable
     */
    public Throwable getException() {
        return exception;
    }
}
