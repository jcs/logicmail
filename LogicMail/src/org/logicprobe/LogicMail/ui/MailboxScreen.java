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
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.UiApplication;
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
import org.logicprobe.LogicMail.model.MessageNodeEvent;
import org.logicprobe.LogicMail.model.MessageNodeListener;

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
			UiApplication.getUiApplication().invokeLater(new EventObjectRunnable(e) {
				public void run() {
					mailboxNode_MailboxStatusChanged((MailboxNodeEvent)getEvent());
				}
			});
		}
    };
    
    private MessageNodeListener messageNodeListener = new MessageNodeListener() {
		public void messageStatusChanged(MessageNodeEvent e) {
			UiApplication.getUiApplication().invokeLater(new EventObjectRunnable(e) {
				public void run() {
					messageNode_MessageStatusChanged((MessageNodeEvent)getEvent());
				}
			});
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
        int size = knownMessages.size();
        for(int i=0; i<size; i++) {
        	((MessageNode)knownMessages.elementAt(i)).addMessageNodeListener(messageNodeListener);
        }
        
        // TODO: Support message list changes between display pushing
    }
    
	protected void onUndisplay() {
        this.mailboxNode.removeMailboxNodeListener(mailboxNodeListener);
        int size = knownMessages.size();
        for(int i=0; i<size; i++) {
        	((MessageNode)knownMessages.elementAt(i)).removeMessageNodeListener(messageNodeListener);
        }
        
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
    private MenuItem deleteItem = new MenuItem("Delete", 130, 10) {
        public void run() {
        	deleteSelectedMessage();
        }
    };
    private MenuItem undeleteItem = new MenuItem("Undelete", 135, 10) {
        public void run() {
        	undeleteSelectedMessage();
        }
    };

    protected void makeMenu(Menu menu, int instance) {
    	if(messageListField.getSelectedIndex() != -1) {
    		menu.add(selectItem);
    		menu.add(propertiesItem);
    	}
//        if(this.client.getAcctConfig().getOutgoingConfig() != null) {
//            menu.add(compositionItem);
//        }
        int index = messageListField.getSelectedIndex();
        if(index >= 0 && index < messageListField.getSize() && !displayedMessages.isEmpty()) {
        	MessageNode messageNode = (MessageNode)displayedMessages.elementAt(index);
            if(messageNode.getFolderMessage().isDeleted()) {
                if(mailboxNode.getParentAccount().hasUndelete()) {
                    menu.add(undeleteItem);
                }
            }
            else {
                menu.add(deleteItem);
            }
        }
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
    		for(int i=0; i<messageNodes.length; i++) {
    			knownMessages.addElement(messageNodes[i]);
    			
    			if(isMessageDisplayable(messageNodes[i])) {
    				// Insert the message
	    			insertDisplayableMessage(messageNodes[i]);
    			}
    			
    			if(isDisplayed()) {
    				messageNodes[i].addMessageNodeListener(messageNodeListener);
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
     * Handles message status change events.
     * 
     * @param e Event data.
     */
	private void messageNode_MessageStatusChanged(MessageNodeEvent e) {
		if(e.getType() == MessageNodeEvent.TYPE_FLAGS) {
			MessageNode messageNode = (MessageNode)e.getSource();
			boolean currentlyDisplayed = displayedMessages.contains(messageNode);
			boolean displayable = isMessageDisplayable(messageNode);
			
			if(currentlyDisplayed && !displayable) {
				// Remove from display
				int index = displayedMessages.indexOf(messageNode);
				displayedMessages.removeElementAt(index);
				messageListField.delete(index);
			}
			else if(!currentlyDisplayed && displayable) {
				// Add to display
				insertDisplayableMessage(messageNode);
			}
			else if(currentlyDisplayed) {
				// Just a visual flag update, so find and invalidate the item
				int index = displayedMessages.indexOf(messageNode);
				messageListField.invalidate(index);
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

    private MessageNode getSelectedMessage() {
        int index = messageListField.getSelectedIndex();
        if(index < 0 || index > displayedMessages.size()) {
            return null;
        }
        else {
        	return (MessageNode)displayedMessages.elementAt(index);
        }
    }
    
    private void openSelectedMessage()
    {
    	MessageNode messageNode = getSelectedMessage();
    	if(messageNode != null) {
    		UiApplication.getUiApplication().pushScreen(new MessageScreen(messageNode));
    	}
    }

    private void openSelectedMessageProperties()
    {
    	MessageNode messageNode = getSelectedMessage();
    	if(messageNode != null) {
	        MessagePropertiesDialog dialog =
	        	new MessagePropertiesDialog(messageNode);
	        dialog.doModal();
    	}
    }

    private void deleteSelectedMessage() {
    	MessageNode messageNode = getSelectedMessage();
    	if(messageNode != null) {
	        if(Dialog.ask(Dialog.D_YES_NO, "Are you sure you want to delete this message?") == Dialog.YES) {
	        	messageNode.deleteMessage();
	        }
    	}
    }
    
    private void undeleteSelectedMessage() {
    	MessageNode messageNode = getSelectedMessage();
    	if(messageNode != null) {
    		messageNode.undeleteMessage();
    	}
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
            case Keypad.KEY_BACKSPACE:
            	deleteSelectedMessage();
            	retval = true;
            	break;
        }
        return retval;
    }
}
