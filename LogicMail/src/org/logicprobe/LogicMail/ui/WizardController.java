package org.logicprobe.LogicMail.ui;

import java.util.Vector;

import net.rim.device.api.ui.UiApplication;

/**
 * Provides the base class for wizards, which consist
 * of a series of wizard pages.
 */
public abstract class WizardController {
	private Vector wizardScreens = new Vector();
	
	public WizardController() {
	}
	
	/**
	 * Adds a wizard screen to the controller
	 * @param screen The screen to add
	 */
	protected void addWizardScreen(WizardScreen screen) {
		wizardScreens.addElement(screen);
	}
	
	/**
	 * Removes a wizard screen from the controller
	 * @param screen The screen to remove
	 */
	protected void removeWizardScreen(WizardScreen screen) {
		wizardScreens.removeElement(screen);
	}
	
	/**
	 * Removes all wizard screens from the controller
	 */
	protected void removeAllWizardScreens() {
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
	 * 
	 * @return True if the wizard was completed, false if it was canceled.
	 */
	public boolean start() {
		int index = 0;
		while(true) {
			WizardScreen currentScreen = (WizardScreen)wizardScreens.elementAt(index);
			currentScreen.onPageEnter();
			UiApplication.getUiApplication().pushModalScreen(currentScreen);
			switch(currentScreen.getPageResult()) {
			case WizardScreen.RESULT_NEXT:
				index++;
				while(true) {
					if(index == wizardScreens.size()) {
						onWizardFinished();
						return true;
					}
					else if(!((WizardScreen)wizardScreens.elementAt(index)).isEnabled()) {
						index++;
					}
					else {
						break;
					}
				}
				break;
			case WizardScreen.RESULT_PREV:
				index--;
				while(true) {
					if(index < 0) {
						return false;
					}
					else if(!((WizardScreen)wizardScreens.elementAt(index)).isEnabled()) {
						index--;
					}
					else {
						break;
					}
				}
				break;
			case WizardScreen.RESULT_CANCEL:
				return false;
			default:
				return false;
			}
		}
	}
}
