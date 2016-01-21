package twcore.core;

import java.io.File;

import twcore.core.sql.SQLManager;
import twcore.core.util.InterProcessCommunicator;
import twcore.core.util.SocketCommunicator;
import twcore.core.util.Tools;

/**
    Reference object for all important data shared between bots.
    <p>
    Information stored in CoreData:
    <code><pre>
     InterProcessCommunicator - For the synchronization of message passing
                                between bots.
     SQLManager               - For handling all SQL queries and connection pools.
     OperatorList             - For setting and verifying access levels.
     BotSettings              - For storing and accessing the general settings
                                (core location, login info, exception log, etc.)
    </pre></code>
*/
public class CoreData {

    private InterProcessCommunicator    m_comm;              // Command handler
    private SocketCommunicator          m_socketComm;        // Socket Command handler
    private SQLManager                  m_manager;           // SQL system manager
    private OperatorList                m_accessList;        // Access list
    private BotSettings                 m_generalSettings;   // CFG-loaded settings
    private int                         m_lastIP = 0x050000; // For local IP conversion

    /**
        Load bot settings, SQL settings, and instantiate all member classes.
    */
    public CoreData( File setupFile ) {
        m_generalSettings = new BotSettings( setupFile );
        m_comm = new InterProcessCommunicator();
        m_socketComm = new SocketCommunicator(m_generalSettings.getInt( "SocketPort" ));
        m_accessList = OperatorList.getInstance();
        File sqlFile = new File( m_generalSettings.getString( "Core Location" ) + "/corecfg/sql.cfg" );
        m_manager = new SQLManager( sqlFile );
    }

    /**
        Lite version of the CoreData
        Load bot settings, SQL settings, and instantiate all member classes.
    */
    public CoreData( String host, int port ) {

        m_generalSettings = new BotSettings();
        m_generalSettings.put("Port", port);
        m_generalSettings.put("Server", host);
        m_generalSettings.put("LocalIP", "0");
        m_generalSettings.put("Sysop Password", "");
        m_generalSettings.put("Core Location", System.getProperty("user.dir"));

        m_comm = new InterProcessCommunicator();
        m_accessList = OperatorList.getInstance();

    }

    /**
        @return Port of the server the core is connected to
    */
    public int getServerPort() {
        return m_generalSettings.getInt( "Port" );
    }

    /**
        Returns the name (address) of the server the core is connected to.
        If connected to localhost, also convert to an IP rather than localhost.
        @return Name (address) of the server the core is connected to
    */
    public String getServerName() {
        String name = m_generalSettings.getString( "Server" );

        if( name.equalsIgnoreCase( "localhost" )) {
            name = "127." + ( m_lastIP >> 16 ) + "." + (( m_lastIP & 0x00FF00 ) >> 8 ) + "." + ( m_lastIP & 0x0000FF );
            m_lastIP++;
        }

        return name;
    }


    public String getLocalIP() {
        String name = m_generalSettings.getString("LocalIP");

        if( name.equalsIgnoreCase( "localhost" )) {
            name = "127." + ( m_lastIP >> 16 ) + "." + (( m_lastIP & 0x00FF00 ) >> 8 ) + "." + ( m_lastIP & 0x0000FF );
            m_lastIP++;
        }

        return name;
    }

    /**
        @return Resolution width of the bot
    */
    public int getResolutionX() {
        return m_generalSettings.getInt( "ResolutionX" );
    }

    /**
        @return Resolution height of the bot
    */
    public int getResolutionY() {
        return m_generalSettings.getInt( "ResolutionY" );
    }

    /**
        @return Reference to the locally held InterProcessCommunicator
    */
    public InterProcessCommunicator getInterProcessCommunicator() {
        return m_comm;
    }

    /**
        @return Reference to the locally held SocketCommunicator
    */
    public SocketCommunicator getSocketCommunicator() {
        return m_socketComm;
    }

    /**
        @return Reference to the locally held SQLManager
    */
    public SQLManager getSQLManager() {
        return m_manager;
    }

    /**
        @return Reference to the locally held OperatorList
    */
    public OperatorList getOperatorList() {
        return m_accessList;
    }

    /**
        @return Reference to the locally held BotSettings
    */
    public BotSettings getGeneralSettings() {
        return m_generalSettings;
    }

    /**
        Given the class name of a bot, return its associated configuration.
        If unable to find a configuration file, return null.
        @param botType Class name of the bot to retrieve the CFG of
        @return BotSettings containing the CFG information (null if CFG not found)
    */
    public BotSettings getBotConfig( String botType ) {
        botType = botType.trim().toLowerCase();
        File botBase = new File( m_generalSettings.getString( "Core Location" ) + "/twcore/bots" );
        File botConfig = Tools.getRecursiveFileInDirectory( botBase, botType + ".cfg" );

        if( botConfig == null ) {
            return null;
        } else {
            return new BotSettings( botConfig );
        }
    }

    /**
        Given the class name of a bot, return its associated cfg file.
        If unable to find a configuration file, return null.
        @param botType Class name of the bot to retrieve the CFG of
        @return BotSettings containing the CFG information (null if CFG not found)
    */
    public File getBotConfigFile( String botType ) {
        botType = botType.trim().toLowerCase();
        File botBase = new File( m_generalSettings.getString( "Core Location" ) + "/twcore/bots" );
        File botConfig = Tools.getRecursiveFileInDirectory( botBase, botType + ".cfg" );

        if( botConfig == null ) {
            return null;
        } else {
            return botConfig;
        }
    }
}
