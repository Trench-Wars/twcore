package twcore.bots.welcomebot;

import java.util.Vector;

public class QueueSet extends Vector<String> {
    private static final long serialVersionUID = 1L;
    
    public String pop() {
        if (size() > 0)
            return super.remove(0);
        else
            return null;
    }
    
    public boolean push(String s) {
        if (!super.contains(s)) {
            super.add(s.toLowerCase());
            return true;
        } else return false;
    }

}
