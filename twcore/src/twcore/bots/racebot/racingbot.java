package twcore.bots.racebot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.*;

public class racingbot extends SubspaceBot {

	HashMap<String, RacingBotExtension> modules;
	HashSet<String> twrcOps;
	BotSettings m_botSettings;
	OperatorList m_operatorList;
	TimerTask resetFlags;

	public final String mySQLHost = "twrc";

	public racingbot( BotAction botAction ) {
		super( botAction );

		modules = new HashMap<String, RacingBotExtension>();
		twrcOps = new HashSet<String>();

        m_botAction.getEventRequester().requestAll();
        m_botSettings = m_botAction.getBotSettings();
        m_operatorList = m_botAction.getOperatorList();
	}

	public void handleEvent( LoggedOn event ) {
		String allOps = m_botSettings.getString("TWRC Ops");
		String ops[] = allOps.split(":");
		for(int k = 0;k < ops.length;k++)
			twrcOps.add(ops[k].toLowerCase());
		m_botAction.joinArena("twrc");
	}

	public void distributeEvent( SubspaceEvent event ){

        Iterator<Map.Entry<String, RacingBotExtension>> i = modules.entrySet().iterator();
        while( i.hasNext() ){
            Map.Entry<String, RacingBotExtension> entry = i.next();
            RacingBotExtension ext = entry.getValue();
            ext.handleEvent( event );
        }
    }


	public void handleEvent( Message event ){
        String name = m_botAction.getPlayerName( event.getPlayerID() );
		String message = event.getMessage().toLowerCase();

		if(name == null) return;

		if( message.startsWith( "!go " ) && name != null &&
		    (m_botAction.getOperatorList().isER(name) || twrcOps.contains(name.toLowerCase()))) {
			String pieces[] = message.split(" ");
			if( message.length() < 2 ) return;
        	modules.clear();
			m_botAction.joinArena( pieces[1] );
    	}
    	else if( message.startsWith( "!help" ) && name != null &&
    	        (m_botAction.getOperatorList().isER(name) || twrcOps.contains(name.toLowerCase()))) {
    		help( name, message.substring( 5 ));

    		if(message.substring(5).trim().length() == 0)
    		    distributeEvent( (SubspaceEvent)event );
    	}
    	else if(message.startsWith("!die") && name != null &&
    	        (m_botAction.getOperatorList().isER(name) || twrcOps.contains(name.toLowerCase()))) {
    	    m_botAction.cancelTasks();
    	    m_botAction.die();
    	}
    	else if(message.startsWith("!leave")) {
    	    m_botAction.specWithoutLock(name);
    	} else {
    	    distributeEvent( (SubspaceEvent)event );
    	}
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
   	        m_botAction.sendPrivateMessage(name, "`");
   	        m_botAction.sendPrivateMessage(name, "<> required  [] optional  () comments  -s[=assign] switches");
   	    } else if(key.startsWith("go")) {
   	        m_botAction.sendPrivateMessage(name, "!go <arena>  :  Make the bot move to the specified <arena>");
   	    } else if(key.startsWith("die")) {
   	        m_botAction.sendPrivateMessage(name, "!die   :   Disconnect this bot");
   	    } else if(key.startsWith("leave")) {
   	        m_botAction.sendPrivateMessage(name, "!leave  :  Puts you into spectator");
   	    } else {
            m_botAction.sendPrivateMessage(name, "RacingBot "+accessLevel(m_operatorList.getAccessLevel(name))+" commandlist  (Send ::!help <topic> for more info)");
            m_botAction.sendPrivateMessage(name, "lvl ER:     !help  !go  !die");
            m_botAction.sendPrivateMessage(name, "lvl Player: !leave");
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