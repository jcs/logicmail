package org.logicprobe.LogicMail.ui;

import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;

public class ScreenFactoryBB50T extends ScreenFactoryBB50 {
    public StandardScreen getMailHomeScreen(NavigationController navigationController, MailRootNode mailRootNode) {
        return new StandardTouchScreen(navigationController, new TouchMailHomeScreen(mailRootNode));
    }

    public StandardScreen getMailboxScreen(NavigationController navigationController, MailboxNode mailboxNode) {
        return new StandardTouchScreen(navigationController, new MailboxScreen(mailboxNode));
    }
}
