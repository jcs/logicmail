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

import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;

import net.rim.device.api.ui.Touchscreen;
import net.rim.device.api.ui.VirtualKeyboard;
import net.rim.device.api.ui.component.Dialog;

public class ScreenFactoryBB47 extends ScreenFactoryBB45 {
    protected boolean hasTouchscreen;
    
    public ScreenFactoryBB47() {
        hasTouchscreen = Touchscreen.isSupported();
    }
    
    public StandardScreen getMailHomeScreen(NavigationController navigationController, MailRootNode mailRootNode) {
        if(hasTouchscreen) {
            return getStandardTouchScreen(navigationController, new TouchMailHomeScreen(mailRootNode));
        }
        else {
            return getStandardScreen(navigationController, new MailHomeScreen(mailRootNode));
        }
    }

    public StandardScreen getMailboxScreen(NavigationController navigationController, MailboxNode mailboxNode) {
        if(hasTouchscreen) {
            return getStandardTouchScreen(navigationController, new MailboxScreen(mailboxNode));
        }
        else {
            return getStandardScreen(navigationController, new MailboxScreen(mailboxNode));
        }
    }
    
    public String showFilePicker() {
        FilePickerDialog dialog = new FilePickerDialog();
        if(hasTouchscreen) {
            dialog.getVirtualKeyboard().setVisibility(VirtualKeyboard.HIDE_FORCE);
        }
        if(dialog.doModal() == Dialog.OK) {
            return dialog.getFileUrl();
        }
        else {
            return null;
        }
    }
    
    protected StandardScreen getStandardTouchScreen(NavigationController navigationController, ScreenProvider screenProvider) {
        return new StandardTouchScreen(navigationController, screenProvider);
    }
}
