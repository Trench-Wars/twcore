package twcore.bots.radiobot;

import java.util.*;


import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.ArenaJoined;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;
import twcore.core.events.SQLResultEvent;

/**
 * To assist in hosting radio (while not requiring a host to have staff access).
 * based on relaybot
 * @author Flibb
 * Update @author Derek (dezmond)
 */

public final class radiobot extends SubspaceBot {
    private EventRequester m_req;
    private OperatorList m_opList;
    private LinkedList<String> m_loggedInList;
    private String m_currentPassword = "";
    private LinkedList<String> m_alreadyZoned;
    private String m_currentHost = "";
    private String m_url = "";
    private Poll m_currentPoll = null;
    private boolean m_announcing = false;
    private boolean m_someoneHosting = false;
    private long m_timeStarted;
    private long m_timeToClearZone;
    private long m_timeOfLastZone = 0;
   

    private RadioQueue m_shoutouts;
    private RadioQueue m_requests;
    private RadioQueue m_topics;
    private RadioQueue m_questions;

	private final static String NO_HOST = "No one is currently hosting.";
    private final static int MAX_ANNOUNCE_LINES = 3;
    private final static long SIX_HOURS = 21600000;
    private final static long TEN_MINUTES = 600000;
    private final static long THIRTY_MINUTES = 1800000;
   

    private String[] m_announcement = { "", "", "" };
    private int m_announceLength = 0;
    private String m_welcome = "";
    final String        mySQLHost = "website";
    BotSettings         m_botSettings;

    /** Creates a new instance of radiobot */
    public radiobot(BotAction botAction) {
        super(botAction);
        m_req = botAction.getEventRequester();
        m_req.request(EventRequester.LOGGED_ON);
        m_req.request(EventRequester.MESSAGE);
        m_req.request(EventRequester.PLAYER_ENTERED);
        m_req.request( EventRequester.ARENA_JOINED );
        m_opList = botAction.getOperatorList();
        m_loggedInList = new LinkedList<String>();
        m_alreadyZoned = new LinkedList<String>();
        
        m_currentPassword = m_botAction.getBotSettings().getString("ServPass");
        if(m_currentPassword == null)
        	m_currentPassword = "";

        m_shoutouts = new RadioQueue("Shoutout");
        m_requests = new RadioQueue("Song");
        m_topics = new RadioQueue("Topic");
        m_questions = new RadioQueue("Question");

        m_timeStarted = System.currentTimeMillis();
        m_timeToClearZone = m_timeStarted + SIX_HOURS;
    }

    public boolean isIdle() {
        return !m_someoneHosting;
    }

