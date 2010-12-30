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

import javax.microedition.io.SocketConnection;

import net.rim.device.api.io.transport.ConnectionAttemptListener;
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.ConnectionFactory;
import net.rim.device.api.io.transport.TransportDescriptor;
import net.rim.device.api.io.transport.TransportInfo;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.IntVector;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;

public class NetworkConnectorBB50 extends AbstractNetworkConnector {

    public NetworkConnectorBB50(GlobalConfig globalConfig, ConnectionConfig connectionConfig) {
        super(globalConfig, connectionConfig);
    }
    
    private Exception connectionException;
    
    protected SocketConnection openSocketConnection() throws IOException {
        // Create a new connection factory instance
        ConnectionFactory connectionFactory = new ConnectionFactory();
        
        // Prepare the preferred transports set
        setPreferredTransports(connectionFactory);
        
        // Build the simple connection string
        String url = buildSimpleConnectionString();

        connectionException = null;
        connectionFactory.setConnectionAttemptListener(new ConnectionAttemptListener() {
            public boolean attempting(TransportDescriptor transport, int attemptNumber, String url) {
                connectionException = null;
                return true;
            }
            public void attemptSucceeded(int attemptNumber, ConnectionDescriptor connection) {
                connectionException = null;
            }
            public void attemptAborted(String url, Exception exception) {
                connectionException = exception;
            }
            public void attemptFailed(TransportDescriptor transport, int attemptNumber, String url, Exception exception) {
                connectionException = exception;
            }
        });
        
        // Get the connection through the connection factory
        ConnectionDescriptor descriptor = connectionFactory.getConnection(url);
        
        if(descriptor != null) {
            SocketConnection connection = (SocketConnection)descriptor.getConnection();
            logConnectionInformation(descriptor);
            setConnectionUrl(descriptor.getUrl());
            return connection;
        }
        else {
            throw new WrappedIOException(resources.getString(LogicMailResource.ERROR_UNABLE_TO_OPEN_CONNECTION), connectionException);
        }
    }

    private void setPreferredTransports(ConnectionFactory connectionFactory) {
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

    private String buildSimpleConnectionString() {
        StringBuffer buf = new StringBuffer();
        buf.append(useSSL ? "ssl" : "socket");
        buf.append("://");
        buf.append(serverName);
        buf.append(':');
        buf.append(serverPort);
        String url = buf.toString();
        return url;
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
