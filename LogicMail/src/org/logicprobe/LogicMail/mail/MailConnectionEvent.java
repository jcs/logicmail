package org.logicprobe.LogicMail.mail;

import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.util.EventObject;

/**
 * Object for mail connection events.
 */
public class MailConnectionEvent extends EventObject {
	private ConnectionConfig connectionConfig;
	
	/** Creates a new instance of MailConnectionStateEvent */
	public MailConnectionEvent(Object source, ConnectionConfig connectionConfig) {
		super(source);
		this.connectionConfig = connectionConfig;
	}

	/**
	 * Gets the connection configuration associated with this event.
	 * <p>
	 * This allows classes higher in the system to be capable of
	 * determining which connection was ultimately the source of
	 * this event.
	 * </p>
	 * 
	 * @return Connection configuration
	 */
	public ConnectionConfig getConnectionConfig() {
		return this.connectionConfig;
	}
}
