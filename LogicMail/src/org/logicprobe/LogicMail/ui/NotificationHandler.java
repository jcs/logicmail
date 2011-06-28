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
package org.logicprobe.LogicMail.ui;

import org.logicprobe.LogicMail.util.PlatformUtils;

/**
 * Handles new message notification through the various
 * notification mechanisms of the BlackBerry.
 */
public abstract class NotificationHandler {
	private static NotificationHandler instance = null;


    private static String[] concreteClasses = {
        "org.logicprobe.LogicMail.ui.NotificationHandlerBB60",
        "org.logicprobe.LogicMail.ui.NotificationHandlerBB46",
        "org.logicprobe.LogicMail.ui.NotificationHandlerBB45"
    };

    /**
     * Gets the NotificationHandler instance.
     * 
     * @return Single instance of NotificationHandler
     */
    public static synchronized NotificationHandler getInstance() {
        if(instance == null) {
            instance = (NotificationHandler)PlatformUtils.getFactoryInstance(concreteClasses);
        }
        return instance;
    }
    
    /**
     * Sets whether notifications are enabled.
     * 
     * @param isEnabled True to enable notifications, false to disable
     */
    public abstract void setEnabled(boolean isEnabled);
    
    /**
     * Shutdown the listener and unsubscribe from any system events.
     */
    public abstract void shutdown();
	
    /**
     * Cancel all existing notifications.
     */
    public abstract void cancelNotification();
}
