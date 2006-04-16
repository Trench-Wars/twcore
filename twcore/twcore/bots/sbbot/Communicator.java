package twcore.bots.strikeballbot;
import java.util.*;
import twcore.core.*;

public class Communicator {
    private final Operator operator;
    
    Communicator(Operator op) {
	assert( op != null );
	operator = op;
    }

    public Operator getOperator() { return operator; }
}
