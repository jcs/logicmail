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
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.util.Arrays;

/**
 * Implements a field for entering e-mail addresses, supporting the
 * ability to input addresses from the address book.
 */
public class EmailAddressBookEditField extends EmailAddressEditField {
    
    /** Creates a new instance of EmailAddressBookEditField */
    public EmailAddressBookEditField(String label, String initialValue) {
        super(label, initialValue);
    }

    private MenuItem addressBookMenuItem = new MenuItem("Address book", 200000, 10) {
        public void run() {
            addressBookChooser();
        }
    };

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
        if(choice < 0 || choice > contacts.size()) return;
        
        String[] email = ((ContactItem)contacts.elementAt(choice)).email;
        if(email == null) {
            this.setText("");
        }
        else if(email.length > 1) {
            Dialog addrDlg = new Dialog("Which address?", email, null, 0, Bitmap.getPredefinedBitmap(Bitmap.QUESTION));
            choice = addrDlg.doModal();
            this.setText(email[choice]);
        }
        else {
            this.setText(email[0]);
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
                if(contactItem.name != null && contactItem.email != null)
                    addressList.addElement(contactItem);
            }
        } catch (ControlledAccessException e) {
            // should do something
        } catch (PIMException e) {
            // should do something
        }
        return addressList;
    }
    
    
    protected void makeContextMenu(ContextMenu contextMenu) {
        contextMenu.addItem(addressBookMenuItem);
    }
}
