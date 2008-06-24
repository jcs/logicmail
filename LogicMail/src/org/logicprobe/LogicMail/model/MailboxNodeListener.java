package org.logicprobe.LogicMail.model;

import org.logicprobe.LogicMail.util.EventListener;

/**
 * Listener for MailboxNode events.
 */
public interface MailboxNodeListener extends EventListener {
	/**
	 * Invoked when the status of a mailbox has changed.
	 * 
	 * @param e Mailbox node event data
	 */
	public void mailboxStatusChanged(MailboxNodeEvent e);
}
