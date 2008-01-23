package twcore.bots.eventbot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
 */
public class eventbot extends SubspaceBot {
    private BotSettings m_botSettings;
    private BotAction m_botAction;
    private OperatorList m_opList;
    private CommandInterpreter commandInterpreter;
    
    private final int REQUEST_EXPIRE_TIME_MS = 1000*60*60;	// 60 min.
    
    //This vector stores all the requests
    private HashMap<String,EventRequest> requests = new HashMap<String,EventRequest>(64);
    			// <playername, EventRequest>
    
    // Contains the names of all the players not allowed to use !request - stored in the eventbot.cfg file
    private HashSet<BannedPlayer> bannedPlayers = new HashSet<BannedPlayer>();
    
    // Contains the names of staff member subscribed to event requests
    private HashMap<String, String> subscribers = new HashMap<String, String>(16);
    //              <playername, "all">
    //              <playername, "bwjs">
    
    // SimpleDateFormat to format dates on !listban
    // 2008-01-23 21:02
    // yyyy-MM-dd HH:mm
    private SimpleDateFormat banDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");


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
        
        // ZH+
        int level = OperatorList.ZH_LEVEL;
        commandInterpreter.registerCommand("!subscribe", acceptedMessageTypes, this, "cmdSubscribe", level);
        
