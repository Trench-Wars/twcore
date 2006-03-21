package twcore.bots.sbbot;
import twcore.core.*;
import java.util.*;

public class SBTeam {
    private HashMap<String, SBPlayer> players;
    private String captain;
    private String teamName;
    private int freq;
    private int goals = 0;
    private boolean ready;


    public SBTeam( int frequency ) {
	freq = frequency;
	teamName = "Freq " + freq;
	players = new HashMap<String,SBPlayer>();
    }

    public SBTeam( int frequency, String tName ) {
	freq = frequency;
	teamName = tName;
	players = new HashMap<String,SBPlayer>();
    }

    public void setName( String newName ) { teamName = newName;	}
    public String getName() { return teamName; }
	
    public void setReady( boolean b ) { ready = b; }
    public boolean isReady() { return ready; }
	
    public int getFreq() { return freq; }
    public void setFreq( int i ) {
	freq = i;
    }
    /* TODO implement a way to distinguish between active/inactive players.
       active player - in game and playing or lagged out
       inactive player - player that who was subbed out/missed the return window
       after a lagout.
    */
    public int activePlayerCount() {
	if( players == null ) return 0;
	int count = 0;
	for( SBPlayer player : players.values() ) {
	    if( player.isActive() ) count++;
	}
	return  count;
    }

    public void setCaptain( String name ) {
	captain = name;
    }

    public String getCaptain() { return captain; }
	
    public boolean isCaptain( String name ) {
	if( captain == null ) return false;
	return captain.equalsIgnoreCase( name );
    }

    public void addPlayer( String name ) throws Exception {
	if( isPlayer( name ) ) {
	    throw new Exception(name + "is already playing!");
	}
	players.put( name.toLowerCase(), new SBPlayer( name ) );
    }
	
    public String removePlayer( String name ) throws Exception {
	if( isPlayer( name ) )
	    for( String player : players.keySet() ) {
		if( player.equalsIgnoreCase( name ) ) {
		    players.remove( player );
		    return player;
		}
	    }

	throw new Exception( name + " is not playing for " + teamName + "." );
    }

    public String removeFuzzyPlayer( String name ) throws Exception {
	if( !isFuzzyPlayer( name ) )
	    throw new Exception( name + " is not playing for " + teamName + "." );
	if( isPlayer( name ) ) return removePlayer( name );
	for( String player : players.keySet() ) {
	    if( player.toLowerCase().startsWith( name.toLowerCase() ) ) {
		return removePlayer( player );
	    }
	}
	throw new Exception( "Strange bug in removeFuzzyPlayer." );
    }
	
    public boolean isPlayer( String name ) {
	return players.containsKey( name.toLowerCase() );
    }

    public boolean isFuzzyPlayer( String name ) {
	for( String player : players.keySet() ) {
	    if( player.toLowerCase().startsWith( name.toLowerCase() ) ) return true;
	}
	return false;
    }

    public SBPlayer getPlayer( String name ) {
	return players.get( name.toLowerCase() );
    }

    public boolean isActivePlayer( String name ) {
	SBPlayer p;
	if( (p = getPlayer( name )) != null && p.isActive() ) return true;
	return false;
    }
    
    public SBPlayer getFuzzyPlayer( String name ) {
	for( String player : players.keySet() ) {
	    if( player.toLowerCase().startsWith( name.toLowerCase() ) ) return players.get(player);
	}
	return null;
    }

    public int getGoals() { return goals; }
    public void addGoal( String player ) {
	goals++;
	getPlayer( player ).addGoal();
    }
    public void setGoals( int i ) { goals = i; }

    public SBPlayer getMVP() {
	int bestScore = -1;
	int curScore;
	SBPlayer bestPlayer = null;
	for( String player : players.keySet() ) {
	    if( (curScore = players.get( player ).getGoals()) > bestScore ) {
		bestScore = curScore;
		bestPlayer = players.get(player);
	    }
	}
	assert( bestPlayer != null );
	return bestPlayer;
    }
}