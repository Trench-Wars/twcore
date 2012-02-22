package twcore.bots.multibot.acro2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Spy;
import twcore.core.util.Tools;
import twcore.core.util.StringBag;

public class acro2 extends MultiModule{

    CommandInterpreter  m_commandInterpreter;
    Random              generator;
    // gameState   -1 = pregame; 0 = not playing; 1 = entering acro; 2 = voting, 3 = waiting for host to submit acro
    int                 gameState = 0;
    int                 length = 0;
    int                 intOrder = 0;
    int					intAcroCount = 0;
    int                 round = 1;
    int                 intCustom = 0; // is set to 1 if each rounds acro will be submitted by host
    String              curAcro = "";
    String				CustomHost = "";
    HashMap<String, String> playerIdeas = new HashMap<String, String>();
    HashMap<String, Integer> playerVotes = new HashMap<String, Integer>();
    HashMap<String, Integer> playerScores = new HashMap<String, Integer>();
    HashMap<String, Integer> playerOrder = new HashMap<String, Integer>();
    HashMap<String, Integer> acroDisplay = new HashMap<String, Integer>();
    StringBag           playerNames = new StringBag();
    int                 votes[];
    Spy                 racismSpy;

    public void init()    {
        m_commandInterpreter = new CommandInterpreter( m_botAction );
        registerCommands();
        generator = new Random();
        racismSpy = new Spy( m_botAction );
    }

    public void requestEvents(ModuleEventRequester events)    {
    }

    public boolean isUnloadable()   {
        return true;
    }
    
    /*** This method is called when this module is unloaded. */
    public void cancel(){
    	gamereset();
    	m_botAction.cancelTasks();
    }

