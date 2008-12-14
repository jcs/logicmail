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

package org.logicprobe.LogicMail;

import net.rim.device.api.system.EventLogger;

/*
 * Logging levels:
 *  EventLogger.ALWAYS_LOG   = 0
 *  EventLogger.SEVERE_ERROR = 1
 *  EventLogger.ERROR        = 2
 *  EventLogger.WARNING      = 3
 *  EventLogger.INFORMATION  = 4
 *  EventLogger.DEBUG_INFO   = 5
 */

/**
 * Main class for the application.
 */
public class LogicMailStartup {
    /**
     * The main method.
     * 
     * @param args The arguments
     */
    public static void main(String[] args) {
        // Register with the event logger
        EventLogger.register(AppInfo.GUID, "LogicMail", EventLogger.VIEWER_STRING);
        
        LogicMail app;
        if (args.length > 0 && args[0].equals("autostartup")) {
	        app = new LogicMail(true);
    	}
        else {
	        app = new LogicMail(false);
    	}
        app.run();
    }
}
