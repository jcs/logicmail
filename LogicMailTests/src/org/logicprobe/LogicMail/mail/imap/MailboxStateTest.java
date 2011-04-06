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
package org.logicprobe.LogicMail.mail.imap;

import java.util.Vector;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class MailboxStateTest extends TestCase {
    private MailboxState instance;
    
    public MailboxStateTest() {
    }

    public MailboxStateTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void setUp() {
        instance = new MailboxState();
    }

    public void tearDown() {
        instance = null;
    }

    public void testMailboxSelected() throws Throwable {
        ImapProtocol.SelectResponse response = new ImapProtocol.SelectResponse();
        response.exists = 50;
        response.recent = 5;
        response.unseen = 2;
        response.uidNext = 104;
        response.uidValidity = 123456;
        
        instance.mailboxSelected(response);
        assertEquals(response.exists, instance.getExists());
        assertEquals(response.recent, instance.getRecent());
        assertEquals(response.unseen, instance.getUnseen());
        assertEquals(response.uidNext, instance.getUidNext());
        assertEquals(response.uidValidity, instance.getUidValidity());
    }
    
    public void testMailboxReselected() throws Throwable {
        ImapProtocol.SelectResponse response = new ImapProtocol.SelectResponse();
        response.exists = 50;
        response.recent = 5;
        response.unseen = 2;
        response.uidNext = 104;
        response.uidValidity = 123456;

        ImapMessageToken dummyToken = new ImapMessageToken("INBOX", 100);
        dummyToken.setMessageIndex(48);
        
        String msg;
        
        msg = "Normal select";
        instance.clear();
        instance.mailboxSelected(response);
        instance.messageFetched(dummyToken);
        assertEquals(msg, dummyToken, instance.getMessageToken(48));
        
        msg = "Reselect";
        instance.clear();
        instance.mailboxSelected(response);
        instance.messageFetched(dummyToken);
        instance.mailboxSelected(response);
        assertEquals(msg, dummyToken, instance.getMessageToken(48));
        
        msg = "Reselect after increment";
        instance.clear();
        instance.mailboxSelected(response);
        instance.messageFetched(dummyToken);
        response.exists += 5;
        response.uidNext += 5;
        instance.mailboxSelected(response);
        assertEquals(msg, dummyToken, instance.getMessageToken(48));
        
        msg = "Reselect after EXISTS change";
        instance.clear();
        instance.mailboxSelected(response);
        instance.messageFetched(dummyToken);
        response.exists--;
        instance.mailboxSelected(response);
        assertNull(msg, instance.getMessageToken(48));
        
        msg = "Reselect after UIDNEXT change";
        instance.clear();
        instance.mailboxSelected(response);
        instance.messageFetched(dummyToken);
        response.uidNext++;
        instance.mailboxSelected(response);
        assertNull(msg, instance.getMessageToken(48));
    }
    
    private ImapMessageToken[] fetchMessageBlock(int startingIndex, int size) {
        ImapMessageToken[] tokens = new ImapMessageToken[size];
        for(int i=0; i<tokens.length; i++) {
            tokens[i] = new ImapMessageToken("INBOX", 100 + startingIndex + i);
            tokens[i].setMessageIndex(startingIndex + i);
            instance.messageFetched(tokens[i]);
        }
        return tokens;
    }
    
    public void testMessageFetched() throws Throwable {
        ImapMessageToken[] tokens = fetchMessageBlock(10, 5);

        for(int i=tokens.length-1; i!=0; --i) {
            assertEquals(tokens[i], instance.getMessageToken(10 + i));
        }
    }
    
    public void testMessageExpungedSingle() throws Throwable {
        // These tests all begin with a contiguous block of 5 messages:
        // [10](110), [11](111), [12](112), [13](113), [14](114)
        ImapMessageToken[] tokens;
        Vector updatedTokens = new Vector();
        String msg;

        msg = "Expunge the last message";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(14, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[1], instance.getMessageToken(11));
        assertEquals(msg, tokens[2], instance.getMessageToken(12));
        assertEquals(msg, tokens[3], instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge the first message";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 4, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[1], instance.getMessageToken(10));
        assertEquals(msg, tokens[2], instance.getMessageToken(11));
        assertEquals(msg, tokens[3], instance.getMessageToken(12));
        assertEquals(msg, tokens[4], instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));

        msg = "Expunge the middle message";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(12, updatedTokens);
        assertEquals(msg, 2, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[1], instance.getMessageToken(11));
        assertEquals(msg, tokens[3], instance.getMessageToken(12));
        assertEquals(msg, tokens[4], instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
    }
    
    public void testMessageExpungedMultiple() throws Throwable {
        // These tests all begin with a contiguous block of 5 messages:
        // [10](110), [11](111), [12](112), [13](113), [14](114)
        ImapMessageToken[] tokens;
        Vector updatedTokens = new Vector();
        String msg;
    
        msg = "Expunge the last two messages, lower-to-higher";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(13, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(13, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[1], instance.getMessageToken(11));
        assertEquals(msg, tokens[2], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge the last two messages, higher-to-lower";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(14, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(13, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[1], instance.getMessageToken(11));
        assertEquals(msg, tokens[2], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge the first two messages, lower-to-higher";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 4, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 3, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[2], instance.getMessageToken(10));
        assertEquals(msg, tokens[3], instance.getMessageToken(11));
        assertEquals(msg, tokens[4], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge the first two messages, higher-to-lower";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(11, updatedTokens);
        assertEquals(msg, 3, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 3, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[2], instance.getMessageToken(10));
        assertEquals(msg, tokens[3], instance.getMessageToken(11));
        assertEquals(msg, tokens[4], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge the middle two messages, lower-to-higher";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(12, updatedTokens);
        assertEquals(msg, 2, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(12, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[1], instance.getMessageToken(11));
        assertEquals(msg, tokens[4], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge the middle two messages, higher-to-lower";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(13, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(12, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[1], instance.getMessageToken(11));
        assertEquals(msg, tokens[4], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));

        msg = "Expunge the outer two messages, lower-to-higher";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 4, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(13, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[1], instance.getMessageToken(10));
        assertEquals(msg, tokens[2], instance.getMessageToken(11));
        assertEquals(msg, tokens[3], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge the outer two messages, higher-to-lower";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(14, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 3, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[1], instance.getMessageToken(10));
        assertEquals(msg, tokens[2], instance.getMessageToken(11));
        assertEquals(msg, tokens[3], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge two split messages, lower-to-higher";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(11, updatedTokens);
        assertEquals(msg, 3, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(12, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[2], instance.getMessageToken(11));
        assertEquals(msg, tokens[4], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge two split messages, higher-to-lower";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(13, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(11, updatedTokens);
        assertEquals(msg, 2, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[2], instance.getMessageToken(11));
        assertEquals(msg, tokens[4], instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge all messages, lower-to-higher";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 4, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 3, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 2, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertNull(msg, instance.getMessageToken(10));
        assertNull(msg, instance.getMessageToken(11));
        assertNull(msg, instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
        
        msg = "Expunge all messages, higher-to-lower";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(14, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(13, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(12, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(11, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertNull(msg, instance.getMessageToken(10));
        assertNull(msg, instance.getMessageToken(11));
        assertNull(msg, instance.getMessageToken(12));
        assertNull(msg, instance.getMessageToken(13));
        assertNull(msg, instance.getMessageToken(14));
    }
    
    public void testMessageExpungedUnfetched() throws Throwable {
        // These tests all begin with a contiguous block of 5 messages:
        // [10](110), [11](111), [12](112), [13](113), [14](114)
        ImapMessageToken[] tokens;
        Vector updatedTokens = new Vector();
        String msg;

        msg = "Expunge un-fetched message, higher";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(20, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(10));
        assertEquals(msg, tokens[1], instance.getMessageToken(11));
        assertEquals(msg, tokens[2], instance.getMessageToken(12));
        assertEquals(msg, tokens[3], instance.getMessageToken(13));
        assertEquals(msg, tokens[4], instance.getMessageToken(14));
        
        msg = "Expunge un-fetched message, lower";
        instance.clear();
        tokens = fetchMessageBlock(10, 5);
        instance.messageExpunged(5, updatedTokens);
        assertEquals(msg, 5, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens[0], instance.getMessageToken(9));
        assertEquals(msg, tokens[1], instance.getMessageToken(10));
        assertEquals(msg, tokens[2], instance.getMessageToken(11));
        assertEquals(msg, tokens[3], instance.getMessageToken(12));
        assertEquals(msg, tokens[4], instance.getMessageToken(13));
    }
    
    public void testMessageExpungedNoncontiguous() throws Throwable {
        // These tests all begin with a noncontiguous block of 6 messages:
        // [10](110), [11](111), [12](112), ..., [20](120), [21](121), [22](122)
        ImapMessageToken[] tokens1;
        ImapMessageToken[] tokens2;
        Vector updatedTokens = new Vector();
        String msg;
        
        msg = "Expunge the last message";
        instance.clear();
        tokens1 = fetchMessageBlock(10, 3);
        tokens2 = fetchMessageBlock(20, 3);
        instance.messageExpunged(22, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens1[0], instance.getMessageToken(10));
        assertEquals(msg, tokens1[1], instance.getMessageToken(11));
        assertEquals(msg, tokens1[2], instance.getMessageToken(12));
        assertEquals(msg, tokens2[0], instance.getMessageToken(20));
        assertEquals(msg, tokens2[1], instance.getMessageToken(21));
        assertNull(msg, instance.getMessageToken(22));
        
        msg = "Expunge the first message";
        instance.clear();
        tokens1 = fetchMessageBlock(10, 3);
        tokens2 = fetchMessageBlock(20, 3);
        instance.messageExpunged(10, updatedTokens);
        assertEquals(msg, 5, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens1[1], instance.getMessageToken(10));
        assertEquals(msg, tokens1[2], instance.getMessageToken(11));
        assertEquals(msg, tokens2[0], instance.getMessageToken(19));
        assertEquals(msg, tokens2[1], instance.getMessageToken(20));
        assertEquals(msg, tokens2[2], instance.getMessageToken(21));
        assertNull(msg, instance.getMessageToken(22));

        msg = "Expunge the middle message, first block";
        instance.clear();
        tokens1 = fetchMessageBlock(10, 3);
        tokens2 = fetchMessageBlock(20, 3);
        instance.messageExpunged(11, updatedTokens);
        assertEquals(msg, 4, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens1[0], instance.getMessageToken(10));
        assertEquals(msg, tokens1[2], instance.getMessageToken(11));
        assertEquals(msg, tokens2[0], instance.getMessageToken(19));
        assertEquals(msg, tokens2[1], instance.getMessageToken(20));
        assertEquals(msg, tokens2[2], instance.getMessageToken(21));
        assertNull(msg, instance.getMessageToken(22));
        
        msg = "Expunge the middle message, second block";
        instance.clear();
        tokens1 = fetchMessageBlock(10, 3);
        tokens2 = fetchMessageBlock(20, 3);
        instance.messageExpunged(21, updatedTokens);
        assertEquals(msg, 1, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens1[0], instance.getMessageToken(10));
        assertEquals(msg, tokens1[1], instance.getMessageToken(11));
        assertEquals(msg, tokens1[2], instance.getMessageToken(12));
        assertEquals(msg, tokens2[0], instance.getMessageToken(20));
        assertEquals(msg, tokens2[2], instance.getMessageToken(21));
        assertNull(msg, instance.getMessageToken(22));
    }
    
    public void testMessageExpungedNoncontiguousUnfetched() throws Throwable {
        // These tests all begin with a noncontiguous block of 6 messages:
        // [10](110), [11](111), [12](112), ..., [20](120), [21](121), [22](122)
        ImapMessageToken[] tokens1;
        ImapMessageToken[] tokens2;
        Vector updatedTokens = new Vector();
        String msg;
        
        msg = "Expunge un-fetched message, lower";
        instance.clear();
        tokens1 = fetchMessageBlock(10, 3);
        tokens2 = fetchMessageBlock(20, 3);
        instance.messageExpunged(5, updatedTokens);
        assertEquals(msg, 6, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens1[0], instance.getMessageToken(9));
        assertEquals(msg, tokens1[1], instance.getMessageToken(10));
        assertEquals(msg, tokens1[2], instance.getMessageToken(11));
        assertEquals(msg, tokens2[0], instance.getMessageToken(19));
        assertEquals(msg, tokens2[1], instance.getMessageToken(20));
        assertEquals(msg, tokens2[2], instance.getMessageToken(21));
        
        msg = "Expunge un-fetched message, middle";
        instance.clear();
        tokens1 = fetchMessageBlock(10, 3);
        tokens2 = fetchMessageBlock(20, 3);
        instance.messageExpunged(16, updatedTokens);
        assertEquals(msg, 3, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens1[0], instance.getMessageToken(10));
        assertEquals(msg, tokens1[1], instance.getMessageToken(11));
        assertEquals(msg, tokens1[2], instance.getMessageToken(12));
        assertEquals(msg, tokens2[0], instance.getMessageToken(19));
        assertEquals(msg, tokens2[1], instance.getMessageToken(20));
        assertEquals(msg, tokens2[2], instance.getMessageToken(21));
        
        msg = "Expunge un-fetched message, higher";
        instance.clear();
        tokens1 = fetchMessageBlock(10, 3);
        tokens2 = fetchMessageBlock(20, 3);
        instance.messageExpunged(30, updatedTokens);
        assertEquals(msg, 0, updatedTokens.size()); updatedTokens.removeAllElements();
        assertEquals(msg, tokens1[0], instance.getMessageToken(10));
        assertEquals(msg, tokens1[1], instance.getMessageToken(11));
        assertEquals(msg, tokens1[2], instance.getMessageToken(12));
        assertEquals(msg, tokens2[0], instance.getMessageToken(20));
        assertEquals(msg, tokens2[1], instance.getMessageToken(21));
        assertEquals(msg, tokens2[2], instance.getMessageToken(22));
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("MailboxState");

        suite.addTest(new MailboxStateTest("mailboxSelected", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMailboxSelected(); }}));
        suite.addTest(new MailboxStateTest("mailboxReselected", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMailboxReselected(); }}));
        suite.addTest(new MailboxStateTest("messageFetched", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMessageFetched(); }}));
        suite.addTest(new MailboxStateTest("messageExpungedSingle", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMessageExpungedSingle(); }}));
        suite.addTest(new MailboxStateTest("messageExpungedMultiple", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMessageExpungedMultiple(); }}));
        suite.addTest(new MailboxStateTest("messageExpungedUnfetched", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMessageExpungedUnfetched(); }}));
        suite.addTest(new MailboxStateTest("messageExpungedNoncontiguous", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMessageExpungedNoncontiguous(); }}));
        suite.addTest(new MailboxStateTest("messageExpungedNoncontiguousUnfetched", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((MailboxStateTest) tc).testMessageExpungedNoncontiguousUnfetched(); }}));
        
        return suite;
    }
}
