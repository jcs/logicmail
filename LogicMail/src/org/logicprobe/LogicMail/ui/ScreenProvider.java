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

import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.Menu;

/**
 * Common interface for all classes that provide UI screens.
 */
public interface ScreenProvider {
    /**
     * Gets the style bits to be passed to the constructor of the screen.
     * 
     * @return style bits, or 0 if none are set.
     */
	long getStyle();
    
	/**
	 * Gets the title of the screen.
	 * 
	 * @return the title, or null for a screen without a title
	 */
	String getTitle();
	
	/**
	 * Returns <tt>true</tt> if this screen contains shortcut bar items.
	 * 
	 * @return whether shortcut items exist.
	 */
	boolean hasShortcuts();
	
	/**
	 * Array of shortcut bar items this screen provides.
	 * This array should contain up to 5 elements, using null values
	 * for any blank separator items.
	 * 
	 * @return shortcut bar items
	 */
	ShortcutItem[] getShortcuts();
	
	/**
	 * Sets the navigation controller.
	 * 
	 * @param navigationController the navigation controller
	 */
	void setNavigationController(NavigationController navigationController);
	
	/**
	 * Called when the screen's fields should be initialized and added.
	 * This method can also be used as a roundabout way for screen providers
	 * to obtain a reference to the actual <tt>Screen</tt> instance they
	 * are hooked into.
	 * 
	 * @param screen the screen implementations should add fields to
	 */
	void initFields(Screen screen);

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onDisplay()
     */
	void onDisplay();

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onUndisplay()
     */
	void onUndisplay();

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#onVisibilityChange(boolean)
     */
	void onVisibilityChange(boolean visible);
	
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onClose()
     */
	boolean onClose();
	
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#onSavePrompt()
     */
	boolean onSavePrompt();
	
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
	void makeMenu(Menu menu, int instance);
	
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#navigationClick(int, int)
     */
	boolean navigationClick(int status, int time);
	
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
	boolean keyChar(char c, int status, int time);

	/**
	 * Called when a shortcut bar item is selected.
	 * 
	 * @param item Selected item.
	 */
	void shortcutAction(ShortcutItem item);
	
	/**
	 * Container class representing a shortcut bar item.
	 */
	static class ShortcutItem {
		private int id;
		private String name;
		private String enabledIcon;
		private String disabledIcon;
		
		public ShortcutItem(int id, String name, String enabledIcon, String disabledIcon) {
			this.id = id;
			this.name = name;
			this.enabledIcon = enabledIcon;
			this.disabledIcon = disabledIcon;
		}
		
		public int getId() { return id; }
		public String getName() { return name; }
		public String getEnabledIcon() { return enabledIcon; }
		public String getDisabledIcon() { return disabledIcon; }
	}
}
