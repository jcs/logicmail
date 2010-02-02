/*-
 * Copyright (c) 2010, Derek Konigsberg
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

package org.logicprobe.LogicMail.conf;

import net.rim.device.api.util.LongIntHashtable;

import org.logicprobe.LogicMail.util.EventObject;
import org.logicprobe.LogicMail.util.Serializable;

/**
 * Object for MailSettings events.
 */
public class MailSettingsEvent extends EventObject {
    private int globalChangeType;
    private int listChangeType;
    private LongIntHashtable configChangeMap;

    /** Identity list changed. */
    public static final int LIST_CHANGED_IDENTITY = 0x01;
    /** Account list changed. */
    public static final int LIST_CHANGED_ACCOUNT = 0x02;
    /** Outgoing server list changed. */
    public static final int LIST_CHANGED_OUTGOING = 0x04;
    
    public MailSettingsEvent(Object source) {
        super(source);
    }
    
    void setGlobalChange(int changeType) {
        this.globalChangeType = changeType;
    }
    
    public int getGlobalChange() {
        return globalChangeType;
    }
    
    void setListChange(int changeType) {
        this.listChangeType = changeType;
    }
    
    public int getListChange() {
        return listChangeType;
    }
    
    void setConfigChange(Serializable configObject, int changeType) {
        if(configChangeMap == null) {
            configChangeMap = new LongIntHashtable();
        }
        configChangeMap.put(configObject.getUniqueId(), changeType);
    }
    
    public int getConfigChange(Serializable configObject) {
        if(configChangeMap == null) {
            return 0;
        }
        else {
            return configChangeMap.get(configObject.getUniqueId());
        }
    }
}
