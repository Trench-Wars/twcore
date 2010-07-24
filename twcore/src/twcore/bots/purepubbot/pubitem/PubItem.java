package twcore.bots.purepubbot.pubitem;

import twcore.bots.purepubbot.ItemObserver;

public abstract class PubItem extends PubItemSubject{
    
    protected String name;
    protected int itemNumber;
    protected int price;
    protected boolean isArenaItem;
    
    public PubItem(ItemObserver bot, String name, int itemNumber, int price){
        this.name = name;
        this.itemNumber = itemNumber;
        this.price = price;
    }
    
    public PubItem(String name, int itemNumber, int price) {
        this.name = name;
        this.itemNumber = itemNumber;
        this.price = price;
    }

    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getItemNumber() {
        return itemNumber;
    }
    public void setItemNumber(int itemNumber) {
        this.itemNumber = itemNumber;
    }
    public int getPrice() {
        return price;
    }
    public void setPrice(int price) {
        this.price = price;
    }

    public String toString(){
        return "You've bought a "+this.getName()+" for $"+this.getPrice();
    }

    public boolean isArenaItem(){
        return isArenaItem;
    }
}
