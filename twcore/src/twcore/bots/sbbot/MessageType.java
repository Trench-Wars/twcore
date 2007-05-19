package twcore.bots.sbbot;

/**
 * This class doesn't really have to do anything.  I'm just using it instead of ints
 * to prevent possible accidental collisions.  Direct Object/Object comparison is actually
 * an int comparison (compares pointers), so the operation should be sufficiently fast while
 * guaranteeing each event type is unique.  Subclasses are useful for associating a given call
 * with the correct subclass of Operator (e.g., a SSEventMessage will resolve an addListener
 * call to SSEventOperator.addListener(SSEventMessageType,SSEventListener) rather than
 * Operator.addListener(MessageType,Message);
 */
public class MessageType {
    Class C;
    public MessageType() {
	C = this.getClass();
    }

    public MessageType(Class c) {
	C = c;
    }

    Class getMessageClass() { return C; }
}