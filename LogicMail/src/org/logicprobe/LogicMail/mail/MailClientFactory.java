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

package org.logicprobe.LogicMail.mail;

import java.util.Hashtable;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.conf.PopConfig;
import org.logicprobe.LogicMail.mail.imap.ImapClient;
import org.logicprobe.LogicMail.mail.pop.PopClient;
import org.logicprobe.LogicMail.mail.smtp.SmtpClient;

/**
 * Factory to handle creation and configuration of
 * concrete IncomingMailClient instances
 */
public class MailClientFactory {
    private static Hashtable incomingClientTable = new Hashtable();
    private static Hashtable outgoingClientTable = new Hashtable();
    
    private MailClientFactory() { }
    
    /**
     * Get a concrete mail client instance.
     * If a client already exists for the provided configuration,
     * it will be provided instead of a new one.
     *
     * @param acctConfig User account configuration
     * @return Usable mail client instance
     */
    public static IncomingMailClient createMailClient(AccountConfig acctConfig) {
        IncomingMailClient client = (IncomingMailClient)incomingClientTable.get(acctConfig);
        
        if(client == null) {
            GlobalConfig globalConfig = MailSettings.getInstance().getGlobalConfig();
            if(acctConfig instanceof PopConfig) {
                client = new PopClient(globalConfig, (PopConfig)acctConfig);
                incomingClientTable.put(acctConfig, client);
            }
            else if(acctConfig instanceof ImapConfig) {
                client = new ImapClient(globalConfig, (ImapConfig)acctConfig);
                incomingClientTable.put(acctConfig, client);
            }
        }
        return client;
    }
    
    /**
     * Explicitly set a particular incoming client instance to
     * match a particular account configuration.
     * This method is intended to be used only for testing.
     * 
     * @param acctConfig User account configuration
     * @param client Usable mail client instance
     */
    static void setIncomingMailClient(AccountConfig acctConfig, IncomingMailClient client) {
    	incomingClientTable.put(acctConfig, client);
    }
    
    /**
     * Get a concrete outgoing mail client instance.
     * If a client already exists for the provided configuration,
     * it will be provided instead of a new one.
     *
     * @param outgoingConfig Outgoing configuration
     * @return Usable outgoing mail client instance
     */
    public static OutgoingMailClient createOutgoingMailClient(OutgoingConfig outgoingConfig) {
        OutgoingMailClient client = null;
        if(outgoingConfig != null) {
            client = (OutgoingMailClient)outgoingClientTable.get(outgoingConfig);

            if(client == null) {
                GlobalConfig globalConfig = MailSettings.getInstance().getGlobalConfig();
                client = new SmtpClient(globalConfig, outgoingConfig);
                outgoingClientTable.put(outgoingConfig, client);
            }
        }
        return client;
    }

    /**
     * Explicitly set a particular outgoing client instance to
     * match a particular account configuration.
     * This method is intended to be used only for testing.
     * 
     * @param outgoingConfig Outgoing configuration
     * @param client Usable mail client instance
     */
    static void setOutgoingMailClient(OutgoingConfig outgoingConfig, OutgoingMailClient client) {
    	outgoingClientTable.put(outgoingConfig, client);
    }
}
