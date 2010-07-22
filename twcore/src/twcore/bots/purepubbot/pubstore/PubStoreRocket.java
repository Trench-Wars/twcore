package twcore.bots.purepubbot.pubstore;

import twcore.bots.purepubbot.PubPlayer;
import twcore.bots.purepubbot.pubitem.PubItem;

public class PubStoreRocket
        extends PubSpecificStore {

    public PubStoreRocket(PubItem item) {
        super(item);
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
        return shipType == 2;
    }

    @Override
    protected PubItem getItem() {
        // TODO Auto-generated method stub
        return item;
    }

}
