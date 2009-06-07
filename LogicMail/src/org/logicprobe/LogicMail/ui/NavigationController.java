/*-
 * Copyright (c) 2009, Derek Konigsberg
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
package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.UiApplication;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;

/**
 * Controller class for screen creation.
 * This class is intended to hide all screen creation logic
 * from the rest of the UI, and provide a simple interface
 * in terms of viewing data model objects.
 */
public class NavigationController {
	private MailRootNode mailRootNode;
	
	private UiApplication uiApplication;
	private MailHomeScreen mailHomeView;
	private MailHomePresenter mailHomePresenter;
	
	public NavigationController(UiApplication uiApplication) {
		this.uiApplication = uiApplication;
		this.mailRootNode = MailManager.getInstance().getMailRootNode();
	}
	
	public synchronized void displayMailHome() {
		if(mailHomePresenter == null) {
			mailHomeView = new MailHomeScreen();
			mailHomePresenter = new MailHomePresenter(this, mailRootNode, mailHomeView);
			mailHomePresenter.initialize();
		}
		uiApplication.pushScreen(mailHomeView);
	}
	
	public synchronized void displayAccountConfigurationWizard() {
		UiApplication.getUiApplication().invokeLater(new Runnable() {
			public void run() {
				// Start the new account configuration wizard
				AccountConfigWizard wizard = new AccountConfigWizard();
				if(wizard.start()) {
					AccountConfig newAccountConfig = wizard.getAccountConfig();
					
					// Find the newly created account, and trigger a folder refresh (if applicable)
					AccountNode[] accounts = mailRootNode.getAccounts();
					for(int i=0; i<accounts.length; i++) {
						if(accounts[i].getAccountConfig() == newAccountConfig) {
							if(accounts[i].hasFolders()) {
								accounts[i].refreshMailboxes();
							}
							break;
						}
					}
				}
			}
		});
	}
	
	public synchronized void displayMailbox(MailboxNode mailboxNode) {
		MailboxScreen screen = new MailboxScreen(this, mailboxNode);
		uiApplication.pushScreen(screen);
	}
	
	public synchronized void displayMessage(MessageNode messageNode) {
		MessageScreen screen = new MessageScreen(this, messageNode);
		uiApplication.pushScreen(screen);
	}
	
	public synchronized void displayComposition(AccountNode accountNode) {
		CompositionScreen screen = new CompositionScreen(this, accountNode);
		uiApplication.pushScreen(screen);
	}

	public synchronized void displayComposition(AccountNode accountNode, MessageNode messageNode) {
		CompositionScreen screen = new CompositionScreen(this,
				accountNode,
				messageNode,
				CompositionScreen.COMPOSE_NORMAL);
		uiApplication.pushScreen(screen);
	}

	public void displayComposition(AccountNode accountNode, String address) {
		CompositionScreen screen = new CompositionScreen(this, accountNode, address);
		uiApplication.pushScreen(screen);
	}
	
	public synchronized void displayCompositionReply(AccountNode accountNode, MessageNode messageNode, boolean replyAll) {
		CompositionScreen screen = new CompositionScreen(this,
				accountNode,
				messageNode,
				replyAll ? CompositionScreen.COMPOSE_REPLY_ALL : CompositionScreen.COMPOSE_REPLY);
		uiApplication.pushScreen(screen);
	}

	public synchronized void displayCompositionForward(AccountNode accountNode, MessageNode messageNode) {
		CompositionScreen screen = new CompositionScreen(this,
				accountNode,
				messageNode,
				CompositionScreen.COMPOSE_FORWARD);
		uiApplication.pushScreen(screen);
	}
}
