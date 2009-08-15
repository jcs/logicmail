/*-
 * Copyright (c) 2009, Derek Konigsberg
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
import org.logicprobe.LogicMail.mail.MailConnectionListener;
import org.logicprobe.LogicMail.mail.MailConnectionLoginEvent;
import org.logicprobe.LogicMail.mail.MailConnectionManager;
import org.logicprobe.LogicMail.mail.MailConnectionStateEvent;
import org.logicprobe.LogicMail.mail.MailConnectionStatusEvent;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.util.EventObjectRunnable;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.Status;
import net.rim.device.api.ui.container.MainScreen;

/**
 * Standard UI screen implementation.
 * This implementation is designed to separate RIM API inheritance
 * relationships from concrete UI screens through composition.
 * The concrete UI is implemented through a <tt>ScreenProvider</tt>
 * implementation.
 */
public class StandardScreen extends MainScreen {
	protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
	protected static StatusBarField statusBarField = new StatusBarField();
	private NavigationController navigationController;
	private HeaderField headerField;
	private boolean isExposed = false;
	private Field originalStatusField;
	
	private ScreenProvider screenProvider;
	
    /**
     * Instantiates a new standard screen.
     * 
     * @param navigationController the navigation controller
     * @param screenProvider the screen provider
     */
    public StandardScreen(NavigationController navigationController, ScreenProvider screenProvider) {
        super();
        if(navigationController == null || screenProvider == null) {
        	throw new IllegalArgumentException();
        }
        
        this.navigationController = navigationController;
        this.screenProvider = screenProvider;
        initialize();
    }

