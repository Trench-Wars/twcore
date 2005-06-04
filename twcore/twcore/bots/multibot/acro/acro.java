package twcore.bots.multibot.acro;

import twcore.core.*;
import twcore.bots.multibot.*;
import java.util.*;

public class acro extends MultiModule{

	CommandInterpreter  m_commandInterpreter;
	Random				generator;
	int					gameState = 0;
	int					length = 0;
	int					numIdeas = 0;
	int					maxVote = -1;
	int					round = 1;
	String				curAcro = "";
	HashMap				playerIdeas = new HashMap();
	HashMap				playerVotes = new HashMap();
	HashMap				playerScores = new HashMap();
	Vector				phrases;
	int					votes[];

    public void init()    {
		m_commandInterpreter = new CommandInterpreter( m_botAction );
		registerCommands();
		generator = new Random();
	}

	public void requestEvents(EventRequester events)	{
		events.request(EventRequester.MESSAGE);
	}

	public String[] getModHelpMessage()	{
		final String[] helpText = {
			"!start - Starts a game of acromania",
			"!die   - Kills me",
			"NOTE: This event should only be hosted by Mod+!"
        };

        return helpText;
	}

	public boolean isUnloadable()	{
		return true;
	}

	public void registerCommands()	{
	        int acceptedMessages;

	        acceptedMessages = Message.PRIVATE_MESSAGE;
	        m_commandInterpreter.registerCommand( "!start", acceptedMessages, this, "doStartGame" );
	        m_commandInterpreter.registerCommand( "!die", acceptedMessages, this, "doDie" );
			m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "showHelp" );
	        m_commandInterpreter.registerDefaultCommand( Message.PRIVATE_MESSAGE, this, "doCheckPrivate" );
    }

    public void doStartGame( String name, String message ) {
	    	if( m_botAction.getOperatorList().isER( name ) ) {
		    	if( gameState == 0 ) {
		    		gameState = -1;
			    	m_botAction.sendArenaMessage( "A randomly generated Acronym will be displayed, your goal is to write a sentence/phrase that matches the acronym then vote for the best! -" + m_botAction.getBotName(), 22 );
					TimerTask preStart = new TimerTask() {
						public void run() {
							setUpShow();
						}
					};
					m_botAction.scheduleTask( preStart, 10000 );
				}
			}
	    }

	    public void setUpShow() {
			gameState = 1;
			length = Math.abs( generator.nextInt() ) % 2 + 4;
			m_botAction.sendArenaMessage( "Challenge #" + round + " : " + generateAcro( length ) );

	    	TimerTask end = new TimerTask() {
	    		public void run() {
	    			phrases = new Vector();
	    			gameState = 2;
	    			m_botAction.sendArenaMessage( "Submitted Answers: " );
	    			int i = 0;
	    			Set set = playerIdeas.keySet();
	                Iterator it = set.iterator();
	                while (it.hasNext()) {
	                	i++;
	                	String curAnswer = (String) it.next();
	                	m_botAction.sendArenaMessage( " " + i + "- " + playerIdeas.get( curAnswer ) );
	                	phrases.addElement( curAnswer + "%" + playerIdeas.get( curAnswer ) );
	                }
	                votes = new int[i];
	                numIdeas = i;
	                m_botAction.sendArenaMessage( "Vote: Personal Message me the # of your favorite phrase! -" + m_botAction.getBotName(), 103 );
	                setUpVotes();
	    		}
	    	};
	    	m_botAction.scheduleTask( end, 46000 );
	    }

	    public void setUpVotes() {
	    	TimerTask vote = new TimerTask() {
	    		public void run() {
	    			m_botAction.sendArenaMessage( "Round Winners: " );
	    			for( int i = 0; i < phrases.size(); i++ )
	    				if( votes[i] == maxVote ) {
	    					String piece[] = Tools.stringChopper( ((String)phrases.elementAt( i )), '%' );
	    					if( playerScores.containsKey( piece[0] ) ) {
	    						int s = Integer.parseInt( (String)playerScores.get( piece[0] ) );
	    						s += 10;
	    						playerScores.put( piece[0], ""+s );
	    					} else
	    						playerScores.put( piece[0], "10" );
	    					m_botAction.sendArenaMessage( Tools.formatString(piece[0], 25 )+ " - " + piece[1].substring(1) );
	    				}
						playerIdeas.clear();
						playerVotes.clear();
						phrases.clear();
						maxVote = -1;
						round++;
						if( round > 10 ) gameOver();
						else {
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
			    	Set set = playerScores.keySet();
			        Iterator it = set.iterator();
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

	    public void doCheckPrivate( String name, String message ) {
	    	if( gameState == 1 ) {
		    	String pieces[] = message.split( " " );
		    	String pieces2[] = curAcro.split( " " );
		    	if( pieces.length == pieces2.length ) {
		    		boolean valid = true;
		    		for( int i = 0; i < pieces.length; i++ ) {
		    			if( pieces[i].toLowerCase().charAt(0) != pieces2[i].toLowerCase().charAt(0) )
		    				valid = false;
		    		}
		    		if( valid ) {
		    			if(	!playerIdeas.containsKey( name ) ) {
		    				playerIdeas.put( name, message );
		    				m_botAction.sendPrivateMessage( name, "Your answer has been recorded." );
		    			} else {
		    				playerIdeas.remove( name );
		    				playerIdeas.put( name, message );
		    				m_botAction.sendPrivateMessage( name, "Your answer has been changed." );
		    			}
		    		} else m_botAction.sendPrivateMessage( name, "You have submitted an invalid acronym" );

		    	} else m_botAction.sendPrivateMessage( name, "You can only use the set letters for the acronym" );
		    } else if( gameState == 2 ) {
		    	int vote = 0;
		    	try { vote = Integer.parseInt( message ); } catch (Exception e ) {}
		    	if( vote > 0 && vote <= numIdeas ) {
		    	    try {
		    	        String cur     = (String)phrases.elementAt( vote - 1);
	    				String parts[] = Tools.stringChopper( cur, '%' );

	    				if( playerVotes.containsKey( name ) ) {
	    					m_botAction.sendPrivateMessage( name, "You have already voted!." );
	    				    return;
	    				}

	    				if( !parts[0].toLowerCase().equals( name.toLowerCase() ) ) {
	    					votes[vote-1]++;
	    					playerVotes.put( name, name );
	    					if( votes[vote-1] > maxVote ) maxVote = votes[vote-1];
	    					m_botAction.sendPrivateMessage( name, "Your vote has been counted." );
	    				} else m_botAction.sendPrivateMessage( name, "You cannot vote for your own." );
		    	    } catch (Exception e) {
		    	        m_botAction.sendPrivateMessage( name, "Unable to process your vote!  Please notify the host." );
		    	    }
		    	} else m_botAction.sendPrivateMessage( name, "Please enter a valid vote." );
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

	    public void handleEvent( Message event ) {
			m_commandInterpreter.handleEvent( event );
    }

}