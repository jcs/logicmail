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

import java.util.Calendar;
import java.util.Vector;
import net.rim.device.api.system.Application;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.system.UnsupportedOperationException;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.IncomingMailClient;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MailboxNodeEvent;
import org.logicprobe.LogicMail.model.MailboxNodeListener;
import org.logicprobe.LogicMail.model.MessageNode;

/**
 * Display the active mailbox listing.
 * If the supplied folder matches the configured sent folder
 * for the provided account, then the display fields will be
 * adjusted accordingly.
 */
public class MailboxScreen extends BaseScreen {
	private MailboxNode mailboxNode;
    private ListField messageListField;
    private Vector knownMessages;
    private Vector displayedMessages;
    private boolean firstDisplay = true;
    private MailSettings mailSettings;
	
    // Things to calculate in advance
    private int lineHeight;
    private int dateWidth;
    private int senderWidth;
    private int maxWidth;
    
    // TODO: Register listeners with all known messages and handle status changes
    
    /**
     * Initializes a new MailboxScreen to view the provided mailbox.
     * 
     * @param mailboxNode Mailbox node to view.
     */
    public MailboxScreen(MailboxNode mailboxNode) {
    	super(mailboxNode.getName());
    	this.mailboxNode = mailboxNode;
    	this.knownMessages = new Vector();
    	this.displayedMessages = new Vector();
    	mailSettings = MailSettings.getInstance();
    	
    	initFields();
    	
        // Determine field sizes
        maxWidth = Graphics.getScreenWidth();
        dateWidth = Font.getDefault().getAdvance("00/0000");
        senderWidth = maxWidth - dateWidth - 20;
    }

    public MailboxScreen(IncomingMailClient client, FolderTreeItem item) {
    	// Kept so the code still compiles before the other UI classes are updated.
    	throw new UnsupportedOperationException("Dummy constructor");
    }

    private MailboxNodeListener mailboxNodeListener = new MailboxNodeListener() {
		public void mailboxStatusChanged(MailboxNodeEvent e) {
			mailboxNode_MailboxStatusChanged(e);
		}
    };

    protected void onDisplay() {
    	super.onDisplay();
        this.mailboxNode.addMailboxNodeListener(mailboxNodeListener);
        if(firstDisplay) {
            MessageNode[] initialMessages = this.mailboxNode.getMessages();
            for(int i=0; i<initialMessages.length; i++) {
            	knownMessages.addElement(initialMessages[i]);
            	insertDisplayableMessage(initialMessages[i]);
            }
            
        	this.mailboxNode.refreshMessages();
        	firstDisplay = false;
        }
        // TODO: Support message list changes between display pushing
    }
    
	protected void onUndisplay() {
        this.mailboxNode.removeMailboxNodeListener(mailboxNodeListener);
    	super.onUndisplay();
    }
    
    
	private void initFields() {
        messageListField = new ListField();
        lineHeight = messageListField.getRowHeight();
        messageListField.setRowHeight(lineHeight * 2);
        
        messageListField.setCallback(new ListFieldCallback() {
            public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
            	messageListField_drawListRow(listField, graphics, index, y, width);
            }
            public int getPreferredWidth(ListField listField) {
                return messageListField_getPreferredWidth(listField);
            }
            public Object get(ListField listField, int index) {
                return messageListField_get(listField, index);
            }
            public int indexOfList(ListField listField, String prefix, int start) {
                return messageListField_indexOfList(listField, prefix, start);
            }
        });
        
        add(messageListField);
	}    

    protected boolean onSavePrompt() {
        return true;
    }

    private MenuItem selectItem = new MenuItem("Select", 100, 10) {
        public void run() {
            openSelectedMessage();
        }
    };
    private MenuItem propertiesItem = new MenuItem("Properties", 105, 10) {
        public void run() {
            openSelectedMessageProperties();
        }
    };
