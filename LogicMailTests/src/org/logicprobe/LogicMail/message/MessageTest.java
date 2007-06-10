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

package org.logicprobe.LogicMail.message;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;
import java.util.Calendar;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * Unit test for MessageReplyConverter
 */
public class MessageTest extends TestCase {
    private String sampleText;
    private MessageEnvelope envelope;
    private MessagePart body;
    private Message message;
    
    public MessageTest() {
    }
    
    public MessageTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
        sampleText =
                "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
                "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
                "ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
                "nisi ut aliquip ex ea commodo consequat.";

        body = new TextPart("plain", sampleText);
        
        envelope = new MessageEnvelope();
        envelope.from = new String[] { "John Doe <jdoe@generic.org>" };
        envelope.sender = new String[] { "John Doe <jdoe@generic.org>" };
        envelope.to = new String[] { "Jim Smith <jsmith@random.net>",
                                     "Jane Doe <jdoe@generic.org>"};
        envelope.cc = new String[] { "Jane Smith <jane.smith@random.net>" };
        envelope.bcc = new String[] { "Jane Smith <smith12@otherplace.com>" };
        
        envelope.date = Calendar.getInstance().getTime();
        envelope.subject = "The subject";
        envelope.messageId = "1234567890";
        
        message = new Message(envelope, body);
    }

    public void tearDown() {
        sampleText = null;
        envelope = null;
        body = null;
        message = null;
    }

    public void testMessage() {
        assertEquals(envelope, message.getEnvelope());
        assertEquals(body, message.getBody());
    }
    
    public void testToReplyMessage() {
        Message replyMessage = message.toReplyMessage();
        MessageEnvelope replyEnvelope = replyMessage.getEnvelope();
        assertNotNull(replyEnvelope);
        MessagePart replyBody = replyMessage.getBody();
        assertNotNull(replyBody);
        
        // Perform a simple test to ensure that the reply converter executed
        assertTrue(replyBody instanceof TextPart);
        String expectedText =
            "On "+StringParser.createDateString(envelope.date)+", John Doe wrote:\r\n" +
            "> Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
            "> eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
            "> ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
            "> nisi ut aliquip ex ea commodo consequat.";
        assertEquals(expectedText, ((TextPart)replyBody).getText());

        // Check the reply header fields
        assertEquals(envelope.messageId, replyEnvelope.inReplyTo);
        assertEquals("Re: " + envelope.subject, replyEnvelope.subject);
        assertEquals(1, replyEnvelope.to.length);
        assertEquals(envelope.from[0], replyEnvelope.to[0]);
        assertTrue(replyEnvelope.cc == null || replyEnvelope.cc.length == 0);
        assertTrue(replyEnvelope.bcc == null || replyEnvelope.bcc.length == 0);
        
        // Set the Reply-To header and try again
        message.getEnvelope().replyTo = new String[] { "John Doe <jdoe@doemail.net>" };
        replyMessage = message.toReplyMessage();
        replyEnvelope = replyMessage.getEnvelope();
        assertNotNull(replyEnvelope);
        replyBody = replyMessage.getBody();
        assertNotNull(replyBody);
        
        // Check the reply header fields
        assertEquals(envelope.messageId, replyEnvelope.inReplyTo);
        assertEquals("Re: " + envelope.subject, replyEnvelope.subject);
        assertEquals(1, replyEnvelope.to.length);
        assertEquals(envelope.replyTo[0], replyEnvelope.to[0]);
        assertTrue(replyEnvelope.cc == null || replyEnvelope.cc.length == 0);
        assertTrue(replyEnvelope.bcc == null || replyEnvelope.bcc.length == 0);
    }
    
    public void testToReplyAllMessage() {
        Message replyMessage = message.toReplyAllMessage("jsmith@random.net");
        MessageEnvelope replyEnvelope = replyMessage.getEnvelope();
        assertNotNull(replyEnvelope);
        MessagePart replyBody = replyMessage.getBody();
        assertNotNull(replyBody);
        
        // Perform a simple test to ensure that the reply converter executed
        assertTrue(replyBody instanceof TextPart);
        String expectedText =
            "On "+StringParser.createDateString(envelope.date)+", John Doe wrote:\r\n" +
            "> Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
            "> eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
            "> ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
            "> nisi ut aliquip ex ea commodo consequat.";
        assertEquals(expectedText, ((TextPart)replyBody).getText());

        // Check the reply header fields
        assertEquals(envelope.messageId, replyEnvelope.inReplyTo);
        assertEquals("Re: " + envelope.subject, replyEnvelope.subject);

        assertEquals(2, replyEnvelope.to.length);
        assertEquals(envelope.from[0], replyEnvelope.to[0]);
        assertEquals(envelope.to[1], replyEnvelope.to[1]);

        assertEquals(1, replyEnvelope.cc.length);
        assertEquals(envelope.cc[0], replyEnvelope.cc[0]);

        assertTrue(replyEnvelope.bcc == null || replyEnvelope.bcc.length == 0);

        // Set the Reply-To header and try again
        message.getEnvelope().replyTo = new String[] { "John Doe <jdoe@doemail.net>" };
        replyMessage = message.toReplyAllMessage("jsmith@random.net");
        replyEnvelope = replyMessage.getEnvelope();
        assertNotNull(replyEnvelope);
        replyBody = replyMessage.getBody();
        assertNotNull(replyBody);
        
        // Check the reply header fields
        assertEquals(envelope.messageId, replyEnvelope.inReplyTo);
        assertEquals("Re: " + envelope.subject, replyEnvelope.subject);
        assertEquals(2, replyEnvelope.to.length);
        assertEquals(envelope.replyTo[0], replyEnvelope.to[0]);
        assertEquals(envelope.to[1], replyEnvelope.to[1]);

        assertTrue(replyEnvelope.bcc == null || replyEnvelope.bcc.length == 0);
        
    }
    
    public void testToForwardMessage() {
        Message forwardMessage = message.toForwardMessage();
        fail("Test not implemented yet");
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("Message");

        suite.addTest(new MessageTest("Message", new TestMethod()
        { public void run(TestCase tc) {((MessageTest)tc).testMessage(); } }));
        
        suite.addTest(new MessageTest("toReplyMessage", new TestMethod()
        { public void run(TestCase tc) {((MessageTest)tc).testToReplyMessage(); } }));
        
        suite.addTest(new MessageTest("toReplyAllMessage", new TestMethod()
        { public void run(TestCase tc) {((MessageTest)tc).testToReplyAllMessage(); } }));
        
        suite.addTest(new MessageTest("toForwardMessage", new TestMethod()
        { public void run(TestCase tc) {((MessageTest)tc).testToForwardMessage(); } }));
        
        return suite;
    }
}
