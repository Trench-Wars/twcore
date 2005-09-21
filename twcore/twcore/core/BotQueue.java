package twcore.core;
import java.util.*;
import java.io.*;

/**
 * Separate thread that holds a queue of bots to be spawned, spawns them when
 * ready, and maintains a reference to every spawned bot.  Handles bot removal
 * as necessary.  Also ensures that spawning limits for each type of bot are
 * enforced.
 */
public class BotQueue extends Thread {
    private Map         m_botTypes;       // Bot type -> number of bots of type
    private Map         m_botStable;      // Bot name -> instance of ChildBot
    private BotAction   m_botAction;      // Reference to utility class
    private List        m_spawnQueue;     // Queue of bots waiting to be spawned 
    private long        m_lastSpawnTime;  // Time of last spawn (in system ms)
    private ThreadGroup m_group;          // Thread group this bot belongs to
    private int   SPAWN_DELAY = 20000;    // Delay in ms between bot spawnings
    
    private AdaptiveClassLoader m_loader; // Instance of dynamic loader
    private Vector repository;            // Recursively built directory list
    private File directory;               // Root directory of TWCore
    
    /**
     * Initializes necessary data and recursively sets up a bot repository
     * for the AdaptiveClassLoader to load from.  Also sets the spawn delay
     * between bots to a low value if the bots are being spawned locally. 
     * @param group The thread grouping to which the queue belongs
     * @param botAction Reference to a BotAction instantiation
     */
    BotQueue( ThreadGroup group, BotAction botAction ){
        super( group, "BotQueue" );
        repository = new Vector();
        m_group = group;
        m_lastSpawnTime = 0;
        m_botAction = botAction;
        directory = new File( m_botAction.getCoreData().getGeneralSettings().getString( "Core Location" ));

        if (m_botAction.getCoreData().getGeneralSettings().getString( "Server" ).equals("localhost"))
            SPAWN_DELAY = 100;
		
        resetRepository();
        
        m_botTypes = Collections.synchronizedMap( new HashMap() );
        m_botStable = Collections.synchronizedMap( new HashMap() );
        m_spawnQueue = Collections.synchronizedList( new LinkedList() );
        
        m_botTypes.put( "hubbot", new Integer( 1 ));
        if( repository.size() == 0 ){
            Tools.printLog( "There are no bots to load.  Did you set the Core Location parameter in setup.cfg improperly?" );
        } else {
            System.out.println( "Looking through " + directory.getAbsolutePath()
            + " for bots.  " + (repository.size() - 1) + " child directories." );
        }
        m_loader = new AdaptiveClassLoader( repository, getClass().getClassLoader() );
    }
    
    /**
     * Clears the current repository and adds the core location and all
     * subdirectories to the current bot repository.
     */
    public void resetRepository(){
        repository.clear();
        addDirectories( directory );
    }
    
    /**
     * Returns the total number of bots of a given class.
     * @param className Name of the class of bots
     * @return Number of bots of the class
     */
    int getNumberOfBots( String className ){
        Integer         number = (Integer)m_botTypes.get( className );
        
        if( number == null ){
            return 0;
        } else {
            return number.intValue();
        }
    }
    
    /**
     * Displays the contents of the current bot spawning queue in PM to the
     * specified player.
     * @param messager Name of the player who will receive the message 
     */
    void listWaitingList( String messager ){
        int          i;
        ChildBot     bot;
        ListIterator iterator;
        
        m_botAction.sendSmartPrivateMessage( messager, "Waiting list:" );
        for( iterator=m_spawnQueue.listIterator(), i=1; iterator.hasNext(); i++ ){
            bot = (ChildBot)iterator.next();
            m_botAction.sendSmartPrivateMessage( messager, i + ": " + bot.getBot().getBotName() + ", created by " + bot.getCreator() + "." );
        }
        m_botAction.sendSmartPrivateMessage( messager, "End of list" );
    }
    
    /**
     * Lists the number of bots of each currently spawned type to the specified
     * player in PM.  If for some reason the count is 0 or less, the result is
     * not displayed.
     * @param messager Name of the player who will receive the message 
     */
    void listBotTypes( String messager ){
        Iterator        i;
        Integer         number;
        String          className;
        
        m_botAction.sendSmartPrivateMessage( messager, "Listing bot types:" );
        for( i = m_botTypes.keySet().iterator(); i.hasNext(); ){
            className = (String)i.next();
            number = (Integer)m_botTypes.get( className );
            if( number.intValue() > 0 )                
                m_botAction.sendSmartPrivateMessage( messager, className + ": " + number );
        }
        m_botAction.sendSmartPrivateMessage( messager, "End of list" );
    }
    
