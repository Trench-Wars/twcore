package twcore.bots.racingbot;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.AdaptiveClassLoader;
import twcore.core.BotAction;
import twcore.core.BotSettings;
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

public class racingbot extends SubspaceBot {

	private AdaptiveClassLoader m_loader;
	HashMap<String, RacingBotExtension> modules;
	HashMap<String, RBExtender> extensions;
	HashSet<String> twrcOps;
	BotSettings m_botSettings;
	TimerTask resetFlags;
	boolean locked = false;
	File botRoot;
	File coreRoot;

	final String mySQLHost = "local";

	public racingbot( BotAction botAction ) {
		super( botAction );

		Vector repository = new Vector();
        coreRoot = new File( botAction.getGeneralSettings().getString( "Core Location" ) + "/twcore/bots/racingbot/" );
		m_loader = new AdaptiveClassLoader( repository, getClass().getClassLoader() );
		modules = new HashMap<String, RacingBotExtension>();
		extensions = new HashMap<String, RBExtender>();
		twrcOps = new HashSet<String>();

        coreRoot = new File( botAction.getGeneralSettings().getString( "Core Location" ) );
        botRoot = new File(coreRoot.getPath() + "/twcore/bots/racingbot");

        m_botAction.getEventRequester().requestAll();
        m_botSettings = m_botAction.getBotSettings();
	}

	public void handleEvent( LoggedOn event ) {
		String allOps = m_botSettings.getString("TWRC Ops");
		String ops[] = allOps.split(":");
		for(int k = 0;k < ops.length;k++)
			twrcOps.add(ops[k].toLowerCase());
		m_botAction.joinArena("twrc");
	}

	public void distributeEvent( SubspaceEvent event ){

        Iterator i = modules.entrySet().iterator();
        while( i.hasNext() ){
            Map.Entry entry = (Map.Entry)i.next();
            RacingBotExtension ext = (RacingBotExtension)entry.getValue();
            ext.handleEvent( event );
        }
        Iterator i2 = extensions.entrySet().iterator();
        while( i2.hasNext() ){
            Map.Entry entry = (Map.Entry)i2.next();
            RBExtender ext = (RBExtender)entry.getValue();
            ext.handleEvent( event );
        }
    }


	public void handleEvent( Message event ){
        distributeEvent( (SubspaceEvent)event );
        try {
	        String name = m_botAction.getPlayerName( event.getPlayerID() );
			String message = event.getMessage().toLowerCase();
			if(m_botAction.getOperatorList().isER(name) || twrcOps.contains(name.toLowerCase()))
			{

				if( message.startsWith( "!go " )) {
					if(!locked)
					{
						String pieces[] = message.split(" ");
						if( message.length() < 2 ) return;
			        	modules.clear();
						m_botAction.joinArena( pieces[1] );
					}
					else
						m_botAction.sendPrivateMessage(name, "Please unlock before moving the bot.");

		    	}
		    	else if(message.startsWith("!load "))
		    	{
		    		String pieces[] = message.split(" ");
		    		load(name, pieces[1]);
		    	}
		    	else if(message.startsWith("!unload "))
		    	{
		    		String pieces[] = message.split(" ");
		    		remove(name, pieces[1]);
		    	}
		    	else if(message.startsWith("!lock"))
		    	{
		    		locked = true;
		    		m_botAction.sendPrivateMessage(name, "Bot locked.");
		    	}
		    	else if(message.startsWith("!unlock"))
		    	{
		    		locked = false;
		    		m_botAction.sendPrivateMessage(name, "Bot unlocked.");
		    	}
		    	else if(message.startsWith("!modules"))
		    		handleList(name, message);
		    	else if( message.startsWith( "!help " )){
            		help( name, message.substring( 6 ));
            	}
		    	if(message.startsWith("!die"))
		    		m_botAction.die();
		    }

		    if(message.startsWith("!leave"))
			{
				m_botAction.spec(name);
				m_botAction.spec(name);
			}
		} catch(Exception e) {}
    }

    public void load( String name, String extensionType ){
        if(locked && !extensionType.toLowerCase().equals("twrc") || !extensionType.toLowerCase().equals("trackmanager") || !extensionType.toLowerCase().equals("race")) {
            try{
                if( m_loader.shouldReload() ) m_loader.reinstantiate();
                extensionType = extensionType.toLowerCase();
                RBExtender extension = (RBExtender)m_loader.loadClass( "twcore.bots.racingbot.Rb" + extensionType ).newInstance();
                extension.set(m_botAction, m_botAction.getOperatorList(), this );
                extensions.put(extensionType, extension);
                m_botAction.sendPrivateMessage(name, extensionType + " loaded.");
            } catch( Exception e ){e.printStackTrace(); m_botAction.sendPrivateMessage(name, "Could not load module: " + extensionType);}
        }
        else {
            m_botAction.sendPrivateMessage( name, "Please !lock the bot first before loading a module." );
        }
    }

	public void remove( String name, String key ){
        if( modules.containsKey( key )){
            modules.remove(key);
            m_botAction.sendPrivateMessage( name, key
            + " Successfully Removed" );
        } else {
            m_botAction.sendPrivateMessage( name, key
            + " is not loaded, so it cannot be removed.  Keep in mind the "
            + "names are case sensitive." );
        }
    }

    public void handleList( String name, String message )
    {
        String[] s = botRoot.list();
        m_botAction.sendPrivateMessage( name, "I contain the following "
        + "modules:" );
        m_botAction.sendPrivateMessage( name, "A * indicates a loaded module" );
        for( int i = 0; i < s.length; i++ ){
            if( s[i].endsWith( ".class" )){
                s[i] = s[i].substring( 0, s[i].lastIndexOf( '.' ));
                if( s[i].startsWith( "Rb" ) && s[i].indexOf( '$' ) == -1 ){
                    String reply = s[i].substring( 2 );
                    if( modules.containsKey( reply ))
                        reply = reply + " *";
                    m_botAction.sendPrivateMessage(name, reply);
                }
            }
        }
    }

    public void handleEvent( ArenaJoined event ){

        distributeEvent( (SubspaceEvent)event );

        RbRace mod = new RbRace();
        mod.set( m_botAction, mySQLHost, this);
        modules.put( "Race", mod );

        RbTrackManager mod2 = new RbTrackManager();
        mod2.set( m_botAction, mySQLHost, this);
        modules.put( "Track Manager", mod2 );

        RbTWRC mod3 = new RbTWRC();
       	mod3.set(m_botAction, mySQLHost, this);
        modules.put("TWRC", mod3);

        m_botAction.setPlayerPositionUpdating( 250 );
    }

   	public void help( String name, String key ){
        key = key.toLowerCase();
        if( extensions.containsKey( key )){
            String[] helps = extensions.get( key ).getHelpMessages();
            m_botAction.privateMessageSpam( name, helps );
        } else {
            m_botAction.sendPrivateMessage( name, "Sorry, but the module " + key
            + " has not been loaded." );
        }
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
}