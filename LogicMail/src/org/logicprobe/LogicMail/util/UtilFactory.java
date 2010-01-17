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
import java.util.Vector;

import org.logicprobe.LogicMail.conf.ConnectionConfig;

/**
 * Creates platform-specific instances of various utility classes.
 */
public abstract class UtilFactory {
    private static UtilFactory instance;

    /**
     * Array of concrete ConnectionFactory classes, in order from the highest
     * API version to the lowest.
     */
    private static String[] factoryClasses = {
        "org.logicprobe.LogicMail.util.UtilFactoryBB50",
        "org.logicprobe.LogicMail.util.UtilFactoryBB47",
        "org.logicprobe.LogicMail.util.UtilFactoryBB46",
        "org.logicprobe.LogicMail.util.UtilFactoryBB42"
    };

    private Vector openConnections = new Vector();

    /**
     * Gets the single instance of UtilFactory.
     * 
     * @return instance of UtilFactory
     */
    public static synchronized UtilFactory getInstance() {
        if(instance == null) {
            instance = (UtilFactory)PlatformUtils.getFactoryInstance(factoryClasses);
        }
        return instance;
    }

    protected UtilFactory() {
    }

    void addOpenConnection(Connection connection) {
        synchronized (openConnections) {
            if (!openConnections.contains(connection)) {
                openConnections.addElement(connection);
            }
        }
    }
    
    void removeOpenConnection(Connection connection) {
        synchronized (openConnections) {
            if (openConnections.contains(connection)) {
                openConnections.removeElement(connection);
            }
        }
    }
    
    /**
     * Determine whether open connections exist
     *
     * @return True if there are open connections
     */
    public boolean hasOpenConnections() {
        boolean result;

        synchronized (openConnections) {
            result = !openConnections.isEmpty();
        }

        return result;
    }

    /**
     * Close all open connections
     */
    public void closeAllConnections() {
        synchronized (openConnections) {
            int size = openConnections.size();

            for (int i = 0; i < size; i++) {
                try {
                    ((Connection) openConnections.elementAt(i)).close();
                } catch (IOException e) { }
            }

            openConnections.removeAllElements();
        }
    }
    
    /**
     * Creates a new connection object.
     * 
     * connectionConfig Configuration data for the connection
     * 
     * @return the connection object
     */
    public abstract Connection createConnection(ConnectionConfig connectionConfig);
}
