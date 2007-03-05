package twcore.bots.twdopstats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.SQLResultEvent;

public class twdopstats extends SubspaceBot {

	private EventRequester events; 
	private final String mySQLHost = "local";
	
    private Vector<EventData> callList = new Vector<EventData>();
	
	private CommandInterpreter m_commandInterpreter;
	
	public static final int CALL_EXPIRATION_TIME = 90000;
	
	private HashMap<String,String> twdops = new HashMap<String,String>();
	
	private updateTWDOpsTask updateOpsList;
	private String updateTWDOpDelay = String.valueOf(24 * 60 * 60 * 1000); // once every day
	
	public twdopstats(BotAction botAction) {
		super(botAction);
		
		events = m_botAction.getEventRequester();
        events.request(EventRequester.MESSAGE);
        
        registerCommands();
	}
	
	private void registerCommands(){
        m_commandInterpreter = new CommandInterpreter( m_botAction );
        m_commandInterpreter.registerCommand( "!status", Message.CHAT_MESSAGE, this, "handleStatusCommand" );
        m_commandInterpreter.registerCommand( "!help", Message.PRIVATE_MESSAGE, this, "handleHelpCommand" );
        m_commandInterpreter.registerCommand( "!update", Message.PRIVATE_MESSAGE, this, "handleUpdateCommand" );
        m_commandInterpreter.registerCommand( "!die", Message.PRIVATE_MESSAGE, this, "handleDieCommand");
    }
	
    public void handleEvent(LoggedOn event) {
    	 m_botAction.joinArena( "#robopark" );
         m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Staff Chat" ) + "," + m_botAction.getGeneralSettings().getString( "Chat Name" ) );
         
         // Load the TWD Operators list
         loadTWDOps();
         
         // Start the task to update the TWD Operator list
         updateOpsList = new updateTWDOpsTask(this);
         m_botAction.scheduleTaskAtFixedRate(updateOpsList, Long.valueOf(this.updateTWDOpDelay).longValue(), Long.valueOf(this.updateTWDOpDelay).longValue());
    }
    
    /**
     * @see twcore.core.SubspaceBot.handleDisconnect() 
     */
    public void handleDisconnect() {
    	this.updateOpsList.cancel();
    	updateOpsList = null;
    }
    
    private void loadTWDOps() {
    	String query = "SELECT tblUser.fcUsername FROM `tbluserrank`, `tblUser` WHERE `fnRankID` = '14' AND tblUser.fnUserID = tbluserrank.fnUserID";
    	m_botAction.SQLBackgroundQuery("local", "TWDOpsUpdate", query);
    }
    
    /**
     * Handle the background sql process once it's finished
     * and update the TWD Operator list
     */
    public void handleEvent( SQLResultEvent event) {
    	if(event.getIdentifier().equals("TWDOpsUpdate")) {
    		ResultSet resultSet = event.getResultSet();
    		
    		if(resultSet == null) {
    			throw new RuntimeException("ERROR: Null resultSet returned; connection may be down.");
    		}
    		
			// Clear current list of TWD Operators
   			twdops.clear();
   			
    		// Iterate over all the TWD Operators from the database and add them to the hashmap
    		try {    		
    			while(resultSet.next()) {
    				String name = resultSet.getString("fcUsername");
    				twdops.put( name.toLowerCase(), name );
        		}
    		} catch(SQLException sqle) {
    			throw new RuntimeException("SQL Error: " + sqle.getMessage(), sqle);
    		}
    		
    	}
    }
    
    public void handleEvent( Message event ){

        m_commandInterpreter.handleEvent( event );

        if( event.getMessageType() == Message.ALERT_MESSAGE ){
            String command = event.getAlertCommandType().toLowerCase();
            if( command.equals( "help" ) || command.equals("cheater")){
            	if( event.getMessager().compareTo( m_botAction.getBotName() ) != 0 &&
            		event.getMessage().toLowerCase().indexOf("twd") != -1) {
                	callList.addElement( new EventData( new Date().getTime() ) );
                }
            }
        }
        else if (event.getMessageType() == Message.CHAT_MESSAGE) {
        	String message = event.getMessage().toLowerCase();
        	if (message.startsWith("on it"))
        		handleOnIt(event.getMessager(), event.getMessage());
        }
    }
    
