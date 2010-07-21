package twcore.bots.purepubbot.pubstore;

import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.NullItem;
import twcore.bots.purepubbot.pubitem.PubItem;

public abstract class PubSpecificStore {

    protected PubItem item;
    
    public  PubSpecificStore(PubItem item){
        this.item = item;
    }
     
    public final PubItem sellItem(PubPlayer player, int shipType){
        
        if(!checkShipType(shipType))
            return new NullItem("null item", 0, 0, "Couldn't buy the item on your ship");
        if(!checkEnoughMoney(player)){
           return new NullItem("null item", 0, 0, "You do not have enough money");
        }
        return getItem();
    }
    
    protected abstract boolean checkShipType(int shipType);
    protected abstract boolean checkEnoughMoney(PubPlayer player);
    protected abstract PubItem getItem();
    
    public String toString(){
        return "I am a Specific Store";
    }
}
