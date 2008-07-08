package org.logicprobe.LogicMail.mail;

import org.logicprobe.LogicMail.conf.ConnectionConfig;

/**
 * Object for mail connection state change events.
 */
public class MailConnectionStateEvent extends MailConnectionEvent {
	private int state;
	
	public static final int STATE_DISCONNECTED = 0;
	public static final int STATE_CONNECTED    = 1;
	
	/** Creates a new instance of MailConnectionStateEvent */
	public MailConnectionStateEvent(Object source, ConnectionConfig connectionConfig, int state) {
		super(source, connectionConfig);
		this.state = state;
	}

	/**
	 * Gets the new state of this connection.
	 * 
	 * @return Connection state.
	 */
	public int getState() {
		return state;
	}
}
