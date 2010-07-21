package twcore.bots.purepubbot.pubitem;

public abstract class PubItem {
    
    private String name;
    private int itemNumber;
    private int price;
    
    public PubItem(String name, int itemNumber, int price){
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


}
