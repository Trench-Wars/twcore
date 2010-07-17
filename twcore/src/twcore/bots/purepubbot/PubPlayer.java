package twcore.bots.purepubbot;

public class PubPlayer implements Comparable<PubPlayer>{

    
    private String p_name;
    private int id;
    private int point;
    
    public PubPlayer(String name){
        p_name = name;
        point = 0;
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

    @Override
    public int compareTo(PubPlayer o) {
        // TODO Auto-generated method stub
        if(o.getPoint() > getPoint()) return 1;
        if(o.getPoint() < getPoint()) return 0;
        
        return -1;
    }
    
    
}
