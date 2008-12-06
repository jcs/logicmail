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

package org.logicprobe.LogicMail.conf;

import java.io.DataInputStream;

import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Store account configuration for IMAP
 */
public class ImapConfig extends AccountConfig {
    private String folderPrefix;
    
    public ImapConfig() {
        super();
    }
    
    public ImapConfig(DataInputStream input) {
        super(input);
    }
    
    protected void setDefaults() {
        super.setDefaults();
        setServerPort(143);
        folderPrefix = null;
    }    

    public String toString() {
        String text = getAcctName().concat(" (IMAP)");
        return text;
    }

    public String getFolderPrefix() {
        return folderPrefix;
    }

    public void setFolderPrefix(String folderPrefix) {
        this.folderPrefix = folderPrefix;
    }

    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        if(folderPrefix != null) {
            table.put("account_imap_folderPrefix", folderPrefix);
        }
        else {
            table.put("account_imap_folderPrefix", "");
        }
    }
    
    public void readConfigItems(SerializableHashtable table) {
        super.readConfigItems(table);
        Object value;

        value = table.get("account_imap_folderPrefix");
        if(value != null && value instanceof String) {
            folderPrefix = (String)value;
            if(folderPrefix.length() == 0) {
                folderPrefix = null;
            }
        }
    }
}
