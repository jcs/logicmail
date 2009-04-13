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

import org.logicprobe.LogicMail.model.Node;

/**
 * View interface for the mail home screen.
 */
public interface MailHomeView extends BaseView {
	public static final int MENUITEM_SELECT           = 1 << 0;
	public static final int MENUITEM_REFRESH_STATUS   = 1 << 1;
	public static final int MENUITEM_REFRESH_FOLDERS  = 1 << 2;
	public static final int MENUITEM_COMPOSE_EMAIL    = 1 << 3;
	public static final int MENUITEM_DISCONNECT       = 1 << 4;
	
	/** Tree node data class */
	static class TreeNode {
		public static final int TYPE_ACCOUNT = 1;
		public static final int TYPE_MAILBOX = 2;
		
		public Node node;
		public int type;
		public TreeNode[] children;
		
		public TreeNode(Node node, int type) {
			this.node = node;
			this.type = type;
		}
	}
	
	/**
	 * Sets the <tt>MailHomeListener</tt> instance used to handle events
	 * relating to this view.
	 * 
	 * @param listener The <tt>MailHomeListener</tt> to be set.
	 */
	void setMailHomeListener(MailHomeListener listener);
	
	/**
	 * Populate the displayed mail tree.
	 * Will clear out any existing items before displaying the new tree.
	 * 
	 * @param rootNode Root node of the tree.
	 */
	void populateMailTree(TreeNode rootNode);
	
	/**
	 * Refresh a node of the displayed mail tree.
	 * 
	 * @param node Node to refresh.
	 */
	void refreshMailTreeNode(TreeNode node);
}
