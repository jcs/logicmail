package org.logicprobe.LogicMail.ui;

/**
 * This interface provides a common set of methods for
 * <code>TreeField</code> node cookies.
 * It allows for generic handling of common tree field operations.
 */
public interface TreeFieldNode {
	/**
	 * Returns <code>true</code> if this node can be opened.
	 * 
	 * @return True if selectable, false otherwise
	 */
	boolean isNodeSelectable();
}
