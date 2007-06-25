package twcore.bots.eventbot;

import java.util.HashMap;
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
 * A basic form of a bot, but with command interpretation not provided in
 * the "basicbot" class template.  Use this one instead.
 * @author Maverick
 */
public class eventbot extends SubspaceBot {
    private BotSettings m_botSettings;
    private BotAction m_botAction;
    private OperatorList m_opList;
    private CommandInterpreter commandInterpreter;
    
    //This vector stores all the requests
    private Vector<EventRequest> requests = new Vector<EventRequest>(64);


    public eventbot(BotAction botAction) {
        super(botAction);
        m_botAction = BotAction.getBotAction();
        m_opList = m_botAction.getOperatorList();
        m_botSettings = m_botAction.getBotSettings();
        int acceptedMessageTypes;
      
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
        commandInterpreter.registerCommand("!die", acceptedMessageTypes, this, "cmdDie", OperatorList.MODERATOR_LEVEL);
        
        // handle the chat commands
        acceptedMessageTypes = Message.CHAT_MESSAGE;
        commandInterpreter.registerCommand("!denyrequest", acceptedMessageTypes, this, "cmdDenyRequest");
        commandInterpreter.registerDefaultCommand(Message.ARENA_MESSAGE, this, "handleZone");
        commandInterpreter.registerDefaultCommand(Message.ALERT_MESSAGE, this, "handleAlert");
    }


    /** write an event handler for each requested packet */
    public void handleEvent(Message event) {
    	commandInterpreter.handleEvent(event);
    }


    /* when the bot logs on, you have to manually send it to an arena */
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Staff Chat" ));
    }
    
    public void handleAlert(String name, String message) {
    	m_botAction.sendTeamMessage("ALERT: "+name+"="+message);
    }
    
    public void handleZone(String name, String message) {
    	m_botAction.sendTeamMessage("ZONE: "+name+"="+message);
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
    		"                              You can make only one request per hour"
    	};
    	m_botAction.smartPrivateMessageSpam(name, publicCommands);
    	
    	// ER+ commands
    	String[] erCommands = 
    	{
    		"!requests [#]               - Display the top 10 or top [#] event requests",
    		"!requests [event]           - Display details of the requests for [event]",
    		"[CHAT] !removerequest <name>- Remove request of player<name>"
    		
    	};
    	if(m_opList.isER(name))
    		m_botAction.smartPrivateMessageSpam(name, erCommands);
    	
    	// Mod+ commands
    	String[] modCommands = {
    		"!die                        - Remove this bot from the zone"
    	};
    	if(m_opList.isModerator(name))
    		m_botAction.smartPrivateMessageSpam(name, modCommands);
    }
    
    public void cmdRequest(String name, String message) {
    	message = message.trim();
    	
    	// Validate all parameters
    	if(message.length() == 0) {
    		m_botAction.sendSmartPrivateMessage(name, "Syntax error: Please specify the <event> and any optional [comments] seperated by ':'. Type ::!help for more information.");
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
    		requests.add(eventRequest);			// Add to request list
    		
    		// Let staff know there is a new request
    		if(comments.length() > 0)	comments = "('"+comments+"') ";
    		m_botAction.sendChatMessage("New event request: "+name+" requested "+event+" "+comments+"(rank:"+getEventRank(event)+")");
    		
    		// Notify requester about his request
    		m_botAction.sendSmartPrivateMessage(name, "Your request for "+event+" "+comments+"has been registered and forwarded to staff.");
    		m_botAction.sendSmartPrivateMessage(name, "If you want to change your request or make a new request once this request expires (after 60 mins), please use !request again.");
    	} else {
    		throw new NullPointerException("EventRequest is null in EventBot, method cmdRequest()");
    	}
    }
    
    public void cmdRequests(String name, String message) {
    	HashMap<String, Integer> rank = new HashMap<String, Integer>();
    	Vector<String> rankPosition = new Vector<String>();
    	
    	// Create the event with count mapping
    	for(EventRequest request:requests) {
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
    	if(nr < 1 || nr > 100)
    		nr = 10;
    	
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
    }
    
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
    	for(EventRequest request:requests) {
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
    
}


