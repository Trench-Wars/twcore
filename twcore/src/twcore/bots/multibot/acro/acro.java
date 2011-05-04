package twcore.bots.multibot.acro;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Spy;
import twcore.core.util.Tools;

public class acro extends MultiModule{

    CommandInterpreter  m_commandInterpreter;
    Random              generator;
    // -1 = pregame; 0 = not playing; 1 = entering acro; 2 = voting
    int                 gameState = 0;
    int                 length = 0;
    int                 numIdeas = 0;
    int                 maxVote = -1;
    int                 round = 1;
    String              curAcro = "";
    HashMap<String, String> playerIdeas = new HashMap<String, String>();
    HashMap<String, String> playerVotes = new HashMap<String, String>();
    HashMap<String, String> playerScores = new HashMap<String, String>();
    Vector<String>      phrases;
    int                 votes[];
    Spy                 racismSpy;

    public void init()    {
        m_commandInterpreter = new CommandInterpreter( m_botAction );
        registerCommands();
        generator = new Random();
        racismSpy = new Spy( m_botAction );

        //for testing over chat
        m_botAction.sendUnfilteredPublicMessage( "?chat=acro,games" );
    }

    public void requestEvents(ModuleEventRequester events)    {
    }

    public boolean isUnloadable()   {
        return true;
    }
    
    /**
     * This method is called when this module is unloaded.
     */
    public void cancel(){
    	m_botAction.cancelTasks();
        gameState = 0;
    }

    private void spamChatMessage(String message) {
        m_botAction.sendChatMessage(1, message);
        m_botAction.sendChatMessage(2, message);
    }

