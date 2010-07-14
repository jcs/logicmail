package org.logicprobe.LogicMail.ui;

import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.ui.VirtualKeyboard;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.picker.FilePicker;

import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;

public class ScreenFactoryBB50T extends ScreenFactoryBB50 {
    public StandardScreen getMailHomeScreen(NavigationController navigationController, MailRootNode mailRootNode) {
        return new StandardTouchScreen(navigationController, new TouchMailHomeScreen(mailRootNode));
    }

    public StandardScreen getMailboxScreen(NavigationController navigationController, MailboxNode mailboxNode) {
        return new StandardTouchScreen(navigationController, new MailboxScreen(mailboxNode));
    }
    
    public String showFilePicker() {
        String fileUrl;
        try {
            fileUrl = FilePicker.getInstance().show();
        } catch (ControlledAccessException e) {
            // There is a bug (JAVAAPI-830) in certain BlackBerry OS 5.0
            // releases that causes this exception to be thrown if the new
            // FilePicker API is used on an actual device.  To avoid having to
            // know specifically when the FilePicker will work, we catch this
            // exception and fall back to our own implementation.
            FilePickerDialog dialog = new FilePickerDialog();
            dialog.getVirtualKeyboard().setVisibility(VirtualKeyboard.HIDE_FORCE);
            if(dialog.doModal() == Dialog.OK) {
                fileUrl = dialog.getFileUrl();
            }
            else {
                fileUrl = null;
            }
        }
        
        return fileUrl;
    }
}
