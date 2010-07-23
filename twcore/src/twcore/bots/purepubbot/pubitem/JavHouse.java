package twcore.bots.purepubbot.pubitem;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.purepubbot.ItemObserver;
import twcore.core.util.Tools;

public class JavHouse
        extends PubItem {

    public JavHouse(ItemObserver bot, String name, int itemNumber, int price) {
        super(bot, name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.observers = new Vector<ItemObserver>();
        registerObserver(bot);
    }

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        for(final ItemObserver io:observers){
            io.doSetCmd(playerName, "!set 2:1"); //enables jav
            TimerTask disableAfterFiveMinutes = new TimerTask(){
                public void run(){
                    io.doSetCmd("", "!set 2:0"); //disables back after the time runs out
                }
            };
            Timer timer = new Timer(); //sets timer to be run after 5 mins so it'll be disabled.
            timer.schedule(disableAfterFiveMinutes, 5*Tools.TimeInMillis.MINUTE);
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

}