    public void registerCommands()  {
        int acceptedMessages;

        acceptedMessages = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!start", acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!stop", acceptedMessages, this, "doStopGame" );
        m_commandInterpreter.registerCommand( "!showanswers", acceptedMessages, this, "doShowAnswers" );
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "doShowHelp" );
        m_commandInterpreter.registerDefaultCommand( Message.REMOTE_PRIVATE_MESSAGE, this, "doCheckPrivate" );

        acceptedMessages = Message.CHAT_MESSAGE | Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!pm", acceptedMessages, this, "doPm" );
    }

    public void doStartGame( String name, String message ) {
        if( m_botAction.getOperatorList().isER( name ) ) {
            if( gameState == 0 ) {
                gameState = -1;
                m_botAction.sendArenaMessage( "A randomly generated Acronym will be displayed, your goal is to write a sentence/phrase that matches the acronym then vote for the best! -" + m_botAction.getBotName(), 22 );
                spamChatMessage( "A randomly generated Acronym will be displayed, your goal is to write a sentence/phrase that matches the acronym then vote for the best! -" + m_botAction.getBotName());
                TimerTask preStart = new TimerTask() {
                    public void run() {
                        setUpShow();
                    }
                };
                m_botAction.scheduleTask( preStart, 10000 );
            }
        }
    }

    public void doStopGame(String name, String message) {
        if(m_botAction.getOperatorList().isER( name )) {
            m_botAction.cancelTasks();
            gameState = 0;
            m_botAction.sendArenaMessage("This game has been slaughtered by: " + name);
            spamChatMessage("This game has been slaughtered by: " + name);
        }
    }

    public void doShowAnswers(String name, String message) {
        if(m_botAction.getOperatorList().isModerator( name )) {
            if( gameState == 2 ) {
                Iterator<String> it = playerIdeas.keySet().iterator();
                String player, answer;
                while (it.hasNext()) {
                    player = it.next();
                    answer = playerIdeas.get(player);
                    m_botAction.sendSmartPrivateMessage( name, player + ":  " + answer );
                }
            } else {
                m_botAction.sendSmartPrivateMessage( name, "Currently the game isn't in the voting stage." );
            }
        }
    }

    public void setUpShow() {
        gameState = 1;
        length = Math.abs( generator.nextInt() ) % 2 + 4;
        m_botAction.sendArenaMessage( "Challenge #" + round + " : " + generateAcro( length ) );
        spamChatMessage( "Challenge #" + round + " : " + generateAcro( length ) );

        TimerTask end = new TimerTask() {
            public void run() {
                phrases = new Vector<String>();
                gameState = 2;
                m_botAction.sendArenaMessage( "Submitted Answers: " );
                spamChatMessage( "Submitted Answers: " );
                int i = 0;
                Set<String> set = playerIdeas.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext()) {
                    i++;
                    String curAnswer = (String) it.next();
                    m_botAction.sendArenaMessage( " " + i + "- " + playerIdeas.get( curAnswer ) );
                    spamChatMessage( " " + i + "- " + playerIdeas.get( curAnswer ) );
                    phrases.addElement( curAnswer + "%" + playerIdeas.get( curAnswer ) );
                }
                votes = new int[i];
                numIdeas = i;
                m_botAction.sendArenaMessage( "Vote: Private Message me the # of your favorite phrase! -" + m_botAction.getBotName(), 103 );
                spamChatMessage( "Vote: Private Message me the # of your favorite phrase! -" + m_botAction.getBotName());
                setUpVotes();
            }
        };
        m_botAction.scheduleTask( end, 46000 );
    }

    public void setUpVotes() {
        TimerTask vote = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "Round Winners: " );
                spamChatMessage( "Round Winners: " );
                for( int i = 0; i < phrases.size(); i++ ) {
                    if( votes[i] == maxVote ) {
                        String piece[] = Tools.stringChopper( phrases.elementAt( i ), '%' );
                        if( playerVotes.containsKey( piece[0] ) ) {

                            if( playerScores.containsKey( piece[0] ) ) {
                                int s = Integer.parseInt( playerScores.get( piece[0] ) );
                                s += 10;
                                playerScores.put( piece[0], ""+s );
                            } else
                                playerScores.put( piece[0], "10" );
                            m_botAction.sendArenaMessage( Tools.formatString(piece[0], 25 )+ " - " + piece[1].substring(1) );
                            spamChatMessage( Tools.formatString(piece[0], 25 )+ " - " + piece[1].substring(1) );
                        } else {
                            //TODO happens every message
                            m_botAction.sendArenaMessage( Tools.formatString(piece[0], 25 )+ " - " + piece[1].substring(1) + " (no vote/score)" );
                            spamChatMessage( Tools.formatString(piece[0], 25 )+ " - " + piece[1].substring(1) + " (no vote/score)" );
                        }
                    }
                }
                playerIdeas.clear();
                playerVotes.clear();
                phrases.clear();
                maxVote = -1;
                round++;
                if( round > 10 ) {
                    gameOver();
                } else {
                    TimerTask preStart = new TimerTask() {
                        public void run() {
                            setUpShow();
                        }
                    };
                    m_botAction.scheduleTask( preStart, 10000 );
                }
            }
        };
        m_botAction.scheduleTask( vote, 30000 );
    }

    public void gameOver() {
        TimerTask game = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "Game Over, Scores: ", 5 );
                spamChatMessage( "Game Over, Scores: ");
                Set<String> set = playerScores.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext()) {
                    String curAnswer = (String) it.next();
                    m_botAction.sendArenaMessage( Tools.formatString( curAnswer, 30 ) +"- " + playerScores.get( curAnswer ) );
                    spamChatMessage( Tools.formatString( curAnswer, 30 ) +"- " + playerScores.get( curAnswer ) );
                }
                playerScores.clear();
                gameState = 0;
                round = 1;
            }
        };
        m_botAction.scheduleTask( game, 10000 );
    }

    public void doCheckPrivate( String name, String message ) {
        if( gameState == 1 ) {
            String pieces[] = message.split( " +" );
            String pieces2[] = curAcro.split( " " );
            if( pieces.length == pieces2.length ) {
                boolean valid = true;
                for( int i = 0; i < pieces.length; i++ ) {
                    if( pieces[i].length() == 0 || pieces[i].toLowerCase().charAt(0) != pieces2[i].toLowerCase().charAt(0) )
                        valid = false;
                    if( pieces[i].contains("_") || pieces[i].contains("-") )
                        valid = false;
                }

                if( valid ) {
                	if( racismSpy.isRacist(message) ) {
                        m_botAction.sendUnfilteredPublicMessage("?cheater Racist acro: (" + name + "): " + message);
                        m_botAction.sendSmartPrivateMessage( name, "You have been reported for attempting to use racism in your answer." );
                		return;
                	}
                    if( !playerIdeas.containsKey( name ) ) {
                        playerIdeas.put( name, message );
                        m_botAction.sendSmartPrivateMessage( name, "Your answer has been recorded." );
                    } else {
                        playerIdeas.remove( name );
                        playerIdeas.put( name, message );
                        m_botAction.sendSmartPrivateMessage( name, "Your answer has been changed." );
                    }
                } else m_botAction.sendSmartPrivateMessage( name, "You have submitted an invalid acronym.  It must match the letters and not contain dashes/underscores." );

            } else m_botAction.sendSmartPrivateMessage( name, "You must use the correct number of letters!" );
        } else if( gameState == 2 ) {
            int vote = 0;
            try { vote = Integer.parseInt( message ); } catch (Exception e ) {}
            if( vote > 0 && vote <= numIdeas ) {
                try {
                    String cur     = phrases.elementAt( vote - 1);
                    String parts[] = Tools.stringChopper( cur, '%' );

                    if( playerVotes.containsKey( name.toLowerCase() ) ) {
                        m_botAction.sendSmartPrivateMessage( name, "You have already voted!." );
                        return;
                    }

                    if( !parts[0].toLowerCase().equals( name.toLowerCase() ) ) {
                        votes[vote-1]++;
                        playerVotes.put( name.toLowerCase(), name );
                        if( votes[vote-1] > maxVote ) maxVote = votes[vote-1];
                        m_botAction.sendSmartPrivateMessage( name, "Your vote has been counted." );
                    } else m_botAction.sendSmartPrivateMessage( name, "You cannot vote for your own." );
                } catch (Exception e) {
                    m_botAction.sendSmartPrivateMessage( name, "Unable to process your vote!  Please notify the host." );
                }
            } else m_botAction.sendSmartPrivateMessage( name, "Please enter a valid vote." );
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
            m_botAction.privateMessageSpam(name, getPlayerHelpMessage());
    }

    @Override
    public String[] getModHelpMessage() {
        String[] help = {
                "!start       - Starts a game of Acromania.",
                "!stop        - Stops a game currently in progress.",
                "!showanswers - Shows who has entered which answer.",
                "NOTE: This event should only be hosted by Mod+!"
        };
        return help;
    }

    public String[] getPlayerHelpMessage() {
        String[] help = {
                "Rules for Acromania: compose a sentence with the letters provided.",
                "PM your answers to me before the timer is up!",
                "Then vote for your favorite answer.  You can't vote for your own!",
                "If you don't vote for someone else's acro, you can't win."
        };
        return help;
    }

    public void doPm( String name, String message ){
        m_botAction.sendSmartPrivateMessage( name, "Now you can use :: to submit your answers." );
    }

    @Override
    public void handleEvent( Message event ) {
        m_commandInterpreter.handleEvent( event );
    }

}