package twcore.bots.sbbot;
import twcore.core.*;
import java.util.*;

public class SBMatch {
    private int DEFAULTSHIP = 1;
    private int MAXPLAYERS = 5;
    private int MINPLAYERS = 1;
    private int MINGOALS = 5;
    private int MAXGOALS = 10;
    // bounding boxes of the 2 start safes. x,y x,y format, followed by the start warp location, x,y format
    private final int MINX = 0, MINY = 1, MAXX = 2, MAXY = 3, STARTX = 4, STARTY = 5;
    private int[] TEAM0BOX = { 469, 597, 480, 603, 474, 600 };
    private int[] TEAM1BOX = { 544, 597, 555, 603, 550, 600 };
    private BotAction m_botAction;
    private SBTeam team0, team1;
    public static final int NONE = 0, STARTING = 1, PICKING = 2, COUNTDOWN = 3, PLAYING = 4, ENDING = 5;
    private int gameState;
    private PickHandler picker;
    private RosterHandler roster;
    private int goals;
    private Vector<TimerTask> tasks;
    private int matchType;

    public static final int HOSTED = 1, AUTOMATED = 2;

    public SBMatch(BotAction action, int type) {
	assert( type == HOSTED || type == AUTOMATED );
	m_botAction = action;
	matchType = type;
	tasks = new Vector<TimerTask>();
	team0 = new SBTeam( 0 );
	team1 = new SBTeam( 1 );
	goals = 10;
	gameState = STARTING;
    }

    public SBMatch( BotAction action, String team0Name, String team1Name, int type ) {
	assert( type == HOSTED || type == AUTOMATED );
	m_botAction = action;
	matchType = type;
	tasks = new Vector<TimerTask>();
	team0 = new SBTeam( 0, team0Name );
	team1 = new SBTeam( 1, team1Name );
	goals = 10;
	gameState = STARTING;
    }

    public void cleanUp() {
	for( TimerTask task : tasks ) {
	    try {
		gameState = NONE;
		task.cancel();
	    } catch( Exception e ) {
	    }
	}
    }
    
    // --- MATCH CONTROLLER INTERFACE --	
    public void setGoals( int i ) throws Exception {
	if( gameState != STARTING )
	    throw new Exception( "The number of goals to win the match can only be changed during setup." );
	if( i < MINGOALS )
	    throw new Exception( "Pshhh... *REAL* ballers play to at least " + MINGOALS + ".  Request denied." );
	if( i > MAXGOALS )
	    throw new Exception( "Sorry, I can't handle balling for longer than a match to " + MAXGOALS + "." );
	goals = i;
    }
    	
    public void setCaptain( String name, int team ) throws Exception {
	assert( team == 0 || team == 1 );
	if( gameState == NONE ) throw new Exception( "You must start a !game first." );
	SBTeam targetTeam, otherTeam;
	if(team == 0) {
	    targetTeam = team0;
	    otherTeam = team1;
	} else {
	    targetTeam = team1;
	    otherTeam = team0;
	}
	if( m_botAction.getFuzzyPlayer( name ) == null )
	    throw new Exception( name + " isn't present in the arena. ");
	name = m_botAction.getFuzzyPlayer( name ).getPlayerName();
	if( !sbbot.isSBOp( name ) ) {
	    if( otherTeam.isCaptain( name ) )
		throw new Exception( "Only ERs and SBOps can be captain of both frequencies at once." );
	}
	targetTeam.setCaptain( m_botAction.getFuzzyPlayer( name ).getPlayerName() );
	m_botAction.sendArenaMessage( name + " assigned as captain of " + targetTeam.getName() + ".");
    }

    public void startPick() throws Exception {
	if( gameState != STARTING ) 
	    throw new Exception( "Picking may only be started during !setup." );
	if( team0.getCaptain() == null || team1.getCaptain() == null ) 
	    throw new Exception( "Both teams must have a captain before picking begins." );
	m_botAction.sendArenaMessage("Picking has begun.");
	picker = new PickHandler();
	roster = new RosterHandler();
	gameState = PICKING;
    }

