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

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;

/**
 * Field to represent labeled horizontal separators.
 * Shows a centered text string against a gradient background.
 */
public class LabeledSeparatorField extends Field {
    private String text;
    private boolean topBorder;
    private boolean bottomBorder;

    public static long TOP_BORDER = 0x0000000000020L;
    public static long BOTTOM_BORDER = 0x0000000000040L;

    /**
     * Instantiates a new mailbox separator field.
     * 
     * @param text The text to display.
     * @param style Combination of field style bits to specify display attributes.
     */
    public LabeledSeparatorField(String text, long style) {
        super(style);
        this.text = text;
        topBorder = ((style & TOP_BORDER) == TOP_BORDER);
        bottomBorder = ((style & BOTTOM_BORDER) == BOTTOM_BORDER);
    }

    /**
     * Instantiates a new mailbox separator field.
     * 
     * @param style Combination of field style bits to specify display attributes
     */
    public LabeledSeparatorField(long style) {
        this("", style);
    }

    /**
     * Sets the text to display;
     * 
     * @param text the new text to display
     */
    public void setText(String text) {
        this.text = text;
        this.invalidate();
    }

    /**
     * Gets the text being displayed
     * 
     * @return the text being displayed
     */
    public String getText() {
        return this.text;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#getPreferredHeight()
     */
    public int getPreferredHeight() {
        return Font.getDefault().getHeight();
    };

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#layout(int, int)
     */
    protected void layout(int width, int height) {
        setExtent(width, getPreferredHeight());
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#onUnfocus()
     */
    protected void onUnfocus() {
        super.invalidate();
        super.onUnfocus();
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#paint(net.rim.device.api.ui.Graphics)
     */
    protected void paint(Graphics graphics) {
        boolean isFocus = this.isFocus();
        int width = this.getWidth();
        int height = this.getHeight();
        int originalColor = graphics.getColor();
        int textWidth = Font.getDefault().getAdvance(text);

        int textX = width / 2 - textWidth / 2;
        if(textX <= 0) {
            textX = 0;
            textWidth = width;
        }

        // Draw the gradient background
        if(!isFocus) {
            for(int i=0; i<height; i++) {
                int color = 0xFF - i;
                graphics.setColor((color * 0x10000) + (color * 0x100) + color);
                graphics.drawLine(0, i, width, i);
            }
        }

        // Draw the separator lines
        graphics.setColor(Color.DARKGRAY);
        if(topBorder) {
            graphics.drawLine(0, 0, width, 0);
        }
        if(bottomBorder) {
            graphics.drawLine(0, height - 1, width, height - 1);
        }
        graphics.setColor(originalColor);

        graphics.drawText(text, textX, 0, DrawStyle.ELLIPSIS, textWidth);
    }
}
