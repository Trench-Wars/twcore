package twcore.bots.purepubbot.pubstore;

import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.PubItem;
import twcore.bots.purepubbot.pubitem.PubItemSubject;

public class PubStoreWeasel
        extends PubSpecificStore {

    public PubStoreWeasel(PubItemSubject item) {
        super((PubItem)item);//Downcast
        // TODO Auto-generated constructor stub
    }

    @Override
    protected boolean checkEnoughMoney(PubPlayer player) {
        // TODO Auto-generated method stub
        return player.getPoint() >= item.getPrice();
    }

    @Override
    protected boolean checkShipType(int shipType) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    protected PubItem getItem() {
        // TODO Auto-generated method stub
        return item;
    }

}