    public void addPlayer( String adder, String addee ) throws Exception {
	if( !isCaptain( adder ) )
	    throw new Exception( "Only Captains can !add players." );
	if( m_botAction.getFuzzyPlayer( addee ) == null )
	    throw new Exception( addee + " isn't present." );
	addee = m_botAction.getFuzzyPlayer( addee ).getPlayerName();
	if( gameState == PICKING )
	    picker.pick( adder,  addee );
	else if( gameState == PLAYING ) 
	    roster.addPlayer( addee, adder );
	else if( gameState == STARTING ) {
	    if( sbbot.isSBOp( adder ) )
		throw new Exception( "You must !startpick before you can begin to !add." );
	    else
		throw new Exception( "The host must finish setting up the game before you can !add." );
	} else
	    throw new Exception( "The game is no longer in progress." );
    }

    public void removePlayer( String captain, String removee ) throws Exception {
	if( !isCaptain( captain ) )
	    throw new Exception( "Only captains can !remove players." );
	SBTeam team, otherTeam;
	if( team0.isCaptain( captain ) ) {
	    team = team0;
	    otherTeam = team1;
	} else {
	    team = team1;
	    otherTeam = team0;
	}
	if( !isFuzzyPlayer( removee ) ) 
	    throw new Exception( removee + " isn't playing." );
	if( !team.isFuzzyPlayer( removee ) )
	    throw new Exception( "You may only remove players from your own team." );
	roster.removePlayer( removee, team );
    }
    
    public void substitutePlayer( String captain, String subRequest ) throws Exception {
	if( !isCaptain( captain ) )
	    throw new Exception( "Only captains can !sub players." );
	if( gameState != PLAYING )
	    throw new Exception( "Substitutions may only be made during the game." );
	String[] playerNames = Tools.cleanStringChopper( subRequest, ':' );
	if( playerNames.length != 2 )
	    throw new Exception( "Use !sub player1:player2." );
	roster.processSubstitution( captain, playerNames[0], playerNames[1] );
    }

    public void lagoutRequest( String name ) throws Exception {
	if( !isPlayer( name ) ) 
	    throw new Exception( "You cannot use !lagout to enter a game you were not a part of." );
	if( m_botAction.getPlayer( name ).getShipType() != 0 )
	    throw new Exception( "You're already in the game!" );
	if( gameState == ENDING )
	    throw new Exception( "You can only reenter during the game." );
	SBTeam team;
	if(team0.isPlayer(name)) team = team0;
	else team = team1;
	if(!team.getPlayer(name).isActive())
	    throw new Exception( "Your spot in the game has been given to someone else." );
	m_botAction.setShip( name, DEFAULTSHIP );
	m_botAction.setFreq( name, team.getFreq() );
    }
    
    public void toggleReady( String name ) throws Exception {
	if( !isCaptain( name ) )
	    throw new Exception( "Only captains may use the !ready command." );
	if( gameState != PICKING )
	    throw new Exception( "The !ready command may only be used during picking." );
	picker.toggleReady( name );
    }
	
    public void setTeamName( String teamName , int team) throws Exception {
	assert(team == 0 || team == 1);
	SBTeam targetTeam;
	if(gameState != STARTING)
	    throw new Exception( "Team names can only be set during !setup." );
	if( team == 0 ) targetTeam = team0;
	else if( team == 1 ) targetTeam = team1;
	else throw new Exception( "Invalid team specified to setTeamName." );
	targetTeam.setName( teamName );

    }
    
    // --- END MATCH CONTROLLER INTERFACE ---
    public int getGoals() { return goals; }
	
    public boolean isGameInProgress() { return gameState != 0; }
    public int getState() { return gameState; }

    public boolean isCaptain( String name ) {
	try {
	    return isCaptain( name, 0 ) || isCaptain( name, 1 );
	} catch (Exception e) {}
	return false;
    }

    public boolean isCaptain( String name, int team ) throws Exception {
	assert( team == 0 || team == 1 );
	if( team == 0 ) return team0.isCaptain( name );
	else if( team == 1 ) return team1.isCaptain( name );
	throw new Exception( "Invalid team specified to isCaptain." );
    }

