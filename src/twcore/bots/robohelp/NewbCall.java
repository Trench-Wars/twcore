package twcore.bots.robohelp;

public class NewbCall extends Call {

    static final int FREE = 0;
    static final int TAKEN = 1;
    static final int FALSE = 2;


    public NewbCall(String player) {
        super();
        this.playerName = player;
        this.message = "";
        this.claimer = "";
        this.claimType = FREE;
    }

    public void claim(String name) {
        claimer = name;
        claimType = TAKEN;
        claimed = true;
        timeClaim = System.currentTimeMillis();
    }

    public String getName() {
        return playerName;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void falsePos(String name) {
        claimType = FALSE;
        claimer = name;
        claimed = true;
        timeClaim = System.currentTimeMillis();
    }

    public void undoFalse(String name) {
        claimType = TAKEN;
        claimer = name;
    }

}
