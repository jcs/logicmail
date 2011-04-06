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

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.VirtualKeyboard;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.util.IntHashtable;

import org.logicprobe.LogicMail.ui.ScreenProvider.ShortcutItem;

/**
 * This class extends the standard screen to provide the necessary functionality
 * specific to touchscreen devices, such as the shortcut bar.
 */
public class StandardTouchScreen extends StandardScreen {
	private ShortcutBarManager shortcutBarManager;
	private IntHashtable shortcutIdMap;
	private Field hoverField;
	
	public StandardTouchScreen(NavigationController navigationController, ScreenProvider screenProvider) {
		super(navigationController, screenProvider);
		
		if(screenProvider.hasShortcuts()) {
			ShortcutItem[] shortcuts = screenProvider.getShortcuts();
			if(shortcuts != null) {
				initializeShortcutBar(shortcuts);
			}
		}
	}

	private void initializeShortcutBar(ShortcutItem[] shortcuts) {
		shortcutIdMap = new IntHashtable(5);
		shortcutBarManager = new ShortcutBarManager();
		int len = Math.min(shortcuts.length, 5);
		for(int i=0; i<len; i++) {
			ShortcutBarButtonField button;
			if(shortcuts[i] == null) {
				button = new ShortcutBarButtonField();
				button.setEditable(false);
			}
			else {
				button = new ShortcutBarButtonField(
						Bitmap.getBitmapResource(shortcuts[i].getEnabledIcon()),
						Bitmap.getBitmapResource(shortcuts[i].getDisabledIcon()));
				button.setCookie(shortcuts[i]);
				button.setEditable(true);
				button.setChangeListener(shortcutButtonChangeListener);
				shortcutIdMap.put(shortcuts[i].getId(), button);
			}
			shortcutBarManager.add(button);
		}
		setStatus(shortcutBarManager);
	}

	private FieldChangeListener shortcutButtonChangeListener = new FieldChangeListener() {
		public void fieldChanged(Field field, int context) {
			if(field.getCookie() instanceof ShortcutItem) {
				ShortcutItem item = (ShortcutItem)field.getCookie();
				if(context == ShortcutBarButtonField.CHANGE_CLICK) {
					screenProvider.shortcutAction(item);
				}
				else if(context == ShortcutBarButtonField.CHANGE_HOVER_GAINED) {
					hoverField = field;
					invalidate();
				}
				else if(context == ShortcutBarButtonField.CHANGE_HOVER_LOST) {
					hoverField = null;
					invalidate();
				}
			}
		}
	};
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.StandardScreen#isShortcutEnabled(int)
	 */
	public boolean isShortcutEnabled(int id) {
		boolean result = false;
		if(shortcutIdMap != null) {
			Object value = shortcutIdMap.get(id);
			if(value instanceof ShortcutBarButtonField) {
				result = ((ShortcutBarButtonField)value).isEditable();
			}
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.StandardScreen#setShortcutEnabled(int, boolean)
	 */
	public void setShortcutEnabled(int id, boolean enabled) {
		if(shortcutIdMap != null) {
			Object value = shortcutIdMap.get(id);
			if(value instanceof ShortcutBarButtonField) {
				((ShortcutBarButtonField)value).setEditable(enabled);
			}
		}
	}
	
	public void showVirtualKeyboard() {
	    VirtualKeyboard keyboard = getVirtualKeyboard();
	    if(keyboard != null) {
	        keyboard.setVisibility(VirtualKeyboard.SHOW);
	    }
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Screen#paint(net.rim.device.api.ui.Graphics)
	 */
	protected void paint(Graphics graphics) {
		super.paint(graphics);
		if(hoverField != null) {
			ShortcutItem item = (ShortcutItem)hoverField.getCookie();
			String tooltip = item.getName();
			Font font = Font.getDefault();
			int tooltipWidth = font.getAdvance(tooltip) + 4;
			int tooltipHeight = font.getHeight() + 4;
			
			XYRect fieldRect = hoverField.getExtent();
			
			int x = fieldRect.x - 1;
			if(x + tooltipWidth > Display.getWidth()) {
				x = Display.getWidth() - tooltipWidth;
			}
			int y = Display.getHeight() - fieldRect.height - tooltipHeight - 12;
			
			graphics.pushRegion(x, y, tooltipWidth, tooltipHeight, 0, 0);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, tooltipWidth, tooltipHeight);
			graphics.setColor(Color.BLACK);
			graphics.drawRect(0, 0, tooltipWidth, tooltipHeight);
			graphics.drawText(tooltip, 2, 2);
			graphics.popContext();
		}
	}
}
