/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import net.rim.device.api.ui.UiApplication;

/**
 * Provides the base class for wizards, which consist
 * of a series of wizard pages.
 */
public abstract class WizardController {
    private Vector wizardScreens = new Vector();
    private boolean started;
    private int index;
    private WizardScreen currentScreen;

    public WizardController() {
    }

    /**
     * Adds a wizard screen to the controller
     * @param screen The screen to add
     */
    protected void addWizardScreen(WizardScreen screen) {
        if(started) { throw new IllegalStateException(); }
        screen.setWizardController(this);
        wizardScreens.addElement(screen);
    }

    /**
     * Removes a wizard screen from the controller
     * @param screen The screen to remove
     */
    protected void removeWizardScreen(WizardScreen screen) {
        if(started) { throw new IllegalStateException(); }
        screen.setWizardController(null);
        wizardScreens.removeElement(screen);
    }

    /**
     * Removes all wizard screens from the controller
     */
    protected void removeAllWizardScreens() {
        if(started) { throw new IllegalStateException(); }
        Enumeration e = wizardScreens.elements();
        while(e.hasMoreElements()) {
            ((WizardScreen)e.nextElement()).setWizardController(null);
        }
        wizardScreens.removeAllElements();
    }

    /**
     * Gets the wizard screens that have been added to the controller.
     * 
     * @return Array of wizard screens
     */
    protected WizardScreen[] getWizardScreens() {
        WizardScreen[] result = new WizardScreen[wizardScreens.size()];
        wizardScreens.copyInto(result);
        return result;
    }

    /**
     * Iterates through all of the WizardScreens, and calls
     * {@link WizardScreen#gatherResults()} on each of them.
     */
    protected void gatherResults() {
        int size = wizardScreens.size();
        for(int i=0; i<size; i++) {
            ((WizardScreen)wizardScreens.elementAt(i)).gatherResults();
        }
    }

    /**
     * Called when the wizard successfully completes
     */
    protected void onWizardFinished() { }

    /**
     * Starts the wizard.
     * Pushes the first screen on the display stack as modal,
     * followed by each successive screen as appropriate.
     */
    public void start() {
        if(started) { throw new IllegalStateException(); }
        
        started = true;
        index = 0;
        
        currentScreen = (WizardScreen)wizardScreens.elementAt(index);
        currentScreen.onPageEnter();
        UiApplication.getUiApplication().pushScreen(currentScreen);
    }

    /**
     * This method should only be called by the active wizard screen,
     * as a substitute for calling {@link WizardScreen#close()}.
     */
    void closeWizardScreen() {
        boolean swapScreen = false;
        boolean wizardFinished = false;
        
        // Check the page result, and determine whether to go to
        // the next page, the previous page, cancel, or finish
        switch(currentScreen.getPageResult()) {
        case WizardScreen.RESULT_NEXT:
            index++;
            while(true) {
                if(index == wizardScreens.size()) {
                    wizardFinished = true;
                    break;
                }
                else if(!((WizardScreen)wizardScreens.elementAt(index)).isEnabled()) {
                    index++;
                }
                else {
                    swapScreen = true;
                    break;
                }
            }
            break;
        case WizardScreen.RESULT_PREV:
            index--;
            while(true) {
                if(index < 0) {
                    swapScreen = false;
                    break;
                }
                else if(!((WizardScreen)wizardScreens.elementAt(index)).isEnabled()) {
                    index--;
                }
                else {
                    swapScreen = true;
                    break;
                }
            }
            break;
        case WizardScreen.RESULT_CANCEL:
        default:
            swapScreen = false;
            break;
        }
        
        if(swapScreen) {
            // Swap the current screen for the next screen by first
            // pushing the new screen, then closing the old screen.
            // This is necessary to prevent flicker on newer platforms.
            WizardScreen nextScreen = (WizardScreen)wizardScreens.elementAt(index);
            nextScreen.onPageEnter();
            UiApplication.getUiApplication().pushScreen(nextScreen);
            UiApplication.getUiApplication().popScreen(currentScreen);
            currentScreen = nextScreen;
        }
        else {
            // Close the current screen and stop the wizard.
            UiApplication.getUiApplication().popScreen(currentScreen);
            if(wizardFinished) {
                onWizardFinished();
            }
            started = false;
        }
    }
}
