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

import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.image.Image;
import net.rim.device.api.ui.image.ImageFactory;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.model.MailboxNode;

public class MailboxScreenBB60 extends MailboxScreen {
    private MenuItem separatorPreviousItem;
    private MenuItem separatorToTopItem;
    private MenuItem separatorNextItem;
    private MenuItem separatorToBottomItem;
    private MenuItem separatorComposeItem;
    private MenuItem separatorMarkPriorOpenedItem;

    private static final Image separatorPreviousImage = ImageFactory.createImage("go-previous_32x32.png");
    private static final Image separatorNextImage = ImageFactory.createImage("go-next_32x32.png");
    private static final Image separatorToTopImage = ImageFactory.createImage("go-top_32x32.png");
    private static final Image separatorToBottomImage = ImageFactory.createImage("go-bottom_32x32.png");
    private static final Image separatorComposeImage = ImageFactory.createImage("shortcut-compose.png");
    private static final Image separatorMarkPriorOpenedImage = ImageFactory.createImage("message-mark-opened_32x32.png");
    
    public MailboxScreenBB60(MailboxNode mailboxNode) {
        super(mailboxNode);
    }

    public String getScreenName() {
        return "MailboxScreen";
    }
    
    protected void initMenuItems() {
        super.initMenuItems();
        
        separatorPreviousItem = new MenuItem(resources, LogicMailResource.MENUITEM_PREVIOUS, 400010, 2000) {
            public void run() {
                if(currentContextField == null) { return; }
                Manager manager = currentContextField.getManager();
                int index = currentContextField.getIndex();
                for(int i=index - 1; i >= 0; --i) {
                    if(manager.getField(i) instanceof MessageSeparatorField) {
                        manager.getField(i).setFocus();
                        break;
                    }
                }
            }
        };
        separatorToTopItem = new MenuItem(resources, LogicMailResource.MENUITEM_TO_TOP, 400020, 2000) {
            public void run() {
                screen.scroll(Manager.TOPMOST);
            }
        };
        separatorNextItem = new MenuItem(resources, LogicMailResource.MENUITEM_NEXT, 400030, 2000) {
            public void run() {
                if(currentContextField == null) { return; }
                Manager manager = currentContextField.getManager();
                int count = manager.getFieldCount();
                int index = currentContextField.getIndex();
                for(int i=index + 1; i < count; i++) {
                    if(manager.getField(i) instanceof MessageSeparatorField) {
                        manager.getField(i).setFocus();
                        break;
                    }
                }
            }
        };
        separatorToBottomItem = new MenuItem(resources, LogicMailResource.MENUITEM_TO_BOTTOM, 400040, 2000) {
            public void run() {
                screen.scroll(Manager.BOTTOMMOST);
            }
        };
        separatorComposeItem = new MenuItem(resources, LogicMailResource.MENUITEM_COMPOSE, 400050, 1010) {
            public void run() {
                compositionItem.run();
            }
        };
        separatorMarkPriorOpenedItem = new MenuItem(resources, LogicMailResource.MENUITEM_MARK_PRIOR_OPENED, 400060, 2000) {
            public void run() {
                markPriorOpenedItem.run();
            }
        };
        
        separatorPreviousItem.setIcon(separatorPreviousImage);
        separatorToTopItem.setIcon(separatorToTopImage);
        separatorNextItem.setIcon(separatorNextImage);
        separatorToBottomItem.setIcon(separatorToBottomImage);
        separatorComposeItem.setIcon(separatorComposeImage);
        separatorMarkPriorOpenedItem.setIcon(separatorMarkPriorOpenedImage);
    }

    protected void makeSeparatorMenu(MessageSeparatorField separatorField, Menu menu, int instance) {
        if(instance == Menu.INSTANCE_DEFAULT) {
            super.makeSeparatorMenu(separatorField, menu, instance);
        }
        else {
            Manager manager = separatorField.getManager();
            int count = manager.getFieldCount();
            int index = separatorField.getIndex();

            boolean hasPrevious = false;
            for(int i=index - 1; i >= 0; --i) {
                if(manager.getField(i) instanceof MessageSeparatorField) {
                    hasPrevious = true;
                    break;
                }
            }
            
            boolean hasNext = false;
            for(int i=index + 1; i < count; i++) {
                if(manager.getField(i) instanceof MessageSeparatorField) {
                    hasNext = true;
                    break;
                }
            }
            
            if(hasPrevious) {
                menu.add(separatorPreviousItem);
            }
            
            if(index > 0) {
                menu.add(separatorToTopItem);
            }
            
            if(hasNext) {
                menu.add(separatorNextItem);
            }

            if(index < count - 1) {
                menu.add(separatorToBottomItem);
            }
            
            if(composeEnabled) {
                menu.add(separatorComposeItem);
            }
            
            menu.add(separatorMarkPriorOpenedItem);
        }
    }
    
    public boolean navigationClick(int status, int time) {
        // This call needs to be rerouted so the long-press action
        // correctly triggers the pop-up context menu.
        return false;
    }
    
    public boolean navigationUnclick(int status, int time) {
        return super.navigationClick(status, time);
    }
}
