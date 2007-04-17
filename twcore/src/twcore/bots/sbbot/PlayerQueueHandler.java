package twcore.bots.sbbot;
import java.util.Vector;

import twcore.core.BotAction;


// Handles queueing of players waiting to go in.  Currently written for correctness rather than
// efficiency.
public class PlayerQueueHandler {
    private BotAction m_botAction;
    private SBMatch match;
    private int matchType;
    private Vector<String> playerQueueAuto;
    // Hold separate queues for each Freq
    private Vector<Vector<String>> playerQueueHosted;

    public PlayerQueueHandler( BotAction b, SBMatch m, int type ) {
	m_botAction = b;
	match = match;
	matchType = type;
	playerQueueAuto = new Vector<String>();
	playerQueueHosted = new Vector<Vector<String>>();
	playerQueueHosted.add( new Vector<String>() );
	playerQueueHosted.add( new Vector<String>() );
    }
	
    public void enQueueAuto( String name ) throws Exception {
	assert( matchType == SBMatch.AUTOMATED );
	if( !playerQueueAuto.contains( name.toLowerCase() ) )
	    playerQueueAuto.add( name.toLowerCase() );
    }

    public void enQueueHosted( String name, int teamNum ) throws Exception {
	assert( matchType == SBMatch.HOSTED );
	int otherTeamNum;
	if( teamNum == 1 ) otherTeamNum = 2;
	else otherTeamNum = 1;
	if( playerQueueHosted.get( teamNum ).contains( name.toLowerCase() ) ) {
	    throw new Exception( name + " is already queued to go in for " +
				 match.getTeamByNum( teamNum ) + "." );
	} else if ( playerQueueHosted.get( otherTeamNum ).contains( name.toLowerCase() ) ) {
	    throw new Exception( name + " is already queued to go in for " +
				 match.getTeamByNum( otherTeamNum ) + ".");
	}
	playerQueueHosted.get( teamNum ).add( name.toLowerCase());
	m_botAction.sendSmartPrivateMessage( name, "You have been queued to go in for " +
					    match.getTeamByNum( teamNum ).getName() + "." );
    }

    private void cleanQueue() {
	if( matchType == SBMatch.AUTOMATED ) {
	    for( String player : playerQueueAuto ) {
		if( m_botAction.getPlayer( player ) == null)
		    playerQueueAuto.remove( player );
	    }
	} else if( matchType == SBMatch.HOSTED ) {
	    for( Vector<String> queue : playerQueueHosted ) {
		for( String player : queue ) {
		    if( m_botAction.getPlayer( player ) == null)
			queue.remove( player );
		}
	    }
	}
	
    }
    
    //Checks if there are two people ready to go in.  Removes absent people from the queue.
    public boolean checkQueue() {
	cleanQueue();
	if( matchType == SBMatch.AUTOMATED ) {
	    if( playerQueueAuto.size() > 1 ) return true;
	    return false;
	} else {
	    if( ( playerQueueHosted.get( 0 ).size() > 0 ) &&
		( playerQueueHosted.get( 1 ).size() > 0 ) )
		return true;
	    return false;
	}
    }
	
    public String[] popPairAuto() {
	assert( matchType == SBMatch.AUTOMATED );
	if( !checkQueue() ) return null;
	String[] pair = (String[]) playerQueueAuto.subList( 0, 2 ).toArray();
	playerQueueAuto.removeElementAt(0);
	playerQueueAuto.removeElementAt(0);
	return pair;
    }

    public String[] popPairHosted() {
	assert( matchType == SBMatch.HOSTED );
	if( !checkQueue() ) return null;
	String[] pair = new String[2];
	pair[0] = playerQueueHosted.get( 0 ).get( 0 );
	pair[1] = playerQueueHosted.get( 1 ).get( 0 );
	playerQueueHosted.get( 0 ).removeElementAt( 0 );
	playerQueueHosted.get( 1 ).removeElementAt( 0 );
	return pair;
    }

    public String popNextPlayer() {
	assert( matchType == SBMatch.AUTOMATED );
	cleanQueue();
	if( playerQueueAuto.size() == 0 ) return null;
	String player = playerQueueAuto.get( 0 );
	playerQueueAuto.remove( player );
	return player;
    }

    public String popNextPlayer(int teamNum) {
	assert( matchType == SBMatch.HOSTED );
	assert( teamNum == 1 || teamNum == 2 );
	cleanQueue();
	teamNum--;
	if( playerQueueHosted.get( teamNum ).size() == 0 ) return null;
	String player = playerQueueHosted.get( teamNum ).get( 0 );
	playerQueueHosted.get( teamNum ).remove( player );
	return player;
    }
}