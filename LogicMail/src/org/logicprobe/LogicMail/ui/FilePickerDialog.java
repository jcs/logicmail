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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.ContentPart;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.MimeMessagePartFactory;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.io.MIMETypeAssociations;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.util.IntVector;

public class FilePickerDialog extends Dialog {
    private static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    private RichTextField dialogLabelField;
    private ListField folderListField;
    private ListField fileListField;

    private static String ROOT_URL = "file:///";
    private static String PREV_FOLDER = "..";
    private static String PATH_SEPARATOR = "/";
    
    private final static int TYPE_ROOT = 0;
    private final static int TYPE_FOLDER = 1;
    
    private String dialogTitleBase;
    private String folderUrl;
    private String fileUrl;
    private ContentPart mimeContentPart;
    private Vector folderList;
    private IntVector folderElementTypeList;
    private Vector fileList;
    private IntVector fileSizeList;
    
    private static final Bitmap rootBitmap = Bitmap.getBitmapResource("fs_root.png");
    private static final Bitmap folderBitmap = Bitmap.getBitmapResource("folder.png");

    /**
     * Instantiates a new folder selection dialog.
     */
    public FilePickerDialog() {
        super("", null, null, 0,
                null,
                Manager.VERTICAL_SCROLLBAR);
        
        dialogTitleBase = resources.getString(LogicMailResource.FILEPICKERDIALOG_TITLE) + '\n';
        dialogLabelField = this.getLabel();
        
        folderUrl = ROOT_URL;
        folderList = new Vector();
        folderElementTypeList = new IntVector();
        fileList = new Vector();
        fileSizeList = new IntVector();
        
        setEscapeEnabled(true);
        initFields();
        populateFolderList();
    }
    
    private void initFields() {
        folderListField = new ListField();
        folderListField.setCallback(folderListFieldCallback);
        fileListField = new ListField();
        fileListField.setCallback(fileListFieldCallback);
        
        add(folderListField);
        add(new SeparatorField());
        add(fileListField);
    }
    
    /**
     * Sets the current folder URL.
     * If there is an error while trying to open the provided URL, the current
     * folder URL is reset to the file system root URL.
     * 
     * @param folderUrl the new folder URL
     */
    public void setFolderUrl(String folderUrl) {
        this.folderUrl = folderUrl;
        if(!populateFolderList()) {
            this.folderUrl = ROOT_URL;
            populateFolderList();
        }
    }
    
    /**
     * Gets the currently selected folder URL.
     * 
     * @return the folder URL
     */
    public String getFolderUrl() {
        return this.folderUrl;
    }
    
    /**
     * Gets the currently selected file URL.
     * 
     * @return the file URL
     */
    public String getFileUrl() {
        return fileUrl;
    }
    
    /**
     * Gets the MIME content part encapsulating information about the
     * currently selected file.
     *
     * @return the MIME content part, or null if no file selected
     */
    public ContentPart getMimeContentPart() {
        return mimeContentPart;
    }
    
