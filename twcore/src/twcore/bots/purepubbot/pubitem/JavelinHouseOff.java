package twcore.bots.purepubbot.pubitem;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.purepubbot.ItemObserver;
import twcore.core.util.Tools;

public class JavelinHouseOff
        extends PubItem {

    public JavelinHouseOff(ItemObserver bot, String name, int itemNumber,
            int price) {
        super(bot, name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.observers = new Vector<ItemObserver>();
        registerObserver(bot);
    }

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        for(final ItemObserver io:observers){
            io.update(playerName, "2 0", 10);
            TimerTask enableAfterTenMinutes = new TimerTask(){
                public void run(){
                    io.doSetCmd("", "2 1");
                }
            };
            Timer timer = new Timer();
            timer.schedule(enableAfterTenMinutes, 10*Tools.TimeInMillis.MINUTE);
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
