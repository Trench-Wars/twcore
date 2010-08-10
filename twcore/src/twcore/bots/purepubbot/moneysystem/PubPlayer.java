package twcore.bots.purepubbot.moneysystem;

import java.util.LinkedList;
import java.util.List;

import twcore.bots.purepubbot.moneysystem.item.PubItem;
import twcore.bots.purepubbot.moneysystem.item.PubShipItem;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerDeath;


public class PubPlayer implements Comparable<PubPlayer>{
    
	private static final int MAX_ITEM_HISTORY = 25;
	
    private String name;
    private int money;
    private LinkedList<PubItem> itemsBought;
    private LinkedList<PubItem> itemsBoughtThisLife;
    
    // A player can only have 1 ShipItem at a time
    private PubShipItem shipItem;
    private int deathsOnShipItem = 0;
    
    // Epoch time
    private long lastMoneyUpdate = 0;
    private long lastSavedState = 0;

    public PubPlayer(String name){
        this.name = name;
        this.money = 0;
        this.itemsBought = new LinkedList<PubItem>();
        this.itemsBoughtThisLife = new LinkedList<PubItem>();
        loadState();
    }
    
    private void loadState() {
    	// DB connection
    	// TODO
    }
    
    public void saveState() {
    	// DB connection
    	// TODO
    	this.lastSavedState = System.currentTimeMillis();
    }

    public String getPlayerName() {
        return name;
    }

    public int getMoney() {
        return money;
    }
    
    public void setMoney(int money) {
        this.money = money;
        this.lastMoneyUpdate = System.currentTimeMillis();
    }

    public void addItem(PubItem item) {
    	if (itemsBought.size() > MAX_ITEM_HISTORY)
    		itemsBought.removeFirst();
        this.itemsBought.add(item);
        this.itemsBoughtThisLife.add(item);
    }
    
    private void resetItems() {
    	this.itemsBoughtThisLife.clear();
    }

    public List<PubItem> getItemsBought() {
    	return itemsBought;
    }
    
    public List<PubItem> getItemsBoughtThisLife() {
    	return itemsBoughtThisLife;
    }
    
    public void resetShipItem() {
    	shipItem = null;
    	deathsOnShipItem = 0;
    }
    
    public void handleShipChange(FrequencyShipChange event) {
    	resetItems();
    	if (shipItem != null && event.getShipType() != shipItem.getShipNumber())
    		resetShipItem();
    }

    public void handleDeath(PlayerDeath event) {
    	resetItems();
    	if (shipItem != null) {
    		deathsOnShipItem++;
    		System.out.println("death: " + deathsOnShipItem);
    	}
    }
    
    public int getDeathsOnShipItem() {
    	return deathsOnShipItem;
    }
    
    public boolean hasShipItem() {
    	return shipItem != null;
    }
    
    public void setShipItem(PubShipItem item) {
    	this.shipItem = item;
    }
    
    @Override
    public int compareTo(PubPlayer o) {
        // TODO Auto-generated method stub
        if(o.getMoney() > getMoney()) return 1;
        if(o.getMoney() < getMoney()) return 0;
        
        return -1;
    }

}
