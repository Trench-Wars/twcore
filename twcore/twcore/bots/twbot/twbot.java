package twcore.bots.twbot;

import java.util.*;
import java.io.*;
import twcore.core.*;

public class twbot extends SubspaceBot {
    private BotAction m_botAction;
    private TimerTask checkTime;
    private int lastUse = 0;
    private HashMap extensions;
    private OperatorList m_opList;
    private BotSettings  m_botSettings;
    private boolean locked = false;
    private String currentArena = "#robopark";
    private int idleTime;
    private String defaultArena;
    private String nameOfHost;
    private AdaptiveClassLoader m_loader;
    private File botRoot;
    private File coreRoot;
    /** Creates a new instance of newportabot */
    public twbot(BotAction botAction){
        super( botAction );
        Vector repository = new Vector();
        coreRoot = new File( botAction.getGeneralSettings().getString( "Core Location" ) );
        botRoot = new File(coreRoot.getPath() + "/twcore/bots/twbot");
        repository.add( coreRoot );
        m_loader = new AdaptiveClassLoader( repository, getClass().getClassLoader() );

        m_botAction = botAction;
        
        m_botSettings = m_botAction.getBotSettings();
        extensions = new HashMap();

        idleTime = m_botSettings.getInt("IdleReturnTime");
        defaultArena = m_botSettings.getString("InitialArena");

        //Checks for an idle unlocked bot.
        checkTime = new TimerTask() {
            public void run() {
                lastUse += 30;
                if( !locked && !currentArena.equals( defaultArena )) {
                    if( lastUse > idleTime )
                        actualGo( defaultArena );
                }
                else lastUse = 0;
            }
        };
        m_botAction.scheduleTaskAtFixedRate( checkTime, 0, 30000 );
        m_botAction.getEventRequester().requestAll();
        
    }

    public void handleEvent( ArenaJoined event ){
        m_botAction.setReliableKills( 1 );
    }

    public void load( String name, String extensionType ){
        if(locked) {
            try{
                if( m_loader.shouldReload() ) m_loader.reinstantiate();
                extensionType = extensionType.toLowerCase();
                TWBotExtension extension = (TWBotExtension)m_loader.loadClass( "twcore.bots.twbot.twbot" + extensionType ).newInstance();
                extension.set( m_botAction, m_opList, this );
                extensions.put( extensionType, extension );
                m_botAction.sendPrivateMessage( name, "Successfully loaded " + extensionType );
            } catch( Exception e ){
                m_botAction.sendPrivateMessage( name, "Failed to load "
                + extensionType );
            }
        }
        else {
            m_botAction.sendPrivateMessage( name, "Please !lock the bot first before loading a module." );
        }
    }

