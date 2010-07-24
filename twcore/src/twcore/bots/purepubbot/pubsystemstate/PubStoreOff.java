package twcore.bots.purepubbot.pubsystemstate;

import twcore.bots.purepubbot.PubPlayer;
import twcore.core.util.Point;

public class PubStoreOff
        extends PubPointStore {

    @Override
    public PubPlayer buyItem(String itemName, PubPlayer player, int shipType) throws SystemIsOffException {
        return null;
    }

    @Override
    public String getLocation(Point point) throws SystemIsOffException{
        return null;
    }

}
