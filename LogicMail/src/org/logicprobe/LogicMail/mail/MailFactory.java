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

package org.logicprobe.LogicMail.mail;

import java.util.Hashtable;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.OutgoingConfig;

/**
 * Factory to handle creation and configuration of
 * concrete mail store and sender instances
 */
public class MailFactory {
	private static LocalMailStore localMailStore;
    private static Hashtable mailStoreTable = new Hashtable();
    private static Hashtable mailSenderTable = new Hashtable();
    
	private MailFactory() { }
	
	/**
	 * Gets the local mail store instance.
	 * 
	 * @return Mail store instance
	 */
	public static AbstractMailStore createLocalMailStore() {
		if(localMailStore == null) {
			localMailStore = new LocalMailStore();
		}
		return localMailStore;
	}
	
	/**
	 * Get the mail store instance for the provided account.
	 * 
	 * @param accountConfig Account configuration
	 * @return Mail store instance
	 */
	public static AbstractMailStore createMailStore(AccountConfig accountConfig) {
		AbstractMailStore mailStore = (AbstractMailStore)mailStoreTable.get(accountConfig);
		
		if(mailStore == null) {
			mailStore = new NetworkMailStore(accountConfig);
			mailStoreTable.put(accountConfig, mailStore);
		}
		
		return mailStore;
	}
	
	/**
	 * Clear the mail store instance for the provided account.
	 * Should only be used when the account is deleted, to prevent
	 * a reference to the mail store from being kept.
	 * 
	 * @param accountConfig Account configuration
	 */
	public static void clearMailStore(AccountConfig accountConfig) {
		mailStoreTable.remove(accountConfig);
	}
	
	/**
	 * Get the mail sender instance for the provided configuration.
	 * 
	 * @param outgoingConfig Configuration
	 * @return Mail sender instance
	 */
	public static AbstractMailSender createMailSender(OutgoingConfig outgoingConfig) {
		AbstractMailSender mailSender = (AbstractMailSender)mailSenderTable.get(outgoingConfig);
		
		if(mailSender == null) {
			mailSender = new NetworkMailSender(outgoingConfig);
			mailSenderTable.put(outgoingConfig, mailSender);
		}
		
		return mailSender;
	}
	
	/**
	 * Clear the mail sender instance for the provided configuration.
	 * Should only be used when the configuration is deleted, to prevent
	 * a reference to the mail sender from being kept.
	 * 
	 * @param outgoingConfig Configuration
	 */
	public static void clearMailSender(OutgoingConfig outgoingConfig) {
		mailSenderTable.remove(outgoingConfig);
	}
}
