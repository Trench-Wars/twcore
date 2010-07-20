package twcore.bots.multibot.story;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiModule;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Spy;
import twcore.core.util.Tools;

/* The story module was heavily adopted from acro (not acro2).
 * Many of the methods and variable declarations are similar,
 * if not the same. However there were a few new methods added 
 * in to accommodate the needs of this particular event. 
 * 
 * GAME SUMMARY -
 * Story will allow players to put together a story using the
 * phrases they submit. To start off the story, the host can
 * either submit a phrase for an opener, or a randomly generated
 * opener will be provided. Much like acro, there will be 10
 * rounds where players will submit phrases and vote on their
 * favorite ones to be appended to the story. A tie in votes
 * between two phrases will result in the phrase voted for first
 * becoming the appended phrase. Parameters for submitted phrases
 * are based off of the number of words allowed in a submitted 
 * phrase. At games end, the winning phrases for each round
 * will all be written out along with the final scores.
 * 
 *  @author: Veloce
 */


public class story extends MultiModule{

    CommandInterpreter  m_commandInterpreter;
    Random              generator;
    //-2 = host submission of opening line; -1 = pregame; 0 = not playing; 1 = entering acro; 2 = voting
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
    String				openingLine;//the opening line for the story
    String				phrasewin;//the winning phrase holder
    Vector<String>		fullstory;//the winning phrases are appended
    String			    fullstory2[];//at games end, fullstory is converted to this array to output using arenaMessageSpam method
    int					customNumberRounds;//custom number of rounds specified by the host
    int					NumberRounds;//the number of rounds for the game, default is 10, unless changed by a !customstart command
    boolean				custom;//boolean will be true if there is a custom number of rounds
    int					FREQ_NOTPLAYING = 666;
    
