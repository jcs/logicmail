/*
 * CacheWriter.java
 *
 * Created on August 1, 2006, 6:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
