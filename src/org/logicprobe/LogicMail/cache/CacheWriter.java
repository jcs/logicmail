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

package org.logicprobe.LogicMail.cache;

import java.util.Vector;
import javax.microedition.rms.*;

/**
 *
 * @author octo
 */
public class CacheWriter {
    String _storeName;
    Vector _storeList;
    
    public CacheWriter(String storeName) {
        _storeName = storeName;
        _storeList = new Vector();
    }
    
    public void addItem(Cacheable item) {
        _storeList.addElement(item);
    }
    
    public void delItem(Cacheable item) {
        _storeList.removeElement(item);
    }
    
    public void clearItems() {
        _storeList.removeAllElements();
    }
    
    public void store() {
        try {
            RecordStore.deleteRecordStore(_storeName);
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(_storeName, true);
            byte[] buffer;
            for(int i=0;i<_storeList.size();i++) {
                buffer = ((Cacheable)_storeList.elementAt(i)).serialize();
                store.addRecord(buffer, 0, buffer.length);
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            if(store != null) store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }
}
