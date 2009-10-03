/*-
 * Copyright (c) 2008, Derek Konigsberg
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
import java.io.DataOutput;
import java.io.IOException;
import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.SerializableHashtable;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * Configuration object to store identity settings.
 */
public class IdentityConfig implements Serializable {
    private long uniqueId;
    private String identityName;
    private String fullName;
    private String emailAddress;
    private String replyToAddress;
    private String msgSignature;
    
    /**
     * Instantiates a new identity with defaults.
     */
    public IdentityConfig() {
        setDefaults();
    }

    /**
     * Instantiates a new identity from serialized data.
     * 
     * @param input The input stream to deserialize from
     */
    public IdentityConfig(DataInput input) {
        try {
            deserialize(input);
        } catch (IOException ex) {
            setDefaults();
        }
    }
    
    /**
     * Sets the default values for all fields.
     */
    private void setDefaults() {
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        identityName = "";
        fullName = "";
        emailAddress = "";
        replyToAddress = "";
        msgSignature = "";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return identityName;
    }
    
    /**
     * Gets the identity name.
     * 
     * @return The identity name
     */
    public String getIdentityName() {
        return identityName;
    }

    /**
     * Sets the identity name.
     * 
     * @param identityName The new identity name
     */
    public void setIdentityName(String identityName) {
        this.identityName = identityName;
    }

    /**
     * Gets the full name of the user.
     * 
     * @return The full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the full name of the user.
     * 
     * @param fullName The new full name
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Gets the E-Mail address for the From header.
     * 
     * @return The email address
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Sets the E-Mail address for the From header.
     * 
     * @param emailAddress The new email address
     */
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Gets the E-Mail address for the Reply-To header.
     * 
     * @return The Reply-To address
     */
    public String getReplyToAddress() {
        return replyToAddress;
    }

    /**
     * Sets the E-Mail address for the Reply-To header.
     * 
     * @param replyToAddress The new Reply-To address
     */
    public void setReplyToAddress(String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }

    /**
     * Gets the message signature.
     * 
     * @return The message signature
     */
    public String getMsgSignature() {
        return msgSignature;
    }

    /**
     * Sets the message signature.
     * 
     * @param msgSignature The new message signature
     */
    public void setMsgSignature(String msgSignature) {
        this.msgSignature = msgSignature;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
     */
    public void serialize(DataOutput output) throws IOException {
        output.writeLong(uniqueId);
        
        SerializableHashtable table = new SerializableHashtable();
        
        table.put("identity_identityName", identityName);
        table.put("identity_fullName", fullName);
        table.put("identity_emailAddress", emailAddress);
        table.put("identity_replyToAddress", replyToAddress);
        table.put("identity_msgSignature", msgSignature);
        
        table.serialize(output);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
     */
    public void deserialize(DataInput input) throws IOException {
        setDefaults();
        uniqueId = input.readLong();
        
        SerializableHashtable table = new SerializableHashtable();
        table.deserialize(input);
        Object value;

        value = table.get("identity_identityName");
        if(value != null && value instanceof String) {
            identityName = (String)value;
        }
        value = table.get("identity_fullName");
        if(value != null && value instanceof String) {
            fullName = (String)value;
        }
        value = table.get("identity_emailAddress");
        if(value != null && value instanceof String) {
            emailAddress = (String)value;
        }
        value = table.get("identity_replyToAddress");
        if(value != null && value instanceof String) {
            replyToAddress = (String)value;
        }
        value = table.get("identity_msgSignature");
        if(value != null && value instanceof String) {
            msgSignature = (String)value;
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
     */
    public long getUniqueId() {
        return uniqueId;
    }
}
