/**
   Notes:
       -Strikeball op names currently cannot contain a ','.
       -runtimeDebugger should probably be in a separate utility file somewhere.
 **/

package twcore.bots.sbbot;

import twcore.core.*;
import java.io.*;
import java.util.*;

public class sbbot extends SubspaceBot {

    private CommandInterpreter CI;
    private BotSettings m_botSettings;
    private BotSettings parameters;
    private BotSettings SBOpSettings;
    private DebugHandler debugger;
    private EventRequester events;

    private SBMatch currentMatch;
    private HashSet<String> notPlayers;
    private final int NOTPLAYINGFREQ = 9998;
    
    // People authorized to start manual strikeball games
    private static HashSet<String> bigBallers;
    private static OperatorList opList;


    public sbbot( BotAction botAction ) {
	super( botAction );

	opList = botAction.getOperatorList();
	m_botSettings = m_botAction.getBotSettings();
	loadSettings();
	CI = new CommandInterpreter( m_botAction );
	events = m_botAction.getEventRequester();
	events.request( EventRequester.MESSAGE );
	events.request( EventRequester.BALL_POSITION );
	events.request( EventRequester.SOCCER_GOAL );
	events.request( EventRequester.PLAYER_POSITION );
	registerCommands();
	notPlayers = new HashSet<String>();
    }

    private void loadSettings() {
	parameters = new BotSettings( m_botSettings.getString( "paramfile" ) );
	System.out.println("Paramfile: " + m_botSettings.getString( "paramfile" ));
	SBOpSettings = new BotSettings( m_botSettings.getString( "opfile" ) );
	bigBallers = new HashSet<String>();
	try {
	    System.out.println("Oplist: " + SBOpSettings.getString( "ops" ) + "\n" );
	    for( String x : Tools.cleanStringChopper( SBOpSettings.getString( "ops" ), ';') ) {
		bigBallers.add(x.toLowerCase());
	    }
	} catch (Exception e){}
	/* After testing.
 	DEFAULTSHIP = Integer.parseInt(parameters.getString("defaultship"));
	MAXPLAYERS = Integer.parseInt(parameters.getString("maxplayers"));
	*/
    }

    private void registerCommands() {
	int mtype = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
	CI.registerCommand("!help", mtype, this, "handleHelp");
	CI.registerCommand("!ops", mtype, this, "handleOps");
	CI.registerCommand("!die", mtype, this, "handleDie");
	//should be !game team1:team2
	CI.registerCommand("!game", mtype, this, "handleGame");
	CI.registerCommand("!t1", mtype, this, "handleT1");
	CI.registerCommand("!t2", mtype, this, "handleT2");
	CI.registerCommand("!goals", mtype, this, "handleGoals");
	CI.registerCommand("!t1-setcaptain", mtype, this, "handleSetCap1");
	CI.registerCommand("!t2-setcaptain", mtype, this, "handleSetCap2");
	CI.registerCommand("!startpick", mtype, this, "handleStartPick");
	CI.registerCommand("!killgame", mtype, this, "handleKillGame");
	CI.registerCommand("!add", mtype, this, "handleAdd");
	CI.registerCommand("!remove", mtype, this, "handleRemove");
	CI.registerCommand("!ready", mtype, this, "handleReady");
	CI.registerCommand("!sub", mtype, this, "handleSub");
	CI.registerCommand("!setcaptain", mtype, this, "handleSetCaptain");
	CI.registerCommand("!notplaying", mtype, this, "handleNotPlaying");
	CI.registerCommand("!lagout",mtype,this,"handleLagout");

	// Some commands I used while developing this.

	CI.registerCommand("!position", mtype, this, "handlePosition");
	CI.registerCommand("!lock", mtype, this, "handleLock");
	CI.registerCommand("!chop", mtype, this, "handleChop");
    }
    
    public void handleEvent( LoggedOn event ) {
        m_botAction.joinArena( m_botSettings.getString( "Arena" ) );
	m_botAction.sendUnfilteredPublicMessage( "?chat=ballers" );
    }

