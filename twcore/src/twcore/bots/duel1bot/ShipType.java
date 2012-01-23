package twcore.bots.duel1bot;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ShipType {
    
    SPEC, WARBIRD, JAVELIN, SPIDER, LEVIATHAN, TERRIER, LANCASTER, SHARK;
    
    private static final Map<Integer, ShipType> lookup = new HashMap<Integer, ShipType>();
    
    static {
        for (ShipType s : EnumSet.allOf(ShipType.class))
            lookup.put(s.getNum(), s);
    }
    
    public static ShipType type(int n) {
        return lookup.get(n);
    }
    
    public int getNum() {
        return this.ordinal();
    }
    
    public boolean hasShrap() {
        return (this.ordinal() == 8 || this.ordinal() == 2);
    }
}
