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
import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.StringParser;

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
    
    public void testExecuteList() {
        try {
            // Test top-level inbox
            instance.addExecuteExpectation(
                    "LIST", "\"\" \"%\"",
                    new String[] { "* LIST (\\HasChildren) \".\" \"INBOX\"" });
            Vector result = instance.executeList("", "%");
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
            ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse)result.elementAt(0);
            
            assertTrue(result1.hasChildren);
            assertTrue(result1.canSelect);
            assertTrue(!result1.marked);
            assertEquals(".", result1.delim);
            assertEquals("INBOX", result1.name);
            
            // Test subfolders of inbox
            instance.addExecuteExpectation(
                    "LIST", "\"INBOX.\" \"%\"",
                    new String[] {
                        "* LIST (\\HasNoChildren) \".\" \"INBOX.Saved\"",
                        "* LIST (\\HasNoChildren) \".\" \"INBOX.Sent\"",
                    });
        
            result = instance.executeList("INBOX.", "%");
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
            assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);
            result1 = (ImapProtocol.ListResponse)result.elementAt(0);
            ImapProtocol.ListResponse result2 = (ImapProtocol.ListResponse)result.elementAt(1);

            assertTrue(!result1.hasChildren);
            assertTrue(result1.canSelect);
            assertTrue(!result1.marked);
            assertEquals(".", result1.delim);
            assertEquals("INBOX.Saved", result1.name);
        
            assertTrue(!result2.hasChildren);
            assertTrue(result2.canSelect);
            assertTrue(!result2.marked);
            assertEquals(".", result2.delim);
            assertEquals("INBOX.Sent", result2.name);

            // Test inbox with a NIL delimiter
            instance.addExecuteExpectation(
                    "LIST", "\"\" \"%\"",
                    new String[] {
                        "* LIST (\\HasNoChildren) NIL INBOX",
                        "* LIST (\\HasNoChildren) \"/\" Sent"
                    });
            result = instance.executeList("", "%");
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
            assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);
            result1 = (ImapProtocol.ListResponse)result.elementAt(0);
            result2 = (ImapProtocol.ListResponse)result.elementAt(1);

            assertTrue(!result1.hasChildren);
            assertTrue(result1.canSelect);
            assertTrue(!result1.marked);
            assertEquals("", result1.delim);
            assertEquals("INBOX", result1.name);
            
            assertTrue(!result2.hasChildren);
            assertTrue(result2.canSelect);
            assertTrue(!result2.marked);
            assertEquals("/", result2.delim);
            assertEquals("Sent", result2.name);
        } catch (Throwable t) {
            fail("Exception thrown during test: "+t.toString());
            t.printStackTrace();
        }
    }
    
    public void testExecuteFetchEnvelope1() {
        try {
            instance.addExecuteExpectation(
                "FETCH", "1:1 (FLAGS ENVELOPE)",
                new String[] {
                    "* 1 FETCH (FLAGS (\\Answered \\Seen) " +
                    "ENVELOPE (\"Mon, 12 Mar 2007 19:38:31 -0700\" \"Re: Calm down! :-)\" " +
                    "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                    "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                    "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                    "((\"John Doe\" NIL \"jdoe\" \"generic.test\")) " +
                    "NIL NIL " +
                    "\"<200703121933.25327.jdoe@generic.test>\" " +
                    "\"<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>\"))"
                });
            ImapProtocol.FetchEnvelopeResponse[] result = instance.executeFetchEnvelope(1, 1);
            assertNotNull(result);
            assertEquals(1, result.length);
            assertNotNull(result[0]);
            
            assertEquals(1, result[0].index);
            assertNotNull(result[0].flags);
            assertTrue(result[0].flags.answered);
            assertTrue(result[0].flags.seen);
            assertTrue(!result[0].flags.deleted);
            assertTrue(!result[0].flags.draft);
            assertTrue(!result[0].flags.flagged);
            assertTrue(!result[0].flags.recent);
            
            assertNotNull(result[0].envelope);
            MessageEnvelope env = result[0].envelope;
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-7"));
            cal.set(Calendar.YEAR, 2007);
            cal.set(Calendar.MONTH, 2);
            cal.set(Calendar.DAY_OF_MONTH, 12);
            cal.set(Calendar.HOUR_OF_DAY, 19);
            cal.set(Calendar.MINUTE, 38);
            cal.set(Calendar.SECOND, 31);
            assertEquals(StringParser.createDateString(cal.getTime()), StringParser.createDateString(env.date));
            
            assertEquals("Re: Calm down! :-)", env.subject);

            assertNotNull(env.from);
            assertEquals(1, env.from.length);
            assertNotNull(env.from[0]);
            assertEquals("jim smith <jsmith@scratch.test>", env.from[0]);
            
            assertNotNull(env.sender);
            assertEquals(1, env.sender.length);
            assertNotNull(env.sender[0]);
            assertEquals("jim smith <jsmith@scratch.test>", env.sender[0]);

            assertNotNull(env.replyTo);
            assertEquals(1, env.replyTo.length);
            assertNotNull(env.replyTo[0]);
            assertEquals("jim smith <jsmith@scratch.test>", env.replyTo[0]);
            
            assertNotNull(env.to);
            assertEquals(1, env.to.length);
            assertNotNull(env.to[0]);
            assertEquals("John Doe <jdoe@generic.test>", env.to[0]);
            
            assertNull(env.cc);
            assertNull(env.bcc);
            
            assertEquals("<200703121933.25327.jdoe@generic.test>", env.inReplyTo);
            assertEquals("<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>", env.messageId);
            
        } catch (MailException e) {
            fail("MailException thrown during test: "+e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            fail("IOException thrown during test: "+e.toString());
            e.printStackTrace();
        }
    }
    
    public void testExecuteFetchEnvelope2() {
        try {
            instance.addExecuteExpectation(
                "FETCH", "1:1 (FLAGS ENVELOPE)",
                new String[] {
                    "* 1 FETCH (" +
                    "ENVELOPE (\"Sun, 08 Jul 2007 09:48:47 +0100\" \"A Test\" " +
                    "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                    "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                    "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                    "((\"\" NIL \"test\" \"generic.test\")) " +
                    "NIL NIL " +
                    "NIL " +
                    "\"<4690A4EF.3070302@mail.scratch.test>\") " +
                    "FLAGS (\\Seen))"
                });
            ImapProtocol.FetchEnvelopeResponse[] result = instance.executeFetchEnvelope(1, 1);
            assertNotNull(result);
            assertEquals(1, result.length);
            assertNotNull(result[0]);
            
            assertEquals(1, result[0].index);
            assertNotNull(result[0].flags);
            assertTrue(result[0].flags.seen);
            assertTrue(!result[0].flags.answered);
            assertTrue(!result[0].flags.deleted);
            assertTrue(!result[0].flags.draft);
            assertTrue(!result[0].flags.flagged);
            assertTrue(!result[0].flags.recent);
            
            assertNotNull(result[0].envelope);
            MessageEnvelope env = result[0].envelope;
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
            cal.set(Calendar.YEAR, 2007);
            cal.set(Calendar.MONTH, 6);
            cal.set(Calendar.DAY_OF_MONTH, 8);
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 48);
            cal.set(Calendar.SECOND, 47);
            assertEquals(StringParser.createDateString(cal.getTime()), StringParser.createDateString(env.date));
            
            assertEquals("A Test", env.subject);

            assertNotNull(env.from);
            assertEquals(1, env.from.length);
            assertNotNull(env.from[0]);
            assertEquals("jim smith <jsmith@scratch.test>", env.from[0]);
            
            assertNotNull(env.sender);
            assertEquals(1, env.sender.length);
            assertNotNull(env.sender[0]);
            assertEquals("jim smith <jsmith@scratch.test>", env.sender[0]);

            assertNotNull(env.replyTo);
            assertEquals(1, env.replyTo.length);
            assertNotNull(env.replyTo[0]);
            assertEquals("jim smith <jsmith@scratch.test>", env.replyTo[0]);
            
            assertNotNull(env.to);
            assertEquals(1, env.to.length);
            assertNotNull(env.to[0]);
            assertEquals("test@generic.test", env.to[0]);
            
            assertNull(env.cc);
            assertNull(env.bcc);
            
            assertEquals("", env.inReplyTo);
            assertEquals("<4690A4EF.3070302@mail.scratch.test>", env.messageId);
            
        } catch (MailException e) {
            fail("MailException thrown during test: "+e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            fail("IOException thrown during test: "+e.toString());
            e.printStackTrace();
        }
    }

    public void testExecuteStore1() {
        try {
            instance.addExecuteExpectation(
                    "STORE", "5 +FLAGS (\\Answered)",
                    new String[] { "* 5 FETCH (FLAGS (\\Seen \\Answered))" });
            ImapProtocol.MessageFlags result = instance.executeStore(5, true, new String[] { "\\Answered" });
            assertNotNull(result);
            
            assertTrue(result.seen);
            assertTrue(result.answered);
            assertTrue(!result.deleted);
            assertTrue(!result.draft);
            assertTrue(!result.flagged);
            assertTrue(!result.recent);
        } catch (Throwable t) {
            fail("Exception thrown during test: "+t.toString());
            t.printStackTrace();
        }
    }

    public void testExecuteStore2() {
        try {
            instance.addExecuteExpectation(
                    "STORE", "5 -FLAGS (\\Answered)",
                    new String[] { "* 5 FETCH (FLAGS ())" });
            ImapProtocol.MessageFlags result = instance.executeStore(5, false, new String[] { "\\Answered" });
            assertNotNull(result);
            
            assertTrue(!result.seen);
            assertTrue(!result.answered);
            assertTrue(!result.deleted);
            assertTrue(!result.draft);
            assertTrue(!result.flagged);
            assertTrue(!result.recent);
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
        suite.addTest(new ImapProtocolTest("executeList", new TestMethod()
        { public void run(TestCase tc) {((ImapProtocolTest)tc).testExecuteList(); } }));
        suite.addTest(new ImapProtocolTest("executeFetchEnvelope1", new TestMethod()
        { public void run(TestCase tc) {((ImapProtocolTest)tc).testExecuteFetchEnvelope1(); } }));
        suite.addTest(new ImapProtocolTest("executeFetchEnvelope2", new TestMethod()
        { public void run(TestCase tc) {((ImapProtocolTest)tc).testExecuteFetchEnvelope2(); } }));
        suite.addTest(new ImapProtocolTest("executeStore1", new TestMethod()
        { public void run(TestCase tc) {((ImapProtocolTest)tc).testExecuteStore1(); } }));
        suite.addTest(new ImapProtocolTest("executeStore2", new TestMethod()
        { public void run(TestCase tc) {((ImapProtocolTest)tc).testExecuteStore2(); } }));

        return suite;
    }
}
