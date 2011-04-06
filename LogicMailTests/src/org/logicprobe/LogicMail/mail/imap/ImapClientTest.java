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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.SocketConnection;

import net.rim.device.api.util.IntVector;

import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.mail.FolderMessageCallback;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MockIncomingMailClientListener;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.util.Connection;
import org.logicprobe.LogicMail.util.NetworkConnector;

import com.hammingweight.hammock.Hammock;
import com.hammingweight.hammock.Hamspy;
import com.hammingweight.hammock.IArgumentMatcher;
import com.hammingweight.hammock.NotNullArgumentMatcher;
import com.hammingweight.hammock.mocks.microedition.io.MockSocketConnection;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class ImapClientTest extends TestCase {
    private TestImapClient instance;
    private GlobalConfig globalConfig;
    private ImapConfig accountConfig;
    
    private Hammock hammock;
    private MockImapProtocol mockImapProtocol;
    private MockIncomingMailClientListener mockClientListener;
    private ImapProtocol.UntaggedResponseListener untaggedResponseListener;
    
    public ImapClientTest() {
    }

    public ImapClientTest(String testName, TestMethod testMethod) {
        super(testName, testMethod);
    }

    public void setUp() {
        hammock = new Hammock();
        mockImapProtocol = new MockImapProtocol(hammock);
        mockClientListener = new MockIncomingMailClientListener(hammock);
        
        globalConfig = new GlobalConfig();
        accountConfig = new ImapConfig();
        accountConfig.setServerName("imap.test.net");
        accountConfig.setServerPort(143);
        accountConfig.setServerSecurity(0);
        accountConfig.setServerUser("user");
        accountConfig.setServerPass("password");

        // Capture the listener that ImapClient passes to ImapProtocol when it
        // is constructed
        hammock.setExpectation(MockImapProtocol.MTHD_SET_UNTAGGED_RESPONSE_LISTENER_$_IMAPPROTOCOL$UNTAGGEDRESPONSELISTENER,
                new Object[] { null }).setArgumentMatcher(0, new IArgumentMatcher() {
                    public boolean areArgumentsEqual(Object argumentExpected, Object argumentActual) {
                        if(argumentActual == null) { return false; }
                        untaggedResponseListener = (ImapProtocol.UntaggedResponseListener)argumentActual;
                        return true;
                    }
                });
        
        instance = new TestImapClient(globalConfig, accountConfig, mockImapProtocol);
        instance.setListener(mockClientListener);
    }

    public void tearDown() {
        instance = null;
        accountConfig = null;
        globalConfig = null;
    }
    
    /**
     * Configures the test <code>ImapClient</code> instance so that
     * <code>open()</code> can be called. This is intended to be the setup code
     * for every test case except those that need to test alternate paths of
     * the connection opening process.
     */
    private void configureForBasicOpen() throws Throwable {
        hammock.setStubExpectation(MockImapProtocol.MTHD_SET_CONNECTION_$_CONNECTION);
        
        Hashtable capabilities = new Hashtable();
        capabilities.put("NAMESPACE", Boolean.TRUE);
        capabilities.put("CHILDREN", Boolean.TRUE);
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_CAPABILITY).setReturnValue(capabilities);
        
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_LOGIN_$_STRING_STRING,
                new Object[] { accountConfig.getServerUser(), accountConfig.getServerPass() })
                .setReturnValue(Boolean.TRUE);
        
        ImapProtocol.NamespaceResponse namespaces = new ImapProtocol.NamespaceResponse();
        namespaces.personal = new ImapProtocol.Namespace[] { new ImapProtocol.Namespace() };
        namespaces.personal[0].prefix = "";
        namespaces.personal[0].delimiter = ".";
        namespaces.other = new ImapProtocol.Namespace[0];
        namespaces.shared = new ImapProtocol.Namespace[0];
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_NAMESPACE).setReturnValue(namespaces);
    }
    
    public void testOpen() throws Throwable {
        configureForBasicOpen();
        
        assertTrue(instance.open());
        assertTrue(instance.isConnected());
        
        hammock.verify();
    }
    
    public void testGetFolderTree() throws Throwable {
        configureForBasicOpen();
        assertTrue(instance.open());

        accountConfig.setOnlySubscribedFolders(false);
        
        Vector listResponses1 = new Vector();
        ImapProtocol.ListResponse listResponse1 = new ImapProtocol.ListResponse();
        listResponse1.name = "INBOX";
        listResponse1.delim = ".";
        listResponse1.canSelect = true;
        listResponse1.hasChildren = true;
        listResponses1.addElement(listResponse1);
        
        Vector listResponses2 = new Vector();
        ImapProtocol.ListResponse listResponse2a = new ImapProtocol.ListResponse();
        listResponse2a.name = "INBOX.Folder1";
        listResponse2a.delim = ".";
        listResponse2a.canSelect = true;
        listResponse2a.hasChildren = false;
        listResponses2.addElement(listResponse2a);
        ImapProtocol.ListResponse listResponse2b = new ImapProtocol.ListResponse();
        listResponse2b.name = "INBOX.Folder2";
        listResponse2b.delim = ".";
        listResponse2b.canSelect = true;
        listResponse2b.hasChildren = false;
        listResponses2.addElement(listResponse2b);
        
        hammock.setExpectation(
                MockImapProtocol.MTHD_EXECUTE_LIST_$_STRING_STRING_MAILPROGRESSHANDLER,
                new Object[] { "", "%", null })
                .setReturnValue(listResponses1);
        hammock.setExpectation(
                MockImapProtocol.MTHD_EXECUTE_LIST_$_STRING_STRING_MAILPROGRESSHANDLER,
                new Object[] { "INBOX.", "%", null })
                .setReturnValue(listResponses2);
        
        FolderTreeItem rootFolder = instance.getFolderTree(null);
        assertNotNull(rootFolder);
        FolderTreeItem[] children = rootFolder.children();
        assertNotNull(children);
        assertEquals(1, children.length);
        assertNotNull(children[0]);
        FolderTreeItem inboxFolder = children[0];
        
        assertEquals("INBOX", inboxFolder.getName());
        assertEquals("INBOX", inboxFolder.getPath());
        assertEquals(".", inboxFolder.getDelim());
        assertTrue(inboxFolder.hasChildren());
        assertTrue(inboxFolder.isSelectable());
        assertTrue(inboxFolder.isAppendable());
        assertEquals(rootFolder, inboxFolder.getParent());
        
        children = inboxFolder.children();
        assertNotNull(children);
        assertEquals(2, children.length);
        
        assertEquals("Folder1", children[0].getName());
        assertEquals("INBOX.Folder1", children[0].getPath());
        assertEquals(".", children[0].getDelim());
        assertTrue(!children[0].hasChildren());
        assertTrue(children[0].isSelectable());
        assertTrue(children[0].isAppendable());
        assertEquals(inboxFolder, children[0].getParent());
        
        assertEquals("Folder2", children[1].getName());
        assertEquals("INBOX.Folder2", children[1].getPath());
        assertEquals(".", children[1].getDelim());
        assertTrue(!children[1].hasChildren());
        assertTrue(children[1].isSelectable());
        assertTrue(children[1].isAppendable());
        assertEquals(inboxFolder, children[1].getParent());
        
        hammock.verify();
    }
    
    private FolderTreeItem configureForSetActiveFolder(int exists) {
        return configureForSetActiveFolder("INBOX", "INBOX", exists, 100 + exists + 1);
    }
    
    private FolderTreeItem configureForSetActiveFolder(String name, String path, int exists, int uidNext) {
        FolderTreeItem folderItem = new FolderTreeItem(null, name, path, ".", true, true);
        ImapProtocol.SelectResponse selectResponse = new ImapProtocol.SelectResponse();
        selectResponse.exists = exists;
        selectResponse.recent = 0;
        selectResponse.unseen = 0;
        selectResponse.uidNext = uidNext;
        selectResponse.uidValidity = 1234;
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_SELECT_$_STRING).setReturnValue(selectResponse);
        return folderItem;
    }
    
    public void testSetActiveFolder() throws Throwable {
        configureForBasicOpen();
        assertTrue(instance.open());
        
        FolderTreeItem inboxFolder = configureForSetActiveFolder(5);
        instance.setActiveFolder(inboxFolder, true);

        hammock.verify();
    }

    public void testGetNewFolderMessages() throws Throwable {
        configureForBasicOpen();
        assertTrue(instance.open());
        
        FolderTreeItem inboxFolder = configureForSetActiveFolder(5);
        instance.setActiveFolder(inboxFolder, true);
        
        // Assume that we get all 5 messages, since the account configuration
        // specifies a fetch limit larger than 5.
        ImapProtocol.FetchEnvelopeResponse[] responses = configureForGetFolderMessages(1, inboxFolder.getMsgCount());
        TestFolderMessageCallback callback = new TestFolderMessageCallback();
        instance.getNewFolderMessages(false, callback, null);

        hammock.verify();
        
        int size = callback.folderMessages.size();
        assertEquals(5, size);
        for(int i=0; i<size; i++) {
            FolderMessage folderMessage = (FolderMessage)callback.folderMessages.elementAt(i);
            assertEquals(responses[i].index, folderMessage.getIndex());
            assertEquals(responses[i].uid, folderMessage.getUid());
        }
    }

    public void testIdleModeBegin() throws Throwable {
        configureForBasicOpen();
        assertTrue(instance.open());
        
        FolderTreeItem inboxFolder = configureForSetActiveFolder(5);
        instance.setActiveFolder(inboxFolder, true);
        
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_$_IMAPPROTOCOL$IDLELISTENER,
                new Object[] { null }).setArgumentMatcher(0, new NotNullArgumentMatcher());
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_DONE);
        
        instance.idleModeBegin();
        instance.idleModeEnd();
        hammock.verify();
    }
    
    public void testIdleModeMessagesAvailable() throws Throwable {
        configureForBasicOpen();
        assertTrue(instance.open());
        
        FolderTreeItem inboxFolder = configureForSetActiveFolder(5);
        instance.setActiveFolder(inboxFolder, true);
        
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_$_IMAPPROTOCOL$IDLELISTENER,
                new Object[] { null }).setArgumentMatcher(0, new NotNullArgumentMatcher());
        hammock.setExpectation(MockIncomingMailClientListener.MTHD_RECENT_FOLDER_MESSAGES_AVAILABLE);
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_DONE);
        
        instance.idleModeBegin();
        untaggedResponseListener.existsResponse(6);
        instance.idleModeEnd();
        hammock.verify();
    }
    
    public void testIdleModeFlagsChanged() throws Throwable {
        configureForBasicOpen();
        assertTrue(instance.open());
        
        FolderTreeItem inboxFolder = configureForSetActiveFolder(5);
        instance.setActiveFolder(inboxFolder, true);

        // Fetch 5 dummy messages
        final ImapProtocol.FetchEnvelopeResponse[] responses = configureForGetFolderMessages(1, inboxFolder.getMsgCount());
        instance.getNewFolderMessages(false, new TestFolderMessageCallback(), null);
       
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_$_IMAPPROTOCOL$IDLELISTENER,
                new Object[] { null }).setArgumentMatcher(0, new NotNullArgumentMatcher());
        hammock.setExpectation(MockIncomingMailClientListener.MTHD_FOLDER_MESSAGE_FLAGS_CHANGED_$_MESSAGETOKEN_MESSAGEFLAGS,
                new Object[] { new ImapMessageToken(inboxFolder.getPath(), responses[1].uid), null }).setArgumentMatcher(1, new IArgumentMatcher() {
                    public boolean areArgumentsEqual(Object argumentExpected, Object argumentActual) {
                        MessageFlags actualFlags = (MessageFlags)argumentActual;
                        return actualFlags.isForwarded();
                    }
                });
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_DONE);
        
        instance.idleModeBegin();
        // Simulate a flags change on the second message in the folder
        ImapProtocol.FetchFlagsResponse flagsResponse = new ImapProtocol.FetchFlagsResponse();
        flagsResponse.index = responses[1].index;
        flagsResponse.uid = responses[1].uid;
        flagsResponse.flags = new ImapProtocol.MessageFlags();
        flagsResponse.flags.forwarded = true;
        untaggedResponseListener.fetchResponse(flagsResponse);
        instance.idleModeEnd();
        hammock.verify();
    }
    
    public void testIdleModeExpungeFlagsChanged() throws Throwable {
        configureForBasicOpen();
        assertTrue(instance.open());
        
        FolderTreeItem inboxFolder = configureForSetActiveFolder(5);
        instance.setActiveFolder(inboxFolder, true);

        // Fetch 5 dummy messages
        final ImapProtocol.FetchEnvelopeResponse[] responses = configureForGetFolderMessages(1, inboxFolder.getMsgCount());
        
        instance.getNewFolderMessages(false, new TestFolderMessageCallback(), null);
       
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_$_IMAPPROTOCOL$IDLELISTENER,
                new Object[] { null }).setArgumentMatcher(0, new NotNullArgumentMatcher());
        hammock.setExpectation(MockIncomingMailClientListener.MTHD_FOLDER_MESSAGE_EXPUNGED_$_MESSAGETOKEN_ARRAY_MESSAGETOKEN,
                new Object[] { new ImapMessageToken(inboxFolder.getPath(), responses[0].uid), null }).ignoreArgument(1);
        hammock.setExpectation(MockIncomingMailClientListener.MTHD_FOLDER_MESSAGE_FLAGS_CHANGED_$_MESSAGETOKEN_MESSAGEFLAGS,
                new Object[] { new ImapMessageToken(inboxFolder.getPath(), responses[1].uid), null })
                .setArgumentMatcher(0, new IArgumentMatcher() {
                    public boolean areArgumentsEqual(Object argumentExpected, Object argumentActual) {
                        ImapMessageToken expectedToken = (ImapMessageToken)argumentExpected;
                        ImapMessageToken actualToken = (ImapMessageToken)argumentActual;
                        // Make sure the tokens are equal based on the normal path/UID
                        // test, and that the actual token has an adjusted index value
                        return (expectedToken.equals(actualToken)
                                && (actualToken.getMessageIndex() == responses[1].index - 1));
                    }
                })
                .setArgumentMatcher(1, new IArgumentMatcher() {
                    public boolean areArgumentsEqual(Object argumentExpected, Object argumentActual) {
                        MessageFlags actualFlags = (MessageFlags)argumentActual;
                        return actualFlags.isForwarded();
                    }
                });
        hammock.setExpectation(MockImapProtocol.MTHD_EXECUTE_IDLE_DONE);
        
        instance.idleModeBegin();
        
        // Simulate an expunge on the first message in the folder
        untaggedResponseListener.expungeResponse(responses[0].index);
        
        // Simulate a flags change on the second message in the folder,
        // with a decremented index and missing UID
        ImapProtocol.FetchFlagsResponse flagsResponse = new ImapProtocol.FetchFlagsResponse();
        flagsResponse.index = responses[1].index - 1;
        flagsResponse.uid = -1;
        flagsResponse.flags = new ImapProtocol.MessageFlags();
        flagsResponse.flags.forwarded = true;
        untaggedResponseListener.fetchResponse(flagsResponse);
        
        instance.idleModeEnd();
        hammock.verify();
    }
    
    private ImapProtocol.FetchEnvelopeResponse[] configureForGetFolderMessages(
            int startingIndex, int endingIndex) {
        
        final ImapProtocol.FetchEnvelopeResponse[] responses = new ImapProtocol.FetchEnvelopeResponse[endingIndex - startingIndex + 1];
        for(int i=0; i<responses.length; i++) {
            responses[i] = createDummyFetchEnvelopeResponse(
                    startingIndex + i,
                    100 + startingIndex + i);
        }
        
        hammock.setExpectation(
                MockImapProtocol.MTHD_EXECUTE_FETCH_ENVELOPE_$_INT_INT_IMAPPROTOCOL$FETCHENVELOPECALLBACK_MAILPROGRESSHANDLER,
                new Object[] { new Integer(startingIndex), new Integer(endingIndex), null, null})
                .setArgumentMatcher(2, new IArgumentMatcher() {
                    public boolean areArgumentsEqual(Object argumentExpected, Object argumentActual) {
                        // This method matches on the callback argument, which
                        // we cannot possibly know in advance. Instead, we use
                        // this method as a hook to capture the actual value
                        // passed by ImapClient, and use it to inject the
                        // fake FETCH responses.
                        ImapProtocol.FetchEnvelopeCallback fetchEnvelopeCallback = (ImapProtocol.FetchEnvelopeCallback)argumentActual;
                        for(int i=0; i<responses.length; i++) {
                            fetchEnvelopeCallback.responseAvailable(responses[i]);
                        }
                        return true;
                    }
                });
        return responses;
    }
    
    private static ImapProtocol.FetchEnvelopeResponse createDummyFetchEnvelopeResponse(int index, int uid) {
        ImapProtocol.FetchEnvelopeResponse response = new ImapProtocol.FetchEnvelopeResponse();
        response.index = index;
        response.uid = uid;
        response.flags = new ImapProtocol.MessageFlags();
        response.envelope = new MessageEnvelope();
        response.structure = new ImapParser.MessageSection();
        response.structure.address = "1";
        response.structure.type = "text";
        response.structure.name = "";
        response.structure.subtype = "plain";
        response.structure.encoding = "7bit";
        response.structure.charset = "US-ASCII";
        response.structure.disposition = "";
        response.structure.contentId = "";
        response.structure.size = 1;
        response.structure.subsections = null;
        return response;
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite("ImapClient");

        suite.addTest(new ImapClientTest("open", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testOpen(); }}));

        suite.addTest(new ImapClientTest("getFolderTree", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testGetFolderTree(); }}));
        suite.addTest(new ImapClientTest("setActiveFolder", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testSetActiveFolder(); }}));

        suite.addTest(new ImapClientTest("getNewFolderMessages", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testGetNewFolderMessages(); }}));

        suite.addTest(new ImapClientTest("idleModeBegin", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testIdleModeBegin(); }}));
        suite.addTest(new ImapClientTest("idleModeMessagesAvailable", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testIdleModeMessagesAvailable(); }}));
        suite.addTest(new ImapClientTest("idleModeFlagsChanged", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testIdleModeFlagsChanged(); }}));
        suite.addTest(new ImapClientTest("idleModeExpungeFlagsChanged", new TestMethod()
        { public void run(TestCase tc) throws Throwable { ((ImapClientTest) tc).testIdleModeExpungeFlagsChanged(); }}));

        return suite;
    }
    
    class TestFolderMessageCallback implements FolderMessageCallback {
        public final Vector folderMessages = new Vector();
        public void folderMessageUpdate(FolderMessage folderMessage) {
            folderMessages.addElement(folderMessage);
        }
    }
    
    class TestImapClient extends ImapClient {
        public TestImapClient(
                GlobalConfig globalConfig,
                ImapConfig accountConfig,
                ImapProtocol imapProtocol) {
            super(new TestNetworkConnector(), globalConfig, accountConfig, imapProtocol);
        }
    }
    
    class TestNetworkConnector implements NetworkConnector {
        public Connection open(ConnectionConfig connectionConfig) throws IOException {
            Hamspy hamspy = new Hamspy(true);
            SocketConnection stubSocket = new MockSocketConnection(hamspy);
            hamspy.setStubExpectation(MockSocketConnection.MTHD_OPEN_DATA_INPUT_STREAM).setReturnValue(
                    new DataInputStream(new ByteArrayInputStream(new byte[] {
                            (byte)'\r', (byte)'\n', (byte)'\r', (byte)'\n',
                            (byte)'\r', (byte)'\n', (byte)'\r', (byte)'\n'
                    })));
            hamspy.setStubExpectation(MockSocketConnection.MTHD_OPEN_DATA_OUTPUT_STREAM).setReturnValue(
                    new DataOutputStream(new OutputStream() {
                        public void write(int b) throws IOException { }
                    }));
            
            Connection connection = new Connection(stubSocket);
            return connection;
        }

        public Connection getConnectionAsTLS(Connection connection) throws IOException {
            return connection;
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
