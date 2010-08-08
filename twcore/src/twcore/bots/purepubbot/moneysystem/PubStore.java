package twcore.bots.purepubbot.moneysystem;

import java.util.HashMap;
import java.util.Map;

import twcore.bots.purepubbot.PubException;

public class PubStore {

    private Map<String, PubItem> items;
    
    private boolean open = true;

    public PubStore(){
        this.items = new HashMap<String, PubItem>();
    }
    
    public PubItem buy(String itemName, PubPlayer player, int shipType) throws PubException {

    	if (!open)
    		throw new PubException("The store is closed!");
    	
        PubItem item = items.get(itemName);

        if (item == null)
        	throw new PubException("This item does not exist.");
        
        if (item.isRestricted())
        	item.checkRestriction(player, shipType); // will throw PubException
        
        if (player.getPoint() < item.getPrice())
        	throw new PubException("You do not have enough money to buy this item.");
        
        if (item != null) {
	        player.setPoint(player.getPoint() - item.getPrice());
	        player.addItem(item);
        }

        return item;
    }
    
    public void addItem(PubItem item, String itemName) {
    	items.put(itemName, item);
    }
    
    public void turnOn() {
    	this.open = true;
    }
    
    public void turnOff() {
    	this.open = false;
    }
}
