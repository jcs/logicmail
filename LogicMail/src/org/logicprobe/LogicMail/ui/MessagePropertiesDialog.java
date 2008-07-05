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
package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;

import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;

public class MessagePropertiesDialog extends Dialog {
	private FolderMessage folderMessage;
	
	public MessagePropertiesDialog(MessageNode messageNode) {
		super(
			"Message Properties",
			new Object[] { "Close" },
			new int[] { Dialog.OK },
			Dialog.OK, NodeIcons.getIcon(messageNode),
			VERTICAL_SCROLL | VERTICAL_SCROLLBAR);
		setEscapeEnabled(true);
		
		this.folderMessage = messageNode.getFolderMessage();
		initFields();
		
	}
	
	private void initFields() {
		MessageEnvelope envelope = folderMessage.getEnvelope();
		add(new LabelField("Subject: " + envelope.subject, Field.FOCUSABLE));
		add(new LabelField("Date: " + envelope.date, Field.FOCUSABLE));
		if(envelope.from != null && envelope.from.length > 0) {
			add(new LabelField("From:", Field.FOCUSABLE));
			for(int i=0; i<envelope.from.length; i++) {
				add(new LabelField("  " + envelope.from[i], Field.FOCUSABLE));
			}
		}
		if(envelope.replyTo != null && envelope.replyTo.length > 0) {
			add(new LabelField("Reply To:", Field.FOCUSABLE));
			for(int i=0; i<envelope.replyTo.length; i++) {
				add(new LabelField("  " + envelope.replyTo[i], Field.FOCUSABLE));
			}
		}
		if(envelope.to != null && envelope.to.length > 0) {
			add(new LabelField("To:", Field.FOCUSABLE));
			for(int i=0; i<envelope.to.length; i++) {
				add(new LabelField("  " + envelope.to[i], Field.FOCUSABLE));
			}
		}
		if(envelope.cc != null && envelope.cc.length > 0) {
			add(new LabelField("Cc:", Field.FOCUSABLE));
			for(int i=0; i<envelope.cc.length; i++) {
				add(new LabelField("  " + envelope.cc[i], Field.FOCUSABLE));
			}
		}
	}
	
}