    public void handleHelpCommand( String playerName, String message ) {
    	String[] helpText = {
                "TWDOpStats commands:",
                "[CHAT] !status  - Gives back status from systems.",
                "[PM  ] !update  - Updates the operator list from the bot core",
                "[PM  ] !die     - Tells the bot to go parachuting (without the parachute)"
            };
    	
    	if( m_botAction.getOperatorList().isSmod(playerName)) {
    		m_botAction.smartPrivateMessageSpam( playerName, helpText );
    	}
    }
    
    /**
     * Returns the (mysql) status of the bot when the !status command is given on the chat
     */
    public void handleStatusCommand( String name, String message ) {
    	if( !m_botAction.SQLisOperational() ){
            m_botAction.sendChatMessage( "NOTE: The database connection is down. Some other bots might  experience problems too." );
            return;
    	}
        
    	try {
            m_botAction.SQLQuery( mySQLHost, "SELECT * FROM tblCall LIMIT 0,1" );
            m_botAction.sendChatMessage( "Statistic Recording: Operational" );

    	} catch (Exception e ) {
            m_botAction.sendChatMessage( "NOTE: The database connection is down. Some other bots might experience problems too." );
        }
    }
    
    public void handleUpdateCommand( String name, String message) {
    	loadTWDOps();
    	m_botAction.sendSmartPrivateMessage(name, "TWD Operator list updated.");
    }
    
    public void handleDieCommand( String name, String message ) {
    	if(m_botAction.getOperatorList().isSmod(name)) {
    		m_botAction.die();
    	}
    }
    
    
    
    /**
     * For strict onits, requiring the "on it" to be at the start of the message.
     * @param name Name of person saying on it
     * @param message Message containing on it
     */
    public void handleOnIt( String name, String message ) {
        boolean recorded = false;
        int i = 0;
        
        if(callList.size()==0) {
        	return;
        }
        
        if(twdops.containsKey(name.toLowerCase()) == false) {
        	return;
        }
        
        while( !recorded && i < callList.size() ) {
            EventData e = callList.elementAt( i );
            if( new Date().getTime() < e.getTime() + CALL_EXPIRATION_TIME ) {
                updateStatRecordsONIT( name );
                callList.removeElementAt( i );
                recorded = true;
            } else {
            	callList.removeElementAt( i );
            }
            i++;
        }
    }
    
    public void updateStatRecordsONIT( String name ) {
        try {
            Calendar thisTime = Calendar.getInstance();
            Date day = thisTime.getTime();
            String time = new SimpleDateFormat("yyyy-MM").format( day ) + "-01";
            
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblTWDCall WHERE fcUserName = '"+name+"' AND fdDate = '"+time+"' LIMIT 0,1" );
            
            if(result.next()) {
                m_botAction.SQLQuery( mySQLHost, "UPDATE tblTWDCall SET fnCount = fnCount + 1 WHERE fcUserName = '"+name+"' AND fdDate = '"+time+"'" );
            } else {
				m_botAction.SQLQuery( mySQLHost, "INSERT INTO tblTWDCall (`fcUserName`, `fnCount`, `fdDate`) VALUES ('"+name+"', '1', '"+time+"')" );
		    }
            
        } catch ( SQLException e ) {
		    m_botAction.sendChatMessage(2,"EXCEPTION: Unable to update the tblTWDCall table: " + e.getMessage() );
		}
    }
    
    
    
    /**
     * Returns the number of rows in the ResultSet
     * @param rs : ResultSet
     * @return count
     */
    public static int getRowCount(ResultSet rs) throws SQLException{
        int numResults = 0;
        
        rs.last();
        numResults = rs.getRow();
        rs.beforeFirst();

        return numResults;
    }
    
    
    class EventData {

        String  arena;
        long    time;
        int     dups;

        public EventData( String a ) {
            arena = a;
            dups  = 1;
        }

        public EventData( long t ) {
            time = t;
        }

        public EventData( String a, long t ) {
            arena = a;
            time = t;
        }

        public void inc() {
            dups++;
        }

        public String getArena() { return arena; }
        public long getTime() { return time; }
        public int getDups() { return dups; }
    }
    
    private class updateTWDOpsTask extends TimerTask {
    	twdopstats botInstance;
    	
    	public updateTWDOpsTask(twdopstats botInstance) {
    		this.botInstance = botInstance;
    	}
    	
    	public void run() {
    		botInstance.loadTWDOps();
    	}
    }

}
