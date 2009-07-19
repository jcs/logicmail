package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.Queue;

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
		this.connectionTimer = new Timer();
	}

	private TimerTask connectionTimerTask;
	
	private class ConnectionTimerTask extends TimerTask {
		public void run() {
			Queue requestQueue = getRequestQueue();
			synchronized(requestQueue) {
				setConnectionState(STATE_CLOSING);
				requestQueue.notifyAll();
			}
		}
	}
	
	/**
	 * Handles a specific request during the REQUESTS state.
	 * 
	 * @param type Type identifier for the request.
	 * @param params Parameters for the request.
     * @throws IOException on I/O errors
     * @throws MailException on protocol errors
	 */
	protected void handleRequest(int type, Object[] params) throws IOException, MailException {
		if(connectionTimerTask != null) {
			connectionTimerTask.cancel();
			connectionTimerTask = null;
		}
		switch(type) {
		case REQUEST_SEND_MESSAGE:
			handleRequestSendMessage(
					(MessageEnvelope)params[0], (Message)params[1]);
			break;
		}
	}

	/**
	 * Handles the start of the IDLE state.
	 */
	protected void handleBeginIdle() {
		if(connectionTimerTask != null) {
			connectionTimerTask.cancel();
			connectionTimerTask = null;
		}
		connectionTimerTask = new ConnectionTimerTask();
		connectionTimer.schedule(connectionTimerTask, CONNECTION_TIMEOUT);
		
		Queue requestQueue = getRequestQueue();
		synchronized(requestQueue) {
			if(requestQueue.element() != null) {
				return;
			}
			else {
				try {
					requestQueue.wait();
				} catch (InterruptedException e) { }
			}
		}
	}
	
	/**
	 * Called at the start of the CLOSING state.
	 */
	protected void handleBeforeClosing() {
		if(connectionTimerTask != null) {
			connectionTimerTask.cancel();
			connectionTimerTask = null;
		}
	}
	
	private void handleRequestSendMessage(MessageEnvelope envelope, Message message) throws IOException, MailException {
		showStatus(resources.getString(LogicMailResource.MAILCONNECTION_REQUEST_SEND_MESSAGE));
		String messageSource = outgoingClient.sendMessage(envelope, message);
		
		MailConnectionHandlerListener listener = getListener();
		if(messageSource != null && messageSource.length() > 0 && listener != null) {
			listener.mailConnectionRequestComplete(REQUEST_SEND_MESSAGE, new Object[] { envelope, message, messageSource });
		}
	}
}
