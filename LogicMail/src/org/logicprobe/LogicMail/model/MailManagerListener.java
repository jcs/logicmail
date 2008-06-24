package org.logicprobe.LogicMail.model;

import org.logicprobe.LogicMail.util.EventListener;

/**
 * Listener for MailManager events.
 */
public interface MailManagerListener extends EventListener {
	/**
	 * Invoked when the configuration of the mail system has changed.
	 * 
	 * @param e MailManager event data
	 */
	public void mailConfigurationChanged(MailManagerEvent e);
}
