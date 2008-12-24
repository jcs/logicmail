/*-
 * Copyright (c) 2008, Derek Konigsberg
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

import java.util.Timer;
import java.util.TimerTask;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;


/**
 * Provides a spinning status indicator
 */
public class ThrobberField extends Field {
	private static double PI_4 = Math.PI / 4.0;
	private int size;
	private int sizeHalf;
	private int sizeDiv;
	private int radius;
	private int position = 0;
	private Timer timer;
	private TimerTask timerTask;

	private int[] cx;
	private int[] cy;
	
	/**
	 * Instantiates a new throbber field.
	 * 
	 * @param size The size
	 */
	public ThrobberField(int size) {
		super();
		initField(size);
	}

	/**
	 * Instantiates a new throbber field.
	 * 
	 * @param size The field size
	 * @param style Combination of field style bits to specify display attributes.
	 */
	public ThrobberField(int size, long style) {
		super(style);
		initField(size);
	}
	
	/**
	 * Initializes the field.
	 * 
	 * @param size The field size
	 */
	private void initField(int size) {
		this.size = size;
		this.sizeHalf = size / 2;
		this.sizeDiv = size / 8;
		this.radius = size / 10;
		this.timer = new Timer();

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
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#onDisplay()
	 */
	protected void onDisplay() {
		super.onDisplay();
		timerTask = new AnimationTimerTask();
		timer.scheduleAtFixedRate(timerTask, 200, 100);
	}

	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#onUndisplay()
	 */
	protected void onUndisplay() {
		timerTask.cancel();
		super.onUndisplay();
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#layout(int, int)
	 */
	protected void layout(int width, int height) {
		setExtent(size, size);
	}

	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#paint(net.rim.device.api.ui.Graphics)
	 */
	protected void paint(Graphics graphics) {
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
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#getPreferredWidth()
	 */
	public int getPreferredWidth() {
		return size;
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#getPreferredHeight()
	 */
	public int getPreferredHeight() {
		return size;
	}

	/**
	 * Internal timer task class to support animation.
	 */
	private class AnimationTimerTask extends TimerTask {
		
		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		public void run() {
			if(position < 7) {
				position++;
			}
			else {
				position = 0;
			}
			ThrobberField.this.invalidate();
		}
	}
}
