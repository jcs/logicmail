/*-
 * Copyright (c) 2011, Derek Konigsberg
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

import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.image.Image;
import net.rim.device.api.ui.image.ImageFactory;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.model.MessageNode;

public class MessageActionsBB60 extends MessageActions {
    private MenuItem contextReplyItem;
    private MenuItem contextForwardItem;
    private MenuItem contextReplyAllItem;
    private MenuItem contextMarkOpenedItem;
    private MenuItem contextMarkUnopenedItem;
    private MenuItem contextSelectItem;
    private MenuItem contextPropertiesItem;
    private MenuItem contextCopyToItem;
    private MenuItem contextMoveToItem;
    private MenuItem contextDeleteItem;
    private MenuItem contextUndeleteItem;
    
    private static final Image contextReplyImage = ImageFactory.createImage("shortcut-message-reply.png");
    private static final Image contextForwardImage = ImageFactory.createImage("shortcut-message-forward.png");
    private static final Image contextReplyAllImage = ImageFactory.createImage("message-reply-all_32x32.png");
    private static final Image contextMarkOpenedImage = ImageFactory.createImage("message-mark-opened_32x32.png");
    private static final Image contextMarkUnopenedImage = ImageFactory.createImage("message-mark-unopened_32x32.png");
    private static final Image contextSelectImage = ImageFactory.createImage("shortcut-message-open.png");
    private static final Image contextPropertiesImage = ImageFactory.createImage("search_32x32.png");
    private static final Image contextCopyToImage = ImageFactory.createImage("shortcut-folder.png");
    private static final Image contextMoveToImage = contextCopyToImage;
    private static final Image contextDeleteImage = ImageFactory.createImage("shortcut-message-delete.png");
    private static final Image contextUndeleteImage = ImageFactory.createImage("message-undelete_32x32.png");
    
    public MessageActionsBB60(NavigationController navigationController) {
        super(navigationController);
    }

    protected void initMenuItems() {
        super.initMenuItems();
        contextReplyItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLY, 400100, 2000) {
            public void run() {
                replyMessage(activeMessageNode);
            }
        };
        contextForwardItem = new MenuItem(resources, LogicMailResource.MENUITEM_FORWARD, 400110, 2000) {
            public void run() {
                forwardMessage(activeMessageNode);
            }
        };
        contextReplyAllItem = new MenuItem(resources, LogicMailResource.MENUITEM_REPLYTOALL, 400120, 2000) {
            public void run() {
                replyAllMessage(activeMessageNode);
            }
        };
        contextMarkOpenedItem = new MenuItem(resources, LogicMailResource.MENUITEM_MARK_OPENED, 400130, 2000) {
            public void run() {
                markMessageOpened(activeMessageNode);
            }
        };
        contextMarkUnopenedItem = new MenuItem(resources, LogicMailResource.MENUITEM_MARK_UNOPENED, 400140, 2000) {
            public void run() {
                markMessageUnopened(activeMessageNode);
            }
        };
        contextSelectItem = new MenuItem(resources, LogicMailResource.MENUITEM_SELECT, 400150, 1010) {
            public void run() {
                openMessage(activeMessageNode);
            }
        };
        contextPropertiesItem = new MenuItem(resources, LogicMailResource.MENUITEM_PROPERTIES, 400160, 1030) {
            public void run() {
                openMessageProperties(activeMessageNode);
            }
        };
        contextCopyToItem = new MenuItem(resources, LogicMailResource.MENUITEM_COPY_TO, 400170, 2000) {
            public void run() {
                copyToMailbox(activeMessageNode);
            }
        };
        contextMoveToItem = new MenuItem(resources, LogicMailResource.MENUITEM_MOVE_TO, 400180, 2000) {
            public void run() {
                moveToMailbox(activeMessageNode);
            }
        };
        contextDeleteItem = new MenuItem(resources, LogicMailResource.MENUITEM_DELETE, 400190, 2000) {
            public void run() {
                deleteMessage(activeMessageNode);
            }
        };
        contextUndeleteItem = new MenuItem(resources, LogicMailResource.MENUITEM_UNDELETE, 400200, 2000) {
            public void run() {
                undeleteMessage(activeMessageNode);
            }
        };
        contextReplyItem.setIcon(contextReplyImage);
        contextForwardItem.setIcon(contextForwardImage);
        contextReplyAllItem.setIcon(contextReplyAllImage);
        contextMarkOpenedItem.setIcon(contextMarkOpenedImage);
        contextMarkUnopenedItem.setIcon(contextMarkUnopenedImage);
        contextSelectItem.setIcon(contextSelectImage);
        contextPropertiesItem.setIcon(contextPropertiesImage);
        contextCopyToItem.setIcon(contextCopyToImage);
        contextMoveToItem.setIcon(contextMoveToImage);
        contextDeleteItem.setIcon(contextDeleteImage);
        contextUndeleteItem.setIcon(contextUndeleteImage);
    };
    
    public void makeContextMenu(Menu menu, int instance, MessageNode messageNode, boolean isOpen) {
        int items = getValidMenuItems(messageNode, isOpen);
        
        if((items & ITEM_REPLY) != 0) { menu.add(contextReplyItem); }
        if((items & ITEM_FORWARD) != 0) { menu.add(contextForwardItem); }
        if((items & ITEM_REPLY_ALL) != 0) { menu.add(contextReplyAllItem); }
        
        if((items & ITEM_MARK_UNOPENED) != 0) { menu.add(contextMarkUnopenedItem); }
        else if((items & ITEM_MARK_OPENED) != 0) { menu.add(contextMarkOpenedItem); }
        
        if((items & ITEM_SELECT) != 0) { menu.add(contextSelectItem); }
        if((items & ITEM_PROPERTIES) != 0) { menu.add(contextPropertiesItem); }
        
        if((items & ITEM_MOVE_TO) != 0) { menu.add(contextMoveToItem); }
        else if((items & ITEM_COPY_TO) != 0) { menu.add(contextCopyToItem); }
        
        if((items & ITEM_UNDELETE) != 0) { menu.add(contextUndeleteItem); }
        else if((items & ITEM_DELETE) != 0) { menu.add(contextDeleteItem); }
        
        this.activeMessageNode = messageNode;
    }
}
