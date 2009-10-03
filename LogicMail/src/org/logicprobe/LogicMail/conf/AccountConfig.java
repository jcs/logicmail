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

import java.io.DataInput;

import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Configuration object to store settings for
 * incoming mail accounts.
 */
public abstract class AccountConfig extends ConnectionConfig {
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

    /**
     * Instantiates a new connection configuration with defaults.
     */
    public AccountConfig() {
        super();
    }
    
    /**
     * Instantiates a new connection configuration from serialized data.
     * 
     * @param input The input stream to deserialize from
     */
    public AccountConfig(DataInput input) {
        super(input);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#setDefaults()
     */
    protected void setDefaults() {
        super.setDefaults();
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getAcctName();
    }

    /**
     * Gets the username to authenticate with.
     * 
     * @return The username
     */
    public String getServerUser() {
        return serverUser;
    }
    
    /**
     * Sets the username to authenticate with.
     * 
     * @param serverUser The new username
     */
    public void setServerUser(String serverUser) {
        this.serverUser = serverUser;
    }
    
    /**
     * Gets the password to authenticate with.
     * 
     * @return The password
     */
    public String getServerPass() {
        return serverPass;
    }
    
    /**
     * Sets the password to authenticate with.
     * 
     * @param serverPass The new password
     */
    public void setServerPass(String serverPass) {
        this.serverPass = serverPass;
    }

    /**
     * Gets the identity configuration.
     * 
     * @return The identity configuration
     */
    public IdentityConfig getIdentityConfig() {
        if(identityConfig == null && identityConfigId != -1L) {
            identityConfig = MailSettings.getInstance().getIdentityConfigByUniqueId(identityConfigId);
        }
        return identityConfig;
    }
    
    /**
     * Sets the identity configuration.
     * 
     * @param identityConfig The new identity configuration
     */
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

    /**
     * Gets the outgoing connection configuration.
     * 
     * @return The outgoing connection configuration
     */
    public OutgoingConfig getOutgoingConfig() {
        if(outgoingConfig == null && outgoingConfigId != -1L) {
            outgoingConfig = MailSettings.getInstance().getOutgoingConfigByUniqueId(outgoingConfigId);
        }
        return outgoingConfig;
    }
    
    /**
     * Sets the outgoing connection configuration.
     * 
     * @param outgoingConfig The new outgoing connection configuration
     */
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

    /**
     * Gets the sent message mailbox.
     * 
     * @return The sent message mailbox
     */
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

    
    /**
     * Sets the sent message mailbox.
     * 
     * @param sentMailbox The new sent message mailbox
     */
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
    
    /**
     * Gets the draft message mailbox.
     * 
     * @return The draft message mailbox
     */
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

    
    /**
     * Sets the draft message mailbox.
     * 
     * @param draftMailbox The new draft message mailbox
     */
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
    
    /**
     * Finds a mailbox node recursively in the mail model tree.
     * 
     * @param currentNode The current node
     * @param id The id to look for
     * 
     * @return The mailbox node
     */
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

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#writeConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        table.put("account_serverUser", serverUser);
        table.put("account_serverPass", serverPass);
        table.put("account_identityConfigId", new Long(identityConfigId));
        table.put("account_outgoingConfigId", new Long(outgoingConfigId));
        table.put("account_sentMailboxId", new Long(sentMailboxId));
        table.put("account_draftMailboxId", new Long(draftMailboxId));
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.ConnectionConfig#readConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
    public void readConfigItems(SerializableHashtable table) {
        super.readConfigItems(table);
        Object value;

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
