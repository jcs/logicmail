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

/**
 * Abstract representation of a message part
 */
public abstract class MessagePart {
    private String mimeType;
    private String mimeSubtype;

    /** Creates a new instance of MessagePart */
    protected MessagePart(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Accept a visitor on this message part.
     * @param visitor The visitor instance
     */
    public abstract void accept(MessagePartVisitor visitor);
    
    /**
     * Get the MIME type for this part
     * @return The "type" part of "type/subtype"
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Get the MIME subtype for this part
     * @return The "subtype" part of "type/subtype"
     */
    public String getMimeSubtype() {
        return mimeSubtype;
    }

    /**
     * Set the MIME subtype for this part.
     * This is only intended for use by concrete implementations
     * of this class.
     * @return The "subtype" part of "type/subtype"
     */
    protected void setMimeSubtype(String mimeSubtype) {
        this.mimeSubtype = mimeSubtype;
    }
}
