package twcore.bots.multibot.chaos;

public class ItemDatabase {
    private String name;
    private int price;
    private int exp;
    private boolean hasLimit;

    public ItemDatabase(){
    }
    
    public ItemDatabase( String newName, int newPrice, int newExp, boolean hasLimit ){
        this.name = newName;
        this.price = newPrice;
        this.exp = newExp;
        this.hasLimit = hasLimit;
    }
    
    public void setName( String newName ){
        this.name = newName;
    }

    public String getName(){
        return name;
    }

    public void setPrice( int newPrice ){
        this.price = newPrice;
    }

    public int getPrice(){
        return price;
    }
    
    public void setExp(int newExp){
        this.exp = newExp;
    }
    
    public int getExp(){
        return exp;
    }
    
    public boolean isBoughtOnce(){
        return hasLimit;
    }
    
    public void hasBeenBought(boolean mode){
        this.hasLimit = mode;
    }
    
    public int alternateNames(String itemName){
        if(itemName.toLowerCase().startsWith("blood"))
            return 1;
        else if(itemName.toLowerCase().startsWith("selini"))
            return 2;
        else if(itemName.toLowerCase().startsWith("pohja"))
            return 3;
        else if(itemName.toLowerCase().startsWith("sephena"))
            return 4;
        else if(itemName.toLowerCase().startsWith("demonic"))
            return 5;
        else if(itemName.toLowerCase().startsWith("salem"))
            return 6;
        else if(itemName.toLowerCase().startsWith("azog"))
            return 7;
        
        return 0;
    }
}