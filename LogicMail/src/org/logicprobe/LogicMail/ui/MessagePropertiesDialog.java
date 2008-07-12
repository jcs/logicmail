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

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;

public class MessagePropertiesDialog extends Dialog {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	private FolderMessage folderMessage;
	
	public MessagePropertiesDialog(MessageNode messageNode) {
		super(
			resources.getString(LogicMailResource.MESSAGEPROPERTIES_TITLE),
			new Object[] { resources.getString(LogicMailResource.MENUITEM_CLOSE) },
			new int[] { Dialog.OK },
			Dialog.OK, NodeIcons.getIcon(messageNode),
			VERTICAL_SCROLL | VERTICAL_SCROLLBAR);
		setEscapeEnabled(true);
		
		this.folderMessage = messageNode.getFolderMessage();
		initFields();
		
	}
	
	private void initFields() {
		MessageEnvelope envelope = folderMessage.getEnvelope();
		add(new LabelField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_SUBJECT) + " " + envelope.subject, Field.FOCUSABLE));
		add(new LabelField(resources.getString(LogicMailResource.MESSAGEPROPERTIES_DATE) + " " + envelope.date, Field.FOCUSABLE));
		
		initFieldAddress(resources.getString(LogicMailResource.MESSAGEPROPERTIES_FROM), envelope.from);
		initFieldAddress(resources.getString(LogicMailResource.MESSAGEPROPERTIES_REPLYTO), envelope.replyTo);
		initFieldAddress(resources.getString(LogicMailResource.MESSAGEPROPERTIES_TO), envelope.to);
		initFieldAddress(resources.getString(LogicMailResource.MESSAGEPROPERTIES_CC), envelope.cc);
	}
	
	private void initFieldAddress(String prefix, String[] addresses) {
		if(addresses != null) {
			if(addresses.length == 1) {
				add(new LabelField(prefix + " " + addresses[0], Field.FOCUSABLE));
			}
			else if(addresses.length > 1) {
				add(new LabelField(prefix));
				for(int i=0; i<addresses.length; i++) {
					add(new LabelField("  " + addresses[i], Field.FOCUSABLE));
				}
			}
		}
	}
}
