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
package org.logicprobe.LogicMail;

import java.util.Hashtable;

import net.rim.device.api.system.DeviceInfo;

/**
 * Data collector for application analytics.
 * <p>
 * This class is designed as a wrapper for the <code>WebtrendsDataCollector</code>
 * class from the BlackBerry Application Analytics library.  If that library is
 * not available, or otherwise disabled, a stub implementation will be used
 * instead.
 * </p>
 */
public abstract class AnalyticsDataCollector {
    private static AnalyticsDataCollector instance;
    
    private static String WEBTRENDS_UIAPPLICATION = "com.webtrends.mobile.rim.WebtrendsUiApplication";
    private static String WEBTRENDS_DATA_COLLECTOR = "org.logicprobe.LogicMail.LogicMailWebtrendsDataCollector";
    
    public static synchronized AnalyticsDataCollector getInstance() {
        if(instance == null) {
            if(!DeviceInfo.isSimulator() && isWebtrendsAvailable()) {
                instance = createWebtrendsDataCollector();
            }
            else {
                instance = new StubDataCollector();
            }
        }
        return instance;
    }

    private static boolean isWebtrendsAvailable() {
        boolean webtrendsAvailable = false;
        try {
            if(Class.forName(WEBTRENDS_UIAPPLICATION) != null) {
                webtrendsAvailable = true;
            }
        } catch (ClassNotFoundException e) { }
        return webtrendsAvailable;
    }
    
    private static AnalyticsDataCollector createWebtrendsDataCollector() {
        AnalyticsDataCollector collectorInstance = null;
        
        Class clazz;
        try {
            clazz = Class.forName(WEBTRENDS_DATA_COLLECTOR);
            collectorInstance = (AnalyticsDataCollector)clazz.newInstance();
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) { }
        
        if(collectorInstance == null) {
            collectorInstance = new StubDataCollector();
        }
        
        return collectorInstance;
    }
    
    /**
     * Turns on or turns off data collection in an application.
     * The default value is <code>true</code>.
     *
     * @param configured Specify <code>true</code> to turn on data collection,
     *     and specify <code>false</code> to turn off data collection.
     */
    public abstract void setConfigured(boolean configured);
    
    /**
     * Invoke this method to track instances that an application starts.
     */
    public abstract void onApplicationStart();
    
    /**
     * Invoke this method to track instances that an application terminates.
     */
    public abstract void onApplicationTerminate();
    
    /**
     * Invoke this method to track instances that an application moves to the
     * foreground.
     */
    public abstract void onApplicationForeground();
    
    /**
     * Invoke this method to track instances that an application moves to the
     * background.
     */
    public abstract void onApplicationBackground();

    /**
     * Tracks instances that a user clicks a button in an application.
     *
     * @param eventPath The hierarchical location of where an object or content
     *     is located in an application.
     * @param eventDesc The name of the screen where the event occurs.
     * @param eventType The event type.
     */
    public abstract void onButtonClick(
            String eventPath,
            String eventDesc,
            String eventType);

    /**
     * Tracks instances that a user views content in an application.
     *
     * @param eventPath The hierarchical location of where an object or content
     *     is located in an application.
     * @param eventDesc The name of the screen where the event occurs.
     * @param eventType The event type.
     * @param contentGroup A category name for the content.
     */
    public abstract void onContentView(
            String eventPath,
            String eventDesc,
            String eventType,
            String contentGroup);

    /**
     * Tracks instances that media events occur in an application.
     *
     * @param eventPath The hierarchical location of where an object or content
     *     is located in an application.
     * @param eventDesc The name of the screen where the event occurs.
     * @param eventType The event type.
     * @param contentGroup A category name for the content.
     * @param mediaName The name of the piece of media.
     * @param mediaType The type of media.
     * @param mediaEventType The status of the media event. Valid values include
     *     p (play), 25 (25% complete), 50 (50% complete), 75 (75% complete),
     *     and f (finished).
     */
    public abstract void onMediaEvent(
            String eventPath,
            String eventDesc,
            String eventType,
            String contentGroup,
            String mediaName,
            String mediaType,
            String mediaEventType);

    /**
     * Tracks instances that custom events occur in an application.
     * This method requires that you pass in the <code>WT.ev</code> parameter
     * by using a <code>Hashtable</code> object. The <code>WT.ev</code>
     * parameter represents the event type, and you can specify any String as
     * the value. The following code sample demonstrates how to invoke
     * <code>onCustomEvent()</code>.
     * <p>
     * <code><pre>
     * Hashtable customParams = new Hashtable();
     * customParams.put("WT.ev", "Level cleared");
     * AnalyticsDataCollector.getInstance().onCustomEvent("ActionGame/main", "Level 3", customParams);
     * </pre></code>
     * </p>
     *
     * @param eventPath The hierarchical location of where an object or content
     *     is located in an application.
     * @param eventDesc The name of the screen where the event occurs.
     * @param customData A hashtable containing custom metadata.
     */
    public abstract void onCustomEvent(
            String eventPath,
            String eventDesc,
            Hashtable customData);
}
