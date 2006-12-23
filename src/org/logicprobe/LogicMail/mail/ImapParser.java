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

package org.logicprobe.LogicMail.mail;

import java.util.Calendar;
import java.util.Vector;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * This class contains all static parser functions
 * needed when using the IMAP protocol
 */
class ImapParser {
    private ImapParser() { }
    
    static Message.Envelope parseMessageEnvelope(String rawText) {
        Vector parsedText = null;
        try {
            parsedText = StringParser.parseNestedParenString(rawText.substring(rawText.indexOf('(')));
        } catch (Exception exp) {
            return generateDummyEnvelope();
        }

        // Sanity checking
        if(parsedText.size() < 2 ||
           !(parsedText.elementAt(1) instanceof Vector))
           return generateDummyEnvelope();
        
        Vector parsedEnv = (Vector)parsedText.elementAt(1);

        // More sanity checking
        if(parsedEnv.size() < 10)
           return generateDummyEnvelope();
            
        Message.Envelope env = new Message.Envelope();

        if(parsedEnv.elementAt(0) instanceof String) {
            env.date = StringParser.parseDateString((String)parsedEnv.elementAt(0));
        }
        
        if(parsedEnv.elementAt(1) instanceof String) {
            env.subject = (String)parsedEnv.elementAt(1);
        }

        if(parsedEnv.elementAt(2) instanceof Vector) {
            env.from = parseAddressList((Vector)parsedEnv.elementAt(2));
        }
        
        if(parsedEnv.elementAt(3) instanceof Vector) {
            env.sender = parseAddressList((Vector)parsedEnv.elementAt(3));
        }

        if(parsedEnv.elementAt(4) instanceof Vector) {
            env.replyTo = parseAddressList((Vector)parsedEnv.elementAt(4));
        }

        if(parsedEnv.elementAt(5) instanceof Vector) {
            env.to = parseAddressList((Vector)parsedEnv.elementAt(5));
        }

        if(parsedEnv.elementAt(6) instanceof Vector) {
            env.cc = parseAddressList((Vector)parsedEnv.elementAt(6));
        }

        if(parsedEnv.elementAt(7) instanceof Vector) {
            env.bcc = parseAddressList((Vector)parsedEnv.elementAt(7));
        }

        if(parsedEnv.elementAt(8) instanceof String) {
            env.inReplyTo = (String)parsedEnv.elementAt(8);
            if(env.inReplyTo.equals("NIL")) env.inReplyTo = "";
        }

        if(parsedEnv.elementAt(9) instanceof String) {
            env.messageId = (String)parsedEnv.elementAt(9);
            if(env.messageId.equals("NIL")) env.messageId = "";
        }
        
        env.isOpened = false;
        
        env.index = Integer.parseInt(rawText.substring(rawText.indexOf(' '), rawText.indexOf("FETCH")-1).trim());
        return env;
    }
    
    static String[] parseAddressList(Vector addrVec) {
        // Find the number of addresses, and allocate the array
        String[] addrList = new String[addrVec.size()];
        int index = 0;
        
        for(int i=0;i<addrVec.size();i++) {
            if((addrVec.elementAt(i) instanceof Vector) &&
               ((Vector)addrVec.elementAt(i)).size() >= 4) {
                
                Vector entry = (Vector)addrVec.elementAt(i);

                String realName = "NIL";
                if(entry.elementAt(0) instanceof String)
                    realName = (String)entry.elementAt(0);

                String mbName = "NIL";
                if(entry.elementAt(2) instanceof String)
                    mbName = (String)entry.elementAt(2);

                String hostName = "NIL";
                if(entry.elementAt(3) instanceof String)
                    hostName = (String)entry.elementAt(3);
                // Now assemble these into a single address entry
                // (possibly eventually storing them separately)
                if(realName.length() > 0 && !realName.equals("NIL"))
                    addrList[index] = realName + " <" + mbName + "@" + hostName + ">";
                else
                    addrList[index] = mbName + "@" + hostName;
                index++;
            }
        }
        return addrList;
    }
    
    static Message.Structure parseMessageStructure(String rawText) {
        Vector parsedText = null;
        try {
            parsedText = StringParser.parseNestedParenString(rawText.substring(rawText.indexOf('(')));
        } catch (Exception exp) {
            return null;
        }

        // Sanity checking
        if(parsedText.size() < 2 ||
           !(parsedText.elementAt(1) instanceof Vector))
           return null;
        
        Vector parsedStruct = (Vector)parsedText.elementAt(1);

        Message.Structure msgStructure = new Message.Structure();

        // Determine the number of body parts and parse
        if(parsedStruct.elementAt(0) instanceof String) {
            msgStructure.sections = new Message.Section[1];
            msgStructure.sections[0] = parseMessageStructureSection(parsedStruct);
        }
        else {
            int count = 0;
            int i;
            for(i=0;i<parsedStruct.size();i++)
                if(parsedStruct.elementAt(i) instanceof Vector)
                    count++;
                else
                    break;
            msgStructure.sections = new Message.Section[count];
            for(i=0;i<count;i++)
                msgStructure.sections[i] = parseMessageStructureSection((Vector)parsedStruct.elementAt(i));
        }
        
        return msgStructure;
    }

    private static Message.Section parseMessageStructureSection(Vector sectionList) {
        Message.Section sec = new Message.Section();
        Vector tmpVec;
        
        if(sectionList.elementAt(0) instanceof String) {
            sec.type = ((String)sectionList.elementAt(0)).toLowerCase();
        }

        if(sectionList.elementAt(1) instanceof String) {
            sec.subtype = ((String)sectionList.elementAt(1)).toLowerCase();
        }

        sec.charset = null;
        if(sectionList.elementAt(2) instanceof Vector) {
            tmpVec = (Vector)sectionList.elementAt(2);
            if(tmpVec.size() >= 2) {
                if((tmpVec.elementAt(0) instanceof String) &&
                   ((String)tmpVec.elementAt(0)).equalsIgnoreCase("charset") &&
                   tmpVec.elementAt(1) instanceof String)
                    sec.charset = (String)tmpVec.elementAt(1);                    
            }
        }
        
        if(sectionList.elementAt(5) instanceof String) {
            sec.encoding = ((String)sectionList.elementAt(5)).toLowerCase();
        }

        if(sectionList.elementAt(6) instanceof String) {
            try {
                sec.size = Integer.parseInt((String)sectionList.elementAt(6));
            } catch (Exception exp) {
                sec.size = -1;
            }
        }

        return sec;
    }

    private static Message.Envelope generateDummyEnvelope() {
        Message.Envelope env = new Message.Envelope();
        env.date = Calendar.getInstance().getTime();
        env.from = new String[1];
        env.from[0] = "<sender>";
        env.subject = "<subject>";
        return env;
    }
}
