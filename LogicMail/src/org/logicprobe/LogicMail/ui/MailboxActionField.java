/*-
 * Copyright (c) 2010, Derek Konigsberg
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
 * Field to represent non-message click-able items on the mailbox screen.
 */
public class MailboxActionField extends Field {
    private String text;
    private int lineHeight;
    private Object tag;
    
    public MailboxActionField(String text, long style) {
        super(style);
        this.text = text;
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
    
    /**
     * Sets the tag object associated with this field.
     *
     * @param tag the new tag object
     */
    public void setTagObject(Object tag) {
        this.tag = tag;
    }
    
    /**
     * Gets the tag object associated with this field.
     *
     * @return the tag object
     */
    public Object getTagObject() {
        return tag;
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#getPreferredHeight()
     */
    public int getPreferredHeight() {
        return (Font.getDefault().getHeight() * 2);
    };
    
    protected void layout(int width, int height) {
        lineHeight = getPreferredHeight() >>> 1;
        setExtent(width, getPreferredHeight());
    }

    protected void paint(Graphics graphics) {
        int doubleLineHeight = lineHeight * 2;
        int width = this.getWidth();
        int height = this.getHeight();
        int originalColor = graphics.getColor();

        // Draw the separator line
        graphics.setColor(Color.DARKGRAY);
        graphics.drawLine(0, doubleLineHeight - 1, width, doubleLineHeight - 1);
        
        if(!isFocus()) {
            graphics.setColor(0xEFEFEF);
            graphics.fillRect(0, 0, width, height - 1);
        }
        
        graphics.setColor(originalColor);
        
        Font font = getFont();
        int advance = font.getAdvance(text);
        
        int x = (width >>> 1) - (advance >>> 1);
        int y = (height >>> 1) - (font.getHeight() >>> 1);
        if(y % 2 == 0) { y--; }

        if(!isEditable()) {
            graphics.setColor(Color.GRAY);
        }
        graphics.drawText(text, x, y, DrawStyle.TOP, width);
        graphics.setColor(originalColor);
    }
}
