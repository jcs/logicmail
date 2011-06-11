/*
 * This source code was generated by HammockMaker.
 * It should be used with the Hammock libraries for the
 * CLDC 1.1 configuration.
 */
package org.logicprobe.LogicMail.mail;

import com.hammingweight.hammock.*;

public class MockIncomingMailClientListener extends AMockObject implements org.logicprobe.LogicMail.mail.IncomingMailClientListener {
    // Overridden methods.
    public static final MockMethod MTHD_FOLDER_MESSAGE_EXPUNGED_$_MESSAGETOKEN_ARRAY_MESSAGETOKEN = new MockMethod(
        MockIncomingMailClientListener.class, 
        "MTHD_FOLDER_MESSAGE_EXPUNGED_$_MESSAGETOKEN_ARRAY_MESSAGETOKEN",
        new Class[]{org.logicprobe.LogicMail.mail.MessageToken.class, org.logicprobe.LogicMail.mail.MessageToken[].class},
        new Class[]{},
        null,
        true);
    public void folderMessageExpunged(org.logicprobe.LogicMail.mail.MessageToken arg0, org.logicprobe.LogicMail.mail.MessageToken[] arg1)  {
        try {
            Object[] args = new Object[2];
            args[0] = arg0;
            args[1] = arg1;
            MethodInvocation mi = new MethodInvocation(MTHD_FOLDER_MESSAGE_EXPUNGED_$_MESSAGETOKEN_ARRAY_MESSAGETOKEN, this, args);
            getInvocationHandler().invoke(mi);
            mi.getReturnValue();
        } catch (Throwable t) {
            if (t instanceof java.lang.Error) { throw (java.lang.Error)t; }
            if (t instanceof java.lang.RuntimeException) { throw (java.lang.RuntimeException)t; }
            throw new HammockException(t);
        }
    }

    public static final MockMethod MTHD_FOLDER_MESSAGE_FLAGS_CHANGED_$_MESSAGETOKEN_MESSAGEFLAGS = new MockMethod(
        MockIncomingMailClientListener.class, 
        "MTHD_FOLDER_MESSAGE_FLAGS_CHANGED_$_MESSAGETOKEN_MESSAGEFLAGS",
        new Class[]{org.logicprobe.LogicMail.mail.MessageToken.class, org.logicprobe.LogicMail.message.MessageFlags.class},
        new Class[]{},
        null,
        true);
    public void folderMessageFlagsChanged(org.logicprobe.LogicMail.mail.MessageToken arg0, org.logicprobe.LogicMail.message.MessageFlags arg1)  {
        try {
            Object[] args = new Object[2];
            args[0] = arg0;
            args[1] = arg1;
            MethodInvocation mi = new MethodInvocation(MTHD_FOLDER_MESSAGE_FLAGS_CHANGED_$_MESSAGETOKEN_MESSAGEFLAGS, this, args);
            getInvocationHandler().invoke(mi);
            mi.getReturnValue();
        } catch (Throwable t) {
            if (t instanceof java.lang.Error) { throw (java.lang.Error)t; }
            if (t instanceof java.lang.RuntimeException) { throw (java.lang.RuntimeException)t; }
            throw new HammockException(t);
        }
    }

    public static final MockMethod MTHD_IDLE_MODE_ERROR = new MockMethod(
        MockIncomingMailClientListener.class, 
        "MTHD_IDLE_MODE_ERROR",
        new Class[]{},
        new Class[]{},
        null,
        true);
    public void idleModeError()  {
        try {
            Object[] args = new Object[0];
            MethodInvocation mi = new MethodInvocation(MTHD_IDLE_MODE_ERROR, this, args);
            getInvocationHandler().invoke(mi);
            mi.getReturnValue();
        } catch (Throwable t) {
            if (t instanceof java.lang.Error) { throw (java.lang.Error)t; }
            if (t instanceof java.lang.RuntimeException) { throw (java.lang.RuntimeException)t; }
            throw new HammockException(t);
        }
    }

    public static final MockMethod MTHD_RECENT_FOLDER_MESSAGES_AVAILABLE = new MockMethod(
        MockIncomingMailClientListener.class, 
        "MTHD_RECENT_FOLDER_MESSAGES_AVAILABLE",
        new Class[]{},
        new Class[]{},
        null,
        true);
    public void recentFolderMessagesAvailable()  {
        try {
            Object[] args = new Object[0];
            MethodInvocation mi = new MethodInvocation(MTHD_RECENT_FOLDER_MESSAGES_AVAILABLE, this, args);
            getInvocationHandler().invoke(mi);
            mi.getReturnValue();
        } catch (Throwable t) {
            if (t instanceof java.lang.Error) { throw (java.lang.Error)t; }
            if (t instanceof java.lang.RuntimeException) { throw (java.lang.RuntimeException)t; }
            throw new HammockException(t);
        }
    }

    // Constructors.
    public MockIncomingMailClientListener() {
        super();
    }

    public MockIncomingMailClientListener(IInvocationHandler handler) {
        super();
        setInvocationHandler(handler);
    }

}