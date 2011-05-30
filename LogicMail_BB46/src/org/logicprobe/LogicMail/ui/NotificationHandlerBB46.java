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
package org.logicprobe.LogicMail.ui;

import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;

import net.rim.blackberry.api.messagelist.ApplicationIcon;
import net.rim.blackberry.api.messagelist.ApplicationIndicator;
import net.rim.blackberry.api.messagelist.ApplicationIndicatorRegistry;
import net.rim.device.api.system.EncodedImage;

public class NotificationHandlerBB46 extends NotificationHandler {
    private ApplicationIndicatorRegistry indicatorRegistry;
    private ApplicationIndicator indicator;
    private boolean indicatorRegistered;
    private final Object indicatorLock = new Object();
    
    private final ApplicationIcon indicatorIcon;
    private final ApplicationIcon indicatorNewIcon;
    
    public NotificationHandlerBB46() {
        super();
        
        EncodedImage indicatorImage = EncodedImage.getEncodedImageResource("mail_indicator_21x21.png");
        EncodedImage indicatorNewImage = EncodedImage.getEncodedImageResource("mail_indicator_new_21x21.png");
        indicatorIcon = new ApplicationIcon(indicatorImage, true);
        indicatorNewIcon = new ApplicationIcon(indicatorNewImage, true);
        
        if(MailSettings.getInstance().getGlobalConfig().isNotificationIconShown()) {
            registerIndicator();
        }
    }
    
    public void shutdown() {
        super.shutdown();
        unregisterIndicator();
    }
    
    protected void mailSettingsSaved(MailSettingsEvent e) {
        boolean showIndicator = MailSettings.getInstance().getGlobalConfig().isNotificationIconShown();
        if(showIndicator) {
            registerIndicator();
        }
        else {
            unregisterIndicator();
        }
        
        super.mailSettingsSaved(e);
    }
    
    protected void indicateUnseenMessageCount(int count, boolean recent) {
        super.indicateUnseenMessageCount(count, recent);
        synchronized(indicatorLock) {
            if(indicatorRegistered) {
                indicator.setIcon(recent ? indicatorNewIcon : indicatorIcon);
                indicator.setValue(count);
                indicator.setVisible(count > 0);
            }
        }
    }
    
    private void registerIndicator() {
        synchronized(indicatorLock) {
            if(indicatorRegistered) { return; }
            try {
                indicatorRegistry = ApplicationIndicatorRegistry.getInstance();
                indicator = indicatorRegistry.register(indicatorIcon, false, false);
                indicatorRegistered = true;
            } catch (Exception e) {
                indicatorRegistered = false;
            }
        }
    }
    
    private void unregisterIndicator() {
        synchronized(indicatorLock) {
            if(!indicatorRegistered) { return; }
            try {
                indicatorRegistry.unregister();
            } catch (Exception e) { }
            indicatorRegistered = false;
        }
    }
}
