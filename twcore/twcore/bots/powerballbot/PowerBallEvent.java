package twcore.bots.powerballbot;

import twcore.core.*;

public class PowerBallEvent {
	
	String scorer = null;
	String assistant = null;
	String assistant2 = null;
	
	String saver = null;
	String shooterOnGoal = null;
	
	public PowerBallEvent() {
	}
	
	public void setScorer( String name ) { scorer = name; }
	public String getScorer() { return scorer; }
	
	public void setAssist( String name ) { assistant = name; }
	public String getAssist() { return assistant; }
	
	public void setSecondAssist( String name ) { assistant2 = name; }
	public String getSecondAssist() { return assistant2; }
	
	public void saved( String name ) { saver = name; }
	public boolean saved() { 
		if( saver == null ) return false;
		else return true;
	}
	public String getSaver() { return saver; }
	
	public void shotOnGoal( String name ) { shooterOnGoal = name; }
	public boolean shotOnGoal() {
		if( shooterOnGoal == null ) return false;
		else return true;
	}
	public String getShooter() { return shooterOnGoal; }
	
	public void setBad() {
	}
}
