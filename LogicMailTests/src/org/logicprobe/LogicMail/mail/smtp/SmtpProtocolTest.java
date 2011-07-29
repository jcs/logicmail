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
package org.logicprobe.LogicMail.mail.smtp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.rim.device.api.util.Arrays;

import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.util.MockConnection;

import com.hammingweight.hammock.Hammock;
import com.hammingweight.hammock.IArgumentMatcher;
import com.hammingweight.hammock.mocks.microedition.io.MockSocketConnection;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class SmtpProtocolTest extends TestCase {
    private Hammock hammock;
    private SmtpProtocol instance;
    
    public SmtpProtocolTest() {
    }
    
    public SmtpProtocolTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() throws Exception {
        hammock = new Hammock();
        instance = new SmtpProtocol();
        
        MockSocketConnection socketConnection = new MockSocketConnection(hammock);
        hammock.setStubExpectation(MockSocketConnection.MTHD_OPEN_DATA_INPUT_STREAM).setReturnValue(null);
        hammock.setStubExpectation(MockSocketConnection.MTHD_OPEN_DATA_OUTPUT_STREAM).setReturnValue(null);
        hammock.setStubExpectation(MockSocketConnection.MTHD_GET_LOCAL_ADDRESS).setReturnValue(null);
        instance.setConnection(new MockConnection(socketConnection, ConnectionConfig.TRANSPORT_WIFI_ONLY, hammock));
       
        hammock.setStubExpectation(MockConnection.MTHD_GET_BYTES_SENT).setReturnValue(new Integer(0));
        hammock.setStubExpectation(MockConnection.MTHD_GET_BYTES_RECEIVED).setReturnValue(new Integer(0));
    }
    
    public void tearDown() throws Exception {
        hammock.verify();
        instance = null;
    }

    public void testExecuteAuthPlain() throws Throwable {
        // Base64 encoded: "\000username\000password"
        expectCommand("AUTH PLAIN AHVzZXJuYW1lAHBhc3N3b3Jk");
        expectResponse("235 ok, go ahead");
        
        boolean result = instance.executeAuth(SmtpProtocol.AUTH_PLAIN, "username", "password");
        assertTrue(result);
    }
    
    public void testExecuteAuthPlainFailed() throws Throwable {
        // Base64 encoded: "\000username\000password"
        expectCommand("AUTH PLAIN AHVzZXJuYW1lAHBhc3N3b3Jk");
        expectResponse("535 authorization failed");
        
        boolean result = instance.executeAuth(SmtpProtocol.AUTH_PLAIN, "username", "password");
        assertTrue(!result);
    }
    
    public void testExecuteData() throws Throwable {
        expectCommand("DATA");
        expectResponse("354 Enter mail, end with \".\" on a line by itself");
        String[] rawMessage = new String[] {
                "Date: Sat, 2 Aug 2008 10:19:55 -0400 (EDT)\r\n",
                "From: Foo Bar <foobar@test.org>\r\n",
                "To: barfoo@test.net\r\n",
                "Subject: Test Message\r\n",
                "Content-Type: text/plain; charset=US-ASCII; format=flowed\r\n",
                "\r\n",
                "This is the message content\r\n",
        };
        expectRawSmtpData(rawMessage);
        expectCommand("\r\n.");
        expectResponse("250 2.0.0 p6SJXCeA015681 Message accepted for delivery");
        boolean result = instance.executeData(toByteArray(rawMessage));
        
        assertTrue(result);
    }
    
    public void testExecuteDataEmpty() throws Throwable {
        expectCommand("DATA");
        expectResponse("354 Enter mail, end with \".\" on a line by itself");
        expectCommand("\r\n.");
        expectResponse("250 2.0.0 p6SJXCeA015681 Message accepted for delivery");
        boolean result = instance.executeData(new byte[0]);
        
        assertTrue(result);
    }
    
    public void testExecuteDataAlmostEmpty() throws Throwable {
        expectCommand("DATA");
        expectResponse("354 Enter mail, end with \".\" on a line by itself");
        String[] rawMessage = new String[] {
                "\r\n"
        };
        expectRawSmtpData(rawMessage);
        expectCommand("\r\n.");
        expectResponse("250 2.0.0 p6SJXCeA015681 Message accepted for delivery");
        boolean result = instance.executeData(toByteArray(rawMessage));
        
        assertTrue(result);
    }

    public void testExecuteDataWithDots() throws Throwable {
        expectCommand("DATA");
        expectResponse("354 Enter mail, end with \".\" on a line by itself");
        String[] rawMessage = new String[] {
                "Date: Sat, 2 Aug 2008 10:19:55 -0400 (EDT)\r\n",
                "From: Foo Bar <foobar@test.org>\r\n",
                "To: barfoo@test.net\r\n",
                "Subject: Test Message\r\n",
                "Content-Type: text/plain; charset=US-ASCII; format=flowed\r\n",
                "\r\n",
                "This is the message content\r\n",
                "...\r\n",
                "..\r\n",
                ".\r\n"
        };
        String[] expectedData = copyArray(rawMessage);
        expectedData[7] = "." + expectedData[7];
        expectedData[8] = "." + expectedData[8];
        expectedData[9] = "." + expectedData[9];
        expectRawSmtpData(expectedData);
        expectCommand("\r\n.");
        expectResponse("250 2.0.0 p6SJXCeA015681 Message accepted for delivery");
        boolean result = instance.executeData(toByteArray(rawMessage));
        
        assertTrue(result);
    }
    
    private void expectCommand(String command) {
        hammock.setExpectation(MockConnection.MTHD_SEND_COMMAND_$_STRING,
                new Object[] { command });
    }
    
    private void expectResponse(String response) {
        hammock.setExpectation(MockConnection.MTHD_RECEIVE)
            .setReturnValue(response.getBytes());
    }
    
    private void expectRawSmtpData(String[] data) {
        int offset = 0;
        for(int i=0; i<data.length; i++) {
            byte[] line = data[i].getBytes();
            if(line[0] == (byte)'.') {
                hammock.setExpectation(MockConnection.MTHD_SEND_RAW_$_ARRAY_BYTE_INT_INT,
                        new Object[] { line, new Integer(0), new Integer(line.length) } );
                offset += line.length - 1;
            }
            else {
                hammock.setExpectation(MockConnection.MTHD_SEND_RAW_$_ARRAY_BYTE_INT_INT,
                        new Object[] { line, new Integer(offset), new Integer(line.length) } )
                        .setArgumentMatcher(0, new ArrayOffsetArgumentMatcher(offset, line.length));
                offset += line.length;
            }
        }
    }

    private static class ArrayOffsetArgumentMatcher implements IArgumentMatcher {
        private final int offset;
        private final int length;
        public ArrayOffsetArgumentMatcher(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
        public boolean areArgumentsEqual(Object argumentExpected, Object argumentActual) {
            byte[] expected = (byte[])argumentExpected;
            byte[] actual = (byte[])argumentActual;
            return Arrays.equals(expected, 0, actual, offset, length);
        }
    }
    
    private static byte[] toByteArray(String[] stringData) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for(int i=0; i<stringData.length; i++) {
            stream.write(stringData[i].getBytes());
        }
        return stream.toByteArray();
    }
    
    private static String[] copyArray(String[] input) {
        if(input == null || input.length == 0) { return input; }

        String[] result = new String[input.length];
        System.arraycopy(input, 0, result, 0, input.length);
        return result;
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("SmtpProtocol");
        
        suite.addTest(new SmtpProtocolTest("executeAuthPlain", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((SmtpProtocolTest)tc).testExecuteAuthPlain(); }}));
        suite.addTest(new SmtpProtocolTest("executeAuthPlainFailed", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((SmtpProtocolTest)tc).testExecuteAuthPlainFailed(); }}));
        suite.addTest(new SmtpProtocolTest("executeData", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((SmtpProtocolTest)tc).testExecuteData(); }}));
        suite.addTest(new SmtpProtocolTest("executeDataEmpty", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((SmtpProtocolTest)tc).testExecuteDataEmpty(); }}));
        suite.addTest(new SmtpProtocolTest("executeDataAlmostEmpty", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((SmtpProtocolTest)tc).testExecuteDataAlmostEmpty(); }}));
        suite.addTest(new SmtpProtocolTest("executeDataWithDots", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((SmtpProtocolTest)tc).testExecuteDataWithDots(); }}));
        
        return suite;
    }
}
