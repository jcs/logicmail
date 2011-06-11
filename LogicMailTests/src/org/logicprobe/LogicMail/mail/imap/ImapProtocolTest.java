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

import net.rim.device.api.util.IntVector;

import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.mail.MailProgressHandler;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.StringParser;

import java.io.IOException;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;


/**
 * Unit test for ImapProtocol
 */
public class ImapProtocolTest extends TestCase {
    private TestImapProtocol instance;
    private TestUntaggedResponseListener listener;

    public ImapProtocolTest() {
    }

    public ImapProtocolTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void setUp() {
        instance = new TestImapProtocol();
        listener = new TestUntaggedResponseListener();
        instance.setUntaggedResponseListener(listener);
    }

    public void tearDown() {
        instance.verifyExpectations();
        instance.clearExpectations();
        instance = null;
        listener = null;
    }

    public void testExecuteCapability() throws Throwable {
        instance.addExecuteExpectation("CAPABILITY", null,
            new String[] { "* CAPABILITY ALPHA BRAVO CHARLIE DELTA" });

        Hashtable result = instance.executeCapability();
        assertNotNull(result);

        assertEquals(4, result.size());
        assertEquals(Boolean.TRUE, result.get("ALPHA"));
        assertEquals(Boolean.TRUE, result.get("BRAVO"));
        assertEquals(Boolean.TRUE, result.get("CHARLIE"));
        assertEquals(Boolean.TRUE, result.get("DELTA"));
    }

    public void testExecuteNamespace1() throws Throwable {
        // Normal namespace:
        // NAMESPACE (("" "/")) (("Other Users/" "/")) NIL
        instance.addExecuteExpectation("NAMESPACE", null,
            new String[] {
                "* NAMESPACE ((\"\" \"/\")) ((\"Other Users/\" \"/\")) NIL"
            });

        ImapProtocol.NamespaceResponse result = instance.executeNamespace();
        assertNotNull("Result", result);

        assertNotNull("Personal", result.personal);
        assertEquals(1, result.personal.length);
        assertEquals("", result.personal[0].prefix);
        assertEquals("/", result.personal[0].delimiter);

        assertNotNull("Other", result.other);
        assertEquals(1, result.other.length);
        assertEquals("Other Users/", result.other[0].prefix);
        assertEquals("/", result.other[0].delimiter);

        assertNull("Shared", result.shared);
    }

    public void testExecuteNamespace2() throws Throwable {
        // Escaped delimiters:
        // NAMESPACE (("" "\\")) (("Other Users\\" "\\")) (("Public Folders\\" "\\"))
        instance.addExecuteExpectation("NAMESPACE", null,
            new String[] {
                "* NAMESPACE ((\"\" \"\\\\\")) ((\"Other Users\\\\\" \"\\\\\")) ((\"Public Folders\\\\\" \"\\\\\"))"
            });

        ImapProtocol.NamespaceResponse result = instance.executeNamespace();
        assertNotNull("Result", result);

        assertNotNull("Personal", result.personal);
        assertEquals(1, result.personal.length);
        assertEquals("", result.personal[0].prefix);
        assertEquals("\\", result.personal[0].delimiter);

        assertNotNull("Other", result.other);
        assertEquals(1, result.other.length);
        assertEquals("Other Users\\", result.other[0].prefix);
        assertEquals("\\", result.other[0].delimiter);

        assertNotNull("Shared ", result.shared);
        assertEquals(1, result.shared.length);
        assertEquals("Public Folders\\", result.shared[0].prefix);
        assertEquals("\\", result.shared[0].delimiter);
    }

    public void testExecuteList1() throws Throwable {
        // Test top-level inbox
        instance.addExecuteExpectation("LIST", "\"\" \"%\"",
            new String[] { "* LIST (\\HasChildren) \".\" \"INBOX\"" });

        Vector result = instance.executeList("", "%", null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);

        ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse) result.elementAt(0);

