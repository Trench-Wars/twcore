package twcore.bots.purepubbot.pubitem;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.purepubbot.ItemObserver;
import twcore.core.util.Tools;

public class LeviHouse2
        extends PubItem {

    public LeviHouse2(ItemObserver bot, String name, int itemNumber, int price) {
        super(bot, name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.observers = new Vector<ItemObserver>();
        registerObserver(bot);
    }

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        for(final ItemObserver io:observers){
            io.update(playerName, "4 1", 10);
            TimerTask disableAfterTenMinutes = new TimerTask(){
                public void run(){
                    io.doSetCmd("", "4 0");
                }
            };
            Timer time = new Timer();
            time.schedule(disableAfterTenMinutes, 10*Tools.TimeInMillis.MINUTE);
        }
    }

    @Override
    public void registerObserver(ItemObserver bot) {
        // TODO Auto-generated method stub
        this.observers.add(bot);
    }

    @Override
    public void removeObserver(ItemObserver bot) {
        // TODO Auto-generated method stub
        this.observers.remove(bot);
    }

}
