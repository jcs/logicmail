/*-
 * Copyright (c) 2006, Derek Konigsberg
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

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class encapsulates all the data to represent an E-Mail message.
 * <p>
 * It is intended for use as a short lived lightweight container for moving
 * data between the protocol layer and the rest of the application.
 * </p>
 */
public class Message {
    private final MimeMessagePart structure;
    private final Hashtable content = new Hashtable();
    private final boolean complete;
    
    /**
     * Creates a new instance of Message
     * @param structure The structured message body tree
     * @param true, if the message data is complete
     */
    public Message(MimeMessagePart structure, boolean complete) {
        this.structure = structure;
        this.complete = complete;
    }
    
    /**
     * Creates a new instance of Message
     * @param structure The structured message body tree
     */
    public Message(MimeMessagePart structure) {
        this(structure, true);
    }
    
    /**
     * Checks if the message complete flag was set.
     * This should always be true, unless this message was created by parsing
     * incomplete MIME source.
     *
     * @return true, if the message is complete
     */
    public boolean isComplete() {
        return complete;
    }
    
    /**
     * Get the message body
     * @return Body
     */
    public MimeMessagePart getStructure() {
        return structure;
    }
    
    public void putContent(MimeMessagePart mimeMessagePart, MimeMessageContent mimeMessageContent) {
    	content.put(mimeMessagePart, mimeMessageContent);
    }
    
    public MimeMessageContent getContent(MimeMessagePart mimeMessagePart) {
    	return (MimeMessageContent)content.get(mimeMessagePart);
    }
    
    public MimeMessageContent[] getAllContent() {
		MimeMessageContent[] result = new MimeMessageContent[content.size()];
		Enumeration e = content.keys();
		int i = 0;
    	while(e.hasMoreElements()) {
    		result[i++] = (MimeMessageContent)content.get(e.nextElement());
    	}
		return result;
    }
    
    public Hashtable getContentMap() {
    	Hashtable result = new Hashtable();
    	Enumeration e = content.keys();
    	while(e.hasMoreElements()) {
    		MimeMessagePart part = (MimeMessagePart)e.nextElement();
    		result.put(part, content.get(part));
    	}
    	return result;
    }
}
