package twcore.bots.robohelp;

public class CheaterCall extends Call {

    static final int FREE = 0;
    static final int TAKEN = 1;
    static final int MINE = 2;
    static final int FORGOT = 3;
    static final int CLEAN = 4;
    
    public CheaterCall(String player, String msg, int type) {
        super();
        this.playerName = player;
        this.message = msg;
        this.claimType = type;
    }

    public void claim(String name) {
        claimer = name;
        claimed = true;
        taken = TAKEN;
        timeClaim = System.currentTimeMillis();
    }

    public void mine(String name) {
        claimer = name;
        claimed = true;
        taken = MINE;
        timeClaim = System.currentTimeMillis();
    }

    public void forget() {
        claimer = "[forgot]";
        claimed = true;
        taken = FORGOT;
        timeClaim = System.currentTimeMillis();
    }
}
