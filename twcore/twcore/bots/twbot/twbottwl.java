// 7/8/03
//  - !host : assigns sender as host of the match
//  - checks if a player is banned from the league before they can be added/subbed
//  - scoreresets subs
//  - on unload module resets current setup

package twcore.bots.twbot;

import java.sql.*;
import java.util.*;
import java.text.*;
import twcore.core.*;
import twcore.misc.database.DBPlayerData;

public class twbottwl extends TWBotExtension {

	final String 	mySQLHost	= "website";
	LeagueMatch 	m_match;
	int 			gameState 	= 0;
	int				secondsOnStart = 0;
	String			timeStart;
	String			timeEnd;
	int				generalTime = 0;
	HashMap			laggers;
	int				watch;
	Objset			myObjects;
	//0 - off
	//1 - match loaded
	//2 - lineups requested
	//3 - 30 seconds till start
	//4 - game in progress
    java.util.Date lockDate;
    java.util.Date lastRoundCutoffDate;

	public twbottwl() {
		laggers = new HashMap();
    }

    //TEMPORARY///
    public void do_loadTestGame( String name, String message ) {
    	do_loadGame( name, "- "+5852 );
    }

    ///Loads a match///
   	public void do_loadGame( String name, String message ) {
		if( gameState != 0 ) {
			m_botAction.sendPrivateMessage( name, "Cannot load game, a game is already running or loaded. If a game hasn't started use !unloadgame" );
			return;
		}
		String pieces[] = message.split( " " );
		try {
			int matchId = Integer.parseInt( pieces[1] );
			if( !sql_loadGame( matchId ) )
				m_botAction.sendPrivateMessage( name, "Unable to load the specified match." );
			else {
				sql_deleteOldSaveStates( matchId );
				m_botAction.sendPrivateMessage( name, "Match #" + matchId + " has been loaded" );
				m_botAction.sendArenaMessage( m_match.getMatchType() + " " + m_match.getMatchId() + ": "+  m_match.getTeam1Name() + " vs " + m_match.getTeam2Name() + " loaded", 26);
				gameState = 1;
			}
			lockDate = sql_getLockDate();
        	lastRoundCutoffDate = sql_getLastRoundCutoffDate();
		} catch (Exception e) {
			m_botAction.sendPrivateMessage( name, "Please use '!loadgame <match #>' " );
		}
	}

	public void do_reloadGame( String name, String message ) {
		if( gameState != 0 ) {
			m_botAction.sendPrivateMessage( name, "Cannot load game, a game is already running or loaded. If a game hasn't started use !unloadgame" );
			return;
		}
		String pieces[] = message.split( " " );
		try {
			int matchId = Integer.parseInt( pieces[1] );
			if( !sql_reloadGame( matchId ) ) {
				m_botAction.sendPrivateMessage( name, "Unable to reload the specified match." );
				return;
			}
			gameState = 3;
			m_match.setRef( name );
			m_botAction.toggleLocked();
			m_botAction.specAll();
			m_botAction.setDoors( 255 );
			m_botAction.setTimer( 0 );
			m_botAction.setMessageLimit( 3 );
			m_botAction.sendPrivateMessage( name, "Match #" + matchId + " has been loaded for RESUMED PLAY" );
			m_botAction.sendArenaMessage( m_match.getMatchType() + " " + m_match.getMatchId() + ": "+  m_match.getTeam1Name() + " vs " + m_match.getTeam2Name() + " loaded for resumed play", 26);
			sql_reloadPlayerStates( matchId );
			TimerTask restart = new TimerTask() {
				public void run() {
				    int team1Score;
			    	int team2Score;
			    	String mvp;
			    	if( m_match.getMatchTypeId() == 3 ) {
						team1Score = m_match.getTeam1Score();
						team2Score = m_match.getTeam2Score();
						if( team1Score > team2Score ) mvp = m_match.getScoreMVP( 1 );
						else mvp = m_match.getScoreMVP( 2 );
					} else {
						team1Score = m_match.getTeam2Deaths();
						team2Score = m_match.getTeam1Deaths();
						if( team1Score > team2Score ) mvp = m_match.getMVP( 1 );
						else mvp = m_match.getMVP( 2 );
					}
					if( m_match.getMatchTypeId() < 3 )
						m_match.displayScores( team1Score, team2Score, mvp, false );
				    else
						m_match.displayBaseScores( team1Score, team2Score, mvp, false );
					m_botAction.sendArenaMessage( "Game RESTARTS in 30 seconds." );
				}
			};
			m_botAction.scheduleTask( restart, 10000 );


			TimerTask timerStartGame = new TimerTask() {
			public void run() {
				secondsOnStart = (int)(System.currentTimeMillis()/1000);
				m_botAction.resetFlagGame();
				m_botAction.scoreResetAll();
				m_botAction.shipResetAll();
				m_botAction.setDoors( 0 );
				m_botAction.sendArenaMessage( "GO GO GO", 104 );
				if( m_match.getMatchTypeId() == 3 ) {
					m_botAction.warpFreqToLocation( 0, 486, 256 );
					m_botAction.warpFreqToLocation( 1, 538, 256 );
				}
				m_botAction.showObject( 52 );
				int minutes = Integer.parseInt( m_match.getRestartTime().split( ":" )[0] );
				int seconds = Integer.parseInt( m_match.getRestartTime().split( ":" )[1] );
				generalTime = minutes*60 + seconds;
				gameState = 4;
				Calendar thisTime = Calendar.getInstance();
	    		java.util.Date day = thisTime.getTime();
				timeStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( day );
				}
			};

			TimerTask timerStart = new TimerTask() {
				public void run() {
					int minutes = Integer.parseInt( m_match.getRestartTime().split( ":" )[0] );
					m_botAction.setTimer( minutes );
				}
			};
			m_botAction.scheduleTask( timerStart, 40000 + Integer.parseInt( m_match.getRestartTime().split( ":" )[1] )*1000 );

			TimerTask timerDoors = new TimerTask() {
				public void run() {
					m_botAction.setDoors( 255 );
				}
			};
			m_botAction.scheduleTask( timerDoors, 50000 );
			m_botAction.scheduleTask( timerStartGame, 40000 );

			TimerTask timer3 = new TimerTask() {
				public void run() {
				m_botAction.sendArenaMessage( "3" );
				}
			};
			m_botAction.scheduleTask( timer3, 37000 );

			TimerTask timer2 = new TimerTask() {
				public void run() {
				m_botAction.sendArenaMessage( "2" );
				}
			};
			m_botAction.scheduleTask( timer2, 38000 );

			TimerTask timer1 = new TimerTask() {
				public void run() {
				m_botAction.sendArenaMessage( "1" );
				}
			};
			m_botAction.scheduleTask( timer1, 39000 );

			TimerTask watchPlayers = new TimerTask() {
				public void run() {
					String name = m_match.getNextPlayer();
					m_botAction.spectatePlayer( name );
				}
			};
			m_botAction.scheduleTaskAtFixedRate( watchPlayers, 2000, 2000 );

			TimerTask updateScores = new TimerTask() {
				public void run() {
					do_updateScoreBoard();
				}
			};
			m_botAction.scheduleTaskAtFixedRate( updateScores, 2000, 1000 );

			TimerTask saveState = new TimerTask() {
				public void run() {
					m_botAction.sendUnfilteredPublicMessage( "?time" );
				}
			};
			m_botAction.scheduleTaskAtFixedRate( saveState, 102000, 10000 );
		} catch (Exception e) {}
	}

	public void do_unloadGame( String name, String message ) {
		if( gameState != 1 ) {
			m_botAction.sendPrivateMessage( name, "Can only unload a game if one is loaded or before !startpick" );
			return;
		}
		m_match = null;
		gameState = 0;
		m_botAction.sendPrivateMessage( name, "Game unloaded." );
	}

	public void do_zoneGame( String name, String message ) {
		if( gameState == 0 ) {
			m_botAction.sendPrivateMessage( name, "A match must first be loaded before you can zone." );
			return;
		}
		if( gameState != 1 ) {
			m_botAction.sendPrivateMessage( name, "You can only zone after you have first loaded a match." );
			return;
		}
		if( m_match.haveZoned() ) {
			m_botAction.sendPrivateMessage( name, "You may only zone one time per game." );
			return;
		}
		m_match.zoned();
		m_botAction.sendZoneMessage( "TWL Season 6 : "+m_match.getMatchType() + " - " +m_match.getTeam1Name() + " vs. " + m_match.getTeam2Name() + " Type ?go " + m_botAction.getArenaName() );
	}

