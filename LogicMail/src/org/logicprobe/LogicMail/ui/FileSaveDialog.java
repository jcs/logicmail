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

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.MailSettings;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.HorizontalFieldManager;

public class FileSaveDialog extends Dialog {
	private static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);

	private ButtonField folderButtonField;
	private LabelField folderNameField;
	private EditField nameEditField;
	private ButtonField saveButtonField;
	private ButtonField cancelButtonField;
	
	private static String ROOT_URL = "file:///";
	private String folderUrl;
	
	/**
	 * Instantiates a new file save dialog.
	 * 
	 * @param folderUrl the folder URL
	 * @param fileName the file name
	 */
	public FileSaveDialog(String folderUrl, String fileName) {
        super(' ' + resources.getString(LogicMailResource.FILESAVEDIALOG_TITLE), null, null, 0,
              Bitmap.getPredefinedBitmap(Bitmap.INFORMATION),
              Manager.NO_VERTICAL_SCROLL);
		
        this.folderUrl = ROOT_URL;
        
		setEscapeEnabled(true);
		initFields();

		if(folderUrl == null || folderUrl.length() == 0 || !folderUrl.startsWith(ROOT_URL)) {
			String localDataLocation = MailSettings.getInstance().getGlobalConfig().getLocalDataLocation();
			if(localDataLocation.startsWith("file:///store")) {
				setFolderUrl("file:///store/home/user/");
			}
			else if(localDataLocation.startsWith("file:///SDCard")) {
				setFolderUrl("file:///SDCard/BlackBerry/");
			}
			else {
				setFolderUrl(null);
			}
		}
		else {
			setFolderUrl(folderUrl);
		}
		
		setFileName(fileName);
	}
	
	/**
	 * Instantiates a new file save dialog.
	 * 
	 * @param fileName the file name
	 */
	public FileSaveDialog(String fileName) {
		this(null, fileName);
	}
	
	private void initFields() {
		HorizontalFieldManager folderFieldManager = new HorizontalFieldManager();
		folderButtonField = new BitmapButtonField(Bitmap.getBitmapResource("folder.png"));
		folderButtonField.setChangeListener(fieldChangeListener);
		folderNameField = new LabelField(folderUrl.substring(ROOT_URL.length() - 1));
		folderFieldManager.add(folderButtonField);
		folderFieldManager.add(new LabelField(" ", Field.NON_FOCUSABLE));
		folderFieldManager.add(folderNameField);
		
		nameEditField = new EditField(resources.getString(LogicMailResource.FILESAVEDIALOG_NAME) + ' ', "");
		nameEditField.setChangeListener(fieldChangeListener);

		HorizontalFieldManager buttonFieldManager = new HorizontalFieldManager(Manager.FIELD_HCENTER);
		saveButtonField = new ButtonField(resources.getString(LogicMailResource.MENUITEM_SAVE));
		saveButtonField.setChangeListener(fieldChangeListener);
		saveButtonField.setEditable(false);
		cancelButtonField = new ButtonField(resources.getString(LogicMailResource.MENUITEM_CANCEL));
		cancelButtonField.setChangeListener(fieldChangeListener);
		buttonFieldManager.add(saveButtonField);
		buttonFieldManager.add(cancelButtonField);
		
		add(folderFieldManager);
		add(nameEditField);
		add(new SeparatorField());
		add(buttonFieldManager);
	}
	
	private FieldChangeListener fieldChangeListener = new FieldChangeListener() {
		public void fieldChanged(Field field, int context) {
			if(field == nameEditField) {
				FileSaveDialog.this.validateDialog();
			}
			else if(field == folderButtonField) {
				FileSaveDialog.this.selectFolder();
			}
			else if(field == saveButtonField) {
				FileSaveDialog.this.select(Dialog.SAVE);
			}
			else if(field == cancelButtonField) {
				FileSaveDialog.this.cancel();
			}
		}
	};
	
	/**
	 * Sets the folder URL.
	 * 
	 * @param folderUrl the new folder URL
	 */
	public void setFolderUrl(String folderUrl) {
		if(folderUrl != null && folderUrl.length() > 0) {
			try {
				FileConnection fileConnection = (FileConnection)Connector.open(folderUrl);
				if(fileConnection.isDirectory() && fileConnection.canWrite()) {
					this.folderUrl = folderUrl;
				}
			} catch (IOException e) {
				EventLogger.logEvent(AppInfo.GUID, ("Unable to open: " + folderUrl).getBytes(), EventLogger.ERROR);
				this.folderUrl = ROOT_URL;
			}
		}
		else {
			this.folderUrl = ROOT_URL;
		}
		folderNameField.setText(this.folderUrl.substring(ROOT_URL.length() - 1));
		validateDialog();
	}
	
	/**
	 * Sets the file name.
	 * 
	 * @param fileName the new file name
	 */
	public void setFileName(String fileName) {
		if(fileName != null) {
			nameEditField.setText(fileName);
		}
		else {
			nameEditField.setText("");
		}
		validateDialog();
	}
	
	/**
	 * Gets the file URL.
	 * This is a combination of the folder URL and the file name.
	 * 
	 * @return the file URL
	 */
	public String getFileUrl() {
		if(saveButtonField.isEditable()) {
			return folderUrl + nameEditField.getText();
		}
		else {
			return null;
		}
	}

	/**
	 * Determines whether the selected folder and filename are
	 * valid, and enables the "Save" button accordingly.
	 */
	private void validateDialog() {
		boolean isValid;
		
		if(folderUrl.equals(ROOT_URL) || nameEditField.getText().length() == 0) {
			isValid = false;
		}
		else {
			isValid = true;
		}
		
		saveButtonField.setEditable(isValid);
	}
	
	/**
	 * Selects the folder, using the folder selection dialog.
	 */
	private void selectFolder() {
		FolderSelectionDialog dialog = new FolderSelectionDialog();
		dialog.setFolderUrl(folderUrl);
		if(dialog.doModal() != Dialog.CANCEL) {
			folderUrl = dialog.getFolderUrl();
			folderNameField.setText(folderUrl.substring(ROOT_URL.length() - 1));
			validateDialog();
		}
	}
}
