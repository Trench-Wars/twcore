package twcore.bots.radiobot;

import java.util.*;
import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;

/**
 * To assist in hosting radio (while not requiring a host to have staff access).
 * based on relaybot
 */
public final class radiobot extends SubspaceBot {
    private EventRequester m_req;
    private OperatorList m_opList;
    private LinkedList<String> m_loggedInList;
    private String m_currentPassword = "";
    private LinkedList<String> m_alreadyZoned;
    private String m_currentHost = "", m_comment = "";
    private Poll m_currentPoll = null;
    private boolean m_announcing = false;
    private boolean m_someoneHosting = false;
    private long m_timeStarted;
    private long m_timeToZone;

    private RadioQueue m_shoutouts;
    private RadioQueue m_requests;
    private RadioQueue m_topics;
    private RadioQueue m_questions;
    private int m_maxShoutouts = 20;
    private int m_maxRequests = 20;
    private int m_maxTopics = 20;
    private int m_maxQuestions = 20;

	private final static String NO_HOST = "No one is currently hosting.";
    private final static int MAX_ANNOUNCE_LINES = 3;
    private final static long TWO_HOURS = 7200000;

    private String[] m_announcement;
    private int m_announceLength;
    private String m_welcome;

    /** Creates a new instance of radiobot */
    public radiobot(BotAction botAction) {
        super(botAction);
        m_req = botAction.getEventRequester();
        m_req.request(EventRequester.LOGGED_ON);
        m_req.request(EventRequester.MESSAGE);
        m_req.request(EventRequester.PLAYER_ENTERED);
        m_opList = botAction.getOperatorList();
        m_loggedInList = new LinkedList<String>();
        m_alreadyZoned = new LinkedList<String>();
        m_currentPassword = m_botAction.getBotSettings().getString("ServPass");
        if(m_currentPassword == null)
        	m_currentPassword = "";

        m_shoutouts = new RadioQueue();
        m_requests = new RadioQueue();
        m_topics = new RadioQueue();
        m_questions = new RadioQueue();

        m_announcement = new String[MAX_ANNOUNCE_LINES];
        m_announcement[0] = "Trenchwars Radio! Trenchwars Radio! Trenchwars Radio!";
        m_announcement[1] = m_announcement[2] = "";
        m_announceLength = 1;
        m_welcome = "";

        m_timeStarted = System.currentTimeMillis();
        m_timeToZone = m_timeStarted + TWO_HOURS;
    }


    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena("radio");
        m_botAction.setMessageLimit(10);
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
		boolean isCurrentHost = isLoggedIn && m_currentHost.equals(name);
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
			if(isLoggedIn) {
            	m_botAction.privateMessageSpam(id, staffHelp);
	            if(isCurrentHost) {
    	    		m_botAction.privateMessageSpam(id, currentRadioHostHelp);
	            }
			}
            m_botAction.privateMessageSpam(id, pubHelp);
			if(isER) {
				m_botAction.privateMessageSpam(id, erHelp);
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
		 * Handle ER+ commands
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
				if(message.startsWith("!host ")) {
					m_comment = message.substring(6);
				} else {
	                m_comment = "^_^";
				}
                m_botAction.sendArenaMessage("Current Host: " + name + " (" + m_comment + ")");

                if(!m_announcing) {
                    m_botAction.scheduleTaskAtFixedRate(
                    	new AnnounceTask(), 30000, 150000);
                    m_announcing = true;
                }
                m_someoneHosting = true;
            } else {
                m_botAction.sendPrivateMessage(id, "Sorry, you must !unhost before you !host.");
            }

        } else if(message.startsWith("!unhost")) {
        	handled = true;
            if(m_someoneHosting) {
                m_botAction.cancelTasks();
                m_announcing = false;
                m_botAction.sendArenaMessage(m_currentHost + " has signed off the radio, thank you for listening!");
                m_currentHost = "";
                m_comment = "";
                m_someoneHosting = false;
            } else {
                m_botAction.sendPrivateMessage(id, NO_HOST);
            }

        } else if(message.startsWith("!who")) {
        	handled = true;
            m_botAction.sendPrivateMessage(id, "Radio hosts who are logged in:");
            Iterator i = m_loggedInList.iterator();
            String who;
            while(i.hasNext()) {
            	who = (String)i.next();
                m_botAction.sendPrivateMessage(id, who + (who.equals(m_currentHost) ? " (current host)" : ""));
            }

        } else if(message.startsWith("!status")) {
        	handled = true;
        	if(!m_someoneHosting) {
        		m_botAction.sendPrivateMessage(id, NO_HOST);
        	} else {
        		m_botAction.sendPrivateMessage(id, "Current host: " + m_currentHost + " - Can zone: " + !m_alreadyZoned.contains(m_currentHost));
        	}
            m_botAction.sendPrivateMessage(id,
            	"Shoutouts:" + m_shoutouts.size()
            	+ " Requests:" + m_requests.size()
            	+ " Topics:" + m_topics.size()
            	+ " Questions:" + m_questions.size());
            m_botAction.sendPrivateMessage(id, "Poll running: " + (m_currentPoll != null));

            long ontime = System.currentTimeMillis() - m_timeStarted;
            m_botAction.sendPrivateMessage(id, "Online for " + (ontime / 1000 / 60 / 60) + " hours and " + (ontime / 1000 / 60 % 60) + " minutes.");

        } else if(!m_currentHost.equals(name)
        		&& (message.startsWith("!poll ")
        		|| message.startsWith("!arena ")
		        || message.startsWith("!zone ")
		        || message.startsWith("!endpoll"))) {
        	handled = true;
            m_botAction.sendPrivateMessage(id, "Sorry, only the current radio host may use that command."
            	+ " If you are hosting, use the !host command.");
        }

        return handled;
    }


	/**
	 * Handle current host only commands
	 */
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
   	        if(System.currentTimeMillis() >= m_timeToZone) {
	            m_alreadyZoned.clear();
	            m_timeToZone += TWO_HOURS;
	        }
            if(m_alreadyZoned.contains(name)) {
                m_botAction.sendPrivateMessage(id, "Sorry, you used your zone message today.");
            } else {
                m_botAction.sendZoneMessage(message.substring(6) + " -" + name, 2);
                m_alreadyZoned.add(name);
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
        		m_botAction.sendPrivateMessage(id, "Max announce lines reached.");
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
        	int i;
        	try {
        		i = Integer.parseInt(message.substring(8));
        		if(i < 0)
        			i = 0;
        		else if(i > 100)
        			i = 100;
        	} catch(NumberFormatException e) {
        		i = 20;
        	}
        	m_maxQuestions = i;
        	m_maxRequests = i;
        	m_maxShoutouts = i;
        	m_maxTopics = i;
        	m_botAction.sendPrivateMessage(id, "Requests limit set to " + i);

        } else if(message.startsWith("!viewannounce")) {
        	handled = true;
        	for(int i = 0; i < m_announceLength; i++) {
        		m_botAction.sendPrivateMessage(id, m_announcement[i]);
        	}
        }
        return handled;
    }


	/**
	 * Handle public commands
	 */
    private boolean handlePrivateMessage(String name, int id, String message) {

    	boolean handled = false;

		if((message.startsWith("!shoutout ")
				|| message.startsWith("!request ")
				|| message.startsWith("!topic ")
				|| message.startsWith("!question "))
				&& !m_someoneHosting) {
			handled = true;
			m_botAction.sendPrivateMessage(id, NO_HOST);
		} else if(message.startsWith("!shoutout ")) {
			handled = true;
	    	if(m_shoutouts.size() >= m_maxShoutouts) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, shoutouts box is full. Try again later.");
	    	} else {
		    	if(m_shoutouts.add(name, message.substring(10)))
		    		m_botAction.sendPrivateMessage(id, "Updated shoutout request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added shoutout request.");
	    	}
	    } else if(message.startsWith("!request ")) {
			handled = true;
	    	if(m_requests.size() >= m_maxRequests) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, requests box is full. Try again later.");
	    	} else {
		    	if(m_requests.add(name, message.substring(9)))
		    		m_botAction.sendPrivateMessage(id, "Updated song request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added song request.");
	    	}
	    } else if(message.startsWith("!topic ")) {
			handled = true;
	    	if(m_topics.size() >= m_maxTopics) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, topics box is full. Try again later.");
	    	} else {
		    	if(m_topics.add(name, message.substring(7)))
		    		m_botAction.sendPrivateMessage(id, "Updated topic request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added topic request.");
	    	}
	    } else if(message.startsWith("!question ")) {
			handled = true;
	    	if(m_questions.size() >= m_maxQuestions) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, questions box is full. Try again later.");
	    	} else {
		    	if(m_questions.add(name, message.substring(10)))
		    		m_botAction.sendPrivateMessage(id, "Updated question request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added question request.");
	    	}
	    } else if(message.startsWith("!login ")) {
			handled = true;
        	if(m_currentPassword.equals("")) {
        		m_botAction.sendPrivateMessage(id, "Login currently disabled.");
        	} else if(m_currentPassword.equals(message.substring(7))) {
                if(!m_loggedInList.contains(m_botAction.getPlayerName(id))) {
                    m_loggedInList.add(name);
                    m_botAction.sendPrivateMessage(id, "Login Successful.");
                } else {
                	m_botAction.sendPrivateMessage(id, "You are already logged in.");
                }
            } else {
                m_botAction.sendPrivateMessage(id, "Incorrect password.");
            }
        }
        return handled;
    }


	/**
	 * Handle ER+ commands
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
			m_botAction.die();

		} else if(message.startsWith("!grantzone")) {
    		handled = true;
			if(m_alreadyZoned.remove(m_currentHost)) {
				m_botAction.sendPrivateMessage(id, "Zoner granted to " + m_currentHost);
				m_botAction.sendPrivateMessage(m_currentHost, name + " has granted you another zone message.");
			} else {
				m_botAction.sendPrivateMessage(id, "No current host or host is already granted a zone.");
			}
		} else if(message.startsWith("!setwelcome ")) {
			handled = true;
			m_welcome = message.substring(12);
			m_botAction.sendPrivateMessage(id, "Welcome message set.");
		} else if(message.startsWith("!welcomeoff")) {
			handled = true;
			m_welcome = "";
			m_botAction.sendPrivateMessage(id, "Welcome message turned off.");
		}
		return handled;
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
            Iterator iterator = votes.values().iterator();
            while(iterator.hasNext()) {
                counters[((Integer)iterator.next()).intValue()]++;
            }
            for(int i = 1; i < counters.length; i++) {
                m_botAction.sendArenaMessage(i + ". " + poll[i] + ": "
                + counters[i]);
            }
        }

    }

    private class AnnounceTask extends TimerTask {
        public void run() {
            m_botAction.sendArenaMessage("Current Host: " + m_currentHost + " (" + m_comment + ")");
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

    	public RadioQueue() {
    		m_queue = new LinkedList<String>();
    		m_map = new HashMap<String,String>();
    	}

    	public boolean add(String name, String msg) {
    		boolean isUpdate = m_map.containsKey(name);
    		if(!isUpdate)
    			m_queue.add(name);
			m_map.put(name, msg);

			return isUpdate;
    	}

    	public String remove() {
    		if(m_queue.size() == 0)
    			return "<empty>";

    		String name = m_queue.remove();
    		return name + "> " + m_map.remove(name);
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
        " --Radio Staff Help",
        "!who                  - Shows who is currently logged into the bot",
        "!host <comment>       - Announces that you are hosting, allows you to use Current Radio Host commands",
        "!unhost               - Logs you out of the radio",
        "!status               - Shows bot status."
    };

    private final static String[] currentRadioHostHelp = {
        " --Current Radio Host Only",
        "!arena <message>      - Sends an arena message.",
        "!zone <message>       - Limited to once a day.",
        "!poll <Topic>:<answer1>:<answer2> (and on and on) - Starts a poll.",
        "!endpoll              - Ends a poll and tallies the results.",
        "!addannounce <msg>    - Adds a line to the announcement. 3 lines max.",
        "!clrannounce          - Clears the announcement.",
        "!viewannounce         - View the current announcement.",
        " --Read requests (append a number to retrieve several at once)",
        "!nextshout !nextreq !nexttop !nextques !nextall",
        "!clear                - Erases all pending requests.",
        "!setmax <#>           - Sets maximum number of requests to remember (Default=20).",
        "Do not abuse the green messages, or write anything inappropriate. Thanks!"
    };

    private final static String[] erHelp = {
    	" --Mod Commands",
    	"!go <arena>           - Moves bot to <arena>.",
    	"!die                  - Disconnects bot.",
    	"!setpw <password>     - Changes login password for this session.",
    	"!grantzone            - Grants the radio host another zoner.",
    	"!setwelcome           - Sets welcome message (!welcomeoff to disable)."
    };

    private final static String[] pubHelp = {
        " --Help",
        "!topic <topic>        - Suggest an idea/topic/poll to the radio host.",
        "!request <artist - title>  - Request a song.",
        "!question <question>  - Ask the radio host a question to be answered on air.",
        "!shoutout <shoutout>  - Request a shoutout to the radio host.",
        " --",
        "!login <password>     - If you are a current radio host, please log into the bot."
    };

}
