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

import net.rim.device.api.command.AlwaysExecutableCommand;
import net.rim.device.api.command.Command;
import net.rim.device.api.command.ReadOnlyCommandMetadata;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.image.ImageFactory;
import net.rim.device.api.ui.toolbar.ToolbarButtonField;
import net.rim.device.api.ui.toolbar.ToolbarManager;
import net.rim.device.api.ui.toolbar.ToolbarSpacer;
import net.rim.device.api.util.IntHashtable;
import net.rim.device.api.util.StringProvider;

import org.logicprobe.LogicMail.ui.ScreenProvider.ShortcutItem;

public class StandardTouchScreenBB60 extends StandardScreenBB60 {
    private ToolbarManager toolbarManager;
    private IntHashtable shortcutIdMap;
    
    public StandardTouchScreenBB60(NavigationController navigationController, ScreenProvider screenProvider) {
        super(navigationController, screenProvider);
        
        if(screenProvider.hasShortcuts()) {
            ShortcutItem[] shortcuts = screenProvider.getShortcuts();
            if(shortcuts != null) {
                initializeShortcutBar(shortcuts);
            }
        }
    }

    private void initializeShortcutBar(ShortcutItem[] shortcuts) {
        shortcutIdMap = new IntHashtable(5);
        toolbarManager = new ToolbarManager();
        int len = Math.min(shortcuts.length, 5);
        for(int i=0; i<len; i++) {
            ToolbarButtonField button;
            if(shortcuts[i] == null) {
                button = new ToolbarSpacer(i);
            }
            else {
                button = new ToolbarButtonField(
                        ImageFactory.createImage(
                                Bitmap.getBitmapResource(shortcuts[i].getEnabledIcon())),
                        new StringProvider(shortcuts[i].getName()), i);
                button.setEnabled(true);
                button.setCommand(new Command(new ShortcutCommand(shortcuts[i])));
                shortcutIdMap.put(shortcuts[i].getId(), button);
            }
            toolbarManager.add(button);
        }
        setStatus(toolbarManager);
        
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.StandardScreen#isShortcutEnabled(int)
     */
    public boolean isShortcutEnabled(int id) {
        boolean result = false;
        if(shortcutIdMap != null) {
            Object value = shortcutIdMap.get(id);
            if(value instanceof ToolbarButtonField) {
                result = ((ToolbarButtonField)value).isEnabled();
            }
        }
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.StandardScreen#setShortcutEnabled(int, boolean)
     */
    public void setShortcutEnabled(int id, boolean enabled) {
        if(shortcutIdMap != null) {
            Object value = shortcutIdMap.get(id);
            if(value instanceof ToolbarButtonField) {
                ((ToolbarButtonField)value).setEnabled(enabled);
            }
        }
    }
    
    private class ShortcutCommand extends AlwaysExecutableCommand {
        private final ShortcutItem item;
        
        public ShortcutCommand(ShortcutItem item) {
            this.item = item;
        }
        
        public void execute(ReadOnlyCommandMetadata metadata, Object context) {
            screenProvider.shortcutAction(item);
        }
    }
}
