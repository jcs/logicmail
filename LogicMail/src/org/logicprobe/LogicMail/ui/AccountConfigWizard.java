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

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.PlatformInfo;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.PopConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.NetworkAccountNode;

import net.rim.device.api.i18n.MessageFormat;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.ChoiceField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.RadioButtonGroup;
import net.rim.device.api.ui.component.RadioButtonField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.component.TextField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.text.TextFilter;
import net.rim.device.api.ui.Font;

public class AccountConfigWizard extends WizardController {
    private static final boolean hasIndicators = PlatformInfo.getInstance().hasApplicationIndicators();
    
    private static String IMAP = "IMAP";
    private static String IMAP_PORT = "143";
    private static String IMAP_PORT_SSL = "993";
    private static String POP = "POP";
    private static String POP_PORT = "110";
    private static String POP_PORT_SSL = "995";
    private static String SMTP = "SMTP";
    private static String SMTP_PORT = "587";
    private static String SMTP_PORT_SSL = "465";
    
    private int accountType = -1;
    private boolean outgoingCreated;

    private static final int ACCOUNT_TYPE_IMAP = 0;
    private static final int ACCOUNT_TYPE_POP  = 1;

    private static String[] PREFIX_INCOMING_SERVER = new String[] { "pop.", "pop3.", "imap." };
    private static String PREFIX_SMTP = "smtp";
    
    public AccountConfigWizard() {
        addWizardScreen(newAccountWizardScreen);
        addWizardScreen(incomingServerWizardScreen);
        addWizardScreen(outgoingServerWizardScreen);
        addWizardScreen(miscWizardScreen);
        addWizardScreen(transportWizardScreen);
        addWizardScreen(finalWizardScreen);
    }

    private IdentityConfig identityConfig;
    private AccountConfig accountConfig;
    private OutgoingConfig outgoingConfig;
    private boolean autoStartEnabled;
    private boolean notificationIconShown;

    // Data objects shared by multiple screens
    private String accountName;
    private String identityEmailAddress;
    private String incomingServerName;
    private String incomingServerUsername;
    private String incomingServerPass;

    private static Field createDescribedField(Field field, String label, String description) {
        VerticalFieldManager manager = new VerticalFieldManager(Field.USE_ALL_WIDTH | Manager.NO_VERTICAL_SCROLL);
        if(label != null) {
            LabelField labelField = new LabelField(label);
            manager.add(labelField);
        }
        if(!(field instanceof ChoiceField)) {
            FieldFactory.getInstance().addRoundedBorder(field);
        }
        manager.add(field);
        if(description != null) {
            LabelField descField = new LabelField(description) {
                protected void paint(Graphics graphics) {
                    graphics.setColor(Color.GRAY);
                    super.paint(graphics);
                };
            };
            descField.setFont(Font.getDefault().derive(Font.PLAIN, 6, Ui.UNITS_pt));
            manager.add(descField);
        }
        manager.add(BlankSeparatorField.createQuarterHeightSeparator());
        return manager;
    }
    
    private static Field createDescribedField(Field field, String label) {
        return createDescribedField(field, label, null);
    }
    
    private static boolean hasText(TextField field) {
        return field.getText().trim().length() > 0;
    }
    
