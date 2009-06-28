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

import net.rim.device.api.ui.Graphics;

/**
 * Provides common code for rendering throbber graphics inside various fields.
 */
public class ThrobberRenderer {
	private static double PI_4 = Math.PI / 4.0;
	private int size;
	private int sizeHalf;
	private int sizeDiv;
	private int radius;
	private int position = 0;

	private int[] cx;
	private int[] cy;
	
	/**
	 * Instantiates a new throbber renderer.
	 * 
	 * @param size the size of the throbber
	 */
	public ThrobberRenderer(int size) {
		this.size = size;
		this.sizeHalf = size / 2;
		this.sizeDiv = size / 8;
		this.radius = size / 10;

		cx = new int[8];
		cy = new int[8];
		
		// left
		cx[0] = (sizeHalf - sizeDiv) / 2;
		cy[0] = sizeHalf;
		// upper-left
		cx[1] = (int)(((sizeHalf - sizeDiv * Math.cos(PI_4)) + (sizeHalf - sizeHalf * Math.cos(PI_4))) / 2.0);
		cy[1] = (int)(((sizeHalf - sizeDiv * Math.sin(PI_4)) + (sizeHalf - sizeHalf * Math.sin(PI_4))) / 2.0);
		// top
		cx[2] = sizeHalf;
		cy[2] = (sizeHalf - sizeDiv) / 2;
		// upper-right
		cx[3] = (int)(((sizeHalf + sizeDiv * Math.cos(PI_4)) + (sizeHalf + sizeHalf * Math.cos(PI_4))) / 2.0);
		cy[3] = (int)(((sizeHalf - sizeDiv * Math.sin(PI_4)) + (sizeHalf - sizeHalf * Math.sin(PI_4))) / 2.0);
		// right
		cx[4] = size - ((sizeHalf - sizeDiv) / 2);
		cy[4] = sizeHalf;
		// lower-right
		cx[5] = (int)(((sizeHalf + sizeDiv * Math.cos(PI_4)) + (sizeHalf + sizeHalf * Math.cos(PI_4))) / 2.0);
		cy[5] = (int)(((sizeHalf + sizeDiv * Math.sin(PI_4)) + (sizeHalf + sizeHalf * Math.sin(PI_4))) / 2.0);
		// bottom
		cx[6] = sizeHalf;
		cy[6] = size - ((sizeHalf - sizeDiv) / 2);
		// lower-left
		cx[7] = (int)(((sizeHalf - sizeDiv * Math.cos(PI_4)) + (sizeHalf - sizeHalf * Math.cos(PI_4))) / 2.0);
		cy[7] = (int)(((sizeHalf + sizeDiv * Math.sin(PI_4)) + (sizeHalf + sizeHalf * Math.sin(PI_4))) / 2.0);
	}
	
	/**
	 * Gets the size this renderer was configured with.
	 * 
	 * @return the size
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Paints the throbber at the current position index.
	 * 
	 * @param graphics the graphics reference
	 */
	public void paint(Graphics graphics) {
		int index = 0;
		for(int i=position; i>=0; i--) {
			int color = 0x1F * Math.min(6, index);
			graphics.setColor((color * 0x10000) + (color * 0x100) + color);
			drawCircle(graphics, i);
			index++;
		}
		for(int i=7; i>position; i--) {
			int color = 0x1F * Math.min(6, index);
			graphics.setColor((color * 0x10000) + (color * 0x100) + color);
			drawCircle(graphics, i);
			index++;
		}
	}

	/**
	 * Draw a circle element of the throbber.
	 * 
	 * @param graphics The graphics instance
	 * @param index The index of the circle to draw, from 0 to 7.
	 */
	private void drawCircle(Graphics graphics, int index) {
		if(index < 0 || index > 7) {
			return;
		}
		graphics.fillArc(cx[index] - radius, cy[index] - radius, radius * 2, radius * 2, 0, 360);
	}

	/**
	 * Resets the graphic to the starting position.
	 */
	public void resetPosition() {
		position = 0;
	}
	
	/**
	 * Advances the graphic to the next position.
	 */
	public void nextPosition() {
		if(position < 7) {
			position++;
		}
		else {
			position = 0;
		}
	}
}
