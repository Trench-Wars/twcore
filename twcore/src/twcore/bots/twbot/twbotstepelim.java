/*
 * Created on May 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package twcore.bots.twbot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.TWBotExtension;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;

/**
 * @author Austin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class twbotstepelim extends TWBotExtension {

	public static final int STOPPED = 0;
	public static final int STARTING = 1;
	public static final int RUNNING = 2;
	public static final int BETWEEN_ROUNDS = 3;

	private int	m_state;
	private int m_roundShipType;
	private boolean m_uneven;

	private HashMap m_teamOne;
	private HashMap m_teamTwo;

	public twbotstepelim() {

		m_teamOne = new HashMap();
		m_teamTwo = new HashMap();
		m_state = STOPPED;

		m_uneven = false;
	}

	private void beginNextRound( HashMap _team ) {

		int count = 0;

		Iterator it = _team.keySet().iterator();
		while( it.hasNext() ) {

			String name = (String)it.next();
			m_botAction.setShip( name, m_roundShipType );

			if( m_botAction.getFuzzyPlayerName( name ) != null )
				count++;
		}

		if( (count % 2) != 0 ) {
			m_botAction.sendTeamMessage( "The teams are uneven, the first to pm me will get play to even the teams!!!!" );
			m_uneven = true;
		} else m_uneven = false;

		m_botAction.sendArenaMessage( "The next round begins in 10 seconds!" );
		m_botAction.createNumberOfTeams( 2 );

		TimerTask preStart = new TimerTask() {
			public void run() {

				m_botAction.createNumberOfTeams( 2 );
			}
		};
		m_botAction.scheduleTask( preStart, 9000 );

		TimerTask tenSecondWarning = new TimerTask() {
			public void run() {

				catalogTeams();

				m_state = RUNNING;
				m_botAction.scoreResetAll();
				m_botAction.sendArenaMessage( "Go Go Go!!!", 104 );
			}
		};
		m_botAction.scheduleTask( tenSecondWarning, 10000 );
	}

	private void catalogTeams() {

		m_teamOne.clear();
		m_teamTwo.clear();

		Iterator it = m_botAction.getPlayingPlayerIterator();
		while( it.hasNext() ) {

			Player player = (Player)it.next();
			if( player.getFrequency() == 0 )
				m_teamOne.put( player.getPlayerName(), player.getPlayerName() );
			else
				m_teamTwo.put( player.getPlayerName(), player.getPlayerName() );
		}
	}

	private void checkEndRound( int _freq ) {

		String name = "";

		int playing = 0;
		Iterator it = m_botAction.getPlayingPlayerIterator();

		while( it.hasNext() ) {
			Player p = (Player)it.next();
			if( p.getFrequency() != _freq )
				playing++;

			name = p.getPlayerName();
		}

		if( playing < 1 ) {

			m_state = BETWEEN_ROUNDS;

			//Get the winning frequency
			HashMap winningFreq;
			if( _freq == 0 )
				winningFreq = new HashMap( m_teamOne );
			else
				winningFreq = new HashMap( m_teamTwo );

			//Determine if the event is over
			boolean over = false;
			if( winningFreq.size() == 1 )
				over = true;

			if( over ) {
				m_botAction.sendArenaMessage( "--= " + name + " has won Step-Elim!!! =--", 5 );
				endGame();
			} else {
				m_botAction.sendArenaMessage( "Freq "+_freq+" has won! Prepare for the next round!!!", 5 );
				beginNextRound( winningFreq );
			}
		}
	}

	private void commandStartGame( String _name, String _param ) {

		if( m_state != STOPPED ) {
			m_botAction.sendSmartPrivateMessage( _name, "A game is already in progress." );
			return;
		}

		if( m_botAction.getNumPlayers() < 4 ) {
			m_botAction.sendSmartPrivateMessage( _name, "There are not enough players to begin this event." );
			return;
		}

		//Determine the shiptype for this round of step-elim
		m_roundShipType = 1;
		try {
			m_roundShipType = Integer.parseInt( _param );
		} catch( Exception _e ) { }
		if( m_roundShipType < 1 || m_roundShipType > 8 ) m_roundShipType = 1;

		m_state = STARTING;
		m_botAction.sendSmartPrivateMessage( _name, "A round of step-elim is starting now. I will lock the arena." );


		m_botAction.sendArenaMessage( "Step-Elim is starting. Players are eliminated after 2 deaths. When a team has won it will be split up and game will start over until 1 person stands.", 2 );
		m_botAction.sendArenaMessage( "The arena will be locked in 20 seconds." );

		TimerTask tenSecondWarning = new TimerTask() {
			public void run() {

				//Lock the arena, set everyone to the appropriate ship, and random the teams
				m_botAction.toggleLocked();
				m_botAction.changeAllShips( m_roundShipType );
				m_botAction.createNumberOfTeams( 2 );
				m_botAction.sendArenaMessage( "The game will begin in 10 seconds...", 2 );
			}
		};
		m_botAction.scheduleTask( tenSecondWarning, 20000 );

		TimerTask beginGame = new TimerTask() {
			public void run() {

				catalogTeams();

				m_state = RUNNING;
				m_botAction.scoreResetAll();
				m_botAction.sendArenaMessage( "Go Go Go!!!", 104 );
			}
		};
		m_botAction.scheduleTask( beginGame, 30000 );
	}

	private void endGame() {

		m_state = STOPPED;
		m_botAction.toggleLocked();
	}

	private void handleCommand( String _name, String _message ) {

		if( _message.startsWith( "!start " ) )
			commandStartGame( _name, _message.substring( 7 ) );
	}

    public void handleEvent( Message _event ){

        String message = _event.getMessage();

        if( _event.getMessageType() == Message.PRIVATE_MESSAGE ){

            String name = m_botAction.getPlayerName( _event.getPlayerID() );

            if( m_state == BETWEEN_ROUNDS && m_uneven ) {
            	m_botAction.setShip( name, m_roundShipType );
            	m_uneven = false;
            }

            if( m_opList.isER( name )) handleCommand( name, message );
        }
    }

    public void handleEvent( FrequencyShipChange _event ) {

    	if( m_state != RUNNING ) return;

    	Player player = m_botAction.getPlayer( _event.getPlayerID() );
    	if( m_teamOne.containsKey( player.getPlayerName() ) )
    		checkEndRound( 1 );
    	else
    		checkEndRound( 0 );

    }

    public void handleEvent( PlayerDeath _event ) {

    	if( m_state != RUNNING ) return;

    	Player killed = m_botAction.getPlayer( _event.getKilleeID() );

    	if( killed.getLosses() >= 2 ) {

    		m_botAction.spec( _event.getKilleeID() );
    		m_botAction.spec( _event.getKilleeID() );
    		m_botAction.sendArenaMessage( killed.getPlayerName() + " is out! (" + killed.getWins() + "-"  + killed.getLosses() + ")" );
    		m_botAction.sendSmartPrivateMessage( killed.getPlayerName(), "Stick around, if your team wins you'll get to play in the next round!" );
    		checkEndRound( _event.getKillerID() );
     	}
    }

	/* (non-Javadoc)
	 * @see twcore.bots.twbot.TWBotExtension#getHelpMessages()
	 */
	public String[] getHelpMessages() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see twcore.bots.twbot.TWBotExtension#cancel()
	 */
	public void cancel() {
		// TODO Auto-generated method stub

	}

}
