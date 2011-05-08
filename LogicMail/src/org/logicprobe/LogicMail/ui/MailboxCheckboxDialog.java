/*-
 * Copyright (c) 2011, Derek Konigsberg
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

import java.util.Enumeration;
import java.util.Hashtable;

import org.logicprobe.LogicMail.model.MailboxNode;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.util.ToIntHashtable;

public class MailboxCheckboxDialog extends Dialog {
    private final MailboxNode rootMailbox;
    private final ToIntHashtable nodeIdMap;
    private final Hashtable selectedNodeSet;
    private final Hashtable disabledNodeSet;
    private TreeField treeField;
    
    public MailboxCheckboxDialog(String title, MailboxNode rootMailbox) {
        super(
                title,
                new Object[0],
                new int[0],
                Dialog.OK, null, VerticalFieldManager.VERTICAL_SCROLLBAR);
        setEscapeEnabled(true);
        
        this.rootMailbox = rootMailbox;
        
        initFields();
        
        nodeIdMap = new ToIntHashtable();
        selectedNodeSet = new Hashtable();
        disabledNodeSet = new Hashtable();
        populateTreeField();
    }

    private void initFields() {
        treeField = FieldFactory.getInstance().getScreenTreeField(new TreeFieldCallback() {
            public void drawTreeItem(TreeField treeField, Graphics graphics,
                    int node, int y, int width, int indent) {
                treeField_DrawTreeItem(treeField, graphics, node, y, width, indent);                
            }
        }, Field.FOCUSABLE);
        
        add(treeField);
    }

    private void populateTreeField() {
        int firstNode = -1;
        MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
        for(int i=mailboxNodes.length - 1; i >= 0; --i) {
            int id = treeField.addChildNode(0, mailboxNodes[i]);
            if(i == 0) { firstNode = id; }
            nodeIdMap.put(mailboxNodes[i], id);
            
            MailboxNode[] subMailboxNodes = mailboxNodes[i].getMailboxes();
            for(int j=subMailboxNodes.length - 1; j>=0; --j) {
                populateTreeFieldMailbox(id, subMailboxNodes[j]);
            }
        }
        
        if(firstNode != -1) {
            treeField.setCurrentNode(firstNode);
        }
    }
    
    private void populateTreeFieldMailbox(int parent, MailboxNode mailboxNode) {
        int id = treeField.addChildNode(parent, mailboxNode);
        nodeIdMap.put(mailboxNode, id);
        
        MailboxNode[] mailboxes = mailboxNode.getMailboxes();
        for(int i=mailboxes.length - 1; i >= 0; --i) {
            populateTreeFieldMailbox(id, mailboxes[i]);
        }
    }
    
    private void treeField_DrawTreeItem(
            TreeField treeField,
            Graphics graphics,
            int node, int y, int width, int indent) {
        MailboxNode mailboxNode = (MailboxNode)treeField.getCookie(node);
        
        Font font = graphics.getFont();
        int margin;
        if(!mailboxNode.isSelectable()) {
            graphics.setFont(font.derive(Font.ITALIC));
            margin = 0;
        }
        else {
            graphics.setFont(font.derive(Font.PLAIN));
            
            int checkboxSize = font.getHeight() - 4;
            margin = checkboxSize + 5;
            int checkboxX = indent + width - margin;
            int checkboxY = y + 2;
            
            boolean enabled = isEnabled(mailboxNode);
            int originalColor = graphics.getColor();
            if(!enabled) {
                graphics.setColor(Color.GRAY);
            }
            graphics.drawRoundRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 12, 12);
            
            if(selectedNodeSet.containsKey(mailboxNode)) {
                graphics.drawLine(checkboxX + 3, checkboxY + 3, checkboxX + checkboxSize - 4, checkboxY + checkboxSize - 4);
                graphics.drawLine(checkboxX + 3, checkboxY + checkboxSize - 4, checkboxX + checkboxSize - 4, checkboxY + 3);
            }
            
            if(!enabled) {
                graphics.setColor(originalColor);
            }
        }
        graphics.drawText(mailboxNode.toString(), indent, y, Graphics.ELLIPSIS, width - margin);
    }

    protected boolean navigationClick(int status, int time) {
        int node = treeField.getCurrentNode();
        if(treeField.isFocus() && node != -1) {
            MailboxNode mailboxNode = (MailboxNode)treeField.getCookie(node);
            if(mailboxNode == null || !isEnabled(mailboxNode)) { return false; }
            setChecked(mailboxNode, !isChecked(mailboxNode));
            treeField.invalidateNode(node);
            return true;
        }
        else {
            return false;
        }
    }

    public void setChecked(MailboxNode mailboxNode, boolean checked) {
        if(checked) {
            selectedNodeSet.put(mailboxNode, Boolean.TRUE);
        }
        else {
            selectedNodeSet.remove(mailboxNode);
        }
    }
    
    public boolean isChecked(MailboxNode mailboxNode) {
        return selectedNodeSet.containsKey(mailboxNode);
    }

    public void setCheckedNodes(MailboxNode[] mailboxNodes) {
        selectedNodeSet.clear();
        if(mailboxNodes == null || mailboxNodes.length == 0) { return; }
        
        for(int i=0; i<mailboxNodes.length; i++) {
            selectedNodeSet.put(mailboxNodes[i], Boolean.TRUE);
        }
    }
    
    public MailboxNode[] getCheckedNodes() {
        MailboxNode[] nodes = new MailboxNode[selectedNodeSet.size()];
        
        int i = 0;
        Enumeration e = selectedNodeSet.keys();
        while(e.hasMoreElements()) {
            nodes[i++] = (MailboxNode)e.nextElement();
        }
        return nodes;
    }
    
    public void setEnabled(MailboxNode mailboxNode, boolean enabled) {
        if(enabled) {
            disabledNodeSet.remove(mailboxNode);
        }
        else {
            disabledNodeSet.put(mailboxNode, Boolean.TRUE);
        }
    }
    
    public boolean isEnabled(MailboxNode mailboxNode) {
        return !disabledNodeSet.containsKey(mailboxNode);
    }
}
