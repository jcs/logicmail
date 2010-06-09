/*-
 * Copyright (c) 2006, Derek Konigsberg
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

package org.logicprobe.LogicMail.mail;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * Relevant information describing a folder tree.
 */
public class FolderTreeItem implements Serializable {
    private long uniqueId;
    private FolderTreeItem parent;
    private FolderTreeItem[] children;
    private String name;
    private String path;
    private String delim;
    private boolean selectable;
    private boolean appendable;
    private int msgCount;
    private int unseenCount;

    /**
     * Create a root folder tree item.
     * @param name The name of the folder
     * @param path The full path to this folder
     * @param delim The path delimiter
     */
    public FolderTreeItem(String name, String path, String delim) {
        this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        this.parent = null;
        this.name = name;
        this.path = path;
        this.delim = delim;
        this.selectable = false;
        this.appendable = false;
        this.msgCount = -1;
        this.unseenCount = -1;
        this.children = null;
    }
    
    /**
     * Create a root folder tree item.
     * @param name The name of the folder
     * @param path The full path to this folder
     * @param delim The path delimiter
     * @param selectable True if the folder is selectable
     */
    public FolderTreeItem(String name, String path, String delim, boolean selectable) {
        this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        this.parent = null;
        this.name = name;
        this.path = path;
        this.delim = delim;
        this.selectable = selectable;
        this.appendable = false;
        this.msgCount = -1;
        this.unseenCount = -1;
        this.children = null;
    }
    
    /**
     * Create a folder tree item.
     * @param parent The parent of this item
     * @param name The name of the folder
     * @param path The full path to this folder
     * @param delim The path delimiter
     * @param selectable True if the folder is selectable
     * @param appendable True if messages can be appended to the folder
     */
    public FolderTreeItem(FolderTreeItem parent, String name, String path, String delim, boolean selectable, boolean appendable) {
        this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        this.parent = parent;
        this.name = name;
        this.path = path;
        this.delim = delim;
        this.selectable = selectable;
        this.appendable = appendable;
        this.msgCount = -1;
        this.unseenCount = -1;
        this.children = null;
    }
    
    /**
     * Create a folder tree item that is a clone of the source item,
     * but has none of the references to parent or children items.
     * 
     * @param source The source item.
     */
    public FolderTreeItem(FolderTreeItem source) {
    	this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        this.name = source.name;
        this.path = source.path;
        this.delim = source.delim;
        this.selectable = source.selectable;
        this.appendable = source.appendable;
        this.msgCount = source.msgCount;
        this.unseenCount = source.unseenCount;
    }
    
    /**
     * Creates an uninitialized folder tree item.
     * Only for use during deserialization.
     */
    public FolderTreeItem() {
        // Possibly redundant, but we want to ensure that
        // this is created even if deserialization fails.
        this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
     */
    public void serialize(DataOutput output) throws IOException {
        output.writeLong(uniqueId);
        output.writeUTF(name);
        output.writeUTF(path);
        output.writeUTF(delim);
        output.writeBoolean(selectable);
        output.writeBoolean(appendable);
        output.writeInt(msgCount);
        if(children != null && children.length > 0) {
            output.writeInt(children.length);
            for(int i=0;i<children.length;i++)
                children[i].serialize(output);
        }
        else {
            output.writeInt(0);
        }
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
     */
    public void deserialize(DataInput input) throws IOException {
        uniqueId = input.readLong();
        name = input.readUTF();
        path = input.readUTF();
        delim = input.readUTF();
        selectable = input.readBoolean();
        appendable = input.readBoolean();
        msgCount = input.readInt();
        int childCount = input.readInt();
        if(childCount > 0) {
            for(int i=0;i<childCount;i++) {
                FolderTreeItem item = new FolderTreeItem();
                item.deserialize(input);
                item.parent = this;
                addChild(item);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
     */
    public long getUniqueId() {
        return uniqueId;
    }
    
    public FolderTreeItem[] children() {
        return children;
    }

    public FolderTreeItem getParent() {
        return parent;
    }

    /**
     * This method only exists to support folder tree generation code in the
     * IMAP implementation.  It should never be called from anywhere else.
     */
    public void setParent(FolderTreeItem parent) {
        this.parent = parent;
    }
    
    public boolean hasChildren() {
        return (children != null && children.length > 0);
    }

    public void addChild(FolderTreeItem item) {
        if(children == null) {
            children = new FolderTreeItem[1];
            children[0] = item;
        }
        else {
            Arrays.add(children, item);
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getDelim() {
        return delim;
    }

    public boolean isSelectable() {
        return selectable;
    }
    
    /**
     * This method only exists to support folder tree generation code in the
     * IMAP implementation.  It should never be called from anywhere else.
     */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
    
    public boolean isAppendable() {
    	return appendable;
    }
    
    public int getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(int msgCount) {
        this.msgCount = msgCount;
    }
    
    public int getUnseenCount() {
        return unseenCount;
    }
    
    public void setUnseenCount(int unseenCount) {
        this.unseenCount = unseenCount;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return 31 * 1 + (int) (uniqueId ^ (uniqueId >>> 32));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FolderTreeItem other = (FolderTreeItem) obj;
        if (uniqueId != other.uniqueId) {
            return false;
        }
        return true;
    }
}
