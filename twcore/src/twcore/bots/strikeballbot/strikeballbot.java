
package twcore.bots.strikeballbot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Player;


/** strikeballbot - an implementation of the Strikeball League bot.
 *
 * @author qan
 */
public class strikeballbot extends SubspaceBot {

    private BotSettings m_botSettings;
    private OperatorList m_opList;

    private String m_currentArena;
    private String m_defaultArena;
    private boolean m_locked = true;

    private int m_currentRound = 0;
    private TimerTask m_endRound;
    private TimerTask m_scoreDelay;
    private TimerTask m_specRef;
    private TimerTask m_roundPause;

    private int m_carrierID = -1;                 // ID of current ball carrier
    private int m_prevCarrierID = -1;             // ID of previous person to carry the ball

    // ** These values are in CFG, but also hardcoded for safety
    private int m_numRounds = 2;
    private int m_minTeamSize = 4;
    private int m_maxTeamSize = 5;
    // **

    private int m_gameType;                  // Playoff rounds are slightly longer
    final static int GAMETYPE_STANDARD = 0;
    final static int GAMETYPE_PLAYOFF = 1;

    private int m_gameState;
    final static int STATE_INACTIVE = 0;     // When no game is loaded
    final static int STATE_LOADED = 1;       // Game is loaded but picks not started
    final static int STATE_LINEUPS = 2;      // Lineups being submitted
    final static int STATE_READY = 3;        // Ready to begin
    final static int STATE_PLAYING = 4;      // Game on

    private int m_forfeit = 0;               // Forfeiting team if they forfeit; else 0.
    private int m_forfeitType = 0;           // Type of forfeit
    final static int FORFEIT_GENERIC = 0;             // Generic forfeit
    final static int FORFEIT_NOTENOUGHPLAYERS = 1;    // Forfeit for not enough players
    final static int FORFEIT_RULEVIOLATION = 2;       // Forfeit for violation of rules
    final static int FORFEIT_CHEATING = 3;            // Forfeit for cheating

    final static int WARNING_GENERIC = 0;             // Generic warning
    final static int WARNING_PHASING = 1;             // Warning for causing a phase
    final static int WARNING_CHERRYPICKING = 2;       // Warning for cherrypicking


    // Team Data
    private String m_team1Name;                  // Team 1 Name as displayed in msgs
    private HashMap m_team1 = new HashMap();     // Teammates for team1
    private String m_team2Name;                  // Team 2 Name as displayed in msgs
    private HashMap m_team2 = new HashMap();     // Teammates for team1

    private HashMap m_allPlayers = new HashMap();// Records of all players (for final stats).
                                                 // This is also a temporary storage for
                                                 // players that lag out, leave, etc.

    private int m_team1Score = 0;
    private int m_team2Score = 0;





/*******************************************************************************
 *                                                                             *
 *    Strikeball League Bot: Table of Contents                                 *
 *                                                                             *
 *       1) GENERAL (constructor, general utility functions)                   *
 *       2) OPERATIONS (methods that are executed by commands)                 *
 *       3) COMMANDS (methods that directly refer to Ref commands)             *
 *       4) DEBUG COMMANDS (methods associated with debug-level commands)      *
 *       5) PLAYER COMMANDS (lagout + some help & versioning methods)          *
 *       6) EVENTS (all event handling)                                        *
 *       7) COMMAND HANDLER (handles intake of all input)                      *
 *       8) HELP / AUXILLARY (help text and misc tertiary unmentionables)      *
 *                                                                             *
 *******************************************************************************/





//****************************************************************************************
//************************************ (1). GENERAL **************************************
//****************************************************************************************

    /** Creates a new instance of strikeballbot
      * @param botAction An instance of the BotAction class.
      */
    public strikeballbot(BotAction botAction) {
        super(botAction);
        requestEvents();

        // m_botSettings contains the data specified in file <botname>.cfg
        m_botSettings = m_botAction.getBotSettings();

        m_defaultArena = m_botSettings.getString("InitialArena");
        m_numRounds = m_botSettings.getInt("Rounds");
        m_minTeamSize = m_botSettings.getInt("MinPlayers");
        m_maxTeamSize = m_botSettings.getInt("MaxPlayers");
    }



    /** Request only the events the bot needs.
      */
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.PLAYER_ENTERED);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.SOCCER_GOAL);
        req.request(EventRequester.BALL_POSITION);
    }



    /** Splits up a command string of format !command param1:param2:param3:etc... and returns
     * parameters in an array
     * @param name Ref's name
     * @param command Command string to split
     * @return A String array containing one argument per String.
     */
    public String[] returnParams( String name, String command ) {
        String[] commstr = command.split(" ", 2);
        try {
            return commstr[1].split(":");
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Use: !command param1:param2:param:..." );
            return null;
        }
    }




