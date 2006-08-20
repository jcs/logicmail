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

import java.util.Calendar;
import java.util.Vector;
import java.io.IOException;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.Menu;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.Message;
import org.logicprobe.LogicMail.controller.MailboxController;
import org.logicprobe.LogicMail.util.Observable;

/**
 * Display the active mailbox listing
 */
public class MailboxScreen extends BaseScreen implements ListFieldCallback {
    private Vector messages;
    private Bitmap bmapOpened;
    private Bitmap bmapUnopened;
    private ListField msgList;
    
    private MailSettings _mailSettings;
    private MailClient _client;
    private MailboxController _mailboxController;
    private MailClient.FolderItem folderItem;
    
    // Things to calculate in advance
    private static int lineHeight;
    private static int dateWidth;
    private static int senderWidth;
    private static int maxWidth;
    
    public MailboxScreen(MailClient client, MailClient.FolderItem folderItem) {
        super(folderItem.name);
        _mailSettings = MailSettings.getInstance();
        _client = client;
        _mailboxController = MailboxController.getInstance();
        _mailboxController.addObserver(this);
        this.folderItem = folderItem;

        bmapOpened = Bitmap.getBitmapResource("mail_opened.png");
        bmapUnopened = Bitmap.getBitmapResource("mail_unopened.png");

        messages = new Vector();

        // add field elements
        msgList = new ListField();
        lineHeight = msgList.getRowHeight();
        msgList.setRowHeight(lineHeight * 2);
        msgList.setCallback(this);
        for(int i=0;i<messages.size();i++)
            msgList.insert(i);
        add(msgList);
        
        // Determine field sizes
        maxWidth = Graphics.getScreenWidth();
        dateWidth = Font.getDefault().getAdvance("00/0000");
        senderWidth = maxWidth - dateWidth - 20;
        
        if(client != null) _mailboxController.refreshMessageList(folderItem);
    }

    protected boolean onSavePrompt() {
        return true;
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

    public void update(Observable subject, Object arg) {
        super.update(subject, arg);
        if(subject.equals(_mailboxController)) {
            if(((String)arg).equals("messages")) {
                Vector msgEnvList = _mailboxController.getMsgEnvList();
                synchronized(Application.getEventLock()) {
                    if(_mailSettings.getGlobalConfig().getDispOrder())
                        messages = msgEnvList;
                    else {
                        messages.removeAllElements();
                        for(int i=msgEnvList.size()-1;i>=0;i--)
                            messages.addElement(msgEnvList.elementAt(i));
                    }

                    for(int i=0;i<msgList.getSize();i++)
                        msgList.delete(0);
                    for(int i=0;i<messages.size();i++)
                        msgList.insert(i);

                    msgList.setDirty(true);

                }
            }
        }
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
        if(index >= messages.size()) return;
        
        Message.Envelope entry = (Message.Envelope)messages.elementAt(index);
        if(entry.isOpened)
            graphics.drawBitmap(1, y, 20, lineHeight*2, bmapOpened, 0, 0);
        else
            graphics.drawBitmap(1, y, 20, lineHeight*2, bmapUnopened, 0, 0);
            
        Font origFont = graphics.getFont();
        graphics.setFont(origFont.derive(Font.BOLD));
        if(entry.from != null && entry.from.length > 0)
            graphics.drawText((String)entry.from[0], 20, y,
                              (int)(getStyle() | DrawStyle.ELLIPSIS),
                               senderWidth);
                               
        graphics.setFont(origFont.derive(Font.PLAIN));
        if(entry.subject != null)
            graphics.drawText((String)entry.subject, 20, y+lineHeight,
                              (int)(getStyle() | DrawStyle.ELLIPSIS),
                               maxWidth-20);
        graphics.setFont(origFont);
        
        // Current time
        // Perhaps this should only be set on initialization
        // of this screen, and/or new message downloads
        if(entry.date != null) {
            Calendar nowCal = Calendar.getInstance();
    
            Calendar dispCal = Calendar.getInstance();
            dispCal.setTime(entry.date);

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
    
    public int getPreferredWidth(ListField listField) {
        return Graphics.getScreenWidth();
    }
    
    public Object get(ListField listField, int index) {
        return (Object)messages.elementAt(index);
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
        if(index < 0 || index > messages.size()) return;
        
        Message.Envelope envelope = (Message.Envelope)messages.elementAt(index);
        
        UiApplication.getUiApplication().pushScreen(new MessageScreen(_client, folderItem, envelope));
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
    
}

