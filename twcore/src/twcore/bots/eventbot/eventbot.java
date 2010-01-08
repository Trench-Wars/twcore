package twcore.bots.eventbot;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * @author Maverick
 * 
 * TODO:
 *  - Track bans using database
 *  - when lifting bans, don't remove the ban
 *  - 1:Tres <ER>> You could set it to go to ?go events after it spawns, and pm anyone who enters with instructions about requesting events
 */
public class eventbot extends SubspaceBot {
  
	private BotSettings m_botSettings;
    private BotAction m_botAction;
    private OperatorList m_opList;
    private CommandInterpreter commandInterpreter;
    
    // Each 30 seconds a player of the oldest checked request is checked if he's still online  
    private final int PLAYER_CHECK_TIME = Tools.TimeInMillis.SECOND*30;
    
    private String checkingPlayer;
    
    private final String SUBSCRIBE_ALL = "all";
    private final String SUBSCRIBE_BWJS = "bwjs";
    
    //This vector stores all the requests
    private Map<String,EventRequest> requests = Collections.synchronizedMap(new HashMap<String,EventRequest>(64));
    			// <playername (lowercase), EventRequest>
    
    // Contains the names of all the players not allowed to use !request - stored in the eventbot.cfg file
    private Set<BannedPlayer> bannedPlayers = Collections.synchronizedSet(new HashSet<BannedPlayer>());
    
    // Contains the names of staff member subscribed to event requests
    private Map<String, String> subscribers = Collections.synchronizedMap(new HashMap<String, String>(16));
    //              <playername, "all">
    //              <playername, "bwjs">
    
    // SimpleDateFormat to format dates on !listban
    // 2008-01-23 21:02
    // yyyy-MM-dd HH:mm
    private SimpleDateFormat banDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    //It'll pm the player added on this list ... about new events starting
    private LinkedList<String> pmPlayersNewEvent = new LinkedList<String>(); //new
    
    public eventbot(BotAction botAction) {
        super(botAction);
        m_botAction = BotAction.getBotAction();
        m_opList = m_botAction.getOperatorList();
        m_botSettings = m_botAction.getBotSettings();
        int acceptedMessageTypes;
        
        loadBannedPlayers();
      
        // Set which events to handle
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.LOGGED_ON);
        
        acceptedMessageTypes = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        
        // handle the PM commands
        commandInterpreter = new CommandInterpreter(m_botAction);
        
        // Anyone
        commandInterpreter.registerCommand("!help", acceptedMessageTypes, this, "cmdHelp");
        // Public only (not enforced by CommandInterpreter)
        commandInterpreter.registerCommand("!request", acceptedMessageTypes, this, "cmdRequest");
        commandInterpreter.registerCommand("!subscribe", acceptedMessageTypes, this, "cmdSubscribe");
        
       
  
        // ER+
        int  level = OperatorList.ER_LEVEL; 
        
        commandInterpreter.registerCommand("!requests", acceptedMessageTypes, this, "cmdRequests", level);
        commandInterpreter.registerCommand("!reqs", acceptedMessageTypes, this, "cmdRequests", level);
        commandInterpreter.registerCommand("!fillrequest", acceptedMessageTypes, this, "cmdFillRequest", level);
        commandInterpreter.registerCommand("!fr", acceptedMessageTypes, this, "cmdFillRequest", level);
        commandInterpreter.registerCommand("!removerequest", acceptedMessageTypes, this, "cmdRemoveRequest", level);
        commandInterpreter.registerCommand("!rr", acceptedMessageTypes, this, "cmdRemoveRequest", level);
        commandInterpreter.registerCommand("!ban", acceptedMessageTypes, this, "cmdBan", level);
        commandInterpreter.registerCommand("!listban", acceptedMessageTypes, this, "cmdListban", level);
        
        // MOD+
        level = OperatorList.MODERATOR_LEVEL;
        commandInterpreter.registerCommand("!liftban", acceptedMessageTypes, this, "cmdLiftban", level);
        
