/*-
 * Copyright (c) 2006, Derek Konigsberg
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

import java.io.IOException;
import java.util.Calendar;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.Menu;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.message.MessageEnvelope;

/**
 * Display the active mailbox listing
 */
public class MailboxScreen extends BaseScreen implements ListFieldCallback, MailClientListener {
    private FolderMessage[] messages;
    private ListField msgList;
    
    // Message icons
    private Bitmap bmapOpened;
    private Bitmap bmapUnopened;
    private Bitmap bmapReplied;
    private Bitmap bmapFlagged;
    private Bitmap bmapDraft;
    private Bitmap bmapDeleted;
    private Bitmap bmapUnknown;

    private MailSettings mailSettings;
    private FolderTreeItem folderItem;
    private MailClient client;
    private RefreshMessageListHandler refreshMessageListHandler;
    
    // Things to calculate in advance
    private static int lineHeight;
    private static int dateWidth;
    private static int senderWidth;
    private static int maxWidth;
    
    public MailboxScreen(MailClient client, FolderTreeItem folderItem) {
        super(folderItem.getName());
        mailSettings = MailSettings.getInstance();
        this.folderItem = folderItem;
        this.client = client;

        // Load message icons
        bmapOpened = Bitmap.getBitmapResource("mail_opened.png");
        bmapUnopened = Bitmap.getBitmapResource("mail_unopened.png");
        bmapReplied = Bitmap.getBitmapResource("mail_replied.png");
        bmapFlagged = Bitmap.getBitmapResource("mail_flagged.png");
        bmapDraft = Bitmap.getBitmapResource("mail_draft.png");
        bmapDeleted = Bitmap.getBitmapResource("mail_deleted.png");
        bmapUnknown = Bitmap.getBitmapResource("mail_unknown.png");

        messages = new FolderMessage[0];
        
        // add field elements
        msgList = new ListField();
        lineHeight = msgList.getRowHeight();
        msgList.setRowHeight(lineHeight * 2);
        msgList.setCallback(this);
        add(msgList);
        
        // Determine field sizes
        maxWidth = Graphics.getScreenWidth();
        dateWidth = Font.getDefault().getAdvance("00/0000");
        senderWidth = maxWidth - dateWidth - 20;

        if(client != null) refreshMessageList();
    }

    private void refreshMessageList() {
        // Initialize the handler on demand
        if(refreshMessageListHandler == null) {
            refreshMessageListHandler = new RefreshMessageListHandler(folderItem);
            refreshMessageListHandler.setMailClientListener(this);
        }

        // Start the background process
        refreshMessageListHandler.start();
    }

    protected boolean onSavePrompt() {
        return true;
    }

    public boolean onClose() {
        if(checkClose()) {
            close();
            return true;
        }
        else
            return false;
    }

    private boolean checkClose() {
        // Immediately close without prompting if we are
        // using a protocol that supports folders.
        if(client.hasFolders())
            return true;
        
        // Otherwise we are on the main screen for the account, so prompt
        // before closing the connection
        if(client.isConnected()) {
            if(Dialog.ask(Dialog.D_YES_NO, "Disconnect from server?") == Dialog.YES) {
                try { client.close(); } catch (Exception exp) { }
                return true;
            }
            else
                return false;
        }
        else {
            return true;
        }
    }

    private MenuItem selectItem = new MenuItem("Select", 100, 10) {
        public void run() {
            openSelectedMessage();
        }
    };

    protected void makeMenu(Menu menu, int instance) {
        menu.add(selectItem);
        super.makeMenu(menu, instance);
    }
    
