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

public abstract class AnalyticsDataCollector {
    private static AnalyticsDataCollector instance;
    
    private static String WEBTRENDS_UIAPPLICATION = "com.webtrends.mobile.rim.WebtrendsUiApplication";
    private static String WEBTRENDS_DATA_COLLECTOR = "org.logicprobe.LogicMail.LogicMailWebtrendsDataCollector";
    
    public static synchronized AnalyticsDataCollector getInstance() {
        if(instance == null) {
            if(isWebtrendsAvailable()) {
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

    public abstract void setConfigured(boolean configured);
    
    public abstract void onApplicationStart();
    
    public abstract void onApplicationTerminate();
    
    public abstract void onApplicationForeground();
    
    public abstract void onApplicationBackground();

    public abstract void onButtonClick(
            String eventPath,
            String eventDesc,
            String eventType,
            Hashtable customData);

    public abstract void onContentView(
            String eventPath,
            String eventDesc,
            String eventType,
            Hashtable customData,
            String contentGroup);

    public abstract void onMediaEvent(
            String eventPath,
            String eventDesc,
            String eventType,
            Hashtable customData,
            String contentGroup,
            String mediaName,
            String mediaType,
            String mediaEventType);

    public abstract void onCustomEvent(
            String eventPath,
            String eventDesc,
            Hashtable customData);
}
