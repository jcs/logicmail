/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.Node;

import net.rim.device.api.system.Bitmap;

public class TouchNodeIcons extends NodeIcons {
    private NodeIconVisitor bigVisitor = new BigNodeIconVisitor();
    
    private Bitmap bigLocalAccountIcon;
    private Bitmap bigNetworkAccountIcon0;
    private Bitmap bigNetworkAccountIcon1;
    private Bitmap bigFolderIcon;
    private Bitmap bigFolderNewIcon;
    private Bitmap bigInboxFolderIcon;
    private Bitmap bigInboxNewFolderIcon;
    private Bitmap bigOutboxFolderIcon;
    private Bitmap bigDraftsFolderIcon;
    private Bitmap bigSentFolderIcon;
    private Bitmap bigTrashFolderIcon;
    private Bitmap bigTrashFullFolderIcon;

    protected TouchNodeIcons() {
        super();
        
        // Load account icons
        bigLocalAccountIcon    = Bitmap.getBitmapResource("account_local_32x32.png");
        bigNetworkAccountIcon0 = Bitmap.getBitmapResource("account_network_offline_32x32.png");
        bigNetworkAccountIcon1 = Bitmap.getBitmapResource("account_network_online_32x32.png");
        
        // Load folder icons
        bigFolderIcon          = Bitmap.getBitmapResource("folder_32x32.png");
        bigFolderNewIcon       = Bitmap.getBitmapResource("folder_new_32x32.png");
        bigInboxFolderIcon     = Bitmap.getBitmapResource("folder_inbox_32x32.png");
        bigInboxNewFolderIcon  = Bitmap.getBitmapResource("folder_inbox_new_32x32.png");
        bigOutboxFolderIcon    = Bitmap.getBitmapResource("folder_outbox_32x32.png");
        bigDraftsFolderIcon    = Bitmap.getBitmapResource("folder_drafts_32x32.png");
        bigSentFolderIcon      = Bitmap.getBitmapResource("folder_sent_32x32.png");
        bigTrashFolderIcon     = Bitmap.getBitmapResource("folder_trash_32x32.png");
        bigTrashFullFolderIcon = Bitmap.getBitmapResource("folder_trash_full_32x32.png");
    }
    
    public static Bitmap getBigIcon(Node node) {
        return ((TouchNodeIcons)instance).getBigIconImpl(node);
    }
    
    public static Bitmap getBigIcon(int type) {
        return ((TouchNodeIcons)instance).getBigIconImpl(type);
    }
    
    private Bitmap getBigIconImpl(Node node) {
        bigVisitor.clearIcon();
        node.accept(bigVisitor);
        return bigVisitor.getIcon();
    }
    
    private Bitmap getBigIconImpl(int type) {
        Bitmap icon;
        switch(type) {
        case ICON_FOLDER:
            icon = bigFolderIcon;
            break;
        default:
            icon = null;
        }
        return icon;
    }
    
    protected class BigNodeIconVisitor extends NodeIconVisitor {
        
        public void visit(AccountNode node) {
            switch(node.getStatus()) {
            case AccountNode.STATUS_LOCAL:
                this.icon = bigLocalAccountIcon;
                break;
            case AccountNode.STATUS_OFFLINE:
                this.icon = bigNetworkAccountIcon0;
                break;
            case AccountNode.STATUS_ONLINE:
                this.icon = bigNetworkAccountIcon1;
                break;
            }
        }

        public void visit(MailboxNode node) {
            switch(node.getType()) {
            case MailboxNode.TYPE_INBOX:
                if(node.getUnseenMessageCount() > 0) {
                    this.icon = bigInboxNewFolderIcon;
                }
                else {
                    this.icon = bigInboxFolderIcon;
                }
                break;
            case MailboxNode.TYPE_DRAFTS:
                this.icon = bigDraftsFolderIcon;
                break;
            case MailboxNode.TYPE_SENT:
                this.icon = bigSentFolderIcon;
                break;
            case MailboxNode.TYPE_TRASH:
                if(node.getMessageCount() > 0) {
                    this.icon = bigTrashFullFolderIcon;
                }
                else {
                    this.icon = bigTrashFolderIcon;
                }
                break;
            case MailboxNode.TYPE_OUTBOX:
                this.icon = bigOutboxFolderIcon;
                break;
            case MailboxNode.TYPE_NORMAL:
            default:
                if(node.getUnseenMessageCount() > 0) {
                    this.icon = bigFolderNewIcon;
                }
                else {
                    this.icon = bigFolderIcon;
                }
                break;
            }
        }
    }
}
