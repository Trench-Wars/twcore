package twcore.core;
import java.util.*;
import java.io.*;

public class BotQueue extends Thread {
    private Map         m_botTypes;
    private Map         m_botStable;
    private BotAction   m_botAction;
    private List        m_waitingRoom;
    private long        m_lastSpawnTime;
    private ThreadGroup m_group;
    private final int   SPAWN_DELAY = 20000;
    
    private AdaptiveClassLoader m_loader;
    private Vector repository;
    private File directory;
    BotQueue( ThreadGroup group, BotAction botAction ){
        super( group, "BotQueue" );
        repository = new Vector();
        m_group = group;
        m_lastSpawnTime = 0;
        m_botAction = botAction;
        directory = new File( m_botAction.getCoreData().getGeneralSettings().getString( "Core Location" ));
        
        resetRepository();
        
        m_botTypes = Collections.synchronizedMap( new HashMap() );
        m_botStable = Collections.synchronizedMap( new HashMap() );
        m_waitingRoom = Collections.synchronizedList( new LinkedList() );
        
        m_botTypes.put( "hubbot", new Integer( 1 ));
        if( repository.size() == 0 ){
            Tools.printLog( "There are no bots to load.  Did you set the Core Location parameter in setup.cfg improperly?" );
        } else {
            System.out.println( "Looking through " + directory.getAbsolutePath()
            + " for bots.  " + (repository.size() - 1) + " child directories." );
        }
        m_loader = new AdaptiveClassLoader( repository, getClass().getClassLoader() );
    }
    
    public void resetRepository(){
        repository.clear();
        addDirectories( directory );
    }
    
    int getNumberOfBots( String className ){
        Integer         number = (Integer)m_botTypes.get( className );
        
        if( number == null ){
            return 0;
        } else {
            return number.intValue();
        }
    }
    
    void listWaitingList( String messager ){
        int          i;
        ChildBot     bot;
        ListIterator iterator;
        
        m_botAction.sendSmartPrivateMessage( messager, "Waiting list:" );
        for( iterator=m_waitingRoom.listIterator(), i=1; iterator.hasNext(); i++ ){
            bot = (ChildBot)iterator.next();
            m_botAction.sendSmartPrivateMessage( messager, i + ": " + bot.getBot().getBotName() + ", created by " + bot.getCreator() + "." );
        }
        m_botAction.sendSmartPrivateMessage( messager, "End of list" );
    }
    
    void listBotTypes( String messager ){
        Iterator        i;
        Integer         number;
        String          className;
        
        m_botAction.sendSmartPrivateMessage( messager, "Listing bot types:" );
        for( i = m_botTypes.keySet().iterator(); i.hasNext(); ){
            className = (String)i.next();
            number = (Integer)m_botTypes.get( className );
            m_botAction.sendSmartPrivateMessage( messager, className + ": " + number );
        }
        m_botAction.sendSmartPrivateMessage( messager, "End of list" );
    }
    
    void listBots( String className, String messager ){
        Iterator     i;
        ChildBot     bot;
        String       rawClassName = className.toLowerCase();
        
        if( rawClassName.compareTo( "hubbot" ) == 0 ){
            m_botAction.sendSmartPrivateMessage( messager, "There is only one true master, and that is me." );
        } else if( getNumberOfBots( className ) == 0 ){
            m_botAction.sendSmartPrivateMessage( messager, "No bots of that type." );
        } else {
            m_botAction.sendSmartPrivateMessage( messager, className + ":" );
            for( i = m_botStable.values().iterator(); i.hasNext(); ){
                bot = (ChildBot)i.next();
                if( bot.getClassName().compareTo( className ) == 0 ){
                    m_botAction.sendSmartPrivateMessage( messager, bot.getBot().getBotName() + ", created by " + bot.getCreator() );
                }
            }
            
            m_botAction.sendSmartPrivateMessage( messager, "End of list" );
        }
    }
    
    void addToBotCount( String className, int valueToAdd ){
        Integer      newBotCount;
        Integer      oldBotCount;
        
        oldBotCount = (Integer)m_botTypes.get( className );
        if( oldBotCount != null ){
            newBotCount = new Integer( oldBotCount.intValue() + valueToAdd );
            m_botTypes.put( className, newBotCount );
        }
    }
    
    int getFreeBotNumber( BotSettings botInfo ){
        int          i;
        String       name;
        int          maxBots;
        int          result = -1;
        
        maxBots = botInfo.getInt( "Max Bots" );
        for( i=1; i<=maxBots && result == -1; i++ ){
            name = botInfo.getString( "Name" + i );
            if( m_botStable.get( name ) == null ){
                result = i;
            }
        }
        
        return result;
    }
    
    void removeBot( String name ){

        ChildBot deadBot = (ChildBot)m_botStable.remove( name );
        if( deadBot != null ){
            deadBot.getBot().getBotAction().cancelTasks();
            addToBotCount( deadBot.getClassName(), (-1) );
            deadBot.getBot().disconnect();
            deadBot = null;
            System.gc();
        }
    }
    
