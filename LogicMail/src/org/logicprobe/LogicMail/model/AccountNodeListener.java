package org.logicprobe.LogicMail.model;

import org.logicprobe.LogicMail.util.EventListener;

/**
 * Listener for AccountNode events.
 */
public interface AccountNodeListener extends EventListener {
	/**
	 * Invoked when the status of an account has changed.
	 * 
	 * @param e Account node event data
	 */
	public void accountStatusChanged(AccountNodeEvent e);
}
