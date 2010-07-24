package twcore.bots.purepubbot.pubstore;

import java.util.HashMap;
import java.util.Map;

import twcore.bots.purepubbot.ItemObserver;
import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.Burst;
import twcore.bots.purepubbot.pubitem.FullCharge;
import twcore.bots.purepubbot.pubitem.JavHouse;
import twcore.bots.purepubbot.pubitem.JavHouse2;
import twcore.bots.purepubbot.pubitem.Javelin;
import twcore.bots.purepubbot.pubitem.JavelinHouseOff;
import twcore.bots.purepubbot.pubitem.LeviHouse;
import twcore.bots.purepubbot.pubitem.LeviHouse2;
import twcore.bots.purepubbot.pubitem.LeviHouseOff;
import twcore.bots.purepubbot.pubitem.Leviathan;
import twcore.bots.purepubbot.pubitem.PubItem;
import twcore.bots.purepubbot.pubitem.Repel;
import twcore.bots.purepubbot.pubitem.Rocket;
import twcore.bots.purepubbot.pubitem.Shield;
import twcore.bots.purepubbot.pubitem.Shrapnel;
import twcore.bots.purepubbot.pubitem.Super;
import twcore.bots.purepubbot.pubitem.WarbirdHouseOff;
import twcore.bots.purepubbot.pubitem.Warp;
import twcore.bots.purepubbot.pubitem.Weasel;
import twcore.bots.purepubbot.pubitem.WeaselHouse;
import twcore.core.BotAction;

public class PubStore {

    private Map<String, PubSpecificStore> hashSpecificStores;
    private BotAction botAction;
    
    public PubStore(ItemObserver bot){
        this.hashSpecificStores = new HashMap<String, PubSpecificStore>();

        PubItem itemsubject1 = new Leviathan(bot, "levi", 0, 250);
        PubItem itemsubject2 = new Javelin(bot, "jav", 0, 100);
        PubItem itemsubject3 = new Weasel(bot, "weasel", 0, 100);
        PubItem itemsubject4 = new JavHouse(bot, "javelin", 0, 250);
        PubItem itemsubject5 = new LeviHouse(bot, "leviathan", 0, 0);
        PubItem itemsubject6 = new WeaselHouse(bot, "weasel2", 0, 200);
        PubItem itemsubject7 = new LeviHouse2(bot, "leviathan2", 0, 700);
        PubItem itemsubject8 = new JavHouse2(bot, "javelin2", 0, 600);
        PubItem itemsubject9 = new LeviHouseOff(bot, "leviathan3", 0, 50);
        PubItem itemsubject10 = new JavelinHouseOff(bot, "javelin3", 0, 50);
        PubItem itemsubject11 = new WarbirdHouseOff(bot, "warbirdoff", 0, 500);
        PubItem item1 = new Repel("repel", 21, 50);
        PubItem item2 = new Super("super", 17, 5000);
        PubItem item3 = new Rocket("rocket", 27, 50);
        PubItem item4 = new Burst("burst", 22, 250);
        PubItem item5 = new Shield("shield", 18, 5000);
        PubItem item6 = new FullCharge("fullcharge", 13, 20);
        PubItem item7 = new Warp("warp", 7, 10);
        PubItem item8 = new Shrapnel("shrap", 19, 30);
        
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
        PubSpecificStore specificStore14 = new PubStoreShrapnel(item8);
        PubSpecificStore specificStore15 = new PubStoreLeviathan(itemsubject7);
        PubSpecificStore specificStore16 = new PubStoreJav(itemsubject8);
        PubSpecificStore specificStore17 = new PubStoreLeviathan(itemsubject9);
        PubSpecificStore specificStore18 = new PubStoreJav(itemsubject10);
        PubSpecificStore specificStore19 = new PubWarbirdStore(itemsubject11);
        //more specifis stores
        
        hashSpecificStores.put("1", specificStore1); //repel
        hashSpecificStores.put("9", specificStore2); //super
        hashSpecificStores.put("2", specificStore3); //rocket
        hashSpecificStores.put("levi", specificStore4);
        hashSpecificStores.put("jav", specificStore5);
        hashSpecificStores.put("weasel", specificStore6);
        hashSpecificStores.put("3", specificStore7); //burst
        hashSpecificStores.put("8", specificStore8); //shield
        hashSpecificStores.put("7", specificStore9); //fullcharge
        hashSpecificStores.put("6", specificStore10); //warp
        hashSpecificStores.put("jav5", specificStore11); //enable jav arena 5 mins
        hashSpecificStores.put("levi5", specificStore12);  //enable levi arena 5 mins
        hashSpecificStores.put("wea5", specificStore13); //enable weasel arena 5 mins
        hashSpecificStores.put("5", specificStore14);
        hashSpecificStores.put("levi10", specificStore15);//enable levi arena 10 mins
        hashSpecificStores.put("jav10", specificStore16); //enable jav arena 10 mins
        hashSpecificStores.put("11", specificStore17); //disable levi 10 mins
        hashSpecificStores.put("10", specificStore18); //disable jav 10 mins
                                                        //disable weasel 10 mins shud be 13
        hashSpecificStores.put("13", specificStore19); //disable warbird 5 mins
    }
    
    public PubPlayer prizeItem(String itemName, PubPlayer player, int shipType){
       
            try{
               // if(player.hasNotReachedLimit()){
                    
                    //itemName = lookAltItemNames(itemName);
                    
                    PubSpecificStore pubStore = hashSpecificStores.get(itemName);
                    
                    PubItem item = pubStore.sellItem(player, shipType);
                    //System.out.println("I'm here, item: "+item.getName());
                    player.setPoint(player.getPoint() - item.getPrice());
                    player.addItemString(item.toString());
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
