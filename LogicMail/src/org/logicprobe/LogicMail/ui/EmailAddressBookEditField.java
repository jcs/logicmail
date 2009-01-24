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

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.ui.ContextMenu;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.util.Arrays;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.pim.Contact;
import javax.microedition.pim.ContactList;
import javax.microedition.pim.PIM;
import javax.microedition.pim.PIMException;


/**
 * Implements a field for entering e-mail addresses, supporting the
 * ability to input addresses from the address book.
 */
public class EmailAddressBookEditField extends EditField {
    public final static int ADDRESS_TO = 1;
    public final static int ADDRESS_CC = 2;
    public final static int ADDRESS_BCC = 3;
    
    private final static int MODE_ADDRESS = 1;
    private final static int MODE_NAME = 2;
    
    private final static char[] invalidChars = { '<', '>', '\"', '{', '}', '|', '\\', '^', '[', ']', '`' };
    
    private String name;
    private String address;
    private int addressMode;
    private int addressType;
    private boolean inAddressBook;

    private MenuItem addressPropertiesMenuItem = new MenuItem("Properties",
            200000, 10) {
            public void run() {
                addressProperties();
            }
        };

    private MenuItem addressBookMenuItem = new MenuItem("Address book", 200010,
            10) {
            public void run() {
                addressBookChooser();
            }
        };

    /**
     * Creates a new instance of EmailAddressBookEditField.
     * @param addressType The type of address (ADDRESS_TO, ADDRESS_CC, or ADDRESS_BCC)
     * @param initialValue The initial value of the field
     */
    public EmailAddressBookEditField(int addressType, String initialValue) {
        super("", "");

        setAddressType(addressType);
        setText(initialValue);
    }

    /**
     * Sets the address type.
     * Will default to ADDRESS_TO if an invalid type is passed.
     * 
     * @param addressType The type of address (ADDRESS_TO, ADDRESS_CC, or ADDRESS_BCC)
     */
    public void setAddressType(int addressType) {
        switch(addressType) {
        case ADDRESS_TO:
        	this.setLabel("To: ");
        	break;
        case ADDRESS_CC:
        	this.setLabel("Cc: ");
        	break;
        case ADDRESS_BCC:
        	this.setLabel("Bcc: ");
        	break;
    	default:
    		this.setLabel("To: ");
    		addressType = ADDRESS_TO;
        }

        this.addressType = addressType;
    }

    /**
     * Gets the address type.
     * 
     * @return the address type
     */
    public int getAddressType() {
    	return this.addressType;
    }
    
    /**
     * Sets the address mode.
     * Also makes any other field state changes
     * that go along with the new mode.
     * 
     * @param addressMode the new address mode
     */
    private void setAddressMode(int addressMode) {
    	this.addressMode = addressMode;
    	switch(addressMode) {
    	case MODE_ADDRESS:
        	super.setText(this.address);
        	super.setEditable(true);
        	break;
    	case MODE_NAME:
            super.setText(this.name);
            super.setEditable(false);
            break;
    	}
    	this.invalidate();
    }
    
    /**
     * Set the address contained within the field.
     * 
     * @return Address in the standard format
     * 
     * @see net.rim.device.api.ui.component.BasicEditField#getText()
     */
    public String getText() {
    	String result;
    	if (name != null && name.length() > 0) {
    		StringBuffer buf = new StringBuffer();
    		buf.append('\"');
    		buf.append(name);
    		buf.append("\" <");
    		buf.append(address);
    		buf.append('>');
    		result = buf.toString();
        } else {
        	result = address;
        }
    	return result;
    }

