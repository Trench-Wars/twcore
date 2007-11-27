package twcore.bots.eventbot;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
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
 * TODO:
 *  - Allow players to cancel their own request
 *  - !removeban
 *  - !listban # (default 10)
 *  - Make sure you can't !ban yourself or staff
 *  - Configurable chats (chat for requests, chat for wbduel/base requests)
 *  - Alias for !fillrequest: !fr
 *  
 *  
 *  
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
    private HashSet<String> bannedPlayers = new HashSet<String>();


    public eventbot(BotAction botAction) {
        super(botAction);
        m_botAction = BotAction.getBotAction();
        m_opList = m_botAction.getOperatorList();
        m_botSettings = m_botAction.getBotSettings();
        int acceptedMessageTypes;
        
        loadBannedPlayers(m_botAction.getBotSettings().getString("bannedPlayers"));
      
        // Set which events to handle
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.LOGGED_ON);
        
        acceptedMessageTypes = Message.PRIVATE_MESSAGE | Message.REMOTE_PRIVATE_MESSAGE;
        // handle the PM commands
        commandInterpreter = new CommandInterpreter(m_botAction);
        commandInterpreter.registerCommand("!help", acceptedMessageTypes, this, "cmdHelp");
        commandInterpreter.registerCommand("!request", acceptedMessageTypes, this, "cmdRequest");
        commandInterpreter.registerCommand("!requests", acceptedMessageTypes, this, "cmdRequests", OperatorList.ER_LEVEL);
        commandInterpreter.registerCommand("!die", acceptedMessageTypes, this, "cmdDie", OperatorList.SMOD_LEVEL);
        commandInterpreter.registerCommand("!ban", acceptedMessageTypes, this, "cmdBan", OperatorList.MODERATOR_LEVEL);
        commandInterpreter.registerCommand("!banned", acceptedMessageTypes, this, "cmdBanned", OperatorList.MODERATOR_LEVEL);
        
        // handle the chat commands
        acceptedMessageTypes = Message.CHAT_MESSAGE;
        commandInterpreter.registerCommand("!removerequest", acceptedMessageTypes, this, "cmdRemoveRequest", OperatorList.ER_LEVEL);
        commandInterpreter.registerCommand("!fillrequest", acceptedMessageTypes, this, "cmdFillRequest", OperatorList.ER_LEVEL);
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
    	String[] publicCommands = 
    	{ 
    		"EventBot commands:",
    		"!help                       - This help menu",
    		"!request <event>:[comments] - Request an <event> to be hosted. Optional; any [comments]",
    		"                              You can make only one request per hour but you can change it."
    	};
    	m_botAction.smartPrivateMessageSpam(name, publicCommands);
    	
    	// ER+ commands
    	String[] erCommands = 
    	{
    		"!requests [#]               - Display the top 10 or top [#] event requests",
    		"!requests [event]           - Display details of the requests for [event]",
    		"[CHAT] !removerequest <name>- Remove request of player<name>",
    		"[CHAT] !fillrequest <event> - Let all players requesting <event> know that you are hosting the event",
    		"                              and removes all matching requests"
    	};
    	if(m_opList.isER(name))
    		m_botAction.smartPrivateMessageSpam(name, erCommands);
    	
    	// Mod+ commands
    	String[] modCommands = {
    		"!ban <name>                 - Permanently disallows player <name> to use !request.",
    		"                              The bot doesn't notify the player that he's banned.",
    		"                              To unban, use !ban again."
    	};
    	if(m_opList.isModerator(name))
    		m_botAction.smartPrivateMessageSpam(name, modCommands);
    	
    	// Smod+ commands
    	String[] smodCommands = {
    		"!die                        - Remove this bot from the zone"
    	};
    	if(m_opList.isSmod(name)) 
    		m_botAction.smartPrivateMessageSpam(name, smodCommands);
    }
    
    /**
     * [PM] !request [PLAYER+]
     * 
     * @param name
     * @param message
     */
    public void cmdRequest(String name, String message) {
    	message = message.trim();
    	
    	// Cleanup expired requests
    	this.removeExpiredRequests();
    	
    	// Validate all parameters
    	if(message.length() == 0) {
    		m_botAction.sendSmartPrivateMessage(name, "Syntax error: Please specify the <event> and any optional [comments] seperated by ':'. Type ::!help for more information.");
    		return;
    	}
    	
    	// Check if this player isn't banned
    	if(bannedPlayers.contains(name.toLowerCase())) {
    		m_botAction.sendSmartPrivateMessage(name, "I'm sorry but you aren't allowed to use !request.");
    		return;
    	}
    	
    	// Staff shouldn't make !requests
    	/*if(m_botAction.getOperatorList().isZH(name)) {
    		m_botAction.sendSmartPrivateMessage(name, "I'm sorry but staff can't make !requests. Type ';hey I want to have <event> hosted!' to continue.");
    		return;
    	}*/
    	
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
						Tools.formatString((i+1)+")", 4)+
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
     * [CHAT] !removerequest <name> [ER+]
     * 
     * @param name
     * @param message
     */
    public void cmdRemoveRequest(String name, String message) {
    	String requester = message.trim().toLowerCase();
    	if(requests.containsKey(requester)) {
    		m_botAction.sendChatMessage("Request of "+message+" ("+requests.get(requester).getEvent()+") removed.");
    		requests.remove(requester);
    	} else {
    		m_botAction.sendChatMessage("No requests found of '"+message+"' .");
    	}
    }
    
    /**
     * [CHAT] !fillrequest <event> [ER+]
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
    			m_botAction.sendSmartPrivateMessage(er.getRequester(), er.getRequester()+", your requested event ("+er.getEvent()+") will now be hosted by "+name+".");
    			// Remove request
    			requests.remove(er.getRequester().toLowerCase());
    			m_botAction.sendChatMessage("Informed "+matchingRequests.size()+" players (and matching requests removed).");
    		}
    		
    	} else {
    		m_botAction.sendChatMessage("No matching requests with the event '"+event+"' found.");
    	}
    }
    
    /**
     * !ban <name> [MOD+]
     * 
     * @param name
     * @param message
     */
    public void cmdBan(String name, String message) {
    	String playername = message.trim().toLowerCase();
    	
    	if(bannedPlayers.contains(playername)) {
    		bannedPlayers.remove(playername);
    		m_botAction.sendSmartPrivateMessage(name, "Player '"+playername+"' removed from !request ban.");
    	} else {
    		bannedPlayers.add(playername);
    		m_botAction.sendSmartPrivateMessage(name, "Player '"+playername+"' is now permanently banned from using !request.");
    	}
    	saveBannedPlayers();
    }
    
    
    /**
     * !banned [MOD+]
     * 
     * @param name
     * @param message
     */
    public void cmdBanned(String name, String message) {
    	if(bannedPlayers.size() == 0) {
    		m_botAction.sendSmartPrivateMessage(name, "No banned players found.");
    	} else {
	    	m_botAction.sendSmartPrivateMessage(name, "Banned EventBot players:");
	    	m_botAction.sendSmartPrivateMessage(name, "------------------------");
	    	
	    	for(String playername:bannedPlayers) {
	    		m_botAction.sendSmartPrivateMessage(name, "  "+playername);
	    	}
    	}
    }
    
    /**
     * !die [SMOD+]
     * 
     * @param name
     * @param message
     */
    public void cmdDie(String name, String message) {
    	requests = null;
    	m_botAction.die();
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
    private void loadBannedPlayers(String banString) {
		StringTokenizer banTokens = new StringTokenizer(banString, ",");
		bannedPlayers.clear();
		
		while(banTokens.hasMoreTokens()) {
			String playername = banTokens.nextToken();
			  
			if(playername.length() > 0) { 
				bannedPlayers.add(playername.toLowerCase());
			}
		}
    }
    
    /**
     * Saves the contents of the HashSet to the eventbot.cfg file
     */
    private void saveBannedPlayers() {
    	String banString = "";
    	
    	for(String bannedplayer:bannedPlayers) {
    		banString += bannedplayer+",";
    	}
    	
    	m_botAction.getBotSettings().put("bannedPlayers", banString);
    	m_botAction.getBotSettings().save();
    }
    
}
