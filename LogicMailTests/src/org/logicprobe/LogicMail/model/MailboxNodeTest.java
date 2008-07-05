package org.logicprobe.LogicMail.model;

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
    	MessageNode message1 = new MessageNode(new FolderMessage(new MessageEnvelope(), 1));
    	MessageNode message2 = new MessageNode(new FolderMessage(new MessageEnvelope(), 2));
    	MessageNode message3 = new MessageNode(new FolderMessage(new MessageEnvelope(), 3));
    	MessageNode message4 = new MessageNode(new FolderMessage(new MessageEnvelope(), 4));
    	
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
}
