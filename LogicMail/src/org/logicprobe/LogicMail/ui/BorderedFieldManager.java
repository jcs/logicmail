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

import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;

/**
 * Provides a container that emulates the behavior of a vertical field
 * manager, adding a border to the enclosed fields.
 */
public class BorderedFieldManager extends Manager {
	/** The border width. */
	private static final int borderWidth = 4;
	
	private static final int borderMargin = borderWidth * 2;
	
	protected static final long BORDER_STYLE_MASK = 0x00000000000000F0L;
    protected static final long FILL_STYLE_MASK   = 0x0000000000000F00L;
    
	/**
	 * Show a normal border on the bottom.
	 */
	public static final long BOTTOM_BORDER_NORMAL = 0x0000000000000000L;
	
	/**
	 * Do not show a border on the bottom.
	 * Used to eliminate excessive border space on
	 * vertically stacked instances.
	 */
	public static final long BOTTOM_BORDER_NONE = 0x0000000000000020L;
	
	/**
	 * Show a line on the bottom of the border.
	 */
	public static final long BOTTOM_BORDER_LINE = 0x0000000000000040L;
	
	/**
	 * Fill in the bordered area, which is normal behavior.
	 */
	public static final long OUTER_FILL_NORMAL = 0x0000000000000000L;
	
	/**
	 * Only draw the inner line, not filling the outer area.
	 */
    public static final long OUTER_FILL_NONE = 0x0000000000000100L;
	
	private boolean bottomBorderNone;
	private boolean bottomBorderLine;
	private boolean outerFill;
	private boolean useAllHeight;
	
	/**
	 * Instantiates a new bordered field manager.
	 */
	public BorderedFieldManager() {
        super(Manager.NO_HORIZONTAL_SCROLL | Manager.NO_VERTICAL_SCROLL | BOTTOM_BORDER_NORMAL);
        initStyles();
    }

    /**
     * Instantiates a new bordered field manager.
     * 
     * @param style the style
     */
    public BorderedFieldManager(long style) {
        super(style);
        initStyles();
    }

    private void initStyles() {
        long style = this.getStyle();
        
        bottomBorderNone = (style & BORDER_STYLE_MASK) == BOTTOM_BORDER_NONE;
        bottomBorderLine = (style & BORDER_STYLE_MASK) == BOTTOM_BORDER_LINE;
        
        outerFill = (style & FILL_STYLE_MASK) == OUTER_FILL_NORMAL;
        
        useAllHeight = ((style & USE_ALL_HEIGHT) == USE_ALL_HEIGHT);
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Manager#paint(net.rim.device.api.ui.Graphics)
     */
    protected void paint(Graphics graphics) {
        int width = this.getWidth();
        int height = this.getHeight();
        int foregroundColor = graphics.getColor();
        int backgroundColor = graphics.getBackgroundColor();

        paintBorder(graphics, width, height, backgroundColor);

        paintSeparator(graphics, width, height);
        
        // Resume normal painting of the contents
        graphics.setColor(foregroundColor);
        super.paint(graphics);
    }

    protected void paintBorder(Graphics graphics, int width, int height, int backgroundColor) {
        if(outerFill) {
            // Paint the fill for the field
            graphics.setColor(Color.LIGHTGREY);
            graphics.fillRect(0, 0, width, height);
    
            // Paint the rounded rectangular cutout section for the contents
            graphics.setColor(backgroundColor);
            graphics.fillRoundRect(borderWidth, borderWidth,
                    width - borderMargin,
                    height - (bottomBorderNone ? borderWidth : borderMargin),
                    10, 10);
        }

        // Paint the inner border of the cutout section
        graphics.setColor(Color.DARKGRAY);
        graphics.drawRoundRect(borderWidth, borderWidth,
                width - borderMargin,
                height - (bottomBorderNone ? borderWidth : borderMargin),
                10, 10);
    }
    
    protected void paintSeparator(Graphics graphics, int width, int height) {
        if(bottomBorderLine) {
            graphics.drawLine(0, height - 1, width - 1, height - 1);
        }
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Manager#sublayout(int, int)
     */
    protected void sublayout(int maxWidth, int maxHeight) {
        int count = this.getFieldCount();
        int y = borderWidth;
        for(int i=0; i<count; i++) {
    		y += 2;
            Field field = this.getField(i);
            this.setPositionChild(field, borderMargin, y);
            this.layoutChild(field, maxWidth - (borderMargin * 2), getPreferredHeightOfChild(field));
            y += field.getHeight();
        }
        int height = getPreferredHeight();
        if(useAllHeight && getManager() != null) {
            Manager manager = getManager();
            int fieldCount = manager.getFieldCount();
            int otherFieldsHeight = 0;
            for(int i=0; i<fieldCount; i++) {
                Field field = manager.getField(i);
                if(field != this) {
                    otherFieldsHeight += field.getExtent().height;
                }
            }
            int displayHeight = Display.getHeight() - manager.getTop();
            if((height + otherFieldsHeight) < displayHeight) {
                height = displayHeight - otherFieldsHeight;
            }
        }
        setExtent(maxWidth, height);
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#getPreferredWidth()
     */
    public int getPreferredWidth() {
    	return Display.getWidth();
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#getPreferredHeight()
     */
    public int getPreferredHeight() {
        int sum = (bottomBorderNone ? borderWidth : borderMargin);
        int count = this.getFieldCount();
        for(int i=0; i<count; i++) {
    		sum += 2;
            sum += this.getField(i).getHeight();
        }
        sum += 2;
        return sum;
    }
}
