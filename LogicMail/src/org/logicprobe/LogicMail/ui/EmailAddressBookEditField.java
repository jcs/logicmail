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

import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.pim.Contact;
import javax.microedition.pim.ContactList;
import javax.microedition.pim.PIM;
import javax.microedition.pim.PIMException;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.ui.ContextMenu;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.util.Arrays;

/**
 * Implements a field for entering e-mail addresses, supporting the
 * ability to input addresses from the address book.
 */
public class EmailAddressBookEditField extends EmailAddressEditField {
    private String name;
    private String address;
    private Bitmap bmapContactItem;
    private Bitmap bmapContactNonItem;
    private int addressType;
    private boolean inAddressBook;
    
    public static int ADDRESS_TO = 1;
    public static int ADDRESS_CC = 2;
    public static int ADDRESS_BCC = 3;
    
    /**
     * Creates a new instance of EmailAddressBookEditField.
     * @param addressType The type of address (ADDRESS_TO, ADDRESS_CC, or ADDRESS_BCC)
     * @param initialValue The initial value of the field
     */
    public EmailAddressBookEditField(int addressType, String initialValue) {
        super("", initialValue);
        if(addressType == ADDRESS_TO)
            this.setLabel("To: ");
        else if(addressType == ADDRESS_CC)
            this.setLabel("Cc: ");
        else if(addressType == ADDRESS_BCC)
            this.setLabel("Bcc: ");
        else {
            this.setLabel("To: ");
            addressType = ADDRESS_TO;
        }
        this.addressType = addressType;
            
        name = null;
        address = "";
        bmapContactItem = Bitmap.getBitmapResource("contact_item.png");
        bmapContactNonItem = Bitmap.getBitmapResource("contact_nonitem.png");
    }

    public String getText() {
        if(name != null) {
            if(name.length() == 0) {
                return address;
            }
            else {
                return "\"" + name + "\" <" + address + ">";
            }
        }
        else
            return super.getText();
    }
    
    /**
     * Set the address contained within the field.
     * Supports handling the "John Doe <jdoe@generic.org>" format.
     * @param address Address to set the field to
     */
    public void setAddress(String sourceAddress) {
        sourceAddress = sourceAddress.trim();
        int p = sourceAddress.indexOf('<');
        int q = sourceAddress.indexOf('>');

        if(p == -1 && q == -1) {
            this.address = sourceAddress;
        }
        else if(p != -1 && q != -1 && p < q && sourceAddress.length() > 2) {
            this.address = sourceAddress.substring(p + 1, q);
        }
        else if(p != -1 && q == -1 && sourceAddress.length() > 1) {
            this.address = sourceAddress.substring(p+1);
        }
        else {
            this.address = "";
        }
        if(this.address.equals("")) {
            return;
        }
        
        if(p != -1 && p > 0) {
            this.name = sourceAddress.substring(0, p).trim();
        }
        else {
            this.name = null;
        }

        if(this.name != null) {
            this.setText(this.name);
            this.setEditable(false);
        }
        else {
            this.setText(this.address);
            this.setEditable(true);
        }
        inAddressBook = false;
    }
    
    public int getAddressType() {
        return addressType;
    }
    
    private MenuItem addressPropertiesMenuItem = new MenuItem("Properties", 200000, 10) {
        public void run() {
            addressProperties();
        }
    };

    private MenuItem addressBookMenuItem = new MenuItem("Address book", 200010, 10) {
        public void run() {
            addressBookChooser();
        }
    };

    protected void makeContextMenu(ContextMenu contextMenu) {
        if(this.name != null || super.getText().length() > 0) {
            contextMenu.addItem(addressPropertiesMenuItem);
        }
        contextMenu.addItem(addressBookMenuItem);
    }
    
    protected void paint(Graphics graphics) {
        if(name != null) {
            int width = this.getExtent().width;
            int height = this.getExtent().height;
            int x = this.getExtent().x;
            int y = this.getExtent().y;
            int labelWidth =
                graphics.drawText(this.getLabel(), x, y,
                                  (int)this.getStyle(),
                                  width);
            int nameWidth =
                graphics.drawText(name, x+labelWidth, y,
                                  (int)(this.getStyle() | DrawStyle.ELLIPSIS),
                                  width-labelWidth-bmapContactItem.getWidth()-5);
            if(inAddressBook) {
                graphics.drawBitmap(x+labelWidth+nameWidth+5, y, width, height, bmapContactItem, 0, 0);
            }
            else {
                graphics.drawBitmap(x+labelWidth+nameWidth+5, y, width, height, bmapContactNonItem, 0, 0);
            }
        }
        else {
            super.paint(graphics);
        }
    }
    
