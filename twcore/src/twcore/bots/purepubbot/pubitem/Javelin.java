package twcore.bots.purepubbot.pubitem;

import java.util.Vector;

import twcore.bots.purepubbot.ItemObserver;

public class Javelin
        extends PubItem {

    public Javelin(ItemObserver bot, String name, int itemNumber, int price) {
        super(name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.observers = new Vector<ItemObserver>();
        registerObserver(bot);
    }

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        for(ItemObserver io:observers)
            io.update(playerName);
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
