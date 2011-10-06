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

import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.component.SeparatorField;

/**
 * Horizontal separator field that is used to provide spacing
 * between other fields.
 */
public class BlankSeparatorField extends SeparatorField {
    private int size;
    
    public BlankSeparatorField(int size) {
        super();
        this.size = size;
    }
    
    /**
     * Convenience method for creating a new blank separator that is half
     * the height of the default font.
     */
    public static BlankSeparatorField createHalfHeightSeparator() {
        return new BlankSeparatorField(Font.getDefault().getAscent() >>> 1);
    }
    
    /**
     * Convenience method for creating a new blank separator that is a quarter
     * the height of the default font.
     */
    public static BlankSeparatorField createQuarterHeightSeparator() {
        return new BlankSeparatorField(Font.getDefault().getAscent() >>> 2);
    }
    
    public int getPreferredHeight() {
        return size;
    }

    protected void layout(int width, int height) {
        setExtent(width, size);
    }
    
    protected void paint(Graphics graphics) {
        // Field should be empty
    }
}
