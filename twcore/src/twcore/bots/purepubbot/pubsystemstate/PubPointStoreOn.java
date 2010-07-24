package twcore.bots.purepubbot.pubsystemstate;

import twcore.bots.purepubbot.ItemObserver;
import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pointlocation.AreaPlayer;
import twcore.bots.purepubbot.pubstore.PubStore;
import twcore.core.util.Point;

public class PubPointStoreOn
        extends PubPointStore {

    public PubPointStoreOn(ItemObserver bot){
        this.facadeStore = new PubStore(bot);
        this.facadePoints = new AreaPlayer();
    }
    @Override
    public PubPlayer buyItem(String itemName, PubPlayer player, int shipType) {
        // TODO Auto-generated method stub
        return facadeStore.prizeItem(itemName, player, shipType);
    }

    @Override
    public String getLocation(Point point) {
        // TODO Auto-generated method stub
        return facadePoints.getLocation(point);
    }

}
