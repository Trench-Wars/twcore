package twcore.bots.purepubbot.pubstore;

import java.util.HashMap;
import java.util.Map;

import twcore.bots.purepubbot.ItemObserver;
import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.Burst;
import twcore.bots.purepubbot.pubitem.FullCharge;
import twcore.bots.purepubbot.pubitem.JavHouse;
import twcore.bots.purepubbot.pubitem.Javelin;
import twcore.bots.purepubbot.pubitem.LeviHouse;
import twcore.bots.purepubbot.pubitem.Leviathan;
import twcore.bots.purepubbot.pubitem.PubItem;
import twcore.bots.purepubbot.pubitem.Repel;
import twcore.bots.purepubbot.pubitem.Rocket;
import twcore.bots.purepubbot.pubitem.Shield;
import twcore.bots.purepubbot.pubitem.Super;
import twcore.bots.purepubbot.pubitem.Warp;
import twcore.bots.purepubbot.pubitem.Weasel;
import twcore.bots.purepubbot.pubitem.WeaselHouse;
import twcore.core.BotAction;

public class PubStore {

    private Map<String, PubSpecificStore> hashSpecificStores;
    private BotAction botAction;
    
    public PubStore(BotAction botAction, ItemObserver bot){
        this.botAction = botAction;
        this.hashSpecificStores = new HashMap<String, PubSpecificStore>();

        PubItem itemsubject1 = new Leviathan(bot, "levi", 0, 10);
        PubItem itemsubject2 = new Javelin(bot, "jav", 0, 100);
        PubItem itemsubject3 = new Weasel(bot, "weasel", 0, 30);
        PubItem itemsubject4 = new JavHouse(bot, "javelin", 0, 50);
        PubItem itemsubject5 = new LeviHouse(bot, "leviathan", 0, 50);
        PubItem itemsubject6 = new WeaselHouse(bot, "weasel2", 0, 50);
        PubItem item1 = new Repel("repel", 21, 50);
        PubItem item2 = new Super("super", 17, 5000);
        PubItem item3 = new Rocket("rocket", 27, 50);
        PubItem item4 = new Burst("burst", 22, 50);
        PubItem item5 = new Shield("shield", 18, 5000);
        PubItem item6 = new FullCharge("fullcharge", 13, 20);
        PubItem item7 = new Warp("warp", 7, 10);
        //PubItem item4 = new Leviathan("levi", 0, 250);
        //more items
        
        PubSpecificStore specificStore1 = new PubStoreRepel(item1);
        PubSpecificStore specificStore2 = new PubStoreSuper(item2);
        PubSpecificStore specificStore3 = new PubStoreRocket(item3);
        PubSpecificStore specificStore7 = new PubStoreBurst(item4);
        PubSpecificStore specificStore4 = new PubStoreLeviathan(itemsubject1);
        PubSpecificStore specificStore5 = new PubStoreJav(itemsubject2);
        PubSpecificStore specificStore6 = new PubStoreWeasel(itemsubject3);
        PubSpecificStore specificStore8 = new PubStoreShield(item5);
        PubSpecificStore specificStore9 = new PubStoreFullCharge(item6);
        PubSpecificStore specificStore10 = new PubStoreWarp(item7);
        PubSpecificStore specificStore11 = new PubStoreJav(itemsubject4);
        PubSpecificStore specificStore12 = new PubStoreLeviathan(itemsubject5);
        PubSpecificStore specificStore13 = new PubStoreWeasel(itemsubject6);
        //more specifis stores
        
        hashSpecificStores.put("repel", specificStore1);
        hashSpecificStores.put("super", specificStore2);
        hashSpecificStores.put("rocket", specificStore3);
        hashSpecificStores.put("levi", specificStore4);
        hashSpecificStores.put("jav", specificStore5);
        hashSpecificStores.put("weasel", specificStore6);
        hashSpecificStores.put("burst", specificStore7);
        hashSpecificStores.put("shield", specificStore8);
        hashSpecificStores.put("fullcharge", specificStore9);
        hashSpecificStores.put("warp", specificStore10);
        hashSpecificStores.put("javelin", specificStore11);
        hashSpecificStores.put("leviathan", specificStore12);
        hashSpecificStores.put("weasel2", specificStore13);
    }
    
    public PubPlayer prizeItem(String itemName, PubPlayer player, int shipType){
       
            try{
               // if(player.hasNotReachedLimit()){
                    
                    //itemName = lookAltItemNames(itemName);
                    
                    PubSpecificStore pubStore = hashSpecificStores.get(itemName);
                    
                    PubItem item = pubStore.sellItem(player, shipType);
                    System.out.println("I'm here, item: "+item.getName());
                    player.setPoint(player.getPoint() - item.getPrice());
                    player.addItemString(item.toString());
                    botAction.specificPrize(player.getP_name(), item.getItemNumber());
                    item.notifyObservers(player.getP_name());
                    
                    return player;
               // }
            }catch(Exception e){
                e.printStackTrace();
            }
        
        return null;
    }
    
    private String lookAltItemNames(String item){
        if(item.startsWith("rep"))
            return "repel";
        if(item.startsWith("r"))
            return "repel";
        if(item.startsWith("sup"))
            return "super";
        if(item.startsWith("levi"))
            return "levi";
        return "";
    }
}
