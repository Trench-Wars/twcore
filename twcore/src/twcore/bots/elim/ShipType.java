package twcore.bots.elim;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ShipType {
    WARBIRD(1, 0),
    JAVELIN(2, 1),
    SPIDER(3, 0),
    LEVIATHAN(4, 0),
    TERRIER(5, 1),
    WEASEL(6, 1),
    LANCASTER(7, 0),
    SHARK(8, 1);

    private int ship, freq;

    private static final Map<Integer, ShipType> lookup = new HashMap<Integer, ShipType>();

    static {
        for (ShipType s : EnumSet.allOf(ShipType.class))
            lookup.put(s.getNum(), s);
    }

    private ShipType(int num, int startFreq) {
        ship = num;
        freq = startFreq;
    }

    public static ShipType type(int n) {
        return lookup.get(n);
    }

    public int getNum() {
        return ship;
    }

    public boolean hasShrap() {
        return (ship == 8 || ship == 2);
    }

    public int getFreq() {
        return freq;
    }

    public boolean inBase() {
        if (freq == 0)
            return false;
        else
            return true;
    }

    public String getType() {
        if (inBase())
            return "BASEELIM";
        else
            return "ELIM";
    }
}