    /**
     * Populates folder list based on the current folder URL.
     * 
     * @return true, if successful, false if any errors were encountered
     */
    private boolean populateFolderList() {
        boolean result;
        
        folderList.removeAllElements();
        folderElementTypeList.removeAllElements();
        folderListField.setSize(0);
        
        fileList.removeAllElements();
        fileSizeList.removeAllElements();
        fileListField.setSize(0);
        
        dialogLabelField.setText(dialogTitleBase + folderUrl.substring(ROOT_URL.length() - 1));
        
        if(folderUrl.equals(ROOT_URL)) {
            Enumeration e = FileSystemRegistry.listRoots();
            while(e.hasMoreElements()) {
                String root = (String)e.nextElement();
                folderList.addElement(root);
                folderElementTypeList.addElement(TYPE_ROOT);
            }
            result = true;
        }
        else {
            try {
                FileConnection fileConnection = (FileConnection)Connector.open(folderUrl);
                if(fileConnection.canRead()) {
                    folderList.addElement(PREV_FOLDER);
                    folderElementTypeList.addElement(TYPE_FOLDER);
                    Enumeration e = fileConnection.list();
                    while(e.hasMoreElements()) {
                        String fileName = (String)e.nextElement();
                        if(fileName.endsWith(PATH_SEPARATOR)) {
                            folderList.addElement(fileName);
                            folderElementTypeList.addElement(TYPE_FOLDER);
                        }
                        else {
                            fileList.addElement(fileName);
                            fileSizeList.addElement(-1);
                        }
                    }
                    result = true;
                }
                else {
                    result = false;
                }
                fileConnection.close();
            } catch (IOException e) {
                EventLogger.logEvent(AppInfo.GUID, ("Unable to open: " + folderUrl).getBytes(), EventLogger.ERROR);
                if(folderList.size() == 0) {
                    folderList.addElement(PREV_FOLDER);
                    folderElementTypeList.addElement(TYPE_FOLDER);
                }
                result = false;
            } catch (ControlledAccessException e) {
                EventLogger.logEvent(AppInfo.GUID, ("No permission to open: " + folderUrl).getBytes(), EventLogger.ERROR);
                if(folderList.size() == 0) {
                    folderList.addElement(PREV_FOLDER);
                    folderElementTypeList.addElement(TYPE_FOLDER);
                }
                result = false;
            }
        }
        
        int fileListSize = fileList.size();
        if(fileListSize > 0) {
            for(int i=0; i<fileListSize; i++) {
                try {
                    FileConnection fileConnection =
                        (FileConnection)Connector.open(
                                folderUrl + (String)fileList.elementAt(i));
                    fileSizeList.setElementAt((int)fileConnection.fileSize(), i);
                    fileConnection.close();
                } catch (IOException e) { }
            }
        }
        
        folderListField.setSize(folderList.size());
        fileListField.setSize(fileList.size());
        return result;
    }
    
    /**
     * Selection logic for when a folder is chosen in the dialog.
     */
    private void selectFolder() {
        int index = folderListField.getSelectedIndex();
        String selectedItem = (String)folderList.elementAt(index);
        if(selectedItem.equals(PREV_FOLDER)) {
            folderUrl = folderUrl.substring(0, folderUrl.lastIndexOf('/', folderUrl.length() - 2) + 1);
            if(folderUrl.length() < ROOT_URL.length()) { folderUrl = ROOT_URL; }
        }
        else {
            folderUrl = folderUrl + selectedItem;
        }
        populateFolderList();
    }
    
