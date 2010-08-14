package twcore.bots.purepubbot.moneysystem;

import java.util.LinkedList;
import java.util.List;

import twcore.bots.purepubbot.moneysystem.item.PubItem;
import twcore.bots.purepubbot.moneysystem.item.PubShipItem;
import twcore.core.BotAction;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerDeath;


public class PubPlayer implements Comparable<PubPlayer>{
    
	private static final int MAX_ITEM_HISTORY = 25;
	
	private BotAction m_botAction;
	private int playerID; // As seen by BotAction
	
    private String name;
    private int money;
    private LinkedList<PubItem> itemsBought;
    private LinkedList<PubItem> itemsBoughtThisLife;
    
    private LvzMoneyPanel cashPanel;
    
    // A player can only have 1 ShipItem at a time
    private PubShipItem shipItem;
    private int deathsOnShipItem = 0;
    
    // Epoch time
    private long lastMoneyUpdate = 0;
    private long lastSavedState = 0;

    public PubPlayer(BotAction m_botAction, String name) {
    	this(m_botAction, name, 0);
    }
    
    public PubPlayer(BotAction m_botAction, String name, int money) {
    	this.m_botAction = m_botAction;
        this.name = name;
        this.money = money;
        this.itemsBought = new LinkedList<PubItem>();
        this.itemsBoughtThisLife = new LinkedList<PubItem>();
        this.cashPanel = new LvzMoneyPanel(m_botAction);
        reloadPanel();
    }


    public String getPlayerName() {
        return name;
    }

    public int getMoney() {
        return money;
    }
    
    public void reloadPanel() {
    	cashPanel.reset(name, money);
    	cashPanel.update(m_botAction.getPlayerID(name), String.valueOf(0), String.valueOf(money), true);
    }
    
    public void setMoney(int money) {
    	int before = this.money;
        this.money = money;
        boolean gained = before > money ? false : true;
        cashPanel.update(m_botAction.getPlayerID(name), String.valueOf(before), String.valueOf(money), gained);
        this.lastMoneyUpdate = System.currentTimeMillis();
    }
    
    public void addMoney(int money) {
    	setMoney(this.money+money);
    }
    
    public void removeMoney(int money) {
    	setMoney(this.money-money);
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
    
    public void savedState() {
    	this.lastSavedState = System.currentTimeMillis();
    }
    
    public boolean isOnSpec() {
    	return ((int)m_botAction.getPlayer(name).getShipType()) == 0;
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
    
    public long getLastMoneyUpdate() {
    	return lastMoneyUpdate;
    }
    
    public long getLastSavedState() {
    	return lastSavedState;
    }
    
    @Override
    public int compareTo(PubPlayer o) {
        // TODO Auto-generated method stub
        if(o.getMoney() > getMoney()) return 1;
        if(o.getMoney() < getMoney()) return 0;
        
        return -1;
    }

}