    /**
     * Initialize the screen elements.
     */
    private void initialize() {
        // Create screen elements
        if(screenProvider.getTitle() != null) {
	        this.headerField = new HeaderField("LogicMail - " + screenProvider.getTitle());
	        setTitle(headerField);
        }
        
        screenProvider.setNavigationController(navigationController);
        screenProvider.initFields(this);
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#setStatus(net.rim.device.api.ui.Field)
     */
    public void setStatus(Field status) {
    	originalStatusField = status;
    	super.setStatus(status);
    }
    
    private MailConnectionListener mailConnectionListener = new MailConnectionListener() {
		public void mailConnectionStateChanged(MailConnectionStateEvent e) { }
		public void mailConnectionStatus(MailConnectionStatusEvent e) {
			if(isExposed) {
				mailConnectionListener_MailConnectionStatus(e);
			}
		}
		public void mailConnectionError(MailConnectionStatusEvent e) {
			if(isExposed) {
				mailConnectionListener_MailConnectionError(e);
			}
		}
		public void mailConnectionLogin(MailConnectionLoginEvent e) {
			if(isExposed) {
				mailConnectionListener_MailConnectionLogin(e);
			}
		}
    };

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onDisplay()
     */
    protected void onDisplay() {
    	super.onDisplay();
    	isExposed = true;
        synchronized(statusBarField) {
        	if(statusBarField.hasStatus()) {
        		setStatus(statusBarField);
        	}
        	else {
        		setStatus(originalStatusField);
        	}
        }
    	MailConnectionManager.getInstance().addMailConnectionListener(mailConnectionListener);
    	NotificationHandler.getInstance().cancelNotification();
    	screenProvider.onDisplay();
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onUndisplay()
     */
    protected void onUndisplay() {
    	screenProvider.onUndisplay();
    	isExposed = false;
        synchronized(statusBarField) {
    		setStatus(originalStatusField);
        }
    	MailConnectionManager.getInstance().removeMailConnectionListener(mailConnectionListener);
    	NotificationHandler.getInstance().cancelNotification();
    	super.onUndisplay();
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onExposed()
     */
    protected void onExposed() {
    	super.onExposed();
    	isExposed = true;
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onObscured()
     */
    protected void onObscured() {
    	super.onObscured();
    	isExposed = false;
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#onClose()
     */
    public boolean onClose() {
    	boolean result = screenProvider.onClose();
    	if(result) {
    		if(this.isDisplayed()) {
    			close();
    		}
    	}
    	return result;
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#onVisibilityChange(boolean)
     */
    protected void onVisibilityChange(boolean visible) {
    	screenProvider.onVisibilityChange(visible);
    }
    
    // Create menu items
    private MenuItem configItem = new MenuItem(resources, LogicMailResource.MENUITEM_CONFIGURATION, 10020, 10) {
        public void run() {
            showConfigScreen();
        }
    };
    private MenuItem aboutItem = new MenuItem(resources, LogicMailResource.MENUITEM_ABOUT, 10050, 10) {
        public void run() {
            // Show the about dialog
        	AboutDialog dialog = new AboutDialog();
        	dialog.doModal();
        }
    };
    private MenuItem closeItem = new MenuItem(resources, LogicMailResource.MENUITEM_CLOSE, 200000, 10) {
        public void run() {
        	// TODO: Deal with closing/hiding while still running
        	
            StandardScreen.super.onClose();
        }
    };
    private MenuItem exitItem = new MenuItem(resources, LogicMailResource.MENUITEM_EXIT, 200001, 10) {
        public void run() {
        	tryShutdownApplication();
        }
    };

    public void tryShutdownApplication() {
    	// Get all accounts
    	AccountNode[] accounts = MailManager.getInstance().getMailRootNode().getAccounts();
    	
    	// Find out of we still have an open connection
    	boolean openConnection = false;
    	for(int i=0; i<accounts.length; i++) {
    		if(accounts[i].getStatus() == AccountNode.STATUS_ONLINE) {
    			openConnection = true;
    			break;
    		}
    	}
    	
        if(openConnection) {
            if(Dialog.ask(Dialog.D_YES_NO, resources.getString(LogicMailResource.BASE_CLOSEANDEXIT)) == Dialog.YES) {
            	for(int i=0; i<accounts.length; i++) {
            		if(accounts[i].getStatus() == AccountNode.STATUS_ONLINE) {
            			accounts[i].requestDisconnect(true);
            		}
            	}
                headerField.removeListeners();
                NotificationHandler.getInstance().shutdown();
                System.exit(0);
            }
        }
        else {
            headerField.removeListeners();
            NotificationHandler.getInstance().shutdown();
            System.exit(0);
        }
    }
    
    /**
     * Shows the configuration screen.
     * Subclasses should override this method if they need to
     * refresh their view of the configuration after the screen
     * is closed.
     */
    protected void showConfigScreen() {
        UiApplication.getUiApplication().pushModalScreen(new ConfigScreen());
    }
    
    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    protected void makeMenu(Menu menu, int instance) {
    	screenProvider.makeMenu(menu, instance);
        menu.addSeparator();
        menu.add(configItem);
        menu.add(aboutItem);
        menu.add(closeItem);
        menu.add(exitItem);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#onSavePrompt()
     */
    protected boolean onSavePrompt() {
        return screenProvider.onSavePrompt();
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#navigationClick(int, int)
     */
    protected boolean navigationClick(int status, int time) {
    	boolean result = screenProvider.navigationClick(status, time);
    	if(!result) {
    		result = super.navigationClick(status, time);
    	}
    	return result;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
    protected boolean keyChar(char c, int status, int time) {
    	boolean result = screenProvider.keyChar(c, status, time);
    	if(!result) {
    		result = super.keyChar(c, status, time);
    	}
    	return result;
    }
    
	/**
	 * Invoked when there is a change in status from
	 * the mail connection.
	 *
	 * @param e Status event data
	 */
    protected void mailConnectionListener_MailConnectionStatus(MailConnectionStatusEvent e) {
		UiApplication.getUiApplication().invokeLater(new EventObjectRunnable(e) {
			public void run() {
		    	String message = ((MailConnectionStatusEvent)getEvent()).getMessage();
	    		if(isExposed) {
	    			statusBarField.setStatusText(message);
	    			if(statusBarField.hasStatus()) {
	    				StandardScreen.this.setStatus(statusBarField);
	    			}
	    			else {
	    				StandardScreen.this.setStatus(null);
	    			}
	    		}
			}
		});
    }
    
	/**
	 * Invoked when an error occurs with the mail connection.
	 *
	 * @param e Error event data
	 */
    protected void mailConnectionListener_MailConnectionError(MailConnectionStatusEvent e) {
		UiApplication.getUiApplication().invokeLater(new EventObjectRunnable(e) {
			public void run() {
				String message = ((MailConnectionStatusEvent)getEvent()).getMessage();
				if(message == null) { message = resources.getString(LogicMailResource.ERROR_UNKNOWN); }
	            try {
	                Screen activeScreen =
	                        UiApplication.getUiApplication().getActiveScreen();
	                if(activeScreen instanceof Status) {
	                    UiApplication.getUiApplication().popScreen(activeScreen);
	                }
	            } catch (Exception e) { }
	            Status.show(message, 5000);
			}
		});
    }
    
	/**
	 * Invoked when the mail connection needs login
	 * information to be provided by the user interface.
	 * 
	 * @param e Login event data
	 */
    protected void mailConnectionListener_MailConnectionLogin(MailConnectionLoginEvent e) {
		UiApplication.getUiApplication().invokeAndWait(new EventObjectRunnable(e) {
			public void run() {
				MailConnectionLoginEvent e = (MailConnectionLoginEvent)getEvent();
				LoginDialog dialog = new LoginDialog(e.getUsername(), e.getPassword());
				if(dialog.doModal() == Dialog.OK) {
					e.setUsername(dialog.getUsername());
					e.setPassword(dialog.getPassword());
				}
				else {
					e.setCanceled(true);
				}
			}
		});
    }
}