    public boolean isActivePlayer( String name ) {
	try {
	    return team0.isActivePlayer( name ) || team1.isActivePlayer( name );
	} catch (Exception e) {
	}
	return false;
    }
    
    public boolean isPlayer( String name ) {
	try {
	    return isPlayer( name, 0 ) || isPlayer( name, 1 );
	} catch( Exception e ) {
	}
	return false;
    }

    public boolean isPlayer( String name, int team ) throws Exception {
	assert( team == 0 || team == 1 );
	if( team == 0 ) return team0 != null && team0.isPlayer( name );
	return team1 != null && team1.isPlayer( name );
    }

    public boolean isFuzzyPlayer( String name ) {
	try {
	    return isFuzzyPlayer( name, 0 ) || isFuzzyPlayer( name, 1 );
	} catch( Exception e ) {
	}
	return false;
    }

    public boolean isFuzzyPlayer( String name, int team ) throws Exception {
	assert( team == 0 || team == 1 );
	if( team == 0 ) return team0 != null && team0.isFuzzyPlayer( name );
	return team1 != null && team1.isFuzzyPlayer( name );
    }

    

    public String getTeamName( int team ) throws Exception {
	assert( team == 0 || team == 1 );
	if( team == 0 ) return team0.getName();
	return team1.getName();
    }

    private SBTeam getTeam( String player ) {
	if( team0 != null && team0.isPlayer( player ) )
	    return team0;
	if( team1 != null && team1.isPlayer( player ) )
	    return team1;
	return null;
    }

    private boolean waitingOnDrop = false;
    
    private void beginCountDown() {
	m_botAction.sendUnfilteredPublicMessage("*restart");
	m_botAction.sendArenaMessage( "Both teams ready!  Game starts shortly.", 2 );
	m_botAction.warpFreqToLocation( team0.getFreq(), TEAM0BOX[STARTX], TEAM0BOX[STARTY] );
	m_botAction.warpFreqToLocation( team1.getFreq(), TEAM1BOX[STARTX], TEAM0BOX[STARTY] );
	gameState = COUNTDOWN;
	/* this part is necessary because lag will sometimes screw it up if there's no delay */
	TimerTask t = new TimerTask() {
		public void run() {
		    gameState = PLAYING;
		    m_botAction.scoreResetAll();
		    m_botAction.warpAllRandomly();
		    m_botAction.sendArenaMessage( "Get ready for the drop!,", 26 );
		    waitingOnDrop = true;
		}
	    };
	tasks.add( t );
	m_botAction.scheduleTask( t, 10000 );
    }

    // --- EVENTS ---
    public void ballPositionEvent( BallPosition event ) {
	if( waitingOnDrop ) {
	    m_botAction.sendArenaMessage( "Go go go!", 104 );
	    waitingOnDrop = false;
	}
    }

    public void playerPositionEvent( PlayerPosition event ) {
	if( gameState == COUNTDOWN ) {
	    String name = m_botAction.getPlayer( event.getPlayerID() ).getPlayerName();
	    int[] bBox;
	    int xTile, yTile;
	    try {
		if( isPlayer( name, 0 ) ) bBox = TEAM0BOX;
		else bBox = TEAM1BOX;
		xTile = event.getXLocation() / 16;
		yTile = event.getYLocation() / 16;
		if( xTile < bBox[MINX] || xTile > bBox[MAXX] || yTile < bBox[MINY] || yTile > bBox[MAXY] )
		    m_botAction.warpTo( name, bBox[STARTX], bBox[STARTY] );
	    } catch( Exception e ) {}
	}
    }

