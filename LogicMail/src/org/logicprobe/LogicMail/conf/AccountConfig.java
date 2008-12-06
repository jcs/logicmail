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

package org.logicprobe.LogicMail.conf;

import java.io.DataInputStream;

import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Store account configuration for LogicMail
 */
public abstract class AccountConfig extends ConnectionConfig {
    final public static int TYPE_POP = 0;
    final public static int TYPE_IMAP = 1;
    
    private int serverType;
    private String serverUser;
    private String serverPass;
    private IdentityConfig identityConfig;
    private long identityConfigId;
    private OutgoingConfig outgoingConfig;
    private long outgoingConfigId;

    private MailboxNode sentMailbox;
    private long sentMailboxId;
    private MailboxNode draftMailbox;
    private long draftMailboxId;

    public AccountConfig() {
        super();
    }
    
    public AccountConfig(DataInputStream input) {
        super(input);
    }

    protected void setDefaults() {
        super.setDefaults();
        serverType = TYPE_POP;
        serverUser = "";
        serverPass = "";
        setServerPort(110);
        identityConfig = null;
        identityConfigId = -1L;
        outgoingConfig = null;
        outgoingConfigId = -1L;
        sentMailbox = null;
        sentMailboxId = -1L;
        draftMailbox = null;
        draftMailboxId = -1L;
    }

    public String toString() {
        String text = getAcctName();
        if(serverType == TYPE_POP) {
            text = text.concat(" (POP)");
        }
        else if(serverType == TYPE_IMAP) {
            text = text.concat(" (IMAP)");
        }
        
        return text;
    }

    public int getServerType() {
        return serverType;
    }
    
    public void setServerType(int serverType) {
        this.serverType = serverType;
    }
    
   
    public String getServerUser() {
        return serverUser;
    }
    
    public void setServerUser(String serverUser) {
        this.serverUser = serverUser;
    }
    
    public String getServerPass() {
        return serverPass;
    }
    
    public void setServerPass(String serverPass) {
        this.serverPass = serverPass;
    }

    public IdentityConfig getIdentityConfig() {
        if(identityConfig == null && identityConfigId != -1L) {
            identityConfig = MailSettings.getInstance().getIdentityConfigByUniqueId(identityConfigId);
        }
        return identityConfig;
    }
    
    public void setIdentityConfig(IdentityConfig identityConfig) {
        if(identityConfig == null) {
            this.identityConfig = null;
            this.identityConfigId = -1L;
        }
        else {
            this.identityConfig = identityConfig;
            this.identityConfigId = identityConfig.getUniqueId();
        }
    }

    public OutgoingConfig getOutgoingConfig() {
        if(outgoingConfig == null && outgoingConfigId != -1L) {
            outgoingConfig = MailSettings.getInstance().getOutgoingConfigByUniqueId(outgoingConfigId);
        }
        return outgoingConfig;
    }
    
    public void setOutgoingConfig(OutgoingConfig outgoingConfig) {
        if(outgoingConfig == null) {
            this.outgoingConfig = null;
            this.outgoingConfigId = -1L;
        }
        else {
            this.outgoingConfig = outgoingConfig;
            this.outgoingConfigId = outgoingConfig.getUniqueId();
        }
    }

    public MailboxNode getSentMailbox() {
        if(sentMailbox == null && sentMailboxId != -1L) {
        	MailRootNode rootNode = MailManager.getInstance().getMailRootNode();
        	
        	AccountNode[] accounts = rootNode.getAccounts();
        	for(int i=0; i<accounts.length; i++) {
        		sentMailbox = findMailboxNode(accounts[i].getRootMailbox(), sentMailboxId);
        		if(sentMailbox != null) {
        			break;
        		}
        	}
        }
        return sentMailbox;
    }

    
    public void setSentMailbox(MailboxNode sentMailbox) {
        if(sentMailbox == null) {
            this.sentMailbox = null;
            this.sentMailboxId = -1L;
        }
        else {
            this.sentMailbox = sentMailbox;
            this.sentMailboxId = sentMailbox.getUniqueId();
        }
    }
    
    public MailboxNode getDraftMailbox() {
        if(draftMailbox == null && draftMailboxId != -1L) {
        	MailRootNode rootNode = MailManager.getInstance().getMailRootNode();
        	
        	AccountNode[] accounts = rootNode.getAccounts();
        	for(int i=0; i<accounts.length; i++) {
        		draftMailbox = findMailboxNode(accounts[i].getRootMailbox(), draftMailboxId);
        		if(draftMailbox != null) {
        			break;
        		}
        	}
        }
        return draftMailbox;
    }

    
    public void setDraftMailbox(MailboxNode draftMailbox) {
        if(draftMailbox == null) {
            this.draftMailbox = null;
            this.draftMailboxId = -1L;
        }
        else {
            this.draftMailbox = draftMailbox;
            this.draftMailboxId = draftMailbox.getUniqueId();
        }
    }
    
    private static MailboxNode findMailboxNode(MailboxNode currentNode, long id) {
    	if(currentNode.getUniqueId() == id) {
    		return currentNode;
    	}
    	else {
    		MailboxNode[] nodes = currentNode.getMailboxes();
    		for(int i=0; i<nodes.length; i++) {
    			MailboxNode result = findMailboxNode(nodes[i], id);
    			if(result != null) {
    				return result;
    			}
    		}
    	}
    	return null;
    }

    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        table.put("account_serverType", new Integer(serverType));
        table.put("account_serverUser", serverUser);
        table.put("account_serverPass", serverPass);
        table.put("account_identityConfigId", new Long(identityConfigId));
        table.put("account_outgoingConfigId", new Long(outgoingConfigId));
        table.put("account_sentMailboxId", new Long(sentMailboxId));
        table.put("account_draftMailboxId", new Long(draftMailboxId));
    }

    public void readConfigItems(SerializableHashtable table) {
        super.readConfigItems(table);
        Object value;

        value = table.get("account_serverType");
        if(value != null && value instanceof Integer) {
            serverType = ((Integer)value).intValue();
        }
        value = table.get("account_serverUser");
        if(value != null && value instanceof String) {
            serverUser = (String)value;
        }
        value = table.get("account_serverPass");
        if(value != null && value instanceof String) {
            serverPass = (String)value;
        }
        value = table.get("account_identityConfigId");
        if(value != null && value instanceof Long) {
            identityConfigId = ((Long)value).longValue();
        }
        else {
            identityConfigId = -1;
        }
        identityConfig = null;
        value = table.get("account_outgoingConfigId");
        if(value != null && value instanceof Long) {
            outgoingConfigId = ((Long)value).longValue();
        }
        else {
            outgoingConfigId = -1;
        }
        outgoingConfig = null;

        value = table.get("account_sentMailboxId");
        if(value != null && value instanceof Long) {
        	sentMailboxId = ((Long)value).longValue();
        }
        else {
        	sentMailboxId = -1L;
        }
        
        value = table.get("account_draftMailboxId");
        if(value != null && value instanceof Long) {
        	draftMailboxId = ((Long)value).longValue();
        }
        else {
        	draftMailboxId = -1L;
        }
    }
}
