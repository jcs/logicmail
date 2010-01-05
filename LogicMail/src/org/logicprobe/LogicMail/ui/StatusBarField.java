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

import java.util.Timer;
import java.util.TimerTask;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;

/**
 * Provides a status bar with an animated throbber.
 */
public class StatusBarField extends Field {
    private int preferredHeight;
    private String statusText;
    private ThrobberRenderer throbberRenderer;
    private Timer timer;
    private TimerTask timerTask;

    /**
     * Instantiates a new status bar field.
     */
    public StatusBarField() {
        super(Field.USE_ALL_WIDTH);

        preferredHeight = Font.getDefault().getHeight() + 2;
        throbberRenderer = new ThrobberRenderer(preferredHeight);
        this.timer = new Timer();
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#onDisplay()
     */
    protected void onDisplay() {
        super.onDisplay();
        timerTask = new AnimationTimerTask();
        timer.scheduleAtFixedRate(timerTask, 200, 100);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#onUndisplay()
     */
    protected void onUndisplay() {
        timerTask.cancel();
        super.onUndisplay();
    }

    /**
     * Sets the status text.
     * 
     * @param statusText the new status text
     */
    public void setStatusText(String statusText) {
        if(statusText==null || !statusText.equals(this.statusText)) {
            this.statusText = statusText;
            this.invalidate();
        }
    }

    /**
     * Gets the status text.
     * 
     * @return the status text
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * Checks whether status has been set.
     * 
     * @return true, if set
     */
    public boolean hasStatus() {
        return (statusText != null && statusText.length() > 0);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#layout(int, int)
     */
    protected void layout(int width, int height) {
        setExtent(getPreferredWidth(), getPreferredHeight());
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#getPreferredWidth()
     */
    public int getPreferredWidth() {
        return Graphics.getScreenWidth();
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#getPreferredHeight()
     */
    public int getPreferredHeight() {
        return preferredHeight;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Field#paint(net.rim.device.api.ui.Graphics)
     */
    protected void paint(Graphics graphics) {
        int width = this.getPreferredWidth();
        int throbberSize = throbberRenderer.getSize();
        int backgroundColor = graphics.getBackgroundColor();
        graphics.setBackgroundColor(Color.LIGHTGREY);
        graphics.clear();

        graphics.drawRect(0, 0, width, preferredHeight);

        if(statusText != null && statusText.length() > 0) {
            graphics.drawText(statusText, 1, 1, Graphics.ELLIPSIS, width - throbberSize - 2);
        }
        graphics.pushRegion(width - throbberSize, 0, throbberSize, throbberSize, 0, 0);
        throbberRenderer.paint(graphics);
        graphics.popContext();
        graphics.setBackgroundColor(backgroundColor);
    }

    /**
     * Internal timer task class to support animation.
     */
    private class AnimationTimerTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        public void run() {
            StatusBarField.this.throbberRenderer.nextPosition();
            StatusBarField.this.invalidate();
        }
    }
}
