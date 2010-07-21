package twcore.bots.purepubbot.pubitem;

public class NullItem
        extends PubItem {

    private String reason;
    public NullItem(String name, int itemNumber, int price, String reason) {
        super(name, itemNumber, price);
        // TODO Auto-generated constructor stub
        this.reason = reason;
    }
    
    public void setReason(String reason){
        this.reason = reason;
    }
    
    @Override
    public String toString(){
        return reason;
    }
    
}
