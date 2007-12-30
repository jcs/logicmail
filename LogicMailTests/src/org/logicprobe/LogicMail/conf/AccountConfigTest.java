/*-
 * Copyright (c) 2007, Derek Konigsberg
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

package org.logicprobe.LogicMail.conf;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.rim.device.api.util.Arrays;

/**
 * Unit test for AccountConfig
 */
public class AccountConfigTest extends TestCase {
    class TestOutputStream extends OutputStream {
        private int[] buffer;
        
        public TestOutputStream() {
            super();
            buffer = new int[0];
        }
        public void write(int b) throws IOException {
            Arrays.add(buffer, b);
        }
        public int[] getBuffer() {
            return buffer;
        }
    }
    
    class TestInputStream extends InputStream {
        private int[] buffer;
        private int index;
        
        public TestInputStream(int[] buffer) {
            this.buffer = buffer;
            index = 0;
        }
        public int read() throws IOException {
            return buffer[index++];
        }
    }
    
    public AccountConfigTest() {
    }
    
    public AccountConfigTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }
    
    public void tearDown() {
    }
    
    public void testInitialization() {
        AccountConfig instance = new ImapConfig();
        assertEquals("", instance.getAcctName());
        assertEquals("", instance.getServerName());
        assertEquals(AccountConfig.TYPE_POP, instance.getServerType());
        assertTrue(!instance.getServerSSL());
        assertEquals("", instance.getServerUser());
        assertEquals("", instance.getServerPass());
        assertEquals(143, instance.getServerPort());
        assertTrue(!instance.getDeviceSide());
    }

    public void testSerialization() {
        try {
            // Initialize the configuration
            AccountConfig instance = new ImapConfig();
            instance.setAcctName("FooMail");
            instance.setServerName("mail.foomail.test");
            instance.setServerType(AccountConfig.TYPE_IMAP);
            instance.setServerSSL(true);
            instance.setServerUser("jdoe");
            instance.setServerPass("12345");
            instance.setServerPort(995);
            instance.setDeviceSide(true);
            
            // Serialize
            TestOutputStream testOutput = new TestOutputStream();
            instance.serialize(new DataOutputStream(testOutput));
            
            // Deserialize
            TestInputStream testInput = new TestInputStream(testOutput.getBuffer());
            instance = new ImapConfig();
            instance.deserialize(new DataInputStream(testInput));
            
            // Verify results
            assertEquals("FooMail", instance.getAcctName());
            assertEquals("mail.foomail.test", instance.getServerName());
            assertEquals(AccountConfig.TYPE_IMAP, instance.getServerType());
            assertTrue(instance.getServerSSL());
            assertEquals("jdoe", instance.getServerUser());
            assertEquals("12345", instance.getServerPass());
            assertEquals(995, instance.getServerPort());
            assertTrue(instance.getDeviceSide());
        } catch (Throwable t) {
            fail("Exception thrown during test: "+t.toString());
            t.printStackTrace();
        }
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("AccountConfig");

        suite.addTest(new AccountConfigTest("initialization", new TestMethod()
        { public void run(TestCase tc) {((AccountConfigTest)tc).testInitialization(); } }));
        suite.addTest(new AccountConfigTest("serialization", new TestMethod()
        { public void run(TestCase tc) {((AccountConfigTest)tc).testSerialization(); } }));

        return suite;
    }
}
