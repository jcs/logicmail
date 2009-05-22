package org.logicprobe.LogicMail.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Abstract representation of a message part representing content.
 */
public abstract class ContentPart extends MessagePart {
	private String name;
	private String encoding;
	private String disposition;

	public ContentPart(String mimeType, String mimeSubtype, String name, String encoding, String disposition, int size, String tag) {
        super(mimeType, mimeSubtype, size, tag);
        this.name = name;
        this.encoding = encoding;
        this.disposition = disposition;
    }

	public ContentPart(String mimeType, String mimeSubtype, String name, String encoding, String disposition, int size) {
        this(mimeType, mimeSubtype, name, encoding, disposition, size, "");
    }

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
    public String getEncoding() {
    	return encoding;
    }
    
    public void setEncoding(String encoding) {
    	this.encoding = encoding;
    }
    
    public String getDisposition() {
    	return disposition;
    }
    
    public void setDisposition(String disposition) {
    	this.disposition = disposition;
    }

    /* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutputStream)
	 */
	public void serialize(DataOutputStream output) throws IOException {
		super.serialize(output);
		output.writeUTF(name);
		output.writeUTF(encoding);
		output.writeUTF(disposition);
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInputStream)
	 */
	public void deserialize(DataInputStream input) throws IOException {
		super.deserialize(input);
		name = input.readUTF();
		encoding = input.readUTF();
		disposition = input.readUTF();
	}
}