    public void handleEvent(LoggedOn event) {
        BotSettings config = m_botAction.getBotSettings();
        String chat = config.getString( "chat" );
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + chat );
        m_botAction.joinArena("radio");
        m_botAction.setMessageLimit(10);
       
    }
    
    public void handleEvent (ArenaJoined event) {
        m_botAction.sendChatMessage(m_botAction.getBotName() + " is here!");
    }
       
    
    public void handleEvent(PlayerEntered event) {
		if(!m_welcome.equals("")) {
			m_botAction.sendPrivateMessage(event.getPlayerID(), m_welcome);
		}
    }


        public void handleEvent(Message event) {
        	
    	if(event.getMessageType() != Message.PRIVATE_MESSAGE)
        return;
        	
        	 int id = event.getPlayerID();
             String name = m_botAction.getPlayerName(id);
             if(name == null)
             	return;
		String message = event.getMessage();
		boolean isLoggedIn = m_loggedInList.contains(name);
		boolean isCurrentHost = m_someoneHosting && isLoggedIn && m_currentHost.equals(name);
		boolean isER = m_opList.isER(name);

		//make command part lowercase
		message = message.trim();
		int indexOfSpace = message.indexOf(' ');
		if(indexOfSpace > 0)
			message = message.substring(0, indexOfSpace).toLowerCase() + message.substring(indexOfSpace);
		else
			message = message.toLowerCase();

		/**
		 * Handle !help
		 */
		if(message.startsWith("!help")) {
			m_botAction.sendPrivateMessage(id, m_someoneHosting ? "The current host is " + m_currentHost : NO_HOST);
            m_botAction.privateMessageSpam(id, pubHelp);
			if(isLoggedIn) {
            	m_botAction.privateMessageSpam(id, staffHelp);
	            if(isCurrentHost) {
    	    		m_botAction.privateMessageSpam(id, currentRadioHostHelp);
	            }
			}
			if(isER) {
				m_botAction.privateMessageSpam(id, modHelp);
			}
			return;
        }

        /**
         * Handle logged in commands
         */
        if(isLoggedIn && handleStaffMessage(name, id, message)) {
        	return;
        }

        /**
         * Handle current host only commands
         */
        if(isCurrentHost && handleCurrentHostOnly(name, id, message, event.getSoundCode())) {
        	return;
        }

        /**
         * Handle public commands
         */
        if(handlePrivateMessage(name, id, message)) {
        	return;
        }

		/**
		 * Handle <ER>+ commands
		 */
		if(isER && handleModMessage(name, id, message)) {
			return;
		}

		/**
		 * Handle poll votes
		 */
		if(m_currentPoll != null){
			m_currentPoll.handlePollCount(name, message);
        }
    }


	/**
	 * Handle logged in commands
	 */
    private boolean handleStaffMessage(String name, int id, String message) {

    	boolean handled = false;

		if(message.startsWith("!host")) {
        	handled = true;
            if(!m_someoneHosting){
                m_currentHost = name;
                if(!m_announcing) {
                    m_botAction.scheduleTaskAtFixedRate(
                    	new AnnounceTask(), 10000, 150000);
                    m_announcing = true;
                }
                m_someoneHosting = true;
                m_botAction.sendPrivateMessage(id, "You are now the current host. Do not abuse the green messages, or write anything inappropriate. Thanks!");
                m_botAction.sendChatMessage(name +" has enabled current hosting commands.");
            } else {
                m_botAction.sendPrivateMessage(id, "Someone is already hosting. Wait until they are done, or get a <ER>+ to !unhost them first.");
            }

        } else if(message.startsWith("!who")) {
        	handled = true;
            m_botAction.sendPrivateMessage(id, "Radio hosts who are logged in:");
            Iterator<String> i = m_loggedInList.iterator();
            String who;
            while(i.hasNext()) {
            	who = i.next();
                m_botAction.sendPrivateMessage(id, who + (who.equals(m_currentHost) ? " (current host)" : ""));
            }

        } else if(message.startsWith("!status")) {
        	handled = true;
        	long now = System.currentTimeMillis();
            long ontime = now - m_timeStarted;

        	if(!m_someoneHosting) {
        		m_botAction.sendPrivateMessage(id, NO_HOST);
        	} else {
	            long nextzone = (m_timeOfLastZone + TEN_MINUTES - now) / 1000;
        		m_botAction.sendPrivateMessage(id, "Current host: " + m_currentHost + " - Can zone: "
        			+ (m_alreadyZoned.contains(m_currentHost) ? "no"
        			: (nextzone <= 0) ? "yes" : "in " + (nextzone / 60) + " mins " + (nextzone % 60) + " secs"));
        	}
            m_botAction.sendPrivateMessage(id,
            	"Topics:" + m_topics.size() + "/" + m_topics.getMax()
            	+ " Requests:" + m_requests.size() + "/" + m_requests.getMax()
            	+ " Questions:" + m_questions.size() + "/" + m_questions.getMax()
            	+ " Shoutouts:" + m_shoutouts.size() + "/" + m_shoutouts.getMax());
            m_botAction.sendPrivateMessage(id, "Poll running: " + (m_currentPoll != null));
            m_botAction.sendPrivateMessage(id, "Welcome: " + m_welcome);
            m_botAction.sendPrivateMessage(id, "URL: " + m_url);
            

            m_botAction.sendPrivateMessage(id, "Online for " + (ontime / 1000 / 60 / 60) + " hours and " + (ontime / 1000 / 60 % 60) + " minutes.");

        } else if(!m_currentHost.equals(name)
        		&& (message.startsWith("!poll ")
        		|| message.startsWith("!arena ")
		        || message.startsWith("!zone ")
		        || message.startsWith("!endpoll"))) {
        	handled = true;
            m_botAction.sendPrivateMessage(id, "Sorry, only the current radio host may use that command."
            	+ " If you are hosting, use the !host command.");
        
   
        } else if (message.startsWith("!sex")) {
		    Random r = new Random();
		      int randInt = Math.abs(r.nextInt()) % 14;
		    if( randInt == 1 ){
		    m_botAction.sendChatMessage( "O yea baby keep hosting it that way OMG OMG YAAAA BABY!" );
		    } else if(randInt == 2){
		    m_botAction.sendChatMessage( "You're so sexy...GOD YEA!" );
		    } else if(randInt == 3){
		    m_botAction.sendChatMessage( "Unf Unf!!" );
		    } else if(randInt == 4){
		    m_botAction.sendChatMessage( "Why Do you want sex now?" );
		    } else if(randInt == 5){
		    m_botAction.sendChatMessage( "I'M BUSY!" );
		    } else if(randInt == 6){
		    m_botAction.sendChatMessage( "Can you repeat that?" );
		    } else if(randInt == 7){
		    m_botAction.sendChatMessage( "Gonna Ride you like a horse!" );
		    } else if(randInt == 8){
		    m_botAction.sendChatMessage( "I'm here to amuse the hosts!" );

		    } 
		      
		    
		    
		    }

        return handled;
    }


	/**
	 * Handle current host only commands
	 */
	@SuppressWarnings("fallthrough")
    private boolean handleCurrentHostOnly(String name, int id, String message, int sound) {

    	boolean handled = false;

		if(message.startsWith("!arena ") && name.equals(m_currentHost)) {
        	handled = true;
			m_botAction.sendArenaMessage(message.substring(7) + " -" + m_currentHost
				, sound == 12 ? 2 : sound);

		} else if(message.startsWith("!poll ")) {
        	handled = true;
            if(m_currentPoll != null) {
                m_botAction.sendPrivateMessage(name, "A poll is currently in session."
                	+ " End this poll before beginning another one.");
                return true;
            }
            StringTokenizer izer = new StringTokenizer(message.substring(6), ":");
            int tokens = izer.countTokens();
            if(tokens < 2) {
                m_botAction.sendPrivateMessage(id, "Sorry but the poll format is wrong.");
                return true;
            }

            String[] polls = new String[tokens];
            int i = 0;
            while(izer.hasMoreTokens()) {
                polls[i] = izer.nextToken();
                i++;
            }

            m_currentPoll = new Poll(polls);

        } else if(message.startsWith("!endpoll")) {
        	handled = true;
            if(m_currentPoll == null) {
                m_botAction.sendPrivateMessage(id, "There is no poll running right now.");
            } else {
                m_currentPoll.endPoll();
                m_currentPoll = null;
            }

        } else if(message.startsWith("!zone ")) {
        	handled = true;
        	long now = System.currentTimeMillis();
   	        if(now >= m_timeToClearZone) {
	            m_alreadyZoned.clear();
	            m_timeToClearZone += SIX_HOURS;
	        }
            if(m_alreadyZoned.contains(name)) {
                m_botAction.sendPrivateMessage(id, "Sorry, you've already used your zone message. Get an <ER>+ to grant you another.");
            } else if(now < m_timeOfLastZone + TEN_MINUTES) {
            	m_botAction.sendPrivateMessage(id, "Sorry, you must wait "
            		+ ((m_timeOfLastZone + TEN_MINUTES - now) / 1000 / 60) + " minutes and "
            		+ ((m_timeOfLastZone + TEN_MINUTES - now) / 1000 % 60) + " seconds before you may zone.");
            } else {
                m_botAction.sendZoneMessage(message.substring(6) + " -" + name, 2);
                m_alreadyZoned.add(name);
                m_timeOfLastZone = now;
            }

        } else if(message.startsWith("!nextshout")) {
        	handled = true;
        	int i = 1;
        	if(message.startsWith("!nextshout ")) {
	        	try {
	        		i = Integer.parseInt(message.substring(11));
	        	} catch(NumberFormatException e) {
	        		i = 1;
	        	}
        	}
        	i = i < m_shoutouts.size() ? i : m_shoutouts.size();
        	for(; i > 0; i--) {
        		m_botAction.sendPrivateMessage(id, "Shout: " + m_shoutouts.remove());
        	}

        } else if(message.startsWith("!nextreq")) {
        	handled = true;
        	int i = 1;
        	if(message.startsWith("!nextreq ")) {
	        	try {
	        		i = Integer.parseInt(message.substring(9));
	        	} catch(NumberFormatException e) {
	        		i = 1;
	        	}
        	}
        	i = i < m_requests.size() ? i : m_requests.size();
        	for(; i > 0; i--) {
        		m_botAction.sendPrivateMessage(id, "Reqst: " + m_requests.remove());
        	}

        } else if(message.startsWith("!nexttop")) {
        	handled = true;
        	int i = 1;
        	if(message.startsWith("!nexttop ")) {
	        	try {
	        		i = Integer.parseInt(message.substring(9));
	        	} catch(NumberFormatException e) {
	        		i = 1;
	        	}
        	}
        	i = i < m_topics.size() ? i : m_topics.size();
        	for(; i > 0; i--) {
        		m_botAction.sendPrivateMessage(id, "Topic: " + m_topics.remove());
        	}

        } else if(message.startsWith("!nextques")) {
        	handled = true;
        	int i = 1;
        	if(message.startsWith("!nextques ")) {
	        	try {
	        		i = Integer.parseInt(message.substring(10));
	        	} catch(NumberFormatException e) {
	        		i = 1;
	        	}
        	}
        	i = i < m_questions.size() ? i : m_questions.size();
        	for(; i > 0; i--) {
        		m_botAction.sendPrivateMessage(id, "Quest: " + m_questions.remove());
        	}

        } else if(message.startsWith("!nextall")) {
        	handled = true;
       		m_botAction.sendPrivateMessage(id, "Shout: " + m_shoutouts.remove());
       		m_botAction.sendPrivateMessage(id, "Reqst: " + m_requests.remove());
    		m_botAction.sendPrivateMessage(id, "Topic: " + m_topics.remove());
    		m_botAction.sendPrivateMessage(id, "Quest: " + m_questions.remove());

        } else if(message.startsWith("!clear")) {
        	handled = true;
        	m_shoutouts.clear();
        	m_requests.clear();
        	m_topics.clear();
        	m_questions.clear();
        	m_botAction.sendPrivateMessage(id, "All requests cleared.");

        } else if(message.startsWith("!addannounce ")) {
        	handled = true;
        	if(m_announceLength >= MAX_ANNOUNCE_LINES) {
        		m_botAction.sendPrivateMessage(id, "Too many announce lines. (Do !clrannounce)");
        	} else {
	        	m_announcement[m_announceLength++] = message.substring(13);
	        	m_botAction.sendPrivateMessage(id, "Message added.");
        	}

        } else if(message.startsWith("!clrannounce")) {
        	handled = true;
			m_announceLength = 0;
			m_botAction.sendPrivateMessage(id, "Announcement cleared.");

        } else if(message.startsWith("!setmax ")) {
        	handled = true;

			String[] params = message.substring(8).split(" +");
			int count = params.length;
			int[] nums = new int[count];

			for(int i = 0, j; i < count; i++) {
				try {
					j = Integer.parseInt(params[i]);
					if(j < 0) j = 0;
					else if(j > 100) j = 100;
				} catch(NumberFormatException e) {
					j = 20;
				}
				nums[i] = j;
			}
			switch(count) {
				case 4:
					m_shoutouts.setMax(nums[3]);
				case 3:
					m_questions.setMax(nums[2]);
				case 2:
					m_requests.setMax(nums[1]);
				case 1:
					m_topics.setMax(nums[0]);
		        	m_botAction.sendPrivateMessage(id, "New limits: Topics=" + m_topics.getMax()
		        		+ " Requests=" + m_requests.getMax()
		        		+ " Questions=" + m_questions.getMax()
		        		+ " Shoutouts=" + m_shoutouts.getMax());
					break;
				default:
					m_botAction.sendPrivateMessage(id, "Too many parameters.");
			}

        } else if(message.startsWith("!viewannounce")) {
        	handled = true;
        	for(int i = 0; i < m_announceLength; i++) {
        		m_botAction.sendPrivateMessage(id, m_announcement[i]);
        	
        	
       }} else if(message.startsWith("!setwelcome ")) {
			handled = true;
			m_welcome = message.substring(12);
			m_botAction.sendPrivateMessage(id, "Welcome message set.");

       } else if(message.startsWith("!welcomeoff")) {
			handled = true;
			m_welcome = "";
			m_botAction.sendPrivateMessage(id, "Welcome message turned off.");
			
       } else if(message.startsWith("!seturl ")) {
           handled = true;
           m_url = message.substring(8);
           m_botAction.sendPrivateMessage(id, "URL set to " + m_url);

        } else if(message.startsWith("!unhost")) {
        	handled = true;
            m_botAction.cancelTasks();
            m_announcing = false;
            m_botAction.sendArenaMessage(m_currentHost + " has signed off the radio, thank you for listening!");
            m_botAction.sendChatMessage(name +" has finished hosting radio.");
            m_currentHost = "";
            m_someoneHosting = false;
            
        } else if(message.startsWith("!ask")) {
            handled = true;
            long now = System.currentTimeMillis();
            m_botAction.sendUnfilteredPublicMessage("?help The radio host is requesting a zoner (" + ((now - m_timeOfLastZone) / 1000 / 60) + " minutes and "
                    + ((now - m_timeOfLastZone) / 1000 % 60) + " seconds since last zone)" );
        }
        
        return handled;
    }


	/**
	 * Handle public commands
	 */
    private boolean handlePrivateMessage(String name, int id, String message) {

    	boolean handled = false;

		if(!m_someoneHosting
				&& (message.startsWith("!shoutout ")
				|| message.startsWith("!request ")
				|| message.startsWith("!topic ")
				|| message.startsWith("!question "))) {
			handled = true;
			m_botAction.sendPrivateMessage(id, NO_HOST);

		} else if(message.startsWith("!shoutout ")) {
			handled = true;
			m_shoutouts.handleAdd(id, name, message.substring(10));

	    } else if(message.startsWith("!request ")) {
			handled = true;
			m_requests.handleAdd(id, name, message.substring(9));

	    } else if(message.startsWith("!topic ")) {
			handled = true;
			m_topics.handleAdd(id, name, message.substring(7));

	    } else if(message.startsWith("!question ")) {
			handled = true;
			m_questions.handleAdd(id, name, message.substring(10));
			
	    } else if(message.equals("!how")) {
    		handled = true;
    		m_botAction.sendSmartPrivateMessage(name, "TW Radio is managed by a Radio Server. If you want to be a DJ fill out an application at ...");
			

	    } else if(message.startsWith("!login ")) {
			handled = true;
        	if(m_currentPassword.equals("")) {
        		m_botAction.sendPrivateMessage(id, "Login currently disabled.");
        	} else if(m_currentPassword.equals(message.substring(7))) {
                if(!m_loggedInList.contains(m_botAction.getPlayerName(id))) {
                    m_loggedInList.add(name);
                    m_botAction.sendChatMessage(name +" has logged in successfully.");
                    m_botAction.sendSmartPrivateMessage(name,"Login Successfull.");
                } else {
                	m_botAction.sendPrivateMessage(id, "You are already logged in.");
                }
            } else {
                m_botAction.sendPrivateMessage(id, "Incorrect password.");
                m_botAction.sendChatMessage(name + " has attempted to login and failed. Is he/she a host?.");
            }
        }
        return handled;
    }


	/**
	 * Handle Mod commands
	 */
	private boolean handleModMessage(String name, int id, String message) {

		boolean handled = false;

    	if(message.startsWith("!setpw ")) {
    		handled = true;
    		m_currentPassword = message.substring(7);
    		m_botAction.sendPrivateMessage(id, "Password changed.");

		} else if(message.startsWith("!go ")) {
    		handled = true;
    		String arena = message.substring(4);
    		if(Tools.isAllDigits(arena)) {
    			m_botAction.sendPrivateMessage(id, "You cannot move me to a public arena.");
    		} else {
				m_botAction.changeArena(arena);
    		}

		} else if(message.equals("!die")) {
    		handled = true;
    		m_botAction.sendChatMessage( "Dying....");
			m_botAction.die();

		} else if(message.startsWith("!grantzone")) {
    		handled = true;
    		long now = System.currentTimeMillis();
			if(m_alreadyZoned.contains(m_currentHost)) {
				if(now < m_timeOfLastZone + THIRTY_MINUTES) {
					m_botAction.sendPrivateMessage(id, "Sorry, you may only grantzone 30 minutes from the last zone which was "
						+ ((now - m_timeOfLastZone) / 1000 / 60) + " minutes ago.");
				} else {
					m_alreadyZoned.remove(m_currentHost);
					m_botAction.sendPrivateMessage(id, "Zoner granted to " + m_currentHost);
					m_botAction.sendPrivateMessage(m_currentHost, name + " has granted you another zone message.");
				}
			} else {
				m_botAction.sendPrivateMessage(id, "No current host or host is already granted a zone.");
			}

		

		} else if(message.startsWith("!unhost")) {
			handled = true;
			if(m_someoneHosting) {
				handleCurrentHostOnly(name, id, "!unhost", 0);
			} else {
				m_botAction.sendPrivateMessage(id, NO_HOST);
			}
			
		} else if(message.startsWith("!setwelcome ")) {
			handled = true;
			m_welcome = message.substring(12);
			m_botAction.sendPrivateMessage(id, "Welcome message set.");

       } else if(message.startsWith("!welcomeoff")) {
			handled = true;
			m_welcome = "";
			m_botAction.sendPrivateMessage(id, "Welcome message turned off.");

		} else if(message.startsWith("!seturl ")) {
			handled = true;
			m_url = message.substring(8);
			m_botAction.sendPrivateMessage(id, "URL set to " + m_url);
			
		} else if(message.startsWith("!dbcon")) {
		    handled = true;
		    if( !m_botAction.SQLisOperational() ){
	            m_botAction.sendChatMessage( "WARNING: The Radio Database is offline, I can't connect!" );
	            
	        }
	        try {
	            m_botAction.SQLQueryAndClose( mySQLHost, "SELECT * FROM tblHost LIMIT 0,1" );
	            m_botAction.sendChatMessage( "The Database is online. I can connect to it!" );
	        } catch (Exception e ) {
	            m_botAction.sendChatMessage( "WARNING: The Radio Database is offline, I can't connect!" );
	        }
	    }
			
		
        return handled;
        
       
        
		} {
    	

	}


    private class Poll {

        private String[] poll;
        private int range;
        private HashMap<String,Integer> votes;

        public Poll(String[] poll) {
            this.poll = poll;
            votes = new HashMap<String,Integer>();
            range = poll.length - 1;
            m_botAction.sendArenaMessage("Poll: " + poll[0]);
            for(int i = 1; i < poll.length; i++) {
                m_botAction.sendArenaMessage(i + ": " + poll[i]);
            }
            m_botAction.sendArenaMessage("Private message your answers to " + m_botAction.getBotName());
        }

        public void handlePollCount(String name, String message) {
            try{
                if(!Tools.isAllDigits(message)) {
                    return;
                }
                int vote;
                try {
                    vote = Integer.parseInt(message);
                } catch(NumberFormatException nfe) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid vote. "
                    + "Your vote must be a number corresponding to the choices "
                    + "in the poll.");
                    return;
                }

                if(!(vote > 0 && vote <= range)) {
                    return;
                }

				if(votes.containsKey(name))
	                m_botAction.sendSmartPrivateMessage(name, "Your vote has been changed.");
				else
	                m_botAction.sendSmartPrivateMessage(name, "Your vote has been counted.");

                votes.put(name, new Integer(vote));
            } catch(Exception e) {
                m_botAction.sendArenaMessage(e.getMessage());
                m_botAction.sendArenaMessage(e.getClass().getName());
            }
        }

        public void endPoll() {
            m_botAction.sendArenaMessage("The poll has ended! Question: " + poll[0]);

            int[] counters = new int[range+1];
            Iterator<Integer> iterator = votes.values().iterator();
            while(iterator.hasNext()) {
                counters[iterator.next().intValue()]++;
            }
            for(int i = 1; i < counters.length; i++) {
                m_botAction.sendArenaMessage(i + ". " + poll[i] + ": "
                + counters[i]);
            }
        }

    }

    private class AnnounceTask extends TimerTask {
        public void run() {
            m_botAction.sendArenaMessage("Current Host: " + m_currentHost
            	+ (m_url.equals("") ? "" : "  (To listen, open " + m_url + " in your media player)"));
            for(int i = 0; i < m_announceLength; i++) {
                m_botAction.sendArenaMessage(m_announcement[i]);
            }
            m_botAction.sendSmartPrivateMessage(m_currentHost,
            	"Shoutouts:" + m_shoutouts.size()
            	+ " Requests:" + m_requests.size()
            	+ " Topics:" + m_topics.size()
            	+ " Questions:" + m_questions.size());
        }
    }


    private class RadioQueue {
    	private LinkedList<String> m_queue;
    	private HashMap<String,String> m_map;
    	private int m_maxItems;
    	private String m_label;

    	public RadioQueue(String label) {
    		m_queue = new LinkedList<String>();
    		m_map = new HashMap<String,String>();
    		m_maxItems = 20;
    		m_label = label;
    	}

		public void handleAdd(int id, String name, String msg) {
			if(m_maxItems <= 0) {
				m_botAction.sendPrivateMessage(id, m_label + " requests have been disabled by the host.");
			} else if(m_map.containsKey(name)) {
				m_map.put(name, msg);
				m_botAction.sendPrivateMessage(id, "Your " + m_label + " request was updated.");
			} else if(m_queue.size() >= m_maxItems) {
				m_botAction.sendPrivateMessage(id, "Sorry, " + m_label + " request box is full. Try later.");
			} else {
				m_queue.add(name);
				m_map.put(name, msg);
				m_botAction.sendPrivateMessage(id, "Your " + m_label + " request was added.");
			}
		}

    	public String remove() {
    		if(m_queue.size() == 0)
    			return "<empty>";

    		String name = m_queue.remove();
    		return name + "> " + m_map.remove(name);
    	}

    	public void setMax(int max) {
    		m_maxItems = max;
    	}

    	public int getMax() {
    		return m_maxItems;
    	}

    	public String peek() {
    		if(m_queue.size() == 0)
    			return "<empty>";

    		String name = m_queue.peek();
    		return name + "> " + m_map.get(name);
    	}

    	public void clear() {
    		m_queue.clear();
    		m_map.clear();
    	}

    	public int size() {
    		return m_queue.size();
    	}
    }


    public void handleDisconnect(){
    }


    private final static String[] staffHelp = {
        "+-------------------------Radio Staff Help----------------------------------------+",
        "|!who                  - Shows who is currently logged into the bot.              |",
        "|!host                 - Enables hosting commands. Announces you are current host.|",
        "|!status               - Shows bot status.                                        |",
        "|!sex                  - Lets amuse the hosts in chat! - H.M.S.                   |",
        "+---------------------------------------------------------------------------------+"   
    };

    private final static String[] currentRadioHostHelp = {
        "+---------------------Current Radio Host Only-------------------------------------+",
        "|!arena <message>      - Sends an arena message.                                  |",
        "|!zone <message>       - Limited to once a day.                                   |",
        "|!poll <Topic>:<answer1>:<answer2> (and on and on) - Starts a poll.               |",
        "|!endpoll              - Ends a poll and tallies the results.                     |",
        "|!addannounce <msg>    - Adds a line to the announcement. 3 lines max.            |",
        "|!clrannounce          - Clears the announcement.                                 |",
        "|!viewannounce         - View the current announcement.                           |",
        "|!unhost               - Do this when you're done to allow someone else to host.  |",
        "|!setwelcome <msg>     - Sets welcome message (!welcomeoff to disable).           |",
        "|!seturl               - Sets the URL that appears in your announcement.          |",
        "|!ask                  - Sends a ?help asking for another zoner.                  |",
        "+---------------------------------------------------------------------------------+",
        "|    Read requests (append a number to retrieve several at once)                  |",
        "+---------------------------------------------------------------------------------|",
        "|!nexttop !nextreq !nextques !nextshout !nextall                                  |",
        "|!clear                - Erases all pending requests.                             |",
        "|!setmax <t> <r> <q> <s> - Sets max queue size for Topics, Requests,              |",
        "| Questions, Shoutouts. (Disable=0)                                               |",
        "+---------------------------------------------------------------------------------+"   
    
    };

    private final static String[] modHelp = {
    	"+-------------------------ER Commands--------------------------------------------",
    	"|!go <arena>           - Moves bot to <arena>.                                    |",
    	"|!die                  - Disconnects bot.                                         |",
    	"|!setpw <password>     - Changes login password for this session.                 |",
    	"|!seturl <url>         - Sets the URL that appears in the announcements.          |",
    	"|!grantzone            - Grants the radio host another zoner.                     |",
    	"|!unhost               - Removes the current host.                                |",
    	"|!dbcon                - Checks for a database connection                         |",
    	"|!setwelcome           - Use this if the host has put an inappropiate welcome msg |",
    	"|                        (Also !welcomeoff)                                       |",
        "+---------------------------------------------------------------------------------+"  
    };

    private final static String[] pubHelp = {
        "+--------------------------Player Help--------------------------------------------+",
        "|!topic <topic>        - Suggest an idea/topic/poll to the radio host.            |",
        "|!request <artist - title>  - Request a song.                                     |",
        "|!question <question>  - Ask the radio host a question to be answered on air.     |",
        "|!shoutout <shoutout>  - Request a shoutout to the radio host.                    |",
        "|!how                  - Shows how you could host Radio and how its done.         |",
        "|!login <password>     - If you are a current radio host, please log into the bot.|",
        "+---------------------------------------------------------------------------------+"
    };
    

}
