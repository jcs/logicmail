package org.logicprobe.LogicMail.ui;

import java.io.IOException;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.PopConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.mail.MailClient;
import org.logicprobe.LogicMail.mail.MailClientFactory;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.NetworkAccountNode;
import org.logicprobe.LogicMail.util.WrappedIOException;

import net.rim.device.api.i18n.MessageFormat;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.RadioButtonGroup;
import net.rim.device.api.ui.component.RadioButtonField;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.text.TextFilter;
import net.rim.device.api.ui.Font;

public class AccountConfigWizard extends WizardController {
    private int accountType = -1;
    private boolean identityCreated;
    private boolean outgoingCreated;

    private final static int ACCOUNT_TYPE_IMAP = 0;
    private final static int ACCOUNT_TYPE_POP  = 1;

    public AccountConfigWizard() {
        addWizardScreen(newAccountWizardScreen);
        addWizardScreen(accountTypeWizardScreen);
        addWizardScreen(accountNameWizardScreen);
        addWizardScreen(mailServerWizardScreen);
        addWizardScreen(authenticationWizardScreen);
        addWizardScreen(outgoingServerWizardScreen);
        addWizardScreen(outgoingAuthenticationWizardScreen);
        addWizardScreen(transportWizardScreen);
        addWizardScreen(finalWizardScreen);
    }

    private IdentityConfig identityConfig;
    private AccountConfig accountConfig;
    private OutgoingConfig outgoingConfig;

