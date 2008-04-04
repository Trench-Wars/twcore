/*
 * Created on Jan 3, 2005
 *
 *  beginTracking() should be called when stat tracking should start.
 *  No players will be added to the tracking mechanism until beginTracking()
 *  is called. Any player entering during the process of tracking will automatically
 *  be added into the mechanism.
 *
 *  How *watchdamage works:
 *  - when watchdamage is on there are only 2 ways to turn it off
 * 		1. Use *watchdamage again on the player.
 * 		2. The player must leave the arena.
 */
package twcore.bots.statsbot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import twcore.core.BotAction;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;

/**
 * @author 2dragons
 */
public class StatTracker {

	//Static variables to turn the 'playeruptobat' scoreboard on/off
	public static final boolean SCOREBOARD_OFF = false;
	public static final boolean SCOREBOARD_ON = true;

	//Static variables to turn stat tracking on/off
	public static final boolean TRACKING_OFF = false;
	public static final boolean TRACKING_ON = true;

	//Holds our bot action!
	private BotAction	m_botAction;

	//Holds if the 'playeruptobat' scoreboard is enabled
	private boolean 	m_scoreboardEnabled;

	//True if the stat tracker should be tracking statistics
	private boolean		m_trackingStats;

	//Holds a collection of players that are being tracked
	private HashMap<String, PlayerStatistics> m_players;

	public StatTracker( BotAction _botAction, boolean _scoreboardEnabled ) {

		m_botAction 		= _botAction;
		m_scoreboardEnabled = _scoreboardEnabled;

		m_players			= new HashMap<String, PlayerStatistics>();
		m_trackingStats 	= TRACKING_OFF;
	}

	/** Begins statistical tracking for anyone in the arena. Will track any player
	 * that jumps in during the course of stat tracking.
	 */
	public void beginTracking() {

		//turn tracking on
		m_trackingStats = TRACKING_ON;

		//iterate through the players in game and add them to the tracking mechanism
		//watchdamage will be on for new players
		Iterator<Player> playing = m_botAction.getPlayingPlayerIterator();

		while( playing.hasNext() ) {

			Player player = (Player)playing.next();
			ensurePlayer( player );
		}
	}

	public void endTracking() {

		try {
			PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( "StatsLog.txt", true )));

			m_trackingStats = TRACKING_OFF;

			//stop watchdamage
			Iterator<String> playList = m_players.keySet().iterator();
			while( playList.hasNext() ) {

				String name = (String)playList.next();
				PlayerStatistics playerStats = getPlayer( name );
				out.println( playerStats.toString() );
				m_botAction.sendUnfilteredPrivateMessage( name, "*watchdamage" );
			}

			m_players.clear();

