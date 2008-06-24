package org.logicprobe.LogicMail.model;

/**
 * Interface for visitors that perform some action
 * with the mail data model tree.
 */
public interface NodeVisitor {
	/**
	 * Visit the root node.
	 * 
	 * @param node The node.
	 */
	void visit(MailRootNode node);
	
	/**
	 * Visit an account node.
	 * 
	 * @param node The node.
	 */
	void visit(AccountNode node);
	
	/**
	 * Visit a mailbox node.
	 * 
	 * @param node The node.
	 */
	void visit(MailboxNode node);
	
	/**
	 * Visit a message node.
	 * 
	 * @param node The node.
	 */
	void visit(MessageNode node);
}
