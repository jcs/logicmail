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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.SocketConnection;

import net.rim.device.api.crypto.tls.tls10.TLS10Connection;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.EventLogger;
import net.rim.device.cldc.io.ssl.TLSException;

import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;

public abstract class AbstractNetworkConnector implements NetworkConnector {
    protected static ResourceBundle resources = ResourceBundle.getBundle(LogicMailResource.BUNDLE_ID, LogicMailResource.BUNDLE_NAME);
    
    /** Select everything except WiFi */
    protected static final int TRANSPORT_AUTO = 0xFE;
    /** Select WiFi */
    protected static final int TRANSPORT_WIFI = 0x01;
    /** Select Direct TCP */
    protected static final int TRANSPORT_DIRECT_TCP = 0x02;
    /** Select MDS */
    protected static final int TRANSPORT_MDS = 0x04;
    /** Select WAP 2.0 */
    protected static final int TRANSPORT_WAP2 = 0x08;
    
    protected final GlobalConfig globalConfig;
    protected final ConnectionConfig connectionConfig;

    protected String serverName;
    protected int serverPort;
    protected boolean useSSL;
    protected int transports;
    private String connectionUrl;
    
    private SocketConnection socket;
    
    private static String CRLF = "\r\n";

    protected AbstractNetworkConnector(GlobalConfig globalConfig, ConnectionConfig connectionConfig) {
        this.globalConfig = globalConfig;
        this.connectionConfig = connectionConfig;
    }
    
    public Connection open(ConnectionConfig connectionConfig) throws IOException {
        this.serverName = connectionConfig.getServerName();
        this.serverPort = connectionConfig.getServerPort();
        this.useSSL = (connectionConfig.getServerSecurity() == ConnectionConfig.SECURITY_SSL);
        
        initalizeSelectedTransportSet(connectionConfig);
        
        try {
            socket = openSocketConnection();
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID, e.getMessage().getBytes(), EventLogger.ERROR);
            throw new WrappedIOException(resources.getString(LogicMailResource.ERROR_UNABLE_TO_OPEN_CONNECTION), e);
        }
        
        logOpenConnection();
        
        Connection connection = new Connection(socket);
        return connection;
    }

    private void initalizeSelectedTransportSet(ConnectionConfig connectionConfig) {
        int transportType;
        boolean enableWiFi;
        if(connectionConfig.getTransportType() == ConnectionConfig.TRANSPORT_GLOBAL) {
            transportType = globalConfig.getTransportType();
            enableWiFi = globalConfig.getEnableWiFi();
        }
        else {
            transportType = connectionConfig.getTransportType();
            enableWiFi = connectionConfig.getEnableWiFi();
        }

        // Populate the bit-flags for the selected transport types
        // based on the configuration parameters.
        switch(transportType) {
        case ConnectionConfig.TRANSPORT_WIFI_ONLY:
            transports = TRANSPORT_WIFI;
            break;
        case ConnectionConfig.TRANSPORT_AUTO:
            transports = TRANSPORT_AUTO;
            break;
        case ConnectionConfig.TRANSPORT_DIRECT_TCP:
            transports = TRANSPORT_DIRECT_TCP;
            break;
        case ConnectionConfig.TRANSPORT_MDS:
            transports = TRANSPORT_MDS;
            break;
        case ConnectionConfig.TRANSPORT_WAP2:
            transports = TRANSPORT_WAP2;
            break;
        default:
            // Should only get here in rare cases of invalid configuration
            // data, so we select full automatic with WiFi.
            transports = TRANSPORT_AUTO;
            enableWiFi = true;
            break;
        }
        if(enableWiFi) { transports |= TRANSPORT_WIFI; }
    }

    private void logOpenConnection() throws IOException {
        if (EventLogger.getMinimumLevel() >= EventLogger.INFORMATION) {
            StringBuffer buf = new StringBuffer();
            buf.append("Connection established:\r\n");
            buf.append("Socket: ").append(socket.getClass().toString()).append(CRLF);
            buf.append("Local address: ").append(((SocketConnection)socket).getLocalAddress()).append(CRLF);
            buf.append("URL: ").append(connectionUrl);
            EventLogger.logEvent(AppInfo.GUID, buf.toString().getBytes(),
                    EventLogger.INFORMATION);
        }
    }
    
    /**
     * Open a socket connection.
     * This method should encapsulate all platform-specific logic for opening
     * network connections.  If the connection is successfully opened, this
     * method should also call {@link #setConnectionUrl(String)} to set the
     * chosen connection string.
     * 
     * @return the socket connection
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected abstract SocketConnection openSocketConnection() throws IOException;
    
    /**
     * Sets the connection URL that was used to open the socket connection.
     *
     * @param connectionUrl the new connection URL
     */
    protected void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }
    
    public Connection getConnectionAsTLS(Connection connection) throws IOException {
        SocketConnection socket = connection.getSocket();
        
        // Shortcut the method if we're already in SSL mode
        if(socket instanceof TLS10Connection) { return connection; }
        
        if(socket == null || connectionUrl == null) {
            throw new IOException("Connection has not been opened");
        }

        try {
            TLS10Connection tlsSocket = new TLS10Connection(
                    new SocketConnectionWrapper(
                            socket,
                            (DataInputStream)connection.getInput(),
                            (DataOutputStream)connection.getOutput()),
                            connectionUrl,
                            true);

            Connection tlsConnection = new Connection(tlsSocket);
            return tlsConnection;
            
        } catch (IOException e) {
            EventLogger.logEvent(AppInfo.GUID,
                    ("Unable to switch to TLS mode: " + e.getMessage()).getBytes(), EventLogger.ERROR);
            throw new WrappedIOException(resources.getString(LogicMailResource.ERROR_UNABLE_TO_SWITCH_TO_TLS_MODE), e);
        } catch (TLSException e) {
            EventLogger.logEvent(AppInfo.GUID,
                    ("Unable to switch to TLS mode: " + e.getMessage()).getBytes(), EventLogger.ERROR);
            throw new WrappedIOException(resources.getString(LogicMailResource.ERROR_UNABLE_TO_SWITCH_TO_TLS_MODE), e);
        }
    }
    
    /**
     * Decorator to wrap an existing socket connection so its I/O streams
     * can be reopened without throwing exceptions.
     */
    private static class SocketConnectionWrapper implements SocketConnection {
        private SocketConnection socket;
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;

        public SocketConnectionWrapper(SocketConnection socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
            this.socket = socket;
            this.dataInputStream = dataInputStream;
            this.dataOutputStream = dataOutputStream;
        }

        public DataInputStream openDataInputStream() throws IOException {
            return dataInputStream;
        }
        public InputStream openInputStream() throws IOException {
            return dataInputStream;
        }
        public void close() throws IOException {
            socket.close();
        }
        public DataOutputStream openDataOutputStream() throws IOException {
            return dataOutputStream;
        }
        public OutputStream openOutputStream() throws IOException {
            return dataOutputStream;
        }
        public String getAddress() throws IOException {
            return socket.getAddress();
        }
        public String getLocalAddress() throws IOException {
            return socket.getLocalAddress();
        }
        public int getLocalPort() throws IOException {
            return socket.getLocalPort();
        }
        public int getPort() throws IOException {
            return socket.getPort();
        }
        public int getSocketOption(byte option) throws IllegalArgumentException, IOException {
            return socket.getSocketOption(option);
        }
        public void setSocketOption(byte option, int value) throws IllegalArgumentException, IOException {
            socket.setSocketOption(option, value);
        }
    }
}
