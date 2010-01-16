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

import net.rim.device.api.i18n.Locale;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.TextField;
import net.rim.device.api.ui.text.TextFilter;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.PlatformInfo;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;

/**
 * Global Configuration screen
 */
public class GlobalConfigScreen extends AbstractConfigScreen implements FieldChangeListener {
    private MailSettings mailSettings;
    private GlobalConfig existingConfig;
    private String localHostname;
    private String[] languageChoices;
    private String[] languageCodes;
    private String[] fileSystemRoots;
    private String[] fileSystemRootChoices;
    private int selectedFileSystemRootIndex;

    private ObjectChoiceField languageChoiceField;
    private CheckboxField unicodeNormalizationCheckboxField;

    private ObjectChoiceField messageDisplayChoiceField;
    private BasicEditField messageCountEditField;
    private ObjectChoiceField displayOrderChoiceField;
    private CheckboxField hideDeletedMessagesCheckboxField;
    private ObjectChoiceField networkTransportChoiceField;
    private CheckboxField enableWiFiCheckboxField;
    private ObjectChoiceField localDataLocationChoiceLabel;
    private CheckboxField connectionDebuggingCheckboxField;
    private CheckboxField overrideHostnameCheckboxField;
    private BasicEditField localHostnameEditField;

    public GlobalConfigScreen() {
        super(resources.getString(LogicMailResource.APPNAME) + " - " +
                resources.getString(LogicMailResource.CONFIG_GLOBAL_TITLE));

        mailSettings = MailSettings.getInstance();
        existingConfig = mailSettings.getGlobalConfig();
        localHostname = existingConfig.getLocalHostname();
        languageChoices = new String[] {
                "BlackBerry",   // System default
                "Dansk",        // Danish: da
                "Deutsch",      // German: de
                "English",      // English: en
                "Espa\u00f1ol", // Spanish: es
                "Fran\u00e7ais",// French: fr
                "Italiano",     // Italian: it
                "Nederlands",   // Dutch: nl
                "Ti\u00ea\u0301ng Vi\u00ea\u0323t", // Vietnamese: vi
                "\u4E2D\u6587", // Chinese: zh
        };
        languageCodes = new String[] {
                "",   // System default
                "da", // Danish
                "de", // German
                "en", // English
                "es", // Spanish
                "fr", // French
                "it", // Italian
                "nl", // Dutch
                "vi", // Vietnamese
                "zh", // Chinese
        };

        // Populate fileSystemRoots with a list of all
        // available and writable storage devices
        String selectedFileSystemRoot = existingConfig.getLocalDataLocation();
        selectedFileSystemRootIndex = 0;
        
        fileSystemRoots = PlatformInfo.getInstance().getFilesystemRoots();
        fileSystemRootChoices = new String[fileSystemRoots.length + 1];
        for(int i=0; i<fileSystemRoots.length; i++) {
            String root = fileSystemRoots[i];
            if(selectedFileSystemRoot != null && selectedFileSystemRoot.indexOf(root) != -1) {
                selectedFileSystemRootIndex = i;
            }
            if(root.indexOf("Card/") != -1) {
                fileSystemRootChoices[i] =
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_MEDIA_CARD);
            }
            else if(root.indexOf("store/") != -1) {
                fileSystemRootChoices[i] =
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_DEVICE_MEMORY);
            }
            else {
                int p = root.indexOf('/', GlobalConfig.FILE_URL_PREFIX.length() - 1);
                int q = root.indexOf('/', p + 1);
                if(p != -1 && q != -1 && p < q) {
                    fileSystemRootChoices[i] = root.substring(p + 1, q);
                }
                else {
                    fileSystemRootChoices[i] = root;
                }
            }
        }
        fileSystemRootChoices[fileSystemRootChoices.length - 1] =
            resources.getString(LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_DISABLED);
        if(selectedFileSystemRoot == null) {
            selectedFileSystemRootIndex = fileSystemRootChoices.length - 1;
        }
        
        initFields();
    }

    private void initFields() {
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
                resources.getString(LogicMailResource.CONFIG_GLOBAL_LANGUAGE),
                languageChoices,
                languageIndex);

        unicodeNormalizationCheckboxField = new CheckboxField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_UNICODE_NORMALIZATION),
                existingConfig.getUnicodeNormalization());

        messageDisplayChoiceField = new ObjectChoiceField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_FORMAT),
                new String[] {
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_FORMAT_PLAIN_TEXT),
                "HTML" },
                existingConfig.getMessageDisplayFormat());

        messageCountEditField = new BasicEditField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_COUNT) + ' ',
                Integer.toString(existingConfig.getRetMsgCount()), 16, TextField.NO_NEWLINE);
        messageCountEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));

        String[] orderTypes = {
                resources.getString(LogicMailResource.MENUITEM_ORDER_ASCENDING),
                resources.getString(LogicMailResource.MENUITEM_ORDER_DESCENDING)
        };

        if (existingConfig.getDispOrder()) {
            displayOrderChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_ORDER) + ' ',
                    orderTypes, 0);
        } else {
            displayOrderChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_MESSAGE_ORDER) + ' ',
                    orderTypes, 1);
        }

        hideDeletedMessagesCheckboxField = new CheckboxField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_HIDE_DELETED_MESSAGES),
                existingConfig.getHideDeletedMsg());


        String[] transportChoices = {
                resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_AUTO),
                resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_DIRECT_TCP),
                resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_MDS),
                resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_WAP2)
        };
        networkTransportChoiceField = new ObjectChoiceField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_NETWORK_TRANSPORT),
                transportChoices,
                getTransportChoice(existingConfig.getTransportType()));
        
        enableWiFiCheckboxField = new CheckboxField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_ENABLE_WIFI),
                existingConfig.getEnableWiFi());
        
        localDataLocationChoiceLabel = new ObjectChoiceField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_LOCATION) + ' ',
                fileSystemRootChoices,
                selectedFileSystemRootIndex);

        boolean overrideHostname = localHostname.length() > 0;
        overrideHostnameCheckboxField = new CheckboxField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_OVERRIDE_HOSTNAME),
                overrideHostname);
        overrideHostnameCheckboxField.setChangeListener(this);

        if (overrideHostname) {
            localHostnameEditField = new BasicEditField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_HOSTNAME) + ' ',
                    localHostname, 256, TextField.NO_NEWLINE);
        } else {
            String hostname = System.getProperty("microedition.hostname");
            localHostnameEditField = new HostnameEditField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_HOSTNAME) + ' ',
                    ((hostname != null) ? hostname : "localhost"));
            localHostnameEditField.setEditable(false);
        }

        connectionDebuggingCheckboxField = new CheckboxField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_CONNECTION_DEBUGGING),
                existingConfig.getConnDebug());

        add(new LabeledSeparatorField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_SECTION_MESSAGE_DISPLAY),
                Field.NON_FOCUSABLE | LabeledSeparatorField.BOTTOM_BORDER));
        add(messageDisplayChoiceField);
        add(messageCountEditField);
        add(displayOrderChoiceField);
        add(hideDeletedMessagesCheckboxField);

        add(new LabeledSeparatorField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_SECTION_NETWORKING),
                Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        add(networkTransportChoiceField);
        add(enableWiFiCheckboxField);
        add(overrideHostnameCheckboxField);
        add(localHostnameEditField);

        add(new LabeledSeparatorField(
                resources.getString(LogicMailResource.CONFIG_GLOBAL_SECTION_OTHER),
                Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        add(localDataLocationChoiceLabel);
        add(languageChoiceField);
        add(unicodeNormalizationCheckboxField);
        add(connectionDebuggingCheckboxField);
    }

    private static int getTransportChoice(int transportSetting) {
        int result;
        switch(transportSetting) {
        case ConnectionConfig.TRANSPORT_AUTO:
            result = 0;
            break;
        case ConnectionConfig.TRANSPORT_DIRECT_TCP:
            result = 1;
            break;
        case ConnectionConfig.TRANSPORT_MDS:
            result = 2;
            break;
        case ConnectionConfig.TRANSPORT_WAP2:
            result = 3;
            break;
        default:
            result = 0;
            break;
        }
        return result;
    }
    
    private static int getTransportSetting(int transportChoice) {
        int result;
        switch(transportChoice) {
        case 0:
            result = ConnectionConfig.TRANSPORT_AUTO;
            break;
        case 1:
            result = ConnectionConfig.TRANSPORT_DIRECT_TCP;
            break;
        case 2:
            result = ConnectionConfig.TRANSPORT_MDS;
            break;
        case 3:
            result = ConnectionConfig.TRANSPORT_WAP2;
            break;
        default:
            result = ConnectionConfig.TRANSPORT_AUTO;
            break;
        }
        return result;
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

        config.setMessageDisplayFormat(messageDisplayChoiceField.getSelectedIndex());

        try {
            config.setRetMsgCount(Integer.parseInt(messageCountEditField.getText()));
        } catch (Exception e) { }

        if (displayOrderChoiceField.getSelectedIndex() == 0) {
            config.setDispOrder(true);
        } else {
            config.setDispOrder(false);
        }

        config.setHideDeletedMsg(hideDeletedMessagesCheckboxField.getChecked());

        config.setTransportType(getTransportSetting(networkTransportChoiceField.getSelectedIndex()));
        
        config.setEnableWiFi(enableWiFiCheckboxField.getChecked());

        int fsRootIndex = localDataLocationChoiceLabel.getSelectedIndex();
        String url;
        if(fsRootIndex > fileSystemRoots.length) {
            url = null;
        }
        else {
            url = fileSystemRoots[fsRootIndex];
        }
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
