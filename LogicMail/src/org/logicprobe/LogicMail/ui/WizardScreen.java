package org.logicprobe.LogicMail.ui;

import org.logicprobe.LogicMail.LogicMailResource;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
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
	private LabelField titleLabel;
	private HorizontalFieldManager statusFieldManager;
	private ButtonField cancelButton;
	private ButtonField prevButton;
	private ButtonField nextButton;

	private final static int MENU_CONTEXT = 0x10000;
	private final static int MENU_MAIN = 0x40000000;

	public final static int PAGE_NORMAL = 0;
	public final static int PAGE_FIRST  = 1;
	public final static int PAGE_LAST   = 2;

	public final static int RESULT_CANCEL = 0;
	public final static int RESULT_PREV   = 1;
	public final static int RESULT_NEXT   = 2;
	
	private String title;
	private int pageType;
	private int pageResult;
	private boolean isDataValid;
	private boolean isEnabled = true;
	
	public WizardScreen(String title, int pageType) {
		this.title = title;
		this.pageType = pageType;
		this.pageResult = RESULT_CANCEL;
		initBaseFields();
		initFields();
		nextButton.setEditable(isDataValid);
	}
	
	private void initBaseFields() {
        titleLabel = new LabelField(title, LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleLabel);
		
        cancelButton = new ButtonField("Cancel");
        cancelButton.setChangeListener(new FieldChangeListener() {
			public void fieldChanged(Field field, int context) {
				cancelButton_fieldChanged(field, context);
			}
        });
        prevButton = new ButtonField("< Prev");
        prevButton.setChangeListener(new FieldChangeListener() {
			public void fieldChanged(Field field, int context) {
				prevButton_fieldChanged(field, context);
			}
        });
        nextButton = new ButtonField("Next >");
        nextButton.setChangeListener(new FieldChangeListener() {
			public void fieldChanged(Field field, int context) {
				nextButton_fieldChanged(field, context);
			}
        });
        
        statusFieldManager = new HorizontalFieldManager() {
    	    protected void onFocus(int direction) {
    	    	if(direction == 1) {
    	    		// Force focus to the last button on field entry
    	    		if(isDataValid) {
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
        	nextButton.setLabel("Finish");
        	statusFieldManager.add(nextButton);
        }
        
        setStatus(statusFieldManager);
	}

	/**
	 * Creates the page-specific input fields.
	 */
	protected abstract void initFields();
	
	/**
	 * Sets whether this screen contains valid data,
	 * and should provide a next button.
	 * 
	 * @param isDataValid True if data is valid
	 */
	protected void setDataValid(boolean isDataValid) {
		this.isDataValid = isDataValid;
		nextButton.setEditable(isDataValid);
	}
	
	/**
	 * Gets whether this screen contains valid data.
	 * 
	 * @return True if data is valid
	 */
	protected boolean isDataValid() {
		return this.isDataValid;
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
	
    public boolean onMenu(int instance) {
		if (instance == MENU_MAIN) {
			// Main menu button pressed, display menu
			return super.onMenu(instance);
		}
		else if (instance == MENU_CONTEXT) {
			// Trackball click, call override method
			if(!onClick()) {
				return super.onMenu(instance);
			}
			else {
				return false;
			}
		}
		else {
			// Trackwheel click, display menu
			return super.onMenu(instance);
		}
	}
    
    /**
     * Invoked when the user clicks the trackball on
     * devices that have a separate menu button.
     * 
     * @return True if the click was handled, false to fall
     *         through and display the context menu.
     */
    protected boolean onClick() {
    	if(getFieldWithFocus() == statusFieldManager) {
    		return true;
    	}
    	return false;
    }

	protected boolean keyChar(char c, int status, int time) {
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
	
	public boolean onClose() {
		if(pageResult == RESULT_CANCEL) {
            int result =
            	Dialog.ask(Dialog.D_YES_NO, "Are you sure you want to cancel?");
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

	public int getPageResult() {
		return pageResult;
	}
	
	/**
	 * Called by the controller on all pages in sequence, so that results
	 * of the wizard can be gathered and processed.
	 */
	public void gatherResults() { }
}
