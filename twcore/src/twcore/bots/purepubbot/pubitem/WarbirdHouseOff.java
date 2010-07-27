package twcore.bots.purepubbot.pubitem;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import twcore.bots.purepubbot.ItemObserver;
import twcore.core.util.Tools;

public class WarbirdHouseOff
        extends PubItem {

    public WarbirdHouseOff(ItemObserver bot, String name, int itemNumber,
            int price) {
        super(bot, name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.isArenaItem = true;
        this.observers = new ArrayList<ItemObserver>();
        registerObserver(bot);
    }

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        for(final ItemObserver io:observers){
            io.update(playerName, "1 0", 5);
            TimerTask enableAfterFiveMinutes = new TimerTask(){
                public void run(){
                    io.doSetCmd("", "1 1");
                }
            };
            Timer timer = new Timer();
            timer.schedule(enableAfterFiveMinutes, 5*Tools.TimeInMillis.MINUTE);
        }
    }

    @Override
    public void registerObserver(ItemObserver bot) {
        // TODO Auto-generated method stub
        observers.add(bot);
    }

    @Override
    public void removeObserver(ItemObserver bot) {
        // TODO Auto-generated method stub
        observers.remove(bot);
    }

    @Override
    public String toString(){
        return "Warbird OFF($"+this.price+") for 5 minutes";
    }
}