    public void distributeEvent( SubspaceEvent event ){
        Iterator i = extensions.entrySet().iterator();
        while( i.hasNext() ){
            Map.Entry entry = (Map.Entry)i.next();
            TWBotExtension ext = (TWBotExtension)entry.getValue();
            ext.handleEvent( event );
        }
    }
    public void handleWhatsNew( String name ){
        try{
            BufferedReader in = new BufferedReader( new FileReader( "whatsnew.txt" ));
            String line = "";
            do{
                line = in.readLine();
                if( line == null ) break;
                m_botAction.sendPrivateMessage( name, line );
            } while( line != null );
            in.close();
        } catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Error reading whatsnew.txt file, try again later." );
            Tools.printStackTrace( "Error reading whatsnew.txt file", e );
        }
    }
    public void handleList( String name, String message ){
        String[] s = botRoot.list();
        m_botAction.sendPrivateMessage( name, "I contain the following "
        + "modules:" );
        m_botAction.sendPrivateMessage( name, "A * indicates a loaded module" );
        for( int i = 0; i < s.length; i++ ){
            if( s[i].endsWith( ".class" )){
                s[i] = s[i].substring( 0, s[i].lastIndexOf( '.' ));
                if( s[i].startsWith( "twbot" ) && !s[i].equals( "twbot" )
                && s[i].indexOf( '$' ) == -1 ){
                    String reply = s[i].substring( 5 );
                    if( extensions.containsKey( reply ))
                        reply = reply + " *";
                    m_botAction.sendPrivateMessage( name, reply );
                }
            }
        }
    }

    public void remove( String name, String key ){
        if( extensions.containsKey( key )){
            ((TWBotExtension)extensions.remove( key )).cancel();
            m_botAction.sendPrivateMessage( name, key
            + " Successfully Removed" );
        } else {
            m_botAction.sendPrivateMessage( name, key
            + " is not loaded, so it cannot be removed.  Keep in mind the "
            + "names are case sensitive." );
        }
    }

    public void listLoaded( String name ){
        if( extensions.size() == 0 ){
            m_botAction.sendPrivateMessage( name, "There are no loaded modules." );
        } else {
            m_botAction.sendPrivateMessage( name, "Loaded modules are:" );
            Iterator i = extensions.keySet().iterator();
            while( i.hasNext() ){
                m_botAction.sendPrivateMessage( name, (String)i.next() );
            }
        }
    }

    public void help( String name, String key ){
        key = key.toLowerCase();
        if( extensions.containsKey( key )){
            String[] helps = ((TWBotExtension)extensions.get( key )).getHelpMessages();
            m_botAction.privateMessageSpam( name, helps );
        } else {
            m_botAction.sendPrivateMessage( name, "Sorry, but the module " + key
            + " has not been loaded." );
        }
    }

    public void allhelp( String name ){
        m_botAction.privateMessageSpam( name, helps );
        Iterator i = extensions.keySet().iterator();
        while( i.hasNext() ){
            String key = (String)i.next();
            TWBotExtension ext = (TWBotExtension)extensions.get( key );
            m_botAction.sendPrivateMessage( name, key + " module contains:" );
            m_botAction.privateMessageSpam( name, ext.getHelpMessages() );
        }
    }

    public void handleGo( String name, String arena ){
        if( !locked && !Tools.isAllDigits( arena )){
            actualGo( arena );
        } else {
            m_botAction.sendSmartPrivateMessage( name, "Sorry, but I am currently "
            + "locked.  Please !unlock me first." );
        }
    }

    private void actualGo( String arena ){
        clear();
        currentArena = arena;
        m_botAction.changeArena( arena );
    }

    private void clear(){
        Iterator i = extensions.values().iterator();
        while( i.hasNext() ){
            ((TWBotExtension)i.next()).cancel();
        }
        extensions.clear();
        //twbotstandard std = new twbotstandard();
        //std.set( m_botAction, m_opList, this );
        //extensions.put( "standard", std );
    }

    public void handleEvent( Message event ){
        distributeEvent( (SubspaceEvent)event );
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( message.startsWith( "!host" )){
                if( nameOfHost == null )
                    m_botAction.sendPrivateMessage( name, "There is no registered host, or I am not in use currently." );
                else
                    m_botAction.sendPrivateMessage( name, "Your current host is: " + nameOfHost );
            }
            if( !m_opList.isER( name )) return;
            lastUse = 0;
            handleCommand( name, message );
        }else if( event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE ){
            String rpmname = event.getMessager();
            String rpmmessage = event.getMessage();
            if( message.startsWith( "!games" )){
                if( locked ){
                    if( currentArena.startsWith( "#" )){
                        m_botAction.sendSmartPrivateMessage( rpmname,
                        "In use in a private arena." );
                    } else {
                        m_botAction.sendSmartPrivateMessage( rpmname,
                        nameOfHost + " is hosting " + currentArena + "." );
                    }
                } else {
                    if( currentArena.startsWith( "#robopark" )){
                        m_botAction.sendSmartPrivateMessage( rpmname,
                        "Not in use." );
                    } else if( currentArena.startsWith( "#" )){
                        m_botAction.sendSmartPrivateMessage( rpmname,
                        "Idle in a private arena." );
                    } else {
                        m_botAction.sendSmartPrivateMessage( rpmname,
                        "Idle in " + currentArena );
                    }
                }
            }
            if( !m_opList.isER( rpmname )) return;
            lastUse = 0;
            if( message.startsWith( "!go " )){
                handleGo( rpmname, rpmmessage.substring( 4 ));
            } else if( message.startsWith( "!come " )){
                handleGo( rpmname, rpmmessage.substring( 6 ));
            } else if( message.startsWith( "!where" )){
                if( locked ) m_botAction.sendSmartPrivateMessage( rpmname,
                "In use in " + currentArena + " by " + nameOfHost + ".");
                else m_botAction.sendSmartPrivateMessage( rpmname, "In "
                + currentArena );
            }

        }
    }

    public void handleCommand( String name, String message ){
        if( message.startsWith( "!load " )){
            load( name, message.substring( 6 ));
        } else if( message.startsWith( "!modules" )){
            handleList( name, message );
        } else if( message.startsWith( "!unload " )){
            remove( name, message.substring( 8 ));
        } else if( message.startsWith( "!loaded" )){
            listLoaded( name );
        } else if( message.startsWith( "!off" )){
            clear();
            m_botAction.sendPrivateMessage( name, "TWBot Reset" );
        } else if( message.startsWith( "!help " )){
            help( name, message.substring( 6 ));
        } else if( message.startsWith( "!help" )){
            m_botAction.privateMessageSpam( name, helps );
        } else if( message.startsWith( "!allhelp" )){
            allhelp( name );
        } else if( message.startsWith( "!go " )){
            handleGo( name, message.substring( 4 ));
        } else if( message.startsWith( "!come " )){
            handleGo( name, message.substring( 6 ));
        } else if( message.startsWith( "!home" )){
            if( currentArena.equals( defaultArena ) && !locked ){
                m_botAction.sendPrivateMessage( name, "I'm already home." );
            } else if( currentArena.equals( defaultArena ) && locked ){
                m_botAction.sendPrivateMessage( name, "Unlocked.  I'm already home, though." );
            } else {
                m_botAction.sendPrivateMessage( name, "Seeya! It's quittin' time!" );
                nameOfHost = null;
                locked = false;
                handleGo( name, defaultArena );
            }
        } else if( message.startsWith( "!mybot" )){
            if( locked ){
                if( name.equals( nameOfHost )){
                    m_botAction.sendPrivateMessage( name, "You already own this bot." );
                    return;
                }
                m_botAction.sendPrivateMessage( nameOfHost, "This bot now belongs to " + name );
                m_botAction.sendPrivateMessage( name, "I'm yours.  Ownership transferred from " + nameOfHost );
                nameOfHost = name;
            } else {
                m_botAction.sendPrivateMessage( name, "This bot is unowned, because the bot is unlocked." );
            }
        } else if( message.startsWith( "!lock" )){
            if( locked ){
                m_botAction.sendPrivateMessage( name, "I'm already locked.  If you want to unlock me, use !unlock." );
                return;
            }
            twbotstandard std = new twbotstandard();
            std.set( m_botAction, m_opList, this );
            extensions.put( "standard", std );
            twbotwarp warp = new twbotwarp();
            warp.set(m_botAction, m_opList, this);
            extensions.put("warp", warp);
            twbotspec spec = new twbotspec();
            spec.set(m_botAction, m_opList, this);
            extensions.put("spec", spec);
            nameOfHost = name;
            m_botAction.sendPrivateMessage( name, "Locked.  Standard, Warp, and Spec modules loaded." );
            locked = true;
        } else if( message.startsWith( "!unlock" )){
            if( !locked ){
                m_botAction.sendPrivateMessage( name, "I'm already unlocked." );
                return;
            }
            m_botAction.sendPrivateMessage( name, "Unlocked.  You may now move me with !go and !come or disconnect me with !die" );
            nameOfHost = null;
            locked = false;
        } else if( message.startsWith( "!whatsnew" )){
            handleWhatsNew( name );
        } else if( message.startsWith( "!die" )){
            if( !locked ){
                m_botAction.sendSmartPrivateMessage( name, "Goodbye!" );
                m_botAction.die();
            } else {
                m_botAction.sendPrivateMessage( name, "I am locked, sorry." );
            }
        }
    }

    public void handleUnlock( String name ){
    }
    public void handleEvent( LoggedOn event ){
        distributeEvent( (SubspaceEvent)event );
        m_botAction.joinArena( currentArena );
        m_botAction.sendUnfilteredPublicMessage( "?chat=robodev" );
        m_opList = m_botAction.getOperatorList();
    }

    public void handleEvent( PlayerLeft event ){
        distributeEvent( (SubspaceEvent)event );}
    public void handleEvent( SubspaceEvent event ){
        distributeEvent( (SubspaceEvent)event );}
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


    static final String[] helps = {
        "The following commands are the base TWBot:",
        "!help <module>     - Lists the !help for a module",
        "!modules           - Lists the module types available",
        "!load <module>     - Loads a module",
        "!unload <module>   - Removes a loaded module",
        "!loaded            - Lists the modules that are currently loaded",
        "!off               - Cleans up the bot and shuts down all modules",
        "!lock              - Locks bot so it can't be moved.",
        "!unlock            - Unlocks bot so it can be moved.",
        "!come, !go <arena> - Tells the bot to come to an arena",
        "!die               - Tells the bot to take a hike... off a cliff.",
        "!home              - Tells the bot to unlock and go home",
        "!mybot             - Lets everyone know you are hosting instead of the former host."
    };
}
