package org.logicprobe.LogicMail.mail;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.logicprobe.LogicMail.util.Queue;

public class OutgoingMailConnectionHandler extends AbstractMailConnectionHandler {
	private Timer connectionTimer = null;
	
	/** Two minute timeout */
	private static final int CONNECTION_TIMEOUT = 120 * 1000;
	
	public OutgoingMailConnectionHandler(OutgoingMailClient client) {
		super(client);
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

	protected void handleRequest(ConnectionHandlerRequest request) throws IOException, MailException {
        if(connectionTimerTask != null) {
            connectionTimerTask.cancel();
            connectionTimerTask = null;
        }
	    super.handleRequest(request);
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
	}
	
	protected void handleEndIdle() throws IOException, MailException {
	    // No specific commands necessary here
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
}
