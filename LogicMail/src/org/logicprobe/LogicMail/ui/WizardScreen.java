/*-
 * Copyright (c) 2009, ${author}
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

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.HorizontalFieldManager;

/**
 * Provides the base class for wizard pages, with a title
 * and standard navigation controls.  Subclasses should
 * override {@link #initFields()} to add the necessary
 * fields for input and instruction.
 */
public abstract class WizardScreen extends MainScreen {
    protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    private WizardController controller;
    private LabelField titleLabel;
    private HorizontalFieldManager statusFieldManager;
    private ButtonField cancelButton;
    private ButtonField prevButton;
    private ButtonField nextButton;

    public final static int PAGE_NORMAL = 0;
    public final static int PAGE_FIRST  = 1;
    public final static int PAGE_LAST   = 2;

    public final static int RESULT_CANCEL = 0;
    public final static int RESULT_PREV   = 1;
    public final static int RESULT_NEXT   = 2;

    private final static int MENU_CONTEXT = 0x10000;

    private String title;
    private int pageType;
    private int pageResult;
    private boolean isInputValid;
    private boolean isEnabled = true;

    public WizardScreen(String title, int pageType) {
        this.title = title;
        this.pageType = pageType;
        this.pageResult = RESULT_CANCEL;
        initBaseFields();
        initFields();
        nextButton.setEditable(isInputValid);
    }

    private void initBaseFields() {
        titleLabel = new LabelField(title, LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleLabel);

        cancelButton = new ButtonField(resources.getString(LogicMailResource.MENUITEM_CANCEL));
        cancelButton.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                cancelButton_fieldChanged(field, context);
            }
        });
        prevButton = new ButtonField("< " + resources.getString(LogicMailResource.WIZARD_PREV));
        prevButton.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                prevButton_fieldChanged(field, context);
            }
        });
        nextButton = new ButtonField(resources.getString(LogicMailResource.WIZARD_NEXT) + " >");
        nextButton.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                nextButton_fieldChanged(field, context);
            }
        });

        statusFieldManager = new HorizontalFieldManager() {
            protected void onFocus(int direction) {
                if(direction == 1) {
                    // Force focus to the last button on field entry
                    if(isInputValid) {
                        getField(getFieldCount() - 1).setFocus();
                    }
                    else {
                        getField(getFieldCount() - 2).setFocus();
                    }
                }
                else {
                    super.onFocus(direction);
                }
            }
        };

        statusFieldManager.add(cancelButton);
        if(pageType == PAGE_NORMAL) {
            statusFieldManager.add(prevButton);
            statusFieldManager.add(nextButton);
        }
        else if(pageType == PAGE_FIRST) {
            statusFieldManager.add(nextButton);
        }
        else if(pageType == PAGE_LAST) {
            statusFieldManager.add(prevButton);
            nextButton.setLabel(resources.getString(LogicMailResource.WIZARD_FINISH));
            statusFieldManager.add(nextButton);
        }

        setStatus(statusFieldManager);
    }

    /**
     * Creates the page-specific input fields.
     */
    protected abstract void initFields();

    /**
     * Sets the reference to the wizard controller.
     * This method should only be called by the controller
     * itself when the screen is added or removed.
     * 
     * @param controller reference to the wizard controller
     */
    final void setWizardController(WizardController controller) {
        this.controller = controller;
    }
    
    /**
     * Sets whether this screen contains input valid data,
     * and should provide a next button.
     * 
     * @param isInputValid True if input data is valid
     */
    protected void setInputValid(boolean isInputValid) {
        this.isInputValid = isInputValid;
        nextButton.setEditable(isInputValid);
    }

    /**
     * Gets whether this screen contains valid input data.
     * 
     * @return True if input data is valid
     */
    protected boolean isInputValid() {
        return this.isInputValid;
    }

    /**
     * Sets whether this screen is marked as enabled.
     * 
     * @param isEnabled True for enabled, false for disabled.
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Gets whether this screen is marked as enabled.
     * 
     * @return True for enabled, false for disabled.
     */
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onMenu(int)
     */
    public boolean onMenu(int instance) {
        boolean result;
        // Prevent the context menu from being shown if focus
        // is on the field containing navigation buttons.
        if (getFieldWithFocus() == statusFieldManager
                && instance == MENU_CONTEXT) {
            result = false;
        }
        else {
            result = super.onMenu(instance);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
    protected boolean keyChar(char c, int status, int time) {
        if(c == Keypad.KEY_ESCAPE) {
            pageResult = RESULT_CANCEL;
        }
        return super.keyChar(c, status, time);
    }

    private void cancelButton_fieldChanged(Field field, int context) {
        pageResult = RESULT_CANCEL;
        onClose();
    }

    private void prevButton_fieldChanged(Field field, int context) {
        pageResult = RESULT_PREV;
        onClose();
    }

    private void nextButton_fieldChanged(Field field, int context) {
        pageResult = RESULT_NEXT;	
        onClose();
    }

    /**
     * Called by the controller just before pushing the screen on
     * the display stack, so the screen can do anything necessary
     * to populate its contents from shared data.
     */
    public void onPageEnter() { }

    /**
     * Called just before a page transition, so the screen can
     * store any local data.
     */
    protected void onPageFlip() { }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onClose()
     */
    public boolean onClose() {
        if(pageResult == RESULT_CANCEL) {
            int result =
                Dialog.ask(Dialog.D_YES_NO, resources.getString(LogicMailResource.WIZARD_CONFIRM_CANCEL));
            if(result == Dialog.YES) {
                close();
                return true;
            }
            else {
                return false;
            }
        }
        else {
            onPageFlip();
            close();
            return true;
        }
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#close()
     */
    public final void close() {
        controller.closeWizardScreen();
    }
    
    public int getPageResult() {
        return pageResult;
    }

    /**
     * Called by the controller on all pages in sequence, so that results
     * of the wizard can be gathered and processed.
     */
    public void gatherResults() { }
}
