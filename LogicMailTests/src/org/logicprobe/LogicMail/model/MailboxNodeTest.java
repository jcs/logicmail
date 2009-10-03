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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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
    	instance = new MailboxNode();
    }
    
    public void tearDown() {
    	instance = null;
    }
    
    public void testAddMessage() {
    	MessageNode message1 = new MessageNode(new FolderMessage(new FakeMessageToken(1), new MessageEnvelope(), 1, 11));
    	MessageNode message2 = new MessageNode(new FolderMessage(new FakeMessageToken(2), new MessageEnvelope(), 2, 12));
    	MessageNode message3 = new MessageNode(new FolderMessage(new FakeMessageToken(3), new MessageEnvelope(), 3, 13));
    	MessageNode message4 = new MessageNode(new FolderMessage(new FakeMessageToken(4), new MessageEnvelope(), 4, 14));
    	
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
    
    public Test suite() {
        TestSuite suite = new TestSuite("MailboxNode");

        suite.addTest(new MailboxNodeTest("addMessage", new TestMethod()
        { public void run(TestCase tc) {((MailboxNodeTest)tc).testAddMessage(); } }));

        return suite;
    }

    private class FakeMessageToken implements MessageToken {
		private long uniqueId;
		public FakeMessageToken(long uniqueId) { this.uniqueId = uniqueId; }
		public long getUniqueId() { return uniqueId; }
		public void deserialize(DataInput input) throws IOException { }
		public void serialize(DataOutput output) throws IOException { }
		public boolean containedWithin(FolderTreeItem folderTreeItem) { return true; }
	}
}