    private WizardScreen newAccountWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_TITLE),
            WizardScreen.PAGE_FIRST) {

        private FieldChangeListener fieldChangeListener;
        private EditField accountNameEditField;
        private EditField identityNameEditField;
        private EmailAddressEditField emailAddressEditField;
        private RadioButtonGroup accountTypeGroup;
        private RadioButtonField imapAccountType;
        private RadioButtonField popAccountType;

        protected void initFields() {
            fieldChangeListener = new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    validateData();
                }
            };
            
            accountNameEditField = new EditField();
            accountNameEditField.setChangeListener(fieldChangeListener);
            add(createDescribedField(
                    accountNameEditField,
                    resources.getString(LogicMailResource.WIZARD_SCREEN_ACCOUNT_NAME_ACCOUNT_NAME)));
            
            identityNameEditField = new EditField();
            identityNameEditField.setChangeListener(fieldChangeListener);
            add(createDescribedField(
                    identityNameEditField,
                    resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_YOUR_NAME),
                    resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_YOUR_NAME_HELP)));
            
            emailAddressEditField = new EmailAddressEditField("", "");
            emailAddressEditField.setChangeListener(fieldChangeListener);
            add(createDescribedField(
                    emailAddressEditField,
                    resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_EMAIL_ADDRESS)));
            
            accountTypeGroup = new RadioButtonGroup();
            accountTypeGroup.setChangeListener(fieldChangeListener);
            imapAccountType = new RadioButtonField(IMAP, accountTypeGroup, false) {
                protected void layout(int width, int height) {
                    int temp = this.getPreferredWidth();
                    setExtent(temp, height);
                    super.layout(temp, height);                    
                };
            };
            popAccountType = new RadioButtonField(POP, accountTypeGroup, false) {
                protected void layout(int width, int height) {
                    int temp = this.getPreferredWidth();
                    setExtent(temp, height);
                    super.layout(temp, height);                    
                };
            };

            HorizontalFieldManager typeManager = new HorizontalFieldManager(Field.USE_ALL_WIDTH);
            typeManager.add(imapAccountType);
            typeManager.add(new LabelField("    ", Field.NON_FOCUSABLE));
            typeManager.add(popAccountType);
            add(createDescribedField(
                    typeManager,
                    resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_ACCOUNT_TYPE)));
            
            validateData();
        }
        
        private void validateData() {
            if(hasText(accountNameEditField)
                    && hasText(identityNameEditField)
                    && hasText(emailAddressEditField)
                    && accountTypeGroup.getSelectedIndex() != -1) {
                setInputValid(true);
            }
            else {
                setInputValid(false);
            }
        }
        
        protected void onPageFlip() {
            accountName = accountNameEditField.getText().trim();
            identityEmailAddress = emailAddressEditField.getText().trim();
            
            if(imapAccountType.isSelected()) {
                accountType = ACCOUNT_TYPE_IMAP;
            }
            else if(popAccountType.isSelected()) {
                accountType = ACCOUNT_TYPE_POP;
            }
        };
        
        public void gatherResults() {
            String fullName = identityNameEditField.getText().trim();
            String emailAddress = emailAddressEditField.getText().trim();
            String identityName = MessageFormat.format(
                    "{0} ({1})",
                    new Object[] { fullName, accountName });
            
            identityConfig.setIdentityName(identityName);
            identityConfig.setFullName(fullName);
            identityConfig.setEmailAddress(emailAddress);
        }
    };
    
    private WizardScreen incomingServerWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_INCOMING_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private FieldChangeListener fieldChangeListener;
        private Manager serverNameManager;
        private HostnameEditField serverNameEditField;
        private ObjectChoiceField securityChoiceField;
        private BasicEditField portEditField;
        private BasicEditField userEditField;
        private PasswordEditField passEditField;

        protected void initFields() {
            fieldChangeListener = new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    mailServerWizardScreen_fieldChanged(field, context);
                }
            };

            serverNameEditField = new HostnameEditField("", "");
            serverNameEditField.setChangeListener(fieldChangeListener);

            serverNameManager = (Manager)createDescribedField(
                    serverNameEditField,
                    "");
            add(serverNameManager);
            
            securityChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY),
                    new Object[] {
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_NEVER),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS_IF_AVAILABLE),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_SSL)},
                        ConnectionConfig.SECURITY_SSL);
            securityChoiceField.setChangeListener(fieldChangeListener);
            add(securityChoiceField);
            add(BlankSeparatorField.createQuarterHeightSeparator());
            
            portEditField = new BasicEditField("", IMAP_PORT_SSL);
            portEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
            portEditField.setChangeListener(fieldChangeListener);
            add(createDescribedField(
                    portEditField,
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT)));
            
            userEditField = new BasicEditField(TextField.NO_NEWLINE | TextField.NO_LEARNING);
            add(createDescribedField(
                    userEditField,
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME)));

            passEditField = new PasswordEditField();
            add(createDescribedField(
                    passEditField,
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD),
                    resources.getString(LogicMailResource.WIZARD_SCREEN_INCOMING_PASSWORD_HELP)));
            
            add(new NullField());
        }

        public void onPageEnter() {
            ((LabelField)serverNameManager.getField(0)).setText(
                    MessageFormat.format(
                            resources.getString(LogicMailResource.WIZARD_SCREEN_INCOMING_SERVER),
                            new Object[] {
                                (accountType == ACCOUNT_TYPE_IMAP) ? IMAP : POP
                            }
                    ));
            
            if (!hasText(serverNameEditField)) {
                serverNameEditField.setText(getDomainNameFromEmail());

                if (accountType == ACCOUNT_TYPE_POP) {
                    portEditField.setText(POP_PORT_SSL);
                } else if (accountType == ACCOUNT_TYPE_IMAP) {
                    portEditField.setText(IMAP_PORT_SSL);
                }
            }
            if (!hasText(userEditField)) {
                userEditField.setText(getUsernameFromEmail());
            }
            validateData();
        };

        private void mailServerWizardScreen_fieldChanged(Field field, int context) {
            if(field == securityChoiceField) {
                if(accountType == ACCOUNT_TYPE_POP) {
                    if(securityChoiceField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
                        portEditField.setText(POP_PORT_SSL);
                    }
                    else {
                        portEditField.setText(POP_PORT);
                    }
                }
                else if(accountType == ACCOUNT_TYPE_IMAP) {
                    if(securityChoiceField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
                        portEditField.setText(IMAP_PORT_SSL);
                    }
                    else {
                        portEditField.setText(IMAP_PORT);
                    }
                }
            }
            validateData();
        }

        private void validateData() {
            if (hasText(serverNameEditField)
                    && hasText(portEditField)) {
                setInputValid(true);
            } else {
                setInputValid(false);
            }
        }

        protected void onPageFlip() {
            incomingServerName = serverNameEditField.getText().trim();
            incomingServerUsername = userEditField.getText();
            incomingServerPass = passEditField.getText();
        }
        
        public void gatherResults() {
            accountConfig.setAcctName(accountName);
            accountConfig.setServerName(serverNameEditField.getText().trim());
            accountConfig.setServerSecurity(securityChoiceField.getSelectedIndex());
            accountConfig.setServerPort(Integer.parseInt(portEditField.getText().trim()));
            accountConfig.setTransportType(ConnectionConfig.TRANSPORT_GLOBAL);
            accountConfig.setServerUser(userEditField.getText().trim());
            accountConfig.setServerPass(passEditField.getText());
        }
    };
    
    private WizardScreen outgoingServerWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private FieldChangeListener fieldChangeListener;
        private HostnameEditField serverNameEditField;
        private ObjectChoiceField securityChoiceField;
        private BasicEditField portEditField;
        private ObjectChoiceField authChoiceField;
        private BasicEditField userEditField;
        private PasswordEditField passEditField;
        private Field userEditFieldManager;
        private Field passEditFieldManager;
        private String nameProvided;
        private String portProvided;
        private boolean authVisible;

        protected void initFields() {
            nameProvided = "";
            portProvided = "";
            fieldChangeListener = new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    outgoingServerWizardScreen_fieldChanged(field, context);
                }};
            
            serverNameEditField = new HostnameEditField("", "");
            serverNameEditField.setChangeListener(fieldChangeListener);
            add(createDescribedField(
                    serverNameEditField,
                    MessageFormat.format(
                            resources.getString(LogicMailResource.WIZARD_SCREEN_INCOMING_SERVER),
                            new Object[] { SMTP }
                    )));

            securityChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY),
                    new Object[] {
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_NEVER),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS_IF_AVAILABLE),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_SSL)},
                        ConnectionConfig.SECURITY_TLS);
            securityChoiceField.setChangeListener(fieldChangeListener);
            add(securityChoiceField);
            add(BlankSeparatorField.createQuarterHeightSeparator());

            portEditField = new BasicEditField("", Integer.toString(587));
            portEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
            portEditField.setChangeListener(fieldChangeListener);
            add(createDescribedField(
                    portEditField,
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT)));

            String authTypes[] = {
                    resources.getString(LogicMailResource.MENUITEM_NONE),
                    "PLAIN", "LOGIN", "CRAM-MD5" };
            authChoiceField = new ObjectChoiceField(
                        resources.getString(LogicMailResource.CONFIG_OUTGOING_AUTHENTICATION),
                        authTypes, 1);
            authChoiceField.setChangeListener(fieldChangeListener);
            add(authChoiceField);
            add(BlankSeparatorField.createQuarterHeightSeparator());
            
            userEditField = new BasicEditField(TextField.NO_NEWLINE | TextField.NO_LEARNING);
            userEditFieldManager = createDescribedField(
                    userEditField,
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME));
            add(userEditFieldManager);

            passEditField = new PasswordEditField();
            passEditFieldManager = createDescribedField(
                    passEditField,
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD),
                    resources.getString(LogicMailResource.WIZARD_SCREEN_INCOMING_PASSWORD_HELP));
            add(passEditFieldManager);
            authVisible = true;
        }

        private void outgoingServerWizardScreen_fieldChanged(Field field, int context) {
            if(field == securityChoiceField) {
                if(securityChoiceField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
                    portEditField.setText(SMTP_PORT_SSL);
                }
                else {
                    portEditField.setText(SMTP_PORT);
                }
            }
            else if (field == authChoiceField) {
                updateAuthChoiceFields();
            }
            
            validateData();
        }
        
        private void updateAuthChoiceFields() {
            if (authChoiceField.getSelectedIndex() == 0) {
                if(authVisible) {
                    delete(userEditFieldManager);
                    delete(passEditFieldManager);
                    authVisible = false;
                }
            } else {
                if(!authVisible) {
                    add(userEditFieldManager);
                    add(passEditFieldManager);
                    authVisible = true;
                }
            }
            
            if(authVisible) {
                if(!hasText(userEditField)) {
                    userEditField.setText(incomingServerUsername);
                }
                if(!hasText(passEditField)) {
                    passEditField.setText(incomingServerPass);
                }
            }
        }

        private void validateData() {
            if(hasText(serverNameEditField)) {
                setInputValid(hasText(portEditField));
            }
            else {
                setInputValid(true);
            }
        }
        
        protected boolean confirmPageFlip() {
            if(hasText(serverNameEditField)) {
                return true;
            }
            else {
                int result = Dialog.ask(
                        Dialog.D_YES_NO,
                        resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_EMPTY_CONFIRM),
                        Dialog.NO);
                return result == Dialog.YES;
            }
        };
        
        public void onPageEnter() {
            populateOutgoingItems();
            updateAuthChoiceFields();
            validateData();
            serverNameEditField.setFocus();
        };
        
        private void populateOutgoingItems() {
            serverNameEditField.setText(nameProvided.equals("") ? getOutgoingServerFromIncoming() : nameProvided);
            portEditField.setText(portProvided.equals("") ? 
                    (securityChoiceField.getSelectedIndex() == securityChoiceField.getSize() - 1 ? SMTP_PORT_SSL : SMTP_PORT) : 
                        portProvided);

            serverNameEditField.setEditable(true);
            securityChoiceField.setEditable(true);
            portEditField.setEditable(true);
        }
        protected void onPageFlip() {
            nameProvided = serverNameEditField.getText().trim();
            portProvided = portEditField.getText();
        }
        public void gatherResults() {
            if(hasText(serverNameEditField)) {
                outgoingConfig.setAcctName(accountName);
                outgoingConfig.setServerName(serverNameEditField.getText().trim());
                outgoingConfig.setServerSecurity(securityChoiceField.getSelectedIndex());
                outgoingConfig.setServerPort(Integer.parseInt(portEditField.getText()));
                outgoingConfig.setTransportType(ConnectionConfig.TRANSPORT_GLOBAL);

                int index = authChoiceField.getSelectedIndex();
                outgoingConfig.setUseAuth(index);
                if(index > 0) {
                    outgoingConfig.setServerUser(userEditField.getText().trim());
                    outgoingConfig.setServerPass(passEditField.getText());
                }
                else {
                    outgoingConfig.setServerUser("");
                    outgoingConfig.setServerPass("");
                }

                outgoingCreated = true;
            }
            else {
                outgoingCreated = false;
            }
        }
    };

    private WizardScreen miscWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_MISC_TITLE),
            WizardScreen.PAGE_NORMAL) {

        private CheckboxField autoStartupCheckboxField;
        private CheckboxField notificationIconCheckboxField;
        private ObjectChoiceField refreshOnStartupChoiceField;
        private ObjectChoiceField refreshFrequencyChoiceField;
        
        protected void initFields() {
            GlobalConfig globalConfig = MailSettings.getInstance().getGlobalConfig();
            
            refreshOnStartupChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_REFRESH_ON_STARTUP),
                    new Object[] {
                        resources.getString(LogicMailResource.MENUITEM_NEVER),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_REFRESH_STATUS),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_REFRESH_HEADERS)
                    }, 0);
            add(refreshOnStartupChoiceField);
            add(BlankSeparatorField.createHalfHeightSeparator());
            
            String minutePattern = resources.getString(LogicMailResource.CONFIG_ACCOUNT_REFRESH_FREQUENCY_MINUTES);
            refreshFrequencyChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_REFRESH_FREQUENCY),
                    new Object[] {
                        resources.getString(LogicMailResource.MENUITEM_NEVER),
                        MessageFormat.format(minutePattern, new Object[] { new Integer(5) }),
                        MessageFormat.format(minutePattern, new Object[] { new Integer(10) }),
                        MessageFormat.format(minutePattern, new Object[] { new Integer(15) }),
                        MessageFormat.format(minutePattern, new Object[] { new Integer(30) }),
                    },
                    0);
            add(refreshFrequencyChoiceField);
            
            add(BlankSeparatorField.createHalfHeightSeparator());
            add(new SeparatorField());
            add(BlankSeparatorField.createHalfHeightSeparator());

            autoStartupCheckboxField = new CheckboxField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_AUTO_STARTUP),
                    globalConfig.isAutoStartupEnabled());
            add(autoStartupCheckboxField);
            add(BlankSeparatorField.createHalfHeightSeparator());
            
            if(hasIndicators) {
                notificationIconCheckboxField = new CheckboxField(
                        resources.getString(LogicMailResource.CONFIG_GLOBAL_SHOW_NOTIFICATION_ICON),
                        globalConfig.isNotificationIconShown());
                add(notificationIconCheckboxField);
                add(BlankSeparatorField.createHalfHeightSeparator());
            }
            setInputValid(true);
        }
        
        public void gatherResults() {
            accountConfig.setRefreshOnStartup(refreshOnStartupChoiceField.getSelectedIndex());
            
            int refreshFrequency;
            switch(refreshFrequencyChoiceField.getSelectedIndex()) {
            case 1:
                refreshFrequency = 5;
                break;
            case 2:
                refreshFrequency = 10;
                break;
            case 3:
                refreshFrequency = 15;
                break;
            case 4:
                refreshFrequency = 30;
                break;
            default:
                refreshFrequency = 0;
            }
            accountConfig.setRefreshFrequency(refreshFrequency);
            
            autoStartEnabled = autoStartupCheckboxField.getChecked();
            if(hasIndicators) {
                notificationIconShown = notificationIconCheckboxField.getChecked();
            }
        };
    };
    
    private WizardScreen transportWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_TRANSPORT_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private CheckboxField defaultCheckboxField;
        private RadioButtonGroup connectionGroup;
        private RadioButtonField connectionAuto;
        private RadioButtonField connectionDirectTCP;
        private RadioButtonField connectionMDS;
        private RadioButtonField connectionWAP2;
        private RadioButtonField connectionWiFiOnly;
        private CheckboxField enableWiFiCheckboxField;
        
        protected void initFields() {
            defaultCheckboxField = new CheckboxField(
                    resources.getString(LogicMailResource.WIZARD_SCREEN_TRANSPORT_USE_DEFAULT),
                    true);
            defaultCheckboxField.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    if(defaultCheckboxField.getChecked()) {
                        setUseGlobalDefaults();
                    }
                    else {
                        setUseCustomSettings();
                    }
                }
            });
            
            connectionGroup = new RadioButtonGroup();
            connectionAuto = new RadioButtonField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_AUTO),
                    connectionGroup, true);
            connectionDirectTCP = new RadioButtonField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_DIRECT_TCP),
                    connectionGroup, false);
            connectionMDS = new RadioButtonField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_MDS),
                    connectionGroup, false);
            connectionWAP2 = new RadioButtonField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_WAP2),
                    connectionGroup, false);
            connectionWiFiOnly = new RadioButtonField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_TRANSPORT_WIFI_ONLY),
                    connectionGroup, false);
            
            enableWiFiCheckboxField = new CheckboxField(
                    resources.getString(LogicMailResource.CONFIG_GLOBAL_ENABLE_WIFI),
                    true);
            
            add(defaultCheckboxField);
            add(connectionAuto);
            add(connectionDirectTCP);
            add(connectionMDS);
            add(connectionWAP2);
            add(connectionWiFiOnly);
            add(enableWiFiCheckboxField);
            setUseGlobalDefaults();
            
            // This screen always has valid input
            setInputValid(true);
        }
        private void setUseGlobalDefaults() {
            GlobalConfig globalConfig = MailSettings.getInstance().getGlobalConfig();
            switch(globalConfig.getTransportType()) {
            case ConnectionConfig.TRANSPORT_AUTO:
                connectionGroup.setSelectedIndex(0);
                break;
            case ConnectionConfig.TRANSPORT_DIRECT_TCP:
                connectionGroup.setSelectedIndex(1);
                break;
            case ConnectionConfig.TRANSPORT_MDS:
                connectionGroup.setSelectedIndex(2);
                break;
            case ConnectionConfig.TRANSPORT_WAP2:
                connectionGroup.setSelectedIndex(3);
                break;
            case ConnectionConfig.TRANSPORT_WIFI_ONLY:
                connectionGroup.setSelectedIndex(4);
                break;
            default:
                connectionGroup.setSelectedIndex(0);
                break;
            }
            enableWiFiCheckboxField.setChecked(globalConfig.getEnableWiFi());
            
            connectionAuto.setEditable(false);
            connectionDirectTCP.setEditable(false);
            connectionMDS.setEditable(false);
            connectionWAP2.setEditable(false);
            connectionWiFiOnly.setEditable(false);
            enableWiFiCheckboxField.setEditable(false);
        }
        
        private void setUseCustomSettings() {
            connectionGroup.setSelectedIndex(0);
            connectionAuto.setEditable(true);
            connectionDirectTCP.setEditable(true);
            connectionMDS.setEditable(true);
            connectionWAP2.setEditable(true);
            connectionWiFiOnly.setEditable(true);
            enableWiFiCheckboxField.setEditable(true);
            enableWiFiCheckboxField.setChecked(true);
        }
        
        public void gatherResults() {
            if(defaultCheckboxField.getChecked()) {
                accountConfig.setTransportType(ConnectionConfig.TRANSPORT_GLOBAL);
                if(outgoingCreated) { outgoingConfig.setTransportType(ConnectionConfig.TRANSPORT_GLOBAL); }
            }
            else {
                int transport;
                switch(connectionGroup.getSelectedIndex()) {
                case 0:
                    transport = ConnectionConfig.TRANSPORT_AUTO;
                    break;
                case 1:
                    transport = ConnectionConfig.TRANSPORT_DIRECT_TCP;
                    break;
                case 2:
                    transport = ConnectionConfig.TRANSPORT_MDS;
                    break;
                case 3:
                    transport = ConnectionConfig.TRANSPORT_WAP2;
                    break;
                case 4:
                    transport = ConnectionConfig.TRANSPORT_WIFI_ONLY;
                    break;
                default:
                    transport = ConnectionConfig.TRANSPORT_AUTO;
                    break;
                }
                
                accountConfig.setTransportType(transport);
                accountConfig.setEnableWiFi(enableWiFiCheckboxField.getChecked());
                if(outgoingCreated) {
                    outgoingConfig.setTransportType(transport);
                    outgoingConfig.setEnableWiFi(enableWiFiCheckboxField.getChecked());
                }
            }
        };
    };
    private WizardScreen finalWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_TITLE),
            WizardScreen.PAGE_LAST) {
        private ButtonField testButton;
        private AccountConfigTester accountTester;
        private VerticalFieldManager testOutputManager;

        protected void initFields() {
            testButton = new ButtonField(
                    resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_TEST_CONFIGURATION),
                    Field.FIELD_HCENTER | ButtonField.CONSUME_CLICK);
            testButton.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    testButtonFieldChanged();
                }});
            accountTester = new AccountConfigTester();
            testOutputManager = accountTester.getTestOutputManager();
            testOutputManager.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    if(context == AccountConfigTester.TEST_COMPLETE) {
                        testComplete();
                    }
                }
            });

            add(testButton);
            add(BlankSeparatorField.createHalfHeightSeparator());
            add(testOutputManager);
            setInputValid(true);
        }

        public void onPageEnter() {
            // Build the empty configuration objects
            identityConfig = new IdentityConfig();
            if(accountType == ACCOUNT_TYPE_IMAP) {
                accountConfig = new ImapConfig();
            }
            else if(accountType == ACCOUNT_TYPE_POP) {
                accountConfig = new PopConfig();
            }
            outgoingConfig = new OutgoingConfig();

            // Go through all the previous pages and gather the configuration data
            AccountConfigWizard.this.gatherResults();
            accountConfig.setIdentityConfig(identityConfig);
            accountConfig.setOutgoingConfig(outgoingConfig);
        }
        
        private void testButtonFieldChanged() {
            testButton.setEditable(false);
            setInputValid(false);
            accountTester.runConnectionTest(accountConfig);
        }

        public boolean onClose() {
            if(super.onClose()) {
                if(accountTester.isTestInProgress()) {
                    accountTester.setTestCanceled();
                }
                return true;
            }
            else {
                return false;
            }
        };
        
        private void testComplete() {
            testButton.setEditable(true);
            setInputValid(true);
        }
    };

    protected void onWizardFinished() {
        // Add the configuration that was just built
        MailSettings mailSettings = MailSettings.getInstance();
        mailSettings.addIdentityConfig(identityConfig);
        if(outgoingCreated) { mailSettings.addOutgoingConfig(outgoingConfig); }
        mailSettings.addAccountConfig(accountConfig);
        
        MailRootNode rootNode = MailManager.getInstance().getMailRootNode();
        
        // Attempt to find local Sent/Drafts folders, and associate them
        MailboxNode[] localMailboxes = rootNode.getLocalAccount().getRootMailbox().getMailboxes();
        for(int i=0; i<localMailboxes.length; i++) {
            if(localMailboxes[i].getType() == MailboxNode.TYPE_SENT) {
                accountConfig.setSentMailbox(localMailboxes[i]);
            }
            else if(localMailboxes[i].getType() == MailboxNode.TYPE_DRAFTS) {
                accountConfig.setDraftMailbox(localMailboxes[i]);
            }
        }

        // Set the global options that may have been changed
        GlobalConfig globalConfig = mailSettings.getGlobalConfig();
        globalConfig.setAutoStartupEnabled(autoStartEnabled);
        if(hasIndicators) {
            globalConfig.setNotificationIconShown(notificationIconShown);
        }
        
        // Save the new configuration
        mailSettings.saveSettings();
        
        // Find the newly created account, and trigger a folder refresh (if applicable)
        NetworkAccountNode newAccount = rootNode.findAccountForConfig(accountConfig);
        if(newAccount != null && newAccount.hasFolders()) {
            newAccount.refreshMailboxes();
        }
    }

    /**
     * Gets the account configuration that was just created.
     * 
     * @return Account configuration
     */
    public AccountConfig getAccountConfig() {
        return this.accountConfig;
    }

    private String getDomainNameFromEmail() {
        String emailDomain = identityEmailAddress.substring(identityEmailAddress.indexOf("@") + 1);
        return emailDomain;
    }

    private String getOutgoingServerFromIncoming() {
        String result = incomingServerName;
        for (int i = 0; i < PREFIX_INCOMING_SERVER.length; i++) {
            if (incomingServerName.startsWith(PREFIX_INCOMING_SERVER[i])) {
                result = (new StringBuffer(PREFIX_SMTP).append(incomingServerName.substring(PREFIX_INCOMING_SERVER[i]
                        .length() - 1))).toString();
                return result;
            }
        }
        return result;
    }

    private String getUsernameFromEmail() {
        try {
            String userName = identityEmailAddress.substring(0, identityEmailAddress.indexOf("@"));
            return userName;
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }    
}
