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

import java.io.DataInput;
import org.logicprobe.LogicMail.util.SerializableHashtable;

/**
 * Configuration object to store settings for
 * POP mail accounts.
 */
public class PopConfig extends AccountConfig {
    private int maxMessageLines;

    /**
     * Instantiates a new connection configuration with defaults.
     */
    public PopConfig() {
        super();
    }
    
    /**
     * Instantiates a new connection configuration from serialized data.
     * 
     * @param input The input stream to deserialize from
     */
    public PopConfig(DataInput input) {
        super(input);
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.AccountConfig#setDefaults()
     */
    protected void setDefaults() {
        super.setDefaults();
        setServerPort(110);
        this.maxMessageLines = 400;
    }    

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.AccountConfig#toString()
     */
    public String toString() {
        String text = getAcctName().concat(" (POP)");
        return text;
    }

    /**
     * Gets the maximum message lines.
     * 
     * @return The maximum message lines
     */
    public int getMaxMessageLines() {
        return maxMessageLines;
    }

    /**
     * Sets the maximum message lines.
     * 
     * @param maxMessageLines The new maximum message lines
     */
    public void setMaxMessageLines(int maxMessageLines) {
        if(this.maxMessageLines != maxMessageLines) {
            this.maxMessageLines = maxMessageLines;
            changeType |= CHANGE_TYPE_LIMITS;
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.AccountConfig#writeConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
    public void writeConfigItems(SerializableHashtable table) {
        super.writeConfigItems(table);
        table.put("account_pop_maxMessageLines", new Integer(maxMessageLines));
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.conf.AccountConfig#readConfigItems(org.logicprobe.LogicMail.util.SerializableHashtable)
     */
    public void readConfigItems(SerializableHashtable table) {
        super.readConfigItems(table);
        Object value;
        value = table.get("account_pop_maxMessageLines");
        if (value instanceof Integer) {
        	maxMessageLines = ((Integer) value).intValue();
        }
    }    
}