    /**
     * Lists the login names of all bots of a particular class name to the
     * specified player in PM.
     * @param className Class of bots in question
     * @param messager Name of the player who will receive the message 
     */
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
    
    /**
     * Increments the count of a particular class of bots with a specified value.
     * @param className Class of bots to increment the count of
     * @param valueToAdd Amount to add
     */
    void addToBotCount( String className, int valueToAdd ){
        Integer      newBotCount;
        Integer      oldBotCount;
        
        oldBotCount = (Integer)m_botTypes.get( className );
        if( oldBotCount != null ){
            if( oldBotCount.intValue() + valueToAdd >= 0 ) {
                newBotCount = new Integer( oldBotCount.intValue() + valueToAdd );
            	m_botTypes.put( className, newBotCount );
            }
        }
    }
    
    /**
     * Returns the next free bot number of a class of bots, given the bot's
     * settings.
     * @param botInfo Settings for the particular bot type
     * @return Next free bot number of this class of bots
     */
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
    
    /**
     * Given the login name of a bot, removes it from the system.
     * @param name Login name of bot to remove
     * @return True if removal succeeded
     */
    boolean removeBot( String name ){

        ChildBot deadBot = (ChildBot)m_botStable.remove( name );
        if( deadBot != null ){
            Session deadSesh = deadBot.getBot();
            if( deadSesh != null ) {
                deadSesh.getBotAction().cancelTasks();
                deadSesh.disconnect();
            }
            // Decrement count for this type of bot
            addToBotCount( deadBot.getClassName(), (-1) );
            deadBot = null;
            System.gc();
            return true;
        }
        return false;
    }
    
    /**
     * Removes all bots of a given type.  For debug purposes, or the impatient.
     * @param className Class of bots to remove
     */
    void hardRemoveAllBotsOfType( String className ) {
        String       rawClassName = className.toLowerCase();
        ChildBot c;
        
        Iterator i = m_botStable.values().iterator();
        while( i.hasNext() ) {
            c = (ChildBot)i.next();
            if( c != null ) { 
                if( c.getClassName() == rawClassName ) {
                    removeBot( c.getBot().getName() );
                }
            }
        }
        if( ((Integer)m_botTypes.get( rawClassName )).intValue() != 0 )
            m_botTypes.put( rawClassName, new Integer(0) );
    }
    
    /**
     * Spawns a bot into existence based on a given class name.  In order to
     * be spawned, the class must exist, the CFG must exist and be properly
     * formed, and the maximum number of bots of the type allowed must be
     * less than the number currently active.
     * @param className Class name of bot to spawn
     * @param messager Name of player trying to spawn the bot
     */
    void spawnBot( String className, String messager ){
        CoreData cdata = m_botAction.getCoreData();
        long         currentTime;
        
        String       rawClassName = className.toLowerCase();
        BotSettings  botInfo = cdata.getBotConfig( rawClassName );

        if( botInfo == null ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn bot of type " + className + ".  Invalid bot type or missing CFG file." );
            m_botAction.sendSmartPrivateMessage( messager, "That bot type does not exist, or the CFG file for it is missing." );
            return;
        }

        Integer      maxBots = botInfo.getInteger( "Max Bots" );
        Integer      currentBotCount = (Integer)m_botTypes.get( rawClassName );
        
        if( maxBots == null ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn bot of type " + className + ".  Invalid settings file. (MaxBots improperly defined)" );
            m_botAction.sendSmartPrivateMessage( messager, "The CFG file for that bot type is invalid. (MaxBots improperly defined)" );
            return;
        }
        
        if( maxBots.intValue() == 0 ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn bot of type " + className + ".  Spawning for this type is disabled on this hub." );
            m_botAction.sendSmartPrivateMessage( messager, "Bots of this type are currently disabled on this hub.  If you are running another hub, please try from it instead." );
            return;
        }
        
        if( currentBotCount == null ){
            currentBotCount = new Integer( 0 );
            m_botTypes.put( rawClassName, currentBotCount );
        }
        
        if( currentBotCount.intValue() >= maxBots.intValue() ){
            m_botAction.sendChatMessage( 1, messager + " tried to spawn a new bot of type " + className + ".  Maximum number already reached (" + maxBots + ")" );
            m_botAction.sendSmartPrivateMessage( messager, "Maximum number of bots of this type (" + maxBots + ") has been reached." );
            return;
        }
        
        currentBotCount = new Integer( getFreeBotNumber( botInfo ) );
        String botName = botInfo.getString( "Name" + currentBotCount );
        String botPassword = botInfo.getString( "Password" + currentBotCount );
                
        Session childBot = null;
        try{
            if( m_loader.shouldReload() ){
                System.out.println( "Reinstantiating" );
                resetRepository();
                m_loader = m_loader.reinstantiate();
            }
            
            Class roboClass = m_loader.loadClass( "twcore.bots." + rawClassName + "." + rawClassName );
            childBot = new Session( cdata, roboClass, botName, botPassword, currentBotCount.intValue(), m_group );
        } catch( ClassNotFoundException cnfe ){
            Tools.printLog( "Class not found: " + rawClassName + ".class.  Reinstall this bot?" );
            m_botAction.sendSmartPrivateMessage( messager, "The class file does not exist.  Cannot start bot." );
            return;
        }
                
        currentTime = System.currentTimeMillis();
        if( m_lastSpawnTime + SPAWN_DELAY > currentTime ){
            m_botAction.sendSmartPrivateMessage( messager, "Subspace only allows a certain amount of logins in a short time frame.  Please be patient while your bot waits to be spawned." );
            if( m_spawnQueue.isEmpty() == false ){
                int size = m_spawnQueue.size();
                if( size > 1 ){
                    m_botAction.sendSmartPrivateMessage( messager, "There are currently " + m_spawnQueue.size() + " bots in front of yours." );
                } else {
                    m_botAction.sendSmartPrivateMessage( messager, "There is only one bot in front of yours." );
                }
            } else {
                m_botAction.sendSmartPrivateMessage( messager, "You are the only person waiting in line.  Your bot will log in shortly." );
            }
            m_botAction.sendChatMessage( 1, messager + " in queue to spawn bot of type " + className );
        }
        
        ChildBot newChildBot = new ChildBot( className, messager, childBot );
        addToBotCount( rawClassName, 1 );
        m_botStable.put( botName, newChildBot );
        m_spawnQueue.add( newChildBot );
    }
    