//****************************************************************************************
//********************************** (2). OPERATIONS *************************************
//****************************************************************************************

    /** Make bot ?go to a given arena.
     * @param name Ref's name
     * @param arena Name of arena.
     */
    public void do_go( String name, String arena ) {
        if( !m_locked ){
            m_currentArena = arena;
            m_botAction.changeArena( arena );
        } else {
            m_botAction.sendSmartPrivateMessage( name, "Bot currently locked.  Please !unlock before moving." );
        }
    }



   /** Stores a player when they are removed from the game (so as to still keep track of
    * their stats).
    * @param sbP Player to store.
    */
   public void do_storePlayer( StrikeballPlayer sbP ) {
        if( m_allPlayers == null )
            return;

        // Check if player is already stored
        StrikeballPlayer dummy = (StrikeballPlayer)m_allPlayers.get( sbP.getName() );

        // If so, remove before adding so as not to have dup's
        if( dummy != null )
            m_allPlayers.remove( sbP.getName() );

        m_allPlayers.put( sbP.getName(), sbP );
    }



    /** Retrieve a player from storage for use in the game.
     * @param playerName Name of player to retrieve.
     * @return The player object of the player found, or null if not found.
     */
    public StrikeballPlayer do_retrievePlayer( String playerName ) {
        if( m_allPlayers == null )
            return null;

        StrikeballPlayer sbP = (StrikeballPlayer) m_allPlayers.get( playerName );
        if( sbP == null )
            return null;

        // Remove player to keep from making duplicates
        m_allPlayers.remove( playerName );
        return sbP;
    }



    /** Returns the player object associated with a given name, if playing in the game.
     * @param playerName Name of player to retrieve.
     * @return The player object of the player found, or null if not found.
     */
    public StrikeballPlayer do_getSBPlayer( String playerName ) {
        StrikeballPlayer sbP;
        Player p = m_botAction.getFuzzyPlayer( playerName );
        if( p == null )
            return null;

        if( m_team1 == null || m_team2 == null )
            return null;

        sbP = (StrikeballPlayer) m_team1.get( p.getPlayerName() );
        if( sbP == null )
            sbP = (StrikeballPlayer) m_team2.get( p.getPlayerName() );
        if( sbP == null )
            return null;

        return sbP;
    }



    /** Returns the player object associated with a given ID, if playing in the game.
     * @param playerID ID of player to retrieve.
     * @return The player object of the player found, or null if not found.
     */
    public StrikeballPlayer do_getSBPlayer( int playerID ) {
        StrikeballPlayer sbP;
        Player p = m_botAction.getPlayer( playerID );
        if( p == null )
            return null;

        if( m_team1 == null || m_team2 == null )
            return null;

        sbP = (StrikeballPlayer) m_team1.get( p.getPlayerName() );
        if( sbP == null )
            sbP = (StrikeballPlayer) m_team2.get( p.getPlayerName() );
        if( sbP == null )
            return null;

        return sbP;
    }



    /** Changes the state of the game (INACTIVE, LOADED, LINEUPS, READY or PLAYING) and applies
     * all necessary operations.
     * @param gameState State to change to, as defined by STATE_* finals.
     */
    public void do_changeState( int gameState ) {
        switch( gameState ) {
        case STATE_LOADED:
            m_botAction.sendUnfilteredPublicMessage("*restart");
            m_botAction.specAll();
            m_carrierID = -1;
            m_prevCarrierID = -1;
            m_currentRound = 0;
            m_team1Score = 0;
            m_team2Score = 0;
            m_forfeit = 0;
            m_team1 = new HashMap();
            m_team2 = new HashMap();
            m_allPlayers = new HashMap();
            if( m_endRound != null )
                m_endRound.cancel();
            do_showLogo();
            m_gameState = STATE_LOADED;
            break;
        case STATE_LINEUPS:
            m_gameState = STATE_LINEUPS;
            break;
        case STATE_READY:
            m_botAction.scoreResetAll();
            m_botAction.shipResetAll();
            m_gameState = STATE_READY;
            break;
        case STATE_PLAYING:
            m_prevCarrierID = -1;
            m_carrierID = -1;
            m_botAction.sendUnfilteredPublicMessage("*lockpublic");
            m_botAction.sendArenaMessage("Blueout enabled.  Staff, please refrain from speaking in public chat.");
            m_botAction.scoreResetAll();
            m_botAction.shipResetAll();
            m_gameState = STATE_PLAYING;
            break;

        // default case = STATE_INACTIVE (a failsafe)
        default:
            m_carrierID = -1;
            m_prevCarrierID = -1;
            m_currentRound = 0;
            if( m_endRound != null )
                m_endRound.cancel();
            m_botAction.sendUnfilteredPublicMessage("*restart");
            m_gameState = STATE_INACTIVE;
        }
    }



    /** Ends the round (fired by a TimerTask).
     */
    public void do_endRound() {

        if( m_currentRound >= m_numRounds ) {
            do_endGame();
            return;
        }


        m_botAction.sendArenaMessage( "ROUND " + m_currentRound + " IS OVER!  Score: "
               + m_team1Name + " [" + m_team1Score + " - " + m_team2Score + "] " + m_team2Name, 2 );


        m_botAction.setTimer( 0 );
        m_botAction.specAll();

        m_botAction.sendUnfilteredPublicMessage("*lockpublic");
        do_changeState( STATE_READY );

        if( m_roundPause != null )
            m_roundPause.cancel();

        m_roundPause = new TimerTask() {
            public void run() {

                Collection team1, team2;
                Iterator t1, t2;
                StrikeballPlayer sbP;
                team1 = m_team1.values();
                team2 = m_team2.values();

                t1 = team1.iterator();
                t2 = team2.iterator();

                while( t1.hasNext() ) {
                    sbP = (StrikeballPlayer)t1.next();

                    // If first or odd round, standard ship setup
                    if( m_currentRound % 2 == 1 ) {
                        m_botAction.setShip( sbP.getName(), 1 );
                        m_botAction.setFreq( sbP.getName(), 0 );
                    } else {
                        m_botAction.setShip( sbP.getName(), 2 );
                        m_botAction.setFreq( sbP.getName(), 1 );
                    }
                }

                while( t2.hasNext() ) {
                    sbP = (StrikeballPlayer)t2.next();

                    // If first or odd round, standard ship setup
                    if( m_currentRound % 2 == 1 ) {
                        m_botAction.setShip( sbP.getName(), 2 );
                        m_botAction.setFreq( sbP.getName(), 1 );
                    } else {
                        m_botAction.setShip( sbP.getName(), 1 );
                        m_botAction.setFreq( sbP.getName(), 0 );
                    }
                }

                m_botAction.setTimer( 5 );  // 5 min
                m_botAction.sendArenaMessage( "Captains, you have 5 minutes to submit any substitutions.", 2 );

            };
        };
        m_botAction.scheduleTask(m_roundPause, 1000 );

    }



    /** Ends the game if scores are not tied.
     */
    public void do_endGame() {
        StrikeballPlayer sbP;

        if( m_team1Score == m_team2Score ) {

            if( m_endRound != null )
                m_endRound.cancel();
            m_endRound = new TimerTask() {
                public void run() {
                    do_endGame();
                };
            };
            m_botAction.scheduleTask(m_endRound, 60000 * 5); // Add 5 minutes to the timer, keep going
            m_botAction.setTimer( 5 );

            m_botAction.sendArenaMessage( "Round " + m_currentRound + ": OVERTIME.  Time extended by 5:00, play continues." );

            return;
        }


        m_botAction.specAll();


        if( m_team1Score > m_team2Score ) {
            m_botAction.sendArenaMessage( "TIME!  " + m_team1Name + " has defeated " + m_team2Name + "!" +
                                    "  Final score: [" + m_team1Score + " - " + m_team2Score + "]", 1);
        } else {
            m_botAction.sendArenaMessage( "TIME!  " + m_team2Name + " has defeated " + m_team1Name + "!" +
                                    "  Final score: [" + m_team1Score + " - " + m_team2Score + "]", 1);
        }


        if( m_scoreDelay != null )
            m_scoreDelay.cancel();
        m_scoreDelay = new TimerTask() {
            public void run() {
                do_storeScores( do_showScores() );
            };
        };
        m_botAction.scheduleTask(m_scoreDelay, 3000 );

        if( m_gameState != STATE_READY )
            m_botAction.sendUnfilteredPublicMessage("*lockpublic");

        do_changeState( STATE_INACTIVE );
    }



    /** Shows player scores.
     * @return Vector containing Strings that make up the score displays.
     */
    public Vector do_showScores() {
        String line, val, space = "";
        int length;
        int[] stats;
        Vector scoreDisp = new Vector();
        Collection players = m_allPlayers.values();
        StrikeballPlayer sbP;
        Iterator i;
        Collection team1, team2;

        // Add everyone into one big happy ol' hashmap, m_allPlayers
        team1 = m_team1.values();
        team2 = m_team2.values();

        i = team1.iterator();
        while( i.hasNext() ) {
            sbP = (StrikeballPlayer) i.next();
            do_storePlayer( sbP );
        }

        i = team2.iterator();
        while( i.hasNext() ) {
            sbP = (StrikeballPlayer) i.next();
            do_storePlayer( sbP );
        }


        i = players.iterator();

        // Make sure there are players to show before we put out the score line
        if( i.hasNext() ) {
            line = "Player         | Kills | Deaths| Goals |Assists|Carries|Steals |  TO's | Warns |";
            scoreDisp.add( line );
        }

        // Format a line
        while( i.hasNext() ) {

            // Name display
            sbP = (StrikeballPlayer) i.next();
            stats = sbP.getAllStats();

            space = "";

            length = 17 - sbP.getName().length();
            if( length < 0 ) {
                line = sbP.getName().substring(0, 17);
            } else {
                for( int j = 0; j < length; j++ )
                    space = space + " ";
                line = sbP.getName() + space;
            }

            // Stat display.  3 spaces allotted per #, with 5 trailing spaces.
            for( int k = 0; k < 8; k++ ) {
                space = "";

                val = Integer.toString( stats[k] );
                length = 3 - val.length();

                for( int j = 0; j < length; j++ )
                    space = space + " ";
                val = space + val;

                space = "";

                for( int j = 0; j < 5; j++ )
                    space = space + " ";
                val = val + space;

                line = line + val;
            }

            scoreDisp.add( line );
        }

        // Spit that sh*t out.
        i = scoreDisp.iterator();
        while( i.hasNext() ) {
            line = (String)i.next();
            m_botAction.sendArenaMessage( line );
        }

        return scoreDisp;
    }



    /** Stores the scores in a log file.
     */
    public void do_storeScores( Vector scoreDisp ) {
        try {
            // Open for append
            BufferedWriter outBuffer = new BufferedWriter( new FileWriter( m_botSettings.getString("LogPath") + m_botSettings.getString("LogFile"), true ) );
            Date m_gameEndTime = new Date();

            if( m_gameType == GAMETYPE_STANDARD )
                outBuffer.write( "*** SBL Season Match between " + m_team1Name + " and " + m_team2Name + " at " + m_gameEndTime.toString() + " ***" );
            else
                outBuffer.write( "*** SBL Playoff Match between " + m_team1Name + " and " + m_team2Name + " at " + m_gameEndTime.toString() + " ***" );

            outBuffer.newLine();

            if( m_forfeit == 0 ) {
                if( m_team1Score > m_team2Score ) {
                    outBuffer.write( "RESULT: " + m_team1Name + " defeats " + m_team2Name + ".  SCORE: " + m_team1Score + " - " + m_team2Score );
                } else {
                    outBuffer.write( "RESULT: " + m_team2Name + " defeats " + m_team1Name + ".  SCORE: " + m_team2Score + " - " + m_team1Score );
                }
            } else {
                String message;

                switch( m_forfeitType ) {
                case FORFEIT_NOTENOUGHPLAYERS:
                    message = "not enough players";
                    break;
                case FORFEIT_RULEVIOLATION:
                    message = "excessive or extreme rule violation";
                    break;
                case FORFEIT_CHEATING:
                    message = "cheating";
                    break;
                default:
                    message = "no reason given";
                }

                if( m_forfeit == 1 ) {
                    outBuffer.write( "RESULT: " + m_team1Name + " defeats " + m_team2Name + " by way of forfeit (" + message + ")" );
                } else {
                    outBuffer.write( "RESULT: " + m_team2Name + " defeats " + m_team1Name + " by way of forfeit (" + message + ")" );
                }
            }

            outBuffer.newLine();
            outBuffer.newLine();

            // Dump main score details
            Iterator i = scoreDisp.iterator();
            String line;
            while( i.hasNext() ) {
                line = (String)i.next();
                outBuffer.write( line );
                outBuffer.newLine();
            }

            outBuffer.write( "*** End of match log ***" );
            outBuffer.newLine();
            outBuffer.newLine();
            outBuffer.newLine();

            outBuffer.close();

        } catch (IOException e) {
        }
    }



    /** Displays the Strikeball logo.
     */
    public void do_showLogo() {
        for( int i = 0; i < logo.length; i++ )
            m_botAction.sendArenaMessage( logo[i] );
    }



    /** Shows team versus message.
     */
    public void do_showTeamVs() {
        if( m_gameType == GAMETYPE_STANDARD ) {
            m_botAction.sendArenaMessage( "Strikeball Regular Season: " + m_team1Name + " vs. " + m_team2Name + ".", 1 );
        } else {
            m_botAction.sendArenaMessage( "Strikeball Playoffs: " + m_team1Name + " vs. " + m_team2Name + ".", 1 );
        }
    }



    /** Cancels the game.
     */
    public void do_cancel() {
        do_changeState( STATE_INACTIVE );
    }




