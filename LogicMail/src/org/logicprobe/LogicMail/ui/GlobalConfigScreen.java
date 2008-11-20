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
package org.logicprobe.LogicMail.ui;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.file.FileSystemRegistry;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.text.TextFilter;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;

/**
 * Configuration screen
 */
public class GlobalConfigScreen extends BaseCfgScreen implements FieldChangeListener {
    private MailSettings mailSettings;
    private GlobalConfig existingConfig;
    private String localHostname;
    private String[] fileSystemRoots;
    private int selectedFileSystemRootIndex;

    private static String LOCAL_FILE_BASE = "LogicMail/";
    
    private RichTextField globalSettingsLabel;
    private BasicEditField messageCountEditField;
    private ObjectChoiceField displayOrderChoiceField;
    private CheckboxField hideDeletedMessagesCheckboxField;
    private ObjectChoiceField wifiModeChoiceField;
    private ObjectChoiceField localDataLocationChoiceLabel;
    private RichTextField imapSettingsLabel;
    private BasicEditField imapMaxMsgSizeEditField;
    private BasicEditField imapMaxFolderDepthEditField;
    private RichTextField popSettingsLabel;
    private BasicEditField popMaxLinesEditField;
    private CheckboxField connectionDebuggingCheckboxField;
    private CheckboxField overrideHostnameCheckboxField;
    private BasicEditField localHostnameEditField;
    private ButtonField saveButton;

    public GlobalConfigScreen() {
        super("LogicMail - " +
            resources.getString(LogicMailResource.CONFIG_GLOBAL_TITLE));

        mailSettings = MailSettings.getInstance();
        existingConfig = mailSettings.getGlobalConfig();
        localHostname = existingConfig.getLocalHostname();

        // Populate fileSystemRoots with a list of all
        // available storage devices
        Vector resultsVector = new Vector();
        String selectedFileSystemRoot = existingConfig.getLocalDataLocation();
        int i = 0;
        selectedFileSystemRootIndex = 0;
        Enumeration e = FileSystemRegistry.listRoots();
        while(e.hasMoreElements()) {
        	String root = (String)e.nextElement();
        	if(selectedFileSystemRoot.endsWith(root + LOCAL_FILE_BASE)) {
        		selectedFileSystemRootIndex = i;
        	}
        	resultsVector.addElement(root);
        	i++;
        }
        fileSystemRoots = new String[resultsVector.size()];
        resultsVector.copyInto(fileSystemRoots);
        
        initFields();
    }

    private void initFields() {
        globalSettingsLabel = new RichTextField(
        		resources.getString(LogicMailResource.CONFIG_GLOBAL_GLOBAL_SETTINGS),
        		Field.NON_FOCUSABLE);

	    messageCountEditField = new BasicEditField(
	    		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_COUNT) + ' ',
	            Integer.toString(existingConfig.getRetMsgCount()));
	    messageCountEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
	
	    String[] orderTypes = {
	            resources.getString(LogicMailResource.MENUITEM_ORDER_ASCENDING),
	            resources.getString(LogicMailResource.MENUITEM_ORDER_DESCENDING)
	        };
	
