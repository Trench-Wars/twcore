package twcore.bots.purepubbot.pubitem;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import twcore.bots.purepubbot.ItemObserver;
import twcore.core.util.Tools;

public class WeaselHouse
        extends PubItem {

    public WeaselHouse(ItemObserver bot, String name, int itemNumber, int price) {
        super(bot, name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.observers = new LinkedList<ItemObserver>();
        registerObserver(bot);
    }

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        for(final ItemObserver io : observers){
            io.doSetCmd(playerName, "6 1");
            TimerTask disableAfterFiveMinutes = new TimerTask(){
                public void run(){
                    io.doSetCmd("", "6 0");
                }
            };
            Timer timer = new Timer();
            timer.schedule(disableAfterFiveMinutes, 5*Tools.TimeInMillis.MINUTE);
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
