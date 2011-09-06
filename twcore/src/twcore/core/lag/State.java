package twcore.core.lag;

import java.util.Vector;

/**
 * This class models a State tree.  The purpose of this class is to allow for
 * a simple way to set up parent and child states.  This datastructure allows
 * the user to add states to the tree, specifing the parent state and the
 * child name.  After building the state tree, the user may set the current
 * state to one of the states that was just created.  Then, a check may be
 * performed to see if the current state is a descendant of another state as
 * specified by the user.
 *
 * Updated 03/09/04 by Cpt.Guano!
 * - Added a toString function to output a string representation of the State
 *   Tree.
 *
 * @author Cpt.Guano!
 * @version 1.1, 03/09/04
 */
public class State {
    public static final String ROOT_NAME = "Root";
    private StateNode root;
    private String currentState;

    /**
     * Constructor for the state class.  Creates a root node.
     */

    public State() {
        root = new StateNode(ROOT_NAME);
        currentState = ROOT_NAME;
    }

    /**
     * Checks to see if the object is in a certain state.  If the current state
     * is a descendant of the target state then true is returned.
     *
     * @param stateName is the name of the state to check.
     * @return True if it is in the state specified by stateName.
     */

    public boolean isCurrentState(String stateName) {
        StateNode checkState = getStateNode(stateName);

        return containsStateRec(checkState, currentState);
    }

    /**
     * Adds a state to the state tree at the root node.
     *
     * @param stateName is the name of the state to add.
     */

    public void addState(String stateName) {
        addState(ROOT_NAME, stateName);
    }

    /**
     * Adds a state to the state tree at a node specified.
     *
     * @param parentState is the name of the parent state.
     * @param stateName is the name of the state to add.
     */

    public void addState(String parentState, String stateName) {
        if (!containsState(parentState))
            throw new IllegalArgumentException("ERROR: Parent state not found.");
        if (containsState(stateName))
            throw new IllegalArgumentException("ERROR: State already exists.");

        StateNode addNode = getStateNode(parentState);
        addNode.addChild(stateName);
    }

    /**
     * Sets the current state to the state specified.
     *
     * @param stateName is the name of the state to switch to.
     */

    public void setCurrentState(String stateName) {
        if (!containsState(stateName))
            throw new IllegalArgumentException("ERROR: Invalid state name.");
        currentState = stateName;
    }

    /**
     * This method displays the tree as a string.  It is in the following format:
     * (Parent, Child1, Child2, ... Childn)
     * The Children are subtrees and thus can have children of their own.
     *
     * @return the string representation of the State Tree is returned
     */

    public String toString() {
        return toStringRec(root).toString();
    }

    /**
     * (Helper Function) This method creates a string representation of the
     * State Tree using a recursive method.
     *
     * @param currentNode is the current node that is being analyzed.
     * @return a StringBuffer of the tree as a string is returned.
     */

    private StringBuffer toStringRec(StateNode currentNode) {
        StringBuffer returnString = new StringBuffer();

        returnString.append(currentNode.getName());
        for (int index = 0; index < currentNode.getNumChildren(); index++)
            returnString.append(", " + toStringRec(currentNode.getChild(index)));
        if (currentNode.getNumChildren() > 0) {
            returnString.insert(0, '(');
            returnString.append(')');
        }
        return returnString;
    }

    /**
     * (Helper function) Gets a state node from the state tree.
     *
     * @param stateName is the name of the state to get.
     * @return the appropriate state is returned.  If it is not found then null
     * is sent back.
     */

    private StateNode getStateNode(String stateName) {
        if (!containsState(stateName))
            throw new IllegalArgumentException("ERROR: State not found.");
        return getStateNodeRec(root, stateName);
    }

    /**
     * (Helper Function) Recursive method to get the state node from the state
     * tree.
     *
     * @param currentNode is the current node that is being analyzed.
     * @param stateName is the name of the state to get.
     * @return the appropriate state is returned.  If it is not found then null
     * is sent back.
     */

    private StateNode getStateNodeRec(StateNode currentNode, String stateName) {
        StateNode returnNode;

        if (currentNode.getName().equalsIgnoreCase(stateName))
            return currentNode;

        for (int index = 0; index < currentNode.getNumChildren(); index++) {
            returnNode = getStateNodeRec(currentNode.getChild(index), stateName);
            if (returnNode != null)
                return returnNode;
        }
        return null;
    }

    /**
     * (Helper Function) Checks to see if the state tree contains a certain state.
     *
     * @param stateName the name of the state to check.
     * @return true if the current state is within the scope of the target state.
     */

    private boolean containsState(String stateName) {
        return containsStateRec(root, stateName);
    }

    /**
     * (Helper Function) Recursive method to check to see if the state tree
     * contains a certain state.
     *
     * @param currentNode is the node being analyzed.
     * @param stateName is the name of the state to check.
     * @return true if the current state is within the scope of the target state.
     */

    private boolean containsStateRec(StateNode currentNode, String stateName) {
        if (currentNode.getName().equalsIgnoreCase(stateName))
            return true;

        for (int index = 0; index < currentNode.getNumChildren(); index++) {
            if (containsStateRec(currentNode.getChild(index), stateName))
                return true;
        }
        return false;
    }

    /**
     * This class models a node of the state tree.  It has a name and a Vector of
     * children.
     */

    private class StateNode {
        private String name;
        private Vector<StateNode> children;

        /**
         * Constructor for the StateNode class.  It initializes the name of the
         * state to a given parameter.
         *
         * @param name the name of the state.
         */

        public StateNode(String name) {
            this.name = name;
            children = new Vector<StateNode>();
        }

        /**
         * This method returns the name of the state.
         *
         * @return the name of the string is returned.
         */

        public String getName() {
            return name;
        }

        /**
         * This method returns a certain child.
         *
         * @param index is the number of the child to return.
         * @return the appropriate child is returned.
         */

        public StateNode getChild(int index) {
            if (index >= children.size() || index < 0)
                throw new IllegalArgumentException("ERROR: Invalid child number.");

            return children.get(index);
        }

        /**
         * This method returns how many children a node has.
         *
         * @return the number of children are returned.
         */

        public int getNumChildren() {
            return children.size();
        }

        /**
         * This method adds a child to the node.
         *
         * @param name is the name of the child to add.
         */

        public void addChild(String name) {
            children.add(new StateNode(name));
        }
    }
}
