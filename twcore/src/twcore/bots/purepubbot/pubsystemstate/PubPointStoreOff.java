package twcore.bots.purepubbot.pubsystemstate;

import java.util.List;
import java.util.Vector;

import twcore.bots.purepubbot.PubPlayer;
import twcore.core.util.Point;

public class PubPointStoreOff
        extends PubPointStore {

    public PubPointStoreOff(){
     
    }
    @Override
    public PubPlayer buyItem(String itemName, PubPlayer player, int shipType) throws RuntimeException {
        return null;
    }

    @Override
    public String getLocation(Point point) throws RuntimeException{
        return null;
    }
    @Override
    public List<String> displayAvailableItems() {
        // TODO Auto-generated method stub
        List list = new Vector<String>();
        list.add("There are no items, the store is off today!");
        return list;
    }

}
