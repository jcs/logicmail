/*-
 * Copyright (c) 2007, Derek Konigsberg
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

package org.logicprobe.LogicMail.util;

import net.rim.device.api.util.Arrays;

/**
 * Utility class to hold EventListeners.
 * Based on the interface of javax.swing.event.EventListenerList
 */
public class EventListenerList {
    protected Object[] listenerList = new Object[0];
    
    /** Creates a new instance of EventListenerList */
    public EventListenerList() {
    }
    
    public void add(Class t, EventListener l) {
        if(l == null || !t.isInstance(l)) {
            throw new IllegalArgumentException();
        }
        if(listenerList.length == 0) {
            listenerList = new Object[] { t, l };
        }
        else {
            Arrays.add(listenerList, t);
            Arrays.add(listenerList, l);
        }
    }
    
    public void remove(Class t, EventListener l) {
        if(l == null || !t.isInstance(l)) {
            throw new IllegalArgumentException();
        }
        for(int i=0; i<listenerList.length; i+=2) {
            if(t == (Class)listenerList[i] &&
               l == (EventListener)listenerList[i+1]) {
               Arrays.removeAt(listenerList, i);
               Arrays.removeAt(listenerList, i);
            }
        }
    }
    
    public int getListenerCount() {
        return listenerList.length/2;
    }
    
    public int getListenerCount(Class t) {
        int count = 0;
        for(int i=0; i<listenerList.length; i+=2) {
            if(t == (Class)listenerList[i]) {
                count++;
            }
        }
        return count;
    }
    
    public Object[] getListenerList() {
        return listenerList;
    }
    
    public EventListener[] getListeners(Class t) {
        int count = getListenerCount(t);
        int index = 0;
        EventListener[] result = new EventListener[count];
        for(int i=0; i<listenerList.length; i+=2) {
            if(t == (Class)listenerList[i]) {
                result[index++] = (EventListener)listenerList[i+1];
            }
        }
        return result;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("EventListenerList: ");
        for (int i=0 ; i<listenerList.length; i+=2) {
            buf.append(((Class)listenerList[i]).getName());
            buf.append(" --> ");
            buf.append(listenerList[i+1]);
        }
        return buf.toString();
    }
}
