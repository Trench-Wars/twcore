package twcore.bots.multibot.chaos;

public class ItemDrops {
    private String name;
    private int prizeId;

    public ItemDrops(){
    }

    public ItemDrops( String newName, int prizeID ){
        this.name = newName;
        this.prizeId = prizeID;
    }

    public void setName( String newName ){
        name = newName;
    }

    public String getName(){
        return name;
    }

    public void setPrizeId( int prizeID ){
        prizeId = prizeID;
    }

    public int createDropRate(int wins) {
        return (int) (Math.random() * 100 + wins);
    }
    
    public int getDropId() {
        return prizeId;
    }
}