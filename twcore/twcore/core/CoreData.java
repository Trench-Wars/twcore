package twcore.core;

import java.io.*;
public class CoreData {
    
    private InterProcessCommunicator    m_comm;
    private SQLManager                  m_manager;
    private OperatorList                m_accessList;
    private BotSettings                 m_generalSettings;
    private int                         m_lastIP = 0x050000;

    /** Creates a new instance of Class */
    public CoreData( File setupFile ){
        m_generalSettings = new BotSettings( setupFile );
        m_comm = new InterProcessCommunicator();
        m_accessList = new OperatorList();
        File sqlFile = new File( m_generalSettings.getString( "Core Location" )
        + "/corecfg/sql.cfg" );
        m_manager = new SQLManager( sqlFile );
    }
    
    public InterProcessCommunicator getInterProcessCommunicator(){
        return m_comm;
    }
    
    public int getServerPort(){
        return m_generalSettings.getInt( "Port" );
    }
    
    public String getServerName(){
        String name = m_generalSettings.getString( "Server" );
        if( name.equalsIgnoreCase( "localhost" )){
            name = "127." + ( m_lastIP >> 16 ) + "." + (( m_lastIP & 0x00FF00 ) >> 8 ) + "." + ( m_lastIP & 0x0000FF );
            m_lastIP++;
        }
        return name;        
    }
    
    public SQLManager getSQLManager(){
        return m_manager;
    }
    
    public OperatorList getOperatorList(){
        return m_accessList;
    }
    
    public BotSettings getGeneralSettings(){
        return m_generalSettings;
    }
    
    public BotSettings getBotConfig( String botType ){
        botType = botType.trim().toLowerCase();
        File botBase = new File( m_generalSettings.getString( "Core Location" ) + "/twcore/bots" );
        File botConfig = Tools.getRecursiveFileInDirectory( botBase, botType + ".cfg" );

        if( botConfig == null ){
            return null;
        } else {
            return new BotSettings( botConfig );
        }
    }
}