    /**
     * Selection logic for when a file is chosen in the dialog.
     */
    private void selectFile() {
        int index = fileListField.getSelectedIndex();
        String selectedItem = (String)fileList.elementAt(index);
        try {
            FileConnection fileConnection = (FileConnection)Connector.open(
                    folderUrl + selectedItem);
            if(fileConnection.canRead()) {
                fileUrl = fileConnection.getURL();
                
                String mimeType = MIMETypeAssociations.getMIMEType(fileUrl);
                if(mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                
                int p = mimeType.indexOf('/');
                String mimeSubtype = mimeType.substring(p + 1);
                mimeType = mimeType.substring(0, p);
                
                p = fileUrl.lastIndexOf('/');
                String fileName = fileUrl.substring(p + 1);
                
                MimeMessagePart part =
                    MimeMessagePartFactory.createMimeMessagePart(
                            mimeType,
                            mimeSubtype,
                            fileName,
                            null,         // encoding
                            null,         // param
                            "attachment", // disposition
                            null,         // content ID
                            (int)fileConnection.fileSize(),
                            fileUrl);
                
                if(part instanceof ContentPart) {
                    mimeContentPart = (ContentPart)part;
                }
            }
            else {
                fileUrl = null;
                mimeContentPart = null;
            }
            fileConnection.close();
        } catch (IOException e) {
            fileUrl = null;
            mimeContentPart = null;
        }
        
        if(fileUrl != null && mimeContentPart != null) {
            select(Dialog.OK);
        }
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#trackwheelUnclick(int, int)
     */
    protected boolean trackwheelUnclick(int status, int time) {
        boolean result;
        Field fieldWithFocus = getLeafFieldWithFocus();
        if(fieldWithFocus == folderListField) {
            selectFolder();
            result = true;
        }
        else if(fieldWithFocus == fileListField) {
            selectFile();
            result = true;
        }
        else {
            result = false;
        }
        return result;
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.component.Dialog#keyChar(char, int, int)
     */
    protected boolean keyChar(char key, int status, int time) {
        boolean result = false;
        if(key == Keypad.KEY_ENTER) {
            Field fieldWithFocus = getLeafFieldWithFocus();
            if(fieldWithFocus == folderListField) {
                selectFolder();
                result = true;
            }
            else if(fieldWithFocus == fileListField) {
                selectFile();
                result = true;
            }
        }
        
        if(result) {
            return true;
        }
        else {
            return super.keyChar(key, status, time);
        }
    }
    
    private ListFieldCallback folderListFieldCallback = new ListFieldCallback() {
        public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
            int rowHeight = folderListField.getRowHeight();
            int fontHeight = graphics.getFont().getHeight();
            String elementName = folderList.elementAt(index).toString();
            
            Bitmap icon;
            int elementType = folderElementTypeList.elementAt(index);
            if(elementType == TYPE_ROOT) {
                icon = rootBitmap;
            }
            else {
                icon = folderBitmap;
            }
            int iconSide = icon.getWidth();
            int iconSideHalf = iconSide >>> 1;

            graphics.drawBitmap(
                    (rowHeight >>> 1) - iconSideHalf,
                    y + ((fontHeight >>> 1) - iconSideHalf),
                    iconSide,
                    iconSide,
                    icon, 0, 0);
            
            int indent = Math.max(rowHeight, iconSide + iconSideHalf);
            graphics.drawText(elementName, indent, y, DrawStyle.ELLIPSIS, width - indent);
        }

        public Object get(ListField listField, int index) {
            return folderList.elementAt(index);
        }

        public int getPreferredWidth(ListField listField) {
            return 0;
        }

        public int indexOfList(ListField listField, String prefix, int start) {
            return 0;
        }
    };
    
    private ListFieldCallback fileListFieldCallback = new ListFieldCallback() {
        public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
            int rowHeight = folderListField.getRowHeight();
            Font font = graphics.getFont();
            int fontHeight = font.getHeight();
            String fileName = (String)fileList.elementAt(index);
            int fileSize = fileSizeList.elementAt(index);
            
            Bitmap fileBitmap = MessageIcons.getIcon(MIMETypeAssociations.getMIMEType(fileName));
            int iconSide = fileBitmap.getWidth();
            int iconSideHalf = iconSide >>> 1;

            graphics.drawBitmap(
                    (rowHeight >>> 1) - iconSideHalf,
                    y + ((fontHeight >>> 1) - iconSideHalf),
                    iconSide,
                    iconSide,
                    fileBitmap, 0, 0);
            
            int indent = Math.max(rowHeight, iconSide + iconSideHalf);
            
            String fileSizeText;
            if(fileSize < 0) {
                fileSizeText = null;
            }
            else if(fileSize < 1024) {
                fileSizeText = Long.toString(fileSize) + " B";
            }
            else {
                fileSizeText = Long.toString(fileSize / 1024) + " KB";
            }
            
            int sizeWidth;
            if(fileSizeText != null) {
                sizeWidth = font.getAdvance(fileSizeText);
                graphics.drawText(fileSizeText, width - sizeWidth, y);
            }
            else {
                sizeWidth = 0;
            }
            
            graphics.drawText(fileName, indent, y, DrawStyle.ELLIPSIS,
                    width - indent - sizeWidth);
        }

        public Object get(ListField listField, int index) {
            return fileList.elementAt(index);
        }

        public int getPreferredWidth(ListField listField) {
            return 0;
        }

        public int indexOfList(ListField listField, String prefix, int start) {
            return 0;
        }
    };
}
