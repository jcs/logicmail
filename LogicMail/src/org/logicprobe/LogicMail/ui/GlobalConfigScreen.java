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

import net.rim.device.api.i18n.Locale;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.RichTextField;
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
    private String[] languageChoices;
    private String[] languageCodes;
    private String[] fileSystemRoots;
    private int selectedFileSystemRootIndex;

    private static String LOCAL_FILE_BASE = "LogicMail/";
    
    private RichTextField globalSettingsLabel;
    private ObjectChoiceField languageChoiceField;
    private CheckboxField unicodeNormalizationCheckboxField;
    private BasicEditField messageCountEditField;
    private ObjectChoiceField displayOrderChoiceField;
    private CheckboxField hideDeletedMessagesCheckboxField;
    private ObjectChoiceField wifiModeChoiceField;
    private ObjectChoiceField localDataLocationChoiceLabel;
    private CheckboxField connectionDebuggingCheckboxField;
    private CheckboxField overrideHostnameCheckboxField;
    private BasicEditField localHostnameEditField;

    public GlobalConfigScreen() {
        super("LogicMail - " +
            resources.getString(LogicMailResource.CONFIG_GLOBAL_TITLE));

        mailSettings = MailSettings.getInstance();
        existingConfig = mailSettings.getGlobalConfig();
        localHostname = existingConfig.getLocalHostname();
        languageChoices = new String[] {
                "BlackBerry", // System default
                "Dansk",        // Danish: da
                "Deutsch",      // German: de
                "English",      // English: en
                "Español",      // Spanish: es
                "Français",     // French: fr
                "Nederlands",   // Dutch: nl
                "Ti\u00ea\u0301ng Vi\u00ea\u0323t", // Vietnamese: vi
                "\u4E2D\u6587", // Chinese: zh
        };
        languageCodes = new String[] {
                "", // System default
                "da", // Danish
                "de", // German
                "en", // English
                "es", // Spanish
                "fr", // French
                "nl", // Dutch
                "vi", // Vietnamese
                "zh", // Chinese
        };

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

        String languageCode = existingConfig.getLanguageCode();
        int languageIndex = 0;
        if(languageCode != null && languageCode.length() != 0) {
            for(int i=0; i<languageCodes.length; i++) {
                if(languageCodes[i].equals(languageCode)) {
                    languageIndex = i;
                    break;
                }
            }
        }
        languageChoiceField = new ObjectChoiceField(
        		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_LANGUAGE),
        		languageChoices,
        		languageIndex);
        
		unicodeNormalizationCheckboxField = new CheckboxField(
				resources.getString(LogicMailResource.CONFIG_GLOBAL_UNICODE_NORMALIZATION),
				existingConfig.getUnicodeNormalization());

	    messageCountEditField = new BasicEditField(
	    		"  " + resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_COUNT) + ' ',
	            Integer.toString(existingConfig.getRetMsgCount()));
	    messageCountEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
	
	    String[] orderTypes = {
	            resources.getString(LogicMailResource.MENUITEM_ORDER_ASCENDING),
	            resources.getString(LogicMailResource.MENUITEM_ORDER_DESCENDING)
	        };
	
	    if (existingConfig.getDispOrder()) {
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
	
	    add(globalSettingsLabel);
		add(languageChoiceField);
		add(unicodeNormalizationCheckboxField);
	    add(messageCountEditField);
	    add(displayOrderChoiceField);
	    add(hideDeletedMessagesCheckboxField);
	    add(wifiModeChoiceField);
	    add(localDataLocationChoiceLabel);
	    add(overrideHostnameCheckboxField);
	    add(localHostnameEditField);
	    add(connectionDebuggingCheckboxField);
    }
    
    public void fieldChanged(Field field, int context) {
        if (field == overrideHostnameCheckboxField) {
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

        String languageCode = languageCodes[languageChoiceField.getSelectedIndex()];
        if(languageCode != null && languageCode.length() != 0) {
            try {
                Locale.setDefault(Locale.get(languageCode));
                config.setLanguageCode(languageCode);
            } catch (Exception e) { }
        }
        else {
            Locale.setDefault(Locale.getDefault());
            config.setLanguageCode("");
        }
        
        config.setUnicodeNormalization(unicodeNormalizationCheckboxField.getChecked());
        
        try {
            config.setRetMsgCount(Integer.parseInt(messageCountEditField.getText()));
        } catch (Exception e) { }

        if (displayOrderChoiceField.getSelectedIndex() == 0) {
            config.setDispOrder(true);
        } else {
            config.setDispOrder(false);
        }

        config.setHideDeletedMsg(hideDeletedMessagesCheckboxField.getChecked());

        config.setWifiMode(wifiModeChoiceField.getSelectedIndex());

        String url = "file:///" + fileSystemRoots[localDataLocationChoiceLabel.getSelectedIndex()] + LOCAL_FILE_BASE;
        config.setLocalDataLocation(url);

        if (overrideHostnameCheckboxField.getChecked()) {
            config.setLocalHostname(localHostnameEditField.getText().trim());
        } else {
            config.setLocalHostname("");
        }

        config.setConnDebug(connectionDebuggingCheckboxField.getChecked());
        mailSettings.saveSettings();
    }
}