    public void handleEvent( Message event ) {
	if( currentMatch != null &&
	    currentMatch.isGameInProgress() &&
	    event.getMessageType() == Message.ARENA_MESSAGE &&
	    event.getMessage().startsWith("Arena UNLOCKED") ) {
	    m_botAction.sendSmartPrivateMessage("arceo","Game in progress. Arena re-locked.");
	    m_botAction.toggleLocked();
	} 
	CI.handleEvent( event );
	if( currentMatch != null ) currentMatch.messageEvent( event );
    }

    public void handleEvent( BallPosition event ) {
	if( currentMatch != null ) currentMatch.ballPositionEvent( event );
    }

    public void handleEvent( PlayerPosition event ) {
	if( currentMatch != null ) currentMatch.playerPositionEvent( event );
    }

    public void handleEvent( SoccerGoal event ) {
	if( currentMatch != null ) currentMatch.soccerGoalEvent( event );
    }
    // *** Bot command implementations

    public void handleChop( String name, String Message ) {
	for( String x : Tools.cleanStringChopper( Message, ' ' ) ) {
	    m_botAction.sendSmartPrivateMessage( name, x );
	}
    }
    
    private TimerTask positionPinger;
    public void handlePosition( final String name, String message ) {
	if( !opList.isModerator( name ) ) return;
	if( positionPinger != null ) {
	    positionPinger.cancel();
	    positionPinger = null;
	    m_botAction.sendSmartPrivateMessage( name, "Pings turned off." );
	    return;
	}

	positionPinger = new TimerTask() {
		public void run() {
		    	int x, y;
			Player player = m_botAction.getPlayer( name );
			x = player.getXLocation();
			y = player.getYLocation();
			m_botAction.sendSmartPrivateMessage( name, "You are at " + x + "," + y +
							     ", tileX: " + (x/16) + " tileY: " + (y/16) );
		}
	    };
	m_botAction.scheduleTaskAtFixedRate( positionPinger, 2000, 2000 );
    }

    public void handleLock( String name, String message ) {
	if( !opList.isModerator( name) ) return;
	m_botAction.toggleLocked();
	m_botAction.sendSmartPrivateMessage( name, "Lock toggled." );
    }
    
    public void handleHelp( String name, String message ) {
	ArrayList<String[]> helpSections = new ArrayList<String[]>();

	final String[] generalHelp = {
	    "Hello and welcome to the strikeball arena.  Currently available commands are:",
	    "!help - display this dialog.",
	};
	helpSections.add(generalHelp);
	final String[] staffHelp = {
	    "!die - Kills the strikeballbot.",
	    "!ops - Lists the strikeball operators specified in strikeballbot.cfg.",
	};
	final String[] preGameOpHelp = {
	    "!game <team 1 name:team 2 name> - Begin setting up for a manually hosted game of strikeball.",
	    "--- Once a game has been started, use these commands to change the game settings. ---",
	    "!t1 <team 1 name> - Optional.  (Re)Set team 1's name.",
	    "!t2 <team 2 name> - Optional.  (Re)Set team 2's name.",
	    "!goals <number> - Optional.  Set the number of goals to win the game.",
	    "!t1-setcaptain <name> - Set the captain for team 1.",
	    "!t2-setcaptain <name> - Set the captain for team 2.",
	    "!startpick - Begin arranging lineups.",
	    "!killgame - Cancel the setup."
	};
	final String[] inGameOpHelp = {
	    "!t1-setcaptain <name> - Set the captain for team 1.",
	    "!t2-setcaptain <name> - Set the captain for team 2.",
	    "!killgame - Cancel the game.",
	};
	final String[] preGameCapHelp = {
	    "!add <name> - Add a player to your lineup.",
	    "!remove <name> - Remove a player from your lineup.",
	    "!ready - Signal that you're ready to begin.",
	};
	final String[] inGameCapHelp = {
	    "!sub <name1>:<name2> - Substitute name1 with name2.",
	    "!setcaptain <name> - Abdicate the throne!"
	};

	if( opList.isER( name ) || isSBOp( name ) ) {
	    if( currentMatch == null || currentMatch.getState() == SBMatch.NONE ) {
		    helpSections.add( preGameOpHelp );
	    } else {
		switch( currentMatch.getState() ) {
		case SBMatch.STARTING:
		case SBMatch.PICKING:
		case SBMatch.COUNTDOWN:
		case SBMatch.PLAYING:
		    helpSections.add( inGameOpHelp );
		    break;
		case SBMatch.ENDING:
		    break;
		}
	    }
	}
	if( currentMatch != null && currentMatch.isCaptain( name ) ) {
	    switch( currentMatch.getState() ) {
	    case SBMatch.STARTING:
		helpSections.add( preGameCapHelp );
		break;
	    case SBMatch.PICKING:
	    case SBMatch.COUNTDOWN:
		break;
	    case SBMatch.PLAYING:
		helpSections.add( inGameCapHelp );
		break;
	    }
	}
	for( String[] section : helpSections ) {
	    for( String x : section ) {
		m_botAction.sendSmartPrivateMessage( name, x );
	    }
	}
    }
    