//****************************************************************************************
//*********************************** (3). COMMANDS **************************************
//****************************************************************************************

    /** Load a standard or playoff game.
     * @param name Ref's name
     * @param team1 Name of first team.
     * @param team2 Name of second team.
     */
    public void cmd_loadgame( String name, String team1, String team2 ) {

        if( m_gameState > STATE_INACTIVE ) {
            m_botAction.sendPrivateMessage( name, "A StrikeBall League game is already loaded.  !cancel the current one if you wish to load another." );
            return;
        }

        m_team1Name = team1;
        m_team2Name = team2;

        if( m_scoreDelay != null )
            m_scoreDelay.cancel();
        m_scoreDelay = new TimerTask() {
            public void run() {
                do_showTeamVs();
            };
        };
        m_botAction.scheduleTask(m_scoreDelay, 3000 );

        do_changeState( STATE_LOADED );

        m_botAction.sendPrivateMessage( name, "Game loaded.  Send !startpick when you are ready for captains to submit their lineups." );
    }



    /** Start player picks after game has been loaded.
     * @param name Ref's name
     */
    public void cmd_startpick( String name ) {
        if( m_gameState == STATE_INACTIVE ) {
            m_botAction.sendPrivateMessage( name, "You must use !loadseason T1:T2 or !loadplayoff T1:T2 before starting picks." );
            return;
        }
        if( m_gameState == STATE_LINEUPS ) {
            m_botAction.sendPrivateMessage( name, "Picks have already begun.  Use !add to add players, and !setcaptain to assign captains, then !startgame to start." );
            return;
        }
        if( m_gameState == STATE_READY || m_gameState == STATE_PLAYING ) {
            m_botAction.sendPrivateMessage( name, "Game has already been started.  Use !sub, !add and !remove to modify lineups." );
            return;
        }

        m_botAction.sendArenaMessage( "Captains, you have 5 minutes to submit your lineups to " + name + ".", 2 );
        m_botAction.sendPrivateMessage( name, "Picks started.  Use !add name:team# to add players PMed to you to the lineup, then !startgame" );
        m_botAction.setTimer( 5 );  // 5 min
        do_changeState( STATE_LINEUPS );

    }



    /** Starts the game, putting everyone in, after all picks are set.
     * @param name Ref's name
     */
    public void cmd_startgame( String name ) {
        Collection team1, team2;
        Iterator t1, t2;
        StrikeballPlayer sbP;
        String teamOutput = "";

        if( m_gameState < STATE_LINEUPS ) {
            m_botAction.sendPrivateMessage( name, "You must have a game loaded and have lineups set (!startpick) before you can begin." );
            return;
        }
        if( m_gameState == STATE_PLAYING ) {
            m_botAction.sendPrivateMessage( name, "Game is already started.  Use !cancel if you'd like to stop it." );
            return;
        }
        if( m_team1.size() != m_team2.size() ) {
            m_botAction.sendPrivateMessage( name, "Team sizes are mismatched.  Correct and !startgame again." );
            return;
        }
        if( m_team1.size() < m_minTeamSize ) {
            m_botAction.sendPrivateMessage( name, "Team must have " + m_minTeamSize + " players at minimum to start." );
            return;
        }
        if( m_team1.size() > m_maxTeamSize ) {
            m_botAction.sendPrivateMessage( name, "ERROR: teams have too many players!  This message should not be seen.  Please report immediately.", 7 );
            return;
        }

        team1 = m_team1.values();
        team2 = m_team2.values();

        t1 = team1.iterator();
        t2 = team2.iterator();

        teamOutput = "In for " + m_team1Name + ": ";
        while( t1.hasNext() ) {
            sbP = (StrikeballPlayer)t1.next();

            m_botAction.setShip( sbP.getName(), 1 );
            m_botAction.setFreq( sbP.getName(), 0 );
            teamOutput = teamOutput + sbP.getName() + "  ";
        }
        m_botAction.sendArenaMessage( teamOutput );

        teamOutput = "In for " + m_team2Name + ": ";
        while( t2.hasNext() ) {
            sbP = (StrikeballPlayer)t2.next();

            m_botAction.setShip( sbP.getName(), 2 );
            m_botAction.setFreq( sbP.getName(), 1 );
            teamOutput = teamOutput + sbP.getName() + "  ";
        }
        m_botAction.sendArenaMessage( teamOutput );

        // Add ref in to get ball
        m_botAction.setShip( name, 1 );
        m_botAction.setFreq( name, 1337 );

        m_botAction.sendArenaMessage( "PLAYERS: Please get in position immediately, and allow the game ref, " + name + ", to get the ball.", 2 );

        m_botAction.sendPrivateMessage( name, "Do !restart to reset arena score (if needed), then get hold of the ball and do a !roundstart to start the round." );

        do_changeState( STATE_READY );
    }



    /** Starts the round, warping the ref to the center with the ball and speccing him/her.
     * @param name Ref's name
     */
    public void cmd_roundstart( String name ) {
        if( m_gameState < STATE_READY ) {
            m_botAction.sendPrivateMessage( name, "Game must be loaded, have lineups set, and players in (!startgame) to begin a round." );
            return;
        }
        if( m_gameState > STATE_READY ) {
            m_botAction.sendPrivateMessage( name, "Round has already begun.  Use !cancel if you wish to cancel the game." );
            return;
        }

        // New round
        m_currentRound++;

        // Warp freq 1 to left safe
        m_botAction.warpFreqToLocation( 0, 504, 512 );
        // Warp freq 2 to right safe
        m_botAction.warpFreqToLocation( 1, 520, 512 );
        // Warp ref to center
        m_botAction.warpTo( name, 512, 512);

        final String fName = name;

        if( m_specRef != null )
            m_specRef.cancel();
        m_specRef = new TimerTask() {
            public void run() {
                m_botAction.spec( fName );
                m_botAction.spec( fName );
            };
        };
        m_botAction.scheduleTask( m_specRef, 500 );



        if( m_endRound != null )
            m_endRound.cancel();
        m_endRound = new TimerTask() {
            public void run() {
                do_endRound();
            };
        };

        if( m_gameType == GAMETYPE_STANDARD ) {
            m_botAction.scheduleTask(m_endRound, 60000 * m_botSettings.getInt("RegularRoundLength"));
            m_botAction.setTimer( m_botSettings.getInt("RegularRoundLength") );
            m_botAction.sendArenaMessage( m_team1Name + " vs " + m_team2Name + " - ROUND " + m_currentRound + ".  Time: " + m_botSettings.getInt("RegularRoundLength") + ":00");
        } else {
            m_botAction.scheduleTask(m_endRound, 60000 * m_botSettings.getInt("PlayoffRoundLength"));
            m_botAction.setTimer( m_botSettings.getInt("PlayoffRoundLength") );
            m_botAction.sendArenaMessage( m_team1Name + " vs " + m_team2Name + " - ROUND " + m_currentRound + ".  Time: " + m_botSettings.getInt("PlayoffRoundLength") + ":00");
        }

        m_botAction.sendArenaMessage( "GO GO GO!!", 104 );

        do_changeState( STATE_PLAYING );
    }



    /** Adds a player to the game on a specific team.
     * @param name Ref's name
     * @param playerName Name of the player to add.
     * @param team Team (1 or 2) to add to.
     */
    public void cmd_addplayer( String name, String playerName, int team ) {
        String teamname;
        StrikeballPlayer sbP;

        if( m_gameState == STATE_INACTIVE ) {
            m_botAction.sendPrivateMessage( name, "You must load a game with !loadseason or !loadplayoff before adding players." );
            return;
        }
        if( m_gameState == STATE_LOADED ) {
            m_botAction.sendPrivateMessage( name, "Use !startpick once you are ready to begin picks." );
            return;
        }
        if( team != 1 && team != 2 ) {
            m_botAction.sendPrivateMessage( name, "Invalid team number; add to either team 1 or team 2." );
            return;
        }
        if( (team == 1 && m_team1.size() >= m_maxTeamSize) || (team == 2 && m_team2.size() >= m_maxTeamSize)) {
            m_botAction.sendPrivateMessage( name, "The team is full.  Use !remove first, then !add. (or !dbg-show and !dbg-rem)" );
            return;
        }


        playerName = m_botAction.getFuzzyPlayerName( playerName );

        if( playerName == null ) {
            m_botAction.sendPrivateMessage( name, "Player not found." );
            return;
        }
        if( name.equals( playerName ) ) {
            m_botAction.sendPrivateMessage( name, "The host can't be added!  You need to be able to serve the ball." );
            return;
        }


        sbP = do_getSBPlayer( playerName );

        if( sbP != null ) {
            m_botAction.sendPrivateMessage( name, "Player has already been added.  Please !remove them if you wish to re-add them. (or have them do a !lagout if they have lagged out)" );
            return;
        }

        if( team == 1 )
            teamname = m_team1Name;
        else
            teamname = m_team2Name;

        // Check if player has been in before.  If so, reuse record
        sbP = do_retrievePlayer( playerName );

        // If not, make a new one.
        if( sbP == null ) {
            sbP = new StrikeballPlayer( playerName, team, teamname, m_botAction );
        } else {
            sbP.setTeam( team );
            sbP.setTeamName( teamname );
        }

        if( team == 1 )
            m_team1.put( sbP.getName(), sbP );
        else
            m_team2.put( sbP.getName(), sbP );


        m_botAction.sendPrivateMessage( name, sbP.getName() + " in for " + teamname + "." );
        m_botAction.sendPrivateMessage( sbP.getName(), "You have been put in for " + teamname + " (you will be added when lineups are final)." );

        // If game has started and round is incremented, set to their ship/freq of the round
        if( m_gameState >= STATE_READY ) {
            if( team == 1 ) {
                if( m_currentRound % 2 == 1 ) {
                    m_botAction.setShip( sbP.getName(), 1 );
                    m_botAction.setFreq( sbP.getName(), 0 );
                } else {
                    m_botAction.setShip( sbP.getName(), 2 );
                    m_botAction.setFreq( sbP.getName(), 1 );
                }
            } else {
                if( m_currentRound % 2 == 1 ) {
                    m_botAction.setShip( sbP.getName(), 2 );
                    m_botAction.setFreq( sbP.getName(), 1 );
                } else {
                    m_botAction.setShip( sbP.getName(), 1 );
                    m_botAction.setFreq( sbP.getName(), 0 );
                }
            }

        // Else, just set their freq according to the default/starting freq.
        } else {
            if( team == 1 ) {
                m_botAction.setFreq( sbP.getName(), 0 );
            } else {
                m_botAction.setFreq( sbP.getName(), 1 );
            }
        }
    }



    /** Removes a player from the game.
     * @param name Ref's name.
     * @param playerName Name of player to remove.
     */
    public void cmd_removeplayer( String name, String playerName ) {
        StrikeballPlayer sbP;

        if( m_gameState < STATE_LINEUPS ) {
            m_botAction.sendPrivateMessage( name, "Have a game loaded and do !startpick before adding or removing players." );
            return;
        }

        sbP = do_getSBPlayer( playerName );

        if( sbP == null ) {
            m_botAction.sendPrivateMessage( name, "Player is not in for either team." );
            return;
        }

        if( sbP.isWarnedOut() ) {
            m_botAction.sendPrivateMessage( name, "Player received too many warnings and can not be removed (except with !dbg- cmds)." );
            return;
        }

        // Do not allow them to !lagout back into the game
        if( sbP.isLaggedOut() )
            sbP.cancelLagoutTask();

        m_team1.remove( sbP.getName() );
        m_team2.remove( sbP.getName() );

        do_storePlayer( sbP );

        m_botAction.spec( sbP.getName() );
        m_botAction.spec( sbP.getName() );

        m_botAction.sendPrivateMessage( name, playerName + " removed from " + sbP.getTeamName() + " lineup." );
    }



    /** Substitutes one player for the other.
     * @param name Ref's name
     * @param p1Name Name of player to substitute out.
     * @param p2Name Name of player to substitute in.
     */
    public void cmd_subplayer( String name, String p1Name, String p2Name ) {
        StrikeballPlayer sbP, sbP2;
        int team;

        if( m_gameState < STATE_READY ) {
            m_botAction.sendPrivateMessage( name, "Subs can only be made while players are in the arena.  Use !add and !remove for pre-game work." );
            return;
        }

        p1Name = m_botAction.getFuzzyPlayerName( p1Name );
        p2Name = m_botAction.getFuzzyPlayerName( p2Name );

        if( p2Name == null ) {
            m_botAction.sendPrivateMessage( name, "Player to sub in not found in arena." );
            return;
        }

        sbP = do_getSBPlayer( p1Name );
        sbP2 = do_getSBPlayer( p2Name );

        if( sbP == null) {
            m_botAction.sendPrivateMessage( name, "Player to sub out is not on the lineup." );
            return;
        }
        if( sbP2 != null) {
            m_botAction.sendPrivateMessage( name, "Player to sub in is already on the lineup." );
            return;
        }
        if( sbP.isWarnedOut() || sbP2.isWarnedOut() ) {
            m_botAction.sendPrivateMessage( name, "One of the players has received too many warnings and can not be subbed (may still use !dbg cmds)." );
            return;
        }

        if( sbP.isLaggedOut() )
            sbP.cancelLagoutTask();  // Cancel to disallow !lagout from occuring

        team = sbP.getTeam();
        cmd_removeplayer( name, p1Name );
        cmd_addplayer( name, p2Name, team );

        m_botAction.sendArenaMessage( sbP.getTeamName() + " - " + p2Name + " subbed in for " + p1Name +  "." );
    }



    /** Warns a player for doing something Wrong.
     * @param name Ref's name
     * @param playerName Name of player to warn.
     * @param warnType Type of warning, as specified by WARNING_* finals.
     */
    public void cmd_warnplayer( String name, String playerName, int warnType ) {
        StrikeballPlayer sbP;
        boolean lastWarning;
        String warnString;

        if( m_gameState < STATE_READY ) {
            m_botAction.sendPrivateMessage( name, "Warnings need only to be made after the game has begun or is about to begin." );
            return;
        }

        if( warnType < 0 || warnType > 2 ) {
            m_botAction.sendPrivateMessage( name, "Warning # out of range.  Use 0, 1 or 2." );
            return;
        }

        sbP = do_getSBPlayer( playerName );
        if( sbP == null ) {
            m_botAction.sendPrivateMessage( name, "Player not found." );
            return;
        }


        switch( warnType ) {
        case WARNING_PHASING:
            warnString = "You have received a warning for phasing (causing lag through ball).  Repeated warnings will result in being spec'd.";
            break;
        case WARNING_CHERRYPICKING:
            warnString = "You have received a warning for cherrypicking (waiting at enemy goal).  Repeated warnings will result in being spec'd.";
            break;
        default: // Generic
            warnString = "You have received a warning.  Repeated warnings will result in being specced.";
        }

        lastWarning = sbP.addWarning( warnType );

        if( lastWarning ) {
            m_botAction.spec( sbP.getName() );
            m_botAction.spec( sbP.getName() );
            m_botAction.sendArenaMessage( sbP.getName() + " has received three warnings (specced).  Player can not be subbed." );
            sbP.cancelLagoutTask();
        }

        m_botAction.sendPrivateMessage( name, sbP.getName() + " has been warned." );
        m_botAction.sendPrivateMessage( sbP.getName(), warnString );

    }



    /** Forfeits the game for a given reason, with the team specified losing the match.
     * @param name Ref's name
     * @param team Team number (1 or 2) that will forfeit.
     * @param reason Reason for forfeit, as defined by FORFEIT_* finals.
     */
    public void cmd_forfeitgame( String name, int team, int reason ) {
        String message, tname;

        if( m_gameState == STATE_INACTIVE ) {
            m_botAction.sendPrivateMessage( name, "A game has not been loaded.  Nothing to forfeit!" );
            return;
        }
        if( team != 1 && team != 2 ) {
            m_botAction.sendPrivateMessage( name, "Team must be either 1 or 2." );
            return;
        }

        if( team == 1 )
            tname = m_team1Name;
        else
            tname = m_team2Name;

        switch( reason ) {
        case FORFEIT_NOTENOUGHPLAYERS:
            message = tname + " has forfeited the game because they did not have the minimum " + m_minTeamSize + " players.";
            break;
        case FORFEIT_RULEVIOLATION:
            message = tname + " has forfeited the game for violation of the rules.";
            break;
        case FORFEIT_CHEATING:
            message = tname + " has forfeited the game due to cheating.";
            break;
        default:
            message = tname + " has forfeited the game.";
        }

        m_botAction.sendArenaMessage( message, 1 );

        if( m_scoreDelay != null )
            m_scoreDelay.cancel();

        m_forfeit = team;
        m_forfeitType = reason;

        if( m_scoreDelay != null)
            m_scoreDelay.cancel();
        m_scoreDelay = new TimerTask() {
            public void run() {
                do_storeScores( do_showScores() );
            };
        };
        m_botAction.scheduleTask(m_scoreDelay, 3000 );

        if( m_gameState != STATE_READY )
            m_botAction.sendUnfilteredPublicMessage("*lockpublic");

        do_changeState( STATE_INACTIVE );
    }



    /** Restarts the ball game.
     */
    public void cmd_restart() {
        m_botAction.sendUnfilteredPublicMessage("*restart");
    }



    /** Cancels the game instantly, in any state (with NO score display).
     * @param name Ref's name
     */
    public void cmd_cancelgame( String name ) {
        m_botAction.sendArenaMessage( "Game has been cancelled by " + name + "." );
        do_cancel();
    }




