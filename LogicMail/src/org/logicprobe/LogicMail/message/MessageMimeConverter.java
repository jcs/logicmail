/*
 * MessageMimeConverter.java
 *
 * Created on February 24, 2007, 7:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.logicprobe.LogicMail.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import net.rim.device.api.mime.MIMEOutputStream;
import org.logicprobe.LogicMail.util.StringParser;
import org.logicprobe.LogicMail.util.UtilProxy;

/**
 * Converts a message into the equivalent MIME structure
 */
public class MessageMimeConverter implements MessagePartVisitor {
    private ByteArrayOutputStream byteArrayOutputStream;
    private MIMEOutputStream mimeOutputStream;
    private UtilProxy utilProxy;
    
    /** maps message parts to MIMEOutputStream objects */
    private Hashtable partMimeMap;
    
    /** Creates a new instance of MessageMimeConverter */
    public MessageMimeConverter() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        mimeOutputStream = null;
        utilProxy = UtilProxy.getInstance();
        partMimeMap = new Hashtable();
    }

    /**
     * Get the contents of this converter.
     * Due to the internal implementation, this method
     * may only be called once per instance.
     *
     * @return Message encoded in MIME format
     */
    public String toMimeString() {
        try {
            mimeOutputStream.flush();
            mimeOutputStream.close();
        } catch (IOException ex) {
            System.err.println("Unable to close MIMEOutputStream");
            return "";
        }
        return byteArrayOutputStream.toString();
    }
    
    public void visitMultiPart(MultiPart part) {
        // Handle the case of this being the root part
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, true, null);
            mimeOutputStream.setContentType(part.getMimeType() + "/" + part.getMimeSubtype());
            partMimeMap.put(part, mimeOutputStream);
        }
        // Otherwise handle the case of this being a child part
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            MIMEOutputStream currentStream = parentStream.getPartOutputStream(true, null);
            partMimeMap.put(part, currentStream);
        }
    }

    public void visitTextPart(TextPart part) {
        String charset = part.getCharset();
        boolean isBinary;
        boolean isQP;
        String encoding;
        
        if(charset.equalsIgnoreCase("US-ASCII")) {
            isBinary = false;
            isQP = false;
            encoding = "7bit";
        }
        else if(charset.equalsIgnoreCase("ISO-8859-1")) {
            isBinary = false;
            isQP = true;
            encoding = "quoted-printable";
        }
        else {
            isBinary = true;
            isQP = false;
            encoding = "base64";
        }
        
        MIMEOutputStream currentStream;
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, false, encoding);
            currentStream = mimeOutputStream;
        }
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            currentStream = parentStream.getPartOutputStream(false, encoding);
        }
        partMimeMap.put(part, currentStream);
        currentStream.setContentType(part.getMimeType() + "/" + part.getMimeSubtype());
        currentStream.addContentTypeParameter("charset", charset.toLowerCase());

        // Add the content, encoding as necessary
        try {
            if(!isBinary) {
                if(isQP)
                    currentStream.write(StringParser.encodeQuotedPrintable(part.getText()).getBytes(charset));
                else
                    currentStream.write(part.getText().getBytes(charset));
            }
            else {
                byte[] data = part.getText().getBytes(charset);
                currentStream.write(utilProxy.Base64Encode(data, 0, data.length, true, true));
            }
        } catch (IOException e) {
            System.err.println("Error encoding content");
        }
    }

    public void visitImagePart(ImagePart part) {
        MIMEOutputStream currentStream;
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, false, "base64");
            currentStream = mimeOutputStream;
        }
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            currentStream = parentStream.getPartOutputStream(false, "base64");
        }
        partMimeMap.put(part, currentStream);
        currentStream.setContentType(part.getMimeType() + "/" + part.getMimeSubtype());
        
        try {
            byte[] data = part.getImage().getData();
            currentStream.write(utilProxy.Base64Encode(data, 0, data.length, true, true));
        } catch (IOException e) {
            System.err.println("Error encoding content");
        }
    }

    public void visitUnsupportedPart(UnsupportedPart part) {
        MIMEOutputStream currentStream;
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, false, "7bit");
            currentStream = mimeOutputStream;
        }
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            currentStream = parentStream.getPartOutputStream(false, "7bit");
        }
        partMimeMap.put(part, currentStream);
        currentStream.setContentType("text/plain");
        try {
            currentStream.write("Unable to encode part".getBytes());
        } catch (IOException e) {
            System.err.println("Error encoding content");
        }
    }
}
