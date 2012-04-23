package twcore.bots.duel1bot;

import java.util.HashSet;

/**
 * Short for 'Case Insensitive String Hash Set'
 *
 * @author WingZero
 */
public class CinSet extends HashSet<String> {

    private static final long serialVersionUID = 1L;
    
    public CinSet() {
        super();
    }
    
    public CinSet(int capacity) {
        super(capacity);
    }
    
    public CinSet(int capacity, float load) {
        super(capacity, load);
    }
    
    @Override
    public boolean add(String str) {
        return super.add(low(str));
    }
    
    @Override
    public boolean contains(Object str) {
        return super.contains(low((String) str));
    }
    
    @Override
    public boolean remove(Object str) {
        return super.remove(low((String) str));
    }
    
    private String low(String str) {
        return str.toLowerCase();
    }

}
