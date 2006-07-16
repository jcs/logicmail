package org.logicprobe.LogicMail.mail;

/**
 * Exception class for protocol errors
 */
public class MailException extends Exception {
    public MailException(String message) {
        super(message);
    }
}

