/*
 * MailboxScreen.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.system.Bitmap;
import java.util.Date;
import java.util.Calendar;
import java.util.Vector;
import java.io.IOException;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.system.Application;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.conf.*;

/**
 * Display the active mailbox listing
 */
public class MailboxScreen extends BaseScreen implements ListFieldCallback {
    private Vector messages;
    private Bitmap bmapOpened;
    private Bitmap bmapUnopened;
    private ListField msgList;
    
    private MailSettings mailSettings;
    private MailClient client;
    private MailClient.FolderItem folderItem;
    
    // Things to calculate in advance
    private static int lineHeight;
    private static int dateWidth;
    private static int senderWidth;
    private static int maxWidth;
    
    public MailboxScreen(MailSettings mailSettings, MailClient client, MailClient.FolderItem folderItem) {
        super(folderItem.name);
        this.mailSettings = mailSettings;
        this.client = client;
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
        
        if(client != null) getMessageList();
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
    
    private void getMessageList() {
        Thread thread = new Thread() {
            public void run() {
                Vector msgEnvList = null;
                try {
                    if(!client.isConnected()) client.open();
                    client.setActiveMailbox(folderItem);
                    int firstIndex = folderItem.msgCount - mailSettings.getGlobalConfig().getRetMsgCount();
                    if(firstIndex < 0) firstIndex = 0;
                    msgEnvList = client.getMessageEnvelopes(firstIndex, folderItem.msgCount);
                } catch (IOException exp) {
                    System.out.println(exp);
                    try { client.close(); } catch (Exception exp2) { }
                } catch (MailException exp) {
                    System.out.println("Protocol error: " + exp);
                    msgEnvList = null;
                } catch (Exception exp) {
                    System.out.println("Unknown error: " + exp);
                    try { client.close(); } catch (Exception exp2) { }
                }
                if(msgEnvList == null) return;

                synchronized(Application.getEventLock()) {
                    if(mailSettings.getGlobalConfig().getDispOrder())
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
        };
        thread.start();
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
        
        MailClient.MessageEnvelope entry = (MailClient.MessageEnvelope)messages.elementAt(index);
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
        
        MailClient.MessageEnvelope envelope = (MailClient.MessageEnvelope)messages.elementAt(index);
        
        UiApplication.getUiApplication().pushScreen(new MessageScreen(client, folderItem, envelope));
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