    /**
     * Handle key events
     */
    protected boolean keyChar(char key, int status, int time) {
        switch(key) {
            case Keypad.KEY_BACKSPACE:
            case Keypad.KEY_DELETE:
                if(name != null) {
                    name = null;
                    address = "";
                    setText(address);
                    setEditable(true);
                    setFocus();
                    return true;
                }
                break;
        }
        return super.keyChar(key, status, time);
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
            } catch(Exception e) {
                fldAddress = new EmailAddressEditField("Address: ", "");
            }
            this.add(fldName);
            this.add(fldAddress);
            this.add(new LabelField("", Field.NON_FOCUSABLE));
            
            ButtonField btnOk = new ButtonField("OK", Field.FOCUSABLE | Field.FIELD_HCENTER);
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

    /**
     * Handle the address properties dialog
     */
    private void addressProperties() {
        String localName;
        String localAddress;
        
        if(this.name != null) {
            localName = this.name;
            if(this.address != null) localAddress = this.address;
            else localAddress = "";
        }
        else {
            localName = "";
            localAddress = super.getText();
        }
        
        AddressPropertiesDialog dialog = new AddressPropertiesDialog(localName, localAddress);
        if(dialog.doModal() == Dialog.OK) {
            if(!localName.equals(dialog.getName()) || !localAddress.equals(dialog.getAddress())) {
                localName = dialog.getName();
                localAddress = dialog.getAddress();

                if(localName.length() > 0) {
                    this.name = localName;
                }
                else {
                    this.name = null;
                }
                this.address = localAddress;

                if(this.name != null) {
                    this.setText(this.name);
                    this.setEditable(false);
                }
                else {
                    this.setText(this.address);
                    this.setEditable(true);
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
        String names[] = new String[contacts.size()];
        for(int i=0;i<names.length;i++)
            names[i] = ((ContactItem)contacts.elementAt(i)).name;
        
        Dialog abDlg = new Dialog("Address book", names, null, 0, null, Dialog.LIST);
        int choice;
        choice = abDlg.doModal();
        if(choice < 0 || choice > contacts.size()) {
            return;
        }
        
        ContactItem contactItem = ((ContactItem)contacts.elementAt(choice));
        String[] email = contactItem.email;
        if(email == null) {
            this.setText("");
            inAddressBook = false;
        }
        else if(email.length > 1) {
            Dialog addrDlg = new Dialog("Which address?", email, null, 0, Bitmap.getPredefinedBitmap(Bitmap.QUESTION));
            choice = addrDlg.doModal();
            address = email[choice];
            name = contactItem.name;
            this.setText(name);
            this.setEditable(false);
            inAddressBook = true;
        }
        else {
            address = email[0];
            name = contactItem.name;
            this.setText(name);
            this.setEditable(false);
            inAddressBook = true;
        }
    }

    private static class ContactItem {
        String name;
        String[] email;
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
            ContactList contactList = (ContactList)pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_ONLY);
            Enumeration enumList = contactList.items();
            while(enumList.hasMoreElements()) {
                ContactItem contactItem = new ContactItem();
                Contact c = (Contact)enumList.nextElement();
                int[] fieldIds = c.getFields();
                for(int i=0;i<fieldIds.length;i++) {
                    if(fieldIds[i] == Contact.NAME) {
                        String[] values = c.getStringArray(Contact.NAME, 0);
                        contactItem.name = values[1] + " " + values[0];
                    }
                    if(fieldIds[i] == Contact.EMAIL) {
                        for(int j=0;j<c.countValues(Contact.EMAIL);j++) {
                            if(contactItem.email == null) {
                                contactItem.email = new String[1];
                                contactItem.email[0] = c.getString(Contact.EMAIL, j);
                            }
                            else {
                                Arrays.add(contactItem.email, c.getString(Contact.EMAIL, j));
                            }
                        }
                    }
                }
                if(contactItem.name != null && contactItem.email != null) {
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
    
}
