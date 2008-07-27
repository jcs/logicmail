package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.logicprobe.LogicMail.message.Message;

public class OutgoingMailConnectionHandler extends AbstractMailConnectionHandler {
	private OutgoingMailClient outgoingClient;
	private Timer connectionTimer = null;
	
	/** Two minute timeout */
	private static final int CONNECTION_TIMEOUT = 120 * 1000;
	
	// The various mail sender requests, mirroring the
	// "requestXXXX()" methods from AbstractMailSender
	public static final int REQUEST_SEND_MESSAGE = 10;
	
	public OutgoingMailConnectionHandler(OutgoingMailClient client) {
		super(client);
		this.outgoingClient = client;
	}

	private TimerTask connectionTimerTask = new TimerTask() {
		public void run() {
			setConnectionState(STATE_CLOSING);
		}
	};
	
	/**
	 * Handles a specific request during the REQUESTS state.
	 * 
	 * @param type Type identifier for the request.
	 * @param params Parameters for the request.
     * @throw IOException on I/O errors
     * @throw MailException on protocol errors
	 */
	protected void handleRequest(int type, Object[] params) throws IOException, MailException {
		if(connectionTimer != null) {
			connectionTimer.cancel();
			connectionTimer = null;
		}
		switch(type) {
		case REQUEST_SEND_MESSAGE:
			handleRequestSendMessage(
					(Message)params[0]);
			break;
		}
	}

	/**
	 * Handles the start of the IDLE state.
	 */
	protected void handleBeginIdle() {
		if(connectionTimer != null) {
			connectionTimer.cancel();
		}
		connectionTimer = new Timer();
		connectionTimer.schedule(connectionTimerTask, CONNECTION_TIMEOUT);
	}
	
	/**
	 * Called at the start of the CLOSING state.
	 */
	protected void handleBeforeClosing() {
		if(connectionTimer != null) {
			connectionTimer.cancel();
			connectionTimer = null;
		}
	}
	
	private void handleRequestSendMessage(Message message) throws IOException, MailException {
		String messageSource = outgoingClient.sendMessage(message);
		
		MailConnectionHandlerListener listener = getListener();
		if(messageSource != null && messageSource.length() > 0 && listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_SEND_MESSAGE, new Object[] { message, messageSource });
		}
	}
}
