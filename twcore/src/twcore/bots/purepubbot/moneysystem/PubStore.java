package twcore.bots.purepubbot.moneysystem;

import java.util.LinkedHashMap;

import twcore.bots.purepubbot.PubException;
import twcore.bots.purepubbot.moneysystem.item.PubItem;
import twcore.bots.purepubbot.moneysystem.item.PubItemRestriction;

public class PubStore {

    private LinkedHashMap<String, PubItem> items;
    
    private boolean opened = true;

    public PubStore(){
        this.items = new LinkedHashMap<String, PubItem>();
    }
    
    public PubItem buy(String itemName, PubPlayer player, int shipType) throws PubException {

    	if (!opened)
    		throw new PubException("The store is closed!");
    	
        PubItem item = items.get(itemName);

        if (item == null)
        	throw new PubException("This item does not exist.");
        
        if (item.isRestricted()) {
        	PubItemRestriction restriction = item.getRestriction();
        	restriction.check(item, player, shipType);
        }

        if (player.getMoney() < item.getPrice())
        	throw new PubException("You do not have enough money to buy this item.");
        
        if (item != null) {
	        player.setMoney(player.getMoney() - item.getPrice());
	        player.addItem(item);
        }
        
        item.hasBeenBought();

        return item;
    }
    
    public void addItem(PubItem item, String itemName) {
    	items.put(itemName, item);
    	for(String abbv: item.getAbbreviations()) {
    		items.put(abbv, item);
    	}
    }
    
    public LinkedHashMap<String, PubItem> getItems() {
    	return items;
    }
    
    public void turnOn() {
    	this.opened = true;
    }
    
    public void turnOff() {
    	this.opened = false;
    }
    
    public boolean isOpened() {
    	return opened;
    }
}
