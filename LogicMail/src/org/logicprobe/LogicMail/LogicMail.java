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

import javax.microedition.io.file.FileSystemRegistry;

import net.rim.blackberry.api.homescreen.HomeScreen;
import net.rim.device.api.i18n.Locale;
import net.rim.device.api.notification.NotificationsConstants;
import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.synchronization.SyncManager;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.ApplicationManager;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.Display;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.system.SystemListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.UiEngine;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;

import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.ui.BlankSeparatorField;
import org.logicprobe.LogicMail.ui.HomeScreenPopup;
import org.logicprobe.LogicMail.ui.NavigationController;
import org.logicprobe.LogicMail.ui.NotificationHandler;
import org.logicprobe.LogicMail.ui.ThrobberField;
import org.logicprobe.LogicMail.util.DataStoreFactory;
import org.logicprobe.LogicMail.util.UtilFactory;
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
    private boolean analyticsAppStartHappened = false;
    private boolean autoStart;
    private StartupSystemListener systemListener;
    private NavigationController navigationController;
    private Screen loadingScreen;
    private volatile boolean foreground;

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
        PermissionsHandler.registerReasonProvider();
        if(autoStart) {
            runAutoStartup();
        }
        else {
            runNormalStartup();
        }
        enterEventDispatcher();
    }
    
    public void activate() {
        activateAnalytics();
        super.activate();
    }
    
    public void deactivate() {
        deactivateAnalytics();
        super.deactivate();
    }
    
    protected boolean acceptsForeground() {
        return foreground;
    }

    private void activateAnalytics() {
        if(AppInfo.isAnalyticsEnabled()) {
            if(analyticsAppStartHappened) {
                AnalyticsDataCollector.getInstance().onApplicationForeground();
            }
            else {
                AnalyticsDataCollector.getInstance().onApplicationStart();
                analyticsAppStartHappened = true;
            }
        }
    }

    private void deactivateAnalytics() {
        if(AppInfo.isAnalyticsEnabled() && analyticsAppStartHappened) {
            AnalyticsDataCollector.getInstance().onApplicationBackground();
        }
    }
    
    private void runNormalStartup() {
        logStartupAppInfo();
        createLoadingScreen();
        checkForVersionIncrease();
        
        if(AppInfo.isLicenceAccepted()) {
            beginNormalStartup(true);
        }
        else {
            showLicenseDialog();
        }
    }

    private void runBackgroundStartup() {
        logStartupAppInfo();
        beginNormalStartup(false);
    }
    
    private void checkForVersionIncrease() {
        if(!AppInfo.getVersion().equals(AppInfo.getLastVersion())) {
            AppInfo.setLicenseAccepted(false);
        }
    }

    private void beginNormalStartup(boolean runInForeground) {
        // First check to see if we have an already-running instance
        if(runInForeground) {
            tryRequestForground();
        }
        
        foreground = true;
        PermissionsHandler.checkStartupPermissions(false);
        AnalyticsDataCollector.getInstance().setConfigured(AppInfo.isAnalyticsEnabled());
        if(runInForeground) {
            requestForeground();
            pushScreen(loadingScreen);
        }
        startBackgroundLoadingProcess();
    }

    private void tryRequestForground() {
        try {
            UiApplication applicationInstance =
                LogicMailRuntimeState.getInstance().getApplicationInstance();
            if(applicationInstance != null) {
                applicationInstance.requestForeground();
                System.exit(0);
            }
        } catch (ControlledAccessException e) { }
    }

    private void showLicenseDialog() {
        foreground = false;
        invokeLater(new Runnable() {
            public void run() {
                HomeScreenPopup popupDialog = new HomeScreenPopup();
                pushGlobalScreen(popupDialog, 1, UiEngine.GLOBAL_MODAL);

                if(!popupDialog.isLicenseAccepted()) {
                    PermissionsHandler.unregisterReasonProvider();
                    LogicMailRuntimeState.getInstance().setApplicationInstance(null);
                    System.exit(0);
                }
                else {
                    AppInfo.updateLastVersion();
                    AppInfo.setLicenseAccepted(true);
                    AppInfo.setAnalyticsEnabled(popupDialog.isAnalyticsEnabled());
                    PermissionsHandler.checkStartupPermissions(true);
                    AnalyticsDataCollector.updateAnalyticsState();
                    foreground = true;
                    LogicMail.this.requestForeground();
                    pushScreen(loadingScreen);
                    startBackgroundLoadingProcess();
                }
            }
        });
    }

    private void startBackgroundLoadingProcess() {
        Thread loadingThread = new Thread() {
            public void run() {
                loadConfiguration();

                setDefaultLocale();

                // Initialize the data model explicitly
                MailManager.initialize();

                // Initialize the notification handler
                NotificationHandler.getInstance().setEnabled(true);

                // Initialize the navigation controller
                navigationController = new NavigationController(LogicMail.this);

                addSystemListeners();

                // Explicitly trigger a refresh of the local data location, just in
                // case the file system appeared before we registered the listeners.
                MailSettings.getInstance().simulateGlobalDataChange();
                
                showMailHomeScreen();
            };
        };
        loadingThread.start();
    }

    private void loadConfiguration() {
        // Load the configuration
        DataStoreFactory.getConnectionCacheStore().load();
        MailSettings.getInstance().loadSettings();
    }

    private void setDefaultLocale() {
        // Locale override is not used in release builds
        if(!AppInfo.isRelease()) {
            // Set the language, if configured
            String languageCode =
                MailSettings.getInstance().getGlobalConfig().getLanguageCode();
            if(languageCode != null && languageCode.length() > 0) {
                try {
                    Locale.setDefault(Locale.get(languageCode));
                } catch (Exception e) { }
            }
        }
    }

    private void addSystemListeners() {
        // Add the filesystem listener
        try {
            FileSystemRegistry.addFileSystemListener(MailSettings.getInstance().getFileSystemListener());
        } catch (ControlledAccessException e) {
            // Don't fail if file permissions are denied
        }
        
        // Add any sensor listeners
        try {
            UtilFactory.getInstance().addSensorListeners();
        } catch (ControlledAccessException e) {
            // Don't fail if permissions are denied
        }
        
        // Add the system listener for handling power-related events
        try {
            addSystemListener(MailManager.getInstance().getSystemListener());
        } catch (ControlledAccessException e) {
            // Don't fail if permissions are denied
        }
    }

    private void showMailHomeScreen() {
        invokeLater(new Runnable() {
            public void run() {
                // Push the mail home screen and pop
                // the loading screen
                navigationController.displayMailHome();
                if(loadingScreen != null) {
                    popScreen(loadingScreen);
                    loadingScreen = null;
                }
                MailManager.getInstance().startupComplete();
            }
        });
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
            appendAppVersion(buf);
            buf.append("\r\n");
            buf.append("Platform: ");
            buf.append(DeviceInfo.getDeviceName());
            buf.append(' ');
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
        
        StringBuffer buf = new StringBuffer();
        buf.append("Version ");
        appendAppVersion(buf);
        loadingScreen.add(new LabelField(buf.toString(), Field.FIELD_HCENTER));
    }

    private static void appendAppVersion(StringBuffer buf) {
        buf.append(AppInfo.getVersion());
        if(AppInfo.isRelease()) {
            String moniker = AppInfo.getVersionMoniker();
            if(moniker != null && moniker.length() > 0) {
                buf.append(" (");
                buf.append(moniker);
                buf.append(')');
            }
        }
        else {
            buf.append(" (dev)");
        }
    }
    
    /**
     * Complete the application shutdown process by unregistering any static
     * listeners and exiting the application process.
     */
    public static void shutdownApplication() {
        NotificationHandler.getInstance().shutdown();

        try {
            Application.getApplication().removeSystemListener(MailManager.getInstance().getSystemListener());
        } catch (ControlledAccessException e) {
            // Don't fail if permissions are denied
        }
        
        try {
            UtilFactory.getInstance().removeSensorListeners();
        } catch (ControlledAccessException e) {
            // Don't fail if permissions are denied
        }
        
        try {
            FileSystemRegistry.removeFileSystemListener(MailSettings.getInstance().getFileSystemListener());
        } catch (ControlledAccessException e) {
            // Don't fail if file permissions are denied
        }
        
        PermissionsHandler.unregisterReasonProvider();
        LogicMailRuntimeState.getInstance().setApplicationInstance(null);
        
        AnalyticsDataCollector.getInstance().onApplicationTerminate();
        
        System.exit(0);
    }

    private void runAutoStartup() {
        if(ApplicationManager.getApplicationManager().inStartup()) {
            systemListener = new StartupSystemListener();
            this.addSystemListener(systemListener);
        }
        else {
            invokeLater(new Runnable() {
                public void run() {
                    startupInitialization();
                }
            });
        }
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
    
    private void startupInitialization() {
        // The BlackBerry has finished its startup process

        MailSettings mailSettings = null;
        boolean initialized = false;
        try {
            if(!LogicMailRuntimeState.removeInstance()) {
                // Configure the rollover icons
                HomeScreen.updateIcon(AppInfo.getIcon(), 0);
                HomeScreen.setRolloverIcon(AppInfo.getRolloverIcon(), 0);
                
                try {
                    HomeScreen.updateIcon(AppInfo.getIcon(), 1);
                    HomeScreen.setRolloverIcon(AppInfo.getRolloverIcon(), 1);
                } catch (IllegalArgumentException e) { }
                
                // Register for synchronization
                SyncManager.getInstance().enableSynchronization(LogicMailSyncCollection.getInstance());
            }

            // Load application settings
            mailSettings = MailSettings.getInstance();
            mailSettings.loadSettings();
            
            LogicMailRuntimeState runtimeState = LogicMailRuntimeState.getInstance();
            
            configureNotificationSources(runtimeState, mailSettings);
    
            initialized = true;
        } catch (ControlledAccessException e) {
            // If permissions have not been granted, we may get here
        }

        checkForVersionIncrease();
        
        if(initialized
                && AppInfo.isLicenceAccepted()
                && mailSettings != null
                && mailSettings.getGlobalConfig().isAutoStartupEnabled()) {
            LogicMailRuntimeState.getInstance().setApplicationInstance(this);
            runBackgroundStartup();
        }
        else {
            // Exit the application.
            PermissionsHandler.unregisterReasonProvider();
            LogicMailRuntimeState.getInstance().setApplicationInstance(null);
            System.exit(0);
        }
    }

    private static void configureNotificationSources(LogicMailRuntimeState runtimeState, MailSettings mailSettings) {
        // Configure a notification source for each account
        int numAccounts = mailSettings.getNumAccounts();
        for(int i=0; i<numAccounts; i++) {
            AccountConfig accountConfig = mailSettings.getAccountConfig(i);
            LogicMailEventSource eventSource =
                new LogicMailEventSource(accountConfig.getAcctName(), accountConfig.getUniqueId());
            NotificationsManager.registerSource(
                    eventSource.getEventSourceId(),
                    eventSource,
                    NotificationsConstants.CASUAL);
            runtimeState.putEventSource(eventSource);
        }
    }
} 
