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

import java.util.Hashtable;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.Node;
import org.logicprobe.LogicMail.model.AccountNode;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Provides a common dialog for selecting a mailbox
 */
public class MailboxSelectionDialog extends Dialog {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	private AccountNode[] accounts;
	private Hashtable nodeIdMap;
	private MailboxNode selectedMailboxNode;
	private TreeField treeField;
	private Hashtable unselectableNodeSet;
	
	/**
	 * Constructs a new MailboxSelectionDialog.
	 * 
	 * @param title Title of the dialog
	 * @param accounts Accounts to display mailboxes from
	 */
	public MailboxSelectionDialog(String title, AccountNode[] accounts) {
		super(
			title,
			new Object[0],
			new int[0],
			Dialog.OK, null, VerticalFieldManager.VERTICAL_SCROLLBAR);
		setEscapeEnabled(true);
		this.accounts = accounts;
		
		initFields();
		
		nodeIdMap = new Hashtable();
		unselectableNodeSet = new Hashtable();
		populateTreeField();
	}

	private void initFields() {
		treeField = new TreeField(new TreeFieldCallback() {
			public void drawTreeItem(TreeField treeField, Graphics graphics,
					int node, int y, int width, int indent) {
				treeField_DrawTreeItem(treeField, graphics, node, y, width, indent);
			}
		}, Field.FOCUSABLE);
		treeField.setDefaultExpanded(true);
		treeField.setIndentWidth(20);
		
		add(treeField);
	}

	/**
	 * Sets the selected mailbox node.
	 * 
	 * @param mailboxNode Selected node
	 */
	public void setSelectedMailboxNode(MailboxNode mailboxNode) {
		if(mailboxNode != null && nodeIdMap.containsKey(mailboxNode)) {
			int id = ((Integer)nodeIdMap.get(mailboxNode)).intValue();
			treeField.setCurrentNode(id);
		}
		selectedMailboxNode = mailboxNode;
	}
	
	/**
	 * Gets the selected mailbox node.
	 * 
	 * @return Selected node
	 */
	public MailboxNode getSelectedMailboxNode() {
		return selectedMailboxNode;
	}

	/**
	 * Marks a mailbox node as unselectable.
	 * Unselectable mailboxes can still be set as the initial selection,
	 * but cannot be chosen by the user afterwards.
	 * 
	 * @param mailboxNode Unselectable node.
	 */
	public void addUnselectableNode(MailboxNode mailboxNode) {
		if(!unselectableNodeSet.containsKey(mailboxNode)) {
			unselectableNodeSet.put(mailboxNode, new Object());
		}
	}
	
	/**
	 * Removes a mailbox node from the unselectable set.
	 * 
	 * @param mailboxNode Unselectable node.
	 */
	public void removeUnselectableNode(MailboxNode mailboxNode) {
		if(unselectableNodeSet.containsKey(mailboxNode)) {
			unselectableNodeSet.remove(mailboxNode);
		}
	}
	
	private void populateTreeField() {
		int firstNode = -1;
		for(int i=accounts.length - 1; i >= 0; --i) {
			int id = treeField.addChildNode(0, accounts[i]);
			if(i == 0) { firstNode = id; }
			nodeIdMap.put(accounts[i], new Integer(id));
			
			MailboxNode rootMailbox = accounts[i].getRootMailbox();
			if(rootMailbox != null) {
				MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
				for(int j=mailboxNodes.length - 1; j>=0; --j) {
					populateTreeFieldMailbox(id, mailboxNodes[j]);
				}
			}
		}
		if(firstNode != -1) {
			treeField.setCurrentNode(firstNode);
		}
	}
	
	private void populateTreeFieldMailbox(int parent, MailboxNode mailboxNode) {
		int id = treeField.addChildNode(parent, mailboxNode);
		nodeIdMap.put(mailboxNode, new Integer(id));
		
		MailboxNode[] mailboxes = mailboxNode.getMailboxes();
		for(int i=mailboxes.length - 1; i >= 0; --i) {
			populateTreeFieldMailbox(id, mailboxes[i]);
		}
	}
	
	private void treeField_DrawTreeItem(
			TreeField treeField,
			Graphics graphics,
			int node, int y, int width, int indent) {
		Node treeNode = (Node)treeField.getCookie(node);
		Font font = graphics.getFont();
		if(treeNode instanceof AccountNode) {
			graphics.setFont(font.derive(Font.BOLD));
		}
		else if(treeNode instanceof MailboxNode && !((MailboxNode)treeNode).hasAppend()) {
			graphics.setFont(font.derive(Font.ITALIC));
		}
		else {
			graphics.setFont(font.derive(Font.PLAIN));
		}
		graphics.drawText(treeNode.getName(), indent, y, Graphics.ELLIPSIS, width);
	}

	private MailboxNode getCurrentMailboxNode() {
		MailboxNode result;
		int id = treeField.getCurrentNode();
		if(id != -1 && treeField.getCookie(id) instanceof MailboxNode) {
			result = (MailboxNode)treeField.getCookie(id);
			if(!result.hasAppend() || unselectableNodeSet.containsKey(result)) {
				return null;
			}
		}
		else {
			result = null;
		}
		return result;
	}
	
	protected boolean trackwheelUnclick(int status, int time) {
		MailboxNode currentMailboxNode = getCurrentMailboxNode();
		if(currentMailboxNode != null) {
			this.selectedMailboxNode = currentMailboxNode;
			this.close();
			return true;
		}
		else {
			return super.trackwheelClick(status, time);
		}
	}
	
	protected boolean keyChar(char key, int status, int time) {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
            	MailboxNode currentMailboxNode = getCurrentMailboxNode();
            	if(currentMailboxNode != null) {
            		this.selectedMailboxNode = currentMailboxNode;
	            	this.close();
	            	retval = true;
            	}
                break;
        }
        if(!retval) {
        	retval = super.keyChar(key, status, time);
        }
        return retval;
	}
}
