package org.logicprobe.LogicMail.ui;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.AccountNodeEvent;
import org.logicprobe.LogicMail.model.AccountNodeListener;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailManagerEvent;
import org.logicprobe.LogicMail.model.MailManagerListener;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MailboxNodeEvent;
import org.logicprobe.LogicMail.model.MailboxNodeListener;
import org.logicprobe.LogicMail.ui.MailHomeView.TreeNode;

/**
 * Presenter for displaying the mail home screen.
 */
public class MailHomePresenter {
	private NavigationController navigationController;
	private MailRootNode mailRootNode;
	private MailHomeView mailHomeView;

	private MailManager mailManager;
	private Hashtable accountTreeNodeMap;
	private Hashtable mailboxTreeNodeMap;
	private boolean firstVisible;
	private TreeNode mailTreeRootNode;
	
	private MailManagerListener mailManagerListener;
	private AccountNodeListener accountNodeListener;
	private MailboxNodeListener mailboxNodeListener;
	
	/**
	 * Constructs a new instance of the Mail Home presenter.
	 * 
	 * @param navigationController Controller for handling screen navigation
	 * @param mailRootNode Root node of the mail object model
	 * @param mailHomeView View to display data to
	 */
	public MailHomePresenter(
			NavigationController navigationController,
			MailRootNode mailRootNode,
			MailHomeView mailHomeView) {
		this.navigationController = navigationController;
		this.mailRootNode = mailRootNode;
		this.mailHomeView = mailHomeView;
		
		this.mailManager = MailManager.getInstance();
		this.accountTreeNodeMap = new Hashtable();
		this.mailboxTreeNodeMap = new Hashtable();
		
		this.mailManagerListener = new MailManagerListener() {
			public void mailConfigurationChanged(MailManagerEvent e) {
				mailManager_MailConfigurationChanged(e);
			}
		};
		
		this.accountNodeListener = new AccountNodeListener() {
			public void accountStatusChanged(AccountNodeEvent e) {
				accountNodeListener_AccountStatusChanged(e);
			}
		};

		this.mailboxNodeListener = new MailboxNodeListener() {
			public void mailboxStatusChanged(MailboxNodeEvent e) {
				mailboxNodeListener_MailboxStatusChanged(e);
			}
		};
		
		this.mailHomeView.setMailHomeListener(new MailHomeListener() {
			public int getContextMenuItems(Object activeField) {
				return mailHome_GetContextMenuItems((TreeNode)activeField);
			}
			public void handleContextMenuItem(int menuItem, Object activeField) {
				switch(menuItem) {
				case MailHomeView.MENUITEM_SELECT:
					mailHome_OpenNode((TreeNode)activeField);
					break;
				case MailHomeView.MENUITEM_REFRESH_STATUS:
					mailHome_RefreshNodeStatus((TreeNode)activeField);
					break;
				case MailHomeView.MENUITEM_REFRESH_FOLDERS:
					mailHome_RefreshNodeFolders((TreeNode)activeField);
					break;
				case MailHomeView.MENUITEM_COMPOSE_EMAIL:
					mailHome_ComposeMessage((TreeNode)activeField);
					break;
				case MailHomeView.MENUITEM_DISCONNECT:
					mailHome_DisconnectNodeAccount((TreeNode)activeField);
					break;
				}
			}
			public void handleVisibilityChange(boolean visible) {
				mailHome_HandleVisibilityChange(visible);
			}
		});
	}

	public void initialize() {
		refreshMailTree();
		mailManager.addMailManagerListener(mailManagerListener);
	}
	
	private void mailManager_MailConfigurationChanged(MailManagerEvent e) {
		refreshMailTree();
	}

