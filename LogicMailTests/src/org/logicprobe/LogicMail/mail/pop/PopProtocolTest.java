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
package org.logicprobe.LogicMail.mail.pop;

import java.util.Hashtable;

import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.util.MockConnection;

import com.hammingweight.hammock.Hammock;
import com.hammingweight.hammock.mocks.microedition.io.MockSocketConnection;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class PopProtocolTest extends TestCase {
    private Hammock hammock;
    private PopProtocol instance;
    
    public PopProtocolTest() {
    }
    
    public PopProtocolTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() throws Exception {
        hammock = new Hammock();
        instance = new PopProtocol();
        
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

    public void testExecuteUser() throws Throwable {
        expectCommand("USER testuser");
        expectResponse("+OK Name is a valid mailbox");
        
        instance.executeUser("testuser");
    }
    
    public void testExecutePass() throws Throwable {
        expectCommand("PASS password");
        expectResponse("+OK Mailbox locked and ready");
        
        instance.executePass("password");
    }
    
    public void testExecutePassInvalid() throws Throwable {
        expectCommand("PASS password");
        expectResponse("-ERR [AUTH] Invalid login");

        try {
            instance.executePass("password");
            fail("Expected exception on authentication failure");
        } catch (MailException e) {
            assertTrue(!e.isFatal());
        }
    }   
    
    public void testExecuteCapa() throws Throwable {
        expectCommand("CAPA");
        expectResponse(new String[] {
                "+OK List of capabilities follows",
                "EXPIRE NEVER",
                "TOP",
                "."
        });
        
        Hashtable result = instance.executeCapa();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("NEVER", result.get("EXPIRE"));
        assertEquals(Boolean.TRUE, result.get("TOP"));
    }
    
    public void testExecuteCapaUnsupported() throws Throwable {
        expectCommand("CAPA");
        expectResponse("-ERR Unrecognized command");
        
        Hashtable result = instance.executeCapa();
        assertNull(result);
    }
    
    public void testExecuteRetr() throws Throwable {
        expectCommand("RETR 1");
        String[] expected = new String[] {
                "+OK Message follows",
                "Date: Sat, 2 Aug 2008 10:19:55 -0400 (EDT)",
                "From: Foo Bar <foobar@test.org>",
                "To: barfoo@test.net",
                "Subject: Test Message",
                "Content-Type: text/plain; charset=US-ASCII; format=flowed",
                "",
                "This is the message content",
                "",
                "."
        };
        expectResponse(expected);
        
        byte[][] response = instance.executeRetr(1, null);
        
        assertFollowsEquals(expected, response);
    }
    
    public void testExecuteRetrEscapedDots() throws Throwable {
        expectCommand("RETR 1");
        String[] expected = new String[] {
                "+OK Message follows",
                "Date: Sat, 2 Aug 2008 10:19:55 -0400 (EDT)",
                "From: Foo Bar <foobar@test.org>",
                "To: barfoo@test.net",
                "Subject: Test Message",
                "Content-Type: text/plain; charset=US-ASCII; format=flowed",
                "",
                "This is the message content",
                "....",
                "...",
                "..",
                "",
                "."
        };
        expectResponse(copyArray(expected));
        expected[8] = "...";
        expected[9] = "..";
        expected[10] = ".";
        
        
        byte[][] response = instance.executeRetr(1, null);
        
        assertFollowsEquals(expected, response);
    }

    private static String[] copyArray(String[] input) {
        if(input == null || input.length == 0) { return input; }
        
        String[] result = new String[input.length];
        System.arraycopy(input, 0, result, 0, input.length);
        return result;
    }
    
    private void assertFollowsEquals(String[] expected, byte[][] response) {
        assertNotNull(response);
        assertEquals(expected.length - 2, response.length);
        for(int i=0; i<response.length; i++) {
            assertEquals(expected[i + 1], new String(response[i]));
        }
    }
    
    private void expectCommand(String command) {
        hammock.setExpectation(MockConnection.MTHD_SEND_COMMAND_$_STRING,
                new Object[] { command });
    }
    
    private void expectResponse(String response) {
        hammock.setExpectation(MockConnection.MTHD_RECEIVE)
            .setReturnValue(response.getBytes());
    }
    
    private void expectResponse(String[] response) {
        for(int i=0; i<response.length; i++) {
            hammock.setExpectation(MockConnection.MTHD_RECEIVE)
                .setReturnValue(response[i].getBytes());
        }
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("PopProtocol");

        suite.addTest(new PopProtocolTest("executeUser", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((PopProtocolTest)tc).testExecuteUser(); }}));
        suite.addTest(new PopProtocolTest("executePass", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((PopProtocolTest)tc).testExecutePass(); }}));
        suite.addTest(new PopProtocolTest("executePassInvalid", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((PopProtocolTest)tc).testExecutePassInvalid(); }}));
        suite.addTest(new PopProtocolTest("executeCapa", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((PopProtocolTest)tc).testExecuteCapa(); }}));
        suite.addTest(new PopProtocolTest("executeCapaUnsupported", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((PopProtocolTest)tc).testExecuteCapaUnsupported(); }}));
        suite.addTest(new PopProtocolTest("executeRetr", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((PopProtocolTest)tc).testExecuteRetr(); }}));
        suite.addTest(new PopProtocolTest("executeRetrEscapedDots", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((PopProtocolTest)tc).testExecuteRetrEscapedDots(); }}));
        
        return suite;
    }
}
