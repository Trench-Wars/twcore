package twcore.bots.sbbot;
import twcore.core.*;
import java.util.*;


public class SBPlayer {
    // PENDING - queued to go in
    // PLAYING - on active roster (might be lagged out)
    // INACTIVE - got subbed out
    public static final int PENDING = 1, ACTIVE = 2, INACTIVE = 3;
    final String name;
    private SBTeam currentTeam;

    private int status;

    public SBPlayer(String n) {
	name = n;
    }

    public SBPlayer(String n, SBTeam t) {
	name = n;
	setTeam(t);
    }

    public String getName() { return name; }
    public void setTeam(SBTeam t) { currentTeam = t; }
    public SBTeam getTeam() { return currentTeam; }
    
    public boolean equalsPlayer(String p) {
	if(name.equalsIgnoreCase(p)) return true;
	return false;	
    }
    
    public boolean equalsPlayer(Player p) {
	return equalsPlayer(p.getPlayerName());
    }

    public int getStatus() { return status; }
    public void setStatus(int i) { status = i; }
}