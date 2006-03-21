package twcore.bots.sbbot;

public class SBPlayer {
    private String name;
    private int goals;
    // 
    private boolean active;
    
    SBPlayer( String playername ) {
	goals = 0;
	name = playername;
	active = true;
    }
    public String getName() { return name; }
    public void addGoal() { goals++; }
    public int getGoals() { return goals; }
    public boolean isActive() { return active; }
    public void setActive( boolean b ) { active = b; }
}