package twcore.bots.purepubbot.pubsystemstate;

import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pointlocation.AreaPlayer;
import twcore.bots.purepubbot.pubstore.PubStore;
import twcore.core.util.Point;

//State pattern to make the system on/off

public abstract class PubPointStore {

    protected PubStore facadeStore;
    protected AreaPlayer facadePoints;
    public abstract String getLocation(Point point) throws SystemIsOffException; //won't calculate points if system is off state
    public abstract PubPlayer buyItem(String itemName, PubPlayer player, int shipType) throws SystemIsOffException;

}
