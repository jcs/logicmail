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

import javax.microedition.io.StreamConnection;

import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.IntVector;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.conf.ConnectionConfig;

public class ConnectionBB50 extends Connection {
    private ConnectionFactory connectionFactory;
    
    public ConnectionBB50(ConnectionConfig connectionConfig) {
        super(connectionConfig);
        
        // Create a new connection factory instance
        connectionFactory = new ConnectionFactory();
        
        // Prepare the preferred transports set
        IntVector preferredTransports = new IntVector();
        if((transports & TRANSPORT_WIFI) != 0) {
            preferredTransports.addElement(TransportInfo.TRANSPORT_TCP_WIFI);
        }
        if((transports & TRANSPORT_DIRECT_TCP) != 0) {
            preferredTransports.addElement(TransportInfo.TRANSPORT_TCP_CELLULAR);
        }
        if((transports & TRANSPORT_MDS) != 0) {
            preferredTransports.addElement(TransportInfo.TRANSPORT_MDS);
        }
        if((transports & TRANSPORT_WAP2) != 0) {
            preferredTransports.addElement(TransportInfo.TRANSPORT_WAP2);
        }
        connectionFactory.setPreferredTransportTypes(preferredTransports.toArray());
    }

    protected StreamConnection openStreamConnection() throws IOException {
        // Build the simple connection string
        StringBuffer buf = new StringBuffer();
        buf.append(useSSL ? "ssl" : "socket");
        buf.append("://");
        buf.append(serverName);
        buf.append(':');
        buf.append(serverPort);
        String url = buf.toString();

        // Get the connection through the connection factory
        ConnectionDescriptor descriptor = connectionFactory.getConnection(url);
        
        if(descriptor != null) {
            StreamConnection connection = (StreamConnection)descriptor.getConnection();
            logConnectionInformation(descriptor);
            setConnectionUrl(descriptor.getUrl());
            return connection;
        }
        else {
            return null;
        }
    }

    private void logConnectionInformation(ConnectionDescriptor descriptor) {
        // Log the information for the connection
        if (EventLogger.getMinimumLevel() >= EventLogger.INFORMATION) {
            StringBuffer buf = new StringBuffer();
            buf.append("Opened connection:\r\n");
            
            int transportType = descriptor.getTransportDescriptor().getTransportType();
            if(transportType == TransportInfo.TRANSPORT_TCP_WIFI) { buf.append("TCP_WIFI"); }
            else if(transportType == TransportInfo.TRANSPORT_TCP_CELLULAR) { buf.append("TCP_CELLULAR"); }
            else if(transportType == TransportInfo.TRANSPORT_MDS) { buf.append("MDS"); }
            else if(transportType == TransportInfo.TRANSPORT_WAP2) { buf.append("WAP2"); }
            
            buf.append("\r\n");
            buf.append(descriptor.getUrl());
            
            EventLogger.logEvent(AppInfo.GUID, buf.toString().getBytes(),
                    EventLogger.INFORMATION);
        }
    }
}
