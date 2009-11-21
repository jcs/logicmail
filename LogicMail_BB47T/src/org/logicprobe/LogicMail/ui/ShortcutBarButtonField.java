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
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.TouchEvent;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.decor.Background;
import net.rim.device.api.ui.decor.BackgroundFactory;

public class ShortcutBarButtonField extends Field {
	private Bitmap enabledBitmap;
	private Bitmap disabledBitmap;
	private int bitmapWidth;
	private int bitmapHeight;
	private Background highlightBackground;
	private boolean hasHover;
	
	public static final int CHANGE_HOVER_GAINED = 100;
	public static final int CHANGE_HOVER_LOST = 101;
	public static final int CHANGE_CLICK = 110;
	
	public ShortcutBarButtonField() {
		this(null, null);
	}
	
	public ShortcutBarButtonField(Bitmap enabledBitmap, Bitmap disabledBitmap) {
		highlightBackground =
			BackgroundFactory.createLinearGradientBackground(
					0xD5D5D5, 0xD5D5D5,
					0x303030, 0x303030);
		if(enabledBitmap != null) {
			this.enabledBitmap = enabledBitmap;
			this.disabledBitmap = disabledBitmap;
			this.bitmapWidth = enabledBitmap.getWidth();
			this.bitmapHeight = enabledBitmap.getHeight();
			if(disabledBitmap == null || disabledBitmap.getWidth() != bitmapWidth || disabledBitmap.getHeight() != bitmapHeight) {
				this.disabledBitmap = this.enabledBitmap;
			}
		}
	}
	
	protected void layout(int width, int height) {
		setExtent(width, height);
	}
	
	protected boolean touchEvent(TouchEvent message) {
		boolean result;

		int x = message.getX(1);
		int y = message.getY(1);
		if(x < 0 || x >= this.getWidth() || y < 0 || y >= this.getWidth()) {
			if(hasHover) {
				fieldChangeNotify(CHANGE_HOVER_LOST);
				hasHover = false;
				invalidate();
			}
			result = false;
		}
		else {
			if(!isEditable()) {
				result = false;
			}
			else {
				switch(message.getEvent()) {
				case TouchEvent.DOWN:
					hasHover = true;
					invalidate();
					fieldChangeNotify(CHANGE_HOVER_GAINED);
					result = true;
					break;
				case TouchEvent.UP:
					hasHover = false;
					invalidate();
					fieldChangeNotify(CHANGE_HOVER_LOST);
					result = true;
					break;
				case TouchEvent.CLICK:
					fieldChangeNotify(CHANGE_CLICK);
					result = true;
					break;
				default:
					result = super.touchEvent(message);
				}
			}
		}
		return result;
	}
	
	public void setEditable(boolean editable) {
		super.setEditable(editable);
		invalidate();
	}
	
	protected void paint(Graphics graphics) {
		int width = this.getWidth();
		int height = this.getHeight();
		graphics.setBackgroundColor(Color.BLACK);
		graphics.clear();
		highlightBackground.draw(graphics, new XYRect(0, 0, width, 19));
		
		graphics.setColor(0x525252);
		graphics.drawLine(0, 0, 0, height);
		graphics.setColor(0x212121);
		graphics.drawLine(0, 0, width, 0);
		Bitmap bitmap;
		if(this.isEditable()) {
			bitmap = enabledBitmap;
		}
		else {
			bitmap = disabledBitmap;
		}
		if(bitmap != null) {
			graphics.drawBitmap(
					(width/2) - (bitmapWidth/2),
					height - 10 - bitmapHeight,
					bitmapWidth, bitmapHeight,
					bitmap, 0, 0);
		}
		if(hasHover) {
			graphics.pushContext(0, 0, width, height, 0, 0);
			graphics.setGlobalAlpha(64);
			graphics.setColor(Color.BLUE);
			graphics.fillRect(0, 0, width, height);
			graphics.popContext();
		}
	}
}
