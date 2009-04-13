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

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.Field;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.model.MailboxNode;

/**
 * Main screen for the application, providing a unified
 * view of accounts and folders.
 */
public class MailHomeScreen extends BaseScreen implements MailHomeView {
	private MailHomeListener listener;
	
	private TreeField treeField;

	private MenuItem selectFolderItem;
	private MenuItem refreshStatusItem;
	private MenuItem refreshFoldersItem;
	private MenuItem compositionItem;
	private MenuItem disconnectItem;
	
	private Hashtable nodeIdMap;
	
	public MailHomeScreen() {
		super(null, resources.getString(LogicMailResource.MAILHOME_TITLE));
	
		initFields();
		initMenuItems();
		this.nodeIdMap = new Hashtable();
	}

	private void initFields() {
		treeField = new TreeField(new TreeFieldCallback() {
			public void drawTreeItem(TreeField treeField, Graphics graphics,
					int node, int y, int width, int indent) {
				treeField_DrawTreeItem(treeField, graphics, node, y, width, indent);
			}
		}, Field.FOCUSABLE);
		treeField.setEmptyString(resources.getString(LogicMailResource.MAILHOME_NOACCOUNTS), 0);
		treeField.setDefaultExpanded(true);
		treeField.setIndentWidth(20);
		
		add(treeField);
	}
	
	private void initMenuItems() {
	    selectFolderItem = new ContextMenuItem(MailHomeView.MENUITEM_SELECT, resources, LogicMailResource.MENUITEM_SELECT, 100, 8);
	    refreshStatusItem = new ContextMenuItem(MailHomeView.MENUITEM_REFRESH_STATUS, resources, LogicMailResource.MENUITEM_REFRESH_STATUS, 110, 10);
	    refreshFoldersItem = new ContextMenuItem(MailHomeView.MENUITEM_REFRESH_FOLDERS, resources, LogicMailResource.MENUITEM_REFRESH_FOLDERS, 111, 10);
	    compositionItem = new ContextMenuItem(MailHomeView.MENUITEM_COMPOSE_EMAIL, resources, LogicMailResource.MENUITEM_COMPOSE_EMAIL, 200000, 9);
	    disconnectItem = new ContextMenuItem(MailHomeView.MENUITEM_DISCONNECT, resources, LogicMailResource.MENUITEM_DISCONNECT, 200000, 9);
	}

	private class ContextMenuItem extends MenuItem {
		private int menuItemId;
		
		public ContextMenuItem(int menuItemId, ResourceBundle bundle, int id, int ordinal, int priority) {
			super(bundle, id, ordinal, priority);
			this.menuItemId = menuItemId;
		}

		public void run() {
			int nodeId = treeField.getCurrentNode();
      		if(listener != null && nodeId != -1) {
      			listener.handleContextMenuItem(
      					menuItemId,
      					(TreeNode)treeField.getCookie(nodeId));
      		}
		}
	}
	
	public void setMailHomeListener(MailHomeListener listener) {
		this.listener = listener;
	}
	
	public void populateMailTree(TreeNode rootNode) {
		synchronized(UiApplication.getEventLock()) {
			// Clear any existing nodes
			treeField.deleteAll();
			nodeIdMap.clear();
	
			// Freshly populate the tree
			int firstNode = -1;
			TreeNode[] nodes = rootNode.children;
			if(nodes != null) {
				for(int i = nodes.length - 1; i >= 0; --i) {
					int id = treeField.addChildNode(0, nodes[i]);
					nodeIdMap.put(nodes[i], new Integer(id));
					if(i == 0) { firstNode = id; }
					populateMailTreeChildren(id, nodes[i]);
				}
			}
			if(firstNode != -1) {
				treeField.setCurrentNode(firstNode);
			}
		}
	}

	private void populateMailTreeChildren(int parent, TreeNode node) {
		if(node.children != null) {
			for(int i = node.children.length - 1; i >= 0; --i) {
				int id = treeField.addChildNode(parent, node.children[i]);
				nodeIdMap.put(node.children[i], new Integer(id));
				populateMailTreeChildren(id, node.children[i]);
			}
		}
	}
	
	public void refreshMailTreeNode(TreeNode node) {
		Integer nodeInt = (Integer)nodeIdMap.get(node);
		if(nodeInt != null) {
			synchronized(UiApplication.getEventLock()) {
				treeField.invalidateNode(nodeInt.intValue());
			}
		}
	}
	
	public boolean onClose() {
		tryShutdownApplication();
		return false;
	}

	protected void onVisibilityChange(boolean visible) {
		super.onVisibilityChange(visible);

		if(listener != null) {
			listener.handleVisibilityChange(visible);
		}
	}
	
    protected void makeMenu(Menu menu, int instance) {
    	if(listener != null) {
    		int menuItems = listener.getContextMenuItems((TreeNode)treeField.getCookie(treeField.getCurrentNode()));

    		if((menuItems & MailHomeView.MENUITEM_SELECT)!=0) { menu.add(selectFolderItem); }
			if((menuItems & MailHomeView.MENUITEM_REFRESH_STATUS)!=0) { menu.add(refreshStatusItem); }
			if((menuItems & MailHomeView.MENUITEM_REFRESH_FOLDERS)!=0) { menu.add(refreshFoldersItem); }
			if((menuItems & MailHomeView.MENUITEM_COMPOSE_EMAIL)!=0) { menu.add(compositionItem); }
			if((menuItems & MailHomeView.MENUITEM_DISCONNECT)!=0) { menu.add(disconnectItem); }
    	}
        super.makeMenu(menu, instance);
    }

    protected boolean onClick() {
    	selectFolderItem.run();
    	return true;
    }
    
    public boolean keyChar(char key, int status, int time) {
		boolean retval = false;
		switch (key) {
		case Keypad.KEY_ENTER:
			selectFolderItem.run();
			retval = true;
			break;
		}
		return retval;
	}
    
	private void treeField_DrawTreeItem(
			TreeField treeField,
			Graphics graphics,
			int node, int y, int width, int indent) {
		TreeNode treeNode = (TreeNode)treeField.getCookie(node);
		int rowHeight = treeField.getRowHeight();
		int fontHeight = graphics.getFont().getHeight();
		
		Bitmap icon = NodeIcons.getIcon(treeNode.node);

		graphics.drawBitmap(
				indent + (rowHeight/2 - icon.getWidth()/2),
				y + (fontHeight/2 - icon.getWidth()/2),
				icon.getWidth(),
				icon.getHeight(),
				icon, 0, 0);
		
		Font origFont = graphics.getFont();
		StringBuffer buf = new StringBuffer();
		buf.append(treeNode.node.toString());
		
		if(treeNode.type == TreeNode.TYPE_ACCOUNT) {
			graphics.setFont(origFont.derive(Font.BOLD));
		}
		else if(treeNode.type == TreeNode.TYPE_MAILBOX) {
			MailboxNode mailboxNode = (MailboxNode)treeNode.node;
			
			if(!mailboxNode.isSelectable()) {
                graphics.setFont(origFont.derive(Font.ITALIC));
			}
			
			int unseenCount = mailboxNode.getUnseenMessageCount();
			if(unseenCount > 0) {
                buf.append(" (");
                buf.append(unseenCount);
                buf.append(")");
                graphics.setFont(origFont.derive(Font.BOLD));
			}
			else {
                graphics.setFont(origFont.derive(Font.PLAIN));
			}
		}
		graphics.drawText(buf.toString(), indent + rowHeight, y, Graphics.ELLIPSIS, width);
	}
}
