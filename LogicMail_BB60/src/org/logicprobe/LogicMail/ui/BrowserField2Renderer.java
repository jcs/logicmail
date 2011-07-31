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
package org.logicprobe.LogicMail.ui;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.InputConnection;

import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldConfig;
import net.rim.device.api.browser.field2.BrowserFieldController;
import net.rim.device.api.browser.field2.BrowserFieldListener;
import net.rim.device.api.browser.field2.BrowserFieldRequest;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.TouchEvent;
import net.rim.device.api.ui.Touchscreen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.container.HorizontalFieldManager;

import org.logicprobe.LogicMail.message.ContentPart;
import org.logicprobe.LogicMail.message.MimeMessageContent;
import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.NetworkAccountNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html2.HTMLAnchorElement;

/**
 * Creates a browser field for displaying HTML message content, using the
 * BrowserField2 API.
 */
public class BrowserField2Renderer {
    private final MessageNode messageNode;
    private final TextContent content;

    private static String TAG_NAME_A = "a";
    private static String MAILTO = "mailto:";
    private static String LM_MAILTO = "lmmailto://";
    
    /**
     * Instantiates a new browser field 2 renderer.
     * 
     * @param messageNode the message node
     * @param content the content
     */
    public BrowserField2Renderer(MessageNode messageNode, TextContent content) {
        this.messageNode = messageNode;
        this.content = content;
    }
    
    private BrowserFieldListener listener = new BrowserFieldListener() {
        public void documentLoaded(BrowserField browserField, Document document) throws Exception {
            browserFieldDocumentLoaded(browserField, document);
        }
    };
    
    private BrowserFieldController controller = new BrowserFieldController() {
        public void handleNavigationRequest(BrowserFieldRequest request) throws Exception {
            browserFieldNavigationRequest(request);
        }
        public InputConnection handleResourceRequest(BrowserFieldRequest request) throws Exception {
            return browserFieldResourceRequest(request);
        }
    };
    
    /**
     * Gets the browser field for the content this renderer was initialized with.
     * 
     * @return the browser field
     */
    public Field getBrowserField() {
        BrowserFieldConfig config = new BrowserFieldConfig();
        config.setProperty(BrowserFieldConfig.JAVASCRIPT_ENABLED, Boolean.FALSE);
        config.setProperty(BrowserFieldConfig.INITIAL_SCALE, new Float(1.0));
        config.setProperty(BrowserFieldConfig.USER_SCALABLE, Boolean.TRUE);
        config.setProperty(BrowserFieldConfig.VIEWPORT_WIDTH, new Integer(Display.getWidth()));
        
        config.setProperty(BrowserFieldConfig.NAVIGATION_MODE,
                Touchscreen.isSupported() ?
                        BrowserFieldConfig.NAVIGATION_MODE_NODE :
                            BrowserFieldConfig.NAVIGATION_MODE_POINTER);
        
        config.setProperty(BrowserFieldConfig.CONTROLLER, controller);
        
        final BrowserField browserField = new BrowserField(config);
        browserField.addListener(listener);
        
        HorizontalFieldManager manager = new HorizontalFieldManager(Manager.HORIZONTAL_SCROLL) {
            protected boolean keyChar(char ch, int status, int time) {
                int shortcut = KeyHandler.keyCharBrowserShortcut(ch, status);
                switch(shortcut) {
                case KeyHandler.BROWSER_ZOOM_IN:
                    browserField.setZoomScale(browserField.getZoomScale() + 0.1F);
                    return true;
                case KeyHandler.BROWSER_ZOOM_OUT:
                    browserField.setZoomScale(browserField.getZoomScale() - 0.1F);
                    return true;
                default:
                    return false;
                }
            };
            
            protected boolean touchEvent(TouchEvent message) {
                // We need to handle the touch event here, so that horizontal
                // scrolling works.  We also need to claim that we did not
                // handle the event, so that the parent manager can handle
                // vertical scrolling as well.
                super.touchEvent(message);
                return false;
            }
        };
        manager.add(browserField);
        
        browserField.displayContent(content.getRawData(), "text/html", "");
        
        return manager;
    }

    protected void browserFieldDocumentLoaded(BrowserField browserField, Document document) {
        NodeList nodeList = document.getElementsByTagName(TAG_NAME_A);
        int len = nodeList.getLength();
        for(int i=0; i<len; i++) {
            Node node = nodeList.item(i);
            if(node instanceof HTMLAnchorElement) {
                HTMLAnchorElement anchor = (HTMLAnchorElement)node;
                String href = anchor.getHref();
                if(href != null && href.regionMatches(true, 0, MAILTO, 0, MAILTO.length()) && href.length() > MAILTO.length()) {
                    anchor.setHref(LM_MAILTO + href.substring(7));
                }
            }
        }
    }

    protected void browserFieldNavigationRequest(BrowserFieldRequest request) {
        String url = request.getURL();
        
        if(url == null || url.length() == 0) { return; }
        
        if(url.startsWith(LM_MAILTO)) {
            String address = url.substring(LM_MAILTO.length());
            Screen screen = UiApplication.getUiApplication().getActiveScreen();
            
            if(screen instanceof StandardScreen && messageNode.getParent() != null
                    && messageNode.getParent().getParentAccount() instanceof NetworkAccountNode) {
                
                ((StandardScreen)screen).getNavigationController().displayComposition(
                        (NetworkAccountNode)messageNode.getParent().getParentAccount(),
                        address);
            }
        }
        else {
            BrowserSession browserSession = Browser.getDefaultSession();
            browserSession.displayPage(url);
        }
    }

    private InputConnection browserFieldResourceRequest(BrowserFieldRequest request) throws Exception {
        MimeMessageContent contentMatch = findContentMatch(request);
        if(contentMatch != null) {
            return new LocalResourceInputConnection(contentMatch);
        }
        else {
            return null;
        }
    }
    
    private MimeMessageContent findContentMatch(BrowserFieldRequest request) {
        String url = request.getURL();
        int p = url.indexOf("cid:");
        if(p == -1 || url.length() < 5) { return null; }
        String contentId = '<' + url.substring(4) + '>';
        MimeMessageContent contentMatch = null;

        MimeMessageContent[] contentArray = messageNode.getAllMessageContent();
        for(int i=0; i<contentArray.length; i++) {
            ContentPart part = contentArray[i].getMessagePart();
            if(contentId.equals(part.getContentId())) {
                contentMatch = contentArray[i];
                break;
            }
        }
        return contentMatch;
    }
    
    private static class LocalResourceInputConnection implements InputConnection {
        private final MimeMessageContent content;
        private InputStream stream;

        public LocalResourceInputConnection(MimeMessageContent content) {
            this.content = content;
        }

        public DataInputStream openDataInputStream() throws IOException {
            return new DataInputStream(openInputStream());
        }

        public InputStream openInputStream() throws IOException {
            if(stream == null) {
                byte[] data = content.getRawData();
                if(data == null) {
                    data = new byte[0];
                }
                stream = new ByteArrayInputStream(data);
            }
            return stream;
        }

        public void close() throws IOException {
            if(stream != null) {
                stream.close();
            }
        }
    }
}
