package org.logicprobe.LogicMail.mail;

import java.util.Date;

/**
 * This class encapsulates all the data to represent an E-Mail message.
 */
public class Message {
    /**
     * Relevant header fields for a message.
     */
    public static class Envelope {
        // official envelope fields
        public Date date;
        public String subject;
        public String[] from;
        public String[] sender;
        public String[] replyTo;
        public String[] to;
        public String[] cc;
        public String[] bcc;
        public String inReplyTo;
        public String messageId;
        // other useful tidbits
        public int index;
        public boolean isOpened;
    }

    /**
     * Message body section
     */
    public static class Section {
        public String type;
        public String subtype;
        public String encoding;
        public int size;
    }

    /**
     * Message body structure
     */
    public static class Structure {
        public Section[] sections;
    }
}

