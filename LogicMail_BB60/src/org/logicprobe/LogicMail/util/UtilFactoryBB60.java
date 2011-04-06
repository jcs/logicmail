/*-
 * Copyright (c) 2011, Derek Konigsberg
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
package org.logicprobe.LogicMail.util;

import org.logicprobe.LogicMail.ui.KeyHandler;

import net.rim.device.api.system.Application;
import net.rim.device.api.system.Sensor;
import net.rim.device.api.system.SensorListener;
import net.rim.device.api.ui.UiApplication;

public class UtilFactoryBB60 extends UtilFactoryBB50 {
    public UtilFactoryBB60() {
        super();
    }

    private static SensorListener slideSensorListener = new SensorListener() {
        public void onSensorUpdate(int sensorId, int update) {
            if(sensorId != Sensor.SLIDE) { return; }
            if(update == Sensor.STATE_SLIDE_OPEN | update == Sensor.STATE_SLIDE_CLOSED) {
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        KeyHandler.buildTable();
                    }
                });
            }
        }
    };
    
    public void addSensorListeners() {
        super.addSensorListeners();
        if(Sensor.isSupported(Sensor.SLIDE)) {
            Sensor.addListener(Application.getApplication(), slideSensorListener, Sensor.SLIDE);
        }
    }
    
    public void removeSensorListeners() {
        if(Sensor.isSupported(Sensor.SLIDE)) {
            Sensor.removeListener(Application.getApplication(), slideSensorListener);
        }
        super.removeSensorListeners();
    }
}
