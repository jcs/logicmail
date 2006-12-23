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
import org.logicprobe.LogicMail.mail.FolderTreeItem;
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
            writer.addItem((FolderTreeItem)folderList.elementAt(i));
        writer.store();
    }

    public Vector loadFolderList() {
        String key = "acct_" + _acctName + "_folders";
        CacheReader reader = new CacheReader("LogicMail_acct_" + Integer.toString(key.hashCode()));
        reader.load();
        Vector folderList = new Vector();
        for(int i=0;i<reader.getNumItems();i++) {
            FolderTreeItem item = new FolderTreeItem();
            reader.getItem(i, item);
            folderList.addElement(item);
        }
        return folderList;
    }
}
