package org.logicprobe.LogicMail.model;

/**
 * Common interface for all nodes in the mail data model tree.
 */
public interface Node {
	/**
	 * Gets the name of this node.
	 * 
	 * @return The node name.
	 */
	public String getName();
	
	/**
	 * Accept the provided visitor to perform some action
	 * with the mail data model tree.
	 * 
	 * @param visitor The visitor.
	 */
	public void accept(NodeVisitor visitor);
}