    /*
     * 
     */
    public void init()    {
        m_commandInterpreter = new CommandInterpreter( m_botAction );
        registerCommands();
        generator = new Random();
        racismSpy = new Spy( m_botAction );
        
    }

    
    /*
     * 
     */
    public void requestEvents(ModuleEventRequester events)    {
    }

    
    /*
     * 
     */
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

    
    /*
     * register the commands with the bots
     */
    public void registerCommands()  {
        int acceptedMessages;

        acceptedMessages = Message.PRIVATE_MESSAGE;
        m_commandInterpreter.registerCommand( "!start", acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!stop", acceptedMessages, this, "doStopGame" );
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "doShowHelp" );
        m_commandInterpreter.registerCommand( "!opener", acceptedMessages, this, "doGetOpener" );
        m_commandInterpreter.registerCommand( "!customstart", acceptedMessages, this, "doCustomStart");
        m_commandInterpreter.registerCommand( "!forcenp", acceptedMessages, this, "doForceNp" );
        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "doCheckPrivate" );
    }
    
    
    /*
     * Handles the !customstart #ofRounds command. It invokes the handleRoundEvent()
     * method, then starts the game like normal.
     */
	public void doCustomStart (String name, String message)	{
    	if( m_botAction.getOperatorList().isER( name )) {
    		 if( gameState == 0 )	{
           		handleRoundEvent(message);
           		if( custom == true)	{
           			doStartGame(name,message);
           		}
    		 }
    	}
    }
     /*
     * This method will display instructions to both the host and to the players. 
     * When this method begins, the bot will also begin the time frame in which 
     * the host will be able to submit an opening phrase for the story.
     */
    public void doStartGame( String name, String message ) {
        if( m_botAction.getOperatorList().isER( name ) ) {
            if( gameState == 0 ) {
            	gameState = -2;
            	m_botAction.sendPrivateMessage(name, name + ", you have 45 seconds to private message me with the opening line for this story. I will randomly choose one of my openers if you can't come up with an opening phrase.");
                m_botAction.sendPrivateMessage(name, "Example: :bot:!opener All was quiet in the midnight hours of Trench Wars...");
                if (custom == true)	{
                	NumberRounds = customNumberRounds;
                    m_botAction.sendArenaMessage( "Listen for the opener to the story. Add your phrases to the story and be sure to vote on the best one! If two phrases tie in votes, the appended phrase will be the phrase voted for first. " + NumberRounds + " rounds!   -" + m_botAction.getBotName(), 22 );
                    }	else {
                    	NumberRounds = 10;
                    	m_botAction.sendArenaMessage( "Listen for the opener to the story. Add your phrases to the story and be sure to vote on the best one! If two phrases tie in votes, the appended phrase will be the phrase voted for first. 10 rounds!   -" + m_botAction.getBotName(), 22 );
                    }
                
                openingLine = null;
                phrasewin = null;
    	     
                TimerTask opener = new TimerTask() {
                	 public void run() {
                		 opener();
                	 }
                };
                m_botAction.scheduleTask( opener, 45000);
            } 
        }
    }    
    
    
    /*
     * !stop command, to slaughter the game
     */
    public void doStopGame(String name, String message) {
        if(m_botAction.getOperatorList().isER( name )) {
            m_botAction.cancelTasks();
            gameState = 0;
            m_botAction.sendArenaMessage("This game has been slaughtered by " + name);
        }
    }


	/*
     * This method will be invoked after an !opener command is sent to the bot.
     * It will take in the message and send it to the handleEvent method and
     * also confirm that the opener has been submitted.
     */
    public void doGetOpener (String name, String message)	{
    	if( m_botAction.getOperatorList().isER( name )) {
            handleOpenerEvent(message);
            m_botAction.sendPrivateMessage(name, "Your opener has been submitted. The game will begin momentarily.");
    	}
    }
   
    
	/*
     * Following the !start command (doStartGame method), the host is asked to
     * submit a phrase. The hostOpener() method will be triggered if the host
     * chose to submit a phrase. Otherwise, the randomly generated phrase will
     * be submitted in the gameOpener() method.
     */
     public void opener()	{
    	if (openingLine != null)	{
    		hostOpener();
    	}
    	else	{
    		gameOpener();
    	}
    }
    
     /*
      * The host's submitted phrase will be appended to the fullstory vector
      */
    public void hostOpener() {
    	gameState = -1;
    	m_botAction.sendArenaMessage(openingLine);
	    fullstory = new Vector<String>();
	    fullstory.addElement(openingLine);
	    
	    TimerTask preStart = new TimerTask() {
	        public void run() {
	            setUpShow();
	        }
	     };
	     m_botAction.scheduleTask( preStart, 10000 );
	    
    }
    
    
    /*
     * The game will randomly generate a number that corresponds to an
     * opening phrase, which will then be put in the openingLine string.
     * This method is only invoked when the moderator has chosen not to, 
     * or was not able to successfully submit a phrase in the alloted time.
     */
    public void gameOpener() {
    	
    	gameState = -1;
    	
        int x = Math.abs( generator.nextInt() ) % 120;
	        if( x >= 0 && x <= 20 ) {
	        	openingLine = ("It was a warm bright sunny day outside...");
	        }
	        else if( x > 20 && x <= 40 ) {
	        	openingLine = ("My best friends and I were on our way out for the night...");
	        }
	        else if( x > 40 && x <= 60 ) {
	        	openingLine = ("I planned on staying inside for the day...");
	        }
	        else if( x > 60 && x <= 80 ) {
	        	openingLine = ("Tonight is going to be a great night...");
	        }
	        else if( x > 80 && x <= 100 ) {
	        	openingLine = ("My family and I were taking a vacation...");
	        }
	        else if( x > 100 && x <= 120 ) {
	        	openingLine = ("I think I just had the worst day of my life...");
	        }
	        
	    	m_botAction.sendArenaMessage(openingLine);
	    	fullstory = new Vector<String>();
	    	fullstory.addElement(openingLine);
	        TimerTask preStart = new TimerTask() {
	            public void run() {
	            	setUpShow();
	            }
	        };
	        m_botAction.scheduleTask( preStart, 10000 );
	        
	        	
    }
    
    
    /*
     * setUpShow() method will display the phrase parameters (how many words
     * are allowed in the particular round's phrase). It will also schedule
     * the timer task which will be used to take in the submitted phrases 
     * and enter the voting stage.
     */
    public void setUpShow() {
        gameState = 1;
        
        m_botAction.sendArenaMessage( "Phrase #" + round + " : " + generateAcro() );

        TimerTask end = new TimerTask() {
            public void run() {
                phrases = new Vector<String>();
                gameState = 2;
                m_botAction.sendArenaMessage( "Submitted Phrases: " );
                int i = 0;
                Set<String> set = playerIdeas.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext()) {
                    i++;
                    String curAnswer = (String) it.next();
                    m_botAction.sendArenaMessage( " " + i + "- " + playerIdeas.get( curAnswer ) );
                    phrases.addElement( curAnswer + "-" + playerIdeas.get( curAnswer ) );
                }
                votes = new int[i];
                numIdeas = i;
                m_botAction.sendArenaMessage( "Vote: Private Message me the # of your favorite phrase! -" + m_botAction.getBotName(), 103 );
                setUpVotes();
            }
        };
        m_botAction.scheduleTask( end, 46000 );
    }
    
    
    /*
     * setUpVotes() method will display the round winners and award the points
     * for the round winners. It will append the winning phrase to the fullstory
     * vector. If it is beyond round 10, this method will enter the game over
     * actions. Otherwise, it will return to the setUpShow method to begin another
     * round.
     */
    public void setUpVotes() {
        TimerTask vote = new TimerTask() {
            public void run() {
            	if (phrases != null)	{
	                m_botAction.sendArenaMessage( "Round Winners: " );
	                for( int i = 0; i < phrases.size(); i++ ) {
	                    if( votes[i] == maxVote ) {
	                        String piece[] = Tools.stringChopper( phrases.elementAt( i ), '-' );
	                        phrasewin = piece[1].toString();
	                        if( playerVotes.containsKey( piece[0] ) ) {
	                        	
	                            if( playerScores.containsKey( piece[0] ) ) {
	                                int s = Integer.parseInt( playerScores.get( piece[0] ) );
	                                s += 10;
	                                playerScores.put( piece[0], ""+s );
	                            } else
	                                playerScores.put( piece[0], "10" );
	                            m_botAction.sendArenaMessage( Tools.formatString(piece[0], 25 )+ " - " + piece[1].substring(1) );
	                        } else {
	                            m_botAction.sendArenaMessage( Tools.formatString(piece[0], 25 )+ " - " + piece[1].substring(1) + " (no vote/score)" );
	                        }
	                    }
	                }
            	}
            	
            	
                if (phrasewin != null)	{
                	fullstory.addElement(phrasewin);
                }
                else	{
                	phrasewin = "No winner this round.";
                	fullstory.addElement(phrasewin);
                }
                
                phrasewin = null;
                playerIdeas.clear();
                playerVotes.clear();
                phrases.clear();
                maxVote = -1;
                round++;
      
	            if( round > NumberRounds ) {
	                String[] fullstory2 = new String[fullstory.size()];
	                fullstory.toArray(fullstory2);
	                arenaMessageSpam(fullstory2);
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
    
    
    /*
     * At game's end, this method is triggered. Previously, the fullstory 
     * vector was storing the round winners. I decided to convert it to an 
     * array to use the arenaMessageSpam method to output the story. Following
     * this method will be the original acro method at game's end, gameOver().
     */
    public void arenaMessageSpam(final String[] fullstory2) {
    	
    	if(fullstory2 != null) {
	    	for(int i = 0; i < fullstory2.length; i++) {	    	
    		m_botAction.sendArenaMessage(fullstory2[i]);
	    	}
	    gameOver();	
    	}
   }
    
    
    /*
     * This method will display the final scores
     */
    public void gameOver() {
        TimerTask game = new TimerTask() {
            public void run() {
                m_botAction.sendArenaMessage( "Game Over, Scores: ", 5 );
                Set<String> set = playerScores.keySet();
                Iterator<String> it = set.iterator();
                while (it.hasNext()) {
                    String curAnswer = (String) it.next();
                    m_botAction.sendArenaMessage( Tools.formatString( curAnswer, 30 ) +"- " + playerScores.get( curAnswer ) );
                }
                playerScores.clear();
                gameState = 0;
                round = 1;
            }
        };
        m_botAction.scheduleTask( game, 10000 );
    }

    
    /*
     * The doCheckPrivate method will compare number of spaces between 
     * the submitted phrases and the current phrase parameters (I will
     * explain why in the generateAcro() method). In short, this method
     * will handle either votes or phrase submissions.
     */
    public void doCheckPrivate( String name, String message ) {
        if( gameState == 1 ) {
            String pieces[] = message.split( " " );
            String pieces2[] = curAcro.split( " " );
            if( pieces.length == pieces2.length ) {
                boolean valid = true;
                for( int i = 0; i < pieces.length; i++ ) {
                    if( pieces[i].contains("_") || pieces[i].contains("-") )
                        valid = false;
                }

                if( valid ) {
                	if( racismSpy.isRacist(message) ) {
                        m_botAction.sendUnfilteredPublicMessage("?cheater Racist phrase (story module): (" + name + "): " + message);
                        m_botAction.sendPrivateMessage( name, "You have been reported for attempting to use racism in your answer." );
                		return;
                	}
                    if( !playerIdeas.containsKey( name ) ) {
                        playerIdeas.put( name, message );
                        m_botAction.sendPrivateMessage( name, "Your phrase will be submitted for vote." );
                    } else {
                        playerIdeas.remove( name );
                        playerIdeas.put( name, message );
                        m_botAction.sendPrivateMessage( name, "Your phrase has been changed." );
                    }
                } else m_botAction.sendPrivateMessage( name, "You have submitted an invalid phrase.  It must match the length and not contain dashes/underscores." );

            } else m_botAction.sendPrivateMessage( name, "You must use the correct number of words in your phrase!" );
        } else if( gameState == 2 ) {
            int vote = 0;
            try { vote = Integer.parseInt( message ); } catch (Exception e ) {}
            if( vote > 0 && vote <= numIdeas ) {
                try {
                    String cur     = phrases.elementAt( vote - 1);
                    String parts[] = Tools.stringChopper( cur, '-' );

                    if( playerVotes.containsKey( name.toLowerCase() ) ) {
                        m_botAction.sendPrivateMessage( name, "You have already voted!." );
                        return;
                    }

                    if( !parts[0].toLowerCase().equals( name.toLowerCase() ) ) {
                        votes[vote-1]++;
                        playerVotes.put( name.toLowerCase(), name );
                        if( votes[vote-1] > maxVote ) maxVote = votes[vote-1];
                        m_botAction.sendPrivateMessage( name, "Your vote has been counted." );
                    } else m_botAction.sendPrivateMessage( name, "You cannot vote for your own." );
                } catch (Exception e) {
                    m_botAction.sendPrivateMessage( name, "Unable to process your vote!  Please notify the host." );
                }
            } else m_botAction.sendPrivateMessage( name, "Please enter a valid vote." );
        } 
    }

    
    /*
     * The generateAcro() method will generate the phrase length for each round.
     * Note that there are varying number of periods (".") for each. The only
     * way I could think to test the phrase length was to test for the number
     * of spaces in the user's submitted phrase and the phrase displaying the
     * length I wanted. That being said, I added periods to make the generated
     * phrase match the length of the necessary user phrase submissions.
     */
    public String generateAcro() {
        String acro = "";
            int x = Math.abs( generator.nextInt() ) % 80;
            	if (x > 0 || x <= 80)
		            if( x > 0 && x < 20 ) {
		            	acro += "A 5 Word Phrase .";
		            }
		            else if( x >= 20 && x < 40 ) {
		            	acro += "A 6 Word Phrase . .";
		            }
		            else if( x >= 40 && x < 60 ) {
		            	acro += "A 7 Word Phrase . . .";
		            }
		            else if( x >= 60 && x <= 81 ) {
		            	acro += "An 8 Word Phrase . . . .";
		            }

		         		
        
        curAcro = acro;
        return acro;

    }

    
    /*
     * Invoked by a !help command to the bot. 
     */
    public void doShowHelp( String name, String message ) {
        if( ! m_botAction.getOperatorList().isER( name ) )
            m_botAction.privateMessageSpam( name, getPlayerHelpMessage() );
    }
    
    
    /*
     * Host help message
     */
    public String[] getModHelpMessage() {
        String[] help = {
        		"!start                   - Starts a game of Story, with 10 rounds.",
        		"!customstart #ofRounds   - Starts a game of Story, with the number of rounds specified.",
                "!stop                    - Stops a game currently in progress.",
                "!opener                  - Sets the story opener in the alloted timeframe.",
                "!showstory               - Displays the current story.",
                "NOTE: This event should only be hosted by Mod+!"
        };
        return help;
    }

    
    /*
     * Player help message array
     */
    public String[] getPlayerHelpMessage() {
        String[] help = {
                "Rules for Story: compose a phrase with the correct number of words specified.",
                "PM your answers to me before the timer is up!",
                "Then vote for your favorite answer.  You can't vote for your own!",
                "If you don't vote for someone else's phrase, you can't win.",
                "If you wish to see the story so far, private message me with '!showstory'"
        };
        return help;
    }

    

    /*
     * Necessary method to handle incoming messages to the bot. This method will
     * also handle the !showstory command.
     */
 
    @Override
    public void handleEvent( Message event ) {
           	m_commandInterpreter.handleEvent( event );
           	String name = m_botAction.getPlayerName(event.getPlayerID());
           	if(event.getMessage().startsWith("!showstory"))
           	    doShowStory(name);
    }
    
    
    /*
     * This method will be invoked when the player sends a !showstory command
     * to the bot. The bot will create an array of the story to send to the player. 
     * Each time this method is invoked, a new ShowStory array will be created.
     */
    
    public void doShowStory ( String name ) {
        try{
            m_botAction.privateMessageSpam( name, fullstory.toArray(new String[fullstory.size()]) );
        }
        catch(Exception e){
            e.printStackTrace();
        }
     
    }
    
    
    /*
     * Prior to the start of the game, this method will be invoked if the host
     * uses the !customStart command, which will allow the host to use a custom
     * number of rounds. After type "!customstart 4" for example, this method will
     * take in the 4, and store it in the CustomNumberRounds variable.
     */
    
    public void handleRoundEvent(String message) {
    	if (gameState == 0 && message != null)	{
    		customNumberRounds = Integer.parseInt(message);
        	custom = true;
    	}
    }
    
 
    
    
    /*
     * Keeping in mind that gameState = -2 means the game is in the state where
     * the host can submit the opening line, the handleEvent() method will
     * take in the message submitted by the host and store it in the openingLine
     * string.
     */
    public void handleOpenerEvent(String message) {
    	if (gameState == -2 )	{
    		openingLine = message.trim();
    	}
    } 
    

    
   
}