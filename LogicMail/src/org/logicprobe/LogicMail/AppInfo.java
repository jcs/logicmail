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

import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

/**
 * Class to provide information about the application
 * and its environment.  Typically this information
 * should come from the deployment descriptor.
 */
public final class AppInfo {
    /** System event log GUID */
    public static final long GUID = 0x6bc611e33074e780L;
    private static PersistentObject store;
    private static PersistableAppInfo persistableInfo;
    
    private static final Bitmap icon = Bitmap.getBitmapResource("logicmail.png");
    private static final Bitmap rolloverIcon = Bitmap.getBitmapResource("logicmail-rollover.png");
    private static final Bitmap newMessagesIcon = Bitmap.getBitmapResource("logicmail-new.png");
    private static final Bitmap newMessagesRolloverIcon = Bitmap.getBitmapResource("logicmail-new-rollover.png");
    private static String PARAM_RELEASE = "-release";
    private static String appName;
    private static String appVersion;
    private static boolean release;
    private static String appVersionMoniker;
    private static PlatformInfo platformInfo;
    
    static {
        if(tryInitializeStore()) {
            synchronized(store) {
                if (!(store.getContents() instanceof PersistableAppInfo)) {
                    store.setContents(new PersistableAppInfo());
                    store.commit();
                }
            }
            persistableInfo = (PersistableAppInfo)store.getContents();
        }
        else {
            persistableInfo = new PersistableAppInfo();
        }
    }
    
    private static boolean tryInitializeStore() {
        if(store != null) { return true; }
        try {
            //"org.logicprobe.LogicMail.PersistableAppInfo"
            store = PersistentStore.getPersistentObject(0x6f8bfd06eca43499L);
            return true;
        } catch (ControlledAccessException e) {
            return false;
        }
    }
    
    /**
     * Initializes the application information from the descriptor and the
     * command-line arguments.  This method must be called on startup.
     * @param args Arguments
     */
    public static synchronized void initialize(String args[]) {
        ApplicationDescriptor appDesc =
            ApplicationDescriptor.currentApplicationDescriptor();

        appName = appDesc.getName();
        appVersion = appDesc.getVersion();
        platformInfo = PlatformInfo.getInstance();
        
        // Parse the command-line argument list
        for(int i=0; i<args.length; i++) {
            if(args[i].startsWith(PARAM_RELEASE)) {
                release = true;
                int p = args[i].indexOf(':', PARAM_RELEASE.length());
                if(p != -1 && p < args[i].length() - 1) {
                    appVersionMoniker = args[i].substring(p + 1);
                }
            }
        }
    }
    
    public static String getName() {
        return appName;
    }

    public static String getVersion() {
    	return appVersion;
    }
    
    public static String getVersionMoniker() {
        return appVersionMoniker;
    }
    
    public static String getPlatformVersion() {
    	return platformInfo.getPlatformVersion();
    }
    
    public static boolean isRelease() {
        return release;
    }
    
    public static Bitmap getIcon() {
    	return icon;
    }
    
    public static Bitmap getRolloverIcon() {
    	return rolloverIcon;
    }
    
    public static Bitmap getNewMessagesIcon() {
    	return newMessagesIcon;
    }
    
    public static Bitmap getNewMessagesRolloverIcon() {
    	return newMessagesRolloverIcon;
    }
    
    public static String getLastVersion() {
        Object value = persistableInfo.getElement(PersistableAppInfo.FIELD_LAST_APP_VERSION);
        if(value instanceof String) {
            return (String)value;
        }
        else {
            return "";
        }
    }
    
    public static void updateLastVersion() {
        persistableInfo.setElement(PersistableAppInfo.FIELD_LAST_APP_VERSION, appVersion);
        commitPersistableInfo();
    }
    
    public static boolean isLicenceAccepted() {
        Object value = persistableInfo.getElement(PersistableAppInfo.FIELD_LICENSE_ACCEPTED);
        if(value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        }
        else {
            return false;
        }
    }
    
    public static void setLicenseAccepted(boolean accepted) {
        persistableInfo.setElement(PersistableAppInfo.FIELD_LICENSE_ACCEPTED, new Boolean(accepted));
        commitPersistableInfo();
    }
    
    public static void resetPersistableInfo() {
        persistableInfo.setElement(PersistableAppInfo.FIELD_LAST_APP_VERSION, "");
        persistableInfo.setElement(PersistableAppInfo.FIELD_LICENSE_ACCEPTED, Boolean.FALSE);
        commitPersistableInfo();
    }

    private static void commitPersistableInfo() {
        if(tryInitializeStore()) {
            synchronized(store) {
                store.setContents(persistableInfo);
                store.commit();
            }
        }
    }
}
