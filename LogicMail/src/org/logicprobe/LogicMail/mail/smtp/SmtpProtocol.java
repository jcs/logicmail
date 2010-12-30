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

package org.logicprobe.LogicMail.mail.smtp;

import java.io.IOException;
import java.util.Hashtable;

import net.rim.device.api.crypto.MD5Digest;
import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.util.Connection;

/**
 * This class implements the commands for the SMTP protocol
 */
public class SmtpProtocol {
    private Connection connection;
    
    /** Specifies the PLAIN authentication mechanism */
    public static final int AUTH_PLAIN = 1;
    /** Specifies the LOGIN authentication mechanism */
    public static final int AUTH_LOGIN = 2;
    /** Specifies the CRAM-MD5 authentication mechanism */
    public static final int AUTH_CRAM_MD5 = 3;
    /** Specifies the DIGEST-MD5 authentication mechanism */
    public static final int AUTH_DIGEST_MD5 = 4;
    
    /** Creates a new instance of SmtpProtocol */
    public SmtpProtocol() {
    }
    
    /**
     * Sets the connection instance used by this class.
     * This must be set after opening the connection, and prior to calling any
     * command methods.
     *
     * @param connection the new connection instance
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Execute the "AUTH" command.
     * @param mech Authentication mechanism to use
     * @param username Username
     * @param password Password
     * @return True if successful, false on failure
     */
    public boolean executeAuth(int mech, String username, String password) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("SmtpProtocol.executeAuth("+mech+", \""+username+"\", \""+password+"\")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result;
        byte[] data;
        if(mech == AUTH_PLAIN) {
            result = execute(STR_AUTH_PLAIN);
            if(!result.startsWith(CODE_334)) {
                throw new MailException(result.substring(4));
            }
            
            // Format the username and password
            byte[] userData = username.getBytes();
            byte[] passData = password.getBytes();
            data = new byte[userData.length + passData.length + 2];
            int i = 0;
            data[i++] = 0;
            for(int j=0; j<userData.length; j++) {
                data[i++] = userData[j];
            }
            data[i++] = 0;
            for(int j=0; j<passData.length; j++) {
                data[i++] = passData[j];
            }
            
            result = execute(Base64OutputStream.encodeAsString(data, 0, data.length, false, false));
            if(!result.startsWith(CODE_235)) {
                return false;
            }
        }
        else if(mech == AUTH_LOGIN) {
            result = execute(STR_AUTH_LOGIN);
            if(!result.startsWith(CODE_334)) {
                throw new MailException(result.substring(4));
            }
            data = username.getBytes();
            result = execute(Base64OutputStream.encodeAsString(data, 0, data.length, false, false));
            if(!result.startsWith(CODE_334)) {
                throw new MailException("Authentication error");
            }
            data = password.getBytes();
            result = execute(Base64OutputStream.encodeAsString(data, 0, data.length, false, false));
            if(!result.startsWith(CODE_235)) {
                return false;
            }
        }
        else if(mech == AUTH_CRAM_MD5) {
            result = execute(STR_AUTH_CRAM_MD5);
            if(!result.startsWith(CODE_334)) {
                throw new MailException(result.substring(4));
            }
            
            byte[] challenge = Base64InputStream.decode(result.substring(4));
           
            byte[] s = password.getBytes(US_ASCII);
            byte[] digest = hmac_md5(s, challenge);
            StringBuffer buf = new StringBuffer();
            buf.append(username);
            buf.append(' ');
            buf.append(byteArrayToHexString(digest));

            byte[] eval = buf.toString().getBytes(US_ASCII);
            
            result = execute(Base64OutputStream.encodeAsString(eval, 0, eval.length, false, false));
            if(!result.startsWith(CODE_235)) {
                return false;
            }
        }
        else {
            throw new MailException("Unknown authentication mechanism");
        }
        return true;
    }

    /**
     * Execute the "EHLO" command.
     * @param domain Domain name of the client
     * @return Table of returned strings
     */
    public Hashtable executeExtendedHello(String domain) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("SmtpProtocol.executeExtendedHello(\""+domain+"\")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String[] result = executeFollow(EHLO + ' ' + domain);
        Hashtable items = new Hashtable();
        for(int i=0; i<result.length; i++) {
            if(result[i].length() > 4) {
                items.put(result[i].substring(4), Boolean.TRUE);
            }
        }
        return items;
    }

    /**
     * Execute the "STARTTLS" command.
     * The underlying connection mode must be switched after the
     * successful execution of this command.
     */
    public void executeStartTLS() throws IOException, MailException {
        if (EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(AppInfo.GUID,
                ("SmtpProtocol.executeStartTLS()").getBytes(),
                EventLogger.DEBUG_INFO);
        }
		execute(STARTTLS);
	}

    /**
     * Execute the "MAIL FROM" command.
     * @param sender Sender of the message, formatted the standard way,
     *               as "foo@bar.com"
     * @return True if successful, false on failure
     */
    public boolean executeMail(String sender) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("SmtpProtocol.executeMain(\""+sender+"\")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result = execute("MAIL FROM:<" + sender + ">");
        return result.startsWith(CODE_250);
    }

