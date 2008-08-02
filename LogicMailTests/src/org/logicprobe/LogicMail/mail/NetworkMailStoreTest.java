/*-
 * Copyright (c) 2008, Derek Konigsberg
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

package org.logicprobe.LogicMail.mail;

import java.io.IOException;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.TextPart;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 * Unit test for NetworkMailStore.
 * This is technically a partial component test, since it also exercises
 * IncomingMailConnectionHandler to some extent.  However, it does use
 * a fake implementation of IncomingMailClient.
 */
public class NetworkMailStoreTest extends TestCase {
	private NetworkMailStore instance;
	
	private AccountConfig fakeAccountConfig;
	private FakeIncomingMailClient fakeIncomingMailClient;

	private FolderEvent eventFolderTreeUpdated;
	private FolderMessagesEvent eventFolderMessagesAvailable;
	private FolderEvent eventFolderStatusChanged;
	private MessageEvent eventMessageAvailable;
	private MessageEvent eventMessageDeleted;
	private MessageEvent eventMessageUndeleted;
	
    public NetworkMailStoreTest() {
    }
	
    public NetworkMailStoreTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    public void setUp() {
    	fakeAccountConfig = new AccountConfig() { };
    	fakeIncomingMailClient = new FakeIncomingMailClient();
    	MailClientFactory.setIncomingMailClient(fakeAccountConfig, fakeIncomingMailClient);
    	instance = new NetworkMailStore(fakeAccountConfig);

    	instance.addMailStoreListener(new MailStoreListener() {
			public void folderTreeUpdated(FolderEvent e) {
				eventFolderTreeUpdated = e;
			}
    	});
    	
    	instance.addFolderListener(new FolderListener() {
			public void folderMessagesAvailable(FolderMessagesEvent e) {
				eventFolderMessagesAvailable = e;
			}
			public void folderStatusChanged(FolderEvent e) {
				eventFolderStatusChanged = e;
			}
    	});
    	
    	instance.addMessageListener(new MessageListener() {
			public void messageAvailable(MessageEvent e) {
				eventMessageAvailable = e;
			}
			public void messageDeleted(MessageEvent e) {
				eventMessageDeleted = e;
			}
			public void messageUndeleted(MessageEvent e) {
				eventMessageUndeleted = e;
			}
			public void messageFlagsChanged(MessageEvent e) {
			}
    	});
    }
    
    public void tearDown() {
    	instance.shutdown(true);
    	instance = null;
    	fakeAccountConfig = null;
    	fakeIncomingMailClient = null;
    }

    public void testProperties() {
    	assertTrue(!instance.isLocal());
    }

    /**
     * Tests the mail store shutdown process
     */
    public void testShutdown() {
    	// Make a fake request, then call for shutdown
    	fakeIncomingMailClient.folderTree = new FolderTreeItem("INBOX", "INBOX", ".");
    	instance.requestFolderTree();
    	instance.shutdown(true);
    	
    	// Assert that both open and close were really called, and that
    	// the request was processed.
    	assertTrue("open", fakeIncomingMailClient.openCalled);
    	assertNotNull("request", eventFolderTreeUpdated);
    	assertTrue("close", fakeIncomingMailClient.closeCalled);
	}
    
    /**
     * Tests the mail store shutdown/restart process
     */
    public void testRestart() {
    	// Make a fake request, then call for shutdown
    	fakeIncomingMailClient.folderTree = new FolderTreeItem("INBOX", "INBOX", ".");
    	instance.requestFolderTree();
    	instance.shutdown(true);
    	
    	// Assert that both open and close were really called, and that
    	// the request was processed.
    	assertTrue("open", fakeIncomingMailClient.openCalled);
    	assertNotNull("request", eventFolderTreeUpdated);
    	assertTrue("close", fakeIncomingMailClient.closeCalled);
    	
    	// Reset the sense flags
    	fakeIncomingMailClient.openCalled = false;
    	eventFolderTreeUpdated = null;
    	fakeIncomingMailClient.closeCalled = false;
    	
    	// Restart the thread and try again
    	instance.restart();
    	instance.requestFolderTree();
    	instance.shutdown(true);
    	
    	assertTrue("restart open", fakeIncomingMailClient.openCalled);
    	assertNotNull("restart request", eventFolderTreeUpdated);
    	assertTrue("restart close", fakeIncomingMailClient.closeCalled);
	}
    
