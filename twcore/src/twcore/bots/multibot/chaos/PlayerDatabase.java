package twcore.bots.multibot.chaos;

import java.util.LinkedList;

public class PlayerDatabase {
    
    private String playerName;
    private int playerID;
    private int cash;
    private int experience;
    private int safeCount;
    private boolean safeStatus;
    private boolean aoeStatus;
    private boolean deathStatus;
    private LinkedList<Integer> items;
    
    public PlayerDatabase(String name){
        playerName = name;
        cash = 100;
        experience = 0;
        safeCount = 0;
        safeStatus = false;
        aoeStatus = false;
        deathStatus = false;
        items = new LinkedList<Integer>();
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String name) {
        playerName = name;
    }
    
    public int getId() {
        return playerID;
    }
    
    public void setId(int id) {
        this.playerID = id;
    }
    
    public int getMoney() {
        return cash;
    }
    
    public void setMoney(int money) {
        if( money < 0 ) {
            this.cash = 0;
            return;
        }
        
        this.cash = money;
    }
    
    public void gainMoney(int money) {
        this.cash += money;
    }
    
    public void loseMoney(int money) {
        this.cash -= money;
        
        if( this.cash < 0 )
            this.cash = 0;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public void gainExperience(int expBonus) {
        this.experience += expBonus;
    }
    
    public void loseExperience(int expLoss) {
        this.experience -= expLoss;
        
        if( this.experience < 0 )
            this.experience = 0;
    }
    
    public void resetExperience() {
        this.experience = 0;
    }
    
    public void setExperience(int exp) {
        if( exp < 0 ) {
            this.experience = 0;
            return;
        }
        
        this.experience = exp;
    }
    
    /**
     * Stores all of player's bought items
     * @param itemName
     */
    public void loadPlayerItems(int itemId) {
        if(!items.contains(itemId))
            items.add(itemId);
    }
    
    public int getPlayerItemSize() {
        return items.size();
    }
    
    public int[] prizePlayerItems() {
        Object[] objects = items.toArray();
        int[] arr = new int[items.size()];
        for(int i = 0; i < items.size(); i++) {
            String prizeNum = objects[i].toString();
            arr[i] = Integer.parseInt(prizeNum);
        }
        
        return arr;
    }
    
    public boolean checkPlayerItems() {
        return items.isEmpty();
    }
    
    public void removePlayerItems() {
        items.clear();
    }
    
    public void addSafePlayer(boolean mode) {
        this.safeStatus = mode;
        
        if(mode)
            safeCount++;
    }
    
    public boolean isSafePlayer() {
        return safeStatus;
    }
    
    public void activateAOE(boolean mode) {
        this.aoeStatus = mode;
    }
    
    public boolean hasAOE() {
        return aoeStatus;
    }
    
    public int safeAttempt() {
        return safeCount;
    }
    
    public void playerDied(boolean mode) {
        this.deathStatus = mode;
    }
    
    public boolean isDead() {
        return deathStatus;
    }
    
    public void resetPlayerStats() {
        items.clear();
        this.cash = 100;
        this.experience = 0;
        this.safeStatus = false;
    }
}