	private void accountNodeListener_AccountStatusChanged(AccountNodeEvent e) {
		if(e.getType() == AccountNodeEvent.TYPE_CONNECTION) {
			TreeNode node = (TreeNode)accountTreeNodeMap.get(e.getSource());
			if(node != null) {
				mailHomeView.refreshMailTreeNode(node);
			}
		}
		else if(e.getType() == AccountNodeEvent.TYPE_MAILBOX_TREE) {
			refreshAccountFolders((AccountNode)e.getSource());
		}
	}

	private void mailboxNodeListener_MailboxStatusChanged(MailboxNodeEvent e) {
		TreeNode mailboxTreeNode = (TreeNode)mailboxTreeNodeMap.get(e.getSource());
		if(mailboxTreeNode != null) {
			mailHomeView.refreshMailTreeNode(mailboxTreeNode);
		}
	}

	private void refreshMailTree() {
		clearMailTreeSubscriptions();
		generateMailTree();
		mailHomeView.populateMailTree(mailTreeRootNode);
	}
	
	private void generateMailTree() {
		mailTreeRootNode = new TreeNode(null, 0);
		
		AccountNode[] accounts = mailRootNode.getAccounts();
		mailTreeRootNode.children = new TreeNode[accounts.length];
		for(int i=0; i<accounts.length; i++) {
			TreeNode accountTreeNode = new TreeNode(accounts[i], TreeNode.TYPE_ACCOUNT);
			MailboxNode rootMailbox = accounts[i].getRootMailbox();
			if(rootMailbox != null) {
				MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
				accountTreeNode.children = new TreeNode[mailboxNodes.length];
				for(int j=0; j < mailboxNodes.length; j++) {
					accountTreeNode.children[j] = populateMailboxTreeNode(mailboxNodes[j]);
				}
			}
			accounts[i].addAccountNodeListener(accountNodeListener);
			accountTreeNodeMap.put(accounts[i], accountTreeNode);
			mailTreeRootNode.children[i] = accountTreeNode;
		}
	}

	private TreeNode populateMailboxTreeNode(MailboxNode mailboxNode) {
		TreeNode mailboxTreeNode = new TreeNode(mailboxNode, TreeNode.TYPE_MAILBOX);

		mailboxNode.addMailboxNodeListener(mailboxNodeListener);
		mailboxTreeNodeMap.put(mailboxNode, mailboxTreeNode);
		
		MailboxNode[] mailboxes = mailboxNode.getMailboxes();
		mailboxTreeNode.children = new TreeNode[mailboxes.length];
		for(int i=0; i < mailboxes.length; i++) {
			mailboxTreeNode.children[i] = populateMailboxTreeNode(mailboxes[i]);
		}
		
		return mailboxTreeNode;
	}

	private void clearMailTreeSubscriptions() {
		for (Enumeration e = accountTreeNodeMap.keys(); e.hasMoreElements() ;) {
			AccountNode node = (AccountNode)e.nextElement();
			node.removeAccountNodeListener(accountNodeListener);
		}
		accountTreeNodeMap.clear();
		
		for (Enumeration e = mailboxTreeNodeMap.keys(); e.hasMoreElements() ;) {
			MailboxNode node = (MailboxNode)e.nextElement();
			node.removeMailboxNodeListener(mailboxNodeListener);
		}
		mailboxTreeNodeMap.clear();
	}

	private void refreshAccountFolders(AccountNode accountNode) {
		// Unsubscribe from all existing mailbox nodes, remove them
		// from the node-id map, and remove them from the tree.
		Vector mailboxNodeList = new Vector();
		TreeNode accountTreeNode = (TreeNode)accountTreeNodeMap.get(accountNode);
		getMailboxNodes(mailboxNodeList, accountTreeNode);
		int size = mailboxNodeList.size();
		for(int i=0; i<size; i++) {
			MailboxNode mailboxNode = (MailboxNode)mailboxNodeList.elementAt(i);
			if(mailboxTreeNodeMap.containsKey(mailboxNode)) {
				mailboxNode.removeMailboxNodeListener(mailboxNodeListener);
				mailboxTreeNodeMap.remove(mailboxNode);
			}
		}

		// Now get the new mailbox list for the account
		// and repopulate the tree.
		MailboxNode rootMailbox = accountNode.getRootMailbox();
		if(rootMailbox != null) {
			MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
			accountTreeNode.children = new TreeNode[mailboxNodes.length];
			for(int i=0; i < mailboxNodes.length; i++) {
				accountTreeNode.children[i] = populateMailboxTreeNode(mailboxNodes[i]);
			}
		}
		mailHomeView.populateMailTree(mailTreeRootNode);
	}
	