    /**
     * Set the address contained within the field.
     * Supports handling the "John Doe <jdoe@generic.org>" format.
     * 
     * @param text Address to set the field to
     */
    public void setText(String text) {
        text = text.trim();

        int p = text.indexOf('<');
        int q = text.indexOf('>');

        // Attempt to set the address from the parameter
        if ((p == -1) && (q == -1)) {
            this.address = text;
        }
        else if ((p != -1) && (q != -1) && (p < q) && (text.length() > 2)) {
            this.address = text.substring(p + 1, q);
        }
        else if ((p != -1) && (q == -1) && (text.length() > 1)) {
            this.address = text.substring(p + 1);
        }
        else {
            this.address = "";
        }
        
        // Sanity check for empty addresses
        if (this.address.length() == 0) {
        	setAddressMode(MODE_ADDRESS);
            return;
        }

        // Attempt to set the full name from the parameter
        if ((p != -1) && (p > 0)) {
            this.name = text.substring(0, p).trim();
        } else {
            this.name = null;
        }

        // Determine whether we are in MODE_ADDRESS or MODE_NAME
        // and configure the field accordingly
        if (this.name != null) {
            setAddressMode(MODE_NAME);
        }
        else {
        	setAddressMode(MODE_ADDRESS);
        }

        // TODO: Add a quick address-book cross-check if possible
        inAddressBook = false;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.component.BasicEditField#makeContextMenu(net.rim.device.api.ui.ContextMenu)
     */
    protected void makeContextMenu(ContextMenu contextMenu) {
    	if (addressMode == MODE_NAME) {
            contextMenu.addItem(addressPropertiesMenuItem);
        }

        contextMenu.addItem(addressBookMenuItem);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.component.BasicEditField#keyChar(char, int, int)
     */
    protected boolean keyChar(char key, int status, int time) {
    	boolean result = false;

    	if(addressMode == MODE_NAME && status == 0) {
	    	switch(key) {
	        case Keypad.KEY_BACKSPACE:
	        case Keypad.KEY_DELETE:
	        	// Empty the field and sets us
	        	// back into address mode.
                name = null;
                address = "";
                setAddressMode(MODE_ADDRESS);
                setFocus();
                result = true;
	            break;
	        }
    	}
    	else {
    		switch(key) {
    		case Keypad.KEY_SPACE:
    			if(address.indexOf('@') == -1) {
    				result = super.keyChar('@', status, time);
    			}
    			else {
    				result = super.keyChar('.', status, time);
    			}
    			break;
			default:
				// Ignore any invalid characters
				for(int i=0; i<invalidChars.length; i++) {
    				if(key == invalidChars[i]) { result = true; break; }
    			}
    		}
    	}
    	if(!result) {
    		result = super.keyChar(key, status, time);
    	}
        return result;
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.component.TextField#fieldChangeNotify(int)
     */
    protected void fieldChangeNotify(int context) {
    	if(addressMode == MODE_ADDRESS) {
    		this.address = super.getText();
    	}
    	super.fieldChangeNotify(context);
    }
    
    /**
     * Handle the address properties dialog
     */
    private void addressProperties() {
        String localName = this.name;
        String localAddress = this.address;

        AddressPropertiesDialog dialog =
        	new AddressPropertiesDialog(localName, localAddress);

        if (dialog.doModal() == Dialog.OK) {
            if (!localName.equals(dialog.getName()) ||
                    !localAddress.equals(dialog.getAddress())) {
                localName = dialog.getName();
                localAddress = dialog.getAddress();

                if (localName.length() > 0) {
                    this.name = localName;
                } else {
                    this.name = null;
                }

                this.address = localAddress;

                if (this.name != null) {
                	setAddressMode(MODE_NAME);
                } else {
                	setAddressMode(MODE_ADDRESS);
                }

                this.inAddressBook = false;
            }
        }
    }

    /**
     * Handle choosing an address from the address book
     */
    private void addressBookChooser() {
        Vector contacts = getAddressList();
        String[] names = new String[contacts.size()];

        for (int i = 0; i < names.length; i++)
            names[i] = ((ContactItem) contacts.elementAt(i)).name;

        Dialog abDlg = new Dialog("Address book", names, null, 0, null,
                Dialog.LIST);
        int choice;
        choice = abDlg.doModal();

        if ((choice < 0) || (choice > contacts.size())) {
            return;
        }

        ContactItem contactItem = ((ContactItem) contacts.elementAt(choice));
        String[] email = contactItem.email;

        if (email == null) {
        	setAddressMode(MODE_ADDRESS);
            inAddressBook = false;
        } else if (email.length > 1) {
            Dialog addrDlg = new Dialog("Which address?", email, null, 0,
                    Bitmap.getPredefinedBitmap(Bitmap.QUESTION));
            choice = addrDlg.doModal();
            address = email[choice];
            name = contactItem.name;

            setAddressMode(MODE_NAME);
            inAddressBook = true;
        } else {
            address = email[0];
            name = contactItem.name;

            setAddressMode(MODE_NAME);
            inAddressBook = true;
        }
    }

    /**
     * Search the address book and return a list of entries
     * with E-Mail addresses.
     *
     * @return Vector of ContactItems
     */
    private Vector getAddressList() {
        // Note: This implementation is sub-optimal, but works for now.
        Vector addressList = new Vector();

        try {
            PIM pim = PIM.getInstance();
            ContactList contactList = (ContactList) pim.openPIMList(PIM.CONTACT_LIST,
                    PIM.READ_ONLY);
            Enumeration enumList = contactList.items();

            while (enumList.hasMoreElements()) {
                ContactItem contactItem = new ContactItem();
                Contact c = (Contact) enumList.nextElement();
                int[] fieldIds = c.getFields();

                for (int i = 0; i < fieldIds.length; i++) {
                    if (fieldIds[i] == Contact.NAME) {
                        String[] values = c.getStringArray(Contact.NAME, 0);
                        contactItem.name = values[1] + " " + values[0];
                    }

                    if (fieldIds[i] == Contact.EMAIL) {
                        for (int j = 0; j < c.countValues(Contact.EMAIL);
                                j++) {
                            if (contactItem.email == null) {
                                contactItem.email = new String[1];
                                contactItem.email[0] = c.getString(Contact.EMAIL,
                                        j);
                            } else {
                                Arrays.add(contactItem.email,
                                    c.getString(Contact.EMAIL, j));
                            }
                        }
                    }
                }

                if ((contactItem.name != null) && (contactItem.email != null)) {
                    addressList.addElement(contactItem);
                }
            }
        } catch (ControlledAccessException e) {
            // should do something
        } catch (PIMException e) {
            // should do something
        }

        return addressList;
    }

    /**
     * Dialog to allow viewing and editing of address properties
     */
    private static class AddressPropertiesDialog extends Dialog {
        private BasicEditField fldName;
        private EmailAddressEditField fldAddress;

        public AddressPropertiesDialog(String name, String address) {
            super("Address Properties", null, null, 0,
                Bitmap.getPredefinedBitmap(Bitmap.QUESTION),
                Field.FOCUSABLE | Field.FIELD_HCENTER);
            fldName = new BasicEditField("Name: ", name);

            try {
                fldAddress = new EmailAddressEditField("Address: ", address);
            } catch (Exception e) {
                fldAddress = new EmailAddressEditField("Address: ", "");
            }

            this.add(fldName);
            this.add(fldAddress);
            this.add(new LabelField("", Field.NON_FOCUSABLE));

            ButtonField btnOk = new ButtonField("OK",
                    Field.FOCUSABLE | Field.FIELD_HCENTER);
            btnOk.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        AddressPropertiesDialog.this.select(Dialog.OK);
                        AddressPropertiesDialog.this.close();
                    }
                });
            this.add(btnOk);
        }

        public String getName() {
            return fldName.getText();
        }

        public String getAddress() {
            return fldAddress.getText();
        }
    }

    private static class ContactItem {
        String name;
        String[] email;
    }
}
