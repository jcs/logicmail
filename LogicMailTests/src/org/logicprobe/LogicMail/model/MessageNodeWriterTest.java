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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;

import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.MimeMessagePart;
import org.logicprobe.LogicMail.message.MultiPart;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.MessageNode.Flag;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class MessageNodeWriterTest extends TestCase {
	private MessageNode messageNode;
	private ByteArrayOutputStream byteOutput;
	private DataOutputStream output;
	
	public MessageNodeWriterTest() {
	}

	public MessageNodeWriterTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

	public void setUp() {
		// Build a template message node only containing header data
		messageNode = new MessageNode(new FakeMessageToken());
		messageNode.setFlags(Flag.SEEN);
		messageNode.setDate(Calendar.getInstance().getTime());
		messageNode.setSubject("The subject");
		messageNode.setFrom(new Address[] { new Address("Foo Bar <foo@bar.com>") });
		messageNode.setSender(new Address[] { new Address("Foo Bar <foo@bar.com>") });
		messageNode.setReplyTo(new Address[] { new Address("Foo Bar <foo@bar.com>") });
		messageNode.setTo(new Address[] { new Address("Foo Bar <foo@bar.com>") });
		messageNode.setCc(new Address[] { new Address("Foo Bar <foo@bar.com>") });
		messageNode.setBcc(null);
		messageNode.setInReplyTo("12345678");
		messageNode.setMessageId("QWERTY");
		
		byteOutput = new ByteArrayOutputStream();
		output = new DataOutputStream(byteOutput);
    }

    public void tearDown() {
    	messageNode = null;
    	byteOutput = null;
    	output = null;
    }

    public void testBasicMessageNode() throws Throwable {
    	byte[] messageBytes = writeMessageToBytes();
    	
		ByteArrayInputStream byteInput = new ByteArrayInputStream(messageBytes);
		MessageNodeReader reader = new MessageNodeReader(new DataInputStream(byteInput));
		
		MessageNode resultNode = reader.readMessageNode();
		validateMessageHeaders(resultNode);
		
		assertNull("structure", messageNode.getMessageStructure());
		assertEquals("content", 0, messageNode.getAllMessageContent().length);
    }
    
    public void testBasicMessageToken() throws Throwable {
    	byte[] messageBytes = writeMessageToBytes();
    	
		ByteArrayInputStream byteInput = new ByteArrayInputStream(messageBytes);
		MessageNodeReader reader = new MessageNodeReader(new DataInputStream(byteInput));
		
		MessageToken resultToken = reader.read();
		assertTrue("token", resultToken instanceof FakeMessageToken);
		assertEquals("token", 42, resultToken.getUniqueId());
    }
    
    public void testMessageStructure() throws Throwable {
    	MultiPart rootPart = new MultiPart("alternative");
    	rootPart.addPart(new TextPart("plain", "", "", "", "", "", -1));
    	rootPart.addPart(new TextPart("html", "", "", "", "", "", -1));
    	messageNode.setMessageStructure(rootPart);
    	
    	byte[] messageBytes = writeMessageToBytes();
		ByteArrayInputStream byteInput = new ByteArrayInputStream(messageBytes);
		MessageNodeReader reader = new MessageNodeReader(new DataInputStream(byteInput));

		MessageNode resultNode = reader.readMessageNode();
		validateMessageHeaders(resultNode);
		
		MimeMessagePart resultPart = resultNode.getMessageStructure();
		assertNotNull("messageStructure", resultPart);
		assertTrue(resultPart instanceof MultiPart);
		assertEquals("alternative", resultPart.getMimeSubtype());
		MimeMessagePart[] childResultParts = ((MultiPart)resultPart).getParts();
		assertNotNull(childResultParts);
		assertEquals(2, childResultParts.length);
		assertTrue(childResultParts[0] instanceof TextPart);
		assertEquals("plain", childResultParts[0].getMimeSubtype());
		assertTrue(childResultParts[1] instanceof TextPart);
		assertEquals("html", childResultParts[1].getMimeSubtype());
    }
    
    public void testMessageContent() throws Throwable {
    	TextPart rootPart = new TextPart("plain", "", "", "", "", "", -1);
    	messageNode.setMessageStructure(rootPart);
    	messageNode.putMessageContent(new TextContent(rootPart, "This is text"));
    	
    	byte[] messageBytes = writeMessageToBytes();
		ByteArrayInputStream byteInput = new ByteArrayInputStream(messageBytes);
		MessageNodeReader reader = new MessageNodeReader(new DataInputStream(byteInput));

		MessageNode resultNode = reader.readMessageNode();
		validateMessageHeaders(resultNode);
		
		MimeMessagePart resultPart = resultNode.getMessageStructure();
		assertNotNull("messageStructure", resultPart);
		assertTrue(resultPart instanceof TextPart);
		assertEquals("plain", resultPart.getMimeSubtype());
		
		assertTrue("contentAvailable", reader.isContentAvailable());
		MimeMessageContent content = reader.getNextContent();
		assertTrue(content instanceof TextContent);
		assertEquals(rootPart.getUniqueId(), content.getMessagePart().getUniqueId());
		assertEquals("This is text", ((TextContent)content).getText());
    }
    
	private byte[] writeMessageToBytes() throws IOException {
		MessageNodeWriter writer = new MessageNodeWriter(output);
    	writer.write(messageNode);
    	byte[] messageBytes = byteOutput.toByteArray();
    	assertTrue("messageBytes", messageBytes.length > 0);
		return messageBytes;
	}
	
    private void validateMessageHeaders(MessageNode resultNode) {
		MessageToken resultToken = resultNode.getMessageToken();
		assertTrue("token", resultToken instanceof FakeMessageToken);
		assertEquals("token", 42, resultToken.getUniqueId());
		
		assertEquals("flags", messageNode.getFlags(), resultNode.getFlags());
		assertEquals("date", messageNode.getDate().getTime(), resultNode.getDate().getTime());
		assertEquals("subject", messageNode.getSubject(), resultNode.getSubject());
		assertEquals("from", messageNode.getFrom(), resultNode.getFrom());
		assertEquals("sender", messageNode.getSender(), resultNode.getSender());
		assertEquals("replyTo", messageNode.getReplyTo(), resultNode.getReplyTo());
		assertEquals("to", messageNode.getTo(), resultNode.getTo());
		assertEquals("cc", messageNode.getCc(), resultNode.getCc());
		assertEquals("bcc", messageNode.getBcc(), resultNode.getBcc());
		assertEquals("inReplyTo", messageNode.getInReplyTo(), resultNode.getInReplyTo());
		assertEquals("messageId", messageNode.getMessageId(), resultNode.getMessageId());
    }
    
    private void assertEquals(String message, Address[] expected, Address[] actual) {
    	if(expected == actual) { return; }
    	if(expected != null && expected.length == 0 && actual == null) { return; }
    	if(actual != null && actual.length == 0 && expected == null) { return; }
    	assertNotNull(message, expected);
    	assertNotNull(message, actual);
    	assertEquals(message, expected.length, actual.length);
    	for(int i=0; i<expected.length; i++) {
    		assertEquals(message, expected[i].toString(), actual[i].toString());
    	}
    }
    
    public static class FakeMessageToken implements MessageToken {
		public boolean containedWithin(FolderTreeItem folderTreeItem) {
			return false;
		}

		public void serialize(DataOutputStream output) throws IOException {
			output.writeInt(42);
		}
		
		public void deserialize(DataInputStream input) throws IOException {
			input.readInt();
		}

		public long getUniqueId() {
			return 42;
		}
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("MessageNodeWriterTest");
        suite.addTest(new MessageNodeWriterTest("basicMessageNode", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageNodeWriterTest)tc).testBasicMessageNode(); } }));
        suite.addTest(new MessageNodeWriterTest("basicMessageToken", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageNodeWriterTest)tc).testBasicMessageToken(); } }));
        suite.addTest(new MessageNodeWriterTest("messageStructure", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageNodeWriterTest)tc).testMessageStructure(); } }));
        suite.addTest(new MessageNodeWriterTest("messageContent", new TestMethod()
        { public void run(TestCase tc) throws Throwable {((MessageNodeWriterTest)tc).testMessageContent(); } }));
        
        return suite;
    }
}
