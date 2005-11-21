/***********************************************************\
|  PowerBallManager - 6.26.03
|   - 2dragons <2dragons@trenchwars.org>
|  
| Goal: To record and allow easy access to powerball related stats.
|
| Stats Recorded (General Powerball Stats):
|	Kills 		: (Checks)
|   TeamKills
|	Deaths		: (Checked)
|	CarryTime 	: (PuckTime)
|	Steals
|	Turnovers
|	Goals
|	Assists

	Shots on goal - goals + saves made by goalie directly after player releases ball 
|	Saves - goalie within a certain area counts STEALS instead as saves. "Steals" that are Saves are not counted as Steals. 
|	PlusMinus - like in real hockey ... if you are in the game when the opponent scores you lose a point (-1). If your team scores, you get a (+1). Goalies do not get the plus minus rating while they are in goalie ships. If you are on a POWERPLAY (you have more players than the other team because one of your players has a penalty), if you score you do not get a +1 (and they don't get a -1) ... but if you score SHORT HANDED (you are the one with less people), the +|- DOES apply 
|	Shots on goal - if the goalie gets a save on your turnover(shot), it counts as a shot on goal rather than a turnover 
|	IceTime - how much time you are physically in the game 
|	GoalsAgainst - how many goals were scored against a goalie while he was in the game as a goalie 
|
|
| RecordType:
|	1 - unique record for each freq change
|	2 - universal record for all freq changes
|	3 - universal record for all freq changes - reset on freq change
\***********************************************************/
 
package twcore.bots.powerballbot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.Player;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SoccerGoal;
import twcore.core.util.Tools;

public class PowerBallManager {
	
	BotAction m_botAction;
	HashMap   m_playerList;
	HashMap	  m_powerballList;
	
	int lastBallReleased = 0;
	boolean shotOnGoal = false;
	
	int m_recordType = 1;
	
