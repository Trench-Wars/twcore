package twcore.bots.purepubbot.pubsystemstate;

import java.util.List;
import java.util.Vector;

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
        this.displayItems = new Vector<String>();
        insertItems();
    }
    
    private void insertItems() {
        // TODO Auto-generated method stub
        displayItems.add("(!b 1) repel - $50");
        displayItems.add("(!b 2) rocket - $50");
        displayItems.add("(!b 3) burst - $250");
        displayItems.add("(!b 4) portal - price ?");
        displayItems.add("(!b 5) shrapnel - $30");
        displayItems.add("(!b 6) warp - $10");
        displayItems.add("(!b 7) fullcharge - $20");
        displayItems.add("(!b 8) shield - $5000");
        displayItems.add("(!b 9) super - $5000");
        displayItems.add("---- 1 life items ------");
        displayItems.add("!b levi $250");
        displayItems.add("!b jav $200");
        displayItems.add("!b weasel $100");
        displayItems.add("---- arena items  ------");
        displayItems.add("!b levi10 - $700 - levi for 10 mins");
        displayItems.add("!b jav10 - $600 - jav for 10 mins");
        displayItems.add("!b wea10 - $500 - weasel for 10 mins");
        //displayItems.add("!b wb10 - no wb for 10 mins");
        displayItems.add("!b levi5 - $400 - levi for 5 mins");
        displayItems.add("!b jav5 - $250 - jav for 5 mins");
        displayItems.add("!b wea5 - $200 - weasel for 5 mins");
        displayItems.add("!b wb5 - $500 - no wb for 5 mins");
        //displayItems.add("!b wb10 - no wb for 10 mins");
        //displayItems.add("!b wb10 - no wb for 10 mins");
     
    }

    public List<String> displayAvailableItems(){
        return displayItems;
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
