package twcore.bots.purepubbot.pubitem;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import twcore.bots.purepubbot.ItemObserver;
import twcore.core.util.Tools;

public class LeviHouse
        extends PubItem {

    public LeviHouse(ItemObserver bot, String name, int itemNumber, int price) {
        super(bot, name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.observers = new ArrayList<ItemObserver>();
        registerObserver(bot);
        this.isArenaItem = true;
    }

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        for(final ItemObserver io:observers){
            io.update(playerName, "4 1", 5);
            /*
            io.doSetCmd(playerName, "4 1");
            */
            TimerTask disableAfterFiveMinutes = new TimerTask(){
                public void run(){
                    io.doSetCmd("", "4 0");
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
    
    public String toString(){
        return "Leviathan($+"+this.price+") for 5 minutes";
    }
    
    @Override
    public boolean isArenaItem(){
        return isArenaItem;
    }
}
