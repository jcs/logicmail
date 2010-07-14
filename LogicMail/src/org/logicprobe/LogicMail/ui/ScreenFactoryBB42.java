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

import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.Dialog;

import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;

public class ScreenFactoryBB42 extends ScreenFactory {

	public ScreenFactoryBB42() {
	}
	
	public void attachScreenTransition(Screen screen, int transitionType) {
	    // Screen transitions are not supported on this platform version
	}
	
	public void clearScreenTransition(Screen screen) {
        // Screen transitions are not supported on this platform version
	}
	
	public StandardScreen getMailHomeScreen(NavigationController navigationController, MailRootNode mailRootNode) {
		return new StandardScreen(navigationController, new MailHomeScreen(mailRootNode));
	}

	public StandardScreen getMailboxScreen(NavigationController navigationController, MailboxNode mailboxNode) {
		return new StandardScreen(navigationController, new MailboxScreen(mailboxNode));
	}
	
	public StandardScreen getMessageScreen(NavigationController navigationController, MessageNode messageNode) {
		return new StandardScreen(navigationController, new MessageScreen(messageNode));
	}

	public StandardScreen getCompositionScreen(NavigationController navigationController, AccountNode accountNode) {
		return new StandardScreen(navigationController, new CompositionScreen(accountNode));
	}
	
	public StandardScreen getCompositionScreen(NavigationController navigationController, AccountNode accountNode, MessageNode messageNode) {
		return new StandardScreen(navigationController, new CompositionScreen(
				accountNode,
				messageNode,
				CompositionScreen.COMPOSE_NORMAL));
	}
	
	public StandardScreen getCompositionScreen(NavigationController navigationController, AccountNode accountNode, String address) {
		return new StandardScreen(navigationController, new CompositionScreen(accountNode, address));
	}
	
	public StandardScreen getCompositionReplyScreen(NavigationController navigationController, AccountNode accountNode, MessageNode messageNode, boolean replyAll) {
		return new StandardScreen(navigationController, new CompositionScreen(
				accountNode,
				messageNode,
				replyAll ? CompositionScreen.COMPOSE_REPLY_ALL : CompositionScreen.COMPOSE_REPLY));
	}
	
	public StandardScreen getCompositionForwardScreen(NavigationController navigationController, AccountNode accountNode, MessageNode messageNode) {
		return new StandardScreen(navigationController, new CompositionScreen(
				accountNode,
				messageNode,
				CompositionScreen.COMPOSE_FORWARD));
	}
	
	public String showFilePicker() {
	    FilePickerDialog dialog = new FilePickerDialog();
	    if(dialog.doModal() == Dialog.OK) {
	        return dialog.getFileUrl();
	    }
	    else {
	        return null;
	    }
	}
}