    public void testRequestFolderTree() {
    	fakeIncomingMailClient.folderTree = new FolderTreeItem("INBOX", "INBOX", ".");
    	instance.requestFolderTree();
    	instance.shutdown(true);
    	
    	assertNotNull(eventFolderTreeUpdated);
    	assertEquals(fakeIncomingMailClient.folderTree, eventFolderTreeUpdated.getFolder());
    }
    
    public void testRequestFolderStatus() {
    	fakeIncomingMailClient.refreshedMsgCount = 42;
    	FolderTreeItem folder = new FolderTreeItem("INBOX", "INBOX", ".");
    	folder.setMsgCount(0);
    	instance.requestFolderStatus(new FolderTreeItem[] { folder });
    	instance.shutdown(true);
    	
    	assertNotNull(eventFolderStatusChanged);
    	assertEquals("INBOX", eventFolderStatusChanged.getFolder().getName());
    	assertEquals(42, eventFolderStatusChanged.getFolder().getMsgCount());
    }
    
    public void testRequestFolderMessages() {
    	fakeIncomingMailClient.folderMessages = new FolderMessage[] {
    		new FolderMessage(new MessageEnvelope(), 42),
    		new FolderMessage(new MessageEnvelope(), 43),
    	};
    	FolderTreeItem folder = new FolderTreeItem("INBOX", "INBOX", ".");
    	instance.requestFolderMessagesRange(folder, 42, 43);
    	instance.shutdown(true);
    	
    	assertNotNull(eventFolderMessagesAvailable);
    	assertEquals("INBOX", eventFolderMessagesAvailable.getFolder().getName());
    	assertNotNull(eventFolderMessagesAvailable.getMessages());
    	assertEquals(2, eventFolderMessagesAvailable.getMessages().length);
    	assertEquals(42, eventFolderMessagesAvailable.getMessages()[0].getIndex());
    	assertEquals(43, eventFolderMessagesAvailable.getMessages()[1].getIndex());
    }
    
    public void testRequestMessage() {
    	fakeIncomingMailClient.message = new Message(new MessageEnvelope(), new TextPart("plain", "Hello World"));
    	FolderTreeItem folder = new FolderTreeItem("INBOX", "INBOX", ".");
    	FolderMessage folderMessage = new FolderMessage(fakeIncomingMailClient.message.getEnvelope(), 42);
    	instance.requestMessage(folder, folderMessage);
    	instance.shutdown(true);
    	
    	assertNotNull(eventMessageAvailable);
    	assertNotNull(eventMessageAvailable.getMessage());
    	assertEquals(fakeIncomingMailClient.message, eventMessageAvailable.getMessage());
    	assertNotNull(eventMessageAvailable.getFolderMessage());
    }
    
    public void testRequestMessageDelete() {
    	fakeIncomingMailClient.message = new Message(new MessageEnvelope(), new TextPart("plain", "Hello World"));
    	FolderTreeItem folder = new FolderTreeItem("INBOX", "INBOX", ".");
    	FolderMessage folderMessage = new FolderMessage(fakeIncomingMailClient.message.getEnvelope(), 42);
    	instance.requestMessageDelete(folder, folderMessage);
    	instance.shutdown(true);
    	
    	assertNotNull(eventMessageDeleted);
    	assertNull(eventMessageDeleted.getMessage());
    	assertNotNull(eventMessageDeleted.getFolderMessage());
    	assertTrue(eventMessageDeleted.getFolderMessage().isDeleted());
    }
    
    public void testRequestMessageUndelete() {
    	fakeIncomingMailClient.message = new Message(new MessageEnvelope(), new TextPart("plain", "Hello World"));
    	FolderTreeItem folder = new FolderTreeItem("INBOX", "INBOX", ".");
    	FolderMessage folderMessage = new FolderMessage(fakeIncomingMailClient.message.getEnvelope(), 42);
    	instance.requestMessageUndelete(folder, folderMessage);
    	instance.shutdown(true);
    	
    	assertNotNull(eventMessageUndeleted);
    	assertNull(eventMessageUndeleted.getMessage());
    	assertNotNull(eventMessageUndeleted.getFolderMessage());
    	assertTrue(!eventMessageUndeleted.getFolderMessage().isDeleted());
    }
    
