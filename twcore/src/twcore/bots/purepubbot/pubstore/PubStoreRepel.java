package twcore.bots.purepubbot.pubstore;

import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.PubItem;

public class PubStoreRepel
        extends PubSpecificStore {

    public PubStoreRepel(PubItem item) {
        super(item);
        // TODO Auto-generated constructor stub
    }


    @Override
    protected PubItem getItem() {
        // TODO Auto-generated method stub
        return item;
    }


    @Override
    protected boolean checkEnoughMoney(PubPlayer player) {
        // TODO Auto-generated method stub
        System.out.println("Buying repel for Player "+player.getP_name()+" cash: "+player.getPoint());
        return player.getPoint() >= this.item.getPrice();
    }


    @Override
    protected boolean checkShipType(int shipType) {
        // TODO Auto-generated method stub
        return shipType == 8 || shipType == 4 || shipType == 6;
    }
    
    public String toString(){
        return "I'm a Pub Store Repel";
    }
 
}