	public void do_startPick( String name, String message ) {
		if( gameState < 1 ) {
			m_botAction.sendPrivateMessage( name, "You must load a match before you can set the teams." );
			return;
		}
		if( gameState > 1 ) {
			m_botAction.sendPrivateMessage( name, "You can only set teams before the game begins." );
			return;
		}
		m_botAction.sendArenaMessage( "Captains you have approximately 10 minutes to submit your lineups to - " + name );
		m_botAction.setTimer( 10 );
		m_match.setRef( name );
		gameState = 2;
		m_botAction.toggleLocked();
		m_botAction.specAll();
		m_botAction.setDoors( 0 );
	}

    public void do_addPlayer( String name, String message, boolean forced ) {
		if( gameState < 2 ) {
			m_botAction.sendPrivateMessage( name, "You must load a game and !startpick before you can add players." );
			return;
		}
		if( gameState > 2 && !forced ) {
			if( generalTime < 1500 || gameState != 4 ) {
				m_botAction.sendPrivateMessage( name, "Players may only be added before the game starts." );
				return;
			}
		}

		String pieces[] = message.split( ":" );
		String player = message;
		if( pieces.length == 2 ) player = pieces[0];
		//Sets ship type
		int shipType = 3;
		try {
			shipType = Integer.parseInt( pieces[1] );
		} catch (Exception e) {}
		if( shipType < 1 || shipType > 8 ) shipType = m_match.getMatchTypeId();
		if( !m_match.variableShips() ) shipType = m_match.getMatchTypeId();
		//Gets player/teamId
		player = m_botAction.getFuzzyPlayerName( player );
		int playerTeamId = sql_getPlayersTeam( player );
        java.util.Date dateJoined = sql_getJoinDate( player );

		if( player == null ) {
			m_botAction.sendPrivateMessage( name, "Unable to add player, could not find player." );
			return;
		}
		if( !m_match.isTeamMember( playerTeamId ) && !forced ) {
			m_botAction.sendPrivateMessage( name, "Unable to add player, " + player + " is not rostered for either team." );
			return;
		}
        if( dateJoined.after( lockDate ) && !forced ) {
	    	m_botAction.sendPrivateMessage( name, "Unable to add player, " + player + " was rostered after the cutoff date." );
	    	return;
        }
        if( dateJoined.after( lastRoundCutoffDate ) && !forced ) {
	        m_botAction.sendPrivateMessage( name, "Unable to add player, " + player + " must be rostered for at least 1 round to play." );
	        return;
        }
        if( sql_isBanned( player ) ) {
        	m_botAction.sendPrivateMessage( name, "Unable to add player, " + player + " has been banned from this league." );
        	return;
        }
		if( m_match.rosterLimitMet( playerTeamId ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to add player, have reached the player limit for this game." );
			return;
		}
		if( m_match.hasPlayer( player ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to add player, that player is already in game." );
			return;
		}
		if( m_match.shipLimitMet( shipType, playerTeamId ) && m_match.getMatchTypeId() == 3 ) {
			m_botAction.sendPrivateMessage( name, "Unable to add player, this team has reached the ship type limit for this ship." );
			return;
		}
		//Should be good at this point, add the player. Lag check???
		m_match.addPlayer( playerTeamId, player, shipType );
		if( !forced && gameState != 4 ) {
			m_botAction.sendSquadMessage( m_match.getTeamName( player ), player + " has been placed in ship " + shipType );
			m_botAction.sendPrivateMessage( name, player + " has been placed in for " + m_match.getTeamName( player ) + " in ship " + shipType );
		} else {
			m_botAction.setShip( player, m_match.getPlayer( player ).getShip() );
			m_botAction.setFreq( player, m_match.getPlayer( player ).getFreq() );
			m_botAction.sendArenaMessage( player + " in for " + m_match.getTeamName( player ) );
		}
	}

	public void do_removePlayer( String name, String message ) {
		if( gameState < 2 ) {
			m_botAction.sendPrivateMessage( name, "You must load a game and !startpick before you can remove players." );
			return;
		}
		if( gameState > 2 ) {
			m_botAction.sendPrivateMessage( name, "Players may only be removed before the game starts." );
			return;
		}
		String player;
		if( m_match.hasPlayer( message ) ) player = message;
		else player = m_botAction.getFuzzyPlayerName( message );
		if( player == null ) {
			m_botAction.sendPrivateMessage( name, "Unable to remove player " + message + ", could not find player, if the player is not in the arena please try using the full name." );
			return;
		}
		if( !m_match.hasPlayer( player ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to remove player " + player + ", that player is not registered for this game." );
			return;
		}
		m_match.removePlayer( player );
		m_botAction.sendPrivateMessage( name, player + " has been removed." );
		m_botAction.sendSquadMessage( m_match.getTeamName( player ), player + " has been removed." );
	}

	public void do_startGame( String name, String message ) {
		if( gameState < 2 ) {
			m_botAction.sendPrivateMessage( name, "A game must be loaded before a game can start!" );
			return;
		}
		if( gameState > 2 ) {
			m_botAction.sendPrivateMessage( name, "There is a game currently running." );
			return;
		}
		boolean team1 = false, team2 = false;
		if( m_match.getMatchTypeId() < 3 ) {
			if( m_match.getTeam1PlayerCount() < 3 ) team1 = true;
			if( m_match.getTeam2PlayerCount() < 3 ) team2 = true;
		} else {
			if( m_match.getTeam1PlayerCount() < 5 ) team1 = true;
			if( m_match.getTeam2PlayerCount() < 5 ) team2 = true;
		}
		if( team1 && team2 ) {
			sql_storeForfeitResults( 0 );
			m_botAction.sendArenaMessage( "Both teams do not meet the minimum lineup requirement so both FORFEIT." );
		} else if( team1 ) {
			sql_storeForfeitResults( 2 );
			m_botAction.sendArenaMessage( m_match.getTeam1Name() + " forfeits to " + m_match.getTeam2Name() + " for not meeting the minimum lineup requirement" );
		} else if( team2 ) {
			sql_storeForfeitResults( 1 );
			m_botAction.sendArenaMessage( m_match.getTeam2Name() + " forfeits to " + m_match.getTeam1Name() + " for not meeting the minimum lineup requirement" );
		}
		if( team1 || team2 ) {
			gameState = 0;
			m_botAction.toggleLocked();
			m_botAction.cancelTasks();
			m_botAction.toggleBlueOut();
			m_botAction.sendUnfilteredPublicMessage( "*lockspec" );
			m_botAction.setMessageLimit( 0 );
			m_botAction.setTimer( 0 );
			m_match = null;
			generalTime = 0;
			return;
		}
		//Current roster check needs to be taken...

		m_botAction.setMessageLimit( 3 );
		gameState = 3;
		m_botAction.setDoors( 255 );
		m_botAction.setTimer( 0 );
		m_match.placePlayersInGame();

		//Warp players and send arena to start game.
		TimerTask warpPlayers = new TimerTask() {
			public void run() {
				m_match.warpAllPlayers();
				m_botAction.sendArenaMessage( "Game starts in 30 seconds", 2 );
			}
		};
		m_botAction.scheduleTask( warpPlayers, 5000 );

		TimerTask timerStartGame = new TimerTask() {
			public void run() {
				secondsOnStart = (int)(System.currentTimeMillis()/1000);
				m_botAction.resetFlagGame();
				m_botAction.scoreResetAll();
				m_botAction.shipResetAll();
				m_botAction.setDoors( 0 );
				m_botAction.sendArenaMessage( "GO GO GO", 104 );
				if( m_match.getMatchTypeId() == 3 ) {
					m_botAction.warpFreqToLocation( 0, 486, 256 );
					m_botAction.warpFreqToLocation( 1, 538, 256 );
				}
				m_botAction.showObject( 52 );
				m_botAction.setTimer( 30 );
				gameState = 4;
				Calendar thisTime = Calendar.getInstance();
	    		java.util.Date day = thisTime.getTime();
				timeStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( day );
				generalTime = 1800;
				sql_startGame();
			}
		};

		TimerTask timerDoors = new TimerTask() {
			public void run() {
				m_botAction.setDoors( 255 );
			}
		};
		m_botAction.scheduleTask( timerDoors, 45000 );
		m_botAction.scheduleTask( timerStartGame, 35000 );

		TimerTask timer3 = new TimerTask() {
			public void run() {
			m_botAction.sendArenaMessage( "3" );
			}
		};
		m_botAction.scheduleTask( timer3, 32000 );

		TimerTask timer2 = new TimerTask() {
			public void run() {
			m_botAction.sendArenaMessage( "2" );
			}
		};
		m_botAction.scheduleTask( timer2, 33000 );

		TimerTask timer1 = new TimerTask() {
			public void run() {
			m_botAction.sendArenaMessage( "1" );
			}
		};
		m_botAction.scheduleTask( timer1, 34000 );

		TimerTask watchPlayers = new TimerTask() {
			public void run() {
				String name = m_match.getNextPlayer();
				m_botAction.spectatePlayer( name );
			}
		};
		m_botAction.scheduleTaskAtFixedRate( watchPlayers, 2000, 3000 );

		TimerTask updateScores = new TimerTask() {
			public void run() {
				do_updateScoreBoard();
			}
		};
		m_botAction.scheduleTaskAtFixedRate( updateScores, 2000, 1000 );

		TimerTask saveState = new TimerTask() {
			public void run() {
				m_botAction.sendUnfilteredPublicMessage( "?time" );
			}
		};
		m_botAction.scheduleTaskAtFixedRate( saveState, 37000, 10000 );
	}

	public void do_toggleBlueOut( String name ) {
		if( gameState < 4 ) return;
		m_match.toggleBlueOut();
		m_botAction.toggleBlueOut();
		m_botAction.sendUnfilteredPublicMessage( "*lockspec" );
		if( m_match.blueOut() )
			m_botAction.sendArenaMessage( "Blueout has been enabled." );
		else
			m_botAction.sendArenaMessage( "Blueout has been disabled." );
	}

	public void do_updateScoreBoard() {
		myObjects.hideAllObjects();
		generalTime -= 1;
		String team1Score;
    	String team2Score;
    	if( m_match.getMatchTypeId() == 3 ) {
			team1Score = ""+m_match.getTeam1Score();
			team2Score = ""+m_match.getTeam2Score();
		} else {
			team1Score = ""+m_match.getTeam2Deaths();
			team2Score = ""+m_match.getTeam1Deaths();
		}
		for( int i = team1Score.length()-1; i > -1; i-- )
			myObjects.showObject( Integer.parseInt( ""+team1Score.charAt(i) ) + 200 + (team1Score.length()-1-i)*10 );
		for( int i = team2Score.length()-1; i > -1; i-- )
			myObjects.showObject( Integer.parseInt( ""+team2Score.charAt(i) ) + 100 + (team2Score.length()-1-i)*10 );
	    if( generalTime >= 0 ) {
	    	int seconds = generalTime % 60;
	    	int minutes = (generalTime - seconds)/60;
	    	myObjects.showObject( 730 + (int)((minutes - minutes % 10)/10) );
	    	myObjects.showObject( 720 + (int)( minutes % 10) );
	    	myObjects.showObject( 710 + (int)((seconds - seconds % 10)/10) );
	    	myObjects.showObject( 700 + (int)( seconds % 10) );
	    }
	    do_showTeamNames( m_match.getTeam1Name(), m_match.getTeam2Name() );
		m_botAction.setObjects();

	}

	public void do_showTeamNames( String n1, String n2) {
            n1 = n1.toLowerCase();
            n2 = n2.toLowerCase();
            int i;
            String s1 = "", s2 = "";

            for (i=0; i < n1.length(); i++)
                if ((n1.charAt(i) >= 'a') && (n1.charAt(i) <= 'z') && (s1.length() < 5)) s1 = s1 + n1.charAt(i);

            for (i=0; i < n2.length(); i++)
                if ((n2.charAt(i) >= 'a') && (n2.charAt(i) <= 'z') && (s2.length() < 5)) s2 = s2 + n2.charAt(i);

            show_string(s1, 0, 30);

            show_string(s2, 5, 30);
    }

    public void show_string(String new_n,  int pos_offs, int alph_offs) {
        int i, t;
        char to;

        for (i=0; i < new_n.length(); i++) {
            t = new Integer(Integer.toString(((new_n.getBytes()[i])-97)+alph_offs)+ Integer.toString(i+pos_offs)).intValue();
            myObjects.showObject( t );
        }

    }

	//Subs one player for another
	public void do_subPlayer( String name, String message ) {
		if( gameState < 4 ) {
			m_botAction.sendPrivateMessage( name, "A game is not running or has not started." );
			return;
		}
		int teamId = sql_getPlayersTeam( name );
		boolean isCap = sql_isCap( name );
		if( !name.equals( m_match.getRef() ) ) {
			if( !m_match.isTeamMember( teamId ) || !isCap ) {
				m_botAction.sendPrivateMessage( name, "You are not captain for either team." );
				return;
			}
		}
		String pieces[] = message.split( ":" );
		if( pieces.length != 2 ) {
			m_botAction.sendPrivateMessage( name, "Please use    !sub <playerout>:<playerin>" );
			return;
		}
		String subOut = m_botAction.getFuzzyPlayerName( pieces[0] );
		String subIn =  m_botAction.getFuzzyPlayerName( pieces[1] );
		if( subOut == null ) subOut = m_match.getClosestMatch( pieces[0] );
		
		if( !m_match.hasPlayer( subOut ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to sub out player " + subOut + ", that player is not registered for this game." );
			return;
		}
		if( m_match.getTeamId( subOut ) != teamId && !name.equals( m_match.getRef() ) ) {
			m_botAction.sendPrivateMessage( name, "You can only sub players on your own team." );
			return;
		}
		if( subIn == null ) {
			m_botAction.sendPrivateMessage( name, "Unable to sub in player " + pieces[1] +", unable to find player." );
			return;
		}
		int subInTeamId = sql_getPlayersTeam( subIn );
		if( subInTeamId != m_match.getTeamId( subOut ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to sub in player " + subIn + ", that player is not rostered for the team. " );
			return;
		}
        java.util.Date dateJoined = sql_getJoinDate( subIn );
        if(dateJoined.after( lockDate ) ) {
            m_botAction.sendPrivateMessage( name, "Unable to add player, " + subIn + " was rostered after the cutoff date." );
            return;
        }
        if(dateJoined.after( lastRoundCutoffDate ) ) {
            m_botAction.sendPrivateMessage( name, "Unable to add player, " + subIn + " must be rostered for at least 1 round to play." );
            return;
        }
        if( sql_isBanned( subIn ) ) {
        	m_botAction.sendPrivateMessage( name, "Unable to add player, " + subIn + " has been banned from this league." );
        	return;
        }
		if( m_match.hasPlayer( subIn ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to sub in player " + subIn + ", that player has already played or is in game." );
			return;
		}
		if( m_match.subLimitReached( subInTeamId ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to sub in player, sub limit has been reached." );
			return;
		}
		if( m_match.hasPlayer( subIn ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to sub in player, player has already played." );
			return;
		}
		if( m_match.getPlayer( subOut ).isOutOfGame() ) {
			m_botAction.sendPrivateMessage( name, "Unable to sub out player, player is already out of the game." );
			return;
		}
		int lives = m_match.subPlayer( subOut, subIn );
		if( m_match.getMatchTypeId() != 3 )
			m_botAction.sendArenaMessage( subOut + " subbed by " + subIn + " with " + lives + " lives");
		else
			m_botAction.sendArenaMessage( subOut + " subbed by " + subIn );

		if( laggers.containsKey( subOut ) ) {
			Lagger l = (Lagger)laggers.get( subOut );
			l.cancel();
			laggers.remove( subOut );
		}
	}

	//Switches player ships
	public void do_switchPlayer( String name, String message ) {
		if( gameState < 4 ) {
			m_botAction.sendPrivateMessage( name, "Switches can only be done during the game." );
			return;
		}
		int teamId = sql_getPlayersTeam( name );
		boolean isCap = sql_isCap( name );
		if( !m_opList.isER( name ) ) {
			if( !m_match.isTeamMember( teamId ) || !isCap ) {
				m_botAction.sendPrivateMessage( name, "You are not captain for either team." );
				return;
			}
		}
		if( m_match.getMatchTypeId() != 3 ) {
			m_botAction.sendPrivateMessage( name, "Switches can only be done in basing matches." );
			return;
		}
		String pieces[] = message.split( ":" );
		if( pieces.length != 2 ) {
			m_botAction.sendPrivateMessage( name, "Unable to switch players: Improper usage of !switch <player1>:<player2>" );
			return;
		}
		pieces[0] = m_botAction.getFuzzyPlayerName( pieces[0] );
		pieces[1] = m_botAction.getFuzzyPlayerName( pieces[1] );
		if( pieces[0].equals( pieces[1] ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to switch players: Must specify 2 distinct players." );
			return;
		}
		if( m_match.getTeamId( pieces[0] ) != m_match.getTeamId( pieces[1] ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to switch players: Players must be on the same team." );
			return;
		}
		if( m_match.getTeamId( pieces[0] ) != m_match.getTeamId( pieces[1] ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to switch players: Players must be on the same team." );
			return;
		}
		if( teamId != m_match.getTeamId( pieces[0] ) && !name.equals( m_match.getRef() ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to switch players: You are not a captain of a team." );
			return;
		}
		if( m_match.switchLimitReached( m_match.getTeamId( pieces[0] ) ) ) {
			m_botAction.sendPrivateMessage( name, "Unable to switch players: Switch limit reached." );
			return;
		}
		m_match.switchPlayers( pieces[0], pieces[1] );
	}

    public void do_listPlayers( String name, String message ) {
		if( gameState == 0 ) return;
		if( name.equals( m_match.getRef() ) ) {
			m_botAction.sendPrivateMessage( name, m_match.getTeam1Players() );
			m_botAction.sendPrivateMessage( name, m_match.getTeam2Players() );
		} else if( m_match.isTeamOne( m_match.getTeamId( name ) ) )
			m_botAction.sendPrivateMessage( name, m_match.getTeam1Players() );
		else if( m_match.isTeamTwo( m_match.getTeamId( name ) ) )
			m_botAction.sendPrivateMessage( name, m_match.getTeam2Players() );
	}

	public void do_lagOut( String name, String Message ) {
		if( gameState < 3 ) {
			m_botAction.sendPrivateMessage( name, "A game has not started." );
			return;
		}
		if( !m_match.hasPlayer( name ) ) {
			m_botAction.sendPrivateMessage( name, "You are not registered for this game." );
			return;
		}
		if( m_botAction.getPlayer( name ).getShipType() > 0 ) {
			m_botAction.sendPrivateMessage( name, "You are already in the game. " );
			return;
		}
		if( gameState == 3 ) {
			m_botAction.setShip( name, m_match.getPlayer( name ).getShip() );
			m_botAction.setFreq( name, m_match.getPlayer( name ).getFreq() );
			m_match.getPlayer( name ).notLaggedOut();
			if( laggers.containsKey( name ) ) {
				Lagger l = (Lagger)laggers.get( name );
				l.cancel();
				laggers.remove( name );
			}
			return;
		}
		if( m_match.getPlayer( name ).isOutOfGame() || m_match.lagoutLimitReached( name ) ) {
			m_botAction.sendPrivateMessage( name, "You are out of the game, have been lagged out too long, or lagged out too many times." );
			m_botAction.setFreq( name, m_match.getPlayer( name ).getFreq() );
			return;
		}
		if( laggers.containsKey( name ) ) {
			Lagger l = (Lagger)laggers.get( name );
			l.cancel();
			laggers.remove( name );
		}
		m_match.getPlayer( name ).updateTimer();
		m_botAction.setShip( name, m_match.getPlayer( name ).getShip() );
		m_botAction.setFreq( name, m_match.getPlayer( name ).getFreq() );
		m_match.getPlayer( name ).notLaggedOut();
	}

	public void do_myFreq( String name, String message ) {
		if( gameState == 0 ) return;
		int playerTeamId = sql_getPlayersTeam( name );
		if( m_match.isTeamOne( playerTeamId ) ) m_botAction.setFreq( name, 0 );
		if( m_match.isTeamTwo( playerTeamId ) ) m_botAction.setFreq( name, 1 );
	}

	public void do_showScore( String name, String message ) {
		if( gameState == 0 ) return;
		if( gameState < 4 ) {
			m_botAction.sendPrivateMessage( name, m_match.getTeam1Name() + " vs " + m_match.getTeam2Name() + " (Has not started)" );
			return; 
		}
		
		int team1Score = 0;
		int team2Score = 0;
		if( m_match.getMatchTypeId() == 3 ) {
			team1Score = m_match.getTeam1Score();
			team2Score = m_match.getTeam2Score();
		} else {
			team1Score = m_match.getTeam2Deaths();
			team2Score = m_match.getTeam1Deaths();
		}
		m_botAction.sendPrivateMessage( name, m_match.getTeam1Name() + " vs " + m_match.getTeam2Name() + " (" + team1Score + "-" + team2Score + ")" );
	}
	
	public void do_changeHost( String name, String message ) {
		
		if( gameState == 0 ) return;
		if( name.equals( m_match.getRef() ) ) {
			m_botAction.sendPrivateMessage( name, "You are already the match host." );
			return;
		}
		
		m_botAction.sendArenaMessage( "New Match Ref: " + name );
		m_match.setRef( name );
	}

    public void handleEvent( PlayerPosition event ) {
    	if( gameState < 3 ) return;
    	m_match.justSaw( m_botAction.getPlayerName( event.getPlayerID() ) );

		if( gameState == 3 ) {
			m_match.warpPlayer( m_botAction.getPlayerName( event.getPlayerID() ), event.getXLocation(), event.getYLocation() );
			return;
		}

    	if( event.isInSafe() ) {
    		if( (int)(System.currentTimeMillis()/1000) - secondsOnStart > 10 ) {
    			m_match.getPlayer( m_botAction.getPlayerName( event.getPlayerID() ) ).updateTimer();
				m_botAction.sendUnfilteredPrivateMessage( event.getPlayerID(), "*prize #7" );
				return;
			}
		}

    	if( gameState != 4 || m_match.getMatchTypeId() != 2 ) return;
    	String player = m_botAction.getPlayerName( event.getPlayerID() );

    	if( event.getYLocation() > 470*16 ) {
    		if(	m_match.handlePlayerOutOfBounds( player ) ) {
    			m_botAction.sendArenaMessage( player + " has been given 1 death for being out of base too long." );
    			m_botAction.sendUnfilteredPrivateMessage( event.getPlayerID(), "*prize #7" );
    			m_botAction.shipReset( event.getPlayerID() );
    			m_match.getPlayer( player ).addDeath();
	    		if( m_match.getPlayer( player ).getDeaths() >= m_match.getPlayer( player ).getDeathLimit() ) {
		    		m_botAction.sendArenaMessage( player + " is out. " + m_match.getPlayer( player ).getKills() + " wins " + m_match.getPlayer( player ).getDeaths() + " losses " );
					m_match.getPlayer( player ).isOut();
					m_botAction.spec( player );
					m_botAction.spec( player );
					m_botAction.setFreq( player, m_match.getPlayer( player ).getFreq() );
					m_match.removeFromWatch( player );
					if( m_match.gameOver() )
						do_endGame();
				}
    		}
    	} else if( m_match.getPlayer( player ).timeOutOfBounds() > 5 ) {
    		m_match.getPlayer( player ).inBase();
    	}
	}

	public void handleEvent( PlayerDeath event ) {
    	if( gameState != 4 ) return;
    	Player killed = m_botAction.getPlayer( event.getKilleeID() );
    	Player killer = m_botAction.getPlayer( event.getKillerID() );
    	String killedName = killed.getPlayerName();
    	String killerName = killer.getPlayerName();

    	//Want to catch people who aren't recorded.
    	if( !m_match.hasPlayer( killedName ) || !m_match.hasPlayer( killerName ) )
    		return;

    	//Store the stats.
    	m_match.getPlayer( killedName ).addDeath();
    	m_match.getPlayer( killerName ).addToScore( event.getScore() );
    	if( m_match.getPlayer( killedName ).getFreq() == m_match.getPlayer( killerName ).getFreq() )
    		m_match.getPlayer( killerName ).addTeamKill();
    	else {
    		if( m_botAction.getPlayer( killedName ).getShipType() == 5 )
    			m_match.getPlayer( killerName ).addTerrierKill();
    		m_match.getPlayer( killerName ).addKill();
    	}

    	//If gametype = wb/jav check if the player is out.
    	if( m_match.getMatchTypeId() > 2 ) return;

    	if( m_match.getPlayer( killedName ).getDeaths() >= m_match.getPlayer( killedName ).getDeathLimit() ) {
    		m_botAction.sendArenaMessage( killedName + " is out. " + m_match.getPlayer( killedName ).getKills() + " wins " + m_match.getPlayer( killedName ).getDeaths() + " losses " );
			m_match.getPlayer( killedName ).isOut();
			m_botAction.spec( killedName );
			m_botAction.spec( killedName );
			m_botAction.setFreq( killedName, m_match.getPlayer( killedName ).getFreq() );
			m_match.removeFromWatch( killedName );
			if( m_match.gameOver() )
				do_endGame();
    	}
    }

    public void do_endGame() {
    	int team1Score;
    	int team2Score;
    	String mvp;
    	if( m_match.getMatchTypeId() == 3 ) {
			team1Score = m_match.getTeam1Score();
			team2Score = m_match.getTeam2Score();
			if( team1Score > team2Score ) mvp = m_match.getScoreMVP( 1 );
			else mvp = m_match.getScoreMVP( 2 );
		} else {
			team1Score = m_match.getTeam2Deaths();
			team2Score = m_match.getTeam1Deaths();
			if( team1Score > team2Score ) mvp = m_match.getMVP( 1 );
			else mvp = m_match.getMVP( 2 );
		}
		if( team1Score == team2Score ) {
			m_botAction.setTimer( 5 );
			m_botAction.sendArenaMessage( m_match.getOverTime() + " Overtime", 2 );
			m_match.incOverTime();
			return;
		}
		if( m_match.getMatchTypeId() < 3 )
			m_match.displayScores( team1Score, team2Score, mvp, true );
	    else
			m_match.displayBaseScores( team1Score, team2Score, mvp, true );

		gameState = 0;
		//do_storeResults( mvp, team1Score, team2Score, m_match.getMatchId() );
		m_botAction.toggleLocked();
		m_botAction.cancelTasks();
		m_botAction.toggleBlueOut();
		m_botAction.sendUnfilteredPublicMessage( "*lockspec" );
		m_botAction.setMessageLimit( 0 );
		m_botAction.setTimer( 0 );
		sql_storeResults( mvp, team1Score, team2Score, m_match.getMatchId() );
		TimerTask hideObjects = new TimerTask() {
			public void run() {
				m_match = null;
				myObjects.hideAllObjects();
				m_botAction.setObjects();
				generalTime = 0;
			}
		};
		m_botAction.scheduleTask( hideObjects, 5000 );

    }

    public void handleEvent( FlagReward event ) {
    	if( gameState != 4 ) return;
    	if( m_match.getMatchTypeId() != 3 ) return;
    	m_match.addFlagReward( event.getFrequency(), event.getPoints() );
    }

    public void handleEvent( FrequencyShipChange event ) {
    	if( gameState != 4 ) return;
    	Player p = m_botAction.getPlayer( event.getPlayerID() );
    	String name = p.getPlayerName();
    	if( !m_match.hasPlayer( name ) ) return;

    	//Check if they were specced for hitting death limit
    	if( p.getShipType() == 0  ) {
    		if( m_match.getPlayer( name ).isOutOfGame() ) return;

    		String output;
    		if( m_match.getPlayer( name ).getTimeBetweenLagouts() < 10 ) {
    			output = name + " lagged out or specced, he/she has 3 minutes to return. This was NOT recorded as a lagout.";
    		} else {
				output = name + " lagged out or specced, he/she has 3 minutes to return";
				m_match.getPlayer( name ).addLagout();
			}
			m_match.getPlayer( name ).laggedOut();
    		m_botAction.sendPrivateMessage( m_match.getRef(), output );
    		m_botAction.sendSquadMessage( m_match.getTeamName( name ), output );

    		if( m_match.lagoutLimitReached( name ) ) {
    			m_botAction.sendSquadMessage( m_match.getTeamName( name ), name + " has reached the lagout limit." );
    			m_botAction.sendPrivateMessage( m_match.getRef(), name + "has reached the lagout limit." );
    		}
    		if( m_match.getMatchTypeId() != 3 ) {
	    		if( laggers.containsKey( p.getPlayerName() ) ) {
	    			((Lagger)laggers.get( p.getPlayerName() )).cancel();
	    			laggers.remove( p.getPlayerName() );
	    		}
	    		laggers.put( p.getPlayerName(), new Lagger( p.getPlayerName(), m_match, laggers ) );
	    		Lagger l = (Lagger)laggers.get( p.getPlayerName() );
	    		m_botAction.scheduleTask( l, 180000 );
	    	}
    	}
    }
    
    public void handleEvent( PlayerLeft event ) {
    	
		if( gameState != 4 ) return;
		
		Player p = m_botAction.getPlayer( event.getPlayerID() );
		String name = p.getPlayerName();
		if( !m_match.hasPlayer( name ) ) return;
		
			if( m_match.getPlayer( name ).isOutOfGame() ) return;
		
			if( m_match.getPlayer( name ).isLagged() ) return;
		
			String output;
			if( m_match.getPlayer( name ).getTimeBetweenLagouts() < 10 ) {
				output = name + " lagged out or specced, he/she has 3 minutes to return. This was NOT recorded as a lagout.";
			} else {
				output = name + " lagged out or specced, he/she has 3 minutes to return";
				m_match.getPlayer( name ).addLagout();
			}
			m_match.getPlayer( name ).laggedOut();
			m_botAction.sendPrivateMessage( m_match.getRef(), output );
			m_botAction.sendSquadMessage( m_match.getTeamName( name ), output );
		
			if( m_match.lagoutLimitReached( name ) ) {
				m_botAction.sendSquadMessage( m_match.getTeamName( name ), name + " has reached the lagout limit." );
				m_botAction.sendPrivateMessage( m_match.getRef(), name + "has reached the lagout limit." );
			}
			if( m_match.getMatchTypeId() != 3 ) {
				if( laggers.containsKey( p.getPlayerName() ) ) {
					((Lagger)laggers.get( p.getPlayerName() )).cancel();
					laggers.remove( p.getPlayerName() );
				}
				laggers.put( p.getPlayerName(), new Lagger( p.getPlayerName(), m_match, laggers ) );
				Lagger l = (Lagger)laggers.get( p.getPlayerName() );
				m_botAction.scheduleTask( l, 180000 );
		} 
 
    }

    class Lagger extends TimerTask {
    	String player;
    	LeagueMatch match;
    	HashMap laggers;

    	public Lagger( String name, LeagueMatch m, HashMap l ) {
    		player = name;
    		match = m;
    		laggers = l;
    	}

    	public void run() {
    		if( m_match == null ) return;
    		m_botAction.sendArenaMessage( player + " has been out for over 3 minutes." );
    		m_match.getPlayer( player ).isOut();
    		m_match.getPlayer( player ).setDeaths( m_match.getPlayer( player ).getDeathLimit() );
    		m_match.removeFromWatch( player );
    		if( m_match.gameOver() )
				do_endGame();
			laggers.remove( player );
    	}
    }

    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ) ) handleCommand( name, message );
            else handlePlayerCommand( name, message );
        } else if( event.getMessageType() == Message.ARENA_MESSAGE ) {
        	if( message.equals( "Arena UNLOCKED" )  && gameState > 1 ) {
        		m_botAction.toggleLocked();
    		} else if( message.equals( "NOTICE: Game over" ) && gameState == 4 ) {
    			do_endGame();
    		} else if( message.equals( "Public Messages LOCKED" ) ) {
    			if( gameState == 4 )
    				if( !m_match.blueOut() ) m_botAction.toggleBlueOut();
    			else m_botAction.toggleBlueOut();
    		} else if( message.equals( "Public Messages UNLOCKED" ) && gameState == 4 ) {
    			if( m_match.blueOut() ) m_botAction.toggleBlueOut();
    		} else if( message.equals( "Message lock applies to spectators only." ) ) {
    			if( gameState == 4 )
    				if( !m_match.blueOut() ) m_botAction.sendUnfilteredPublicMessage( "*lockspec" );
    			else m_botAction.sendUnfilteredPublicMessage( "*lockspec" );
        	} else if( message.equals( "Message lock applies to everybody." ) && gameState == 4 ) {
    			if( m_match.blueOut() ) m_botAction.sendUnfilteredPublicMessage( "*lockspec" );
    		} else if( message.startsWith( "Time left:" ) && gameState == 4 ) {
        		sql_saveGameState( message );
        	}
    	}
    }

    public void handleCommand( String name, String message ) {
    	if( message.toLowerCase().startsWith( "!loadgame " ) ) {
			do_loadGame( name, message );
		} else if( message.toLowerCase().startsWith( "!reloadgame" ) ) {
			do_reloadGame( name, message );
		} else if( message.toLowerCase().startsWith( "!unloadgame" ) ) {
			do_unloadGame( name, message );
		} else if( message.toLowerCase().startsWith( "!zone" ) ) {
			do_zoneGame( name, message );
		} else if( message.toLowerCase().startsWith( "!startpick" ) ) {
			do_startPick( name, message );
		} else if( message.toLowerCase().startsWith( "!add " ) ) {
			do_addPlayer( name, message.substring( 5, message.length() ), false );
		} else if( message.toLowerCase().startsWith( "!fadd " ) ) {
			if( m_opList.isSmod( name ) )
			do_addPlayer( name, message.substring( 6, message.length() ), true );
		} else if( message.toLowerCase().startsWith( "!remove " ) ) {
			do_removePlayer( name, message.substring( 8, message.length() ) );
		} else if( message.toLowerCase().startsWith( "!startgame" ) ) {
			do_startGame( name, message );
		} else if( message.toLowerCase().startsWith( "!blueout" ) ) {
			do_toggleBlueOut( name );
		} else if( message.toLowerCase().startsWith( "!sub " ) ) {
			do_subPlayer( name, message.substring( 5, message.length() ) );
		} else if( message.toLowerCase().startsWith( "!switch " ) ) {
			do_switchPlayer( name, message.substring( 8, message.length() ) );
		} else if( message.toLowerCase().startsWith( "!list" ) ) {
			do_listPlayers( name, message );
		} else if( message.toLowerCase().startsWith( "!lagout" ) ) {
			do_lagOut( name, message );
		} else if( message.toLowerCase().startsWith( "!myfreq" ) ) {
			do_myFreq( name, message );
		} else if( message.toLowerCase().startsWith( "!score" ) ) {
			do_showScore( name, message );
		} else if( message.toLowerCase().startsWith( "!ref" ) ) {
			if( gameState == 0 ) return;
			m_botAction.sendPrivateMessage( name, "Match Ref: " + m_match.getRef() );
		} else if( message.toLowerCase().startsWith( "!host" ) ) {
			do_changeHost( name, message );
		}else if( message.toLowerCase().startsWith( "!creatematch " ) ) {
			if( m_opList.isSmod( name ) || name.equals( "Demonic" ) )
			sql_createFakeMatch( name, message.substring( 13, message.length() ) );
		} else if( message.toLowerCase().startsWith( "!loadtestgame" ) ) {
			do_loadTestGame( name, message );
		}
    }

    public void handlePlayerCommand( String name, String message ) {
    	if( message.toLowerCase().startsWith( "!list" ) ) {
			do_listPlayers( name, message );
		} else if( message.toLowerCase().startsWith( "!sub " ) ) {
			do_subPlayer( name, message.substring( 5, message.length() ) );
		} else if( message.toLowerCase().startsWith( "!lagout" ) ) {
			do_lagOut( name, message );
		} else if( message.toLowerCase().startsWith( "!myfreq" ) ) {
			do_myFreq( name, message );
		} else if( message.toLowerCase().startsWith( "!help" ) ) {
			showHelpMessages( name, message );
		} else if( message.toLowerCase().startsWith( "!score" ) ) {
			do_showScore( name, message );
		} else if( message.toLowerCase().startsWith( "!ref" ) ) {
			if( gameState == 0 ) return;
			m_botAction.sendPrivateMessage( name, "Match Ref: " + m_match.getRef() );
		}
    }

    public String[] getHelpMessages() {
    	String help[] = {
    		"---------------------- REF COMMANDS ------------------------------------",
    		"!loadgame <game#>                   - loads match identified by <game#>",
    		"!unloadgame                         - unloads the current match",
    		"!zone                               - zones for the current game",
    		"!startpick                          - will allow team lineups to be set",
    		"!add <name>:<ship> / !add <name>    - adds <name> in <ship> if specified",
    		"!remove <name>                      - removes <name> from the lineup",
    		"!startgame                          - starts the match",
    		"!blueout                            - toggles spectator blueout",
    		"!sub <playerOut>:<playerIn>         - puts <playerIn> in for <playerOut>",
    		"!switch <player1>:<player2>         - switches <player1> and <player2>",
    		"-------------------- PLAYER COMMANDS -----------------------------------",
    		"!list                               - list the players for your team",
    		"!myfreq                             - puts you on your team freq",
    		"!lagout                             - places you back into the game",
    		"!score                              - shows the current game score",
    		"!ref                                - shows the game ref"

    	};
    	return help;
    }

    public void showHelpMessages( String name, String message ) {
    	String help[] = {
    		"-------------------- PLAYER COMMANDS -----------------------------------",
    		"!list                               - list the players for your team",
    		"!myfreq                             - puts you on your team freq",
    		"!lagout                             - places you back into the game",
    		"!score                              - shows the current game score",
    		"!ref                                - shows the match ref"
    	};
    	m_botAction.privateMessageSpam( name, help );
    }

    public void cancel() {
    	gameState = 0;
		m_botAction.cancelTasks();
		m_botAction.setMessageLimit( 0 );
		m_botAction.setTimer( 0 );
		m_match = null;
		myObjects.hideAllObjects();
		m_botAction.setObjects();
		generalTime = 0;
    }

    //Query to load a match///
    public boolean sql_loadGame( int matchId ) {
    	myObjects = m_botAction.getObjectSet();
		try {
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT * FROM tblMatch WHERE fnMatchID = '"+matchId+"'" );
			if( result.next() ) {
				m_match = new LeagueMatch( result, m_botAction );
				return true;
			} else
				return false;
		} catch (Exception e) { return false; }
	}

	public void sql_deleteOldSaveStates( int matchId ) {
		try {
			m_botAction.SQLQuery( "local", "DELETE FROM tblTwlMatchState WHERE fnMatchID = "+matchId );
			m_botAction.SQLQuery( "local", "DELETE FROM tblTwlPlayerState WHERE fnMatchStateID = "+matchId );
		} catch (Exception e) { System.out.println( e ); }
	}

	public boolean sql_reloadGame( int matchId ) {
		try {
			ResultSet result = m_botAction.SQLQuery( "local", "SELECT * FROM tblTwlMatchState WHERE fnMatchID = '"+matchId+"'" );
			if( result.next() ) {
				if( sql_loadGame( matchId ) ) {
					m_match.loadMatchState( result );
					return true;
				} else return false;
			} else
				return false;
		} catch (Exception e) { return false; }
	}

	public boolean sql_reloadPlayerStates( int matchId ) {
		try {
			ResultSet result = m_botAction.SQLQuery( "local", "SELECT * FROM tblTwlPlayerState WHERE fnMatchStateID = "+matchId );
			while( result.next() ) {
				if( !m_match.reloadPlayer( result ) ) {
					String name = result.getString( "fcPlayerName" );
					if( m_match.getMatchTypeId() != 3 ) {
			    		if( laggers.containsKey( name ) ) {
			    			((Lagger)laggers.get( name )).cancel();
			    			laggers.remove( name );
			    		}
			    		laggers.put( name, new Lagger( name, m_match, laggers ) );
			    		Lagger l = (Lagger)laggers.get( name );
			    		m_botAction.scheduleTask( l, 180000 );
			    		m_botAction.sendArenaMessage( name + " not here for " + m_match.getTeamName( name ) + " and has 3 minutes to be subbed or return." );
			    	} else m_botAction.sendArenaMessage( name + " not here for " + m_match.getTeamName( name ) );
				}
			}
			return true;
		} catch (Exception e) { return false; }
	}

	///Gets the teamID of a player///
	public int sql_getPlayersTeam( String player ) {
		try {
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fnTeamID FROM tblTeamUser WHERE fnUserID = '"+sql_getPlayerId( player )+"' AND fnCurrentTeam = 1" );
			if( result.next() )
				return result.getInt( "fnTeamID" );
			else
				return -1;
		} catch (Exception e) { return -1; }
	}

        ///Gets when the player joined the squad///

        public java.util.Date sql_getJoinDate( String player ) {
          try {
                  ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fdJoined FROM tblTeamUser WHERE fnUserID = '"+sql_getPlayerId( player )+"' AND fnCurrentTeam = 1" );
                  if( result.next() )
                          return result.getDate( "fdJoined" );
                  else
                          return new java.util.Date();
          } catch (Exception e) { return new java.util.Date(); }
  }

	///Gets playerID of a player///
	public int sql_getPlayerId( String player ) {
		try {
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fnUserID FROM tblUser WHERE fcUserName = '"+Tools.addSlashesToString(player)+"'" );
			if( result.next() )
				return result.getInt( "fnUserID" );
			else
				return -1;
		} catch (Exception e) { return -1; }
	}

	//Returns true if this player is a captain.
	public boolean sql_isCap( String player ) {
		try {
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT * FROM tblUserRank WHERE fnUserID = '"+sql_getPlayerId( player )+"' AND fnRankID > 2 AND fnRankID < 5" );
			if( result.next() )
				return true;
			else
				return false;
		} catch (Exception e) { return false; }
	}

    //Gets the date and time of roster lock
	public java.util.Date sql_getLockDate() {
		try {
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fcVarValue FROM tblSiteVar WHERE fcVarName = 'LockDate'" );
			if( result.next() )
		      	return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(result.getString( "fcVarValue" ));
			else
		      	return new java.util.Date();
		} catch (Exception e) { 
			Tools.printStackTrace( e );
			return new java.util.Date(); 
		}
	}

	//Gets the date that the player must have joined by for the 1 round waiting period.
	public java.util.Date sql_getLastRoundCutoffDate() {
		try {
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fcVarValue FROM tblSiteVar WHERE fcVarName = 'LastRoundCutoffDate'" );
			if( result.next() )
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(result.getString( "fcVarValue" ));
			else
				return new java.util.Date();
		} catch (Exception e) { return new java.util.Date(); }
	}

	public void sql_createFakeMatch( String name, String message ) {
		try {
			String pieces[] = message.split( ":" );
			int team1Id = -1, team2Id = -1;
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fnTeamID FROM tblTeam WHERE fcTeamName = '"+pieces[0]+"'" );
			if( result.next() )
				team1Id = result.getInt( "fnTeamID" );
			result = m_botAction.SQLQuery( mySQLHost, "SELECT fnTeamID FROM tblTeam WHERE fcTeamName = '"+pieces[1]+"' ORDER BY fnTeamID DESC LIMIT 1" );
			if( result.next() )
				team2Id = result.getInt( "fnTeamID" );
			if( team1Id == -1 || team2Id == -1 ) {
				m_botAction.sendPrivateMessage( name, "unable to create test match" );
				return;
			}
			m_botAction.SQLQuery( mySQLHost, "UPDATE tblMatch SET fnTeam1ID = "+team1Id+", fnTeam2ID = "+team2Id+", fcTeam1Name = '"+pieces[0]+"', fcTeam2Name = '"+pieces[1]+"', fnMatchTypeID = "+pieces[2]+" WHERE fnMatchID = 5852" );
			m_botAction.sendPrivateMessage( name, "Match Created: " + pieces[0] + " vs " + pieces[1] + ", match type: " + pieces[2] );

		} catch (Exception e) { m_botAction.sendPrivateMessage( name, "unable to create test match: " ); }
	}

	//Stores the game state every minute for !reloadgame
	public void sql_saveGameState( String message ) {
		try {
			int matchId = m_match.getMatchId();
			String timeLeft = message.split( " " )[2] +":" + message.split( " " )[4];
			int team1Subs = m_match.getTeamSubs(1);
			int team2Subs = m_match.getTeamSubs(2);
			int team1Switches = m_match.getTeamSwitches(1);
			int team2Switches = m_match.getTeamSwitches(2);
			if( !m_match.state() ) {
				ResultSet result = m_botAction.SQLQuery( "local", "SELECT fnMatchID FROM `tblTwlMatchState` WHERE fnMatchID = "+matchId );
				if( result.next() ) m_match.toggleState();
			}
			if( !m_match.state() ) {
				m_botAction.SQLBackgroundQuery( "local", timeLeft, "INSERT INTO `tblTwlMatchState` (`fnMatchID`, `fnTeam1Subs`, `fnTeam2Subs`, `fnTeam1Switches`, `fnTeam2Switches`, `fcTimeLeft`) VALUES ("+matchId+", "+team1Subs+", "+team2Subs+", "+team1Switches+", "+team2Switches+", '"+timeLeft+"')" );
				m_match.toggleState();
			} else
				m_botAction.SQLBackgroundQuery( "local", timeLeft, "UPDATE tblTwlMatchState SET fnTeam1Subs = "+team1Subs+", fnTeam2Subs = "+team2Subs+", fnTeam1Switches = "+team1Switches+", fnTeam2Switches = "+team2Switches+", fcTimeLeft = '"+timeLeft+"' WHERE fnMatchID = "+matchId );

			Iterator it = m_match.getTeam1List();
			while( it.hasNext() ) {
				String name = (String)it.next();
				LeaguePlayer p = m_match.getPlayer( name );
				int outOfGame = 0;
				if( p.isOutOfGame() ) outOfGame = 1;
				if( !p.state() ) {
					ResultSet result = m_botAction.SQLQuery( "local", "SELECT fcPlayerName FROM `tblTwlPlayerState` WHERE fnMatchStateID = "+matchId+" AND fcPlayerName = '"+Tools.addSlashesToString(name)+"'");
					if( result.next() ) p.toggleState();
				}
				if( !p.state() ) {
					m_botAction.SQLBackgroundQuery( "local", name+timeLeft, "INSERT INTO `tblTwlPlayerState` (`fnMatchStateID`, `fnTeamID`, `fcPlayerName`, `fcSubbedBy`, `fnShipType`, `fnFreq`, `fnKills`, `fnDeaths`, `fnLives`, `fnTeamKills`, `fnTerrierKills`, `fnLagouts`, `fnScore`, `fnState`) VALUES ("+matchId+", "+m_match.getTeamId(name)+", '"+Tools.addSlashesToString(name)+"', '"+Tools.addSlashesToString( p.getSub() )+"', "+p.getShip()+", "+p.getFreq()+", "+p.getKills()+", "+p.getDeaths()+", "+p.getDeathLimit()+", "+p.getTeamKills()+", "+p.getTerrierKills()+", "+p.getLagouts()+", "+p.getScore()+", "+outOfGame+")" );
					p.toggleState();
				} else
					m_botAction.SQLBackgroundQuery( "local", name+timeLeft, "UPDATE tblTwlPlayerState SET fcSubbedBy ='"+Tools.addSlashesToString(p.getSub())+"', fnShipType = "+p.getShip()+", fnFreq = "+p.getFreq()+", fnKills = "+p.getKills()+", fnDeaths ="+p.getDeaths()+", fnLives ="+p.getDeathLimit()+", fnTeamKills ="+p.getTeamKills()+", fnTerrierKills ="+p.getTerrierKills()+", fnLagouts ="+p.getLagouts()+", fnScore ="+p.getScore()+", fnState ="+outOfGame+" WHERE fnMatchStateID = "+matchId+" AND fcPlayerName = '"+Tools.addSlashesToString(name)+"'");
			}

			it = m_match.getTeam2List();
			while( it.hasNext() ) {
				String name = (String)it.next();
				LeaguePlayer p = m_match.getPlayer( name );
				int outOfGame = 0;
				if( p.isOutOfGame() ) outOfGame = 1;
				if( !p.state() ) {
					ResultSet result = m_botAction.SQLQuery( "local", "SELECT fcPlayerName FROM `tblTwlPlayerState` WHERE fnMatchStateID = "+matchId+" AND fcPlayerName = '"+Tools.addSlashesToString(name)+"'");
					if( result.next() ) p.toggleState();
				}
				if( !p.state() ) {
					m_botAction.SQLBackgroundQuery( "local", name+timeLeft, "INSERT INTO `tblTwlPlayerState` (`fnMatchStateID`, `fnTeamID`, `fcPlayerName`, `fcSubbedBy`, `fnShipType`, `fnFreq`, `fnKills`, `fnDeaths`, `fnLives`, `fnTeamKills`, `fnTerrierKills`, `fnLagouts`, `fnScore`, `fnState`) VALUES ("+matchId+", "+m_match.getTeamId(name)+", '"+Tools.addSlashesToString(name)+"', '"+Tools.addSlashesToString( p.getSub() )+"', "+p.getShip()+", "+p.getFreq()+", "+p.getKills()+", "+p.getDeaths()+", "+p.getDeathLimit()+", "+p.getTeamKills()+", "+p.getTerrierKills()+", "+p.getLagouts()+", "+p.getScore()+", "+outOfGame+")" );
					p.toggleState();
				} else
					m_botAction.SQLBackgroundQuery( "local", name+timeLeft, "UPDATE tblTwlPlayerState SET fcSubbedBy ='"+Tools.addSlashesToString(p.getSub())+"', fnShipType = "+p.getShip()+", fnFreq = "+p.getFreq()+", fnKills = "+p.getKills()+", fnDeaths ="+p.getDeaths()+", fnLives ="+p.getDeathLimit()+", fnTeamKills ="+p.getTeamKills()+", fnTerrierKills ="+p.getTerrierKills()+", fnLagouts ="+p.getLagouts()+", fnScore ="+p.getScore()+", fnState ="+outOfGame+" WHERE fnMatchStateID = "+matchId+" AND fcPlayerName = '"+Tools.addSlashesToString(name)+"'");
			}

		} catch (Exception e) { System.out.println("Error Saving Game State: " +e); }
	}
	
	public void sql_startGame() {
		try {
			m_botAction.SQLQuery( mySQLHost, "UPDATE tblMatch SET fnMatchStateID = 2 WHERE fnMatchID = "+m_match.getMatchId() );
		} catch (Exception e) {}
	}


	public void sql_storeResults( String mvp, int team1, int team2, int matchId ) {
		String timeEnd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( Calendar.getInstance().getTime() );

		DBPlayerData player = new DBPlayerData( m_botAction, "local", m_match.getRef(), true );
		String matchUpdate = "UPDATE tblMatch SET fnRefereeUserID = "+player.getUserID();
		matchUpdate += ", fcRefereeName = '"+Tools.addSlashesToString( m_match.getRef() ) + "'";
		matchUpdate += ", fcRefereeComment = '"+Tools.addSlashesToString( mvp ) + "'";
		matchUpdate += ", ftTimeStarted = '"+timeStart+"'";
		matchUpdate += ", ftTimeEnded = '"+timeEnd+"'";
		matchUpdate += ", fnTeam1Score = "+team1;
		matchUpdate += ", fnTeam2Score = "+team2;
		matchUpdate += ", fnMatchStateID = 3";
		matchUpdate += " WHERE fnMatchID = "+matchId;
		try {
			m_botAction.SQLQuery( mySQLHost, matchUpdate );
		} catch (Exception e) { System.out.println( e ); }
		String roundUpdate = "INSERT INTO tblMatchRound (`fnMatchID`, `fnRoundStateID`, `fnRoundNumber`, `ftTimeStarted`, `ftTimeEnded`, `fnTeam1Score`, `fnTeam2Score`, `fnRemoteMatchRoundID`, `ftUpdated` ) VALUES (";
		roundUpdate += matchId+", ";
		roundUpdate += "3, ";
		roundUpdate += "1, ";
		roundUpdate += "'"+timeStart+"', ";
		roundUpdate += "'"+timeEnd+"', ";
		roundUpdate += team1+", ";
		roundUpdate += team2+", ";
		roundUpdate += "0, ";
		roundUpdate += "'"+timeEnd+"' )";
		int matchRoundID = -1;
		try {
			m_botAction.SQLQuery( mySQLHost, roundUpdate );
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT fnMatchRoundID  FROM tblMatchRound WHERE fnMatchID = "+matchId );
			if( result.next() )
				matchRoundID = result.getInt( "fnMatchRoundID" );
		} catch (Exception e) { System.out.println( e ); }
		if( matchRoundID == -1 ) {
			m_botAction.sendPrivateMessage( m_match.getRef(), "Unable to store player stats, please write the stats down." );
			return;
		}
		Iterator i = m_match.getTeam1List();
		while( i.hasNext() ) {
			String name = (String)i.next();
			DBPlayerData p = new DBPlayerData( m_botAction, "website", name, true );
			LeaguePlayer pp = m_match.getPlayer( name );
			String playerQuery = "INSERT INTO tblMatchRoundUser ( `fnMatchRoundID`, `fnTeamUserID`, `fnUserID`, `fcUserName`, `fnTeam`, `fnShipTypeID`, `fnValid`, `fnScore`, `fnWins`, `fnLosses`, `fnLagout`, `ftTimeStarted`, `ftTimeEnded`, `fnSubstituted`, `fnRemoteMatchRoundUserID`, `ftUpdated`, `fnTeamKills`, `fnTerrKills`) VALUES (";
			playerQuery += matchRoundID+", "+m_match.getTeamId( name )+", "+p.getUserID()+", ";
			playerQuery += "'"+Tools.addSlashesToString( name )+"', 1, ";
			playerQuery += pp.getShip()+", 1, "+pp.getScore()+", "+pp.getKills()+", "+pp.getDeaths()+", "+pp.getLagouts()+", ";
			playerQuery += "'"+timeStart+"', '"+timeEnd+"', ";
			playerQuery += "0, 0, '"+timeEnd+"', "+pp.getTeamKills()+", "+pp.getTerrierKills()+" )";
			try { m_botAction.SQLBackgroundQuery( mySQLHost, name, playerQuery ); }
			catch (Exception e) { Tools.printStackTrace(e); }
		}
		i = m_match.getTeam2List();
		while( i.hasNext() ) {
			String name = (String)i.next();
			DBPlayerData p = new DBPlayerData( m_botAction, "website", name, true );
			LeaguePlayer pp = m_match.getPlayer( name );
			String playerQuery = "INSERT INTO tblMatchRoundUser ( `fnMatchRoundID`, `fnTeamUserID`, `fnUserID`, `fcUserName`, `fnTeam`, `fnShipTypeID`, `fnValid`, `fnScore`, `fnWins`, `fnLosses`, `fnLagout`, `ftTimeStarted`, `ftTimeEnded`, `fnSubstituted`, `fnRemoteMatchRoundUserID`, `ftUpdated`, `fnTeamKills`, `fnTerrKills`) VALUES (";
			playerQuery += matchRoundID+", "+m_match.getTeamId( name )+", "+p.getUserID()+", ";
			playerQuery += "'"+Tools.addSlashesToString( name )+"', 2, ";
			playerQuery += pp.getShip()+", 1, "+pp.getScore()+", "+pp.getKills()+", "+pp.getDeaths()+", "+pp.getLagouts()+", ";
			playerQuery += "'"+timeStart+"', '"+timeEnd+"', ";
			playerQuery += "0, 0, '"+timeEnd+"', "+pp.getTeamKills()+", "+pp.getTerrierKills()+" )";
			try { m_botAction.SQLBackgroundQuery( mySQLHost, name, playerQuery ); }
			catch (Exception e) { Tools.printStackTrace(e); }
		}
	}

	public void sql_storeForfeitResults( int winner ) {
		int team1 = 0;
		int team2 = 0;
		if( m_match.getMatchTypeId() < 3 ) {
			if( winner == 1 ) team1 = 50;
			if( winner == 2 ) team2 = 50;
		} else {
			if( winner == 1 ) team1 = 100000;
			if( winner == 2 ) team2 = 100000;
		}
		timeStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( Calendar.getInstance().getTime() );
		String timeEnd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( Calendar.getInstance().getTime() );

		DBPlayerData player = new DBPlayerData( m_botAction, "local", m_match.getRef(), true );
		String matchUpdate = "UPDATE tblMatch SET fnRefereeUserID = "+player.getUserID();
		matchUpdate += ", fcRefereeName = '"+Tools.addSlashesToString( m_match.getRef() ) + "'";
		matchUpdate += ", fcRefereeComment = 'FORFEIT'";
		matchUpdate += ", ftTimeStarted = '"+timeStart+"'";
		matchUpdate += ", ftTimeEnded = '"+timeEnd+"'";
		matchUpdate += ", fnTeam1Score = "+team1;
		matchUpdate += ", fnTeam2Score = "+team2;
		matchUpdate += ", fnMatchStateID = 3";
		matchUpdate += " WHERE fnMatchID = "+m_match.getMatchId();
		try {
			m_botAction.SQLQuery( mySQLHost, matchUpdate );
		} catch (Exception e) { m_botAction.sendPrivateMessage( m_match.getRef(), "Unable to store results." ); }
	}
	
	public boolean sql_isBanned( String name ) {
		try {
			ResultSet result = m_botAction.SQLQuery( mySQLHost, "SELECT * FROM tblTWLBan WHERE fnUserID = "+sql_getPlayerId(name)+" AND fnRoundsLeft > 0" );
			if( result.next() ) return true;
			else return false;
		} catch (Exception e) { Tools.printStackTrace(e); return false; }
	}

}