    private void debug(String m) {
	m_botAction.sendSmartPrivateMessage("Arceo", m);
    }
    private String lastScorer;
    public void messageEvent( Message event ) {
	if( gameState == PLAYING ) {
	    if( event.getMessageType() == Message.ARENA_MESSAGE &&
		// this should work since the bot should always be in spec.. no need to check
		// for team goal
		event.getMessage().startsWith("Enemy Goal!") ) {
		lastScorer = null;
		String[] words = Tools.cleanStringChopper( event.getMessage(), ' ' );
		for(int i = 3; i < words.length - 2; i++) {
		    if( lastScorer == null ) lastScorer = words[i];
		    else lastScorer = lastScorer + " " + words[i];
		}
	    } else if( event.getMessageType() == Message.ARENA_MESSAGE &&
		       event.getMessage().startsWith("Soccer game over")) {
		TimerTask t = new TimerTask() {
			public void run() {
			    postGame();
			}
		    };
		// Necessary to avoid race condition that prevents recording of last goal
		m_botAction.scheduleTask( t, 1000 );
	    }
	}
    }

    public void soccerGoalEvent( SoccerGoal event ) {
	if( gameState == PLAYING ) {
	    try {
		m_botAction.sendSmartPrivateMessage( "arceo", "Goal scored by " + lastScorer + "." );
		getTeamByFreq( event.getFrequency() ).addGoal( lastScorer );
		lastScorer = null;
		
	    } catch( Exception e ) {
		m_botAction.sendSmartPrivateMessage("arceo", "soccerGoalEvent: " + e.toString() );
	    }
	}
    }
    
    TimerTask doPostGame = null;
    private void postGame() {
	if(gameState == NONE) return;
	gameState = ENDING;
	if( doPostGame != null ) return;
	doPostGame = new TimerTask() {
		public void run() {
		    doPostGame = null;
		    compileStats();
		}
	    };
	m_botAction.sendArenaMessage("And that's the game!", 5);
	m_botAction.scheduleTask( doPostGame, 5000 );
    }

    private void compileStats() {
	gameState = NONE;
	SBPlayer MVP = team0.getMVP();
	if( MVP.getGoals() < team1.getMVP().getGoals() ) MVP = team1.getMVP();
	m_botAction.sendArenaMessage("MVP: " + MVP.getName() + " with " + MVP.getGoals() + " goals!", 7);
    }
    
    public SBTeam getTeamByFreq( int i ) throws Exception {
	if( team0.getFreq() == i ) return team0;
	else if( team1.getFreq() == i ) return team1;
	throw new Exception( "No team exists on that frequency." );
    }

    public SBTeam getTeamByNum( int i ) {
	assert( i == 0 || i == 1 );
	if( i == 0 ) return team0;
	return team1;
    }
    
    // Private classes for managing some of the more complex tasks.. Maybe need to be turned
    // into their own proper classes later.
    private class RosterHandler {

	public RosterHandler() {

	}
	
	public void addPlayer( String player, SBTeam team ) throws Exception {
	    if( isPlayer( player ) )
		throw new Exception( "Cannot add " + player + " as he/she is already playing.");
	    if( isCaptain( player ) && !team.isCaptain( player ) && !sbbot.isSBOp( player ) )
		throw new Exception( player + " is the captain of the other team." );/*
	    if( gameState == PLAYING ) {
		addToWaitingList(player, team);
		m_botAction(
		}*/
	    m_botAction.spec( player );
	    m_botAction.setShip( player, DEFAULTSHIP );
	    m_botAction.setFreq( player, team.getFreq() );
	    team.addPlayer( player );
	    m_botAction.sendArenaMessage( player + " in for " + team.getName()+ "." );
	}

	public void addPlayer( String player, String cap ) throws Exception {
	    if( team0.isCaptain( cap ) ) addPlayer( player, team0 );
	    else if( team1.isCaptain( cap ) ) addPlayer( player, team1 );
	    else throw new Exception( "Only captains may !add." );
	}
	    
	public void removePlayer( String player, SBTeam team ) throws Exception {
	    assert( team.isPlayer( player ) );
	    if( gameState != PICKING )
		throw new Exception( "Players may only be removed during picking." );
	    String fullName = team.removeFuzzyPlayer( player );
	    if( m_botAction.getPlayer( fullName ) != null ) m_botAction.spec( fullName );
	    m_botAction.sendArenaMessage( fullName + " removed from " + team.getName() + "." );
	    if( team.isReady() ) picker.toggleReady( team.getCaptain() );
	}

