package org.logicprobe.LogicMail.model;

import org.logicprobe.LogicMail.util.EventListener;

/**
 * Listener for MailboxNode events.
 */
public interface MessageNodeListener extends EventListener {
	/**
	 * Invoked when the status of a message has changed.
	 * 
	 * @param e Message node event data
	 */
	public void messageStatusChanged(MessageNodeEvent e);
}