//    private MenuItem compositionItem = new MenuItem("Compose E-Mail", 120, 10) {
//        public void run() {
//            //UiApplication.getUiApplication().pushScreen(new CompositionScreen(client.getAcctConfig()));
//        }
//    };
//    private MenuItem deleteItem = new MenuItem("Delete", 130, 10) {
//        public void run() {
//            if(Dialog.ask(Dialog.D_YES_NO, "Are you sure you want to delete this message?") == Dialog.YES) {
//                int index = msgList.getSelectedIndex();
//                if(index >= 0 && index < msgList.getSize() && messages[index] != null) {
//                    DeleteMessageHandler deleteMessageHandler = new DeleteMessageHandler(messages[index], true);
//                    deleteMessageHandler.setListener(new MailClientHandlerListener() {
//                        public void mailActionComplete(MailClientHandler source, boolean result) {
//                            source.setListener(null);
//                            msgList.setDirty(true);
//                            msgList.invalidate();
//                        }
//                    });
//                    deleteMessageHandler.start();
//                }
//            }
//        }
//    };
//    private MenuItem undeleteItem = new MenuItem("Undelete", 135, 10) {
//        public void run() {
//            int index = msgList.getSelectedIndex();
//            if(index >= 0 && index < msgList.getSize() && messages[index] != null) {
//                DeleteMessageHandler deleteMessageHandler = new DeleteMessageHandler(messages[index], false);
//                deleteMessageHandler.setListener(new MailClientHandlerListener() {
//                    public void mailActionComplete(MailClientHandler source, boolean result) {
//                        source.setListener(null);
//                        msgList.setDirty(true);
//                        msgList.invalidate();
//                    }
//                });
//                deleteMessageHandler.start();
//            }
//        }
//    };

    protected void makeMenu(Menu menu, int instance) {
    	if(messageListField.getSelectedIndex() != -1) {
    		menu.add(selectItem);
    		menu.add(propertiesItem);
    	}
//        if(this.client.getAcctConfig().getOutgoingConfig() != null) {
//            menu.add(compositionItem);
//        }
//        int index = msgList.getSelectedIndex();
//        if(index >= 0 && index < msgList.getSize() && messages[index] != null) {
//            if(messages[index].isDeleted()) {
//                if(client.hasUndelete()) {
//                    menu.add(undeleteItem);
//                }
//            }
//            else {
//                menu.add(deleteItem);
//            }
//        }
        super.makeMenu(menu, instance);
    }

    /**
     * Handles mailbox status change events.
     * 
     * @param e Event data.
     */
    private void mailboxNode_MailboxStatusChanged(MailboxNodeEvent e) {
    	if(e.getType() == MailboxNodeEvent.TYPE_NEW_MESSAGES) {
    		MessageNode[] messageNodes = e.getAffectedMessages();
    		synchronized(Application.getEventLock()) {
	    		for(int i=0; i<messageNodes.length; i++) {
	    			knownMessages.addElement(messageNodes[i]);
	    			
	    			if(isMessageDisplayable(messageNodes[i])) {
	    				// Insert the message
		    			insertDisplayableMessage(messageNodes[i]);
	    			}
	    		}
    		}
    	}
	}
    
    /**
     * Determines whether a message should be displayed,
     * per the configuration.
     * 
     * @param messageNode Message to check.
     * @return True if it should be displayed, false otherwise.
     */
    private boolean isMessageDisplayable(MessageNode messageNode) {
    	if(messageNode.getFolderMessage().isDeleted() &&
    	   mailSettings.getGlobalConfig().getHideDeletedMsg()) {
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
    /**
     * Insert a message into the list and associated data structures.
     * This will insert into the correct order, per the configuration.
     * 
     * @param messageNode Message to insert.
     */
    private void insertDisplayableMessage(MessageNode messageNode) {
    	int selectedIndex = messageListField.getSelectedIndex();
    	
		if(displayedMessages.size() > 0) {
			int msgId = messageNode.getId();
			int index = displayedMessages.size();
			
			if(mailSettings.getGlobalConfig().getDispOrder()) {
				// Ascending order
				MessageNode lastMessage = (MessageNode)displayedMessages.lastElement();
				while(index > 0 && lastMessage.getId() > msgId) {
					index--;
					if(index > 0) { lastMessage = (MessageNode)displayedMessages.elementAt(index - 1); }
				}
			}
			else {
				// Descending order
				MessageNode lastMessage = (MessageNode)displayedMessages.lastElement();
				while(index > 0 && lastMessage.getId() < msgId) {
					index--;
					if(index > 0) { lastMessage = (MessageNode)displayedMessages.elementAt(index - 1); }
				}
			}
			displayedMessages.insertElementAt(messageNode, index);
			messageListField.insert(index);
			if(selectedIndex != -1) { messageListField.setSelectedIndex(selectedIndex); }
			messageListField.invalidate(index);
		}
		else {
			displayedMessages.addElement(messageNode);
			messageListField.insert(0);
			if(selectedIndex != -1) { messageListField.setSelectedIndex(selectedIndex); }
			messageListField.invalidate(0);
		}
    }
    
    /**
     * Draw a row of the message list.
     * Currently using a double row so that more meaningful
     * information can be displayed for each message entry.
     * Actual rendering is crude at the moment, and needs to
     * be reworked to use relative positioning for everything.
     */
    private void messageListField_drawListRow(
    		ListField listField,
            Graphics graphics,
            int index,
            int y,
            int width)
    {
    	MessageNode messageNode = (MessageNode)displayedMessages.elementAt(index);
        // sanity check
        if(messageNode == null) {
            return;
        }
        
        FolderMessage entry = (FolderMessage)messageNode.getFolderMessage();
        MessageEnvelope env = entry.getEnvelope();
        graphics.drawBitmap(1, y, 20, lineHeight*2, NodeIcons.getIcon(messageNode), 0, 0);
            
        Font origFont = graphics.getFont();
        graphics.setFont(origFont.derive(Font.BOLD));

        if(mailboxNode.getType() == MailboxNode.TYPE_SENT) {
            if(env.to != null && env.to.length > 0) {
                graphics.drawText((String)env.to[0], 20, y,
                                  (int)(getStyle() | DrawStyle.ELLIPSIS),
                                   senderWidth);
            }
        }
        else {
            if(env.from != null && env.from.length > 0) {
                graphics.drawText((String)env.from[0], 20, y,
                                  (int)(getStyle() | DrawStyle.ELLIPSIS),
                                   senderWidth);
            }
        }
        graphics.setFont(origFont.derive(Font.PLAIN));
        if(env.subject != null) {
            graphics.drawText((String)env.subject, 20, y+lineHeight,
                              (int)(getStyle() | DrawStyle.ELLIPSIS),
                               maxWidth-20);
        }
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
    
    public int messageListField_getPreferredWidth(ListField listField) {
        return Graphics.getScreenWidth();
    }
    
    public Object messageListField_get(ListField listField, int index) {
        return this.displayedMessages.elementAt(index);
    }
    
    public int messageListField_indexOfList(
    		ListField listField,
            String prefix,
            int start)
    {
        return -1;
    }

    private void openSelectedMessage()
    {
//	        int index = messageListField.getSelectedIndex();
//	        if(index < 0 || index > displayedMessages.size()) {
//	            return;
//	        }
//	        
//        UiApplication.getUiApplication().pushScreen(new MessageScreen(client, folderItem, messages[index]));
    }

    private void openSelectedMessageProperties()
    {
        int index = messageListField.getSelectedIndex();
        if(index < 0 || index > displayedMessages.size()) {
            return;
        }
        
        MessagePropertiesDialog dialog =
        	new MessagePropertiesDialog((MessageNode)displayedMessages.elementAt(index));
        dialog.doModal();
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