        assertTrue(result1.hasChildren);
        assertTrue(result1.canSelect);
        assertTrue(!result1.marked);
        assertEquals(".", result1.delim);
        assertEquals("INBOX", result1.name);
    }

    public void testExecuteList2() throws Throwable {
        // Test subfolders of inbox
        instance.addExecuteExpectation("LIST", "\"INBOX.\" \"%\"",
            new String[] {
                "* LIST (\\HasNoChildren) \".\" \"INBOX.Saved\"",
                "* LIST (\\HasNoChildren) \".\" \"INBOX.Sent\"",
            });

        Vector result = instance.executeList("INBOX.", "%", null);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
        assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);

        ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse) result.elementAt(0);
        ImapProtocol.ListResponse result2 = (ImapProtocol.ListResponse) result.elementAt(1);

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
    }

    public void testExecuteList3() throws Throwable {
        // Test inbox with a NIL delimiter
        instance.addExecuteExpectation("LIST", "\"\" \"%\"",
            new String[] {
                "* LIST (\\HasNoChildren) NIL INBOX",
                "* LIST (\\HasNoChildren) \"/\" Sent"
            });

        Vector result = instance.executeList("", "%", null);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
        assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);

        ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse) result.elementAt(0);
        ImapProtocol.ListResponse result2 = (ImapProtocol.ListResponse) result.elementAt(1);

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
    }

    public void testExecuteList4() throws Throwable {
        // Test parameter in response
        instance.addExecuteExpectation("LIST", "\"INBOX.\" \"%\"",
            new String[] {
                "* LIST (\\HasChildren) \".\" \"INBOX\"",
                "* LIST (\\HasNoChildren) \".\" \"INBOX.Saved\"",
                "* LIST (\\HasNoChildren) \".\" \"INBOX.Sent\"",
            });

        Vector result = instance.executeList("INBOX.", "%", null);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
        assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);

        ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse) result.elementAt(0);
        ImapProtocol.ListResponse result2 = (ImapProtocol.ListResponse) result.elementAt(1);

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
    }

    public void testExecuteList5() throws Throwable {
        // Test escaped delimiter
        instance.addExecuteExpectation("LIST", "\"INBOX\\\\\" \"%\"",
            new String[] {
                "* LIST (\\HasNoChildren) \"\\\\\" \"INBOX\\Saved\\Stuff\"",
                "* LIST (\\HasNoChildren) \"\\\\\" \"INBOX\\Sent\"",
            });

        Vector result = instance.executeList("INBOX\\", "%", null);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
        assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);

        ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse) result.elementAt(0);
        ImapProtocol.ListResponse result2 = (ImapProtocol.ListResponse) result.elementAt(1);

        assertTrue(!result1.hasChildren);
        assertTrue(result1.canSelect);
        assertTrue(!result1.marked);
        assertEquals("\\", result1.delim);
        assertEquals("INBOX\\Saved\\Stuff", result1.name);

        assertTrue(!result2.hasChildren);
        assertTrue(result2.canSelect);
        assertTrue(!result2.marked);
        assertEquals("\\", result2.delim);
        assertEquals("INBOX\\Sent", result2.name);
    }

    public void testExecuteList6() throws Throwable {
        // Test specified-length encoding for path name
        instance.addExecuteExpectation("LIST", "\"2007\\\\\" \"%\"",
            new String[] {
                "* LIST (\\HasNoChildren) \"\\\\\" {12}", "2007\\Q3-2007",
                "* LIST (\\HasNoChildren) \"\\\\\" {12}", "2007\\Q4-2007"
            });

        Vector result = instance.executeList("2007\\", "%", null);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
        assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);

        ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse) result.elementAt(0);
        ImapProtocol.ListResponse result2 = (ImapProtocol.ListResponse) result.elementAt(1);

        assertTrue(!result1.hasChildren);
        assertTrue(result1.canSelect);
        assertTrue(!result1.marked);
        assertEquals("\\", result1.delim);
        assertEquals("2007\\Q3-2007", result1.name);

        assertTrue(!result2.hasChildren);
        assertTrue(result2.canSelect);
        assertTrue(!result2.marked);
        assertEquals("\\", result2.delim);
        assertEquals("2007\\Q4-2007", result2.name);
    }

    public void testExecuteList7() throws Throwable {
        // Test specified-length encoding for path name, with spaces
        instance.addExecuteExpectation("LIST", "\"\" \"%\"",
            new String[] {
                "* LIST (\\Noselect) \":\" {5}", "Marya",
                "* LIST (\\Noselect) \":\" {11}", "Old Inboxes",
                "* LIST (\\Noinferiors) \":\" {17}", "Pre-filtered Junk"
            });

        Vector result = instance.executeList("", "%", null);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.elementAt(0) instanceof ImapProtocol.ListResponse);
        assertTrue(result.elementAt(1) instanceof ImapProtocol.ListResponse);
        assertTrue(result.elementAt(2) instanceof ImapProtocol.ListResponse);

        ImapProtocol.ListResponse result1 = (ImapProtocol.ListResponse) result.elementAt(0);
        ImapProtocol.ListResponse result2 = (ImapProtocol.ListResponse) result.elementAt(1);
        ImapProtocol.ListResponse result3 = (ImapProtocol.ListResponse) result.elementAt(2);

        assertTrue(!result1.canSelect);
        assertTrue(!result1.noInferiors);
        assertEquals(":", result1.delim);
        assertEquals("Marya", result1.name);

        assertTrue(!result2.canSelect);
        assertTrue(!result2.noInferiors);
        assertEquals(":", result2.delim);
        assertEquals("Old Inboxes", result2.name);

        assertTrue(result3.canSelect);
        assertTrue(result3.noInferiors);
        assertEquals(":", result3.delim);
        assertEquals("Pre-filtered Junk", result3.name);
    }

    public void testExecuteFetchEnvelope1() throws Throwable {
        instance.addExecuteExpectation("FETCH",
            "1:1 (FLAGS UID ENVELOPE BODYSTRUCTURE)",
            new String[] {
                "* 1 FETCH (FLAGS (\\Answered \\Seen) UID 10 " +
                "ENVELOPE (\"Mon, 12 Mar 2007 19:38:31 -0700\" \"Re: Calm down! :-)\" " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"John Doe\" NIL \"jdoe\" \"generic.test\")) " +
                "NIL NIL " + "\"<200703121933.25327.jdoe@generic.test>\" " +
                "\"<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>\") " +
                "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 44 2 NIL NIL NIL))"
            });

        ShimCallback shim = new ShimCallback();
        instance.executeFetchEnvelope(1, 1, shim, null);

        ImapProtocol.FetchEnvelopeResponse[] result = shim.getResponses();

        assertNotNull(result);
        assertEquals(1, result.length);
        assertNotNull(result[0]);

        assertEquals(1, result[0].index);
        assertEquals(10, result[0].uid);
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
        assertEquals(StringParser.createDateString(cal.getTime()),
            StringParser.createDateString(env.date));

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
        assertEquals("<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>",
            env.messageId);

        ImapParser.MessageSection structure = result[0].structure;
        assertNotNull(structure);
        assertEquals("text", structure.type);
        assertEquals("plain", structure.subtype);
        assertEquals("us-ascii", structure.charset);
        assertEquals("7bit", structure.encoding);
    }

    public void testExecuteFetchEnvelope2() throws Throwable {
        instance.addExecuteExpectation("FETCH",
            "1:1 (FLAGS UID ENVELOPE BODYSTRUCTURE)",
            new String[] {
                "* 1 FETCH (" +
                "ENVELOPE (\"Sun, 08 Jul 2007 09:48:47 +0100\" \"A Test\" " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"\" NIL \"test\" \"generic.test\")) " + "NIL NIL " +
                "NIL " + "\"<4690A4EF.3070302@mail.scratch.test>\") " +
                "FLAGS (\\Seen) UID 10 " +
                "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 44 2 NIL NIL NIL))"
            });

        ShimCallback shim = new ShimCallback();
        instance.executeFetchEnvelope(1, 1, shim, null);

        ImapProtocol.FetchEnvelopeResponse[] result = shim.getResponses();

        assertNotNull(result);
        assertEquals(1, result.length);
        assertNotNull(result[0]);

        assertEquals(1, result[0].index);
        assertEquals(10, result[0].uid);
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
        assertEquals(StringParser.createDateString(cal.getTime()),
            StringParser.createDateString(env.date));

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

        ImapParser.MessageSection structure = result[0].structure;
        assertNotNull(structure);
        assertEquals("text", structure.type);
        assertEquals("plain", structure.subtype);
        assertEquals("us-ascii", structure.charset);
        assertEquals("7bit", structure.encoding);
    }

    public void testExecuteFetchEnvelope3() throws Throwable {
        instance.addExecuteExpectation("FETCH",
            "1:1 (FLAGS UID ENVELOPE BODYSTRUCTURE)",
            new String[] {
                "* 1 FETCH (" + "FLAGS (\\Seen) UID 10 " +
                "ENVELOPE (\"Wed, 06 Aug 2008 02:23:30 -0000\" " +
                "\"[LogicMail for BlackBerry] #93: Endless \\\"refresh folders\\\" loop when using qmail server\" " +
                "((\"LogicMail for BlackBerry\" NIL \"trac\" \"scratch.test\")) " +
                "((\"LogicMail for BlackBerry\" NIL \"trac\" \"scratch.test\")) " +
                "((NIL NIL \"trac\" \"scratch.test\")) " +
                "((NIL NIL \"undisclosed-recipients\" NIL)) " +
                "NIL NIL NIL " +
                "\"<060.de38dad18d49d570300daa4869e9abed@scratch.test>\") " +
                "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 44 2 NIL NIL NIL))"
            });

        ShimCallback shim = new ShimCallback();
        instance.executeFetchEnvelope(1, 1, shim, null);

        ImapProtocol.FetchEnvelopeResponse[] result = shim.getResponses();

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
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.YEAR, 2008);
        cal.set(Calendar.MONTH, 7);
        cal.set(Calendar.DAY_OF_MONTH, 6);
        cal.set(Calendar.HOUR_OF_DAY, 2);
        cal.set(Calendar.MINUTE, 23);
        cal.set(Calendar.SECOND, 30);
        assertEquals(StringParser.createDateString(cal.getTime()),
            StringParser.createDateString(env.date));

        assertEquals("[LogicMail for BlackBerry] #93: Endless \"refresh folders\" loop when using qmail server",
            env.subject);

        assertNotNull(env.from);
        assertEquals(1, env.from.length);
        assertNotNull(env.from[0]);
        assertEquals("LogicMail for BlackBerry <trac@scratch.test>",
            env.from[0]);

        assertNotNull(env.sender);
        assertEquals(1, env.sender.length);
        assertNotNull(env.sender[0]);
        assertEquals("LogicMail for BlackBerry <trac@scratch.test>",
            env.sender[0]);

        assertNotNull(env.replyTo);
        assertEquals(1, env.replyTo.length);
        assertNotNull(env.replyTo[0]);
        assertEquals("trac@scratch.test", env.replyTo[0]);

        assertNotNull(env.to);
        assertEquals(1, env.to.length);
        assertNotNull(env.to[0]);
        assertEquals("undisclosed-recipients", env.to[0]);

        assertNull(env.cc);
        assertNull(env.bcc);

        assertEquals("", env.inReplyTo);
        assertEquals("<060.de38dad18d49d570300daa4869e9abed@scratch.test>",
            env.messageId);

        ImapParser.MessageSection structure = result[0].structure;
        assertNotNull(structure);
        assertEquals("text", structure.type);
        assertEquals("plain", structure.subtype);
        assertEquals("us-ascii", structure.charset);
        assertEquals("7bit", structure.encoding);
    }

    public void testExecuteFetchEnvelope4() throws Throwable {
        instance.addExecuteExpectation("FETCH",
            "1:1 (FLAGS UID ENVELOPE BODYSTRUCTURE)",
            new String[] {
                "* 1 FETCH (FLAGS (\\Seen) UID 10 " +
                "ENVELOPE (\"Sun, 10 Aug 2008 14:47:16 -0000\" " +
                "\"Re: [LogicMail for BlackBerry] #94: Message \\\"\\\"This message does not contain any sections that could be displayed\\\"\" " +
                "((\"LogicMail for BlackBerry\" NIL \"trac\" \"scratch.test\")) " +
                "((\"LogicMail for BlackBerry\" NIL \"trac\" \"scratch.test\")) " +
                "((NIL NIL \"trac\" \"scratch.test\")) " +
                "((NIL NIL \"undisclosed-recipients\" NIL)) " + "NIL NIL " +
                "\"<060.dbfdcb41f7f58c1f764727b7af599d3a@scratch.test>\" " +
                "\"<069.001addbae7c31283d93b5f5d97756e65@scratch.test>\") " +
                "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 44 2 NIL NIL NIL))"
            });

        ShimCallback shim = new ShimCallback();
        instance.executeFetchEnvelope(1, 1, shim, null);

        ImapProtocol.FetchEnvelopeResponse[] result = shim.getResponses();

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
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.YEAR, 2008);
        cal.set(Calendar.MONTH, 7);
        cal.set(Calendar.DAY_OF_MONTH, 10);
        cal.set(Calendar.HOUR_OF_DAY, 14);
        cal.set(Calendar.MINUTE, 47);
        cal.set(Calendar.SECOND, 16);
        assertEquals(StringParser.createDateString(cal.getTime()),
            StringParser.createDateString(env.date));

        assertEquals("Re: [LogicMail for BlackBerry] #94: Message \"\"This message does not contain any sections that could be displayed\"",
            env.subject);

        assertNotNull(env.from);
        assertEquals(1, env.from.length);
        assertNotNull(env.from[0]);
        assertEquals("LogicMail for BlackBerry <trac@scratch.test>",
            env.from[0]);

        assertNotNull(env.sender);
        assertEquals(1, env.sender.length);
        assertNotNull(env.sender[0]);
        assertEquals("LogicMail for BlackBerry <trac@scratch.test>",
            env.sender[0]);

        assertNotNull(env.replyTo);
        assertEquals(1, env.replyTo.length);
        assertNotNull(env.replyTo[0]);
        assertEquals("trac@scratch.test", env.replyTo[0]);

        assertNotNull(env.to);
        assertEquals(1, env.to.length);
        assertNotNull(env.to[0]);
        assertEquals("undisclosed-recipients", env.to[0]);

        assertNull(env.cc);
        assertNull(env.bcc);

        assertEquals("<060.dbfdcb41f7f58c1f764727b7af599d3a@scratch.test>",
            env.inReplyTo);
        assertEquals("<069.001addbae7c31283d93b5f5d97756e65@scratch.test>",
            env.messageId);

        ImapParser.MessageSection structure = result[0].structure;
        assertNotNull(structure);
        assertEquals("text", structure.type);
        assertEquals("plain", structure.subtype);
        assertEquals("us-ascii", structure.charset);
        assertEquals("7bit", structure.encoding);
    }
    
    public void testExecuteFetchEnvelopeUntagged() throws Throwable {
        instance.addExecuteExpectation("UID FETCH",
            "10:* (FLAGS UID ENVELOPE BODYSTRUCTURE)",
            new String[] {
                "* 10 FETCH (FLAGS (\\Answered \\Seen) UID 15 " +
                "ENVELOPE (\"Mon, 12 Mar 2007 19:38:31 -0700\" \"Re: Calm down! :-)\" " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"jim smith\" NIL \"jsmith\" \"scratch.test\")) " +
                "((\"John Doe\" NIL \"jdoe\" \"generic.test\")) " +
                "NIL NIL " + "\"<200703121933.25327.jdoe@generic.test>\" " +
                "\"<7b02460f0703121938sff23a05xd3c2a37dc6b9eb7d@mail.scratch.test>\") " +
                "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"us-ascii\") NIL NIL \"7BIT\" 44 2 NIL NIL NIL))",
                "* 8 FETCH (FLAGS (\\Answered \\Seen) UID 14)",
                "* 2 EXPUNGE"
            });

        ShimCallback shim = new ShimCallback();
        instance.executeFetchEnvelopeUid(10, shim, null);

        ImapProtocol.FetchEnvelopeResponse[] result = shim.getResponses();

        // Perform a very minimal verification on the direct response, just
        // to ensure we got one relating to the right FETCH.  The above
        // test data is more rigorously verified in a different test case.
        assertNotNull(result);
        assertEquals(1, result.length);
        assertNotNull(result[0]);
        assertEquals(10, result[0].index);
        assertEquals(15, result[0].uid);
        assertNotNull(result[0].envelope);
        MessageEnvelope env = result[0].envelope;
        assertEquals("Re: Calm down! :-)", env.subject);

        String msg ="Untagged FETCH";
        assertEquals(msg, 1, listener.fetch.size());
        ImapProtocol.FetchFlagsResponse untaggedFetch = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(0);
        assertEquals(msg, 8, untaggedFetch.index);
        assertEquals(msg, 14, untaggedFetch.uid);
        assertTrue(msg, untaggedFetch.flags.seen);
        assertTrue(msg, untaggedFetch.flags.answered);
        assertTrue(msg, !untaggedFetch.flags.deleted);
        assertTrue(msg, !untaggedFetch.flags.draft);
        assertTrue(msg, !untaggedFetch.flags.flagged);
        assertTrue(msg, !untaggedFetch.flags.recent);
        
        msg = "Untagged EXPUNGE";
        assertEquals(msg, 1, listener.expunge.size());
        assertEquals(msg, 2, listener.expunge.elementAt(0));
    }
    
    public void testExecuteFetchFlags1() throws Throwable {
        instance.addExecuteExpectation("FETCH", "1:1 (FLAGS UID)",
            new String[] { "* 1 FETCH (FLAGS () UID 42)" });

        ImapProtocol.FetchFlagsResponse[] result = instance.executeFetchFlags(1,
                1, null);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1, result[0].index);
        assertEquals(42, result[0].uid);

        ImapProtocol.MessageFlags flags = result[0].flags;
        assertNotNull(flags);
        assertTrue("answered", !flags.answered);
        assertTrue("deleted", !flags.deleted);
        assertTrue("draft", !flags.draft);
        assertTrue("flagged", !flags.flagged);
        assertTrue("junk", !flags.junk);
        assertTrue("recent", !flags.recent);
        assertTrue("seen", !flags.seen);
    }

    public void testExecuteFetchFlags2() throws Throwable {
        instance.addExecuteExpectation("FETCH", "1:1 (FLAGS UID)",
            new String[] { "* 1 FETCH (FLAGS (\\Seen) UID 42)" });

        ImapProtocol.FetchFlagsResponse[] result = instance.executeFetchFlags(1,
                1, null);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1, result[0].index);
        assertEquals(42, result[0].uid);

        ImapProtocol.MessageFlags flags = result[0].flags;
        assertNotNull(flags);
        assertTrue("answered", !flags.answered);
        assertTrue("deleted", !flags.deleted);
        assertTrue("draft", !flags.draft);
        assertTrue("flagged", !flags.flagged);
        assertTrue("junk", !flags.junk);
        assertTrue("recent", !flags.recent);
        assertTrue("seen", flags.seen);
    }
    
    public void testExecuteFetchFlagsUntagged() throws Throwable {
        instance.addExecuteExpectation(
                "UID FETCH", "42:* (FLAGS UID)",
                new String[] {
                        "* 40 FETCH (FLAGS (\\Seen) UID 42)",
                        "* 5 EXPUNGE"
                });

        ImapProtocol.FetchFlagsResponse[] result = instance.executeFetchFlagsUid(42, null);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(40, result[0].index);
        assertEquals(42, result[0].uid);

        ImapProtocol.MessageFlags flags = result[0].flags;
        assertNotNull(flags);
        assertTrue("answered", !flags.answered);
        assertTrue("deleted", !flags.deleted);
        assertTrue("draft", !flags.draft);
        assertTrue("flagged", !flags.flagged);
        assertTrue("junk", !flags.junk);
        assertTrue("recent", !flags.recent);
        assertTrue("seen", flags.seen);
        
        assertEquals("EXPUNGE", 1, listener.expunge.size());
        assertEquals("EXPUNGE", 5, listener.expunge.elementAt(0));
    }
    
    public void testExecuteFetchBody() throws Throwable {
        instance.addExecuteExpectation(
            "UID FETCH", "288 (BODY[1]<0.1024>)",
            new String[] {
                "* 84 FETCH (UID 288 BODY[1]<0> {7}\r\nHello\r\n)"
            });

        byte[] result = instance.executeFetchBody(288, "1", null);

        assertNotNull(result);
        assertEquals(7, result.length);
        assertEquals("Hello\r\n", new String(result));
    }
    
    public void testExecuteFetchBodyLong() throws Throwable {
        StringBuffer buf = new StringBuffer();
        for(int i=0; i<128; i++) {
            buf.append((char)('A' + (i%32)));
        }
        String body1 = buf.toString();
        
        buf.setLength(0);
        for(int i=0; i<128; i++) {
            buf.append((char)('a' + (i%32)));
        }
        String body2 = buf.toString();

        buf.setLength(0);
        for(int i=0; i<32; i++) {
            buf.append((char)('1' + (i%10)));
        }
        String body3 = buf.toString();

        instance.setFetchIncrement(128);
        
        instance.addExecuteExpectation(
                "UID FETCH", "288 (BODY[1]<0.128>)",
                new String[] {
                        "* 84 FETCH (UID 288 BODY[1]<0> {128}\r\n" + body1 + ")"
                });
        instance.addExecuteExpectation(
                "UID FETCH", "288 (BODY[1]<128.128>)",
                new String[] {
                        "* 84 FETCH (UID 288 BODY[1]<128> {128}\r\n" + body2 + ")"
                });
        instance.addExecuteExpectation(
                "UID FETCH", "288 (BODY[1]<256.128>)",
                new String[] {
                        "* 84 FETCH (UID 288 BODY[1]<256> {32}\r\n" + body3 + ")"
                });

        byte[] result = instance.executeFetchBody(288, "1", null);

        assertNotNull(result);
        assertEquals(body1.length() + body2.length() + body3.length(), result.length);
        assertEquals(body1 + body2 + body3, new String(result));
    }
    
    public void testExecuteFetchBodyUntagged() throws Throwable {
        instance.addExecuteExpectation(
            "UID FETCH", "288 (BODY[1]<0.1024>)",
            new String[] {
                "* 83 FETCH (FLAGS (\\Answered \\Seen) UID 287)",
                "* 84 FETCH (UID 288 BODY[1]<0> {7}\r\nHello\r\n)"
            });

        byte[] result = instance.executeFetchBody(288, "1", null);

        assertNotNull(result);
        assertEquals(7, result.length);
        assertEquals("Hello\r\n", new String(result));
        
        String msg ="Untagged FETCH";
        assertEquals(msg, 1, listener.fetch.size());
        ImapProtocol.FetchFlagsResponse untaggedFetch = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(0);
        assertEquals(msg, 83, untaggedFetch.index);
        assertEquals(msg, 287, untaggedFetch.uid);
        assertTrue(msg, untaggedFetch.flags.seen);
        assertTrue(msg, untaggedFetch.flags.answered);
        assertTrue(msg, !untaggedFetch.flags.deleted);
        assertTrue(msg, !untaggedFetch.flags.draft);
        assertTrue(msg, !untaggedFetch.flags.flagged);
        assertTrue(msg, !untaggedFetch.flags.recent);
    }

    public void testExecuteStore1() throws Throwable {
        instance.addExecuteExpectation("UID STORE",
            "15 +FLAGS (\\Answered)",
            new String[] { "* 5 FETCH (FLAGS (\\Seen \\Answered) UID 15)" });

        instance.executeStore(15, true, new String[] { "\\Answered" });
        
        assertEquals(1, listener.fetch.size());
        ImapProtocol.FetchFlagsResponse result = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(0);

        assertEquals(5, result.index);
        assertEquals(15, result.uid);
        assertTrue(result.flags.seen);
        assertTrue(result.flags.answered);
        assertTrue(!result.flags.deleted);
        assertTrue(!result.flags.draft);
        assertTrue(!result.flags.flagged);
        assertTrue(!result.flags.recent);
    }

    public void testExecuteStore2() throws Throwable {
        instance.addExecuteExpectation("UID STORE",
            "15 -FLAGS (\\Answered)",
            new String[] { "* 5 FETCH (FLAGS () UID 15)" });

        instance.executeStore(15, false, new String[] { "\\Answered" });

        assertEquals(1, listener.fetch.size());
        ImapProtocol.FetchFlagsResponse result = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(0);

        assertEquals(5, result.index);
        assertEquals(15, result.uid);
        assertTrue(!result.flags.seen);
        assertTrue(!result.flags.answered);
        assertTrue(!result.flags.deleted);
        assertTrue(!result.flags.draft);
        assertTrue(!result.flags.flagged);
        assertTrue(!result.flags.recent);
    }

    public void testExecuteStore3() throws Throwable {
        instance.addExecuteExpectation("UID STORE",
            "36410 +FLAGS (\\Deleted)",
            new String[] {
                "* 5 FETCH (UID 36410 FLAGS (\\Deleted \\Seen $NotJunk NotJunk))"
            });

        instance.executeStore(36410, true, new String[] { "\\Deleted" });
        
        assertEquals(1, listener.fetch.size());
        ImapProtocol.FetchFlagsResponse result = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(0);

        assertEquals(5, result.index);
        assertEquals(36410, result.uid);
        assertTrue(result.flags.seen);
        assertTrue(!result.flags.answered);
        assertTrue(result.flags.deleted);
        assertTrue(!result.flags.draft);
        assertTrue(!result.flags.flagged);
        assertTrue(!result.flags.recent);
    }
    
    public void testExecuteStoreUntagged() throws Throwable {
        instance.addExecuteExpectation("UID STORE",
            "15 +FLAGS (\\Answered)",
            new String[] {
                "* 5 FETCH (FLAGS (\\Seen \\Answered) UID 15)",
                "* 85 FETCH (FLAGS (\\Recent \\Seen) UID 287)",
                "* 86 FETCH (FLAGS (\\Recent \\Seen) UID 288)"
            });

        instance.executeStore(15, true, new String[] { "\\Answered" });
        
        assertEquals(3, listener.fetch.size());
        ImapProtocol.FetchFlagsResponse result0 = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(0);
        ImapProtocol.FetchFlagsResponse result1 = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(1);
        ImapProtocol.FetchFlagsResponse result2 = (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(2);

        String msg = "5 FETCH";
        assertEquals(msg, 5, result0.index);
        assertEquals(msg, 15, result0.uid);
        assertTrue(msg, result0.flags.seen);
        assertTrue(msg, result0.flags.answered);
        assertTrue(msg, !result0.flags.deleted);
        assertTrue(msg, !result0.flags.draft);
        assertTrue(msg, !result0.flags.flagged);
        assertTrue(msg, !result0.flags.recent);
        
        msg = "85 FETCH";
        assertEquals(msg, 85, result1.index);
        assertEquals(msg, 287, result1.uid);
        assertTrue(msg, result1.flags.seen);
        assertTrue(msg, !result1.flags.answered);
        assertTrue(msg, !result1.flags.deleted);
        assertTrue(msg, !result1.flags.draft);
        assertTrue(msg, !result1.flags.flagged);
        assertTrue(msg, result1.flags.recent);
        
        msg = "86 FETCH";
        assertEquals(msg, 86, result2.index);
        assertEquals(msg, 288, result2.uid);
        assertTrue(msg, result2.flags.seen);
        assertTrue(msg, !result2.flags.answered);
        assertTrue(msg, !result2.flags.deleted);
        assertTrue(msg, !result2.flags.draft);
        assertTrue(msg, !result2.flags.flagged);
        assertTrue(msg, result2.flags.recent);
    }
   
    public void testExecuteExpunge() throws Throwable {
        instance.addExecuteExpectation("EXPUNGE", null,
                new String[] {
                    "* 80 EXPUNGE",
                    "* 81 EXPUNGE",
                    "* 78 EXISTS",
                    "* 2 RECENT"
                });
        
        instance.executeExpunge();
        
        assertEquals(2, listener.expunge.size());
        assertEquals(80, listener.expunge.elementAt(0));
        assertEquals(81, listener.expunge.elementAt(1));
    }
    
    public void testExecuteNoop() throws Throwable {
        instance.addExecuteExpectation("NOOP", null,
                new String[] {
                    "* OK Still here",
                    "* 80 EXPUNGE",
                    "* 81 EXPUNGE",
                    "* 78 EXISTS",
                    "* 2 RECENT",
                    "* 26 FETCH (FLAGS (\\Answered \\Seen))"
                });
        
        instance.executeNoop();
        
        assertEquals(2, listener.expunge.size());
        assertEquals(80, listener.expunge.elementAt(0));
        assertEquals(81, listener.expunge.elementAt(1));
        
        assertEquals(1, listener.exists.size());
        assertEquals(78, listener.exists.elementAt(0));
        
        assertEquals(1, listener.recent.size());
        assertEquals(2, listener.recent.elementAt(0));
        
        assertEquals(1, listener.fetch.size());
        ImapProtocol.FetchFlagsResponse fetchResponse =
            (ImapProtocol.FetchFlagsResponse)listener.fetch.elementAt(0);
        assertEquals(26, fetchResponse.index);
        assertEquals(-1, fetchResponse.uid);
        assertTrue(fetchResponse.flags.seen);
        assertTrue(fetchResponse.flags.answered);
        assertTrue(!fetchResponse.flags.flagged);
        assertTrue(!fetchResponse.flags.deleted);
        assertTrue(!fetchResponse.flags.draft);
        assertTrue(!fetchResponse.flags.recent);
        assertTrue(!fetchResponse.flags.forwarded);
        assertTrue(!fetchResponse.flags.junk);
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("ImapProtocol");

        suite.addTest(new ImapProtocolTest("executeCapability", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteCapability(); }}));
        
        suite.addTest(new ImapProtocolTest("executeNamespace1", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteNamespace1(); }}));
        suite.addTest(new ImapProtocolTest("executeNamespace2", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteNamespace2(); }}));

        suite.addTest(new ImapProtocolTest("executeList1", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteList1(); }}));
        suite.addTest(new ImapProtocolTest("executeList2", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteList2(); }}));
        suite.addTest(new ImapProtocolTest("executeList3", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteList3(); }}));
        suite.addTest(new ImapProtocolTest("executeList4", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteList4(); }}));
        suite.addTest(new ImapProtocolTest("executeList5", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteList5(); }}));
        suite.addTest(new ImapProtocolTest("executeList6", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteList6(); }}));
        suite.addTest(new ImapProtocolTest("executeList7", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteList7(); }}));

        suite.addTest(new ImapProtocolTest("executeFetchEnvelope1", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchEnvelope1(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchEnvelope2", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchEnvelope2(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchEnvelope3", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchEnvelope3(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchEnvelope4", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchEnvelope4(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchEnvelopeUntagged", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchEnvelopeUntagged(); }}));

        suite.addTest(new ImapProtocolTest("executeFetchFlags1", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchFlags1(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchFlags2", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchFlags2(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchFlagsUntagged", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchFlagsUntagged(); }}));
        
        suite.addTest(new ImapProtocolTest("executeFetchBody", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchBody(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchBodyLong", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchBodyLong(); }}));
        suite.addTest(new ImapProtocolTest("executeFetchBodyUntagged", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteFetchBodyUntagged(); }}));
        
        suite.addTest(new ImapProtocolTest("executeStore1", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteStore1(); }}));
        suite.addTest(new ImapProtocolTest("executeStore2", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteStore2(); }}));
        suite.addTest(new ImapProtocolTest("executeStore3", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteStore3(); }}));
        suite.addTest(new ImapProtocolTest("executeStoreUntagged", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteStoreUntagged(); }}));

        suite.addTest(new ImapProtocolTest("executeExpunge", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteExpunge(); }}));
        suite.addTest(new ImapProtocolTest("executeNoop", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapProtocolTest) tc).testExecuteNoop(); }}));
        
        return suite;
    }

    private static class ShimCallback implements ImapProtocol.FetchEnvelopeCallback {
        private Vector responses = new Vector();

        public void responseAvailable(
            ImapProtocol.FetchEnvelopeResponse response) {
            if (response != null) {
                responses.addElement(response);
            }
        }

        public ImapProtocol.FetchEnvelopeResponse[] getResponses() {
            ImapProtocol.FetchEnvelopeResponse[] result = new ImapProtocol.FetchEnvelopeResponse[responses.size()];
            responses.copyInto(result);

            return result;
        }
    }

    /**
     * Variation on ImapProtocol that overrides the "execute" method
     * to facilitate testing
     */
    class TestImapProtocol extends ImapProtocol {
        private Vector executeExpectations;
        private int fetchIncrement = 1024;

        public TestImapProtocol() {
            super();
            executeExpectations = new Vector();
        }

        public void addExecuteExpectation(String command, String arguments,
            String[] result) {
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

        protected String[] execute(String command, String arguments,
            MailProgressHandler progressHandler) {
            assertTrue("No expectations", !executeExpectations.isEmpty());

            ExecuteExpectation expect = (ExecuteExpectation) executeExpectations.firstElement();
            assertEquals("Bad command", expect.command, command);
            assertEquals("Bad arguments", expect.arguments, arguments);
            executeExpectations.removeElement(expect);

            return expect.result;
        }

        protected byte[][] executeResponse(String command, String arguments,
                MailProgressHandler progressHandler) throws IOException, MailException {
            assertTrue("No expectations", !executeExpectations.isEmpty());

            ExecuteExpectation expect = (ExecuteExpectation) executeExpectations.firstElement();
            assertEquals("Bad command", expect.command, command);
            assertEquals("Bad arguments", expect.arguments, arguments);
            executeExpectations.removeElement(expect);

            byte[][] result = new byte[expect.result.length][];
            for(int i=0; i<expect.result.length; i++) {
                result[i] = expect.result[i].getBytes();
            }
            return result;
        }
        
        protected void executeResponse(String command, String arguments,
                ExecuteCallback callback, MailProgressHandler progressHandler)
                throws IOException, MailException {
            assertTrue("No expectations", !executeExpectations.isEmpty());

            ExecuteExpectation expect = (ExecuteExpectation) executeExpectations.firstElement();
            assertEquals("Bad command", expect.command, command);
            assertEquals("Bad arguments", expect.arguments, arguments);
            executeExpectations.removeElement(expect);

            for(int i=0; i<expect.result.length; i++) {
                callback.processResponse(expect.result[i].getBytes());
            }
        }
        
        public void setFetchIncrement(int fetchIncrement) {
            this.fetchIncrement = fetchIncrement;
        }
        
        protected int getFetchIncrement(int previousIncrement, long previousTime) {
            return fetchIncrement;
        }
        
        private class ExecuteExpectation {
            public String command;
            public String arguments;
            public String[] result;
        }
    }
    
    class TestUntaggedResponseListener implements ImapProtocol.UntaggedResponseListener {
        public IntVector exists = new IntVector();
        public IntVector recent = new IntVector();
        public IntVector expunge = new IntVector();
        public Vector fetch = new Vector();

        public void existsResponse(int value) {
            exists.addElement(value);
        }
        public void recentResponse(int value) {
            recent.addElement(value);
        }
        public void expungeResponse(int value) {
            expunge.addElement(value);
        }
        public void fetchResponse(ImapProtocol.FetchFlagsResponse value) {
            fetch.addElement(value);
        }
    }
}
