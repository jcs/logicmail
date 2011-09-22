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
package org.logicprobe.LogicMail.mail;

import java.io.IOException;

import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.util.Connection;
import org.logicprobe.LogicMail.util.NetworkConnector;
import org.logicprobe.LogicMail.util.UtilFactory;

import net.rim.device.api.system.WLANInfo;

class NetworkHandoverRequest extends NetworkMailStoreRequest implements MailStoreRequest {
    private String attemptedSSID;
    private long attemptTime;
    
    NetworkHandoverRequest(NetworkMailStore mailStore) {
        super(mailStore);
    }

    protected String getInitialStatus() {
        return null;
    }

    public String getAttemptedSSID() {
        return attemptedSSID;
    }

    public long getAttemptTime() {
        return attemptTime;
    }

    public void execute(MailClient client) throws IOException, MailException {
        IncomingMailClient incomingClient = (IncomingMailClient)client;
        if(!prepareForHandover(incomingClient)) {
            fireMailStoreRequestFailed(null, true);
            return;
        }
        
        Connection connection;
        try {
            NetworkConnector connector = UtilFactory.getInstance().getNetworkConnector(
                    MailSettings.getInstance().getGlobalConfig(),
                    incomingClient.getAcctConfig());
            connection = connector.open(incomingClient.getAcctConfig(), true);
            if(connection.getConnectionType() != ConnectionConfig.TRANSPORT_WIFI_ONLY) {
                connection.close();
                connection = null;
            }
        } catch (IOException e) {
            fireMailStoreRequestFailed(e, true);
            return;
        }
        
        if(connection == null) {
            fireMailStoreRequestFailed(null, true);
        }
        
        incomingClient.close();
        incomingClient.open(connection);
        fireMailStoreRequestComplete();
    }

    private boolean prepareForHandover(IncomingMailClient incomingClient) {
        if(incomingClient.getConnectionType() != ConnectionConfig.TRANSPORT_WIFI_ONLY
                && WLANInfo.getWLANState() == WLANInfo.WLAN_STATE_CONNECTED) {
            WLANInfo.WLANAPInfo apInfo = WLANInfo.getAPInfo();
            if(apInfo != null) {
                attemptedSSID = apInfo.getSSID();
                attemptTime = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }
}
