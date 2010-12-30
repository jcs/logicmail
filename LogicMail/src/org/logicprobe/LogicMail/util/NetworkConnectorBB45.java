/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import net.rim.device.api.servicebook.ServiceBook;
import net.rim.device.api.servicebook.ServiceRecord;
import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.system.WLANInfo;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;

public class NetworkConnectorBB45 extends AbstractNetworkConnector {
    
    /** Stores transport ServiceBooks if found. Otherwise, null */
    private ServiceRecord srMDS, srWAP2, srWiFi;
    /** Flags indicating the coverage status of each transport */
    protected boolean coverageTCP=false, coverageMDS=false, coverageWAP2=false, coverageWiFi=false;
    
    public NetworkConnectorBB45(GlobalConfig globalConfig, ConnectionConfig connectionConfig) {
        super(globalConfig, connectionConfig);
    }
    
    protected SocketConnection openSocketConnection() throws IOException {
        initializeTransportAvailability();
        
        String urlBase = buildConnectionStringBase();
        
        SocketConnection connection = attemptToOpenConnection(urlBase);
        
        return connection;
    }
    
    /**
     * Initializes the ServiceRecord instances for each transport (if available). Otherwise leaves it null.
     * Also determines if sufficient coverage is available for each transport and sets coverage* flags.
     */
    private void initializeTransportAvailability() {
        ServiceRecord[] records = getServiceRecords();

        checkForKnownRecordTypes(records);
        
        initializeCoverage();
        
        logAvailableTransports();
    }

    private ServiceRecord[] getServiceRecords() {
        ServiceBook sb = ServiceBook.getSB();
        ServiceRecord[] records = sb.getRecords();
        return records;
    }

    private void checkForKnownRecordTypes(ServiceRecord[] records) {
        for (int i = 0; i < records.length; i++) {
            ServiceRecord myRecord = records[i];

            if (myRecord.isValid() && !myRecord.isDisabled()) {
                String cid = myRecord.getCid().toLowerCase();
                String uid = myRecord.getUid().toLowerCase();
                // BES
                if (cid.indexOf("ippp") != -1 && uid.indexOf("gpmds") == -1) {
                    srMDS = myRecord;
                }
                // WiFi
                if (cid.indexOf("wptcp") != -1 && uid.indexOf("wifi") != -1) {
                    srWiFi = myRecord;
                }               
                // Wap2
                if (cid.indexOf("wptcp") != -1 && uid.indexOf("wap2") != -1) {
                    srWAP2 = myRecord;
                }
            }
        }
    }

    /**
     * Coverage check APIs change on different OS versions, so this method
     * exists to allow subclasses to override as necessary.
     */
    protected void initializeCoverage() {
        if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_DIRECT)) {
            coverageTCP=true;
            coverageWAP2=true;
        }
        if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_MDS)) {           
            coverageMDS=true;
        }   
        if(WLANInfo.getWLANState()==WLANInfo.WLAN_STATE_CONNECTED) {
            coverageWiFi = true;
        }
    }

    private void logAvailableTransports() {
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            StringBuffer buf = new StringBuffer();
            buf.append("Availability: ");
            buf.append("MDS=").append(srMDS != null);
            buf.append(", WAP2=").append(srWAP2 != null);
            buf.append(", WiFi=").append(srWiFi != null);
            buf.append("\r\n");
            buf.append("Coverage: ");
            buf.append("TCP=").append(coverageTCP);
            buf.append(", MDS=").append(coverageMDS);
            buf.append(", WAP2=").append(coverageWAP2);
            buf.append(", WiFi=").append(coverageWiFi);
            EventLogger.logEvent(AppInfo.GUID, buf.toString().getBytes(),
                    EventLogger.DEBUG_INFO);
        }
    }
    
    private String buildConnectionStringBase() {
        StringBuffer buf = new StringBuffer();
        buf.append(useSSL ? "ssl" : "socket");
        buf.append("://");
        buf.append(serverName);
        buf.append(':');
        buf.append(serverPort);
        String urlBase = buf.toString();
        return urlBase;
    }

    private SocketConnection attemptToOpenConnection(String urlBase) throws IOException {
        SocketConnection connection = null;
        if(((transports & TRANSPORT_WIFI) != 0) && coverageWiFi) {
            connection = attemptWiFi(urlBase);
        }
        if(connection == null && ((transports & TRANSPORT_DIRECT_TCP) != 0) && coverageTCP) {
            connection = attemptDirectTCP(urlBase);
        }
        if(connection == null && ((transports & TRANSPORT_MDS) != 0) && srMDS != null && coverageMDS) {
            connection = attemptMDS(urlBase);
        }
        if(connection == null && ((transports & TRANSPORT_WAP2) != 0) && srWAP2 != null && coverageWAP2) {
            connection = attemptWAP2(urlBase);
        }
        return connection;
    }
    
    private SocketConnection attemptWiFi(String urlBase) throws IOException {
        String connectStr = urlBase + ";interface=wifi";
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(AppInfo.GUID, "Attempting WiFi".getBytes(),
                    EventLogger.DEBUG_INFO);
        }
        SocketConnection socket = openSocket(connectStr);
        return socket;
    }

    private SocketConnection attemptDirectTCP(String urlBase) throws IOException {
        String connectStr = urlBase + ";deviceside=true";
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(AppInfo.GUID, "Attempting Direct TCP".getBytes(),
                    EventLogger.DEBUG_INFO);
        }
        SocketConnection socket = openSocket(connectStr);
        return socket;
    }

    private SocketConnection attemptMDS(String urlBase) throws IOException {
        String connectStr = urlBase + ";deviceside=false";
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(AppInfo.GUID, "Attempting MDS".getBytes(),
                    EventLogger.DEBUG_INFO);
        }
        SocketConnection socket = openSocket(connectStr);
        return socket;
    }

    private SocketConnection attemptWAP2(String urlBase) throws IOException {
        String connectStr = urlBase + ";deviceside=true;ConnectionUID=" + srWAP2.getUid();
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(AppInfo.GUID, "Attempting WAP2".getBytes(),
                    EventLogger.DEBUG_INFO);
        }
        SocketConnection socket = openSocket(connectStr);
        return socket;
    }

    private SocketConnection openSocket(String connectStr) throws IOException {
        SocketConnection socket = (SocketConnection) Connector.open(
                connectStr,
                Connector.READ_WRITE, true);
        setConnectionUrl(connectStr);
        return socket;
    }
}
