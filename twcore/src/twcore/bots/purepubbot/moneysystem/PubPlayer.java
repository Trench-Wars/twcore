package twcore.bots.purepubbot.moneysystem;

import java.util.LinkedList;
import java.util.List;



public class PubPlayer implements Comparable<PubPlayer>{
    
	private static final int MAX_ITEM_HISTORY = 25;
	
    private String p_name;
    private int id;
    private int point;
    private LinkedList<PubItem> itemsBought;
    private LinkedList<PubItem> itemsBoughtThisLife;

    public PubPlayer(String name){
        p_name = name;
        point = 0;
        itemsBought = new LinkedList<PubItem>();
        itemsBoughtThisLife = new LinkedList<PubItem>();
    }

    public String getPlayerName() {
        return p_name;
    }
    public void setPlayerName(String pName) {
        p_name = pName;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getPoint() {
        return point;
    }
    public void setPoint(int point) {
        this.point = point;
    }

    public void addItem(PubItem item) {
    	if (itemsBought.size() > MAX_ITEM_HISTORY)
    		itemsBought.removeFirst();
        this.itemsBought.add(item);
        this.itemsBoughtThisLife.add(item);
    }
    
    public void resetItems() {
    	this.itemsBoughtThisLife.clear();
    }

    public List<PubItem> getItemsBought() {
    	return itemsBought;
    }
    
    public List<PubItem> getItemsBoughtThisLife() {
    	return itemsBoughtThisLife;
    }
    
    @Override
    public int compareTo(PubPlayer o) {
        // TODO Auto-generated method stub
        if(o.getPoint() > getPoint()) return 1;
        if(o.getPoint() < getPoint()) return 0;
        
        return -1;
    }

}
