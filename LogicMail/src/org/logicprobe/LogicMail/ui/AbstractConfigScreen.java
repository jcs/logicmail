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

import org.logicprobe.LogicMail.LogicMailResource;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.LabelField;

/**
 * This class is the base for all configuration screens
 * in LogicMail. Its purpose is to provide uniform menu
 * and event handler interfaces for those screens.
 */
public abstract class AbstractConfigScreen extends MainScreen {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);

	private final static int MENU_CONTEXT = 0x10000;
	private final static int MENU_MAIN = 0x40000000;
	
	/** Creates a new instance of BaseCfgScreen */
    public AbstractConfigScreen(String title) {
        LabelField titleField =
                new LabelField(title, LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleField);
    }

    public boolean onMenu(int instance) {
		if (instance == MENU_MAIN) {
			// Main menu button pressed, display menu
			return super.onMenu(instance);
		}
		else if (instance == MENU_CONTEXT) {
			// Trackball click, call override method
			if(!onClick()) {
				return super.onMenu(instance);
			}
			else {
				return false;
			}
		}
		else {
			// Trackwheel click, display menu
			return super.onMenu(instance);
		}
	}
    
    /**
     * Invoked when the user clicks the trackball on
     * devices that have a separate menu button.
     * 
     * @return True if the click was handled, false to fall
     *         through and display the context menu.
     */
    protected boolean onClick() {
    	return false;
    }
}
