package twcore.bots.purepubbot.pubitem;

import twcore.bots.purepubbot.ItemObserver;

//Null Object pattern
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

    @Override
    public void notifyObservers(String playerName) {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void registerObserver(ItemObserver bot) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeObserver(ItemObserver bot) {
        // TODO Auto-generated method stub
        
    }
    
}
