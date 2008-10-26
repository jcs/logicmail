package org.logicprobe.LogicMail.ui;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.PopConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.conf.OutgoingConfig;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.EmailAddressEditField;
import net.rim.device.api.ui.component.RadioButtonGroup;
import net.rim.device.api.ui.component.RadioButtonField;
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
		addWizardScreen(finalWizardScreen);
	}

	private IdentityConfig identityConfig;
	private AccountConfig accountConfig;
	private OutgoingConfig outgoingConfig;
	
	private WizardScreen newAccountWizardScreen = new WizardScreen("New account setup", WizardScreen.PAGE_FIRST) {
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
			identityChoices[identityChoices.length - 1] = "Create new";
			
	        fieldChangeListener = new FieldChangeListener() {
	            public void fieldChanged(Field field, int context) {
	            	newAccountWizardScreen_fieldChanged(field, context);
	            }};
	            
			identityChoiceField = new ObjectChoiceField(
					"Select the identity to use with this account:",
					identityChoices, 0);
			identityChoiceField.setChangeListener(fieldChangeListener);
			
			identityNameEditField = new EditField("Identity: ", "");
			identityNameEditField.setChangeListener(fieldChangeListener);
			
			nameEditField = new EditField("Your name: ", "");
			nameEditField.setChangeListener(fieldChangeListener);
			
			emailAddressEditField = new EmailAddressEditField("E-Mail address: ", "");
			emailAddressEditField.setChangeListener(fieldChangeListener);
			
			commentsField = new RichTextField("The identity contains information people see about you when they read your messages.");
			commentsField.setFont(this.getFont().derive(Font.ITALIC));

			add(identityChoiceField);
			add(identityNameEditField);
			add(nameEditField);
			add(emailAddressEditField);
			add(new LabelField());
			add(commentsField);
			
			populateIdentityItems();
			validateData();
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
					identityNameEditField.setText("Default");
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
	private WizardScreen accountTypeWizardScreen = new WizardScreen("Account type", WizardScreen.PAGE_NORMAL) {
		private LabelField descriptionLabelField;
		private RadioButtonGroup accountTypeGroup;
		private RadioButtonField imapAccountType;
		private RadioButtonField popAccountType;
		
		protected void initFields() {
			descriptionLabelField = new LabelField("What type of account would you like to create?");
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
	private WizardScreen accountNameWizardScreen = new WizardScreen("Account name", WizardScreen.PAGE_NORMAL) {
		private LabelField descriptionLabelField;
		private EditField nameEditField;
		
		protected void initFields() {
			descriptionLabelField = new LabelField("What name would you would like to use to refer to this account?");
			nameEditField = new EditField("Account name: ", "");
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
	private WizardScreen mailServerWizardScreen = new WizardScreen("Mail server", WizardScreen.PAGE_NORMAL) {
		private FieldChangeListener fieldChangeListener;
		private LabelField descriptionLabelField;
		private BasicEditField nameEditField;
		private CheckboxField sslCheckboxField;
		private BasicEditField portEditField;
		private CheckboxField useMdsCheckboxField;
		
		protected void initFields() {
	        fieldChangeListener = new FieldChangeListener() {
	            public void fieldChanged(Field field, int context) {
	            	mailServerWizardScreen_fieldChanged(field, context);
	            }};
			
			descriptionLabelField = new LabelField("Enter the name of your incoming mail server:");
			
			nameEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_SERVER) + " ", "");
			nameEditField.setChangeListener(fieldChangeListener);
			
			sslCheckboxField = new CheckboxField("SSL", false);
			sslCheckboxField.setChangeListener(fieldChangeListener);
			
			portEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT) + " ", Integer.toString(143));
			portEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
			portEditField.setChangeListener(fieldChangeListener);
			
			useMdsCheckboxField = new CheckboxField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USEMDSPROXY), true);
			
			add(descriptionLabelField);
			add(nameEditField);
			add(sslCheckboxField);
			add(portEditField);
			add(useMdsCheckboxField);
		}
		
		private void mailServerWizardScreen_fieldChanged(Field field, int context) {
	        if(field == sslCheckboxField) {
	            if(accountType == ACCOUNT_TYPE_POP) {
	                if(sslCheckboxField.getChecked()) {
	                	portEditField.setText("995");
	                }
	                else {
	                	portEditField.setText("110");
	                }
	            }
	            else if(accountType == ACCOUNT_TYPE_IMAP) {
	                if(sslCheckboxField.getChecked()) {
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
			accountConfig.setServerSSL(sslCheckboxField.getChecked());
			accountConfig.setServerPort(Integer.parseInt(portEditField.getText()));
			accountConfig.setDeviceSide(!useMdsCheckboxField.getChecked());
		}
	};
	private WizardScreen authenticationWizardScreen = new WizardScreen("Authentication", WizardScreen.PAGE_NORMAL) {
		private LabelField descriptionLabelField;
		private BasicEditField userEditField;
		private PasswordEditField passEditField;
		private RichTextField commentsField;
		
		protected void initFields() {
			descriptionLabelField = new LabelField("Enter the user name and password used to access your account:");
			userEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME) + " ", "");
			
			passEditField = new PasswordEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD) + " ", "");
			
			commentsField = new RichTextField("If you leave any of these settings blank, you will be prompted for them when LogicMail tries to connect to your mail server.");
			commentsField.setFont(this.getFont().derive(Font.ITALIC));
			
			add(descriptionLabelField);
			add(userEditField);
			add(passEditField);
			add(new LabelField());
			add(commentsField);
        	setInputValid(true);
		}
		public void gatherResults() {
			accountConfig.setServerUser(userEditField.getText());
			accountConfig.setServerPass(passEditField.getText());
		}
	};
	private WizardScreen outgoingServerWizardScreen = new WizardScreen("Outgoing mail server", WizardScreen.PAGE_NORMAL) {
		private FieldChangeListener fieldChangeListener;
		private ObjectChoiceField outgoingChoiceField;
		private BasicEditField nameEditField;
		private CheckboxField sslCheckboxField;
		private BasicEditField portEditField;
		private CheckboxField useMdsCheckboxField;
		
		protected void initFields() {
			// Populate the list of existing identities
			MailSettings mailSettings = MailSettings.getInstance();
			int numOutgoing = mailSettings.getNumOutgoing();
			Object[] outgoingChoices = new Object[numOutgoing + 1];
			for(int i=0; i<numOutgoing; i++) {
				outgoingChoices[i] = mailSettings.getOutgoingConfig(i);
			}
			outgoingChoices[outgoingChoices.length - 1] = "Create new";
			
	        fieldChangeListener = new FieldChangeListener() {
	            public void fieldChanged(Field field, int context) {
	            	outgoingServerWizardScreen_fieldChanged(field, context);
	            }};
	            
            outgoingChoiceField = new ObjectChoiceField(
					"Select the outgoing mail (SMTP) server to use with this account:",
					outgoingChoices, 0);
			outgoingChoiceField.setChangeListener(fieldChangeListener);
			
			nameEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_SERVER) + " ", "");
			nameEditField.setChangeListener(fieldChangeListener);
			
			sslCheckboxField = new CheckboxField("SSL", false);
			sslCheckboxField.setChangeListener(fieldChangeListener);
			
			portEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PORT) + " ", Integer.toString(25));
			portEditField.setFilter(TextFilter.get(TextFilter.NUMERIC));
			portEditField.setChangeListener(fieldChangeListener);
			
			useMdsCheckboxField = new CheckboxField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USEMDSPROXY), true);

			add(outgoingChoiceField);
			add(nameEditField);
			add(sslCheckboxField);
			add(portEditField);
			add(useMdsCheckboxField);
			
			populateOutgoingItems();
			validateData();
		}

		private void outgoingServerWizardScreen_fieldChanged(Field field, int context) {
			if(field == sslCheckboxField) {
                if(sslCheckboxField.getChecked()) {
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
				sslCheckboxField.setChecked(outgoingConfig.getServerSSL());
				portEditField.setText(Integer.toString(outgoingConfig.getServerPort()));
				useMdsCheckboxField.setChecked(!outgoingConfig.getDeviceSide());
				
				nameEditField.setEditable(false);
				sslCheckboxField.setEditable(false);
				portEditField.setEditable(false);
				useMdsCheckboxField.setEditable(false);
			}
			else {
				nameEditField.setText("");
				sslCheckboxField.setChecked(false);
				portEditField.setText("25");
				useMdsCheckboxField.setChecked(true);
				
				nameEditField.setEditable(true);
				sslCheckboxField.setEditable(true);
				portEditField.setEditable(true);
				useMdsCheckboxField.setEditable(true);
			}
		}
		protected void onPageFlip() {
			int index = outgoingChoiceField.getSelectedIndex();
			Object item = outgoingChoiceField.getChoice(index);
			if(item instanceof OutgoingConfig) {
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
			else {
				outgoingConfig.setAcctName(accountConfig.getAcctName());
				outgoingConfig.setServerName(nameEditField.getText());
				outgoingConfig.setServerSSL(sslCheckboxField.getChecked());
				outgoingConfig.setServerPort(Integer.parseInt(portEditField.getText()));
				outgoingConfig.setDeviceSide(!useMdsCheckboxField.getChecked());
				outgoingCreated = true;
			}
		}
	};
	private WizardScreen outgoingAuthenticationWizardScreen = new WizardScreen("Outgoing authentication", WizardScreen.PAGE_NORMAL) {
		private FieldChangeListener fieldChangeListener;
		private LabelField descriptionLabelField;
		private ObjectChoiceField authChoiceField;
		private BasicEditField userEditField;
		private PasswordEditField passEditField;
		private LabelField commentsSpacerLabel;
		private RichTextField commentsField;
		private boolean authMode;
		
		protected void initFields() {
	        fieldChangeListener = new FieldChangeListener() {
	            public void fieldChanged(Field field, int context) {
	            	outgoingAuthenticationWizardScreen_fieldChanged(field, context);
	            }};
	            
			descriptionLabelField = new LabelField("What type of authentication, if any, is required to send mail?");

			String authTypes[] = {
	        		resources.getString(LogicMailResource.MENUITEM_NONE),
	        		"PLAIN", "LOGIN", "CRAM-MD5" };
			authChoiceField =
	            new ObjectChoiceField(resources.getString(LogicMailResource.CONFIG_OUTGOING_AUTHENTICATION) + " ", authTypes, 0);
			authChoiceField.setChangeListener(fieldChangeListener);
			
			userEditField = new BasicEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_USERNAME) + " ", "");
			
			passEditField = new PasswordEditField(resources.getString(LogicMailResource.CONFIG_ACCOUNT_PASSWORD) + " ", "");
			
			commentsSpacerLabel = new LabelField();
			commentsField = new RichTextField("If you leave any of these settings blank, you will be prompted for them when LogicMail tries to connect to your mail server.");
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
	private WizardScreen finalWizardScreen = new WizardScreen("Finish", WizardScreen.PAGE_LAST) {
		private ButtonField testButton;
		
		protected void initFields() {
			testButton = new ButtonField("Test Connection", Field.FIELD_HCENTER);

			add(new LabelField());
			add(testButton);
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
	};
	
	protected void onWizardFinished() {
		// Add the configuration that was just built
		MailSettings mailSettings = MailSettings.getInstance();
		if(identityCreated) { mailSettings.addIdentityConfig(identityConfig); }
		if(outgoingCreated) { mailSettings.addOutgoingConfig(outgoingConfig); }
		mailSettings.addAccountConfig(accountConfig);
		mailSettings.saveSettings();
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
