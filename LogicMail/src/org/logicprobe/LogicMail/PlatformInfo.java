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
package org.logicprobe.LogicMail;

import org.logicprobe.LogicMail.util.PlatformUtils;

/**
 * Provides information about the platform the application is running on,
 * wrapping OS version specific API calls as necessary.
 * Also used as a placeholder to make sure all the platform libraries
 * are built correctly.
 */
public abstract class PlatformInfo {
    private static PlatformInfo instance;

    /**
     * Array of concrete PlatformInfo classes, in order from the highest
     * API version to the lowest.
     */
    private static String[] infoClasses = {
        "org.logicprobe.LogicMail.PlatformInfoBB60",
        "org.logicprobe.LogicMail.PlatformInfoBB50",
        "org.logicprobe.LogicMail.PlatformInfoBB47",
        "org.logicprobe.LogicMail.PlatformInfoBB46",
        "org.logicprobe.LogicMail.PlatformInfoBB42"
    };

    /**
     * Gets the single instance of PlatformInfo.
     * 
     * @return instance of PlatformInfo
     */
    public static synchronized PlatformInfo getInstance() {
        if(instance == null) {
            instance = (PlatformInfo)PlatformUtils.getFactoryInstance(infoClasses);
        }
        return instance;
    }

    /**
     * Gets the BlackBerry platform version.
     * 
     * @return the platform version
     */
    public abstract String getPlatformVersion();

    /**
     * Checks for whether the platform has a touch screen.
     * 
     * @return true, if this is a touch screen device
     */
    public abstract boolean hasTouchscreen();

    /**
     * Gets the filesystem roots that the application can write to.
     * 
     * @return the filesystem roots
     */
    public abstract String[] getFilesystemRoots();
}
