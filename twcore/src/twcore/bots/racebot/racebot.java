package twcore.bots.racebot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.BallPosition;
import twcore.core.events.FileArrived;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.events.FlagPosition;
import twcore.core.events.FlagReward;
import twcore.core.events.FlagVictory;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerBanner;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.ScoreReset;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.SoccerGoal;
import twcore.core.events.SubspaceEvent;
import twcore.core.events.TurfFlagUpdate;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;
import twcore.core.util.Tools;

public class racebot extends SubspaceBot {

	HashMap<String, RaceBotExtension> modules;
	HashSet<String> twrcOps;
	BotSettings m_botSettings;
	OperatorList m_operatorList;

	public final String mySQLHost = "twrc";

	public racebot( BotAction botAction ) {
		super( botAction );

		modules = new HashMap<String, RaceBotExtension>();
		twrcOps = new HashSet<String>();

        m_botAction.getEventRequester().requestAll();
        m_operatorList = m_botAction.getOperatorList();
	}

	public void handleEvent( LoggedOn event ) {
	    loadOperators();
	    m_botSettings = m_botAction.getBotSettings();
		m_botAction.joinArena(m_botSettings.getString("InitialArena"));
	}
	
	/**
     * Loads the operators from the twrcbot.cfg configuration file
     */
    private void loadOperators() {
        // Get the operators from the twrcbot configuration file
        BotSettings twrcbotSettings = m_botAction.getCoreData().getBotConfig("twrcbot");
        String allOps = twrcbotSettings.getString("TWRC Ops");
        String ops[] = allOps.split(":");
        twrcOps.clear();
        
        for(int k = 0;k < ops.length;k++) {
            twrcOps.add(ops[k]);
        }
    }
    
    protected boolean isOperator(String name) {
        boolean isOperator = false;
        
        for(String operator:twrcOps) {
            if(operator.equalsIgnoreCase(name)) {
                isOperator = true;
                break;
            }
        }
        
        return isOperator;
    }

	public void distributeEvent( SubspaceEvent event ){

        Iterator<Map.Entry<String, RaceBotExtension>> i = modules.entrySet().iterator();
        while( i.hasNext() ){
            Map.Entry<String, RaceBotExtension> entry = i.next();
            RaceBotExtension ext = entry.getValue();
            ext.handleEvent( event );
        }
    }


	public void handleEvent( Message event ){
        String name = m_botAction.getPlayerName( event.getPlayerID() );
		String message = event.getMessage().toLowerCase();

		if(name == null) return;
		
		// Operator / Staff commands
		if(m_operatorList.isSmod(name) || twrcOps.contains(name)) {
		    if( message.startsWith( "!go " ) ) {
	            String pieces[] = message.split(" ");
	            if( message.length() < 2 ) return;
	            if( Tools.isAllDigits(pieces[1])) return;
	            modules.clear();
	            m_botAction.joinArena( pieces[1] );
	        }
		    else if(message.startsWith("!die") ) {
	            m_botAction.cancelTasks();
	            m_botAction.die();
	        }
		    else if(message.startsWith("!reload") ) {
	            loadOperators();
	            m_botAction.sendPrivateMessage(name, "Reload completed.");
	        }
		} 
		
		// Player commands
	    if( message.startsWith( "!help" ) ) {
            help( name, message.substring( 5 ));
        }
	    else if(message.startsWith("!operators")) {
            m_botAction.sendPrivateMessage(name, "TWRC Operators: ");
            
            String ops = "";
            for(String op:twrcOps) {
                if(ops.length() > 0)
                    ops += ", ";
                ops += op;
            }
            
            m_botAction.sendPrivateMessage(name, ops);
        }
	    else if(message.startsWith("!leave")) {
            m_botAction.specWithoutLock(name);
        }

		// Distribute message to modules
    	distributeEvent( (SubspaceEvent)event );
	}

    public void handleEvent( ArenaJoined event ){

        distributeEvent( (SubspaceEvent)event );

        if(!modules.containsKey("Race")) {
            RbRace mod = new RbRace();
            mod.set( m_botAction, mySQLHost, this);
            modules.put( "Race", mod );
        }

        if(!modules.containsKey("Track Manager")) {
            RbTrackManager mod2 = new RbTrackManager();
            mod2.set( m_botAction, mySQLHost, this);
            modules.put( "Track Manager", mod2 );
        }

        if(!modules.containsKey("TWRC")) {
            RbTWRC mod3 = new RbTWRC();
           	mod3.set(m_botAction, mySQLHost, this);
            modules.put("TWRC", mod3);
        }

        m_botAction.setPlayerPositionUpdating( 250 );
    }

   	public void help( String name, String key ){
   	    key = key.trim();

   	    if(key.startsWith("!")) {
   	        key = key.substring(1);
   	    }

   	    if(key.startsWith("help")) {
   	        m_botAction.sendPrivateMessage(name, "!help [command]  :  Display available commands and what they do");
   	        m_botAction.sendPrivateMessage(name, " ");
   	        m_botAction.sendPrivateMessage(name, "<> required  [] optional  () comments  -s[=assign] switches");
   	    } else if(key.startsWith("go")) {
   	        m_botAction.sendPrivateMessage(name, "!go <arena>  :  Make the bot move to the specified <arena>");
   	    } else if(key.startsWith("die")) {
   	        m_botAction.sendPrivateMessage(name, "!die   :   Disconnect this bot");
   	    } else if(key.startsWith("reload")) {
   	        m_botAction.sendPrivateMessage(name, "!reload :  Reloads the TWRC Operators from TWRCBot configuration.");
   	    } else if(key.startsWith("leave")) {
   	        m_botAction.sendPrivateMessage(name, "!leave  :  Puts you into spectator");
   	    } else if(key.startsWith("operators")) {
   	        m_botAction.sendPrivateMessage(name, "!operators : Returns the current TWRC Operators");
   	    } else {
   	        boolean operator = twrcOps.contains(name);
   	        String showOp = operator ? " (TWRC Op)" : "";
   	        
            m_botAction.sendPrivateMessage(name, "RacingBot "+accessLevel(m_operatorList.getAccessLevel(name))+showOp+" commandlist  (Send ::!help <command> for more info)");
            if(operator || m_operatorList.isSmod(name))
            m_botAction.sendPrivateMessage(name, "lvl Operator:  !go  !die  !reload");
            m_botAction.sendPrivateMessage(name, "lvl Player:    !help  !operators  !leave");
   	    }
    }

   	private String accessLevel(int level) {
   	    switch(level) {
   	        case 9: return "Owner";
   	        case 8 : return "System Operator";
   	        case 7 : return "Super Moderator";
   	        case 6 : ;
   	        case 5 : ;
   	        case 4: return "Moderator";
   	        case 3: return "Event Referee";
   	        case 2: ;
   	        case 1: return "Zone Helper";
   	        case 0: return "Player";
   	    }
   	    return "UNKNOWN";
   	}


    public void handleEvent( PlayerLeft event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( SubspaceEvent event ){
        distributeEvent( event );}
    public void handleEvent( ScoreReset event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( PlayerEntered event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( PlayerPosition event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( PlayerDeath event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( ScoreUpdate event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( WeaponFired event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FrequencyChange event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FrequencyShipChange event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FileArrived event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FlagReward event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FlagVictory event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( WatchDamage event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( SoccerGoal event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( BallPosition event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( Prize event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FlagPosition event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FlagClaimed event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( SQLResultEvent event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( FlagDropped event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( TurfFlagUpdate event ){
    	distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( PlayerBanner event ){
        distributeEvent( (SubspaceEvent)event );}
}