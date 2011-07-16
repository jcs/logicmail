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

import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.LongHashtable;

/**
 * Container class for the application state information that is kept in
 * the <code>RuntimeStore</code>.
 */
public final class LogicMailRuntimeState {
    private static LogicMailRuntimeState instance;
    private final LongHashtable eventSourceMap;
    private UiApplication applicationInstance;
    
    private LogicMailRuntimeState() {
        this.eventSourceMap = new LongHashtable();
    }
    
    /**
     * Gets the runtime state object from the runtime store.
     *
     * @return single instance of LogicMailRuntimeState
     * @throws ControlledAccessException if permissions to access this object
     *     within the runtime store are refused.
     */
    public static synchronized LogicMailRuntimeState getInstance() {
        if(instance == null) {
            instance = (LogicMailRuntimeState)RuntimeStore.getRuntimeStore().get(AppInfo.GUID);
            if(instance == null) {
                LogicMailRuntimeState runtimeState = new LogicMailRuntimeState();
                RuntimeStore.getRuntimeStore().put(AppInfo.GUID, runtimeState);
                instance = runtimeState;
            }
        }
        
        return instance;
    }
    
    /**
     * Removes the runtime state object from the runtime store.
     *
     * @return true, if the object was removed
     */
    public static synchronized boolean removeInstance() {
        return RuntimeStore.getRuntimeStore().remove(AppInfo.GUID) != null;
    }
    
    public void putEventSource(LogicMailEventSource eventSource) {
        synchronized(eventSourceMap) {
            eventSourceMap.put(eventSource.getAccountUniqueId(), eventSource);
        }
    }
    
    public LogicMailEventSource getEventSource(long accountUniqueId) {
        LogicMailEventSource result = null;
        synchronized(eventSourceMap) {
            result = (LogicMailEventSource)eventSourceMap.get(accountUniqueId);
        }
        return result;
    }
    
    public void removeEventSource(long accountUniqueId) {
        synchronized(eventSourceMap) {
            eventSourceMap.remove(accountUniqueId);
        }
    }
    
    public void removeAllEventSources() {
        synchronized(eventSourceMap) {
            eventSourceMap.clear();
        }
    }
    
    public synchronized UiApplication getApplicationInstance() {
        return applicationInstance;
    }
    
    public synchronized void setApplicationInstance(UiApplication applicationInstance) {
        this.applicationInstance = applicationInstance;
    }
}