	public PowerBallManager( BotAction botAction ) {
		m_botAction = botAction;
		m_playerList = new HashMap();
		m_powerballList = new HashMap();
		
		EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.BALL_POSITION );
        events.request( EventRequester.FREQUENCY_CHANGE );
        events.request( EventRequester.FREQUENCY_SHIP_CHANGE );
        events.request( EventRequester.PLAYER_DEATH );
        events.request( EventRequester.PLAYER_LEFT );
        events.request( EventRequester.SOCCER_GOAL );
	}
	
	public PowerBallManager( BotAction botAction, int recordType) {
		
		m_botAction = botAction;
		m_recordType = recordType;
		m_playerList = new HashMap();
		
		EventRequester events = m_botAction.getEventRequester();
        events.request( EventRequester.BALL_POSITION );
        events.request( EventRequester.FREQUENCY_CHANGE );
        events.request( EventRequester.FREQUENCY_SHIP_CHANGE );
        events.request( EventRequester.PLAYER_DEATH );
        events.request( EventRequester.PLAYER_LEFT );
        events.request( EventRequester.SOCCER_GOAL );
	}
	
	public PowerBallEvent handleEvent( BallPosition event ) {
		
		PowerBallEvent thisEvent = new PowerBallEvent();
		
		try {
		
		//Register Ball ID
		if( !m_powerballList.containsKey( new Integer( event.getBallID() ) ) )
			m_powerballList.put( new Integer( event.getBallID() ), new PowerBall() );
		
		//Get associated powerball
		PowerBall ball = (PowerBall)m_powerballList.get( new Integer( event.getBallID() ) );
		
		//Ball carried
		if( event.getTimeStamp() == 0 ) {
			String name = m_botAction.getPlayerName( event.getPlayerID() );
			//New carrier
			if( ball.setCarried() ) {
				
				Player player = m_botAction.getPlayer( name );
				
				//Set player as having picked up the ball
				PowerBallPlayer thisPlayer = (PowerBallPlayer)m_playerList.get( name );
				getPlayer( name ).setPickUp( event.getBallID() );
				//Check if it was a steal/save
				if( ball.steal( player.getFrequency() ) ) {
					
					//Save/ShotOnGoal
					if( player.getShipType() == 7 || player.getShipType() == 8 && shotOnGoal ) {

						getPlayer( name ).addSave();
						thisEvent.saved( name );
						
						if( ball.getLastCarrier() != null ) {
							getPlayer( ball.getLastCarrier() ).addShotOnGoal();
							thisEvent.shotOnGoal( ball.getLastCarrier() );
						}
					//Steal/Turnover
					} else {

						getPlayer( name ).addSteal();
						if( ball.getLastCarrier() != null )
							getPlayer( ball.getLastCarrier() ).addTurnOver();
					}
				}
				//m_botAction.sendArenaMessage( "Ball Picked Up By: " + name );
			}
			ball.updateCarrierList( name );
		//Ball not carried
		} else {
			
			//New player drop so update the player stats
			if( ball.setDropped() ) {
				try {				
					getPlayer( ball.getLastCarrier() ).endPickUp( event.getBallID() );
				} catch (Exception e) {}
				
				shotOnGoal = intersectsWithGoal( event );
				if( shotOnGoal ) {
					//m_botAction.sendArenaMessage( "Shot Towards Goal" );
				}
				
				lastBallReleased = event.getBallID();
				//m_botAction.sendArenaMessage( "Ball Dropped By: " + ball.getLastCarrier() );	
			}				
		}
		
	} catch (Exception e) { Tools.printStackTrace(e); }
	
		return thisEvent;
	
	}
	
	public void handleEvent( FrequencyChange event ) {
		
		try {
		
		Player player = m_botAction.getPlayer( event.getPlayerID() );
		String name = player.getPlayerName();
		
		if( player.getShipType() == 0 ) return;
		
		PowerBallPlayer thisPlayer = (PowerBallPlayer)m_playerList.get( name );
		if( !thisPlayer.sameFrequency( player.getFrequency() ) ) return;
		
		if( m_recordType == 1 )
			thisPlayer.switchRecord( player.getFrequency() );
		else if( m_recordType == 2 )
			thisPlayer.changeRecordFreq( player.getFrequency() );
		else if( m_recordType == 3 )
			thisPlayer.newRecord( player.getFrequency() ); 
			
		} catch (Exception e) { Tools.printStackTrace(e); }
	}
	
	public void handleEvent( FrequencyShipChange event ) {
		
		try {
		
		Player player = m_botAction.getPlayer( event.getPlayerID() );
		String name = player.getPlayerName();
		
		if( player.getShipType() != 0 ) {
			//New player
			if( !m_playerList.containsKey( name ) )
				m_playerList.put( name, new PowerBallPlayer( player ) );
			//Old player
			else {
				PowerBallPlayer thisPlayer = (PowerBallPlayer)m_playerList.get( name );
				if( !thisPlayer.sameFrequency( player.getFrequency() ) ) return;
				
				if( m_recordType == 1 )
					thisPlayer.switchRecord( player.getFrequency() );
				else if( m_recordType == 2 )
					thisPlayer.changeRecordFreq( player.getFrequency() );
				else if( m_recordType == 3 )
					thisPlayer.newRecord( player.getFrequency() ); 
			}
		} else {
			//Player has left the game, stop adding time to the clock.
		}
		
		} catch (Exception e) { Tools.printStackTrace(e); }
	}
	
	public void handleEvent( PlayerDeath event ) {
		
		Player killer = m_botAction.getPlayer( event.getKillerID() );
		Player killed = m_botAction.getPlayer( event.getKilleeID() );
		
		if( killer.getFrequency() == killed.getFrequency() ) {
			//Teamkill
			getPlayer( killer ).addTeamKill();
		} else {
			//Normal kill
			getPlayer( killer ).addKill();
		}
		getPlayer( killed ).addDeath();
	}
	
	public void handleEvent( PlayerLeft event ) {
	}
	
	//player position?
	
	public PowerBallEvent handleEvent( SoccerGoal event ) {
		
		try {
		
		
		PowerBallEvent thisEvent = new PowerBallEvent();
		
		//Get associated powerball
		PowerBall ball = (PowerBall)m_powerballList.get( new Integer( lastBallReleased ) );
		
		//Get associated scorer and update goals
		String scorer = ball.getLastCarrier();
		int freq = event.getFrequency();
		if( scorer != null ) {
			getPlayer( scorer ).addGoal();
			thisEvent.setScorer( scorer );
		}
		
		//Get associated assistant and if same team adds assist
		String assistant = ball.getTouch(2);
		if( assistant != null ) {
			if( m_botAction.getPlayer( assistant ).getFrequency() == freq ) {
				getPlayer( assistant ).addAssist();
				thisEvent.setAssist( assistant );
			}
		} else {
			ball.reset();
			return thisEvent; 
		}
		
		//Gets associated 2nd assistant and if same team adds assist
		assistant = ball.getTouch(3);
		if( assistant != null ) {
			if( m_botAction.getPlayer( assistant ).getFrequency() == freq ) {
				getPlayer( assistant ).addAssist();
				thisEvent.setSecondAssist( assistant );
			}
		}
		
		ball.reset();
		return thisEvent;
		
		} catch (Exception e) { Tools.printStackTrace(e); return null; }
	}
	
	//weapon fired?
	
	
	/** Returns the appropriate player data record
	 * @param name - name of player to return
	 * @return PlayerStats object containing statistics
	 */
	public PlayerStats getPlayer( String name ) {
		
		//New player
		if( !m_playerList.containsKey( name ) )
			m_playerList.put( name, new PowerBallPlayer( m_botAction.getPlayer(name) ) );

		return ((PowerBallPlayer)m_playerList.get( name )).getPlayer();
	}
	
	/**
	 */	
	public PlayerStats getPlayer( Player player ) {
		
		return getPlayer( player.getPlayerName() );
	}







	public boolean intersectsWithGoal( BallPosition event ) {
		
		if( !(( event.getXVelocity() < 0 && event.getXLocation() / 16 >= 379) || 
		      ( event.getXVelocity() > 0 && event.getXLocation() / 16 <= 644)) ) 
		    return false;
		
		double line_x = ( (double)event.getXLocation() ) / 16;
		double line_y = ( (double)event.getYLocation() ) / 16; 
	
		double run =  ( (double)event.getXVelocity() ) / 16;
		double rise = ( (double)event.getYVelocity() ) / 16;
	
		//sendPublic("*arena line_x = " + (String)line_x);
		//sendPublic("*arena line_y = " + (String)line_y);
	
		//sendPublic("*arena run = " + (String)run);
		//sendPublic("*arena rise = " + (String)rise);
	
		double temp_run = run;
		double temp_rise = rise;
	
		if (run < 0)
			temp_run = -run;
		if (rise < 0)
			temp_rise = -rise;
	
		if( temp_run > temp_rise ) {
			run = run/temp_run;
			rise = rise/temp_run;
		} else if( temp_rise > temp_run ) {
			run = run/temp_rise;
			rise = rise/temp_rise;
		} else {
			run = 1;
			rise = 1;
		}
	
		//sendPublic("*arena run = " + (String)run);
		//sendPublic("*arena rise = " + (String)rise);
	
		int x_boundary;
		int y_boundary;
	
		if( run < 0) x_boundary = 363;
		else x_boundary = 661;
	
		if( rise < 0 ) y_boundary = 443;
		else y_boundary = 580;
	
		boolean hit_wall = false;
		
		while( (int)line_x != x_boundary && (int)line_y != y_boundary ) {
			
			if( isOnGoalMouth( (int)line_x, (int)line_y) )
				return true;
			
			if( !hit_wall && ( (int)line_y <= 448 || (int)line_y >= 575 ) ) {
				rise = -rise;
				hit_wall = true;
			}
	
			line_x += run;
			line_y += rise;
		}
	
		return false;
	}

	public boolean isOnGoalMouth( int x, int y ) {
		if( x == 379 || x == 644 ) {
			if( y >= 501 && y <= 522 )
				return true;
		}
		return false;
	}
	
	//public boolean isSave( Player p ) {
	//	return false;
	//}








