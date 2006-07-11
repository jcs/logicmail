/*
 * AcctCfgScreen.java
 *
 * © <your company here>, 2003-2005
 * Confidential and proprietary.
 */

package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;

/**
 * Configuration screen
 */
public class AcctCfgScreen extends MainScreen implements FieldChangeListener {
    private BasicEditField fldAcctName;
    private BasicEditField fldServerName;
    private ObjectChoiceField fldServerType;
    private CheckboxField fldServerSSL;
    private BasicEditField fldServerUser;
    private PasswordEditField fldServerPass;
    private boolean saveData;

    private ButtonField btSave;

    public AcctCfgScreen() {
        super();
        LabelField titleField = new LabelField
         ("LogicMail - Account", LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleField);

        add(fldAcctName = new BasicEditField("Account name: ", ""));
        add(fldServerName = new BasicEditField("Server: ", ""));
        String[] serverTypes = { "POP", "IMAP" };
        add(fldServerType = new ObjectChoiceField("Type: ", serverTypes));
        add(fldServerSSL = new CheckboxField("SSL", false));
        add(fldServerUser = new BasicEditField("Username: ", ""));
        add(fldServerPass = new PasswordEditField("Password: ", ""));
        
        btSave = new ButtonField("Save", Field.FIELD_HCENTER);
        btSave.setChangeListener(this);
        add(btSave);
        
        saveData = false;
    }

    public void fieldChanged(Field field, int context) {
        if(field == btSave) {
            onClose();
        }
    }

    public void save() {
        saveData = true;
    }

    public boolean getSaveData() {
        return saveData;
    }

    public String getAcctName() {
        return fldAcctName.getText();
    }
    
    public void setAcctName(String acctName) {
        fldAcctName.setText(acctName);
    }
    
    public String getServerName() {
        return fldServerName.getText();
    }
    
    public void setServerName(String serverName) {
        fldServerName.setText(serverName);
    }

    public int getServerType() {
        return fldServerType.getSelectedIndex();
    }
    
    public void setServerType(int serverType) {
        fldServerType.setSelectedIndex(serverType);
    }
    public boolean getServerSSL() {
        return fldServerSSL.getChecked();
    }
    
    public void setServerSSL(boolean serverSSL) {
        fldServerSSL.setChecked(serverSSL);
    }
    public String getServerUser() {
        return fldServerUser.getText();
    }
    
    public void setServerUser(String serverUser) {
        fldServerUser.setText(serverUser);
    }
    
    public String getServerPass() {
        return fldServerPass.getText();
    }
    
    public void setServerPass(String serverPass) {
        fldServerPass.setText(serverPass);
    }
}