//****************************************************************************************
//******************************** (4). DEBUG COMMANDS ***********************************
//****************************************************************************************


    /** Debug: Shows a list of all players marked as "playing," and indexes them.
     * @param name Ref's name
     */
    public void cmd_dbg_show( String name ) {
        String formatString = "";
        Collection t1 = m_team1.values();
        Collection t2 = m_team2.values();
        StrikeballPlayer[] team1 = (StrikeballPlayer[]) t1.toArray( new StrikeballPlayer[0] );
        StrikeballPlayer[] team2 = (StrikeballPlayer[]) t2.toArray( new StrikeballPlayer[0] );

        if( team1 == null || team2 == null ) {
            m_botAction.sendPrivateMessage( name, "One or more teams were reported as null (!).  Can't show." );
            return;
        }

        m_botAction.sendPrivateMessage( name, "Team 1 Players" );

        for(int i = 0; i < team1.length; i++ )
            formatString = formatString + i + ") " + team1[i].getName() + "  ";

        m_botAction.sendPrivateMessage( name, formatString );

        formatString = "";

        m_botAction.sendPrivateMessage( name, "Team 2 Players" );

        for(int i = 0; i < team2.length; i++ )
            formatString = formatString + i + ".) " + team2[i].getName() + "  ";

        m_botAction.sendPrivateMessage( name, formatString );

    }



    /** Debug: Removes a player based on an index number.
     * @param name Ref's name
     * @param team Team number of player to remove.
     * @param index Index of player to remove (use !dbg-show to see indeces).
     */
    public void cmd_dbg_rem( String name, int team, int index ) {

        if( team != 1 && team != 2 ) {
            m_botAction.sendPrivateMessage( name, "Team must be either 1 or 2." );
            return;
        }

        StrikeballPlayer[] teamArray;
        Collection t1 = m_team1.values();
        Collection t2 = m_team2.values();

        if( team == 1 )
            teamArray = (StrikeballPlayer[]) t1.toArray( new StrikeballPlayer[0] );
        else
            teamArray = (StrikeballPlayer[]) t2.toArray( new StrikeballPlayer[0] );

        if( index < 0 || index > teamArray.length - 1 ) {
            m_botAction.sendPrivateMessage( name, "Index out of bounds.  Must be between 0 and " + m_maxTeamSize + "." );
            return;
        }

        StrikeballPlayer sbP = teamArray[index];

        if( sbP == null ) {
            m_botAction.sendPrivateMessage( name, "Can not locate index." );
            return;
        }

        if( team == 1 )
            m_team1.remove( sbP.getName() );
        else
            m_team2.remove( sbP.getName() );

        // NOTE: Does NOT store player.

        m_botAction.sendPrivateMessage( name, "Removed " + sbP.getName() + " at index " + index + "." );
    }



    /** Debug: Sets game state manually.
     * @param name Ref's name
     * @param state State to set to, as defined by STATE_* finals:
     * 0 - Inactive
     * 1 - Loaded
     * 2 - Lineups
     * 3 - Ready
     * 4 - Playing
     */
    public void cmd_dbg_state( String name, int state ) {
        if( state < 0 || state > STATE_PLAYING ) {
            m_botAction.sendPrivateMessage( name, "Invalid state.  Must be a number from 1 to " + STATE_PLAYING + "." );
            return;
        }
        do_changeState( state );
    }



    /** Debug: Gets info on player
     * @param name Ref's name
     * @param playerName Player's name on which to retrieve info.
     */
    public void cmd_dbg_info( String name, String playerName ) {
        StrikeballPlayer sbP = do_getSBPlayer( playerName );

        if( sbP == null ) {
            sbP = (StrikeballPlayer) m_allPlayers.get( playerName );
            if( sbP == null ) {
                m_botAction.sendPrivateMessage( name, "Player not found (either active or inactive)." );
                return;
            }
        }

        m_botAction.sendPrivateMessage( name, sbP.getName() + " of " + sbP.getTeamName() + " (Team " + sbP.getTeam() + ")" );
        m_botAction.sendPrivateMessage( name, "Lagged out: " + ( sbP.isLaggedOut()?"yes":"no" ) + "  Warned out: " + ( sbP.isWarnedOut()?"yes":"no" ) );
        String[] warnings = sbP.getWarningInfo();

        try {
            m_botAction.sendPrivateMessage( name, "Warning #1: " + warnings[0] );
            m_botAction.sendPrivateMessage( name, "Warning #2: " + warnings[1] );
            m_botAction.sendPrivateMessage( name, "Warning #3: " + warnings[2] );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Unexpected error occurred while displaying warnings!  Please report to upper staff." );
        }

    }



    /** Debug: Clears player status entirely, including scores, warnings, lag status, etc.
     * @param name Ref's name
     * @param playerName Name of player to reset
     */
    public void cmd_dbg_clear( String name, String playerName ) {
        StrikeballPlayer sbP = do_getSBPlayer( playerName );

        if( sbP == null ) {
            sbP = (StrikeballPlayer) m_allPlayers.get( playerName );
            if( sbP == null ) {
                m_botAction.sendPrivateMessage( name, "Player not found (either active or inactive)." );
                return;
            }
        }

        sbP.resetPlayer();
    }