	/**
	 * Recursively traverse a tree of TreeNodes and build a
	 * linear vector of all the MailboxNodes contained within.
	 * 
	 * @param result Result vector to add to.
	 * @param nodeId Starting node.
	 */
	private static void getMailboxNodes(Vector result, TreeNode treeNode) {
		if(treeNode.node instanceof MailboxNode) {
			result.addElement(treeNode.node);
		}
		if(treeNode.children != null) {
			for(int i=0; i<treeNode.children.length; i++) {
				getMailboxNodes(result, treeNode.children[i]);
			}
		}
	}

	private void mailHome_HandleVisibilityChange(boolean visible) {
		if(visible && firstVisible) {
			firstVisible = false;
			// Check to see if there are no configured accounts
			if(mailRootNode.getAccounts().length <= 1) {
				navigationController.displayAccountConfigurationWizard();
			}
		}
	}	

	private boolean mailHome_OpenNode(TreeNode treeNode) {
		if(treeNode.node instanceof MailboxNode) {
			MailboxNode mailboxNode = (MailboxNode)treeNode.node;
			navigationController.displayMailbox(mailboxNode);
			return true;
		}
		else {
			return false;
		}
	}

	private void mailHome_RefreshNodeStatus(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);
		
		if(accountNode != null) {
			accountNode.refreshMailboxStatus();
		}
	}

	private void mailHome_RefreshNodeFolders(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);
		
		if(accountNode != null) {
			accountNode.refreshMailboxes();
		}
	}

	private void mailHome_DisconnectNodeAccount(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);

		if(accountNode != null) {
			accountNode.requestDisconnect(false);
		}
	}

	private void mailHome_ComposeMessage(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);

		if(accountNode != null) {
			navigationController.displayComposition(accountNode);
		}
	}

	private int mailHome_GetContextMenuItems(TreeNode treeNode) {
		int menuItems = 0;
		if(treeNode.node instanceof MailboxNode) {
			menuItems |= MailHomeView.MENUITEM_SELECT;
			menuItems |= MailHomeView.MENUITEM_COMPOSE_EMAIL;
		}
		else if(treeNode.node instanceof AccountNode) {
			AccountNode accountNode = (AccountNode)treeNode.node; 
			if(accountNode.getRootMailbox() != null) {
				menuItems |= MailHomeView.MENUITEM_REFRESH_STATUS;
			}
			if(accountNode.hasFolders()) {
				menuItems |= MailHomeView.MENUITEM_REFRESH_FOLDERS;
			}
			if(accountNode.hasMailSender()) {
				menuItems |= MailHomeView.MENUITEM_COMPOSE_EMAIL;
			}
			if(accountNode.getStatus() == AccountNode.STATUS_ONLINE) {
				menuItems |= MailHomeView.MENUITEM_DISCONNECT;
			}
		}
		return menuItems;
	}
	
	private static AccountNode getAccountForTreeNode(TreeNode treeNode) {
		AccountNode accountNode;
		if(treeNode.node instanceof AccountNode) {
			accountNode = (AccountNode)treeNode.node;
		}
		else if(treeNode.node instanceof MailboxNode) {
			MailboxNode mailboxNode = (MailboxNode)treeNode.node;
			accountNode = mailboxNode.getParentAccount();
		}
		else {
			accountNode = null;
		}
		return accountNode;
	}
}
