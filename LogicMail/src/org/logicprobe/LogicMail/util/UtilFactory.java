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

import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;

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
        "org.logicprobe.LogicMail.util.UtilFactoryBB45"
    };

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

    /**
     * Gets the connector used to open network connections.
     * 
     * @return platform-specific connector instance
     */
    public abstract NetworkConnector getNetworkConnector(GlobalConfig globalConfig, ConnectionConfig connectionConfig);
}