//****************************************************************************************
//******************************** (5). PLAYER COMMANDS **********************************
//****************************************************************************************

    /** The player !lagout command handler.  Players are allowed to use this command only
     * after they have been lagged out for 30 seconds, after which the bot will send them
     * a message that they may reenter.
     * @param name Ref's name
     */
    public void cmd_lagout( String name ) {
        if( m_gameState < STATE_READY ) {
            m_botAction.sendPrivateMessage( name, "You may return to the game when players are allowed to enter into the game." );
            return;
        }

        StrikeballPlayer sbP = do_getSBPlayer( name );

        if( sbP == null ) {
            m_botAction.sendPrivateMessage( name, "You are not registered as playing." );
            return;
        }

        if( !sbP.isLaggedOut() ) {
            m_botAction.sendPrivateMessage( name, "You have not lagged out.  If you were fully disconnected you will need to be re-!add'ed by the ref." );
            return;
        }
        if( sbP.isWarnedOut() ) {
            m_botAction.sendPrivateMessage( name, "You have had 3 warnings and can not return to the game!" );
            return;
        }
        if( sbP.canDoLagout() ) {
            m_botAction.sendPrivateMessage( name, "You are not allowed to return to the game yet.  Please wait." );
            return;
        }

        int team = sbP.getTeam();

        if( team == 1 ) {
            if( m_currentRound % 2 == 1 ) {
                m_botAction.setShip( sbP.getName(), 1 );
                m_botAction.setFreq( sbP.getName(), 0 );
            } else {
                m_botAction.setShip( sbP.getName(), 2 );
                m_botAction.setFreq( sbP.getName(), 1 );
            }
        } else {
            if( m_currentRound % 2 == 1 ) {
                m_botAction.setShip( sbP.getName(), 2 );
                m_botAction.setFreq( sbP.getName(), 1 );
            } else {
                m_botAction.setShip( sbP.getName(), 1 );
                m_botAction.setFreq( sbP.getName(), 0 );
            }
        }

        sbP.setLaggedOut( false );
    }




