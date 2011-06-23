/*-
 * Copyright (c) 2011, Derek Konigsberg
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

/**
 * This is a special request for the purpose of enabling or disabling the idle
 * mode behavior of a mail client. If the mail client does not support a
 * special idle mode, this request will do nothing. It should only be used at
 * the beginning and end of batch operations, to improve efficiency.
 */
public class NetworkClientIdleModeRequest extends NetworkMailStoreRequest implements MailStoreRequest {
    private final boolean idleEnabled;
    /**
     * Instantiates a new idle mode control request.
     *
     * @param mailStore the mail store to process this request
     * @param idleEnabled whether or not the idle mode should be enabled
     */
    NetworkClientIdleModeRequest(NetworkMailStore mailStore, boolean idleEnabled) {
        super(mailStore);
        this.idleEnabled = idleEnabled;
        setDeliberate(false);
    }

    public boolean isIdleEnabled() {
        return idleEnabled;
    }
    
    protected String getInitialStatus() {
        return null;
    }
    
    public void execute(MailClient client) throws IOException, MailException {
        IncomingMailClient incomingClient = (IncomingMailClient)client;
        if(incomingClient.hasIdle()) {
            incomingClient.setIdleEnabled(idleEnabled);
        }
        fireMailStoreRequestComplete();
    }
}