        // ER+
        level = OperatorList.ER_LEVEL; 
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
    }


    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Staff Chat" ));
        
        // Only 1 messages per minute allowed for non-staff.
        m_botAction.setMessageLimit(2);
    }
    
    /*****************************************************************************
     *                          COMMAND HANDLERS
     *****************************************************************************/
    public void cmdHelp(String name, String message) {
    	// Public commands
    	String[] startCommands = 
    	{   "/-------------------------------------------------------------------------------\\",
    		"|                                 EVENTBOT                                      |",  };
    	String[] publicCommands = 
    	{	"|                                                                               |",
    		"| !request <event>:[comments] - Request an <event> to be hosted. Optional; any  |",
    		"|                               [comments]. You can make only one request per   |",
    		"|                               hour but you can change it.                     |",
    		"| !request -                  - Cancels your current request.                   |"   };
    	String[] zhCommands = 
    	{   "|------------------------------     ZH+    -------------------------------------|",
    	    "| !subscribe all              - EventBot will PM you on any event request       |",
    	    "| !subscribe bwjs             - EventBot will PM you on event requests for base,|",
    	    "|                               WBDuel, JavDuel or SpidDuel                     |",
    	    "| !subscribe off              - Removes any subscription to event requests      |"
    	};
    	String[] erCommands = 
        {   "|------------------------------     ER+    -------------------------------------|",
    	    "| !requests [#]  / !reqs [#]  - Display the top 10 or top [#] event requests    |",
            "| !requests [event]           - Display details of the requests for [event]     |",
            "| !fillrequest <event> / !fr  - Let all players requesting <event> know that    |",
            "|                               you are hosting the event (soon). Also removes  |",
            "|                               all requests for <event>.                       |",
            "| !removerequest <name> / !rr - Remove request of player<name>                  |",
  	        "| !ban <name>                 - Permanently disallows player <name> to use      |",
  	        "|                               !request. The bot doesn't notify the player that|",
  	        "|                               he's banned.                                    |",
  	        "| !listban [#]                - Lists banned players                            |"    };
    	String[] modCommands = 
    	{   "|-------------------------------    MOD+   -------------------------------------|",
    	    "| !liftban <name>             - Removes ban on <name>                           |"   };
    	String[] smodCommands = 
    	{   "|-------------------------------   SMOD+   -------------------------------------|",
    	    "|  !die                       - Removes EventBot from the zone                  |"    };
    	String[] endCommands =
    	{   "\\-------------------------------------------------------------------------------/"    };
    	
    	
    	m_botAction.smartPrivateMessageSpam(name, startCommands);
    	
    	if(!m_opList.isZH(name))   // Public only, not staff
    	    m_botAction.smartPrivateMessageSpam(name, publicCommands);
    	
    	if(m_opList.isZH(name))    // ZH+
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
    	
    	// TODO: Spread PMs to subscribers on event request
    	
    	// Cleanup expired requests
    	this.removeExpiredRequests();
    	
    	// Validate all parameters
    	if(message.length() == 0) {
    		m_botAction.sendSmartPrivateMessage(name, "Syntax error: Please specify the <event> and any optional [comments] seperated by ':'. Type ::!help for more information.");
    		return;
    	}
    	
    	// Check if this player isn't banned
    	if(isBannedPlayer(name.toLowerCase())) {
    		m_botAction.sendSmartPrivateMessage(name, "I'm sorry but you aren't allowed to use !request.");
    		return;
    	}
    	
    	// Staff shouldn't make !requests
    	if(m_botAction.getOperatorList().isZH(name)) {
    		m_botAction.sendSmartPrivateMessage(name, "I'm sorry but staff can't make !requests. Type ';hey I want to have <event> hosted!' to continue.");
    		return;
    	}
    	
    	// Player cancels his request
    	if(message.equals("-")) {
    	    if(requests.containsKey(name.toLowerCase())) {
    	        String event = requests.get(name.toLowerCase()).getEvent();
    	        m_botAction.sendSmartPrivateMessage(name, "Your request for "+event+" has been removed.");
    	        m_botAction.sendChatMessage(name+" removed his request for "+event+" .");
                requests.remove(name.toLowerCase());
            } else {
                m_botAction.sendSmartPrivateMessage(name, "No request was found to remove.");
            }
    	    return;
    	}
    	
    	EventRequest eventRequest = null;
    	String event, comments = "";
    	
    	if(message.contains(":")) {		// Message includes comments 
    		event = message.substring(0,message.indexOf(":")).trim();
    		comments = message.substring(message.indexOf(":")+1).trim();
    		eventRequest = new EventRequest(name, event, comments);
    	} else {						// No comments available
    		event = message;
    		eventRequest = new EventRequest(name, message);
    	}
    	
    	if(eventRequest != null) {
    		// Check if player's previous request was done more then 1 minute ago to prevent abuse.
    		if(requests.containsKey(name.toLowerCase())) {
    			EventRequest eventReq = requests.get(name.toLowerCase());
    			
    			if((new Date().getTime() - eventReq.getDate().getTime()) <= (1000*60)) {
    				// Player made the request less then one minute ago, tell him he needs to wait before doing another request
    				m_botAction.sendSmartPrivateMessage(name, "You have to wait one minute before changing your previous request. Please try again later.");
    				return;
    			}
    		}
    		
    		   		
    		// If event is base/javduel/spidduel/wbduel-type then make the request directly without saving it.
    		if(	event.toLowerCase().startsWith("base") || 
    				event.toLowerCase().startsWith("javduel") ||
    				event.toLowerCase().startsWith("spidduel") ||
    				event.toLowerCase().startsWith("wbduel")) {
    			
    			// Inform staff
    			m_botAction.sendChatMessage("> Staff, please start a new game in "+event+". (Requested by "+name+")");

    			// Notify requester about his request
    			m_botAction.sendSmartPrivateMessage(name, "Your request for '"+event+"' has been forwarded to staff. It will be dealt with as soon as possible.");
    		} else {
    			boolean newRequest = true;
    			
    			// Replace previous request
        		if(requests.containsKey(name.toLowerCase())) {
        			requests.remove(name.toLowerCase());
        			newRequest = false;
        		}
        		// Add to request list
        		requests.put(name.toLowerCase(), eventRequest);
    			
        		// Inform staff
        		if(comments.length() > 0)	comments = "("+comments+") ";
        		
        		if(newRequest)
        			m_botAction.sendChatMessage(name+" requested '"+event+"' "+comments+"(rank: "+getEventRank(event)+")");
        		else
        			m_botAction.sendChatMessage(name+" changed request to '"+event+"' "+comments+"(rank: "+getEventRank(event)+")");
    			
    			// Notify requester about his request
        		m_botAction.sendSmartPrivateMessage(name, "Your request for "+event+" has been registered and forwarded to staff.");
        		m_botAction.sendSmartPrivateMessage(name, "If you want to change your request or make a new request once this request expires (after 60 mins), please use !request again.");
    		}
    		
    	} else {
    		throw new NullPointerException("EventRequest is null in EventBot, method cmdRequest()");
    	}
    }
    
    /**
     * [PM] !subscribe all / bwjs / off[ZH+]
     * 
     * @param name
     * @param message
     */
    public void cmdSubscribe(String name, String message) {
        message = message.trim().toLowerCase();
        String staffer = name.toLowerCase();
        
        if(message.length() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "Command syntax error. Please specify 'all', 'bwjs' or 'off'. Type ::!help for more information.");

        } else if(message.equals("all")) {
            if(subscribers.containsKey(staffer) && subscribers.get(staffer).indexOf("all")>-1) {
                m_botAction.sendSmartPrivateMessage(name, "You are already subscribed for all event requests.");
            } else {
                if(subscribers.containsKey(staffer) && subscribers.get(staffer).equals("bwjs")) {
                    subscribers.put(staffer, "all+bwjs");
                } else {
                    subscribers.put(staffer, "all");
                }
                
                m_botAction.sendSmartPrivateMessage(name, "You are now subscribed to all event requests.");
            }
        } else if(message.equals("bwjs")) {
            if(subscribers.containsKey(staffer) && subscribers.get(staffer).indexOf("bwjs")>-1) {
                m_botAction.sendSmartPrivateMessage(name, "You are already subscribed for Base/WbDuel/JavDuel/SpidDuel event requests.");
            } else {
                if(subscribers.containsKey(staffer) && subscribers.get(staffer).equals("all")) {
                    subscribers.put(staffer, "all+bwjs");
                } else {
                    subscribers.put(staffer, "bwjs");
                }
                
                m_botAction.sendSmartPrivateMessage(name, "You are now subscribed to Base/WbDuel/JavDuel/SpidDuel event requests.");
            }
        } else if(message.equals("off")) {
            if(subscribers.containsKey(staffer)) {
                m_botAction.sendSmartPrivateMessage(name, "Removed any subscriptions for event requests.");
                subscribers.remove(staffer);
            } else {
                m_botAction.sendSmartPrivateMessage(name, "You are not subscribed to any event requests.");
            }
        } else {
            m_botAction.sendSmartPrivateMessage(name, "Command syntax error. You can only specify 'all', 'bwjs' or 'off'. Type ::!help for more information.");
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
    	
    	// Cleanup expired requests
    	this.removeExpiredRequests();
    	
    	// Create the event with count mapping
    	for(EventRequest request:requests.values()) {
    		if(rank.containsKey(request.getEvent())) {
    			rank.put(request.getEvent(), rank.get(request.getEvent())+1);
    		} else {
    			rank.put(request.getEvent(), new Integer(1));
    			rankPosition.add(request.getEvent());
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
    				if(er.getComments() != null && er.getComments().length()>0) {
    					m_botAction.sendSmartPrivateMessage(name, 
    							Tools.rightString(er.getRequester()+"> ", 20)+er.getComments());
    				} else {
    					m_botAction.sendSmartPrivateMessage(name, " "+
        						Tools.rightString(er.getRequester(), 20));
    				}
    			}
    		} else {
    			m_botAction.sendSmartPrivateMessage(name, "Event '"+event+"' is not found amongst the saved requests of the last "+((REQUEST_EXPIRE_TIME_MS/1000)/60)+" minutes.");
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
    	
    	// Cleanup expired requests
        this.removeExpiredRequests();
    	
    	if(requests.containsKey(requester)) {
    		m_botAction.sendChatMessage(name+" removed request of "+message+" ("+requests.get(requester).getEvent()+") removed.");
    		requests.remove(requester);
    	} else {
    		m_botAction.sendSmartPrivateMessage(name, "No requests found of '"+message+"' .");
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
    	
    	// Cleanup expired requests
        this.removeExpiredRequests();
    	
    	if(matchingRequests.size() > 0) {

    		// Loop over all the EventRequest, inform the player and remove the request
    		for(EventRequest er:matchingRequests) {
    			// Inform player
    			m_botAction.sendRemotePrivateMessage(er.getRequester(), er.getRequester()+", your requested event ("+er.getEvent()+") will now be hosted by "+name+".");
    			// Remove request
    			requests.remove(er.getRequester().toLowerCase());
    		}
    		m_botAction.sendChatMessage(name+" filled requests for "+event+", informing "+matchingRequests.size()+" players. (Matching requests removed.)");
    		
    	} else {
    		m_botAction.sendSmartPrivateMessage(name,"No matching requests with the event '"+event+"' found.");
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
    	} else if(m_opList.isZH(playername)) {
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
	    	m_botAction.sendSmartPrivateMessage(name, "Banned EventBot players:");
	    	m_botAction.sendSmartPrivateMessage(name, "------------------------");
	    	
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
	    	
	    	if(bannedPlayers.size() > count) {
	    	    start = bannedPlayers.size() - count;
	    	    offset = start;
	    	}
	    	
	    	for(BannedPlayer player:bannedPlayers) {
	    	    if(start > 0) {
	    	        start--;
	    	        continue;
	    	    }
	    	    
	    	    String date = banDateFormat.format(player.date);
	    	    String bannedby = player.bannedby.substring(0,10);
	    	    
	    		m_botAction.sendSmartPrivateMessage(name, "#"+(offset+i)+" by "+bannedby+" "+date+" "+player.name);
	    		i++;
	    		
	    		if(i >= count) {
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
    	for(EventRequest request:requests.values()) {
    		if(rank.containsKey(request.getEvent())) {
    			rank.put(request.getEvent(), rank.get(request.getEvent())+1);
    		} else {
    			rank.put(request.getEvent(), new Integer(1));
    			rankPosition.add(request.getEvent());
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
    private void removeExpiredRequests() {
    	
    	for(String playername:requests.keySet()) {
    		EventRequest eventReq = requests.get(playername);
    		long now = new Date().getTime();
    		long hour = 1000*60*60;
    		
    		if((now - eventReq.getDate().getTime()) > hour) {
    			requests.remove(playername);
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
    	HashSet<EventRequest> events = new HashSet<EventRequest>();
    	
    	for(EventRequest er:requests.values()) {
    		if(er.getEvent().equalsIgnoreCase(event))
    			events.add(er);
    	}
    	return events;
    }
    
    /**
     * Updates the HashSet with banned players
     * 
     * @param banString the string from the eventbot.cfg file
     */
    private void loadBannedPlayers() {
        
        int nr = 0;
        String banLine = m_botAction.getBotSettings().getString("bannedPlayer0");
        
        while(banLine != null) {
            if(banLine.trim().length() > 0) {
                String[] pieces = banLine.split(":");
                if(pieces.length == 3) {
                    long date = Long.valueOf(pieces[0]).longValue();
                    this.bannedPlayers.add(new BannedPlayer(pieces[2],pieces[1],new Date(date)));
                }
            }
            
            nr++;
            banLine = m_botAction.getBotSettings().getString("bannedPlayer"+nr);
        }
    }
    
    /**
     * Saves the contents of the HashSet to the eventbot.cfg file
     */
    private void saveBannedPlayers() {
    	int nr = 0;
    	
    	for(BannedPlayer player:bannedPlayers) {
    	    m_botAction.getBotSettings().put("bannedPlayer"+(nr++), player.getDateTime()+":"+player.bannedby+":"+player.name);
    	}
    	
    	m_botAction.getBotSettings().save();
    }
    
    /**
     * Removes a banned player from the HashSet with BannedPlayer's
     * @param name
     */
    private void removeBannedPlayer(String name) {
        for(BannedPlayer p:bannedPlayers) {
            if(p.equals(name)) {
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
            if(p.name.equals(name)) {
                result = true;
                break;
            }
        }
        return result;
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
}