	public void processSubstitution( final String captain, String player, String sub ) throws Exception {
	    assert( isCaptain( captain ) );
	    final SBTeam team;
	    if( team0.isCaptain( captain ) ) team = team0;
	    else team = team1;
	    final SBPlayer p;
	    if( (p = team.getFuzzyPlayer( player )) == null)
		throw new Exception( player + " is not in for your team!" );
	    final String subName;
	    if( m_botAction.getFuzzyPlayer( sub ) == null )
		throw new Exception( sub + " is not present to be subbed in!" );
	    subName = m_botAction.getFuzzyPlayer( sub ).getPlayerName();
	    if( isActivePlayer( subName ) )
		throw new Exception( subName + " is already playing!" );
	    TimerTask t = new TimerTask() {
		    public void run() {
			try {
			    team.addPlayer( subName );
			    p.setActive( false );
			    m_botAction.setShip( subName, DEFAULTSHIP );
			    m_botAction.setFreq( subName, team.getFreq() );
			    // comments in BotAction recommend sending the command twice for some reason
			    m_botAction.spec( p.getName() );
			    m_botAction.spec( p.getName() );
			    m_botAction.sendArenaMessage( p.getName() + " has been substituted by " + subName + "." );
			} catch (Exception e) {
			    m_botAction.sendSmartPrivateMessage( captain, "Error while trying to process substitute request." );
			    m_botAction.sendSmartPrivateMessage( "arceo", e.toString() );
			}
		    }
		};
	    m_botAction.scheduleTask( t, 3000 );
	    m_botAction.sendSmartPrivateMessage( captain, "Your substitute request will be processed in a few seconds." );
	}

    }
    
    private class PickHandler {
	SBTeam currentTeam, otherTeam;

	public PickHandler() {
	    currentTeam = team0;
	    otherTeam = team1;
	    m_botAction.sendArenaMessage( currentTeam.getName() + ", your pick." );
	}

	private void toggle() {
	    if( currentTeam == team0 ) {
		currentTeam = team1;
		otherTeam = team0;
	    } else {
		currentTeam = team0;
		otherTeam = team1;
	    }
	}
	
	public void pick( String picker, String pickee ) throws Exception {
	    if( !isCaptain( picker ) )
		throw new Exception( "You're not a captain. " );
	    //I do this so that it will always allow a freq to pick if it doesn't have enough
	    if( currentTeam.activePlayerCount() >= MAXPLAYERS) toggle();
	    if( currentTeam.activePlayerCount() >= MAXPLAYERS)
		throw new Exception( "The maximum number of players have been added." );
	    if( !currentTeam.isCaptain( picker ) ) {
		if( !currentTeam.isReady() ) 
		    throw new Exception( "It's not your turn to pick." );
		else {
		    // If it's the other team's turn, but they've already readied, let you pick
		    toggle();
		}
	    }
	    
	    roster.addPlayer( pickee, currentTeam );
	    toggle();
	    if( currentTeam.activePlayerCount() >= MAXPLAYERS) return;
	    m_botAction.sendArenaMessage( currentTeam.getName() + ", your pick." );
	}

	public void toggleReady( String name ) throws Exception {
	    SBTeam thisTeam, otherTeam;
	    if( team0.isCaptain( name ) ) {
		thisTeam = team0;
		otherTeam = team1;
	    } else {
		thisTeam = team1;
		otherTeam = team0;
	    }
	    if( thisTeam.isReady() ) {
		thisTeam.setReady( false );
		m_botAction.sendArenaMessage( thisTeam.getName() + " is NOT ready to begin." );
		return;
	    }
	    if( thisTeam.activePlayerCount() < MINPLAYERS )
		throw new Exception( "You must have at least " + MINPLAYERS + " players in order to !ready." );
	    if( otherTeam.activePlayerCount() > thisTeam.activePlayerCount() )
		throw new Exception( "You must have at least as many players as the other team before you !ready." );
	    thisTeam.setReady( true );
	    m_botAction.sendArenaMessage( thisTeam.getName() + " is ready to begin." );
	    if( thisTeam.isReady() && otherTeam.isReady() ) beginCountDown();
	}
    }
}
