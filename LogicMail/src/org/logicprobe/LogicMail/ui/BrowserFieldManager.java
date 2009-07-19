/*-
 * Copyright (c) 2009, Derek Konigsberg
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

import org.logicprobe.LogicMail.util.StringParser;

import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.container.VerticalFieldManager;

/**
 * Field manager to contain the browser field, and to provide
 * control over what menu items it offers.  This class should
 * only be instantiated from within {@link BrowserFieldRenderer}.
 */
public class BrowserFieldManager extends VerticalFieldManager {
	/**
	 * Indicates that the user has highlighted an E-Mail address and
	 * selected the menu option to compose a message to that address.
	 * The address itself should be available via {@link #getSelectedToken()}.
	 */
	public final static int ACTION_SEND_EMAIL = 0x00000001;
	
	private String selectedToken;

	/**
	 * Instantiates a new browser field manager.
	 */
	public BrowserFieldManager() {
		super();
	}

	/**
	 * Creates the menu for this field.
	 * This implementation needs to resort to a lot of trickery so that
	 * invalid or inapplicable menu items from the browser field are not
	 * provided to the user.
	 * 
     * @see net.rim.device.api.ui.Manager#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    protected void makeMenu(Menu menu, int instance) {
		int size = menu.getSize();
		for(int i=0; i<size; i++) {
			MenuItem item = menu.getItem(i);
			if(item instanceof EmailMenuItem) { continue; }
			
			String itemText = item.toString();
			if(itemText == null) {
				menu.deleteItem(i);
				i = 0;
				size = menu.getSize();
			}
			
			String address = getEmailItem(itemText);
			if(address != null) {
				MenuItem emailMenuItem =
					new EmailMenuItem(
						item.toString(),
						item.getOrdinal(),
						item.getPriority(),
						address);
				menu.add(emailMenuItem);
				
				menu.deleteItem(i);
				i = 0;
				size = menu.getSize();
				
				menu.setDefault(emailMenuItem);
			}
		}
		super.makeMenu(menu, instance);
	}

	private static String getEmailItem(String itemText) {
		String[] elements = StringParser.parseTokenString(itemText, " ");
		for(int i = 0; i<elements.length; i++) {
			if(elements[i].indexOf('@') != -1) {
				return elements[i].trim();
			}
		}
		return null;
	}
	
	private class EmailMenuItem extends MenuItem {
		private String address;
		
		public EmailMenuItem(String text, int ordinal, int priority, String address){
			super(text, ordinal, priority);
			this.address = address;
		}

		public void run() {
			BrowserFieldManager.this.selectedToken = address;
			BrowserFieldManager.this.fieldChangeNotify(
					FieldChangeListener.PROGRAMMATIC | BrowserFieldManager.ACTION_SEND_EMAIL);
		}
	}
	
	/**
	 * Gets the selected token from the browser field.
	 * This method should only be called from within
	 * {@link FieldChangeListener#fieldChanged(net.rim.device.api.ui.Field, int)}
	 * to ensure that it contains the desired contents.
	 * 
	 * @return the selected token
	 */
	public String getSelectedToken() {
		return selectedToken;
	}
}
