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

package org.logicprobe.LogicMail;

import java.util.Calendar;

import net.rim.blackberry.api.homescreen.HomeScreen;
import net.rim.device.api.notification.NotificationsConstants;
import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.system.ApplicationManager;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.UiApplication;
import org.logicprobe.LogicMail.ui.MailHomeScreen;
import org.logicprobe.LogicMail.ui.NotificationHandler;
import org.logicprobe.LogicMail.conf.MailSettings;

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
public class LogicMail extends UiApplication {
    /**
     * Instantiates a new instance of the application.
     * 
     * @param autoStart True if this is the autostart instance, false for normal startup
     */
    public LogicMail(boolean autoStart) {
    	if(autoStart) {
    		doAutoStart();
    	}
    	else {
	        // Load the configuration
	        MailSettings.getInstance().loadSettings();
	
	        // Log application startup information
	        if(EventLogger.getMinimumLevel() >= EventLogger.INFORMATION) {
	            StringBuffer buf = new StringBuffer();
	            buf.append("Application startup\r\n");
	            buf.append("Date: ");
	            buf.append(Calendar.getInstance().getTime().toString());
	            buf.append("\r\n");
	            buf.append("Name: ");
	            buf.append(AppInfo.getName());
	            buf.append("\r\n");
	            buf.append("Version: ");
	            buf.append(AppInfo.getVersion());
	            buf.append("\r\n");
	            EventLogger.logEvent(AppInfo.GUID, buf.toString().getBytes(), EventLogger.INFORMATION);
	        }

	        // Initialize the notification handler
	        NotificationHandler.getInstance().setEnabled(true);
	        
	        // Push the mail home screen
	        pushScreen(new MailHomeScreen());
    	}
    }

    /**
     * Run the application.
     */
    public void run() {
    	enterEventDispatcher();
    }
    
    /** The constant event source object. */
    private static final Object eventSource = new Object() {
		public String toString() {
	    	return "LogicMail New Message";
		}
	};
	
    /**
     * Method to execute in autostart mode.
     */
    private void doAutoStart() {
        invokeLater(new Runnable()
        {
            public void run()
            {
                ApplicationManager myApp = ApplicationManager.getApplicationManager();
                boolean keepGoing = true;

                while (keepGoing)
                {
                    if (myApp.inStartup())
                    {
                        try { Thread.sleep(1000); }
                        catch (Exception ex) { }
                    }
                    else
                    {
                        // The BlackBerry has finished its startup process
                        // Configure the rollover icons
                        HomeScreen.updateIcon(AppInfo.getIcon(), 0);
                        HomeScreen.setRolloverIcon(AppInfo.getRolloverIcon(), 0);
                        
                        // Configure the notification source
                        NotificationsManager.registerSource(AppInfo.GUID, eventSource, NotificationsConstants.CASUAL);
                        
                        keepGoing = false;
                    }
                 }
                 //Exit the application.
                 System.exit(0);
            }
        });    	
    }
} 