//****************************************************************************************
//************************************ (6). EVENTS ***************************************
//****************************************************************************************

    /** Joins twsbl arena, proper chats and gets a list of operators when bot logs on.
     * @param event Event object containing all pertinent event info.
     */
    public void handleEvent( LoggedOn event ) {
        m_botAction.joinArena( m_defaultArena );
        m_botAction.sendUnfilteredPublicMessage( "?chat=robodev" );
        m_opList = m_botAction.getOperatorList();
    }



    /** Sets ReliableKills 1 on arena join to make sure bot receives every packet.
     * @param event Event object containing all pertinent event info.
     */
    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }



    /** Handles all messages to the bot, only allowing ZH+ to use ref commands.
     * @param event Event object containing all pertinent event info.
     */
    public void handleEvent(Message event) {
        String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
        if (name == null) return;


        String message = event.getMessage();

        if( message.startsWith( "!lagout" ))
            cmd_lagout( name );

        if( m_opList.isZH( name ))
            handleCommand( name, message );
    }



    /** Increments score, goals, & assists
     */
    public void handleEvent( SoccerGoal event ) {
        if( m_gameState < STATE_PLAYING )
            return;


        StrikeballPlayer sbP;
        StrikeballPlayer ass_sbP;
        boolean hasAssist = false;
        int freq = event.getFrequency();

        // If we are in an even round (ie, usually second round), swap freq #s to keep score straight
        if( m_currentRound % 2 == 0 )
            if( freq == 0 )
                freq = 1;
            else
                freq = 0;

        if( m_gameState != STATE_PLAYING )
            return;

        sbP = do_getSBPlayer( m_carrierID );
        ass_sbP = do_getSBPlayer( m_prevCarrierID );

        if( sbP == null ) {
            if( freq == 0 ) {
                m_botAction.sendArenaMessage( "GOAL for " + m_team1Name + ".  Score: [" + m_team1Score + " - " + m_team2Score + "]" );
                m_team1Score++;
            } else {
                m_botAction.sendArenaMessage( "GOAL for " + m_team2Name + ".  Score: [" + m_team1Score + " - " + m_team2Score + "]" );
                m_team2Score++;
            }

            return;
        }


        // Check for assist
        if( ass_sbP != null )
            if( sbP.getTeam() == ass_sbP.getTeam() )
                hasAssist = true;


        if( freq == 0 ) {
            m_team1Score++;
            if( hasAssist ) {
                m_botAction.sendArenaMessage( "GOAL for " + m_team1Name + " by " + sbP.getName() + ", assist by " + ass_sbP.getName() + ".");
                m_botAction.sendArenaMessage( "Score: " + m_team1Name + " [" + m_team1Score + " - " + m_team2Score + "] " + m_team2Name );
                sbP.incGoals();
                ass_sbP.incAssists();
            } else {
                m_botAction.sendArenaMessage( "GOAL for " + m_team1Name + " by " + sbP.getName() + ".");
                m_botAction.sendArenaMessage( "Score: " + m_team1Name + " [" + m_team1Score + " - " + m_team2Score + "] " + m_team2Name );
                sbP.incGoals();
            }

        } else {
            m_team2Score++;
            if( hasAssist ) {
                m_botAction.sendArenaMessage( "GOAL for " + m_team2Name + " by " + sbP.getName() + ", assist by " + ass_sbP.getName() + ".");
                m_botAction.sendArenaMessage( "Score: " + m_team1Name + " [" + m_team1Score + " - " + m_team2Score + "] " + m_team2Name );
                sbP.incGoals();
                ass_sbP.incAssists();
            } else {
                m_botAction.sendArenaMessage( "GOAL for " + m_team1Name + " by " + sbP.getName() + ".");
                m_botAction.sendArenaMessage( "Score: " + m_team1Name + " [" + m_team1Score + " - " + m_team2Score + "] " + m_team2Name );
                sbP.incGoals();
            }
        }

        m_carrierID = -1;
        m_prevCarrierID = -1;
    }



    public void handleEvent( BallPosition event ) {
        if( m_gameState < STATE_PLAYING )
            return;

        StrikeballPlayer sbP;            // new carrier
        StrikeballPlayer carrier_sbP;    // carrier before this event fired
        int pID = event.getPlayerID();

        if( m_gameState != STATE_PLAYING )
            return;

        // Ignore null values
        if( pID == -1 )
            return;


        // If this is the first carrier of the round, only carries can be incremented
        if( m_carrierID == -1 ) {
            sbP = do_getSBPlayer( pID );

            if( sbP == null )
                return;

            sbP.incCarries();

            m_carrierID = pID;
            return;
        }


        // If ball has changed hands, check for a steal/turnover
        if( pID != m_carrierID ) {
            sbP = do_getSBPlayer( pID );
            carrier_sbP = do_getSBPlayer( m_carrierID );

            if( sbP == null )
                return;

            if( carrier_sbP == null ) {
                sbP.incCarries();
                return;
            }

            sbP.incCarries();
            sbP.incSteals();
            carrier_sbP.incTurnovers();

            m_prevCarrierID = m_carrierID;
            m_carrierID = pID;
        }
    }



    public void handleEvent( PlayerDeath event ) {
        if( m_gameState < STATE_PLAYING )
            return;

        StrikeballPlayer killer = do_getSBPlayer( event.getKillerID() );
        StrikeballPlayer victim = do_getSBPlayer( event.getKilleeID() );

        if( killer == null || victim == null )
            return;

        killer.incKills();
            victim.incDeaths();
    }



    public void handleEvent( FrequencyShipChange event ) {
        if( m_gameState < STATE_PLAYING )
            return;

        if( event.getShipType() != 0 )
            return;

        if( event.getPlayerID() == -1 )
            return;

        StrikeballPlayer sbP = do_getSBPlayer( event.getPlayerID() );
        if( sbP == null )
            return;

        if( sbP.isWarnedOut() || sbP.isLaggedOut() )
            return;

        sbP.setLaggedOut( true );
    }



    public void handleEvent( PlayerLeft event ) {
        if( m_gameState < STATE_LINEUPS )
            return;

        StrikeballPlayer sbP = do_getSBPlayer( event.getPlayerID() );
        if( sbP == null )
            return;

        if( sbP.isWarnedOut() )      // Keep player in lineup regardless of leaving
            return;                  // if warned out!

        // Do not allow them to !lagout back into the game
        if( sbP.isLaggedOut() )
            sbP.cancelLagoutTask();

        if( sbP.getTeam() == 1 )
            m_team1.remove( sbP.getName() );
        else
            m_team2.remove( sbP.getName() );

        do_storePlayer( sbP );

    }



    public void handleEvent( PlayerEntered event ) {
        String dispString = "";
        String name = event.getPlayerName();
        if( name == null )
            return;

        if( m_gameState > STATE_INACTIVE )
            dispString = "Welcome to Strikeball League: " + m_team1Name + " vs " + m_team2Name + ".  We are currently ";
        else
            dispString = "Welcome to Strikeball League.  There is currently no game being played.";


        switch( m_gameState ) {
        case STATE_LOADED:
            dispString = dispString + "waiting to begin lineups.";
            break;
        case STATE_LINEUPS:
            dispString = dispString + "arranging lineups.";
            break;
        case STATE_READY:
            dispString = dispString + "preparing for round start.";
            break;
        case STATE_PLAYING:
            if( m_opList.isZH( name ))
                m_botAction.sendPrivateMessage( name, "STAFF MEMBER: Blueout is enabled; please do not speak in public chat." );
            dispString = dispString + "in game.";
            break;
        }

        m_botAction.sendPrivateMessage( name, dispString );

    }