    public void testRequestBatch() {
    	fakeIncomingMailClient.message = new Message(new MessageEnvelope(), new TextPart("plain", "Hello World"));
    	FolderTreeItem folder = new FolderTreeItem("INBOX", "INBOX", ".");
    	fakeIncomingMailClient.folderTree = folder;
    	fakeIncomingMailClient.folderMessages = new FolderMessage[] {
        		new FolderMessage(fakeIncomingMailClient.message.getEnvelope(), 42),
        		new FolderMessage(new MessageEnvelope(), 43),
        };

    	// Do a whole batch of non-conflicting requests to make
    	// sure the queue is working correctly.
    	instance.requestFolderTree();
    	instance.requestFolderStatus(new FolderTreeItem[] { folder });
    	instance.requestFolderMessagesRange(folder, 42, 43);
    	instance.requestMessage(folder, fakeIncomingMailClient.folderMessages[0]);
    	instance.shutdown(true);
    	
    	// We know the requests work individually, so lets just
    	// make sure they all went through the system.
    	assertNotNull("requestFolderTree", eventFolderTreeUpdated);
    	assertNotNull("requestFolderStatus", eventFolderStatusChanged);
    	assertNotNull("requestFolderMessages", eventFolderMessagesAvailable);
    	assertNotNull("requestMessage", eventMessageAvailable);
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("NetworkMailStore");

        suite.addTest(new NetworkMailStoreTest("properties", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testProperties(); } }));
        suite.addTest(new NetworkMailStoreTest("shutdown", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testShutdown(); } }));
        suite.addTest(new NetworkMailStoreTest("restart", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRestart(); } }));
        suite.addTest(new NetworkMailStoreTest("requestFolderTree", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRequestFolderTree(); } }));
        suite.addTest(new NetworkMailStoreTest("requestFolderStatus", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRequestFolderStatus(); } }));
        suite.addTest(new NetworkMailStoreTest("requestFolderMessages", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRequestFolderMessages(); } }));
        suite.addTest(new NetworkMailStoreTest("requestMessage", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRequestMessage(); } }));
        suite.addTest(new NetworkMailStoreTest("requestMessageDelete", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRequestMessageDelete(); } }));
        suite.addTest(new NetworkMailStoreTest("requestMessageUndelete", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRequestMessageUndelete(); } }));
        suite.addTest(new NetworkMailStoreTest("request batch", new TestMethod()
        { public void run(TestCase tc) {((NetworkMailStoreTest)tc).testRequestBatch(); } }));

        return suite;
    }

	private class FakeIncomingMailClient implements IncomingMailClient {
		public boolean openCalled = false;
		public boolean closeCalled = false;
		public FolderTreeItem activeFolder;
		public int firstIndex;
		public int lastIndex;
		public FolderMessage[] folderMessages;
		public FolderTreeItem folderTree;
		public Message message;
		public FolderMessage deletedMessage;
		public FolderMessage undeletedMessage;
		public int refreshedMsgCount;
		
		public boolean isConnected() { return true; }
		public boolean open() throws IOException, MailException { openCalled = true; return true; }
		public void close() throws IOException, MailException { closeCalled = true; }
		public String getPassword() { return fakeAccountConfig.getServerPass(); }
		public String getUsername() { return fakeAccountConfig.getServerUser(); }
		public void setPassword(String password) { fakeAccountConfig.setServerPass(password); }
		public void setUsername(String username) { fakeAccountConfig.setServerUser(username); }
		public boolean hasFolders() { return true; }
		public boolean hasUndelete() { return true; }
		public AccountConfig getAcctConfig() { return fakeAccountConfig; }
		public FolderTreeItem getActiveFolder() { return activeFolder; }
		public FolderMessage[] getFolderMessages(int firstIndex, int lastIndex)
				throws IOException, MailException { this.firstIndex = firstIndex; this.lastIndex = lastIndex; return this.folderMessages; }
		public FolderTreeItem getFolderTree() throws IOException, MailException { return this.folderTree; }
		public Message getMessage(FolderMessage folderMessage)
				throws IOException, MailException { return this.message; }
		public void refreshFolderStatus(FolderTreeItem[] folders)
				throws IOException, MailException { folders[0].setMsgCount(refreshedMsgCount); }
		public void setActiveFolder(FolderTreeItem folderItem)
				throws IOException, MailException { this.activeFolder = folderItem; }
		public void deleteMessage(FolderMessage folderMessage)
		throws IOException, MailException { folderMessage.setDeleted(true); this.deletedMessage = folderMessage; }
		public void undeleteMessage(FolderMessage folderMessage)
				throws IOException, MailException { folderMessage.setDeleted(false); this.undeletedMessage = folderMessage; }
		public ConnectionConfig getConnectionConfig() { return null; }
	};
}
