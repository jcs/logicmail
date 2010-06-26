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

import org.logicprobe.LogicMail.PlatformInfo;
import org.logicprobe.LogicMail.message.ContentPart;
import org.logicprobe.LogicMail.model.MessageNode;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;

/**
 * Field to represent an attachment item on the message screen.
 */
public class AttachmentField extends Field {
    private MessageNode messageNode;
    private ContentPart messagePart;
    private Bitmap icon;
    private String attachmentName;
    private String attachmentSize;
    private int maxWidth;
    
    private static boolean hasTouchscreen = PlatformInfo.getInstance().hasTouchscreen();
    
    /**
     * Instantiates a new attachment field.
     *
     * @param messageNode the message on which the attachment exists
     * @param messagePart the message part to be represented
     */
    public AttachmentField(MessageNode messageNode, ContentPart messagePart) {
        super(Field.FOCUSABLE);
        this.messageNode = messageNode;
        this.messagePart = messagePart;
        this.icon = MessageIcons.getIcon(messagePart);
        this.attachmentName = buildMessageName(messagePart);
        this.attachmentSize = buildMessageSizeText(messagePart);
    }

    
    /**
     * Gets the message node an attachment is being shown from.
     *
     * @return the message node
     */
    public MessageNode getMessageNode() {
        return messageNode;
    }
    
    /**
     * Gets the message part representing the attachment being shown.
     *
     * @return the message part
     */
    public ContentPart getMessagePart() {
        return messagePart;
    }
    
    private static String buildMessageName(ContentPart messagePart) {
        StringBuffer buf = new StringBuffer();
        buf.append(messagePart.getName());
        if(buf.length() == 0) {
            buf.append(messagePart.getMimeType());
            buf.append('/');
            buf.append(messagePart.getMimeSubtype());
        }
        return buf.toString();
    }
    
    private static String buildMessageSizeText(ContentPart messagePart) {
        StringBuffer buf = new StringBuffer();
        int partSize = messagePart.getSize();
        if(partSize > 0) {
            buf.append('(');
            if(partSize < 1024) {
                buf.append(partSize);
                buf.append('B');
            }
            else {
                partSize /= 1024;
                buf.append(partSize);
                buf.append("kB");
            }
            buf.append(')');
        }
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#layout(int, int)
     */
    protected void layout(int width, int height) {
        this.maxWidth = width;
        setExtent(width, getPreferredHeight());
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#getPreferredHeight()
     */
    public int getPreferredHeight() {
        Font font = getFont();
        if(messageNode.getMessageContent(messagePart) != null) {
            font = font.derive(Font.BOLD);
        }
        if(hasTouchscreen) {
            return font.getHeight() + 10;
        }
        else {
            return font.getHeight();
        }
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#paint(net.rim.device.api.ui.Graphics)
     */
    protected void paint(Graphics graphics) {
        int halfHeight = getHeight() >>> 1;
        Font font = graphics.getFont();
        if(messageNode.getMessageContent(messagePart) != null) {
            font = font.derive(Font.BOLD);
        }
        
        int indent = 2;
        
        if(icon != null) {
            int iconWidth = icon.getWidth();
            
            graphics.drawBitmap(
                    indent,
                    Math.max(halfHeight - (iconWidth >>> 1), 0),
                    iconWidth,
                    icon.getHeight(),
                    icon, 0, 0);
            
            indent += iconWidth + 4;
        }
        
        int textPos = Math.max(halfHeight - (font.getHeight() >>> 1), 0);
        
        int sizeWidth = font.getAdvance(attachmentSize);
        graphics.drawText(attachmentSize, maxWidth - sizeWidth, textPos);
        
        graphics.drawText(attachmentName, indent, textPos, Graphics.ELLIPSIS,
                maxWidth - indent - sizeWidth);
    }
}