//****************************************************************************************
//********************************* (7). COMMAND HANDLER *********************************
//****************************************************************************************

    /** Handles all ref commands.
     * @param name Ref's name
     * @param message Message (hopefully) containing comannd info.
     */
    public void handleCommand( String name, String message ){
        if( message.startsWith( "!go " )) {
            if( !m_opList.isER( name ) ) {
                m_botAction.sendPrivateMessage( name, "Only ERs and above may use restricted bot commands." );
                return;
            }
            do_go( name, message.substring( 4 ));

        } else if( message.startsWith( "!lock" )) {
            if( !m_opList.isER( name ) ) {
                m_botAction.sendPrivateMessage( name, "Only ERs and above may use restricted bot commands." );
                return;
            }

            if( m_locked ){
                m_botAction.sendPrivateMessage( name, "I'm already locked.  If you want to unlock me, use !unlock." );
                return;
            }
            m_botAction.sendPrivateMessage( name, "Locked.  Type !unlock when you're done." );
            m_locked = true;

        } else if( message.startsWith( "!unlock" )) {
            if( !m_opList.isER( name ) ) {
                m_botAction.sendPrivateMessage( name, "Only ERs and above may use restricted bot commands." );
                return;
            }

            if( !m_locked ){
                m_botAction.sendPrivateMessage( name, "I'm already unlocked." );
            } else {
                m_botAction.sendPrivateMessage( name, "Unlocked.  You may now move me with !go and !come or disconnect me with !die" );
                m_locked = false;
            }

        } else if( message.startsWith( "!die" )) {
            if( !m_opList.isER( name ) ) {
                m_botAction.sendPrivateMessage( name, "Only ERs and above may use restricted bot commands." );
                return;
            }

            if( !m_locked ){
                m_botAction.sendSmartPrivateMessage( name, "Shutting down..." );
                m_botAction.sendChatMessage( "I am dying at " + name + "'s request." );
                m_botAction.die();
            } else {
                m_botAction.sendPrivateMessage( name, "I am locked, sorry." );
            }

        } else if( message.startsWith( "!home" )) {
            if( !m_opList.isER( name ) ) {
                m_botAction.sendPrivateMessage( name, "Only ERs and above may use restricted bot commands." );
                return;
            }

            if( m_currentArena.equals( m_defaultArena ) && !m_locked ){
                m_botAction.sendSmartPrivateMessage( name, "I'm already home." );
            } else if( m_currentArena.equals( m_defaultArena ) && m_locked ){
                m_botAction.sendSmartPrivateMessage( name, "Unlocked.  I'm already home, though." );
            } else {
                m_botAction.sendSmartPrivateMessage( name, "Returning home." );
                m_locked = false;
                do_go( name, m_defaultArena );
            }

        } else if( message.startsWith( "!loadseason " )) {
            String[] names = returnParams( name, message );
            m_gameType = GAMETYPE_STANDARD;
            cmd_loadgame( name, names[0], names[1] );

        } else if( message.startsWith( "!loadplayoff " )) {
            String[] names = returnParams( name, message );
            m_gameType = GAMETYPE_PLAYOFF;
            cmd_loadgame( name, names[0], names[1] );

        } else if( message.startsWith( "!startpick" )) {
            cmd_startpick( name );

        } else if( message.startsWith( "!add " )) {
            String[] params = returnParams( name, message );
            try {
                int team = Integer.parseInt( params[1] );
                cmd_addplayer( name, params[0], team );
            } catch (Exception e) {
                m_botAction.sendPrivateMessage( name, "Format: !add player:team" );
            }

        } else if( message.startsWith( "!sub " )) {
            String[] names = returnParams( name, message );
            try {
                cmd_subplayer( name, names[0], names[1] );
            } catch (Exception e) {
                m_botAction.sendPrivateMessage( name, "Format: !sub player1:player2" );
            }

        } else if( message.startsWith( "!remove " )) {
            String[] params = message.split(" ", 2);
            cmd_removeplayer( name, params[1] );

        } else if( message.startsWith( "!startgame" )) {
            cmd_startgame( name );

        } else if( message.startsWith( "!roundstart" )) {
            cmd_roundstart( name );

        } else if( message.startsWith( "!restart" )) {
            cmd_restart( );

        } else if( message.startsWith( "!cancel" )) {
            if( !m_opList.isER( name ) ) {
                m_botAction.sendPrivateMessage( name, "Only ERs and above may use restricted bot commands." );
                return;
            }
            cmd_cancelgame( name );

        } else if( message.startsWith( "!warn " )) {
            String[] params = returnParams( name, message );
            try {
                int warnID = Integer.parseInt( params[1] );
                cmd_warnplayer( name, params[0], warnID );
            } catch (Exception e) {
            }

        } else if( message.startsWith( "!forfeit " )) {
            String[] params = returnParams( name, message );
            try {
                int team = Integer.parseInt( params[0] );
                int reason = Integer.parseInt( params[1] );
                cmd_forfeitgame( name, team, reason );
            } catch (Exception e) {
            }

        } else if( message.startsWith( "!help" )) {
            m_botAction.privateMessageSpam( name, helps );

        } else if( message.startsWith( "!debughelp" )) {
            m_botAction.privateMessageSpam( name, debughelps );


        // --- DEBUG COMMANDS (advanced users) ---

        } else if( !m_opList.isER( name ) ) {
            return;    // Don't process debug commands for ZHs

        } else if( message.startsWith( "!dbg-show" )) {
            cmd_dbg_show( name );

        } else if( message.startsWith( "!dbg-rem " )) {
            String[] params = returnParams( name, message );
            try {
                int team = Integer.parseInt( params[0] );
                int index = Integer.parseInt( params[1] );
                cmd_dbg_rem( name, team, index );
            } catch (Exception e) {
            }

        } else if( message.startsWith( "!dbg-state " )) {
            String[] params = message.split(" ", 2);
            try {
                int state = Integer.parseInt( params[1] );
                cmd_dbg_state( name, state );
            } catch (Exception e) {
            }

        } else if( message.startsWith( "!dbg-info " )) {
            String[] params = message.split(" ", 2);
            cmd_dbg_info( name, params[1] );

        } else if( message.startsWith( "!dbg-clr " )) {
            String[] params = message.split(" ", 2);
            cmd_dbg_clear( name, params[1] );
        }

    }




