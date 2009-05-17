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

import java.util.Calendar;

import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageContent;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.message.TextPart;
import org.logicprobe.LogicMail.util.StringParser;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class MessageNodeTest extends TestCase {
	private MessageNode instance;

	public MessageNodeTest() {
    }
    
    public MessageNodeTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    }
    
    public void tearDown() {
    	instance = null;
    }

    public void testToReplyMessage() {
    	String sampleText =
    		"Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
    		"eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
    		"ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
    		"nisi ut aliquip ex ea commodo consequat.";
    	TextPart part = new TextPart("plain", "", "", -1);
    	TextContent content = new TextContent(part, sampleText);

    	MessageEnvelope env = new MessageEnvelope();
    	env.subject = "This is the subject";
		env.sender = new String[1];
		env.sender[0] = "\"John Doe\" <jdoe@generic.org>";
		env.date = Calendar.getInstance().getTime();

    	instance = new MessageNode(new FolderMessage(null, env, 0, 0));
		instance.setMessageStructure(part);
		instance.putMessageContent(content);
		
		String expectedText =
			"On " + StringParser.createDateString(env.date) + ", John Doe wrote:\r\n" +
			"> Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
			"> eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
			"> ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
			"> nisi ut aliquip ex ea commodo consequat.";

		MessageNode result = instance.toReplyMessage();

		MessagePart resultPart = result.getMessageStructure();
		assertNotNull("Null result", resultPart);
		assertTrue("Bad type", resultPart instanceof TextPart);

		MessageContent resultContent = result.getMessageContent(resultPart);
		assertNotNull("Null result", resultContent);
		assertTrue("Bad type", resultContent instanceof TextContent);
		assertEquals("Content mismatch", expectedText, ((TextContent)resultContent).getText());
    }
    
    public void testToForwardMessage() {
        String sampleText =
            "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
            "ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
            "nisi ut aliquip ex ea commodo consequat.";
    	TextPart part = new TextPart("plain", "", "", -1);
    	TextContent content = new TextContent(part, sampleText);
	    
	    MessageEnvelope env = new MessageEnvelope();
	    env.from = new String[] { "\"John Doe\" <jdoe@generic.org>" };
	    env.to = new String[] { "\"Jim Smith\" <jsmith@something.net>",
	                            "\"Jane Doe\" <jane.doe@things.org>" };
	    env.date = Calendar.getInstance().getTime();
	    env.subject = "The Subject";

	    instance = new MessageNode(new FolderMessage(null, env, 0, 0));
		instance.setMessageStructure(part);
		instance.putMessageContent(content);
	    
	    String expectedText =
	            "----Original Message----\r\n"+
	            "Subject: The Subject\r\n"+
	            "Date: "+StringParser.createDateString(env.date)+"\r\n"+
	            "From: \"John Doe\" <jdoe@generic.org>\r\n"+
	            "To: \"Jim Smith\" <jsmith@something.net>, \"Jane Doe\" <jane.doe@things.org>\r\n"+
	            "\r\n" +
	            "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do\r\n" +
	            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim\r\n" +
	            "ad minim veniam, quis nostrud exercitation ullamco laboris\r\n" +
	            "nisi ut aliquip ex ea commodo consequat.\r\n"+
	            "------------------------";

	    MessageNode result = instance.toForwardMessage();
	    
		MessagePart resultPart = result.getMessageStructure();
		assertNotNull("Null result", resultPart);
		assertTrue("Bad type", resultPart instanceof TextPart);

		MessageContent resultContent = result.getMessageContent(resultPart);
		assertNotNull("Null result", resultContent);
		assertTrue("Bad type", resultContent instanceof TextContent);
		assertEquals("Content mismatch", expectedText, ((TextContent)resultContent).getText());
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("MessageNodeTest");

        suite.addTest(new MessageNodeTest("toReplyMessage", new TestMethod()
        { public void run(TestCase tc) {((MessageNodeTest)tc).testToReplyMessage(); } }));
        suite.addTest(new MessageNodeTest("toForwardMessage", new TestMethod()
        { public void run(TestCase tc) {((MessageNodeTest)tc).testToForwardMessage(); } }));

        return suite;
    }
}