    public void handleOps( String name, String message ) {
	if( bigBallers == null || bigBallers.size() == 0 || !opList.isER( name ) ) return;
	m_botAction.sendSmartPrivateMessage( name, "Your friendly Big Ballers are:" );
	for( String x : bigBallers ) {
	    m_botAction.sendSmartPrivateMessage( name, x );
	}
    }
    
    public void handleDie( String name, String message ) {
	if( !opList.isER( name ) ) return;
	m_botAction.die();
    }

    public boolean isNotPlaying( String name ) {
	return notPlayers.contains(name.toLowerCase());
    }
    
    public void handleNotPlaying( String name, String message ) {
	try {
	    if( currentMatch != null && currentMatch.getState() != SBMatch.NONE && currentMatch.isPlayer( name ) )
		throw new Exception( "You're already playing!" );
	    if( isNotPlaying( name ) ) {
		notPlayers.remove(name.toLowerCase());
		m_botAction.sendSmartPrivateMessage( name, "Notplaying disabled.  Captains may now add you to the game.");
		return;
	    } else {
		notPlayers.add(name.toLowerCase());
		m_botAction.sendSmartPrivateMessage( name, "Notplaying enabled.  Captains will be unable to add you to the game." );
		m_botAction.setFreq( name, NOTPLAYINGFREQ );
	    }
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }
    
    // --- Commands that interact with SBMatch
    public void handleGame( String name, String message ) {
	if( !isSBOp( name ) ) return;
	if( currentMatch == null || !currentMatch.isGameInProgress() ) {
	    String[] teamNames;
	    if( message != null ) {
		teamNames = Tools.cleanStringChopper( message, ':' );
		if( teamNames.length == 2 )
		    currentMatch = new SBMatch( m_botAction, teamNames[0], teamNames[1], SBMatch.HOSTED );
		else
		    currentMatch = new SBMatch( m_botAction, SBMatch.HOSTED );
	    } else
		currentMatch = new SBMatch( m_botAction, SBMatch.HOSTED );
	    m_botAction.sendArenaMessage( "A new game of Strikeball is being started.", 2 );
	    m_botAction.toggleLocked();
	    m_botAction.specAll();
	} else {
	    m_botAction.sendSmartPrivateMessage( name, "You must kill the current game first." );
	}
    }

    public void handleT1( String name, String message ) { handleTX( name, message, 0 ); }
    public void handleT2( String name, String message ) { handleTX( name, message, 1 ); }
    private void handleTX( String name, String message, int team ) {
	if( !isSBOp( name ) ) return;
	try {
	    if( currentMatch == null ) throw new Exception( "You must begin a !game first." );
	    currentMatch.setTeamName( message, team );
	    m_botAction.sendArenaMessage( "Team " + team + "'s name set to " + message + "." );
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }


    public void handleGoals( String name, String message ) {
	if( !isSBOp( name ) ) return;
	// note: need to add a sanity check on the value
	try {
	    if( currentMatch == null ) throw new Exception( "You must begin a !game first." );
	    currentMatch.setGoals( Integer.parseInt( message ) );
	    m_botAction.sendArenaMessage("Game set to first to " + message + ".");
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }

    public void handleSetCap1( String name, String message ) { handleSetCapX( name, message, 0 ); }
    public void handleSetCap2( String name, String message ) { handleSetCapX( name, message, 1 ); }
    private void handleSetCapX( String name, String message, int team ) {
	if( !isSBOp( name ) ) return;
	try {
	    if( currentMatch == null ) throw new Exception( "You must begin a !game first." );
	    currentMatch.setCaptain( message, team );
	} catch (Exception e) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }

    public void handleStartPick( String name, String message ) {
	if( !isSBOp( name ) ) return;
	try {
	    if( currentMatch == null ) throw new Exception( "You must start a !game first." );
	    currentMatch.startPick();
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }

    public void handleKillGame( String name, String message ) {
	if( !isSBOp( name ) ) return;
	m_botAction.sendArenaMessage("The game has been brutally killed by " + name + ".", 13 );
	cleanUp();
    }

    public void handleAdd( String name, String message ) {
	try {
	    if( currentMatch == null )
		throw new Exception( "The !add command may only be used by captains during a game." );
	    if( notPlayers.contains( m_botAction.getFuzzyPlayer( message ).getPlayerName().toLowerCase() ) ) 
		throw new Exception( "That person has !notplaying enabled and may not be added." );
	    if(m_botAction.getBotName().equalsIgnoreCase(m_botAction.getFuzzyPlayer(message).getPlayerName()))
		throw new Exception( "Sorry, but I'm too busy running the match to play :(." );
	    currentMatch.addPlayer( name, message );
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }

    public void handleRemove( String name, String message ) {
	try {
	    if( currentMatch == null )
		throw new Exception( "The !remove command may only be used by captains during picking." );
	    currentMatch.removePlayer( name, message );
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );	    
	}
    }

    public void handleReady( String name, String message ) {
	try {
	    if( currentMatch == null )
		throw new Exception( "The !ready command may only be used by captains during picking." );
	    currentMatch.toggleReady( name );
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }

    public void handleSub( String name, String message ) {
	try {
	    if( currentMatch == null )
		throw new Exception( "The !sub command may only be used by captains after the game has begun." );
	    String[] players = Tools.cleanStringChopper( message, ':' );
	    if( players.length != 2 )
		throw new Exception( " Use !sub player1:player2" );
	    if( isNotPlaying( m_botAction.getFuzzyPlayer( players[1] ).getPlayerName().toLowerCase() ) )
		throw new Exception( "That person has notplaying enabled and cannot be added to the game." );
	    currentMatch.substitutePlayer( name, message );
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }

    public void handleLagout( String name, String message ) {
	try {
	    if( currentMatch == null )
		throw new Exception( "The !lagout command can only be used during a match." );
	    currentMatch.lagoutRequest( name );
	} catch( Exception e ) {
	    m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
	}
    }
    // *** The Guts of the automation

    
    
    // *** Utility functions and objects

    private void cleanUp() {
	if( currentMatch != null ) currentMatch.cleanUp();
	currentMatch = null;
    }
    
    public static boolean isSBOp( String name ) {
	if( opList.isER( name ) ) return true;
	if( bigBallers == null ) return false;
	return bigBallers.contains( name.toLowerCase() );
    }

    private class DebugHandler {
	private boolean turnedOn = false;
	private Vector<String> debuggers;
	
	public DebugHandler() {
	    debuggers = new Vector<String>();
	}

	public void addListener( String listener ) {
	}
    }
}