//****************************************************************************************
//********************************* (8). HELP / AUXILLARY ********************************
//****************************************************************************************

    final static String[] helps = {
        "StrikeballBot Commands:",
        "!loadseason TeamA:TeamB  - Starts a normal game between TeamA and B.",
        "!loadplayoff TeamA:TeamB - Starts a playoff game between TeamA and B.",
        "!startpick               - Starts lineup requests from captains.",
        "!add name:team#          - Add player name to team# (1 or 2)",
        "!startgame               - Starts the game, displaying lineups.",
        "!roundstart              - Starts round.  ***You MUST be in game & have ball!",
        "!restart                 - Restarts SOCCER GAME & resets arena score (*restart)",
        "!warn name:reason        - Warns player for 0-Misc, 1-Phasing, 2-ChPicking.",
        "!forfeit team#:reason    - Forfeits game for team# (team# loses):",
        "  Reasons: 0-Generic, 1-TooFewPlayers, 2-RuleBreak, 3-Cheating",
        "!debughelp               - Lists debug commands (advanced hosts only!)",
        "!lock, !unlock, !home    - Lock/unlock bot, or unlock and return home.",
        "!go <arena>              - Tells the bot to ?go to an arena",
        "!die                     - Forces botdeath."
    };

    final static String[] debughelps = {
        "StrikeballBot Debug Commands (Advanced users only):",
        "!dbg-show                - List all players with indexes (use w/ !dbg-rem)",
        "!dbg-rem team:index      - Remove a player on team at given index.",
        "!dbg-state num           - Sets the game to specified state:",
        "    0-Inactive, 1-Loaded, 2-Lineups, 3-Ready, 4-Playing.",
        "!dbg-info player         - Retrieves info on a player.",
        "!dbg-clear player        - Clears out player stats and warnings."
    };



    final static String[] logo = {
        ".      ____________________________ ____________________________________   .",
        ".    .'                           //                                   / L .",
        ".   /  _____   __   />  ______   /'  ,_   ___  _   _     __   _____   /  E .",
        ".   '___  \\/  / /    _-' ___ /     .' /     /    _// ,  | /  /    /  /   A .",
        ".  /'--'  /  / /  /| \\  /  //     \\  /  ___/ _   \\/     |/  /___ /  /___ G .",
        ". /      /  / /  / |  |/  //  /|   \\/     /      /  /|  /      //      / U .",
        "./______/__/ /__/ /___/__//__/ |___/_____/______/__/ |_/______//______/  E .",
        ".   Map: CrimsonX  Tileset: Shades Child  Settings: Jui Jitsu  Bot: qan    ."
    };


}