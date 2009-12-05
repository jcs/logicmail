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

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

import net.rim.device.api.system.CodeModuleManager;

public class PlatformInfoBB42 extends PlatformInfo {
    protected String platformVersion;
    protected String[] filesystemRoots;

    private static String FILE_URL_PREFIX = "file:///";
    
    public PlatformInfoBB42() {
    }
    
    public String getPlatformVersion() {
        if(platformVersion == null) {
            // Get the platform version
            int[] handles = CodeModuleManager.getModuleHandles();
            int size = handles.length;
            //Check for a particular RIM module (Here, the ribbon app)
            for (int i = size-1; i>=0;--i) {
                    if (CodeModuleManager.getModuleName(handles[i]).equals("net_rim_bb_ribbon_app")) {
                            platformVersion =
                                    CodeModuleManager.getModuleVersion(handles[i]);
                    }
            }
        }
        return platformVersion;
    }
    
    public boolean hasTouchscreen() {
        return false;
    }
    
    public String[] getFilesystemRoots() {
        if(filesystemRoots == null) {
            Vector validRoots = new Vector();
            StringBuffer buf = new StringBuffer(FILE_URL_PREFIX);
            int prefixLength = FILE_URL_PREFIX.length();
            Enumeration e = FileSystemRegistry.listRoots();
            while(e.hasMoreElements()) {
                buf.append((String)e.nextElement());
                String url = buf.toString();
                buf.delete(prefixLength, buf.length());
                
                try {
                    FileConnection conn = (FileConnection)Connector.open(url);
                    if(conn.exists() && conn.isDirectory() && conn.canRead() && conn.canWrite()) {
                        validRoots.addElement(url);
                    }
                    conn.close();
                } catch (Exception ex) {
                }
            }
            filesystemRoots = new String[validRoots.size()];
            validRoots.copyInto(filesystemRoots);
        }
        return filesystemRoots;
    }
}
