package org.logicprobe.LogicMail.ui;

import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.Node;
import org.logicprobe.LogicMail.model.NodeVisitor;

import net.rim.device.api.system.Bitmap;

/**
 * Provides icons for the mail object model nodes.
 */
public class NodeIcons {
	private static NodeIcons instance = new NodeIcons();

	private Bitmap localAccountIcon;
	private Bitmap networkAccountIcon0;
	private Bitmap networkAccountIcon1;
	private Bitmap folderIcon;
	private Bitmap folderNewIcon;
	private Bitmap inboxFolderIcon;
	private Bitmap inboxNewFolderIcon;
	private Bitmap outboxFolderIcon;
	private Bitmap draftsFolderIcon;
	private Bitmap sentFolderIcon;
	private Bitmap trashFolderIcon;
	private Bitmap trashFullFolderIcon;
    private Bitmap openedMessageIcon;
    private Bitmap unopenedMessageIcon;
    private Bitmap repliedMessageIcon;
    private Bitmap flaggedMessageIcon;
    private Bitmap draftMessageIcon;
    private Bitmap deletedMessageIcon;
    private Bitmap unknownMessageIcon;
    private Bitmap junkMessageIcon;
	
	private NodeIcons() {
		// Load account icons
		localAccountIcon    = Bitmap.getBitmapResource("account_local.png");
		networkAccountIcon0 = Bitmap.getBitmapResource("account_network_offline.png");
		networkAccountIcon1 = Bitmap.getBitmapResource("account_network_online.png");
		
		// Load folder icons
		folderIcon          = Bitmap.getBitmapResource("folder.png");
		folderNewIcon       = Bitmap.getBitmapResource("folder_new.png");
		inboxFolderIcon     = Bitmap.getBitmapResource("folder_inbox.png");
		inboxNewFolderIcon  = Bitmap.getBitmapResource("folder_inbox_new.png");
		outboxFolderIcon    = Bitmap.getBitmapResource("folder_outbox.png");
		draftsFolderIcon    = Bitmap.getBitmapResource("folder_drafts.png");
		sentFolderIcon      = Bitmap.getBitmapResource("folder_sent.png");
		trashFolderIcon     = Bitmap.getBitmapResource("folder_trash.png");
		trashFullFolderIcon = Bitmap.getBitmapResource("folder_trash_full.png");
		
		// Load message icons
		openedMessageIcon   = Bitmap.getBitmapResource("mail_opened.png");
		unopenedMessageIcon = Bitmap.getBitmapResource("mail_unopened.png");
		repliedMessageIcon  = Bitmap.getBitmapResource("mail_replied.png");
		flaggedMessageIcon  = Bitmap.getBitmapResource("mail_flagged.png");
		draftMessageIcon    = Bitmap.getBitmapResource("mail_draft.png");
		deletedMessageIcon  = Bitmap.getBitmapResource("mail_deleted.png");
		unknownMessageIcon  = Bitmap.getBitmapResource("mail_unknown.png");
		junkMessageIcon     = Bitmap.getBitmapResource("mail_junk.png");
	}
	
	public static Bitmap getIcon(Node node) {
		return instance.getIconImpl(node);
	}
	
	private Bitmap getIconImpl(Node node) {
		NodeIconVisitor visitor = new NodeIconVisitor();
		node.accept(visitor);
		return visitor.getIcon();
	}
	
	private class NodeIconVisitor implements NodeVisitor {
		private Bitmap icon;

		public void visit(MailRootNode node) { }

		public void visit(AccountNode node) {
			switch(node.getStatus()) {
			case AccountNode.STATUS_LOCAL:
				this.icon = localAccountIcon;
				break;
			case AccountNode.STATUS_OFFLINE:
				this.icon = networkAccountIcon0;
				break;
			case AccountNode.STATUS_ONLINE:
				this.icon = networkAccountIcon1;
				break;
			}
		}

		public void visit(MailboxNode node) {
			switch(node.getType()) {
			case MailboxNode.TYPE_INBOX:
				if(node.getUnseenMessageCount() > 0) {
					this.icon = inboxNewFolderIcon;
				}
				else {
					this.icon = inboxFolderIcon;
				}
				break;
			case MailboxNode.TYPE_DRAFTS:
				this.icon = draftsFolderIcon;
				break;
			case MailboxNode.TYPE_SENT:
				this.icon = sentFolderIcon;
				break;
			case MailboxNode.TYPE_TRASH:
				if(node.getMessageCount() > 0) {
					this.icon = trashFullFolderIcon;
				}
				else {
					this.icon = trashFolderIcon;
				}
				break;
			case MailboxNode.TYPE_OUTBOX:
				this.icon = outboxFolderIcon;
				break;
			case MailboxNode.TYPE_NORMAL:
			default:
				if(node.getUnseenMessageCount() > 0) {
					this.icon = folderNewIcon;
				}
				else {
					this.icon = folderIcon;
				}
				break;
			}
		}

		public void visit(MessageNode node) {
			FolderMessage folderMessage = node.getFolderMessage();
	        if(folderMessage.isDeleted())
	            this.icon = deletedMessageIcon;
	        else if(folderMessage.isJunk())
	        	this.icon = junkMessageIcon;
	        else if(folderMessage.isAnswered())
	        	this.icon = repliedMessageIcon;
	        else if(folderMessage.isFlagged())
	        	this.icon = flaggedMessageIcon;
	        else if(folderMessage.isDraft())
	        	this.icon = draftMessageIcon;
	        else if(folderMessage.isRecent())
	        	this.icon = unopenedMessageIcon;
	        else if(folderMessage.isSeen())
	        	this.icon = openedMessageIcon;
	        else
	        	this.icon = unknownMessageIcon;
		}
		
		public Bitmap getIcon() {
			return this.icon;
		}
	}
}
