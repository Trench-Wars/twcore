package twcore.bots.purepubbot.moneysystem;

import java.util.ArrayList;
import java.util.List;

import twcore.bots.purepubbot.PubException;

public class PubItemRestriction {

	private List<Integer> ships;
	private int maxPerLife = -1;
	private int maxConsecutive = -1;
	
	public PubItemRestriction() {
		ships = new ArrayList<Integer>();
	}
	
	public void addShip(int shipType) {
		ships.add(shipType);
	}
	
	public void setMaxPerLife(int max) {
		this.maxPerLife = max;
	}
	
	public void setMaxConsecutive(int max) {
		this.maxConsecutive = max;
	}
	
	public void check(PubItem item, PubPlayer player, int shipType) throws PubException {
		
		if (ships.contains(shipType))
			throw new PubException("You cannot buy this item with your current ship.");
		
		if (maxPerLife!=-1) {
			List<PubItem> items = player.getItemsBoughtThisLife();
			int count = 1;
			for(int i=0; i<items.size(); i++) {
				if (items.get(items.size()-1-i)==item)
					count++;
				if (count>maxPerLife)
					throw new PubException("This item is limited to " + maxPerLife + " per life.");
			}
		}
		
		if (maxConsecutive!=-1) {
			List<PubItem> items = player.getItemsBoughtThisLife();
			int count = 0;
			for(int i=0; i<Math.min(items.size(), maxConsecutive); i++) {
				if (items.get(items.size()-1-i)==item)
					count++;
				else if (count<maxConsecutive)
					break;
				if (count>=maxConsecutive)
					throw new PubException("Only " + maxConsecutive + " consecutive buy of this item allowed.");
			}
		}

	}
	
}