    /**
     * Queue thread execution loop.  Attempts to spawn the next bot on
     * the waiting list, if the proper delay time has been reached. 
     */
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
                if( m_spawnQueue.isEmpty() == false ){
                    if( m_lastSpawnTime + SPAWN_DELAY < currentTime ){
                        childBot = (ChildBot)m_spawnQueue.remove( 0 );
                        bot = childBot.getBot();
                        
                        bot.start();
                        
                        while( bot.getBotState() == Session.STARTING ){
                            Thread.sleep( 5 );
                        }
                        
                        if( bot.getBotState() == Session.NOT_RUNNING ){
                            removeBot( bot.getBotName() );
                            m_botAction.sendSmartPrivateMessage( childBot.getCreator(), "Bot failed to log in.  Verify login and password are correct." );
                            m_botAction.sendChatMessage( 1, "Bot of type " + childBot.getClassName() + " failed to log in.  Verify login and password are correct."  );
                        } else {
                            m_botAction.sendSmartPrivateMessage( childBot.getCreator(), "Your new bot is named " + bot.getBotName() + "." );
                            m_botAction.sendChatMessage( 1, childBot.getCreator() + " spawned " + childBot.getBot().getBotName() + " of type " + childBot.getClassName() );
                        }
                        
                        m_lastSpawnTime = currentTime;
                    }
                }
                
                // Removes bots that are no longer running.
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
    
    /**
     * Recursively adds all subdirectories of a given base directory to the
     * repository, and adds the base directory itself.
     * @param base Directory from which to start
     */
    public void addDirectories( File base ){
        if( base.isDirectory() ){
            repository.add( base );
            File[] files = base.listFiles();
            for( int i = 0; i < files.length; i++ ){
                addDirectories( files[i] );
            }
        }
    }
    
    /**
     * Internal class abstraction used to store information about bots that have
     * been spawned from a HubBot.
     * 
     * NOTE: Was previously its own class, but was made an internal class.
     */
    public class ChildBot {
        private Session      m_bot;         // Bot's Session object reference
        private String       m_creator;     // Bot's creator (Autoloader / player name)
        private String       m_className;   // Name of the bot's class
        
        /**
         * Create a new instance of ChildBot.
         * @param className Name of the bot's class
         * @param creator Name of the bot's creator
         * @param bot The Session object reference of this bot 
         */
        ChildBot( String className, String creator, Session bot ){
            m_bot = bot;
            m_creator = creator;
            m_className = className;
        }
        
        /**
         * @return Bot's class name (used to spawn the bot)
         */
        public String getClassName(){
            return m_className;
        }
        
        /**
         * @return Name of the bot's creator
         */
        public String getCreator(){
            return m_creator;
        }
        
        /**
         * @return Reference to the bot's Session
         */
        public Session getBot(){
            return m_bot;
        }
    }
}
