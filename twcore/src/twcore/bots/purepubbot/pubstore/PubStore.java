package twcore.bots.purepubbot.pubstore;

import java.util.HashMap;
import java.util.Map;

import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.PubItem;
import twcore.bots.purepubbot.pubitem.Repel;
import twcore.bots.purepubbot.pubitem.Super;
import twcore.core.BotAction;

public class PubStore {

    private Map<String, PubSpecificStore> hashSpecificStores;
    private BotAction botAction;
    
    public PubStore(BotAction botAction){
        this.botAction = botAction;
        this.hashSpecificStores = new HashMap<String, PubSpecificStore>();

        PubItem item1 = new Repel("repel", 21, 50);
        PubItem item2 = new Super("super", 17, 5000);
        //more items
        
        PubSpecificStore specificStore1 = new PubStoreRepel(item1);
        PubSpecificStore specificStore2 = new PubStoreSuper(item2);
        //more specifis stores
        
        hashSpecificStores.put("repel", specificStore1);
        hashSpecificStores.put("super", specificStore2);
    }
    
    public PubPlayer prizeItem(String itemName, PubPlayer player, int shipType){
        
        try{
            
            itemName = lookAltItemNames(itemName);
         
            PubSpecificStore pubStore = hashSpecificStores.get(itemName);
            PubItem item = pubStore.sellItem(player, shipType);
      
            player.setPoint(player.getPoint() - item.getPrice());
            player.addItemString(item.toString());
            botAction.specificPrize(player.getP_name(), item.getItemNumber());
            return player;
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
        
        return "";
    }
}
