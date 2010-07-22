package twcore.bots.purepubbot.pubstore;

import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.PubItem;
import twcore.bots.purepubbot.pubitem.PubItemSubject;

public class PubStoreJav
        extends PubSpecificStore {

    public PubStoreJav(PubItemSubject item) {
        super((PubItem)item); //downcast
        // TODO Auto-generated constructor stub
    }

    @Override
    protected boolean checkEnoughMoney(PubPlayer player) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean checkShipType(int shipType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected PubItem getItem() {
        // TODO Auto-generated method stub
        return null;
    }

}