class PowerBall {
	
	Vector carrierList;
	boolean carried = false;
	
	public PowerBall() {
		carrierList = new Vector();
	}
	
	public void reset() {
		carrierList.clear();
		carried = false;
	}
	
	public boolean newCarrier( String name ) {
		
		if( carrierList.size() == 0 ) return true;
		else if( !name.equals( carrierList.elementAt(0) ) ) return true;
		else return false;
	}
	
	public void updateCarrierList( String name ) {
		
		Iterator it = carrierList.iterator();
		while( it.hasNext() )
			if( ((String)it.next()).equals( name ) )
				it.remove();
			
		if( carrierList.size() == 0 )
			carrierList.addElement( name );
		else 
			carrierList.insertElementAt( name, 0 );
		
	}
	
	public String getLastCarrier() {
		if( carrierList.size() == 0 ) return null;
		else return (String)carrierList.elementAt(0);
	}
	
	public String getTouch( int t ) {
		if( carrierList.size() < t ) return null;
		else return (String)carrierList.elementAt(t-1);
	}
	
	public boolean setCarried() { 
		boolean oldState = carried;
		carried = true; 
		return !oldState;
	}
	
	public boolean setDropped() { 
		boolean oldState = carried;
		carried = false; 
		return oldState;
	}
	
	public boolean steal( int freq ) {
		if( carrierList.size() == 0 ) return false;
		
		String last = (String)carrierList.elementAt(0);
		
		try {
			if( m_botAction.getPlayer(last).getFrequency() == freq ) return false;
			else return true;
		} catch (Exception e) { return false; }
	}
}

}