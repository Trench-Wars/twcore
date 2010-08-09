package twcore.bots.purepubbot.moneysystem.item;

import twcore.bots.purepubbot.PubException;
import twcore.bots.purepubbot.moneysystem.PubPlayer;


public abstract class PubItem {
    
    protected String name;
    protected String displayName;
    protected int price;
    protected boolean arenaItem;
    protected PubItemRestriction restriction;
    protected long lastTimeUsed = 0;

    public PubItem(String name, String displayName, int price) {
        this.name = name;
        this.displayName = displayName;
        this.price = price;
        this.arenaItem = false;
    }

    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
    	return displayName;
    }
    
    public int getPrice() {
        return price;
    }
    
    public void setArenaItem(boolean b) {
    	this.arenaItem = b;
    }

	public void setRestriction(PubItemRestriction r) {
		this.restriction = r;
	}
	
	public boolean isRestricted() {
		return restriction != null;
	}
	
	public boolean isArenaItem() {
		return arenaItem;
	}
	
	public void checkRestriction(PubPlayer player, int shipType) throws PubException {
		if (restriction != null)
			restriction.check(this, player, shipType);
	}
	
	public void hasBeenBought() {
		this.lastTimeUsed = System.currentTimeMillis();
	}
	
	public long getLastTimeUsed() {
		return lastTimeUsed;
	}

}
