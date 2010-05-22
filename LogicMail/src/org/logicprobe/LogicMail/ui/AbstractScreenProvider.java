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

import net.rim.device.api.i18n.ResourceBundle;

import org.logicprobe.LogicMail.LogicMailResource;

import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.Menu;

/**
 * Common parent class for screen providers, implementing default
 * behavior for the methods of the <tt>ScreenProvider</tt> interface.
 */
public abstract class AbstractScreenProvider implements ScreenProvider {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	protected NavigationController navigationController;
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#getStyle()
	 */
	public long getStyle() {
	    return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#getTitle()
	 */
	public String getTitle() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#hasShortcuts()
	 */
	public boolean hasShortcuts() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#getShortcuts()
	 */
	public ShortcutItem[] getShortcuts() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#setNavigationController(org.logicprobe.LogicMail.ui.NavigationController)
	 */
	public void setNavigationController(NavigationController navigationController) {
		this.navigationController = navigationController;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#initFields(net.rim.device.api.ui.Screen)
	 */
	public void initFields(Screen screen) {
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#keyChar(char, int, int)
	 */
	public boolean keyChar(char c, int status, int time) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#makeMenu(net.rim.device.api.ui.component.Menu, int)
	 */
	public void makeMenu(Menu menu, int instance) {
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#navigationClick(int, int)
	 */
	public boolean navigationClick(int status, int time) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#onDisplay()
	 */
	public void onDisplay() {
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#onUndisplay()
	 */
	public void onUndisplay() {
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#onVisibilityChange(boolean)
	 */
	public void onVisibilityChange(boolean visible) {
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#onClose()
	 */
	public boolean onClose() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#onSavePrompt()
	 */
	public boolean onSavePrompt() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.ScreenProvider#shortcutAction(org.logicprobe.LogicMail.ui.ScreenProvider.ShortcutItem)
	 */
	public void shortcutAction(ShortcutItem item) {
	}
}
