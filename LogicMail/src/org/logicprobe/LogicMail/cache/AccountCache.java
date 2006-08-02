/*
 * AccountCache.java
 *
 * Created on July 31, 2006, 10:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.logicprobe.LogicMail.cache;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;
import javax.microedition.rms.*;
import org.logicprobe.LogicMail.mail.MailClient;

public class AccountCache {
    private String _acctName;
    
    public AccountCache(String acctName) {
        _acctName = acctName;
    }
    
    public String getAcctName() {
        return _acctName;
    }
    
    public void setAcctName(String acctName) {
        _acctName = acctName;
    }

    public void saveFolderList(Vector folderList) {
        String key = "acct_" + _acctName + "_folders";
        CacheWriter writer = new CacheWriter("LogicMail_acct_" + Integer.toString(key.hashCode()));
        for(int i=0;i<folderList.size();i++)
            writer.addItem((MailClient.FolderItem)folderList.elementAt(i));
        writer.store();
    }

    public Vector loadFolderList() {
        String key = "acct_" + _acctName + "_folders";
        CacheReader reader = new CacheReader("LogicMail_acct_" + Integer.toString(key.hashCode()));
        reader.load();
        Vector folderList = new Vector();
        for(int i=0;i<reader.getNumItems();i++) {
            MailClient.FolderItem item = new MailClient.FolderItem();
            reader.getItem(i, item);
            folderList.addElement(item);
        }
        return folderList;
    }
}
