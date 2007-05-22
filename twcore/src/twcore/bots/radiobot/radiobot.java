package twcore.bots.radiobot;

import java.util.*;
import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * To assist in hosting radio (while not requiring a host to have staff access).
 */
public class radiobot extends SubspaceBot {
    private EventRequester m_req;
    private LinkedList<String> m_loggedInList;
    private String m_currentPassword;
    private LinkedList<String> m_alreadyZoned;
    private String m_currentHost = "", m_comment = "";
    private Poll m_currentPoll = null;
    private boolean m_announcing = false;
    private long m_timeStarted = System.currentTimeMillis();
    private boolean m_someoneHosting = false;

    private RadioQueue m_shoutouts;
    private RadioQueue m_requests;
    private RadioQueue m_topics;
    private RadioQueue m_questions;
    private int m_maxShoutouts = 20;
    private int m_maxRequests = 20;
    private int m_maxTopics = 20;
    private int m_maxQuestions = 20;

    final static int MAX_ANNOUNCE_LINES = 3;

    private String[] m_announcement = {
        "Trenchwars Radio! Trenchwars Radio! Trenchwars Radio!"
    };

    /** Creates a new instance of radiobot */
    public radiobot(BotAction botAction) {
        super(botAction);
        m_req = botAction.getEventRequester();
        m_req.request(EventRequester.LOGGED_ON);
        m_req.request(EventRequester.MESSAGE);
        m_loggedInList = new LinkedList<String>();
        m_alreadyZoned = new LinkedList<String>();
        m_currentPassword = m_botAction.getBotSettings().getString("ServPass");

        m_shoutouts = new RadioQueue();
        m_requests = new RadioQueue();
        m_topics = new RadioQueue();
        m_questions = new RadioQueue();
    }

    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena("radio");
        m_botAction.setMessageLimit(10);
    }

    public void handleEvent(Message event) {
        if(event.getMessageType() != Message.PRIVATE_MESSAGE)
        	return;

        int id = event.getPlayerID();
        String name = m_botAction.getPlayerName(id);
        if(name == null)
        	return;
		String message = event.getMessage();

		if(message.startsWith("!arena ") && name.equals(m_currentHost)) {
			m_botAction.sendArenaMessage(message.substring(7) + " -" + m_currentHost
				, event.getSoundCode() == 12 ? 2 : event.getSoundCode());
			return;
		}

		if((message.startsWith("!shoutout ")
				|| message.startsWith("!request ")
				|| message.startsWith("!topic ")
				|| message.startsWith("!question "))
				&& !m_someoneHosting) {
			m_botAction.sendPrivateMessage(id, "A host is not currently using RadioBot.");
			return;
		}

	    if(message.startsWith("!shoutout ")) {
	    	if(m_shoutouts.size() >= m_maxShoutouts) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, shoutouts box is full. Try again later.");
	    	} else {
		    	if(m_shoutouts.add(name, message.substring(10)))
		    		m_botAction.sendPrivateMessage(id, "Updated shoutout request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added shoutout request.");
	    	}
	    } else if(message.startsWith("!request ")) {
	    	if(m_requests.size() >= m_maxRequests) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, requests box is full. Try again later.");
	    	} else {
		    	if(m_requests.add(name, message.substring(9)))
		    		m_botAction.sendPrivateMessage(id, "Updated song request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added song request.");
	    	}
	    } else if(message.startsWith("!topic ")) {
	    	if(m_topics.size() >= m_maxTopics) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, topics box is full. Try again later.");
	    	} else {
		    	if(m_topics.add(name, message.substring(7)))
		    		m_botAction.sendPrivateMessage(id, "Updated topic request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added topic request.");
	    	}
	    } else if(message.startsWith("!question ")) {
	    	if(m_questions.size() >= m_maxQuestions) {
	    		m_botAction.sendPrivateMessage(id, "Sorry, questions box is full. Try again later.");
	    	} else {
		    	if(m_questions.add(name, message.substring(10)))
		    		m_botAction.sendPrivateMessage(id, "Updated question request.");
		    	else
		    		m_botAction.sendPrivateMessage(id, "Added question request.");
	    	}
	    }

        if(m_loggedInList.contains(name)) {
            handleStaffMessage(name, event.getPlayerID(), message);
        }

        if(name.equals(m_currentHost)) {
        	handleCurrentHostOnly(name, id, message);
        }

        handlePrivateMessage(name, event.getPlayerID(), message);

        if(m_botAction.getOperatorList().isER(name)) {
        	if(message.startsWith("!setpw ")) {
	    		m_currentPassword = message.substring(7);
	    		m_botAction.sendPrivateMessage(id, "Password changed.");
			} else if(message.startsWith("!go ")) {
				m_botAction.changeArena(message.substring(4));
			} else if(message.equals("!die")) {
				m_botAction.die();
			} else if(message.startsWith("!help")) {
	           	m_botAction.privateMessageSpam(id, erHelp);
			}
        }

        if(m_currentPoll != null){
            m_currentPoll.handlePollCount(name, event.getMessage());
        }
    }


    public void handleStaffMessage( String name, int id, String message ){
        long time = System.currentTimeMillis();
        if(time >= 21600000 + m_timeStarted){
            m_alreadyZoned.clear();
            m_timeStarted = time;
        }
        if( message.startsWith( "!help" )){
            m_botAction.privateMessageSpam( id, staffHelp );
        } else if( message.equals( "!host" )){
            if(!m_someoneHosting){
                m_currentHost = name;
                m_comment = "^_^";
                m_botAction.sendArenaMessage( "Current Host: " + name + " (" + m_comment + ")" );

                if(!m_announcing) {
                    m_botAction.scheduleTaskAtFixedRate(
                    	new AnnounceTask(), 30000, 150000);
                    m_announcing = true;
                }
                m_someoneHosting = true;
            } else {
                m_botAction.sendPrivateMessage( id, "Sorry, you must !unhost before you !host." );
            }
        } else if(message.startsWith("!host ")) {
            if(!m_someoneHosting){
                m_comment = message.substring(6);
                m_currentHost = name;
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
            if(m_someoneHosting) {
                m_botAction.cancelTasks();
                m_announcing = false;
                m_botAction.sendArenaMessage(m_currentHost + " has signed off the radio, thank you for listening!");
                m_currentHost = "";
                m_comment = "";
                m_someoneHosting = false;
            } else {
                m_botAction.sendPrivateMessage(id, "There is no current host.");
            }
        } else if(message.startsWith("!who")) {
            m_botAction.sendPrivateMessage(id, "Radio hosts who are logged in:");
            Iterator i = m_loggedInList.iterator();
            while(i.hasNext()) {
                m_botAction.sendPrivateMessage(id, (String)i.next());
            }
        } else if(!m_currentHost.equals(name)
        		&& (message.startsWith("!poll ")
        		|| message.startsWith("!arena ")
		        || message.startsWith("!zone ")
		        || message.startsWith("!endpoll"))) {
            m_botAction.sendPrivateMessage(id, "Sorry, only the current radio host may use that command."
            	+ " If you are hosting, use the !host command.");
        }
    }


    public void handleCurrentHostOnly(String name, int id, String message) {

        if(message.startsWith("!poll ")) {
            if(m_currentPoll != null) {
                m_botAction.sendPrivateMessage(name, "A poll is currently in session."
                	+ " End this poll before beginning another one.");
                return;
            }
            StringTokenizer izer = new StringTokenizer(message.substring(6), ":");
            int tokens = izer.countTokens();
            if(tokens < 2) {
                m_botAction.sendPrivateMessage(id, "Sorry but the poll format is wrong.");
                return;
            }

            String[] polls = new String[tokens];
            int i = 0;
            while( izer.hasMoreTokens() ){
                polls[i] = izer.nextToken();
                i++;
            }

            m_currentPoll = new Poll(polls);
        } else if(message.startsWith("!endpoll")) {
            if(m_currentPoll == null) {
                m_botAction.sendPrivateMessage(id, "There is no poll running right now.");
            } else {
                m_currentPoll.endPoll();
                m_currentPoll = null;
            }

        } else if( message.startsWith( "!zone " )){
            if(m_alreadyZoned.contains(name)) {
                m_botAction.sendPrivateMessage(id, "Sorry, you used your zone message today.");
            } else {
                m_botAction.sendZoneMessage(message.substring(6) + " -" + name, 2);
                m_alreadyZoned.add(name);
            }

        } else if(message.startsWith("!nextshout")) {
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
       		m_botAction.sendPrivateMessage(id, "Shout: " + m_shoutouts.remove());
       		m_botAction.sendPrivateMessage(id, "Reqst: " + m_requests.remove());
    		m_botAction.sendPrivateMessage(id, "Topic: " + m_topics.remove());
    		m_botAction.sendPrivateMessage(id, "Quest: " + m_questions.remove());

        } else if(message.startsWith("!clear")) {
        	m_shoutouts.clear();
        	m_requests.clear();
        	m_topics.clear();
        	m_questions.clear();
        	m_botAction.sendPrivateMessage(id, "All requests cleared.");

        } else if(message.startsWith("!addannounce ")) {
        	if(m_announcement.length >= MAX_ANNOUNCE_LINES) {
        		m_botAction.sendPrivateMessage(id, "Max announce lines reached.");
        	} else {
	        	String[] temp = new String[m_announcement.length + 1];
	        	System.arraycopy(m_announcement, 0, temp, 0, m_announcement.length);
	        	temp[m_announcement.length] = message.substring(13);
	        	m_announcement = temp;
	        	m_botAction.sendPrivateMessage(id, "Message added.");
        	}

        } else if(message.startsWith("!clrannounce")) {
			m_announcement = new String[0];
			m_botAction.sendPrivateMessage(id, "Announcement cleared.");

        } else if(message.startsWith("!setmax ")) {
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

        } else if(message.startsWith("!help")) {
        	m_botAction.privateMessageSpam(id, currentRadioHostHelp);
        } else if(message.startsWith("!viewannounce")) {
        	m_botAction.privateMessageSpam(id, m_announcement);
        }
    }


    public void handlePrivateMessage(String name, int id, String message) {
        if(message.startsWith("!help")) {
            m_botAction.privateMessageSpam(id, pubHelp);
        } else if( message.startsWith("!login ")) {
        	if(m_currentPassword.equals("")) {
        		m_botAction.sendPrivateMessage(id, "Login currently disabled.");
        		return;
        	}
            if(m_currentPassword.equals(message.substring(7))) {
                if( !m_loggedInList.contains(m_botAction.getPlayerName(id))) {
                    m_loggedInList.add(name);
                    m_botAction.sendPrivateMessage(id, "Login Successful");
                }
            } else {
                m_botAction.sendPrivateMessage(id, "Incorrect password");
            }
        }
    }

    public class Poll {

        private String[] poll;
        private int range;
        private HashMap<String,Integer> votes;

        public Poll( String[] poll ){
            this.poll = poll;
            votes = new HashMap<String,Integer>();
            range = poll.length - 1;
            m_botAction.sendArenaMessage("Poll: " + poll[0]);
            for(int i = 1; i < poll.length; i++) {
                m_botAction.sendArenaMessage(i + ": " + poll[i]);
            }
            m_botAction.sendArenaMessage("Private message your answers to RadioBot");
        }

        public void handlePollCount(String name, String message) {
            try{
                if(!Tools.isAllDigits(message)) {
                    return;
                }
                int vote;
                try {
                    vote = Integer.parseInt( message );
                } catch( NumberFormatException nfe ){
                    m_botAction.sendSmartPrivateMessage( name, "Invalid vote. "
                    + "Your vote must be a number corresponding to the choices "
                    + "in the poll." );
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
            } catch( Exception e ){
                m_botAction.sendArenaMessage( e.getMessage() );
                m_botAction.sendArenaMessage( e.getClass().getName() );
            }
        }

        public void endPoll(){
            m_botAction.sendArenaMessage( "The poll has ended! Question: "
            	+ poll[0] );

            int[] counters = new int[range+1];
            Iterator iterator = votes.values().iterator();
            while( iterator.hasNext() ){
                counters[((Integer)iterator.next()).intValue()]++;
            }
            for( int i = 1; i < counters.length; i++ ){
                m_botAction.sendArenaMessage( i + ". " + poll[i] + ": "
                + counters[i] );
            }
        }

    }

    private class AnnounceTask extends TimerTask {
        public void run() {
            m_botAction.sendArenaMessage("Current Host: " + m_currentHost + " (" + m_comment + ")");
            for(int i = 0; i < m_announcement.length; i++) {
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

    static String[] staffHelp = {
        " --Radio Staff Help",
        "!who                  - Shows who is currently logged into the bot",
        "!host <comment>       - Announces that you are hosting, allows you to use Current Radio Host commands",
        "!unhost               - Logs you out of the radio"
    };

    static String[] currentRadioHostHelp = {
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

    static String[] erHelp = {
    	" --Mod Commands",
    	"!go <arena>           - Moves bot to <arena>.",
    	"!die                  - Disconnects bot.",
    	"!setpw <password>     - Changes login password for this session."
    };

    static String[] pubHelp = {
        " --Help",
        "!topic <topic>        - Suggest an idea/topic/poll to the radio host.",
        "!request <artist - title>  - Request a song.",
        "!question <question>  - Ask the radio host a question to be answered on air.",
        "!shoutout <shoutout>  - Request a shoutout to the radio host.",
        " --",
        "!login <password>     - If you are a current radio host, please log into the bot."
    };

}

