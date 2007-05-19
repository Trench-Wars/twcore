package twcore.bots.sbbot;

public class Communicator {
    private final Operator operator;

    Communicator(Operator op) {
	assert( op != null );
	operator = op;
    }

    public Operator getOperator() { return operator; }
}