    /**
     * Execute the "RCPT TO" command.
     * @param recipient Recipient of the message, formatted the standard way,
     *                  as "foo@bar.com"
     * @return True if successful, false on failure
     */
    public boolean executeRecipient(String recipient) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("SmtpProtocol.executeRecipient(\""+recipient+"\")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result = execute("RCPT TO:<" + recipient + ">");
        return result.startsWith(CODE_250);
    }
    
    /**
     * Execute the "DATA" command.
     * @param message Message data fully serialized into a flat ASCII string
     * @return True if successful, false on failure
     */
    public boolean executeData(String message) throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("SmtpProtocol.executeData(\""+message+"\")").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result = execute(DATA);
        if(!result.startsWith(CODE_354)) {
            return false;
        }
        
        connection.sendRaw(message);
        result = execute("\r\n.");
        
        return result.startsWith(CODE_250);
    }
    
    /**
     * Execute the "RSET" command.
     * @return True if successful, false on failure
     */
    public boolean executeReset() throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("SmtpProtocol.executeReset()").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result = execute(RSET);
        return result.startsWith(CODE_250);
    }
    
    /**
     * Execute the "QUIT" command.
     * @return True if successful, false on failure
     */
    public boolean executeQuit() throws IOException, MailException {
        if(EventLogger.getMinimumLevel() >= EventLogger.DEBUG_INFO) {
            EventLogger.logEvent(
            AppInfo.GUID,
            ("SmtpProtocol.executeQuit()").getBytes(),
            EventLogger.DEBUG_INFO);
        }
        String result = execute(QUIT);
        return result.startsWith(CODE_221);
    }

    /**
     * Execute an SMTP command, and return the result.
     * If the command is null, we still wait for a result
     * so we can receive a multi-line response.
     *
     * @param command The command
     * @return The result
     */
    private String execute(String command) throws IOException, MailException {
        if(command != null) {
            connection.sendCommand(command);
        }
        
        String result = new String(connection.receive());
        
        return result;
    }

    /**
     * Execute an SMTP command that returns multiple lines.
     * This works by running the normal execute() and then
     * receiving every new line until a line with a space
     * between the code and value is encountered.
     *
     * @param command The command to execute
     * @return An array of lines containing the response
     */
    private String[] executeFollow(String command) throws IOException, MailException {
        execute(command);
            
        String buffer = new String(connection.receive());
        String[] lines = new String[0];
        while(buffer != null) {
            buffer = new String(connection.receive());
            Arrays.add(lines, buffer);
            if(buffer.length() >=4 && buffer.charAt(3) == ' ') {
                break;
            }
        }
        return lines;
    }

    private static final char[] HEX_CHARS =
        {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    /**
     * Convert a byte array to a hex string.
     * This converts to a string where the letters in the
     * hex string are in lower-case.
     *
     * @param hash Input byte array
     * @return Hex string
     */
    public static final String byteArrayToHexString( final byte[] hash ) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
                buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
                buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }
    
    /**
     * Computes a CRAM digest using the HMAC algorithm:
     * <pre>
     * MD5(key XOR opad, MD5(key XOR ipad, text))
     * </pre>.
     * <code>secret</code> is null-padded to a length of 64 bytes.
     * If the shared secret is longer than 64 bytes, the MD5 digest of the
     * shared secret is used as a 16 byte input to the keyed MD5 calculation.
     * See RFC 2104 for details.
     */
    private static byte[] hmac_md5(byte[] key, byte[] text) {
        byte[] k_ipad = new byte[64];
        byte[] k_opad = new byte[64];
        byte[] digest;
        
        MD5Digest md5 = new MD5Digest();
        // if key is longer than 64 bytes reset it to key=MD5(key)
        if (key.length>64) {
            md5.update(key);
            key = md5.getDigest();
        }
        // start out by storing key in pads
        System.arraycopy(key, 0, k_ipad, 0, key.length);
        System.arraycopy(key, 0, k_opad, 0, key.length);
        // XOR key with ipad and opad values
        for (int i=0; i<64; i++) {
            k_ipad[i] ^= 0x36;
            k_opad[i] ^= 0x5c;
        }
        // perform inner MD5
        md5.reset();
        md5.update(k_ipad);
        md5.update(text);
        digest = md5.getDigest();
        
        // perform outer MD5
        md5.reset();
        md5.update(k_opad);
        md5.update(digest);
        digest = md5.getDigest();
        
        return digest;
    }

    // String constants
    private static String STR_AUTH_PLAIN = "AUTH PLAIN";
    private static String STR_AUTH_LOGIN = "AUTH LOGIN";
    private static String STR_AUTH_CRAM_MD5 = "AUTH CRAM-MD5";
    private static String US_ASCII = "US-ASCII";
    private static String CODE_221 = "221";
    private static String CODE_235 = "235";
    private static String CODE_250 = "250";
    private static String CODE_334 = "334";
    private static String CODE_354 = "354";
    private static String EHLO = "EHLO";
    private static String STARTTLS = "STARTTLS";
    private static String DATA = "DATA";
    private static String RSET = "RSET";
    private static String QUIT = "QUIT";
}
