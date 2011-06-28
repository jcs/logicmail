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
 * Interface used by the connection handler to manage requests. 
 */
public interface ConnectionHandlerRequest {
    /**
     * Sets whether this is a deliberate request.
     * Deliberate requests are created in response to direct user action,
     * and thus should have status reported. Non-deliberate requests are
     * created by entirely background processes, and the user should
     * not be notified of their status.
     */
    void setDeliberate(boolean deliberate);
    
    /**
     * Checks if this is a deliberate request.
     */
    boolean isDeliberate();
    
    /**
     * Checks if this is an administrative request.  Administrative requests
     * change client configuration parameters, and do not require an active
     * network connection to function.
     */
    boolean isAdministrative();
    
    /**
     * Requests are normally responsible for showing their own status messages.
     * This method is called prior to processing a request, so that its initial
     * status message can be show before {@link #execute(MailClient)} is called.
     */
    void showInitialStatus();
    
    /**
     * Called by the connection handler to tell a request to execute its
     * mail client operations.
     */
    void execute(MailClient client) throws IOException, MailException;
    
    /**
     * Notifies a request that it has failed.
     * This can be caused either by an exception being thrown during its
     * execution, or another error causing it to be removed from the queue.
     */
    void notifyConnectionRequestFailed(Throwable exception, boolean isFinal);
}