        // SMOD+
        level = OperatorList.SMOD_LEVEL;
        commandInterpreter.registerCommand("!die", acceptedMessageTypes, this, "cmdDie", level);
    }


    public void handleEvent(Message event) {
    	commandInterpreter.handleEvent(event);
    		
    	if(event.getMessageType() == Message.ARENA_MESSAGE && event.getMessage().startsWith(checkingPlayer+" - ")) {
    		checkingPlayer = null;
    	}
    	
    	if(event.getMessageType() == Message.ARENA_MESSAGE && event.getMessage().toLowerCase().contains("?go"))
    		pmAdvert(event.getMessage());
    
    }
    
  
    //it'll add the person who wants to receive pms about new events going on
    public void subscribePmPlayer(String name){
    
    	if(!pmPlayersNewEvent.contains(name)){ //if the name isnt on list, it'll add him
    		pmPlayersNewEvent.add(name);
    		m_botAction.sendSmartPrivateMessage(name, "Allright! Any new event I'll pm you!");
    	}
    	
    	else{ //otherwise he doesnt want to receive the pms anymore and it'll remove him from the linkedlist
    		pmPlayersNewEvent.remove(name);
    		m_botAction.sendSmartPrivateMessage(name, "Ok, I won't disturb you anymore!");
    	}
    
    }
    
  
    //this method is to pm the person on the linkedlist about new events starting
  	public void pmAdvert(String message){
  
  		StringTokenizer st = new StringTokenizer(message);
  		String arenaname = "";
  		
  		while(st.hasMoreTokens())
  			if(st.nextToken().equalsIgnoreCase("?go")) arenaname = st.nextToken();	
  		
  	for(String nameOn: this.pmPlayersNewEvent) 
  			m_botAction.sendSmartPrivateMessage(nameOn, "A new event is starting in ?go "+arenaname.toLowerCase()+ "  ... come play!");
  	
  	}
   
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Staff Chat" ));
        
        // Start TimerTask to check players if they are offline and remove their request
        TimerTask checkAndRemoveRequests = new TimerTask() {
			public void run() {
				checkAndRemoveRequests();
			}
		};
        m_botAction.scheduleTaskAtFixedRate(checkAndRemoveRequests, PLAYER_CHECK_TIME, PLAYER_CHECK_TIME);
    }
    
    /*****************************************************************************
     *                          COMMAND HANDLERS
     *****************************************************************************/
    public void cmdHelp(String name, String message) {
    	// Public commands
    	String[] startCommands = 
    	{   "+-------------------------------------------------------------------------------+",
    		"|                                 EVENTBOT                                      |"  };
    	String[] publicCommands = 
    	{	"|                                                                               |",
    		"| !request <event>:[comments] - Request an event to be hosted. Optional; any    |",
    		"|                               [comments]. Once you have made a !request, you  |",
    		"|                               can change it using !request again.             |",
    		"| !request -                  - Cancels your current request.                   |",
    	  "| !subscribe -								 - Toggles alerts in private messages							 |",
    	};
    
    	String[] zhCommands = 
    	{   "|------------------------------     ZH+    -------------------------------------|",
    		  "| !subscribe 																 - Toggles alerts in private messages    	         |",
    			"| !subscribe all              - EventBot will PM you on all event requests      |",
    	    "|                               except for Base, WBDuel, JavDuel and SpidDuel   |",
    	    "| !subscribe bwjs             - EventBot will only PM you on event requests for |",
    	    "|                               Base, WBDuel, JavDuel or SpidDuel               |",
    	    "| !subscribe off              - Removes any subscription to event requests      |"
    	};
    	String[] erCommands = 
        {   "|------------------------------     ER+    -------------------------------------|",
    	    "| !requests [#]  / !reqs [#]  - Display the top 10 or top [#] event requests    |",
            "| !requests [event]           - Display details of the requests for [event]     |",
            "| !fillrequest <event> / !fr  - Let all players requesting <event> know that    |",
            "|                               you are hosting the event (soon). Also removes  |",
            "|                               all requests for <event>.                       |",
            "| !removerequest <name> / !rr - Removes request of player<name>. Note this      |",
            "|                               doesn't inform the player.                      |",
  	        "| !ban <name>                 - Permanently disallows player <name> to use      |",
  	        "|                               !request. The bot doesn't notify the player that|",
  	        "|                               he's banned.                                    |",
  	        "| !listban [#]                - Lists banned players                            |"    };
    	String[] modCommands = 
    	{   "|-------------------------------    MOD+   -------------------------------------|",
    	    "| !liftban <name>             - Removes ban on <name>                           |"   };
    	String[] smodCommands = 
    	{   "|-------------------------------   SMOD+   -------------------------------------|",
    	    "| !die                        - Removes EventBot from the zone                  |"    };
    	String[] endCommands =
    	{   "\\-------------------------------------------------------------------------------/"    };
    	
    	
    	m_botAction.smartPrivateMessageSpam(name, startCommands);
    	
    	if(!m_opList.isBot(name))   // Public only, not staff
    	    m_botAction.smartPrivateMessageSpam(name, publicCommands);
    	
    	if(m_opList.isBot(name))    // ZH+
    	    m_botAction.smartPrivateMessageSpam(name, zhCommands);
    	
    	if(m_opList.isER(name))    // ER+
    		m_botAction.smartPrivateMessageSpam(name, erCommands);
    	
    	if(m_opList.isModerator(name)) // MOD+
    		m_botAction.smartPrivateMessageSpam(name, modCommands);
    	
    	if(m_opList.isSmod(name))  // SMOD+
    		m_botAction.smartPrivateMessageSpam(name, smodCommands);
    	
    	m_botAction.smartPrivateMessageSpam(name, endCommands);
    }
    
    /**
     * [PM] !request [PLAYER+]
     * 
     * @param name
     * @param message
     */
    public void cmdRequest(String name, String message) {
    	message = message.trim();
    	
    	// Validate all parameters
    	if(message.length() == 0) {
    		m_botAction.sendSmartPrivateMessage(name, "Syntax error: Please specify the <event> and any optional [comments] seperated by ':'. Type ::!help for more information.");
    		return;
    	}
    	
    	// Check if this player isn't banned
    	if(isBannedPlayer(name)) {
    		m_botAction.sendSmartPrivateMessage(name, "I'm sorry but you aren't allowed to use !request.");
    		return;
    	}
    	
    	// Staff shouldn't make !requests
    	if(m_botAction.getOperatorList().isBot(name)) {
    		m_botAction.sendSmartPrivateMessage(name, "I'm sorry but staff can't make !requests.");
    		return;
    	}
    	
    	// Player cancels his request
    	if(message.equals("-")) {
    	    synchronized(requests) {
        	    if(requests.containsKey(name.toLowerCase())) {
        	        String event = requests.get(name.toLowerCase()).getEvent();
        	        m_botAction.sendSmartPrivateMessage(name, "Your request for "+event+" has been removed.");
        	        m_botAction.sendChatMessage(name+" removed his request for '"+event+"' .");
                    requests.remove(name.toLowerCase());
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "No request was found to remove.");
                }
    	    }
    	    return;
    	}
    	
    	EventRequest eventRequest = null;
    	String event, comments = "";
    	
    	if(message.contains(":")) {		// Message includes comments 
    		event = message.substring(0,message.indexOf(":")).trim();
    		event = event.toLowerCase().replaceAll("<|>", "");
    		comments = message.substring(message.indexOf(":")+1).trim();
    		eventRequest = new EventRequest(name, event, comments);
    	} else {						// No comments available
    		event = message.toLowerCase().replaceAll("<|>", "");
    		eventRequest = new EventRequest(name, message);
    	}
    	
    	if(eventRequest != null) {
    		// Check if player's previous request was done more then 5 minutes ago to prevent abuse.
    		if(requests.containsKey(name.toLowerCase())) {
    			EventRequest eventReq = requests.get(name.toLowerCase());
    			int changeRequestTime = Tools.TimeInMillis.MINUTE*5;
    			
    			if((new Date().getTime() - eventReq.getDate().getTime()) <= changeRequestTime) {
    				// Player made the request less then 5 minutes ago, tell him he needs to wait before doing another request
    				int minutesLeft = (int)Math.ceil((double)(changeRequestTime - (new Date().getTime() - eventReq.getDate().getTime()))/1000/60);
    				m_botAction.sendSmartPrivateMessage(name, "You have to wait at least "+minutesLeft+" minute(s) before changing your previous request. Please try again later.");
    				return;
    			} else if(eventReq.getEvent().equals(event)) {
    				// if player's previous request is the same event as he's requesting now, inform him that he can't change his request to the same event
    				m_botAction.sendSmartPrivateMessage(name, "You can't change your request using the same event name. If you want to change your request, make sure you request a different event.");
    				return;
    			}
    		}
    		
    		   		
    		// If event is base/javduel/spidduel/wbduel-type then make the request directly without saving it.
    		if(	event.equals("base") || 
    				event.equals("javduel") ||
    				event.equals("spidduel") ||
    				event.equals("wbduel")) {
    			
    			// Inform staff
    			m_botAction.sendChatMessage("> Staff, please start a new game in "+event+". (Requested by "+name+")");
    			
    			// Notify subscribed staffers of request
    			this.notifySubscribed(SUBSCRIBE_BWJS, eventRequest);

    			// Notify requester about his request
    			m_botAction.sendSmartPrivateMessage(name, "Your request for '"+event+"' has been forwarded to staff. It will be dealt with as soon as possible.");
    		} else {
    			boolean newRequest = true;
    			
    			// Replace previous request
    			synchronized(requests) {
            		if(requests.containsKey(name.toLowerCase())) {
            			requests.remove(name.toLowerCase());
            			newRequest = false;
            		}
            		// Add to request list
            		requests.put(name.toLowerCase(), eventRequest);
    			}
    			
        		// Inform staff
        		if(comments.length() > 0)	comments = "("+comments+") ";
        		
        		if(newRequest)
        			m_botAction.sendChatMessage(name+" requested event '"+event+"' "+comments+"(rank: "+getEventRank(event)+")");
        		else
        			m_botAction.sendChatMessage(name+" changed request to '"+event+"' "+comments+"(rank: "+getEventRank(event)+")");
        		
        		// Notify subscribed staffers of request 
        		if(newRequest) {
        		    this.notifySubscribed(SUBSCRIBE_ALL, eventRequest);
        		}
    			
    			// Notify requester about his request
        		m_botAction.sendSmartPrivateMessage(name, "Your request for "+event+" has been saved and forwarded to staff.");
        		m_botAction.sendSmartPrivateMessage(name, "Your request will be saved for as long as you stay online. If you want to change your request please use !request again.");
    		}
    		
    	} else {
    		throw new NullPointerException("EventRequest is null in EventBot, method cmdRequest()");
    	}
    }
    
    /**
     * [PM]  !subscribe all / bwjs / off[ZH+]
     * [new] !subscribe  	-		it'll add the player to a list which will receive pm when a new event starts 
     * @param name
     * @param message
     */
    public void cmdSubscribe(String name, String message) {
    		
    		boolean level = m_opList.isER(name);
    		message = message.trim().toLowerCase();
        String staffer = name.toLowerCase();
        
      //it'll add or remove the player to be pmed if any event is on
        if(message.length() == 0) {
        	subscribePmPlayer(name);
        } 
        
        else if(level){ 
        	if(message.equals("all")) {
            if(subscribers.containsKey(staffer) && subscribers.get(staffer).indexOf(SUBSCRIBE_ALL)>-1) {
                m_botAction.sendSmartPrivateMessage(name, "You are already subscribed for all event requests.");
            } else {
                subscribers.put(staffer, SUBSCRIBE_ALL);
                m_botAction.sendSmartPrivateMessage(name, "You are now subscribed to all event requests.");
            }
        } else if(message.equals("bwjs")) {
            if(subscribers.containsKey(staffer) && subscribers.get(staffer).indexOf(SUBSCRIBE_BWJS)>-1) {
                m_botAction.sendSmartPrivateMessage(name, "You are already subscribed for Base/WbDuel/JavDuel/SpidDuel event requests.");
            } else {
                subscribers.put(staffer, SUBSCRIBE_BWJS);
                m_botAction.sendSmartPrivateMessage(name, "You are now subscribed to Base/WbDuel/JavDuel/SpidDuel event requests.");
            }
        } else if(message.equals("off")) {
            if(subscribers.containsKey(staffer)) {
                m_botAction.sendSmartPrivateMessage(name, "Removed subscriptions for event requests.");
                subscribers.remove(staffer);
            } else {
                m_botAction.sendSmartPrivateMessage(name, "You are not subscribed to any event requests.");
            }
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Command syntax error. You can only specify 'all', 'bwjs' or 'off'. Type ::!help for more information.");
        }
        
      }
    }
    /**
     * [PM] !requests [ER+]
     * 
     * @param name
     * @param message
     */
    public void cmdRequests(String name, String message) {
    	HashMap<String, Integer> rank = new HashMap<String, Integer>();
    	Vector<String> rankPosition = new Vector<String>();
    	
    	// Create the event with count mapping
    	synchronized(requests) {
        	for(EventRequest request:requests.values()) {
        		if(rank.containsKey(request.getEvent())) {
        			rank.put(request.getEvent(), rank.get(request.getEvent())+1);
        		} else {
        			rank.put(request.getEvent(), new Integer(1));
        			rankPosition.add(request.getEvent());
        		}
        	}
    	}

    	
    	// Create the rank, order the hashmap into a ranked list of events to a vector 
    	boolean recheck = true;
    	
    	while(recheck) {
    		recheck = false;
    		
	    	for(int i = 0 ; i < rankPosition.size(); i++) {
	    		if(i+1 < rankPosition.size() && rank.get(rankPosition.get(i)).intValue() < rank.get(rankPosition.get(i+1)).intValue()) {
	    			String tmp = rankPosition.get(i);
	    			rankPosition.set(i, rankPosition.get(i+1));
	    			rankPosition.set(i+1, tmp);
	    			recheck = true;
	    		}
	    	}
    	}
    	
    	message = message.trim();
    	int nr = 10;
    	if(message.length() > 0 && Tools.isAllDigits(message)) {
    		nr = Integer.parseInt(message);
    	}
    	
    	// !requests / !requests # 
    	if(Tools.isAllDigits(message) || message.length() == 0) {
	    	if(nr < 1 || nr > 100)
	    		nr = 10;
	    	
	    	if(rankPosition.size() == 0) {
	    		m_botAction.sendSmartPrivateMessage(name, "There are currently no active requests.");
	    		return;
	    	}
	    	
	    	m_botAction.sendSmartPrivateMessage(name, "Top "+nr+" event requests:");
	    	m_botAction.sendSmartPrivateMessage(name, "----------------------------------");
	    	// PM the top 10
	    	for(int i = 0 ; i < nr; i++) {
	    		if(rankPosition.size()-1 < i) continue;
	    		m_botAction.sendSmartPrivateMessage(name, " "+
						Tools.formatString((i+1)+")", 3)+
						Tools.formatString(rankPosition.get(i),20)+" "+
						rank.get(rankPosition.get(i)));
	    	}
    	} else {
    	// !requests [event]
    		String event = message;
    		
    		if(this.getEventRequests(event).size() > 0) {
    			HashSet<EventRequest> events = this.getEventRequests(event);
    			
    			m_botAction.sendSmartPrivateMessage(name, "Players requesting "+event+":");
    			m_botAction.sendSmartPrivateMessage(name, "----------------------------------");
    			
    			for(EventRequest er:events) {
    			    String requester = er.getRequester();
    			    if(requester.length() >= 12) {
    			        requester = requester.substring(0,12);
    			    }
    			    
    				if(er.getComments() != null && er.getComments().length()>0) {
    					m_botAction.sendSmartPrivateMessage(name, 
    							Tools.rightString(requester+"> ", 14)+er.getComments());
    				} else {
    					m_botAction.sendSmartPrivateMessage(name, " "+
        						Tools.rightString(requester, 14));
    				}
    			}
    		} else {
    			m_botAction.sendSmartPrivateMessage(name, "Event '"+event+"' is not found amongst the saved requests.");
    		}
    		
    	}
    }
    
    /**
     * [PM] !removerequest <name> [ER+]
     * 
     * @param name
     * @param message
     */
    public void cmdRemoveRequest(String name, String message) {
    	String requester = message.trim().toLowerCase();
    	
        synchronized(requests) {
        	if(requests.containsKey(requester)) {
        		m_botAction.sendChatMessage(name+" removed request of "+message+" ("+requests.get(requester).getEvent()+").");
        		requests.remove(requester);
        	} else {
        		m_botAction.sendSmartPrivateMessage(name, "No requests found of '"+message+"' .");
        	}
        }
    }
    
    /**
     * [PM] !fillrequest <event> [ER+]
     * 
     * @param name
     * @param message
     */
    public void cmdFillRequest(String name, String message) {
    	String event = message.trim();
    	HashSet<EventRequest> matchingRequests = this.getEventRequests(event);
    	
    	if(matchingRequests.size() > 0) {

    		// Loop over all the EventRequest, inform the player and remove the request
    		for(EventRequest er:matchingRequests) {
    			// Inform player
    			m_botAction.sendRemotePrivateMessage(er.getRequester(), er.getRequester()+", your requested event ("+er.getEvent()+") will now be hosted by "+name+".");
    			// Remove request
    			requests.remove(er.getRequester().toLowerCase());
    		}
    		m_botAction.sendChatMessage(name+" filled requests for "+event+", informing "+matchingRequests.size()+" player(s). (Matching requests removed.)");
    		
    	} else {
    		m_botAction.sendSmartPrivateMessage(name,"No matching requests for the event '"+event+"' found.");
    	}
    }
    
    /**
     * [PM] !ban <name> [ER+]
     * 
     * @param name
     * @param message
     */
    public void cmdBan(String name, String message) {
    	String playername = message.trim().toLowerCase();
    	
    	if(name.equalsIgnoreCase(playername)) {
    	    m_botAction.sendSmartPrivateMessage(name, "You can't ban yourself.");
    	} else if(m_opList.isBot(playername)) {
    	    m_botAction.sendSmartPrivateMessage(name, "You can't ban a staff member.");
    	} else if(isBannedPlayer(playername)) {
    		m_botAction.sendSmartPrivateMessage(name, "Player '"+playername+"' is already banned.");
    	} else {
    		bannedPlayers.add(new BannedPlayer(playername, name, new Date()));
    		m_botAction.sendSmartPrivateMessage(name, "Player '"+playername+"' is now permanently banned from using !request.");
    		m_botAction.sendChatMessage(name+" banned player '"+playername+"' from using !request.");
    	}
    	saveBannedPlayers();
    }
    
    
    /**
     * [PM] !listban [#] [ER+]
     * 
     * @param name
     * @param message
     */
    public void cmdListban(String name, String message) {
    	if(bannedPlayers.size() == 0) {
    		m_botAction.sendSmartPrivateMessage(name, "No banned players found.");
    	} else {
	    	int i = 1;
	    	int count = 10;
	    	int start = 0;
	    	int offset = 0;
	    	
	    	if(message.trim().length() > 0) {
	    	    try {
	    	        count = Integer.parseInt(message.trim());
	    	    } catch(NumberFormatException nfe) {
	    	        m_botAction.sendSmartPrivateMessage(name, "Wrong command syntax, only use a number for displaying bans count. Defaulting back to 10 bans.");
	    	    }
	    	}
	    	if(count < 0) {
	    	    count = 10;   
	    	}
	    	
	    	m_botAction.sendSmartPrivateMessage(name, "Last "+count+" EventBot banned players:");
	        m_botAction.sendSmartPrivateMessage(name, "--------------------------------");
	            

	    	
	    	// Sort the list by date first
	        Vector<BannedPlayer> bannedSorted = new Vector<BannedPlayer>();
	        
    	    for(BannedPlayer player:bannedPlayers) {
    	        if(!bannedSorted.contains(player)) {
    	            if(bannedSorted.size() == 0) {
    	                bannedSorted.add(player);
    	            } else {
        	            for(int c = 0 ; c < bannedSorted.size(); c++) {
        	                if(bannedSorted.get(c).date.after(player.date)) {
        	                    bannedSorted.add(c, player);
        	                    break;
        	                } else if(c+1 == bannedSorted.size()) {
        	                    bannedSorted.add(player);
        	                    break;
        	                }
        	            }
    	            }
    	        }
    	    }
	    	
	    	if(bannedSorted.size() > count) {
	    	    start = bannedSorted.size() - count;
	    	    offset = start;
	    	}
	    	
	    	for(BannedPlayer player:bannedSorted) {
	    	    if(start > 0) {
	    	        start--;
	    	        continue;
	    	    }
	    	    
	    	    String date = banDateFormat.format(player.date);
	    	    String bannedby = player.bannedby;
	    	    if(bannedby.length() > 10) {
	    	        bannedby.substring(0,11);
	    	    }
	    	    
	    		m_botAction.sendSmartPrivateMessage(name, "#"+(offset+i)+" by "+Tools.formatString(bannedby,10)+" "+date+" "+player.name);
	    		i++;
	    		
	    		if(i > count) {
	    		    break;
	    		}
	    	}
    	}
    }
    
    /**
     * [PM] !liftban <name> [MOD+]
     * 
     * @param name
     * @param message
     */
    public void cmdLiftban(String name, String message) {
        String playername = message.trim().toLowerCase();
        
        if(isBannedPlayer(playername)) {
            removeBannedPlayer(playername);
            m_botAction.sendSmartPrivateMessage(name, "Player '"+playername+"' removed from !request ban.");
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Player '"+playername+"' isn't banned.");
        }
        saveBannedPlayers();
    }
    
    /**
     * [PM] !die [SMOD+]
     * 
     * @param name
     * @param message
     */
    public void cmdDie(String name, String message) {
    	requests = null;
    	m_botAction.die("Disconnected by "+name);
    }
    
    /*****************************************************************************
     *                        END OF COMMAND HANDLERS
     *****************************************************************************/

    /**
     * This method determines the rank of the given event from the requests list.
     * @param event the event to determine the rank of
     * @return the rank of the given event, -1 if not found
     */
    private int getEventRank(String event) {
    	HashMap<String, Integer> rank = new HashMap<String, Integer>();
    	Vector<String> rankPosition = new Vector<String>();
    	
    	// Create the event with count mapping
    	synchronized(requests) {
        	for(EventRequest request:requests.values()) {
        		if(rank.containsKey(request.getEvent())) {
        			rank.put(request.getEvent(), rank.get(request.getEvent())+1);
        		} else {
        			rank.put(request.getEvent(), new Integer(1));
        			rankPosition.add(request.getEvent());
        		}
        	}
    	}

    	
    	// Create the rank, order the hashmap into a ranked list of events to a vector 
    	boolean recheck = true;
    	
    	while(recheck) {
    		recheck = false;
    		
	    	for(int i = 0 ; i < rankPosition.size(); i++) {
	    		if(i+1 < rankPosition.size() && rank.get(rankPosition.get(i)).intValue() < rank.get(rankPosition.get(i+1)).intValue()) {
	    			String tmp = rankPosition.get(i);
	    			rankPosition.set(i, rankPosition.get(i+1));
	    			rankPosition.set(i+1, tmp);
	    			recheck = true;
	    		}
	    	}
    	}
    	
    	return rankPosition.indexOf(event)+1;
    }
    
    /**
     * Removes any expired requests from the HashMap by comparing the date the request was made.
     */
    private void checkAndRemoveRequests() {
        Set<Map.Entry<String, EventRequest>> playerRequests = requests.entrySet();
        
        // Remove the request of the player that was previously checked upon (if the player was found, the variable is cleared)
        if(checkingPlayer != null) {
        	requests.remove(checkingPlayer.toLowerCase());
        	checkingPlayer = null;
        }
        
    	synchronized(requests) {
    	    Iterator<Map.Entry<String, EventRequest>> it = playerRequests.iterator();
    	    Date lastChecked = null;
    	    
    	    while (it.hasNext()) {
                Map.Entry<String, EventRequest> request = it.next();
                
                if(lastChecked == null || request.getValue().getLastCheck().before(lastChecked)) {
                	lastChecked = request.getValue().getLastCheck();
                	checkingPlayer = request.getValue().getRequester();
                }
            }
    	    
    	    if(checkingPlayer != null) {
    	    	m_botAction.locatePlayer(checkingPlayer);
    	    	requests.get(checkingPlayer.toLowerCase()).setLastCheck(new Date());
    	    }
    	}
    }
    
    /**
     * Returns all the EventRequest objects from the requests HashMap matching the event name.
     * 
     * @param event The event to search for EventRequest objects with (ignores case)
     * @return list with all the EventRequests objects from the requests HashMap matching the event name.
     */
    private HashSet<EventRequest> getEventRequests(String event) {
        synchronized(requests) {
        	HashSet<EventRequest> events = new HashSet<EventRequest>();
        	
        	for(EventRequest er:requests.values()) {
        		if(er.getEvent().equalsIgnoreCase(event))
        			events.add(er);
        	}
        	return events;
        }
    }
    
    /**
     * Updates the HashSet with banned players
     * 
     * @param banString the string from the eventbot.cfg file
     */
    private void loadBannedPlayers() {
        
        int nr = 0;
        String banLine = m_botSettings.getString("bannedPlayer0");
        
        while(banLine != null) {
            if(banLine.trim().length() > 0) {
                String[] pieces = banLine.split(":");
                if(pieces.length == 3) {
                    long date = Long.valueOf(pieces[0]).longValue();
                    this.bannedPlayers.add(new BannedPlayer(pieces[2],pieces[1],new Date(date)));
                }
            }
            
            nr++;
            banLine = m_botSettings.getString("bannedPlayer"+nr);
        }
    }
    
    /**
     * Saves the contents of the HashSet to the eventbot.cfg file
     */
    private void saveBannedPlayers() {
    	int nr = 0;
    	
    	// remove all bans from configuration file as it will be put again
    	int i = 0;
    	String key = "bannedPlayer"+i;
    	String p = m_botSettings.getString(key);
    	
    	while(p != null) {
    	    m_botSettings.remove(key);
    	    i++;
    	    key = "bannedPlayer"+i;
            p = m_botSettings.getString(key);
    	}
    	
    	// Put all bans to configuration file
    	for(BannedPlayer player:bannedPlayers) {
    	    m_botSettings.put("bannedPlayer"+(nr++), player.getDateTime()+":"+player.bannedby+":"+player.name);
    	}
    	
    	m_botSettings.save();
    }
    
    /**
     * Removes a banned player from the HashSet with BannedPlayer's
     * @param name
     */
    private void removeBannedPlayer(String name) {
        for(BannedPlayer p:bannedPlayers) {
            if(p.name.equals(name)) {
                bannedPlayers.remove(p);
                break;
            }
        }
    }
    
    /**
     * Checks if given player is banned from using this bot
     * 
     * @param name
     * @return
     */
    private boolean isBannedPlayer(String name) {
        boolean result = false;
        
        for(BannedPlayer p:bannedPlayers) {
            if(p.name.equalsIgnoreCase(name)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    /**
     * Sends a PM to every subscribed staffer
     *  
     * @param subscriptionType
     * @param request
     */
    private void notifySubscribed(String subscriptionType, EventRequest request) {
        for(String name:subscribers.keySet()) {
            if(subscribers.get(name).equals(subscriptionType)) {
                if(subscriptionType.equals(SUBSCRIBE_ALL)) {
                    String comments = "";
                    
                    if(comments.length() > 0)   comments = "("+comments+") ";
                    m_botAction.sendRemotePrivateMessage(name, " "+request.getRequester()+" requested event '"+request.getEvent()+"' "+comments+"(rank: "+getEventRank(request.getEvent())+")");
                }
                else if(subscriptionType.equals(SUBSCRIBE_BWJS))
                    m_botAction.sendRemotePrivateMessage(name, "Please start a new game in "+request.getEvent()+". (Requested by "+request.getRequester()+")");
            }
        }
    }
}

class BannedPlayer {
    
    public String name;
    public String bannedby;
    public Date date;
    
    public BannedPlayer(String name, String bannedby, Date date) {
        this.name = name;
        this.bannedby = bannedby;
        this.date = date;
    }
    
    public String getDateTime() {
        return String.valueOf(this.date.getTime());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((bannedby == null) ? 0 : bannedby.hashCode());
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        final BannedPlayer other = (BannedPlayer) obj;
        if (bannedby == null) {
            if (other.bannedby != null)
                return false;
        } else if (!bannedby.equals(other.bannedby))
            return false;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}



/**
 * POJO class for eventbot.
 * This Plain Old Java Object (POJO) stores a few properties of a event request.
 * 
 * @author Maverick
 */
class EventRequest {
    private String requester;
    private String event;
    private String comments;
    private Date lastrequest;
    private Date lastCheck;
    
    public EventRequest(String requester, String event) {
        this.requester = requester;
        this.event = event;
        this.comments = null;
        this.lastrequest = new Date();
        this.lastCheck = new Date();
    }
    
    public EventRequest(String requester, String event, String comments) {
        this.requester = requester;
        this.event = event;
        this.comments = comments;
        this.lastrequest = new Date();
        this.lastCheck = new Date();
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return lastrequest;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.lastrequest = date;
    }

    /**
     * @return the event
     */
    public String getEvent() {
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * @return the requester
     */
    public String getRequester() {
        return requester;
    }

    /**
     * @param requester the requester to set
     */
    public void setRequester(String requester) {
        this.requester = requester;
    }

    /**
     * @return the comments
     */
    public String getComments() {
        return comments;
    }

    /**
     * @param comments the comments to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * @return the lastrequest
     */
    public Date getLastrequest() {
        return lastrequest;
    }

    /**
     * @param lastrequest the lastrequest to set
     */
    public void setLastrequest(Date lastrequest) {
        this.lastrequest = lastrequest;
    }

	/**
	 * @return the lastCheck
	 */
	public Date getLastCheck() {
		return lastCheck;
	}

	/**
	 * @param lastCheck the lastCheck to set
	 */
	public void setLastCheck(Date lastCheck) {
		this.lastCheck = lastCheck;
	}
    

}