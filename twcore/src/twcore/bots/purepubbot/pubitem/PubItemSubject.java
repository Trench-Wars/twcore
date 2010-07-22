package twcore.bots.purepubbot.pubitem;

import java.util.List;

import twcore.bots.purepubbot.ItemObserver;

public abstract class PubItemSubject {
    
  
    protected List<ItemObserver> observers;
    
    public abstract void registerObserver(ItemObserver bot);
    public abstract void removeObserver(ItemObserver bot);
    public abstract void notifyObservers(String playerName);
    


}
