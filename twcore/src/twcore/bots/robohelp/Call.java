package twcore.bots.robohelp;

public abstract class Call {

    long timeSent;
    long timeClaim;
    String message;
    String playerName;
    int id;
    int dataID;
    int claimType;
    int callType;
    boolean claimed;
    String claimer;
    
    public Call() {
        this.id = -1;
        this.dataID = -1;
        this.timeSent = System.currentTimeMillis();
        this.timeClaim = -1;
        this.claimer = "";
        this.claimed = false;
        this.claimType = 0;
    }

    public String getMessage() {
        return this.message;
    }
    
    public boolean isExpired(long now, int expire) {
        return ((now - getTime()) > expire);
    }
    
    public abstract void claim(String name);

    public String getPlayername() {
        return this.playerName;
    }

    public long getTime() {
        return this.timeSent;
    }

    public long getClaim() {
        return this.timeClaim;
    }

    public int getID() {
        return id;
    }

    public int getCallID() {
        return dataID;
    }

    public String getTaker() {
        return claimer;
    }

    public void setTaker(String name) {
        claimer = name;
    }
    
    public int getClaimType() {
        return claimType;
    }
    
    public void setClaimType(int type) {
        this.claimType = type;
    }

    public boolean isTaken() {
        return claimed;
    }

    public void setID(int db, int id) {
        this.dataID = db;
        this.id = id;
    }

    public void setCallType(int type) {
        this.callType = type;
    }

    public int getCallType() {
        return callType;
    }
}
