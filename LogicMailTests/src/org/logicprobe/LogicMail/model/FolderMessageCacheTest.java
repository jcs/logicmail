/*-
 * Copyright (c) 2010, Derek Konigsberg
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

import org.logicprobe.LogicMail.mail.FakeMessageToken;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class FolderMessageCacheTest extends TestCase {
    private FolderTreeItem folder1;
    private FolderMessage message1a;
    private FolderMessage message1b;
    private FolderTreeItem folder2;
    private FolderMessage message2a;
    private FolderMessage message2b;
    
    public FolderMessageCacheTest() {
    }
    
    public FolderMessageCacheTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }
    
    protected void setUp() throws Exception {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.destroy();
        
        folder1 = new FolderTreeItem("INBOX", "INBOX", ".");
        message1a = new FolderMessage(new FakeMessageToken(1), createEnvelope(), 1, 11, -1);
        message1b = new FolderMessage(new FakeMessageToken(2), createEnvelope(), 2, 12, -1);
        
        folder2 = new FolderTreeItem("Misc", "INBOX.Misc", ".");
        message2a = new FolderMessage(new FakeMessageToken(3), createEnvelope(), 1, 21, -1);
        message2b = new FolderMessage(new FakeMessageToken(4), createEnvelope(), 2, 22, -1);
    }

    private static MessageEnvelope createEnvelope() {
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.from = new String[] { "John Doe <jdoe@generic.org>" };
        envelope.to = new String[] { "Jane Doe <jane.doe@generic.net>" };
        envelope.replyTo = new String[0];
        envelope.sender = new String[0];
        envelope.subject = "The Subject";
        envelope.date = Calendar.getInstance().getTime();
        envelope.inReplyTo = "";
        envelope.messageId = "<" + Long.toString(envelope.getUniqueId()) + "@generic.org>";
        return envelope;
    }
    
    protected void tearDown() throws Exception {
        folder1 = null;
        message1a = null;
        message1b = null;
        folder2 = null;
        message2a = null;
        message2b = null;
        
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.destroy();
    }
    
    public void testGetFolders() {
        FolderMessageCache instance = new TestFolderMessageCache();
        FolderTreeItem[] folders = instance.getFolders();
        assertNotNull("Empty", folders);
        assertEquals("Empty", 0, folders.length);
        
        instance.addFolderMessage(folder1, message1a);
        folders = instance.getFolders();
        assertNotNull("One folder", folders);
        assertEquals("One folder", 1, folders.length);
        assertEquals("One folder", folder1, folders[0]);
        
        instance.addFolderMessage(folder2, message2a);
        folders = instance.getFolders();
        assertNotNull("Two folders", folders);
        assertEquals("Two folders", 2, folders.length);
        assertContains("Two folders", folder1, folders);
        assertContains("Two folders", folder2, folders);
    }
    
    public void testGetFoldersPersistence() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder2, message2a);
        
        instance.commit();
        instance = null;
        
        instance = new TestFolderMessageCache();
        instance.restore();
        
        FolderTreeItem[] folders = instance.getFolders();
        assertNotNull("Two folders restored", folders);
        assertEquals("Two folders restored", 2, folders.length);
        assertContains("Two folders restored", folder1, folders);
        assertContains("Two folders restored", folder2, folders);
    }
    
    public void testAddFolderMessage() {
        FolderMessageCache instance = new TestFolderMessageCache();
        FolderMessage[] messages = instance.getFolderMessages(folder1);
        assertNotNull("Empty", messages);
        assertEquals("Empty", 0, messages.length);

        instance.addFolderMessage(folder1, message1a);
        messages = instance.getFolderMessages(folder1);
        assertNotNull("One message", messages);
        assertEquals("One message", 1, messages.length);
        assertEquals("One message", message1a, messages[0]);
        
        instance.addFolderMessage(folder1, message1b);
        messages = instance.getFolderMessages(folder1);
        assertNotNull("Two messages", messages);
        assertEquals("Two messages", 2, messages.length);
        assertContains("Two messages", message1a, messages);
        assertContains("Two messages", message1b, messages);
        
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        messages = instance.getFolderMessages(folder1);
        assertNotNull("Two messages, two folders", messages);
        assertEquals("Two messages, two folders", 2, messages.length);
        assertContains("Two messages, two folders", message1a, messages);
        assertContains("Two messages, two folders", message1b, messages);
        
        messages = instance.getFolderMessages(folder2);
        assertNotNull("Two messages, two folders", messages);
        assertEquals("Two messages, two folders", 2, messages.length);
        assertContains("Two messages, two folders", message2a, messages);
        assertContains("Two messages, two folders", message2b, messages);
    }
    
    public void testAddFolderMessagePersistence() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder1, message1b);
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        instance.commit();
        instance = null;
        
        instance = new TestFolderMessageCache();
        instance.restore();
        
        FolderMessage[] messages = instance.getFolderMessages(folder1);
        assertNotNull("Two messages, two folders, restored", messages);
        assertEquals("Two messages, two folders, restored", 2, messages.length);
        assertContains("Two messages, two folders, restored", message1a, messages);
        assertContains("Two messages, two folders, restored", message1b, messages);
        
        messages = instance.getFolderMessages(folder2);
        assertNotNull("Two messages, two folders, restored", messages);
        assertEquals("Two messages, two folders, restored", 2, messages.length);
        assertContains("Two messages, two folders, restored", message2a, messages);
        assertContains("Two messages, two folders, restored", message2b, messages);
    }
    
    public void testRemoveFolderMessage() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder1, message1b);
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        instance.removeFolderMessage(folder1, message1a);
        FolderMessage[] messages = instance.getFolderMessages(folder1);
        assertLacks("Remove one", message1a, messages);
        assertContains("Remove one", message1b, messages);
        messages = instance.getFolderMessages(folder2);
        assertContains("Remove one", message2a, messages);
        assertContains("Remove one", message2b, messages);

        instance.removeFolderMessage(folder2, message2b);
        messages = instance.getFolderMessages(folder1);
        assertLacks("Remove two", message1a, messages);
        assertContains("Remove two", message1b, messages);
        messages = instance.getFolderMessages(folder2);
        assertContains("Remove two", message2a, messages);
        assertLacks("Remove two", message2b, messages);
        
        instance.removeFolderMessage(folder1, message1b);
        FolderTreeItem folders[] = instance.getFolders();
        assertLacks("Remove whole folder", folder1, folders);
        assertContains("Remove whole folder", folder2, folders);
    }
    
    public void testRemoveFolderMessagePersistence() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder1, message1b);
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        instance.removeFolderMessage(folder1, message1a);
        
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        FolderMessage[] messages = instance.getFolderMessages(folder1);
        assertLacks("Remove one, restored", message1a, messages);
        assertContains("Remove one, restored", message1b, messages);
        messages = instance.getFolderMessages(folder2);
        assertContains("Remove one, restored", message2a, messages);
        assertContains("Remove one, restored", message2b, messages);
        
        instance.removeFolderMessage(folder1, message1b);
        
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        FolderTreeItem folders[] = instance.getFolders();
        assertLacks("Remove whole folder, restored", folder1, folders);
        assertContains("Remove whole folder, restored", folder2, folders);
    }
    
    public void testUpdateFolderMessage() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder1, message1b);
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        FolderMessage updatedMessage1b = new FolderMessage(new FakeMessageToken(2), new MessageEnvelope(), 10, 12, -1);
        updatedMessage1b.setFlagged(true);
        instance.updateFolderMessage(folder1, updatedMessage1b);
        
        FolderMessage[] messages = instance.getFolderMessages(folder1);
        assertContains("Update one", message1a, messages);
        assertContains("Update one", message1b, messages);
        for(int i=0; i<messages.length; i++) {
            if(messages[i] != null && messages[i].equals(message1b)) {
                assertEquals("Update one", 10, messages[i].getIndex());
                assertTrue("Update one", messages[i].isFlagged());
            }
        }
    }
    
    public void testUpdateFolderMessagePersistence() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder1, message1b);
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        FolderMessage updatedMessage1b = new FolderMessage(new FakeMessageToken(2), new MessageEnvelope(), 10, 12, -1);
        updatedMessage1b.setFlagged(true);
        instance.updateFolderMessage(folder1, updatedMessage1b);
        
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        FolderMessage[] messages = instance.getFolderMessages(folder1);
        assertContains("Update one, restored", message1a, messages);
        assertContains("Update one, restored", message1b, messages);
        for(int i=0; i<messages.length; i++) {
            if(messages[i] != null && messages[i].equals(message1b)) {
                assertEquals("Update one, restored", 10, messages[i].getIndex());
                assertTrue("Update one, restored", messages[i].isFlagged());
            }
        }
    }
    
    public void testRemoveFolder() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder1, message1b);
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        FolderTreeItem[] folders = instance.getFolders();
        assertContains("Initial state", folder1, folders);
        assertContains("Initial state", folder2, folders);
        
        instance.removeFolder(folder1);
        folders = instance.getFolders();
        assertLacks("Remove one", folder1, folders);
        assertContains("Remove one", folder2, folders);
        
        instance.removeFolder(folder2);
        folders = instance.getFolders();
        assertLacks("Remove two", folder1, folders);
        assertLacks("Remove two", folder2, folders);
    }
    
    public void testRemoveFolderPersistence() {
        FolderMessageCache instance = new TestFolderMessageCache();
        instance.addFolderMessage(folder1, message1a);
        instance.addFolderMessage(folder1, message1b);
        instance.addFolderMessage(folder2, message2a);
        instance.addFolderMessage(folder2, message2b);
        
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        instance.removeFolder(folder1);
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        FolderTreeItem[] folders = instance.getFolders();
        assertLacks("Remove one, restored", folder1, folders);
        assertContains("Remove one, restored", folder2, folders);
        
        instance.removeFolder(folder2);
        instance.commit();
        instance = new TestFolderMessageCache();
        instance.restore();
        
        folders = instance.getFolders();
        assertLacks("Remove two, restored", folder1, folders);
        assertLacks("Remove two, restored", folder2, folders);
    }
    
    public void assertContains(String message, Object expected, Object[] array) {
        for(int i=0; i<array.length; i++) {
            if(array[i] != null && array[i].equals(expected)) {
                return;
            }
        }
        fail(message);
    }
    
    public void assertLacks(String message, Object expected, Object[] array) {
        for(int i=0; i<array.length; i++) {
            if(array[i] != null && array[i].equals(expected)) {
                fail(message);
            }
        }
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("FolderMessageCache");

        suite.addTest(new FolderMessageCacheTest("getFolders", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testGetFolders(); } }));
        suite.addTest(new FolderMessageCacheTest("getFoldersPersistence", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testGetFoldersPersistence(); } }));
        suite.addTest(new FolderMessageCacheTest("addFolderMessage", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testAddFolderMessage(); } }));
        suite.addTest(new FolderMessageCacheTest("addFolderMessagePersistence", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testAddFolderMessagePersistence(); } }));
        suite.addTest(new FolderMessageCacheTest("removeFolderMessage", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testRemoveFolderMessage(); } }));
        suite.addTest(new FolderMessageCacheTest("removeFolderMessagePersistence", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testRemoveFolderMessagePersistence(); } }));
        suite.addTest(new FolderMessageCacheTest("updateFolderMessage", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testUpdateFolderMessage(); } }));
        suite.addTest(new FolderMessageCacheTest("updateFolderMessagePersistence", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testUpdateFolderMessagePersistence(); } }));
        suite.addTest(new FolderMessageCacheTest("removeFolder", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testRemoveFolder(); } }));
        suite.addTest(new FolderMessageCacheTest("removeFolderPersistence", new TestMethod()
        { public void run(TestCase tc) {((FolderMessageCacheTest)tc).testRemoveFolderPersistence(); } }));

        return suite;
    }
    
    private class TestFolderMessageCache extends FolderMessageCache {
        public TestFolderMessageCache() {
            //"org.logicprobe.LogicMail.model.TestFolderMessageCacheObject"
            super(0x50eb4d81a1fe479bL);
        }
    }
}