    void spawnBot( String className, String messager ){
        CoreData cdata = m_botAction.getCoreData();
        long         currentTime;
        
        String       rawClassName = className.toLowerCase();
        BotSettings  generalSettings = m_botAction.getGeneralSettings();
        BotSettings  botInfo = cdata.getBotConfig( rawClassName );

        if( botInfo == null ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn a new bot of type " + className + ".  Missing or invalid settings file." );
            m_botAction.sendSmartPrivateMessage( messager, "That bot type does not exist." );
            return;
        }

        int          maxBots = botInfo.getInt( "Max Bots" );
        Integer      currentBotCount = (Integer)m_botTypes.get( rawClassName );
        
        if( maxBots == 0 ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn a new bot of type " + className + ".  Missing or invalid settings file." );
            m_botAction.sendSmartPrivateMessage( messager, "That bot type does not exist." );
            return;
        }
        
        if( currentBotCount == null ){
            currentBotCount = new Integer( 0 );
            m_botTypes.put( className, currentBotCount );
        }
        
        if( currentBotCount.intValue() >= maxBots ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn a new bot of type " + className + ".  Maximum number already reached" );
            m_botAction.sendSmartPrivateMessage( messager, "Maximum number of bots of this type has been reached." );
            return;
        }
        
        currentBotCount = new Integer( getFreeBotNumber( botInfo ) );
        String botName = botInfo.getString( "Name" + currentBotCount );
        String botPassword = botInfo.getString( "Password" + currentBotCount );
        
        //public Session( CoreData cdata, Class roboClass, String name,
        //String password, ThreadGroup parentGroup ){
        
        Session childBot = null;
        try{
            if( m_loader.shouldReload() ){
                System.out.println( "Reinstantiating" );
                resetRepository();
                m_loader = m_loader.reinstantiate();
            }
            
            Class roboClass = m_loader.loadClass( "twcore.bots." + rawClassName + "." + rawClassName );
            childBot = new Session( cdata, roboClass, botName, botPassword, m_group );
        } catch( ClassNotFoundException cnfe ){
            Tools.printLog( "Class not found: " + rawClassName + ".class.  Reinstall this bot?" );
            m_botAction.sendSmartPrivateMessage( messager, "The class file does not exist.  Cannot start bot." );
            return;
        }
        
        addToBotCount( rawClassName, 1 );
        
        currentTime = System.currentTimeMillis();
        if( m_lastSpawnTime + SPAWN_DELAY > currentTime ){
            m_botAction.sendSmartPrivateMessage( messager, "Subspace only allows a certain amount of logins in a short time frame.  Please be patient while your bot waits his turn in line." );
            if( m_waitingRoom.isEmpty() == false ){
                int size = m_waitingRoom.size();
                if( size > 1 ){
                    m_botAction.sendSmartPrivateMessage( messager, "There are currently " + m_waitingRoom.size() + " bots in front of yours." );
                } else {
                    m_botAction.sendSmartPrivateMessage( messager, "There is only one bot in front of yours." );
                }
            } else {
                m_botAction.sendSmartPrivateMessage( messager, "You are the only person waiting in line.  Your bot will log in shortly." );
            }
            m_botAction.sendChatMessage( 1, messager + " is waiting in line to spawn a bot of type " + className );
        }
        
        ChildBot newChildBot = new ChildBot( className, messager, childBot );
        m_botStable.put( botName, newChildBot );
        m_waitingRoom.add( newChildBot );
    }
    
    public void run(){
        Iterator        i;
        String          key;
        Session         bot;
        ChildBot        childBot;
        long            currentTime = 0;
        long            lastStateDetection = 0;
        int             SQLStatusTime = 0;
        final int       DETECTION_TIME = 5000;
        
        while( m_botAction.getBotState() != Session.NOT_RUNNING ){
            if( SQLStatusTime == 2400 ){
                SQLStatusTime = 0;
                m_botAction.getCoreData().getSQLManager().status();
            }
            try {
                currentTime = System.currentTimeMillis() + 1000;
                if( m_waitingRoom.isEmpty() == false ){
                    if( m_lastSpawnTime + SPAWN_DELAY < currentTime ){
                        childBot = (ChildBot)m_waitingRoom.remove( 0 );
                        bot = childBot.getBot();
                        
                        bot.start();
                        
                        while( bot.getBotState() == Session.STARTING ){
                            Thread.sleep( 5 );
                        }
                        
                        if( bot.getBotState() == Session.NOT_RUNNING ){
                            removeBot( bot.getBotName() );
                            m_botAction.sendSmartPrivateMessage( childBot.getCreator(), "Bot failed to log in." );
                        } else {
                            m_botAction.sendSmartPrivateMessage( childBot.getCreator(), "Your new bot is named " + bot.getBotName() + "." );
                            m_botAction.sendChatMessage( 1, childBot.getCreator() + " spawned " + childBot.getBot().getBotName() + " of type " + childBot.getClassName() );
                        }
                        
                        m_lastSpawnTime = currentTime;
                    }
                }
                
                if( lastStateDetection + DETECTION_TIME < currentTime ){
                    for( i = m_botStable.keySet().iterator(); i.hasNext(); ){
                        key = (String)i.next();
                        childBot = (ChildBot)m_botStable.get( key );
                        if( childBot.getBot().getBotState() == Session.NOT_RUNNING ){
                            removeBot( key );
                            m_botAction.sendChatMessage( 1, key + " has disconnected." );
                            childBot = null;
                        }
                    }
                    
                    lastStateDetection = currentTime;
                }
                SQLStatusTime++;
                Thread.sleep( 1000 );
            } catch( ConcurrentModificationException e ){
                //m_botAction.sendChatMessage( 1, "Concurrent modification.  No state detection done this time" );
            } catch( Exception e ){
                Tools.printStackTrace( e );
            }
        }
    }
    
    public void addDirectories( File base ){
        if( base.isDirectory() ){
            repository.add( base );
            File[] files = base.listFiles();
            for( int i = 0; i < files.length; i++ ){
                addDirectories( files[i] );
            }
        }
    }
}