    public void registerCommands()  {
        int acceptedMessages;

        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!start", acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!startcustom", acceptedMessages, this, "doStartCustom" );
        m_commandInterpreter.registerCommand( "!setacro", acceptedMessages, this, "doSetAcro" );
        m_commandInterpreter.registerCommand( "!showanswers", acceptedMessages, this, "doShowAnswers" );
        m_commandInterpreter.registerCommand( "!stop", acceptedMessages, this, "doStopGame" );
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "doShowHelp" );
        m_commandInterpreter.registerCommand( "!rules", acceptedMessages, this, "doShowRules" );
        m_commandInterpreter.registerCommand( "!changes", acceptedMessages, this, "doShowChanges" );
        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "doCheckPrivate" );
    }

    public void doStartGame(String name,String message) {
        initGame(name);
    }

    public void initGame(String name) {
        if( m_botAction.getOperatorList().isER(name)) {
            if( gameState == 0 ) {
                gameState = -1;
                if (intCustom == 1) {
                	gameState = 3;
                	CustomHost = name;
                    m_botAction.sendArenaMessage("ACROMANIA BEGINS! Your host will submit acronyms - prepare your wit!  PM me with !rules to learn how to play. " + m_botAction.getBotName(),22);
                    m_botAction.sendPrivateMessage(name,"Custom Game Initalized.  Send !setacro LETTERS to set the letters for Round #1.");
                } else {
                	m_botAction.sendArenaMessage("ACROMANIA BEGINS! Random acronyms will be generated - prepare your wit!  PM me with !rules to learn how to play. -" + m_botAction.getBotName(),22);
                    TimerTask preStart = new TimerTask() {
                        public void run() {
                            setUpShow();
                        }
                    };
                    m_botAction.scheduleTask( preStart, 10000 );
                }
            }
        }
    }

    public void doSetAcro(String name,String message) {
        if (m_botAction.getOperatorList().isER(name)) {
            if (intCustom == 1) {
                if (gameState == 3) {
                    message = message.trim();
                    message = message.toUpperCase();
                    message = message.replaceAll(" ","");
                    if (message.length() > 8) {
                        m_botAction.sendPrivateMessage(name,"Please submit an acronym 8 characters or less.");
                    } else {
                    	curAcro = "";
                    	// convert LETTERS to L E T T E R S
                    	char letters[] = message.toCharArray();
                    	for (int x = 0; x < letters.length; x++) {
                    		curAcro = curAcro + " " + letters[x];
                    	}
                    	curAcro = curAcro.trim();
                        setUpShow();
                    }
                } else {m_botAction.sendPrivateMessage(name,"Round is not complete, please wait to submit next acronym.");}
            } else {m_botAction.sendPrivateMessage(name,"Game is currently running in regular mode (!start).  Host-submitted acronyms are not allowed.");}
        }
    }
    
    public void doStartCustom(String name, String message) {
        intCustom = 1;
        initGame(name);
    }

    public void doStopGame(String name, String message) {
        if(m_botAction.getOperatorList().isER( name )) {
        	gamereset();
            m_botAction.cancelTasks();
            m_botAction.sendArenaMessage("This game has been slaughtered by: " + name);
        }
    }

    public void gamereset() {
        intCustom = 0;
        intOrder = 0;
        round = 1;
        gameState = 0;
        curAcro = "";
        CustomHost = "";

        playerScores.clear();
        playerIdeas.clear();
        playerVotes.clear();
        playerOrder.clear();
        acroDisplay.clear();
    }
    
    public void doShowAnswers(String name, String message) {
        if(m_botAction.getOperatorList().isModerator( name )) {
            if( gameState == 2 ) {
                Iterator<String> it = playerIdeas.keySet().iterator();
                String player, answer;
                while (it.hasNext()) {
                    player = it.next();
                    answer = playerIdeas.get(player);
                    m_botAction.sendPrivateMessage( name, player + ":  " + answer );
                }
            } else {
                m_botAction.sendPrivateMessage( name, "Currently the game isn't in the voting stage." );
            }
        }
    }

    public void setUpShow() {
        gameState = 1;
        if (intCustom == 0) {
            length = Math.abs( generator.nextInt() ) % 2 + 4;
            curAcro = generateAcro(length);
        } // otherwise, the curAcro global has already been set by doSetAcro

        m_botAction.sendArenaMessage("TO ENTER, PM me a phrase that matches the challenge letters! -" + m_botAction.getBotName());
        m_botAction.sendArenaMessage("ACROMANIA Challenge #" + round + ": " + curAcro);

        TimerTask end = new TimerTask() {
            public void run() {
                gameState = 2;
                m_botAction.sendArenaMessage("ACROMANIA Entries: ");
                int i = 0;
                while (!playerNames.isEmpty()) {
                    i++;
                    String curPlayer = playerNames.grabAndRemove();
                    m_botAction.sendArenaMessage("--- " + i + ": " + playerIdeas.get(curPlayer));
                    acroDisplay.put(curPlayer, i);
                }
                votes = new int[i];
                intAcroCount = i;
                if (intAcroCount > 0) {
                	m_botAction.sendArenaMessage("VOTE: PM me the # of your favorite phrase! -" + m_botAction.getBotName(),103);
                } else {
                	m_botAction.sendArenaMessage("--- 0 entries submitted.");
                }
                setUpVotes();
            }
        };
        m_botAction.scheduleTask( end, 46000 );
    }
    
    public String getPlural(Integer intCount,String strWord) {
    	if (intCount > 1 || intCount == 0) {strWord += "s";} else {strWord += " ";}
    	return intCount + " " + strWord;
    }

    public void setUpVotes() {
        TimerTask vote = new TimerTask() {
            public void run() {
                String strFastPlayer = "";
                String strWinners = "";
                int intMostVotes = 0;
                int numVotes = 0;
                int i = 0;

                numVotes = votes.length;
                // Determine the highest # of votes any acro received
                for (i = 0; i < numVotes; i++) {
                    if (votes[i] > intMostVotes) {intMostVotes = votes[i];}
                }

                // Determine the fastest ACRO that received at least one vote
                int intCurAcro = 0;
                int intCurOrder = 0;
                int intFastest = 100;
                String strCurPlayer = "";
                Set<String> acroSet = acroDisplay.keySet();
                Iterator<String> acroIT = acroSet.iterator();
                while (acroIT.hasNext()) {
                	strCurPlayer = acroIT.next();
                	intCurAcro = acroDisplay.get(strCurPlayer);
                	intCurOrder = playerOrder.get(strCurPlayer);
                	if ((intCurOrder < intFastest) && votes[intCurAcro-1] > 0) {
                		intFastest = intCurOrder;
                		strFastPlayer = strCurPlayer;
                	}
                }

                int intPlayerScore = 0;
                int intPlayerBonus = 0;
                int intPlayerVotes = 0;
                int intPlayerTotal = 0;
                int VotedForVotes = 0;
                m_botAction.sendArenaMessage("ROUND " + round + " RESULTS: ");

                acroIT = acroSet.iterator();
                while (acroIT.hasNext()) {
                	strCurPlayer = acroIT.next();
                	intCurAcro = acroDisplay.get(strCurPlayer);
                	intPlayerVotes = votes[intCurAcro-1];
                	
                	String playerVotedWinner = "-";
                    String playerNotes = "";

                    // Calculate bonus points
                    intPlayerBonus = 0;

                    // +5 pts for receiving the most votes (round winner)
                    if (intPlayerVotes == intMostVotes) {
                        intPlayerBonus += 5;
                        if (strWinners.length() > 0) {strWinners += ", ";}
                    	strWinners += strCurPlayer;
                    }

                    // +1 pt for voting for round winner
                    if (playerVotes.containsKey(strCurPlayer)) {
                        VotedForVotes = votes[playerVotes.get(strCurPlayer)-1];
                        if (VotedForVotes == intMostVotes) {
                            intPlayerBonus += 1;
                            playerVotedWinner = "*";
                        }
                    }

                    // +2 pts if this was the fastest entry with any votes
                    if (strCurPlayer.equals(strFastPlayer)) {intPlayerBonus += 2;}

                    // Update players running score, only if they voted
                    if (playerVotes.containsKey(strCurPlayer)) {
                        if (!playerScores.containsKey(strCurPlayer)) {
                            intPlayerScore = 0;
                        } else {
                            intPlayerScore = playerScores.get(strCurPlayer);
                        }
                        // Score for round = bonus + number of votes their acro received
                        intPlayerScore += intPlayerBonus + intPlayerVotes;
                        playerScores.put(strCurPlayer,intPlayerScore);
                    } else {
                        playerNotes += " [NOVOTE/NOSCORE]";
                    }
                    intPlayerTotal = intPlayerVotes + intPlayerBonus;
                    m_botAction.sendArenaMessage(playerVotedWinner + " " + Tools.formatString(strCurPlayer,14) + " " + getPlural(intPlayerTotal,"pt") + " (" + getPlural(intPlayerVotes,"vote") + "): " + playerIdeas.get(strCurPlayer) + playerNotes);
                }
                if (!strWinners.equals("")) {
                	m_botAction.sendArenaMessage("* = These players voted for the winner(s).");
                	if (!strFastPlayer.equals("")) {
                    	m_botAction.sendArenaMessage("ROUND WINNER(s): " + strWinners + " (most votes), " + strFastPlayer + " (fastest acro with a vote)");
                	} else {
                    	m_botAction.sendArenaMessage("ROUND WINNER(s): " + strWinners + " with the most votes");
                	}
                } else {
                	m_botAction.sendArenaMessage("ROUND WINNER(s): None!  You all lose!");
                }
                playerIdeas.clear();
                playerVotes.clear();
                playerOrder.clear();
                acroDisplay.clear();
                
                round++;
                if(round > 10) {
                    gameOver();
                } else {
                	if (intCustom == 1) {
                		gameState = 3;
                		m_botAction.sendPrivateMessage(CustomHost,"Send !setacro LETTERS to set the letters for the next round.");
                	} else {
                        TimerTask preStart = new TimerTask() {
                            public void run() {
                                setUpShow();
                            }
                        };
                        m_botAction.scheduleTask(preStart,10000);
                	}
                }
            }
        };
        m_botAction.scheduleTask(vote,36000);
    }

    public void gameOver() {
        TimerTask game = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage("GAME OVER! FINAL SCORES: ",5);
                Set<String> set = playerScores.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext()) {
                    String curAnswer = it.next();
                    m_botAction.sendArenaMessage("--- " + Tools.formatString(curAnswer,14) + ": " + playerScores.get(curAnswer));
                }
                gamereset();
            }
        };
        m_botAction.scheduleTask(game,10000);
    }

    public void doCheckPrivate( String name, String message ) {
        if (gameState == 1) {
            String pieces[] = message.split( " +" );
            String pieces2[] = curAcro.split( " " );
            if( pieces.length == pieces2.length ) {
                boolean valid = true;
                if (message.length() > 70) {
                    valid = false;
                } else {
                    for (int i = 0; i < pieces.length; i++) {
                        if (pieces[i].length() == 0 || pieces[i].toLowerCase().charAt(0) != pieces2[i].toLowerCase().charAt(0)) {valid = false;}
                    }
                }

                if (valid) {
                	if(racismSpy.isRacist(message) ) {
                        m_botAction.sendUnfilteredPublicMessage("?cheater Racist acro: (" + name + "): " + message);
                        m_botAction.sendPrivateMessage(name,"You have been reported for attempting to use racism in your answer." );
                		return;
                	}
                	if(!playerIdeas.containsKey(name)) {
                        m_botAction.sendPrivateMessage(name,"Your answer has been recorded.");
                        playerNames.add(name);
                    } else {
                        playerIdeas.remove(name);
                        playerOrder.remove(name);
                        m_botAction.sendPrivateMessage(name,"Your answer has been changed.");
                    }
                    intOrder++;
                    playerOrder.put(name,intOrder);
                    playerIdeas.put(name,message);
                } else {m_botAction.sendPrivateMessage(name,"You have submitted an invalid acronym.  It must match the letters given and be 70 characters or less.");}
            } else m_botAction.sendPrivateMessage(name,"You must use the correct number of letters!");
        } else if (gameState == 2) {
            int vote = 0;
            int intAcroNum = 0;
            try {vote = Integer.parseInt( message );} catch (Exception e) {}
            if (vote > 0 && vote <= intAcroCount) {
            	//if (playerIdeas.containsKey(name)) {
            		intAcroNum = acroDisplay.get(name);
            		if (intAcroNum != vote) {
            			votes[vote-1]++;
            			if (playerVotes.containsKey(name)) {
            				int lastVote = playerVotes.get(name);
            				votes[lastVote-1]--;
            				playerVotes.remove(name);
            				m_botAction.sendPrivateMessage(name,"Your vote has been changed.");
            			} else {
            				m_botAction.sendPrivateMessage(name,"Your vote has been counted.");
            			}
            			playerVotes.put(name,vote);
            		} else {m_botAction.sendPrivateMessage(name,"You cannot vote for your own.");}
            	//} else {m_botAction.sendPrivateMessage(name,"Only players who submitted an entry may vote this round.");}
            } else {m_botAction.sendPrivateMessage(name,"Please enter a valid vote.");}
        }
    }


    public String generateAcro( int size ) {
        String acro = "";
        for( int i = 0; i < size; i ++ ) {
            int x = Math.abs( generator.nextInt() ) % 72;
            if( x > -1 && x < 3 ) acro += "A ";
            else if( x > 2 && x < 6 ) acro += "B ";
            else if( x > 5 && x < 9 ) acro += "C ";
            else if( x > 8 && x < 12 ) acro += "D ";
            else if( x > 11 && x < 15 ) acro += "E ";
            else if( x > 14 && x < 18 ) acro += "F ";
            else if( x > 17 && x < 21 ) acro += "G ";
            else if( x > 20 && x < 24 ) acro += "H ";
            else if( x > 23 && x < 27 ) acro += "I ";
            else if( x > 26 && x < 30 ) acro += "J ";
            else if( x > 29 && x < 31 ) acro += "K ";  //third as likely
            else if( x > 30 && x < 34 ) acro += "L ";
            else if( x > 33 && x < 37 ) acro += "M ";
            else if( x > 36 && x < 40 ) acro += "N ";
            else if( x > 39 && x < 43 ) acro += "O ";
            else if( x > 42 && x < 46 ) acro += "P ";
            else if( x > 45 && x < 49 ) acro += "Q ";
            else if( x > 48 && x < 52 ) acro += "R ";
            else if( x > 51 && x < 55 ) acro += "S ";
            else if( x > 54 && x < 59 ) acro += "T ";
            else if( x > 58 && x < 62 ) acro += "U ";
            else if( x > 61 && x < 64 ) acro += "V "; //two-third as likely
            else if( x > 63 && x < 67 ) acro += "W ";
            else if( x > 66 && x < 68 ) acro += "X "; //third as likely
            else if( x > 67 && x < 71 ) acro += "Y ";
            else if( x > 70 && x < 72 ) acro += "Z "; //third as likely
        }
        curAcro = acro;
        return acro;

    }

    public void doShowHelp( String name, String message ) {
        if( ! m_botAction.getOperatorList().isER( name ) )
            m_botAction.privateMessageSpam( name, getPlayerHelpMessage() );
    }

    public String[] getModHelpMessage() {
        String[] help = {
                "ACROMANIA v2.0 BOT COMMANDS",
                "!start       - Starts a game of acromania.",
                "!stop        - Stops a game currently in progress.",
                "!startcustom - Starts a game of acromania.  Host must",
                "               !setacro each round.",
                "!setacro     - Used to set the acronym for the next round",
                "               (can be used only during a !startcustom game.",
                "!rules   - Displays game rules.",
                "!changes - Display changes since v1.0.",
                "!showanswers - Shows who has entered which answer.",
                "NOTE: This event should only be hosted by Mod+!"
        };
        return help;
    }

    public String[] getPlayerHelpMessage() {
        String[] help = {
                "ACROMANIA v2.0 BOT COMMANDS",
                "!help    - Displays this help message.",
                "!rules   - Displays game rules.",
                "!changes - Display changes since v1.0."
        };
        return help;
    }

    public void doShowRules(String name,String message) {
        String[] help = {
                "ACROMANIA v2.0 RULES:",
                "Each game consists of 10 rounds.  At the start of each round,",
                "a randomly generated acronym will be displayed.  PM me a phrase",
                "that matches the letters provided.  Then vote for your favorite",
                "phrase.  You must submit an acro each round to be able to vote",
                "during that round.  Points will be given as follows:",
                "+1 point for each vote that your acro receives",
                "+1 bonus point if you voted for the winning acro",
                "+2 bonus points for the fastest acro that received a vote",
                "+5 bonus points for receiving the most votes for the round",
                "After votes are tallied, all submitted acros are displayed along",
                "with the # of votes received + the bonus points received.",
                "Players marked with an asterisk (*) voted for the winning acro.",
                "NO POINTS are given to players that did not vote.",
                "Win the game by earning the most points in 10 rounds."
        };
        m_botAction.privateMessageSpam(name,help);
    }

    public void doShowChanges(String name,String message) {
        String[] help = {
                "ACROMANIA CHANGES v1.0 to v2.0:",
                " - underscore and dash characters now allowed in acros",
                " - can now change your vote during the voting round",
                " - only players who submitted an acro can vote each round",
                " - new scoring system, see !rules",
                " - voting period now 36 seconds (was 30)",
                " - fixed end of round display of acros to match original entries",
                "   (punctuation no longer stripped)",
                " - length of submitted acros can be no more than 70 characters",
                " - added option for host to specify each rounds acros, based",
                "   on user request, random names in the channel, a theme, etc.",
                " - current entries are now randomized before being displayed."
        };
        m_botAction.privateMessageSpam(name,help);
    }
    
    public void handleEvent( Message event ) {
        m_commandInterpreter.handleEvent( event );
    }

}