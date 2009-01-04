package twcore.bots.multibot.util;

import java.util.HashMap;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * Module for watching streaks.
 * 
 * @author unknown
 */
public class utilstreak extends MultiUtil {

	HashMap<String, String> playerMap;
    HashMap<String, String> negPlayerMap;
	boolean running = false;
    boolean negRunning = false;
	int 	streak = 3, reStreak = 3, bestStreak = 0;
    int     negStreak = 3, negReStreak = 3, negBestStreak = 0;
	String bestStreakOwner;
	String negBestStreakOwner;
	String  sMessages[] = {
		" On Fire!",
		" Killing Spree!",
		" Rampage!",
		" Dominating!",
		" Unstoppable!",
		" God-Like!",
		" Cheater!",
        " Probably Just AFK Killing!"
	};
    String  nsMessages[] = {
            " Struggling!",
            " Hopeless!",
            " Getting Owned!",
            " Pathetic!",
            " Sickening Display of Failure!",
            " Remapped the Fire Key!",
            " Probably High!",
            " AFK!",
        };

	public void init() {
        playerMap = new HashMap<String, String>();
        negPlayerMap = new HashMap<String, String>();
    }

    /**
     * Requests events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.PLAYER_DEATH );
        modEventReq.request(this, EventRequester.PLAYER_LEFT );
    }
    
    public void handleCommand( String name, String message ) {
    	if( message.toLowerCase().startsWith( "!streakoff" ) ) {
    		stopStreak( name );
    	} else if( message.toLowerCase().startsWith( "!negstreakoff" ) ) {
            stopNegStreak( name );
    	} else if( message.toLowerCase().startsWith( "!streak" ) ) {
    		String pieces[] = message.split( " " );
    		try {
    			int i = Integer.parseInt( pieces[1] );
    			if( i < 0 ) i = 1;
    			if( i > 100 ) i = 100;
    			int j = Integer.parseInt( pieces[2] );
    			if( j < 0 ) j = 1;
    			if( j > 100 ) j = 100;
    			startStreak( name, i, j );
    		} catch (Exception e ) { m_botAction.sendPrivateMessage( name, "Could not start streak watching.  Syntax: !streak <start> <update>" ); }
        } else if( message.toLowerCase().startsWith( "!negstreak" ) ) {
            String pieces[] = message.split( " " );
            try {
                int i = Integer.parseInt( pieces[1] );
                if( i < 0 ) i = 1;
                if( i > 100 ) i = 100;
                int j = Integer.parseInt( pieces[2] );
                if( j < 0 ) j = 1;
                if( j > 100 ) j = 100;
                startNegStreak( name, i, j );
            } catch (Exception e ) { m_botAction.sendPrivateMessage( name, "Could not start death streak watching.  Syntax: !negstreak <start> <update>" ); }
        }
    }

    public void startStreak( String name, int i, int j ) {
    	if( !running ) {
    		running = true;
    		streak = i;
    		reStreak = j;
            bestStreak = streak - 1;
            bestStreakOwner = null;
    		m_botAction.sendPrivateMessage( name, "Enabled.  Reporting streaks starting at " + i +" kills, and updating every " + j + " kills thereafter.");
    	} else m_botAction.sendPrivateMessage( name, "Already monitoring arena for streaks! $$$" );
    }

    public void startNegStreak( String name, int i, int j ) {
        if( !negRunning ) {
            negRunning = true;
            negStreak = i;
            negReStreak = j;
            negBestStreak = negStreak - 1;
            negBestStreakOwner = null;
            m_botAction.sendPrivateMessage( name, "Enabled.  Reporting death streaks starting at " + i +" deaths, and updating every " + j + " deaths thereafter.");
        } else m_botAction.sendPrivateMessage( name, "Already monitoring arena for death streaks." );
    }
    
    public void stopStreak( String name ) {
    	if( running ) {
			running = false;
			playerMap.clear();
			m_botAction.sendPrivateMessage( name, "Disabled.  I was getting bored of watching them kill each other anyway." );
		} else m_botAction.sendPrivateMessage( name, "I wasn't looking for streaks; was I supposed to?" );
    }

    public void stopNegStreak( String name ) {
        if( negRunning ) {
            negRunning = false;
            negPlayerMap.clear();
            m_botAction.sendPrivateMessage( name, "Disabled neg. streak watching." );
        } else m_botAction.sendPrivateMessage( name, "I wasn't looking for negative streaks; was I supposed to?" );
    }
    
	public void getBestStreak( String name ) {
		if( running ) {
		    if( bestStreakOwner != null ) {
		        m_botAction.sendPrivateMessage(name, "Best Streak of the Session: "+ bestStreakOwner + " ("+ bestStreak + ":0)");
		    } else {
	            m_botAction.sendPrivateMessage(name, "Best Streak of the Session: none yet!" );		        
		    }
		}
	}

    public void getWorstStreak( String name ) {
        if( negRunning ) {
            if( negBestStreakOwner != null ) {
                m_botAction.sendPrivateMessage(name, "Worst Dying Streak of the Session: "+ negBestStreakOwner + " (0:"+ negBestStreak + ")");
            } else {
                m_botAction.sendPrivateMessage(name, "Worst Dying Streak of the Session: none yet!" );
            }
        }
    }
    
    public void checkStreak( String name, int kills ) {
		String out = null;
		for( int i = 0; i < sMessages.length; i++ ) {
			int count = streak + i * reStreak;
			if( kills == count ) {
				out =  "Streak!: " + name + " (" + kills +":0)" + sMessages[i];
			}
		}
		if (kills > bestStreak) {
			bestStreak = kills;
			bestStreakOwner = name;
			if (out != null) {
				out += " (Best Streak of the Session!)";
			} else {
				out = "Streak!: " + name + " (" + kills +":0) Best Streak of the Session!";
			}
		}
		if (out != null) {
			m_botAction.sendArenaMessage(out);
		}
	}

    public void checkNegStreak( String name, int deaths ) {
        String out = null;
        for( int i = 0; i < nsMessages.length; i++ ) {
            int count = negStreak + i * negReStreak;
            if( deaths == count ) {
                out =  "Death Streak!: " + name + " (0:" + deaths +")" + nsMessages[i];
            }
        }
        if (deaths > negBestStreak) {
            negBestStreak = deaths;
            negBestStreakOwner = name;
            if (out != null) {
                out += " (Worst Death Streak of the Session!)";
            } else {
                out = "Death Streak!: " + name + " (0:" + deaths +") Worst Death Streak of the Session!";
            }
        }
        if (out != null) {
            m_botAction.sendArenaMessage(out);
        }
    }
    
    public void handleEvent( PlayerDeath event ) {
    	if( (running || negRunning) && event.getKilledPlayerBounty() > 0 ) {
	        Player theKiller  = m_botAction.getPlayer( event.getKillerID() );
	        Player theKillee  = m_botAction.getPlayer( event.getKilleeID() );
	        if( theKiller == null || theKillee == null )
	            return;
	        String killer 	  = theKiller.getPlayerName();
	        String killee 	  = theKillee.getPlayerName();
	        
	        if( running ) {
	            if( !playerMap.containsKey( killer) )
	                playerMap.put( killer, "0" );
	            if( playerMap.containsKey( killer ) ) {
	                String ct = playerMap.get( killer );
	                int it = Integer.parseInt( ct );
	                it++;
	                playerMap.remove( killer );
	                playerMap.put( killer, ""+it );
	                checkStreak( killer, it );
	            }
	            if( playerMap.containsKey( killee ) ) {
	                String ct = playerMap.get( killee );
	                int it = Integer.parseInt( ct );
	                if (it == bestStreak && killee.equals(bestStreakOwner)) {
	                    m_botAction.sendArenaMessage("And the best streak of the session ends for "+killee+" ("+it+" kills)!");
	                }
	                playerMap.remove( killee );
	            }
	        }
	        if( negRunning ) {
                if( !negPlayerMap.containsKey( killee ) )
                    negPlayerMap.put( killee, "0" );
                if( negPlayerMap.containsKey( killee ) ) {
                    String ct = negPlayerMap.get( killee );
                    int it = Integer.parseInt( ct );
                    it++;
                    negPlayerMap.remove( killee );
                    negPlayerMap.put( killee, ""+it );
                    checkNegStreak( killee, it );
                }
                if( negPlayerMap.containsKey( killer ) ) {
                    String ct = negPlayerMap.get( killer );
                    int it = Integer.parseInt( ct );
                    if (it == negBestStreak && killer.equals(negBestStreakOwner)) {
                        m_botAction.sendArenaMessage("And the worst dying streak of the session ends for "+killer+" ("+it+" lousy deaths)!");
                    }
                    negPlayerMap.remove( killer );
                }	            
	        }
	  	}
    }
    
    public void handleEvent( PlayerLeft event ){
        Player p = m_botAction.getPlayer( event.getPlayerID() );
        if( p != null ) {
            playerMap.remove( p.getPlayerName() );
            negPlayerMap.remove( p.getPlayerName() );
        }
        
    }
    

    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
			if (message.equalsIgnoreCase("!best"))
				getBestStreak(name);
            if (message.equalsIgnoreCase("!worst"))
                getWorstStreak(name);
			if( m_opList.isER( name ) || m_botAction.getBotName().equalsIgnoreCase(name)) handleCommand( name, message );
        }
    }

    public String[] getHelpMessages() {
        String[] messages = {
            "!streak <start> <update>    - Watches for streaks beginning at <start>, updated every <update>",
            "!streakoff                  - Turns off the streak watcher.",
            "!negstreak <start> <update> - Watches for death streaks beginning at <start>, updated every <update>",
            "!negstreakoff               - Turns off the death streak watcher.",
			"!best                       - Shows the best streak of the session.",
            "!worst                      - Shows the worst death streak of the session."
        };
        return messages;
    }

    public void cancel() {
        playerMap.clear();
    }

}