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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.util.IntVector;

public class FolderSelectionDialog extends Dialog {
	private static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	private RichTextField dialogLabelField;
	private ListField folderListField;
	
	private static String ROOT_URL = "file:///";
	private static String PREV_FOLDER = "..";
	private static String PATH_SEPARATOR = "/";
	
	private final static int TYPE_ROOT = 0;
	private final static int TYPE_FOLDER = 1;
	private final static int TYPE_OPTION_SELECT = 2;
	
	private String dialogTitleBase;
	private String folderUrl;
	private Vector folderList;
	private IntVector elementTypeList;
	
	private Bitmap rootBitmap = Bitmap.getBitmapResource("fs_root.png");
	private Bitmap folderBitmap = Bitmap.getBitmapResource("folder.png");
	private Bitmap optionSelectBitmap = Bitmap.getBitmapResource("fs_save.png");
	
	/**
	 * Instantiates a new folder selection dialog.
	 */
	public FolderSelectionDialog() {
        super("", null, null, 0,
                null,
                Manager.VERTICAL_SCROLLBAR);
		
        dialogTitleBase = resources.getString(LogicMailResource.FOLDERSELECTIONDIALOG_TITLE) + '\n';
        dialogLabelField = this.getLabel();
        
        folderUrl = ROOT_URL;
        folderList = new Vector();
        elementTypeList = new IntVector();
        
		setEscapeEnabled(true);
		initFields();
		populateFolderList();
	}
	
	private void initFields() {
		folderListField = new ListField();
		folderListField.setCallback(folderListFieldCallback);
		
		add(folderListField);
	}

	/**
	 * Sets the current folder URL.
	 * If there is an error while trying to open the provided
	 * URL, then the current folder URL is reset to the file
	 * system root URL.
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
	 * Populates folder list based on the current folder URL.
	 * 
	 * @return true, if successful, false if any errors were encountered
	 */
	private boolean populateFolderList() {
		boolean result;
		
		folderList.removeAllElements();
		elementTypeList.removeAllElements();
		folderListField.setSize(0);
		
		dialogLabelField.setText(dialogTitleBase + folderUrl.substring(ROOT_URL.length() - 1));
		
		if(folderUrl.equals(ROOT_URL)) {
	        Enumeration e = FileSystemRegistry.listRoots();
	        while(e.hasMoreElements()) {
	        	String root = (String)e.nextElement();
	        	folderList.addElement(root);
	        	elementTypeList.addElement(TYPE_ROOT);
	        }
	        result = true;
		}
		else {
			try {
				FileConnection fileConnection = (FileConnection)Connector.open(folderUrl);
				if(fileConnection.canWrite()) {
					folderList.addElement(resources.getString(LogicMailResource.FOLDERSELECTIONDIALOG_SELECT_FOLDER));
					elementTypeList.addElement(TYPE_OPTION_SELECT);
				}
				folderList.addElement(PREV_FOLDER);
				elementTypeList.addElement(TYPE_FOLDER);
				Enumeration e = fileConnection.list();
		        while(e.hasMoreElements()) {
		        	String file = (String)e.nextElement();
		        	if(file.endsWith(PATH_SEPARATOR)) {
			        	folderList.addElement(file);
			        	elementTypeList.addElement(TYPE_FOLDER);
		        	}
		        }
		        result = true;
			} catch (IOException e) {
				EventLogger.logEvent(AppInfo.GUID, ("Unable to open: " + folderUrl).getBytes(), EventLogger.ERROR);
				if(folderList.size() == 0) {
					folderList.addElement(PREV_FOLDER);
					elementTypeList.addElement(TYPE_FOLDER);
				}
				result = false;
			}
		}
        folderListField.setSize(folderList.size());
        return result;
	}
	
	/**
	 * Selection logic for when an item is chosen in the folder list.
	 */
	private void selectFolder() {
		int index = folderListField.getSelectedIndex();
		String selectedItem = (String)folderList.elementAt(index);
		int selectedType = elementTypeList.elementAt(index);
		if(selectedType == TYPE_OPTION_SELECT) {
			this.select(Dialog.OK);
		}
		else {
			if(selectedItem.equals(PREV_FOLDER)) {
				folderUrl = folderUrl.substring(0, folderUrl.lastIndexOf('/', folderUrl.length() - 2) + 1);
				if(folderUrl.length() < ROOT_URL.length()) { folderUrl = ROOT_URL; }
			}
			else {
				folderUrl = folderUrl + selectedItem;
			}
			populateFolderList();
		}
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Screen#trackwheelUnclick(int, int)
	 */
	protected boolean trackwheelUnclick(int status, int time) {
		selectFolder();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.component.Dialog#keyChar(char, int, int)
	 */
	protected boolean keyChar(char key, int status, int time) {
		if(key == Keypad.KEY_ENTER) {
			selectFolder();
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
			int elementType = elementTypeList.elementAt(index);
			if(elementType == TYPE_OPTION_SELECT) {
				icon = optionSelectBitmap;
			}
			else if(elementType == TYPE_ROOT) {
				icon = rootBitmap;
			}
			else {
				icon = folderBitmap;
			}
			int iconSide = icon.getWidth();

			graphics.drawBitmap(
					rowHeight/2 - iconSide/2,
					y + (fontHeight/2 - iconSide/2),
					iconSide,
					iconSide,
					icon, 0, 0);
			
			int indent = iconSide + iconSide/2;
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
}
