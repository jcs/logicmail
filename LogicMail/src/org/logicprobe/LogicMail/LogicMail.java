/*-
 * Copyright (c) 2009, Derek Konigsberg
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
import java.util.Hashtable;

import net.rim.blackberry.api.homescreen.HomeScreen;
import net.rim.device.api.i18n.Locale;
import net.rim.device.api.notification.NotificationsConstants;
import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.system.ApplicationManager;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.system.SystemListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;

import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.ui.BlankSeparatorField;
import org.logicprobe.LogicMail.ui.NavigationController;
import org.logicprobe.LogicMail.ui.NotificationHandler;
import org.logicprobe.LogicMail.ui.ThrobberField;
import org.logicprobe.LogicMail.conf.AccountConfig;
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
public final class LogicMail extends UiApplication {
    private boolean autoStart;
    private StartupSystemListener systemListener;
    private NavigationController navigationController;
    private Screen loadingScreen;

    /**
     * Instantiates a new instance of the application.
     * 
     * @param args arguments passed to the application's <code>main()</code> method
     */
    public LogicMail(String[] args) {
        for(int i=0; i<args.length; i++) {
            if(args[i].indexOf("autostartup") != -1) {
                autoStart = true;    			
            }
        }
        AppInfo.initialize(args);
    }

    /**
     * Run the application.
     */
    public void run() {
        if(autoStart) {
            if(ApplicationManager.getApplicationManager().inStartup()) {
                systemListener = new StartupSystemListener();
                this.addSystemListener(systemListener);
            }
            else {
                this.startupInitializationLater();
            }
        }
        else {
            logStartupAppInfo();

            createLoadingScreen();

            Thread loadingThread = new Thread() {
                public void run() {
                    // Load the configuration
                    MailSettings.getInstance().loadSettings();
                    // Set the language, if configured
                    String languageCode =
                        MailSettings.getInstance().getGlobalConfig().getLanguageCode();
                    if(languageCode != null && languageCode.length() > 0) {
                        try {
                            Locale.setDefault(Locale.get(languageCode));
                        } catch (Exception e) { }
                    }

                    // Initialize the data model explicitly
                    MailManager.initialize();

                    // Initialize the notification handler
                    NotificationHandler.getInstance().setEnabled(true);

                    // Initialize the navigation controller
                    navigationController = new NavigationController(LogicMail.this);

                    invokeLater(new Runnable() {
                        public void run() {
                            // Push the mail home screen and pop
                            // the loading screen
                            navigationController.displayMailHome();
                            popScreen(loadingScreen);
                            loadingScreen = null;
                        }
                    });
                }
            };

            pushScreen(loadingScreen);
            loadingThread.start();
        }
        enterEventDispatcher();
    }

    private void logStartupAppInfo() {
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
            buf.append("Platform: ");
            buf.append(AppInfo.getPlatformVersion());
            if(PlatformInfo.getInstance().hasTouchscreen()) {
                buf.append(' ');
                buf.append("(touch)");
            }
            buf.append("\r\n");
            EventLogger.logEvent(AppInfo.GUID, buf.toString().getBytes(), EventLogger.INFORMATION);
        }
    }

    private void createLoadingScreen() {
        loadingScreen = new MainScreen();
        int displayWidth = Display.getWidth();
        int displayHeight = Display.getHeight();
        int fieldSpacerSize = displayHeight / 24;
        Bitmap splashLogo = Bitmap.getBitmapResource("splash-logo.png");
        int throbberSize = displayWidth / 4;
        int fontHeight = Font.getDefault().getHeight();
        int spacerSize = (displayHeight / 2) - ((splashLogo.getHeight() + throbberSize + fontHeight) / 2) - fieldSpacerSize;
        if(spacerSize < 0) { spacerSize = 0; }

        loadingScreen.add(new BlankSeparatorField(spacerSize));
        loadingScreen.add(new BitmapField(splashLogo, Field.FIELD_HCENTER));
        loadingScreen.add(new BlankSeparatorField(fieldSpacerSize));
        loadingScreen.add(new ThrobberField(throbberSize, Field.FIELD_HCENTER));
        loadingScreen.add(new BlankSeparatorField(fieldSpacerSize));
        loadingScreen.add(new LabelField("Version " + AppInfo.getVersion(), Field.FIELD_HCENTER));
    }
    
    private class StartupSystemListener implements SystemListener {
        public void powerUp() {
            removeSystemListener(systemListener);
            startupInitialization();
        }
        
        public void powerOff() { }
        public void batteryGood() { }
        public void batteryLow() { }
        public void batteryStatusChange(int status) { }
    }
    
    private void startupInitializationLater() {
        invokeLater(new Runnable() {
            public void run() {
                startupInitialization();
            }
        });
    }
    
    private void startupInitialization() {
        // The BlackBerry has finished its startup process

        if(RuntimeStore.getRuntimeStore().remove(AppInfo.GUID) == null) {
            // Configure the rollover icons
            HomeScreen.updateIcon(AppInfo.getIcon(), 0);
            HomeScreen.setRolloverIcon(AppInfo.getRolloverIcon(), 0);
        }

        // Configure a notification source for each account
        MailSettings mailSettings = MailSettings.getInstance();
        mailSettings.loadSettings();
        int numAccounts = mailSettings.getNumAccounts();
        Hashtable eventSourceMap = new Hashtable(numAccounts);
        for(int i=0; i<numAccounts; i++) {
            AccountConfig accountConfig = mailSettings.getAccountConfig(i);
            LogicMailEventSource eventSource =
                new LogicMailEventSource(accountConfig.getAcctName(), accountConfig.getUniqueId());
            NotificationsManager.registerSource(
                    eventSource.getEventSourceId(),
                    eventSource,
                    NotificationsConstants.CASUAL);
            eventSourceMap.put(new Long(accountConfig.getUniqueId()), eventSource);
        }

        // Save the registered event sources in the runtime store
        RuntimeStore.getRuntimeStore().put(AppInfo.GUID, eventSourceMap);
        
        //Exit the application.
        System.exit(0);
    }
} 
