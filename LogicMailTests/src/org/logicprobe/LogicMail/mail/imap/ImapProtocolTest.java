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

package org.logicprobe.LogicMail.mail.imap;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Unit test for ImapProtocol
 */
public class ImapProtocolTest extends TestCase {
    /**
     * Variation on ImapProtocol that overrides the "execute" method
     * to facilitate testing
     */
    class TestImapProtocol extends ImapProtocol {
        private class ExecuteExpectation {
            public String command;
            public String arguments;
            public String[] result;
        }
        
        private Vector executeExpectations;
        
        public TestImapProtocol() {
            super(null);
            executeExpectations = new Vector();
        }
        
        public void addExecuteExpectation(String command, String arguments, String[] result) {
            ExecuteExpectation expect = new ExecuteExpectation();
            expect.command = command;
            expect.arguments = arguments;
            expect.result = result;
            executeExpectations.addElement(expect);
        }
        
        public void clearExpectations() {
            executeExpectations.removeAllElements();
        }
        
        public void verifyExpectations() {
            assertTrue("Expectations failed", executeExpectations.isEmpty());
        }
        
        protected String[] execute(String command, String arguments) {
            assertTrue("No expectations", !executeExpectations.isEmpty());
            ExecuteExpectation expect = (ExecuteExpectation)executeExpectations.lastElement();
            assertEquals("Bad command", expect.command, command);
            assertEquals("Bad arguments", expect.arguments, arguments);
            executeExpectations.removeElement(expect);
            return expect.result;
        }
    }

    private TestImapProtocol instance;
    
    public ImapProtocolTest() {
    }
    
    public ImapProtocolTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
        instance = new TestImapProtocol();
    }
    
    public void tearDown() {
        instance.verifyExpectations();
        instance.clearExpectations();
        instance = null;
    }
    
    public void testExecuteCapability() {
        try {
            instance.addExecuteExpectation(
                    "CAPABILITY", null,
                    new String[] { "* CAPABILITY ALPHA BRAVO CHARLIE DELTA" });

            Hashtable result = instance.executeCapability();
            assertNotNull(result);

            assertEquals(4, result.size());
            assertEquals(Boolean.TRUE, result.get("ALPHA"));
            assertEquals(Boolean.TRUE, result.get("BRAVO"));
            assertEquals(Boolean.TRUE, result.get("CHARLIE"));
            assertEquals(Boolean.TRUE, result.get("DELTA"));
        } catch (Throwable t) {
            fail("Exception thrown during test: "+t.toString());
            t.printStackTrace();
        }
    }
    
    public void testExecuteNamespace() {
        try {
            // NAMESPACE (("" "/")) (("Other Users/" "/")) NIL
            instance.addExecuteExpectation(
                    "NAMESPACE", null,
                    new String[] { "* NAMESPACE ((\"\" \"/\")) ((\"Other Users/\" \"/\")) NIL" });
            ImapProtocol.NamespaceResponse result = instance.executeNamespace();
            assertNotNull(result);
            
            assertNotNull(result.personal);
            assertEquals(1, result.personal.length);
            assertEquals("", result.personal[0].prefix);
            assertEquals("/", result.personal[0].delimiter);
            
            assertNotNull(result.other);
            assertEquals(1, result.other.length);
            assertEquals("Other Users/", result.other[0].prefix);
            assertEquals("/", result.other[0].delimiter);

            assertNull(result.shared);
        } catch (Throwable t) {
            fail("Exception thrown during test: "+t.toString());
            t.printStackTrace();
        }
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("ImapProtocol");

        suite.addTest(new ImapProtocolTest("executeCapability", new TestMethod()
        { public void run(TestCase tc) {((ImapProtocolTest)tc).testExecuteCapability(); } }));
        suite.addTest(new ImapProtocolTest("executeNamespace", new TestMethod()
        { public void run(TestCase tc) {((ImapProtocolTest)tc).testExecuteNamespace(); } }));

        return suite;
    }
}
