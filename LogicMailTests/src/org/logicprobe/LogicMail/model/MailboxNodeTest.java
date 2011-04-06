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
package org.logicprobe.LogicMail.model;

import java.util.Date;

import org.logicprobe.LogicMail.mail.FakeMessageToken;
import org.logicprobe.LogicMail.mail.FolderExpungedEvent;
import org.logicprobe.LogicMail.mail.FolderMessagesEvent;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class MailboxNodeTest extends TestCase {
	private MailboxNode instance;
	
    /** Creates a new instance of MailboxNodeTest */
    public MailboxNodeTest() {
    }
    
    public MailboxNodeTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    	instance = new MailboxNode(new FolderTreeItem("INBOX", "INBOX", "."));
    }
    
    public void tearDown() {
    	instance = null;
    }
    
    public void testAddMessage() {
    	MessageNode message1 = new MessageNode(createFolderMessage(1, 1, 1));
    	MessageNode message2 = new MessageNode(createFolderMessage(2, 2, 2));
    	MessageNode message3 = new MessageNode(createFolderMessage(3, 3, 3));
    	MessageNode message4 = new MessageNode(createFolderMessage(4, 4, 4));
    	
    	// Add messages in-order
    	instance.addMessage(message1);
    	instance.addMessage(message2);
    	instance.addMessage(message3);
    	instance.addMessage(message4);
    	
    	MessageNode[] messageNodes = instance.getMessages();
    	assertEquals(4, messageNodes.length);
    	assertEquals(message1, messageNodes[0]);
    	assertEquals(message2, messageNodes[1]);
    	assertEquals(message3, messageNodes[2]);
    	assertEquals(message4, messageNodes[3]);
    	
    	// Clear the messages
    	instance.clearMessages();
    	messageNodes = instance.getMessages();
    	assertEquals(0, messageNodes.length);
    	
    	// Add messages out-of-order
    	instance.addMessage(message4);
    	instance.addMessage(message1);
    	instance.addMessage(message3);
    	instance.addMessage(message2);
    	
    	messageNodes = instance.getMessages();
    	assertEquals(4, messageNodes.length);
    	assertEquals(message1, messageNodes[0]);
    	assertEquals(message2, messageNodes[1]);
    	assertEquals(message3, messageNodes[2]);
    	assertEquals(message4, messageNodes[3]);
    }
    
    public void testAddMessagesFromEvent() {
        FolderMessage message1 = createFolderMessage(1, 1, 1);
        FolderMessage message2 = createFolderMessage(2, 2, 2);
        FolderMessage message3 = createFolderMessage(3, 3, 3);
        FolderMessage message4 = createFolderMessage(4, 4, 4);
        
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));
        
        MessageNode[] messageNodes = instance.getMessages();
        assertEquals(4, messageNodes.length);
        assertEquals(message1.getMessageToken(), messageNodes[0].getMessageToken());
        assertEquals(message2.getMessageToken(), messageNodes[1].getMessageToken());
        assertEquals(message3.getMessageToken(), messageNodes[2].getMessageToken());
        assertEquals(message4.getMessageToken(), messageNodes[3].getMessageToken());
    }
    
    public void testHasMoreLoadableMessages() {
        assertTrue("Empty mailbox", instance.hasMoreLoadableMessages());
        
        // Initial set of messages, assuming no more loadable
        FolderMessage message1 = createFolderMessage(1, 1, 1);
        FolderMessage message2 = createFolderMessage(2, 2, 2);
        FolderMessage message3 = createFolderMessage(3, 3, 3);
        FolderMessage message4 = createFolderMessage(4, 4, 4);

        // Need to add from event, since this is the only place where the
        // mailbox is able to directly read FolderMessage properties
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));

        assertTrue("No more loadable", !instance.hasMoreLoadableMessages());

        // Fake an event to remove the first message
        instance.mailStoreFolderExpunged(new FolderExpungedEvent(
                this, instance.getFolderTreeItem(),
                new MessageToken[] { new FakeMessageToken(1) },
                new MessageToken[0]));

        assertTrue("Removed and has more loadable", instance.hasMoreLoadableMessages());
        
        instance.clearMessages();

        // Set of messages, assuming more are loadable
        message1 = createFolderMessage(1, 11, 1);
        message2 = createFolderMessage(2, 12, 2);
        message3 = createFolderMessage(3, 13, 3);
        message4 = createFolderMessage(4, 14, 4);

        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));

        assertTrue("Has more loadable", instance.hasMoreLoadableMessages());
    }

    public void testFindMessageNodeGaps() {
        // Empty mailbox test
        MessageNode[][] result = instance.findMessageNodeGaps();
        String msg = "Empty mailbox";
        assertNotNull(msg, result);
        assertEquals(msg, 0, result.length);
        
        // Initial set of messages, assuming no more loadable
        FolderMessage message1 = createFolderMessage(1, 1, 1);
        FolderMessage message2 = createFolderMessage(2, 2, 2);
        FolderMessage message3 = createFolderMessage(3, 3, 3);
        FolderMessage message4 = createFolderMessage(4, 4, 4);
        
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));
        
        result = instance.findMessageNodeGaps();
        msg = "No gaps";
        assertNotNull(msg, result);
        assertEquals(msg, 0, result.length);
        
        instance.clearMessages();
        
        message1 = createFolderMessage(1, 11, 1);
        message2 = createFolderMessage(2, 12, 2);
        message3 = createFolderMessage(3, 13, 3);
        message4 = createFolderMessage(4, 14, 4);
        
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));
        
        result = instance.findMessageNodeGaps();
        msg = "Gap at start";
        assertNotNull(msg, result);
        assertEquals(msg, 1, result.length);
        MessageNode[] gap = result[0];
        assertNotNull(msg, gap);
        assertEquals(msg, 2, gap.length);
        assertNull(msg, gap[0]);
        assertEquals(msg, new FakeMessageToken(1), gap[1].getMessageToken());
        
        instance.clearMessages();
        
        message1 = createFolderMessage(1, 1, 1);
        message2 = createFolderMessage(2, 2, 2);
        message3 = createFolderMessage(3, 5, 3);
        message4 = createFolderMessage(4, 6, 4);
        
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));
        
        result = instance.findMessageNodeGaps();
        msg = "Gap in middle";
        assertNotNull(msg, result);
        assertEquals(msg, 1, result.length);
        gap = result[0];
        assertNotNull(msg, gap);
        assertEquals(msg, 2, gap.length);
        assertEquals(msg, new FakeMessageToken(2), gap[0].getMessageToken());
        assertEquals(msg, new FakeMessageToken(3), gap[1].getMessageToken());
    }
    
    public void testFindMessageNodeGapsMultiple() {
        // Two gaps in the middle
        FolderMessage message1 = createFolderMessage(10, 1, 1);
        FolderMessage message2 = createFolderMessage(20, 5, 2);
        FolderMessage message3 = createFolderMessage(30, 6, 3);
        FolderMessage message4 = createFolderMessage(40, 10, 4);
        
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));
        
        MessageNode[][] result = instance.findMessageNodeGaps();
        String msg = "Gaps between 10-20 and 30-40";
        assertNotNull(msg, result);
        assertEquals(msg, 2, result.length);
        boolean found1 = false;
        boolean found2 = false;
        for(int i=0; i<result.length; i++) {
            MessageNode[] gap = result[i];
            assertNotNull(msg, gap);
            assertNotNull(msg, gap[0]);
            assertNotNull(msg, gap[1]);
            if(gap[0].getMessageToken().getUniqueId() == 10
                    && gap[1].getMessageToken().getUniqueId() == 20) {
                found1 = true;
            }
            else if(gap[0].getMessageToken().getUniqueId() == 30
                    && gap[1].getMessageToken().getUniqueId() == 40) {
                found2 = true;
            }
        }
        assertTrue(msg, found1 && found2);
    }

    public void testFolderExpunged() {
        // Create our initial set of messages
        FolderMessage message1 = createFolderMessage(1, 1, 1);
        FolderMessage message2 = createFolderMessage(2, 2, 2);
        FolderMessage message3 = createFolderMessage(3, 3, 3);
        FolderMessage message4 = createFolderMessage(4, 4, 4);
        
        // These tests all verify correct expunge handling, which includes
        // the removal of messages without gap creation.
        
        String msg = "Two at the end";
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));

        instance.mailStoreFolderExpunged(new FolderExpungedEvent(
                this, instance.getFolderTreeItem(),
                new MessageToken[] {
                    message3.getMessageToken(), message4.getMessageToken()
                },
                new MessageToken[0]));
        
        MessageNode[] messageNodes = instance.getMessages();
        assertEquals(msg, 2, messageNodes.length);
        assertEquals(msg, message1.getMessageToken(), messageNodes[0].getMessageToken());
        assertEquals(msg, message2.getMessageToken(), messageNodes[1].getMessageToken());
        MessageNode[][] messageGaps = instance.findMessageNodeGaps();
        assertEquals(msg, 0, messageGaps.length);
        instance.clearMessages();

        msg = "Two at the beginning";
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));

        MessageToken updatedToken3 = message3.getMessageToken().clone();
        updatedToken3.updateMessageIndex(1);
        MessageToken updatedToken4 = message4.getMessageToken().clone();
        updatedToken4.updateMessageIndex(2);
        
        instance.mailStoreFolderExpunged(new FolderExpungedEvent(
                this, instance.getFolderTreeItem(),
                new MessageToken[] {
                    message1.getMessageToken(), message2.getMessageToken()
                },
                new MessageToken[] { updatedToken3, updatedToken4 }));
        
        messageNodes = instance.getMessages();
        assertEquals(msg, 2, messageNodes.length);
        assertEquals(msg, message3.getMessageToken(), messageNodes[0].getMessageToken());
        assertEquals(msg, message4.getMessageToken(), messageNodes[1].getMessageToken());
        messageGaps = instance.findMessageNodeGaps();
        assertEquals(msg, 0, messageGaps.length);
        instance.clearMessages();

        msg = "One in the middle";
        instance.mailStoreFolderMessagesAvailable(new FolderMessagesEvent(
                this, instance.getFolderTreeItem(), new FolderMessage[] {
                    message1, message2, message3, message4
                }));

        updatedToken4 = message4.getMessageToken().clone();
        updatedToken4.updateMessageIndex(3);
        
        instance.mailStoreFolderExpunged(new FolderExpungedEvent(
                this, instance.getFolderTreeItem(),
                new MessageToken[] {
                    message3.getMessageToken()
                },
                new MessageToken[] { updatedToken4 }));

        messageNodes = instance.getMessages();
        assertEquals(msg, 3, messageNodes.length);
        assertEquals(msg, message1.getMessageToken(), messageNodes[0].getMessageToken());
        assertEquals(msg, message2.getMessageToken(), messageNodes[1].getMessageToken());
        assertEquals(msg, message4.getMessageToken(), messageNodes[2].getMessageToken());
        
        messageGaps = instance.findMessageNodeGaps();
        assertEquals(msg, 0, messageGaps.length);
    }
    
    private static FolderMessage createFolderMessage(int uid, int index, int sortOrder) {
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.subject = "Test " + uid;
        envelope.date = new Date(1286920000 + sortOrder);
        FakeMessageToken token = new FakeMessageToken(uid);
        token.updateMessageIndex(index);
        FolderMessage message = new FolderMessage(token, envelope, index, uid, -1);
        return message;
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("MailboxNode");

        suite.addTest(new MailboxNodeTest("addMessage", new TestMethod()
        { public void run(TestCase tc) {((MailboxNodeTest)tc).testAddMessage(); } }));
        suite.addTest(new MailboxNodeTest("addMessagesFromEvent", new TestMethod()
        { public void run(TestCase tc) {((MailboxNodeTest)tc).testAddMessagesFromEvent(); } }));
        suite.addTest(new MailboxNodeTest("hasMoreLoadableMessages", new TestMethod()
        { public void run(TestCase tc) {((MailboxNodeTest)tc).testHasMoreLoadableMessages(); } }));
        suite.addTest(new MailboxNodeTest("findMessageNodeGaps", new TestMethod()
        { public void run(TestCase tc) {((MailboxNodeTest)tc).testFindMessageNodeGaps(); } }));
        suite.addTest(new MailboxNodeTest("findMessageNodeGapsMultiple", new TestMethod()
        { public void run(TestCase tc) {((MailboxNodeTest)tc).testFindMessageNodeGapsMultiple(); } }));
        suite.addTest(new MailboxNodeTest("folderExpunged", new TestMethod()
        { public void run(TestCase tc) {((MailboxNodeTest)tc).testFolderExpunged(); } }));
        
        return suite;
    }
}
