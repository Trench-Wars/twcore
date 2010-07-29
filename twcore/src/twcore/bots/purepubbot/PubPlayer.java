package twcore.bots.purepubbot;

import java.util.LinkedList;
import java.util.NoSuchElementException;


import twcore.bots.purepubbot.pubitem.PubItem;

public class PubPlayer implements Comparable<PubPlayer>{
    
    private String p_name;
    private int id;
    private int point;
    private LinkedList<String> listLastItems;
    private LinkedList<PubItem> boughtItems;
    private int limitBought;
    private int itemsBoughtPerLife;
    private int dataObjons[]; 
    public PubPlayer(String name){
        p_name = name;
        point = 0;
        listLastItems = new LinkedList<String>();
        boughtItems = new LinkedList<PubItem>();
        this.limitBought = 3;
        this.itemsBoughtPerLife = 0;
        dataObjons = new int[7];
    }
    
    public void setObjon(int[] dataObjons){
        this.dataObjons = dataObjons;
    }
    
    public int[] getObjon(){
        return dataObjons;
    }
    
    public String getP_name() {
        return p_name;
    }
    public void setP_name(String pName) {
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

    public void addItem(PubItem item){
        this.boughtItems.add(item);
    }
    
    public void addItemString(String itemDetail){
        listLastItems.add(itemDetail);
    }
    
    public String getLastItemDetail(){
        return listLastItems.getLast();
    }
    
    public PubItem getLastItem() throws NoSuchElementException{
        return this.boughtItems.getLast();
    }
    @Override
    public int compareTo(PubPlayer o) {
        // TODO Auto-generated method stub
        if(o.getPoint() > getPoint()) return 1;
        if(o.getPoint() < getPoint()) return 0;
        
        return -1;
    }
    
    public boolean hasNotReachedLimit(){
        return this.limitBought < this.itemsBoughtPerLife;
    }

    /**
     * @param itemsBoughtPerLife the itemsBoughtPerLife to set
     */
    public void setItemsBoughtPerLife(int itemsBoughtPerLife) {
        this.itemsBoughtPerLife = itemsBoughtPerLife;
    }

    /**
     * @return the itemsBoughtPerLife
     */
    public int getItemsBoughtPerLife() {
        return itemsBoughtPerLife;
    }
}