	    if (!existingConfig.getDispOrder()) {
	        displayOrderChoiceField = new ObjectChoiceField(
	        		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_ORDER) + ' ',
	                orderTypes, 0);
	    } else {
	        displayOrderChoiceField = new ObjectChoiceField(
	        		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_ORDER) + ' ',
	                orderTypes, 1);
	    }
	
	    hideDeletedMessagesCheckboxField = new CheckboxField(
	    		resources.getString(LogicMailResource.CONFIG_GLOBAL_HIDE_DELETED_MESSAGES),
                existingConfig.getHideDeletedMsg());
	
	    String[] wifiModes = {
	            resources.getString(LogicMailResource.MENUITEM_DISABLED),
	            resources.getString(LogicMailResource.MENUITEM_PROMPT),
	            resources.getString(LogicMailResource.MENUITEM_ALWAYS)
	        };
	    wifiModeChoiceField = new ObjectChoiceField(
	    		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_WIFI_MODE) + ' ',
	    		wifiModes,
	    		existingConfig.getWifiMode());

	    localDataLocationChoiceLabel = new ObjectChoiceField(
	    		resources.getString(LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_LOCATION) + ' ',
	    		fileSystemRoots,
	    		selectedFileSystemRootIndex);
	    
	    imapSettingsLabel = new RichTextField(
	    		resources.getString(LogicMailResource.CONFIG_GLOBAL_IMAP_SETTINGS),
	            Field.NON_FOCUSABLE);
	    imapMaxMsgSizeEditField = new BasicEditField(
	    		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_IMAP_DOWNLOAD_LIMIT) + ' ',
	            Integer.toString(existingConfig.getImapMaxMsgSize() / 1024));
	    imapMaxMsgSizeEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
	
	    imapMaxFolderDepthEditField = new BasicEditField(
	    		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_IMAP_FOLDER_LIMIT) + ' ',
	            Integer.toString(existingConfig.getImapMaxFolderDepth()));
	    imapMaxFolderDepthEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
	    
	    popSettingsLabel = new RichTextField(
	    		resources.getString(LogicMailResource.CONFIG_GLOBAL_POP_SETTINGS),
	            Field.NON_FOCUSABLE);
	    popMaxLinesEditField = new BasicEditField(
	    		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_POP_DOWNLOAD_LIMIT) + ' ',
	            Integer.toString(existingConfig.getPopMaxLines()));
	    popMaxLinesEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
	
	    boolean overrideHostname = localHostname.length() > 0;
	    overrideHostnameCheckboxField = new CheckboxField(
	    		resources.getString(LogicMailResource.CONFIG_GLOBAL_OVERRIDE_HOSTNAME),
	            overrideHostname);
	    overrideHostnameCheckboxField.setChangeListener(this);
	
	    if (overrideHostname) {
	        localHostnameEditField = new BasicEditField(
	        		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_HOSTNAME) + ' ',
	                localHostname);
	    } else {
	        String hostname = System.getProperty("microedition.hostname");
	        localHostnameEditField = new BasicEditField(
	        		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_HOSTNAME) + ' ',
	                ((hostname != null) ? hostname : "localhost"));
	        localHostnameEditField.setEditable(false);
	    }
	
	    connectionDebuggingCheckboxField = new CheckboxField(
	    		resources.getString(LogicMailResource.CONFIG_GLOBAL_CONNECTION_DEBUGGING),
                existingConfig.getConnDebug());
	
	    saveButton = new ButtonField(
	    		resources.getString(LogicMailResource.MENUITEM_SAVE),
	    		Field.FIELD_HCENTER);
	    saveButton.setChangeListener(this);
	    
	    add(globalSettingsLabel);
	    add(messageCountEditField);
	    add(displayOrderChoiceField);
	    add(hideDeletedMessagesCheckboxField);
	    add(wifiModeChoiceField);
	    add(localDataLocationChoiceLabel);
	    add(new SeparatorField());
	    add(imapSettingsLabel);
	    add(imapMaxMsgSizeEditField);
	    add(imapMaxFolderDepthEditField);
	    add(new SeparatorField());
	    add(popSettingsLabel);
	    add(popMaxLinesEditField);
	    add(new SeparatorField());
	    add(overrideHostnameCheckboxField);
	    add(localHostnameEditField);
	    add(connectionDebuggingCheckboxField);
	    add(new SeparatorField());
	    add(saveButton);
    }
    
    public void fieldChanged(Field field, int context) {
        if (field == saveButton) {
            onClose();
        } else if (field == overrideHostnameCheckboxField) {
            if (overrideHostnameCheckboxField.getChecked()) {
                localHostnameEditField.setText(localHostname);
                localHostnameEditField.setEditable(true);
            } else {
                String hostname = System.getProperty("microedition.hostname");
                localHostnameEditField.setText((hostname != null) ? hostname
                                                            : "localhost");
                localHostnameEditField.setEditable(false);
            }
        }
    }

    public void save() {
        GlobalConfig config = mailSettings.getGlobalConfig();

        try {
            config.setRetMsgCount(Integer.parseInt(messageCountEditField.getText()));
        } catch (Exception e) { }

        if (displayOrderChoiceField.getSelectedIndex() == 0) {
            config.setDispOrder(false);
        } else {
            config.setDispOrder(true);
        }

        config.setHideDeletedMsg(hideDeletedMessagesCheckboxField.getChecked());

        config.setWifiMode(wifiModeChoiceField.getSelectedIndex());

        String url = "file:///" + fileSystemRoots[localDataLocationChoiceLabel.getSelectedIndex()] + LOCAL_FILE_BASE;
        config.setLocalDataLocation(url);
        
        try {
            config.setImapMaxMsgSize(Integer.parseInt(
                    imapMaxMsgSizeEditField.getText()) * 1024);
        } catch (Exception e) { }

        try {
            config.setImapMaxFolderDepth(Integer.parseInt(
                    imapMaxFolderDepthEditField.getText()));
        } catch (Exception e) { }

        try {
            config.setPopMaxLines(Integer.parseInt(popMaxLinesEditField.getText()));
        } catch (Exception e) { }

        if (overrideHostnameCheckboxField.getChecked()) {
            config.setLocalHostname(localHostnameEditField.getText().trim());
        } else {
            config.setLocalHostname("");
        }

        config.setConnDebug(connectionDebuggingCheckboxField.getChecked());
        mailSettings.saveSettings();
    }
}