    /**
     * Draw a row of the message list.
     * Currently using a double row so that more meaningful
     * information can be displayed for each message entry.
     * Actual rendering is crude at the moment, and needs to
     * be reworked to use relative positioning for everything.
     */
    public void drawListRow(ListField listField,
                            Graphics graphics,
                            int index,
                            int y,
                            int width)
    {
        // sanity check
        if(index >= messages.length) return;
        FolderMessage entry = (FolderMessage)messages[index];
        MessageEnvelope env = entry.getEnvelope();
        graphics.drawBitmap(1, y, 20, lineHeight*2, getIconForMessage(entry), 0, 0);
            
        Font origFont = graphics.getFont();
        graphics.setFont(origFont.derive(Font.BOLD));
        if(env.from != null && env.from.length > 0)
            graphics.drawText((String)env.from[0], 20, y,
                              (int)(getStyle() | DrawStyle.ELLIPSIS),
                               senderWidth);
                               
        graphics.setFont(origFont.derive(Font.PLAIN));
        if(env.subject != null)
            graphics.drawText((String)env.subject, 20, y+lineHeight,
                              (int)(getStyle() | DrawStyle.ELLIPSIS),
                               maxWidth-20);
        graphics.setFont(origFont);
        
        // Current time
        // Perhaps this should only be set on initialization
        // of this screen, and/or new message downloads
        if(env.date != null) {
            Calendar nowCal = Calendar.getInstance();
    
            Calendar dispCal = Calendar.getInstance();
            dispCal.setTime(env.date);

            SimpleDateFormat dateFormat;

            // Determine the date format to display,
            // based on the distance from the current time
            if(nowCal.get(Calendar.YEAR) == dispCal.get(Calendar.YEAR))
                if((nowCal.get(Calendar.MONTH) == dispCal.get(Calendar.MONTH)) &&
                (nowCal.get(Calendar.DAY_OF_MONTH) == dispCal.get(Calendar.DAY_OF_MONTH)))
                    dateFormat = new SimpleDateFormat("h:mma");
                else
                    dateFormat = new SimpleDateFormat("MM/dd");
            else
                dateFormat = new SimpleDateFormat("MM/yyyy");
        
            StringBuffer buffer = new StringBuffer();
            dateFormat.format(dispCal, buffer, null);
            graphics.setFont(origFont.derive(Font.BOLD));
            graphics.drawText(buffer.toString(), senderWidth+20, y,
                              (int)(getStyle() | DrawStyle.ELLIPSIS),
                              dateWidth);
            graphics.setFont(origFont);
        }
    }
    
    private Bitmap getIconForMessage(FolderMessage message) {
        if(message.isDeleted())
            return bmapDeleted;
        else if(message.isAnswered())
            return bmapReplied;
        else if(message.isFlagged())
            return bmapFlagged;
        else if(message.isDraft())
            return bmapDraft;
        else if(message.isRecent())
            return bmapUnopened;
        else if(message.isSeen())
            return bmapOpened;
        else
            return bmapUnknown;
    }
    
    public int getPreferredWidth(ListField listField) {
        return Graphics.getScreenWidth();
    }
    
    public Object get(ListField listField, int index) {
        return (Object)messages[index];
    }
    
    public int indexOfList(ListField listField,
                           String prefix,
                           int start)
    {
        return 0;
    }

    private void openSelectedMessage()
    {
        int index = msgList.getSelectedIndex();
        if(index < 0 || index > messages.length) return;
        
        UiApplication.getUiApplication().pushScreen(new MessageScreen(client, folderItem, messages[index]));
    }

    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
                openSelectedMessage();
                retval = true;
                break;
        }
        return retval;
    }

    public void mailActionComplete(MailClientHandler source, boolean result) {
        if(source.equals(refreshMessageListHandler)) {
            if(refreshMessageListHandler.getFolderMessages() != null) {
                FolderMessage[] folderMessages = refreshMessageListHandler.getFolderMessages();
                synchronized(Application.getEventLock()) {
                    if(mailSettings.getGlobalConfig().getDispOrder()) {
                        messages = folderMessages;
                    }
                    else {
                        messages = new FolderMessage[folderMessages.length];
                        int j = 0;
                        for(int i=folderMessages.length-1;i>=0;i--)
                            messages[j++] = folderMessages[i];
                    }
                    int size = msgList.getSize();
                    for(int i=0;i<size;i++)
                        msgList.delete(0);
                    for(int i=0;i<messages.length;i++)
                        msgList.insert(i);

                    msgList.setDirty(true);
                }
            }
        }
    }

    /**
     * Implements the message list refresh action
     */
    private class RefreshMessageListHandler extends MailClientHandler {
        private FolderTreeItem folderItem;
        private FolderMessage[] folderMessages;
        
        public RefreshMessageListHandler(FolderTreeItem folderItem) {
            super(MailboxScreen.this.client, "Retrieving message list");
            this.folderItem = folderItem;
        }

        public void runSession() throws IOException, MailException {
            FolderMessage[] folderMessages;
            try {
                client.setActiveFolder(folderItem);
                int firstIndex = folderItem.getMsgCount() - mailSettings.getGlobalConfig().getRetMsgCount();
                if(firstIndex < 0) firstIndex = 1;
                folderMessages = client.getFolderMessages(firstIndex, folderItem.getMsgCount());
            } catch (MailException exp) {
                folderMessages = null;
                throw exp;
            }
            this.folderMessages = folderMessages;
        }
        
        public FolderMessage[] getFolderMessages() {
            return folderMessages;
        }
    }
    
}

