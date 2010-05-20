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

import net.rim.device.api.i18n.ResourceBundle;

import org.logicprobe.LogicMail.LogicMailResource;

/**
 * Exception class for recipient errors that can occur while sending messages.
 * These errors are recoverable with user intervention, therefore they contain
 * any relevant information to let the user know which recipient caused the
 * error.
 */
public class RecipientException extends MailException {
    private static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    
    public static final int RECIPIENT_TO = 10;
    public static final int RECIPIENT_CC = 11;
    public static final int RECIPIENT_BCC = 12;
    
    private final String address;
    
    /**
     * Instantiates a new recipient exception.
     *
     * @param message the message
     * @param fatal the fatal
     * @param cause the cause
     */
    public RecipientException(int recipientType, String address) {
        super(resources.getString(LogicMailResource.ERROR_RECIPIENT) + ' ' + address, false, recipientType);
        this.address = address;
    }

    /**
     * Gets the address of the recipient that caused the error.
     *
     * @return the recipient address
     */
    public String getAddress() {
        return address;
    }
}