    private WizardScreen newAccountWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_TITLE),
            WizardScreen.PAGE_FIRST) {
        private FieldChangeListener fieldChangeListener;
        private ObjectChoiceField identityChoiceField;
        private EditField identityNameEditField;
        private EditField nameEditField;
        private EmailAddressEditField emailAddressEditField;
        private RichTextField commentsField;

        protected void initFields() {
            // Populate the list of existing identities
            MailSettings mailSettings = MailSettings.getInstance();
            int numIdentities = mailSettings.getNumIdentities();
            Object[] identityChoices = new Object[numIdentities + 1];
            for(int i=0; i<numIdentities; i++) {
                identityChoices[i] = mailSettings.getIdentityConfig(i);
            }
            identityChoices[identityChoices.length - 1] = resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_CREATE_NEW_IDENTITY);

            fieldChangeListener = new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    newAccountWizardScreen_fieldChanged(field, context);
                }};

            identityChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_SELECT_IDENTITY),
                    identityChoices, 0);
            identityChoiceField.setChangeListener(fieldChangeListener);

            identityNameEditField = new EditField(resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_IDENTITY) + ' ', "");
            identityNameEditField.setChangeListener(fieldChangeListener);

            nameEditField = new EditField(resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_YOUR_NAME) + ' ', "");
            nameEditField.setChangeListener(fieldChangeListener);

            emailAddressEditField = new EmailAddressEditField(resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_EMAIL_ADDRESS) + ' ', "");
            emailAddressEditField.setChangeListener(fieldChangeListener);

            commentsField = new RichTextField(resources.getString(LogicMailResource.WIZARD_SCREEN_NEW_ACCOUNT_COMMENTS));
            commentsField.setFont(this.getFont().derive(Font.ITALIC));

            add(identityChoiceField);
            add(identityNameEditField);
            add(nameEditField);
            add(emailAddressEditField);
            add(BlankSeparatorField.createHalfHeightSeparator());
            add(commentsField);

            populateIdentityItems();
            validateData();
            
            if(identityChoiceField.getSize() < 2) {
                identityChoiceField.setEditable(false);
            }
            identityNameEditField.setFocus();
        }

        private void newAccountWizardScreen_fieldChanged(Field field, int context) {
            if(field == identityChoiceField) {
                populateIdentityItems();
            }
            validateData();
        }

        private void validateData() {
            if(identityNameEditField.getText().trim().length() > 0 &&
                    nameEditField.getText().trim().length() > 0 &&
                    emailAddressEditField.getText().trim().length() > 0) {
                setInputValid(true);
            }
            else {
                setInputValid(false);
            }
        }

        private void populateIdentityItems() {
            int index = identityChoiceField.getSelectedIndex();
            Object item = identityChoiceField.getChoice(index);
            if(item instanceof IdentityConfig) {
                IdentityConfig identityConfig = (IdentityConfig)item;
                identityNameEditField.setText(identityConfig.getIdentityName());
                nameEditField.setText(identityConfig.getFullName());
                emailAddressEditField.setText(identityConfig.getEmailAddress());

                identityNameEditField.setEditable(false);
                nameEditField.setEditable(false);
                emailAddressEditField.setEditable(false);
            }
            else {
                if(index == 0) {
                    identityNameEditField.setText(IdentityConfig.getDefaultName());
                }
                else {
                    identityNameEditField.setText("");
                }
                nameEditField.setText("");
                emailAddressEditField.setText("");

                identityNameEditField.setEditable(true);
                nameEditField.setEditable(true);
                emailAddressEditField.setEditable(true);
            }
        }

        public void gatherResults() {
            int index = identityChoiceField.getSelectedIndex();
            Object item = identityChoiceField.getChoice(index);
            if(item instanceof IdentityConfig) {
                identityConfig = (IdentityConfig)item;
                identityCreated = false;
            }
            else {
                identityConfig.setIdentityName(identityNameEditField.getText());
                identityConfig.setFullName(nameEditField.getText());
                identityConfig.setEmailAddress(emailAddressEditField.getText());
                identityCreated = true;
            }
        }
    };
    private WizardScreen accountTypeWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_ACCOUNT_TYPE_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private LabelField descriptionLabelField;
        private RadioButtonGroup accountTypeGroup;
        private RadioButtonField imapAccountType;
        private RadioButtonField popAccountType;

        protected void initFields() {
            descriptionLabelField = new LabelField(resources.getString(LogicMailResource.WIZARD_SCREEN_ACCOUNT_TYPE_QUESTION));
            accountTypeGroup = new RadioButtonGroup();
            accountTypeGroup.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    setInputValid(accountTypeGroup.getSelectedIndex() != -1);
                }
            });
            imapAccountType = new RadioButtonField("IMAP", accountTypeGroup, false);
            popAccountType = new RadioButtonField("POP", accountTypeGroup, false);

            add(descriptionLabelField);
            add(imapAccountType);
            add(popAccountType);
            setInputValid(false);
        }

        protected void onPageFlip() {
            if(imapAccountType.isSelected()) {
                accountType = ACCOUNT_TYPE_IMAP;
            }
            else if(popAccountType.isSelected()) {
                accountType = ACCOUNT_TYPE_POP;
            }
        }

        public void gatherResults() {
            // Nothing to gather here, since the account type is
            // already noted in onPageFlip().
        }
    };
    private WizardScreen accountNameWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_ACCOUNT_NAME_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private LabelField descriptionLabelField;
        private EditField nameEditField;

        protected void initFields() {
            descriptionLabelField = new LabelField(resources.getString(LogicMailResource.WIZARD_SCREEN_ACCOUNT_NAME_QUESTION));
            nameEditField = new EditField(resources.getString(LogicMailResource.WIZARD_SCREEN_ACCOUNT_NAME_ACCOUNT_NAME) + ' ', "");
            nameEditField.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    setInputValid(nameEditField.getText().trim().length() > 0);
                }
            });

            add(descriptionLabelField);
            add(nameEditField);
        }
        public void gatherResults() {
            accountConfig.setAcctName(nameEditField.getText());
        }
    };
    private WizardScreen mailServerWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_MAIL_SERVER_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private FieldChangeListener fieldChangeListener;
        private LabelField descriptionLabelField;
        private HostnameEditField nameEditField;
        private ObjectChoiceField securityChoiceField;
        private BasicEditField portEditField;

        protected void initFields() {
            fieldChangeListener = new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    mailServerWizardScreen_fieldChanged(field, context);
                }};

            descriptionLabelField = new LabelField(resources.getString(LogicMailResource.WIZARD_SCREEN_MAIL_SERVER_QUESTION));

            nameEditField = new HostnameEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_SERVER) + ' ', "");
            nameEditField.setChangeListener(fieldChangeListener);

            securityChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY),
                    new Object[] {
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_NEVER),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS_IF_AVAILABLE),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_SSL)},
                        ConnectionConfig.SECURITY_NONE);
            securityChoiceField.setChangeListener(fieldChangeListener);

            portEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT) + ' ', "143");
            portEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
            portEditField.setChangeListener(fieldChangeListener);

            add(descriptionLabelField);
            add(nameEditField);
            add(securityChoiceField);
            add(portEditField);
        }

        public void onPageEnter() {
            if(accountType == ACCOUNT_TYPE_POP) {
                portEditField.setText("110");
            }
            else if(accountType == ACCOUNT_TYPE_IMAP) {
                portEditField.setText("143");
            }
        };
        
        private void mailServerWizardScreen_fieldChanged(Field field, int context) {
            if(field == securityChoiceField) {
                if(accountType == ACCOUNT_TYPE_POP) {
                    if(securityChoiceField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
                        portEditField.setText("995");
                    }
                    else {
                        portEditField.setText("110");
                    }
                }
                else if(accountType == ACCOUNT_TYPE_IMAP) {
                    if(securityChoiceField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
                        portEditField.setText("993");
                    }
                    else {
                        portEditField.setText("143");
                    }
                }
            }

            if(nameEditField.getText().trim().length() > 0 && portEditField.getText().trim().length() > 0) {
                setInputValid(true);
            }
            else {
                setInputValid(false);
            }
        }
        public void gatherResults() {
            accountConfig.setServerName(nameEditField.getText());
            accountConfig.setServerSecurity(securityChoiceField.getSelectedIndex());
            accountConfig.setServerPort(Integer.parseInt(portEditField.getText()));
            accountConfig.setTransportType(ConnectionConfig.TRANSPORT_GLOBAL);
        }
    };
    private WizardScreen authenticationWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_AUTHENTICATION_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private LabelField descriptionLabelField;
        private BasicEditField userEditField;
        private PasswordEditField passEditField;
        private RichTextField commentsField;

        protected void initFields() {
            descriptionLabelField = new LabelField(resources.getString(LogicMailResource.WIZARD_SCREEN_AUTHENTICATION_QUESTION));
            userEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME) + ' ', "");

            passEditField = new PasswordEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD) + ' ', "");

            commentsField = new RichTextField(resources.getString(LogicMailResource.WIZARD_SCREEN_AUTHENTICATION_COMMENTS));
            commentsField.setFont(this.getFont().derive(Font.ITALIC));

            add(descriptionLabelField);
            add(userEditField);
            add(passEditField);
            add(BlankSeparatorField.createHalfHeightSeparator());
            add(commentsField);
            setInputValid(true);
        }
        public void gatherResults() {
            accountConfig.setServerUser(userEditField.getText());
            accountConfig.setServerPass(passEditField.getText());
        }
    };
    private WizardScreen outgoingServerWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private FieldChangeListener fieldChangeListener;
        private ObjectChoiceField outgoingChoiceField;
        private HostnameEditField nameEditField;
        private ObjectChoiceField securityChoiceField;
        private BasicEditField portEditField;

        protected void initFields() {
            // Populate the list of existing server configurations
            MailSettings mailSettings = MailSettings.getInstance();
            int numOutgoing = mailSettings.getNumOutgoing();
            Object[] outgoingChoices = new Object[numOutgoing + 2];
            for(int i=0; i<numOutgoing; i++) {
                outgoingChoices[i] = mailSettings.getOutgoingConfig(i);
            }
            outgoingChoices[outgoingChoices.length - 2] = resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_CREATE_NEW);
            outgoingChoices[outgoingChoices.length - 1] = resources.getString(LogicMailResource.MENUITEM_DISABLED);

            fieldChangeListener = new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    outgoingServerWizardScreen_fieldChanged(field, context);
                }};

            outgoingChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_QUESTION),
                    outgoingChoices, 0);
            outgoingChoiceField.setChangeListener(fieldChangeListener);

            nameEditField = new HostnameEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_SERVER) + ' ', "");
            nameEditField.setChangeListener(fieldChangeListener);

            securityChoiceField = new ObjectChoiceField(
                    resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY),
                    new Object[] {
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_NEVER),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS_IF_AVAILABLE),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS),
                        resources.getString(LogicMailResource.CONFIG_ACCOUNT_SECURITY_SSL)},
                        ConnectionConfig.SECURITY_NONE);
            securityChoiceField.setChangeListener(fieldChangeListener);

            portEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT) + ' ', Integer.toString(25));
            portEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
            portEditField.setChangeListener(fieldChangeListener);

            add(outgoingChoiceField);
            add(nameEditField);
            add(securityChoiceField);
            add(portEditField);

            populateOutgoingItems();
            validateData();
        }

        private void outgoingServerWizardScreen_fieldChanged(Field field, int context) {
            if(field == securityChoiceField) {
                if(securityChoiceField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
                    portEditField.setText("465");
                }
                else {
                    portEditField.setText("25");
                }
            }
            if(field == outgoingChoiceField) {
                populateOutgoingItems();
            }
            validateData();
        }

        private void validateData() {
            if(nameEditField.getText().trim().length() > 0 && portEditField.getText().trim().length() > 0) {
                setInputValid(true);
            }
            else if(outgoingChoiceField.getSelectedIndex() == outgoingChoiceField.getSize() - 1) {
                setInputValid(true);
            }
            else {
                setInputValid(false);
            }
        }
        private void populateOutgoingItems() {
            int index = outgoingChoiceField.getSelectedIndex();
            Object item = outgoingChoiceField.getChoice(index);
            if(item instanceof OutgoingConfig) {
                OutgoingConfig outgoingConfig = (OutgoingConfig)item;
                nameEditField.setText(outgoingConfig.getServerName());
                securityChoiceField.setSelectedIndex(outgoingConfig.getServerSecurity());
                portEditField.setText(Integer.toString(outgoingConfig.getServerPort()));

                nameEditField.setEditable(false);
                securityChoiceField.setEditable(false);
                portEditField.setEditable(false);
            }
            else if(index == outgoingChoiceField.getSize() - 1) {
                nameEditField.setText("");
                securityChoiceField.setSelectedIndex(0);
                portEditField.setText("");
                nameEditField.setEditable(false);
                securityChoiceField.setEditable(false);
                portEditField.setEditable(false);
            }
            else {
                nameEditField.setText("");
                securityChoiceField.setSelectedIndex(ConnectionConfig.SECURITY_NONE);
                portEditField.setText("25");

                nameEditField.setEditable(true);
                securityChoiceField.setEditable(true);
                portEditField.setEditable(true);
            }
        }
        protected void onPageFlip() {
            int index = outgoingChoiceField.getSelectedIndex();
            Object item = outgoingChoiceField.getChoice(index);
            if(item instanceof OutgoingConfig
                    || index == outgoingChoiceField.getSize() - 1) {
                outgoingAuthenticationWizardScreen.setEnabled(false);
            }
            else {
                outgoingAuthenticationWizardScreen.setEnabled(true);
            }
        }
        public void gatherResults() {
            int index = outgoingChoiceField.getSelectedIndex();
            Object item = outgoingChoiceField.getChoice(index);
            if(item instanceof OutgoingConfig) {
                outgoingConfig = (OutgoingConfig)item;
                outgoingCreated = false;
            }
            else if(index == outgoingChoiceField.getSize() - 1) {
                outgoingConfig = null;
                outgoingCreated = false;
            }
            else {
                outgoingConfig.setAcctName(accountConfig.getAcctName());
                outgoingConfig.setServerName(nameEditField.getText());
                outgoingConfig.setServerSecurity(securityChoiceField.getSelectedIndex());
                outgoingConfig.setServerPort(Integer.parseInt(portEditField.getText()));
                outgoingConfig.setTransportType(ConnectionConfig.TRANSPORT_GLOBAL);
                outgoingCreated = true;
            }
        }
    };
    private WizardScreen outgoingAuthenticationWizardScreen = new WizardScreen(
            WizardScreen.resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_AUTHENTICATION_TITLE),
            WizardScreen.PAGE_NORMAL) {
        private FieldChangeListener fieldChangeListener;
        private LabelField descriptionLabelField;
        private ObjectChoiceField authChoiceField;
        private BasicEditField userEditField;
        private PasswordEditField passEditField;
        private Field commentsSpacerLabel;
        private RichTextField commentsField;
        private boolean authMode;

        protected void initFields() {
            fieldChangeListener = new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    outgoingAuthenticationWizardScreen_fieldChanged(field, context);
                }};

            descriptionLabelField = new LabelField(resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_AUTHENTICATION_QUESTION));

            String authTypes[] = {
                    resources.getString(LogicMailResource.MENUITEM_NONE),
                    "PLAIN", "LOGIN", "CRAM-MD5" };
            authChoiceField =
                new ObjectChoiceField(resources.getString(LogicMailResource.CONFIG_OUTGOING_AUTHENTICATION) + ' ', authTypes, 0);
            authChoiceField.setChangeListener(fieldChangeListener);

            userEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME) + ' ', "");

            passEditField = new PasswordEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD) + ' ', "");

            commentsSpacerLabel = BlankSeparatorField.createHalfHeightSeparator();
            commentsField = new RichTextField(resources.getString(LogicMailResource.WIZARD_SCREEN_OUTGOING_AUTHENTICATION_COMMENTS));
            commentsField.setFont(this.getFont().derive(Font.ITALIC));

            add(descriptionLabelField);
            add(authChoiceField);
            setInputValid(true);
        }

        private void outgoingAuthenticationWizardScreen_fieldChanged(Field field, int context) {
            if(field == authChoiceField) {
                if(authChoiceField.getSelectedIndex() == 0 && authMode) {
                    authMode = false;
                    delete(userEditField);
                    delete(passEditField);
                    delete(commentsSpacerLabel);
                    delete(commentsField);
                }
                else if(!authMode) {
                    authMode = true;
                    add(userEditField);
                    add(passEditField);
                    add(commentsSpacerLabel);
                    add(commentsField);
                }
            }
        }
        public void gatherResults() {
            if(outgoingConfig == null) { return; }
            int index = authChoiceField.getSelectedIndex();
            outgoingConfig.setUseAuth(index);
            if(index > 0) {
                outgoingConfig.setServerUser(userEditField.getText());
                outgoingConfig.setServerPass(passEditField.getText());
            }
            else {
                outgoingConfig.setServerUser("");
                outgoingConfig.setServerPass("");
            }
        }
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
        private VerticalFieldManager testOutputManager;
        private boolean testInProgress;
        private volatile boolean testClosed;

        protected void initFields() {
            testButton = new ButtonField(
                    resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_TEST_CONFIGURATION),
                    Field.FIELD_HCENTER | ButtonField.CONSUME_CLICK);
            testButton.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    testButtonFieldChanged();
                }});
            testOutputManager = new VerticalFieldManager();

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
            testOutputManager.deleteAll();
            testInProgress = true;
            testClosed = false;
            (new Thread() { public void run() {
                addText(MessageFormat.format(
                        resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_TESTING),
                        new Object[] { accountConfig.toString() }),
                        TYPE_INFO);
                MailClient mailClient = MailClientFactory.createTemporaryMailClient(accountConfig);
                testClientConnection(mailClient);
                
                if(testClosed) { testComplete(); return; }
                
                if(accountConfig.getOutgoingConfig() != null) {
                    addText(MessageFormat.format(
                            resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_TESTING),
                            new Object[] { outgoingConfig.toString() }),
                            TYPE_INFO);
                    mailClient = MailClientFactory.createTemporaryOutgoingMailClient(outgoingConfig);
                    testClientConnection(mailClient);
                }
                testComplete();
            }}).start();
        }
        
        private void testClientConnection(MailClient mailClient) {
            boolean success;
            String errorMessage = null;
            String errorDetail = null;
            try {
                if(checkLogin(mailClient)) {
                    success = mailClient.open();
                    errorMessage = resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_LOGIN_FAILED);
                }
                else {
                    success = false;
                    errorMessage = resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_LOGIN_CANCELED);
                }
            } catch (IOException exp) {
                success = false;
                errorMessage = exp.getMessage();
                if(exp instanceof WrappedIOException) {
                    Throwable innerExp = ((WrappedIOException)exp).getInnerException();
                    if(innerExp != null) {
                        errorDetail = innerExp.getMessage();
                    }
                }
            } catch (MailException exp) {
                success = false;
                errorMessage = exp.getMessage();
            }
            
            if(mailClient.isConnected()) {
                try {
                    mailClient.close();
                } catch (Exception e) { }
            }
            
            if(testClosed) { return; }
            
            if(success) {
                addText(resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_CONNECTION_SUCCESSFUL), TYPE_SUCCESS);
            }
            else {
                addText(resources.getString(LogicMailResource.WIZARD_SCREEN_FINAL_CONNECTION_FAILED), TYPE_FAILURE);
                if(errorMessage != null) {
                    addText(errorMessage, TYPE_FAILURE_DETAIL);
                    if(errorDetail != null) {
                        addText(errorDetail, TYPE_FAILURE_DETAIL);
                    }
                }
            }
        }
        
        private boolean checkLogin(final MailClient client) {
            if(!client.isLoginRequired()) { return true; }

            String username = client.getUsername();
            String password = client.getPassword();
            // If the username and password are not null,
            // but are empty, request login information.
            if((username != null && password != null)
                    && (username.trim().equals("") || password.trim().equals(""))) {

                final boolean[] canceled = new boolean[1];
                UiApplication.getUiApplication().invokeAndWait(new Runnable() {
                    public void run() {
                        String username = client.getUsername();
                        String password = client.getPassword();
                        LoginDialog dialog = new LoginDialog(username, password);
                        if(dialog.doModal() == Dialog.OK) {
                            client.setUsername(dialog.getUsername());
                            client.setPassword(dialog.getPassword());
                            canceled[0] = false;
                        }
                        else {
                            canceled[0] = true;
                        }
                    }
                });

                return !canceled[0];
            }
            else {
                return true;
            }
        }
        
        private final static int TYPE_INFO = 0;
        private final static int TYPE_SUCCESS = 1;
        private final static int TYPE_FAILURE = 2;
        private final static int TYPE_FAILURE_DETAIL = 3;
        
        private void addText(final String text, final int type) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    testOutputManager.add(new StatusLabelField(text, type));
                }
            });
        }
        
        class StatusLabelField extends LabelField {
            private final int type;
            StatusLabelField(final String text, final int type) {
                super(text);
                this.type = type;
                if(type == TYPE_SUCCESS) {
                    this.setFont(this.getFont().derive(Font.BOLD));
                }
                else if(type == TYPE_FAILURE) {
                    this.setFont(this.getFont().derive(Font.BOLD));
                }
                else if(type == TYPE_FAILURE_DETAIL) {
                    this.setFont(this.getFont().derive(Font.ITALIC));
                }
            }
            
            protected void paint(net.rim.device.api.ui.Graphics graphics) {
                if(type == TYPE_SUCCESS) {
                    graphics.setColor(Color.GREEN);
                }
                else if(type == TYPE_FAILURE || type == TYPE_FAILURE_DETAIL) {
                    graphics.setColor(Color.RED);
                }
                super.paint(graphics);
            };
        }

        public boolean onClose() {
            if(super.onClose()) {
                if(testInProgress) {
                    testClosed = true;
                    testOutputManager.deleteAll();
                }
                return true;
            }
            else {
                return false;
            }
        };
        
        private void testComplete() {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    testButton.setEditable(true);
                    setInputValid(true);
                    testInProgress = false;
                    if(testClosed) {
                        testOutputManager.deleteAll();
                    }
                }
            });
        }
    };

    protected void onWizardFinished() {
        // Add the configuration that was just built
        MailSettings mailSettings = MailSettings.getInstance();
        if(identityCreated) { mailSettings.addIdentityConfig(identityConfig); }
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
}