			out.println( "---------------------------------------------" );
			out.close();
		} catch (Exception e) {

		}
	}

	public void spamStats() {

		Iterator<PlayerStatistics> playList = m_players.values().iterator();
		while( playList.hasNext() ) {

			PlayerStatistics stats = (PlayerStatistics)playList.next();
			m_botAction.sendTeamMessage( stats.toString() );
		}
	}

	public void reset() {

		m_trackingStats = TRACKING_OFF;
		m_players.clear();
	}

	/** Ensures a player is in stored for tracking if tracking is enabled
	 * @param _player A player object to check
	 */
	private void ensurePlayer( Player _player ) {

		String name = _player.getPlayerName();
		int    ship = _player.getShipType();
		int    freq = _player.getFrequency();

		//Add the player to be tracked if not present in collection
		if( !m_players.containsKey( name ) ) {

			PlayerStatistics playerStats = new PlayerStatistics( name, ship, freq );
			m_players.put( name, playerStats );

			m_botAction.sendUnfilteredPrivateMessage( name, "*watchdamage" );
		}
	}

	/** Gets a PlayerStatistics object based off a player name
	 * Assumed this is only called when tracking is ON
	 * @param _name The name of the player
	 * @return A PlayerStatistics object
	 */
	private PlayerStatistics getPlayer( String _name ) {

		return m_players.get( _name );
	}

	private boolean trackingPlayer( String _name ) {

		return m_players.containsKey( _name );
	}

	public void spamStats( String _name ) {

		if( trackingPlayer( _name ) ) {
			PlayerStatistics playerStats = getPlayer( _name );
			m_botAction.sendTeamMessage( playerStats.toString() );
		} else
			m_botAction.sendTeamMessage( "ERR" );
	}

	public void handleEvent( Message _event ) {

		//Don't ignore message events, but only process arena messages
		if( _event.getMessageType() != Message.ARENA_MESSAGE ) return;

	}

	public void handleEvent( PlayerDeath _event ) {

		//Ignore event if tracking is disabled
		if( m_trackingStats == TRACKING_OFF ) return;

		//Get the player associated with this event
		Player killer = m_botAction.getPlayer( _event.getKillerID() );
		Player killed = m_botAction.getPlayer( _event.getKilleeID() );

		//Get the player stats associated with each player
		PlayerStatistics killerStats = getPlayer( killer.getPlayerName() );
		PlayerStatistics killedStats = getPlayer( killed.getPlayerName() );

		//Distribute mini events
		if( killer.getFrequency() == killed.getFrequency() ) {

			//team kill
			killerStats.processTeamKill();
			killedStats.processTeamDeath();
		} else {

			//normal kill
			killerStats.processKill( _event.getKilledPlayerBounty() );
			killedStats.processDeath( _event.getKilledPlayerBounty() );
		}

	}

	public void handleEvent( FrequencyChange _event ) {

		//Ignore event if tracking is disabled
		if( m_trackingStats == TRACKING_OFF ) return;

		//Get the player associated with this event
		Player player = m_botAction.getPlayer( _event.getPlayerID() );

		//Ensures the player is stored for tracking
		if( player.getShipType() != 0 ) {
			ensurePlayer( player );
		}
	}

	public void handleEvent( FrequencyShipChange _event ) {

		//Ignore event if tracking is disabled
		if( m_trackingStats == TRACKING_OFF ) return;

		//Get the player associated with this event
		Player player = m_botAction.getPlayer( _event.getPlayerID() );

		//Ensures the player is stored for tracking
		if( player.getShipType() != 0 ) {
			ensurePlayer( player );
			PlayerStatistics playerStats = getPlayer( player.getPlayerName() );
			if( !playerStats.isWatchingDamage() ) {

				m_botAction.sendUnfilteredPrivateMessage( player.getPlayerName(), "*watchdamage" );
			}
		}
	}

	public void handleEvent( WatchDamage _event ) {

		//Ignore event if tracking is disabled
		if( m_trackingStats == TRACKING_OFF ) return;

		//Get the players associated with this event
		Player attacker = m_botAction.getPlayer( _event.getAttacker() );
		Player victim	= m_botAction.getPlayer( _event.getVictim() );

		//Get the player stats associated with each player
		PlayerStatistics attackerStats = getPlayer( attacker.getPlayerName() );
		PlayerStatistics victimStats = getPlayer( victim.getPlayerName() );

		int energyLost = _event.getEnergyLost();

		attackerStats.processDamageDealt( energyLost );
		victimStats.processDamageTaken( energyLost );
	}

	public void handleEvent( WeaponFired _event ) {

		//Ignore event if tracking is disabled
		if( m_trackingStats == TRACKING_OFF ) return;

		//Get the player associated with this event
		Player player = m_botAction.getPlayer( _event.getPlayerID() );

		PlayerStatistics playerStats = getPlayer( player.getPlayerName() );

		playerStats.processShot();

	}

	public void handleEvent( FlagClaimed _event ) {

		//Ignore event if tracking is disabled
		if( m_trackingStats == TRACKING_OFF ) return;

		//Get the player associated with this event
		Player player = m_botAction.getPlayer( _event.getPlayerID() );
		PlayerStatistics playerStats = getPlayer( player.getPlayerName() );

		playerStats.processFlagTouch();
	}

	/** This event is tracked only to know if watchdamage is turned off
	 * @param _event PlayerLeft event
	 */
	public void handleEvent( PlayerLeft _event ) {

		//Ignore event if tracking is disabled
		if( m_trackingStats == TRACKING_OFF ) return;

		//Get the players name
		String name = m_botAction.getPlayerName( _event.getPlayerID() );

		//Only process this event if the player was being tracked
		if( trackingPlayer( name ) ) {

			PlayerStatistics playerStats = getPlayer( name );
			playerStats.setWatchDamage( false );
		}
	}
}
