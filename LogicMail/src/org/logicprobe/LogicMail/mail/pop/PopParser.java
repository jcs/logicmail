/*-
 * Copyright (c) 2006, Derek Konigsberg
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

package org.logicprobe.LogicMail.mail.pop;

import java.util.Calendar;
import java.util.Hashtable;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * This class contains all static parser functions
 * needed when using the IMAP protocol
 */
class PopParser {
    private PopParser() { }

    static MessageEnvelope parseMessageEnvelope(String[] rawHeaders) {
        Hashtable headers = StringParser.parseMailHeaders(rawHeaders);
        MessageEnvelope env = new MessageEnvelope();
        
        // Populate the common header field bits of the envelope
        env.subject = StringParser.parseEncodedHeader((String)headers.get("subject"));
        if(env.subject == null) {
            env.subject = "<subject>";
        }
        env.from = parseAddressList((String)headers.get("from"));
        env.to = parseAddressList((String)headers.get("to"));
        env.cc = parseAddressList((String)headers.get("cc"));
        env.bcc = parseAddressList((String)headers.get("bcc"));
        try {
            env.date = StringParser.parseDateString((String)headers.get("date"));
        } catch (Exception e) {
            env.date = Calendar.getInstance().getTime();
        }
        env.replyTo = parseAddressList((String)headers.get("reply-to"));
        env.messageId = (String)headers.get("message-id");
        env.inReplyTo = (String)headers.get("in-reply-to");
        return env;
    }
    
    static String[] parseAddressList(String text) {
        String[] addresses = StringParser.parseTokenString(text, ", ");
        for(int i=0; i<addresses.length; i++) {
            addresses[i] = StringParser.parseEncodedHeader(addresses[i]);
        }
        return addresses;
    }
}
