/*
 * CacheReader.java
 *
 * Created on August 1, 2006, 6:24 PM
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
public class CacheReader {
    String _storeName;
    Vector _storeList;
    
    public CacheReader(String storeName) {
        _storeName = storeName;
        _storeList = new Vector();
    }

    public int getNumItems() {
        return _storeList.size();
    }
    
    public void getItem(int index, Cacheable item) {
        item.deserialize((byte[])_storeList.elementAt(index));
    }
    
    public void load() {
        _storeList.removeAllElements();
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(_storeName, false);
            if(store == null) return;
            int records = store.getNumRecords();
            for(int i=1;i<=records;i++) {
                _storeList.addElement(store.getRecord(i));
            }
        } catch (RecordStoreNotFoundException exp) {
            return;
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
