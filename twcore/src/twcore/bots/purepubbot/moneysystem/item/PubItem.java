package twcore.bots.purepubbot.moneysystem.item;

import twcore.bots.purepubbot.PubException;
import twcore.bots.purepubbot.moneysystem.PubPlayer;


public abstract class PubItem {
    
    protected String name;
    protected String displayName;
    protected int price;
    protected boolean arenaItem;
    protected PubItemDuration duration;
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
    
	public void setDuration(PubItemDuration d) {
		this.duration = d;
	}

	public void setRestriction(PubItemRestriction r) {
		this.restriction = r;
	}
	
	public boolean isRestricted() {
		return restriction != null;
	}
	
	public boolean hasDuration() {
		return duration != null;
	}
	
	public boolean isArenaItem() {
		return arenaItem;
	}
	
	public PubItemRestriction getRestriction() {
		return restriction;	
	}
	
	public PubItemDuration getDuration() {
		return duration;
	}
	
	public void hasBeenBought() {
		this.lastTimeUsed = System.currentTimeMillis();
	}
	
	public long getLastTimeUsed() {
		return lastTimeUsed;
	}

}
