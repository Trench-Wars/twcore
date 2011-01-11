package twcore.bots.robohelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.SQLResultEvent;
import twcore.core.util.SearchableStructure;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;

public class robohelp extends SubspaceBot {
    public static final String ALERT_CHAT = "training"; // chat that the new player alerts get sent to
    
    static int TIME_BETWEEN_ADS = 390000;//6.5 * 60000;
    public static final int LINE_SIZE = 100;
    public static final int CALL_EXPIRATION_TIME = 90000;  // Time after which a call can't
                                                           // can't be claimed (onit/gotit)
    													   // (90 secs - 1,5 min)
    public static final String ZONE_CHANNEL = "Zone Channel";

    boolean             m_banPending = false;
    boolean             m_strictOnIts = true;
    String              m_lastBanner = null;
    BotSettings         m_botSettings;

    SearchableStructure search;
    OperatorList        opList;
    TreeMap<String, String> rawData;
    Map<String, PlayerInfo> m_playerList;

    CommandInterpreter  m_commandInterpreter;
    String              lastHelpRequestName = null;

    final String        mySQLHost = "website";
    Vector<EventData>   eventList = new Vector<EventData>();
    TreeMap<String, EventData> events = new TreeMap<String, EventData>();
    Vector<EventData>   callList = new Vector<EventData>();
    Vector<NewPlayer> newbs = new Vector<NewPlayer>();

    /** Wing's way */
    TreeMap<Integer, HelpRequest> helpList = new TreeMap<Integer, HelpRequest>();
    Vector<Integer> calls = new Vector<Integer>();
    TreeMap<String, Integer> nameList = new TreeMap<String, Integer>();
    
    String				findPopulation = "";
	int					setPopID = -1;

	String	 			lastStafferClaimedCall;

    public robohelp( BotAction botAction ){
        super( botAction );

        m_botSettings = m_botAction.getBotSettings();

        populateSearch();
        opList = botAction.getOperatorList();
        m_playerList = Collections.synchronizedMap( new HashMap<String, PlayerInfo>() );

        m_commandInterpreter = new CommandInterpreter( m_botAction );
        registerCommands();
        m_botAction.getEventRequester().request( EventRequester.MESSAGE );
    }

    void registerCommands(){
        int         acceptedMessages;

        acceptedMessages = Message.REMOTE_PRIVATE_MESSAGE | Message.PRIVATE_MESSAGE;
        
        // Player commands
        m_commandInterpreter.registerCommand( "!next", acceptedMessages, this, "handleNext" );
        m_commandInterpreter.registerCommand( "!summon", acceptedMessages, this, "handleSummon" );
        
        // ZH+ commands
        m_commandInterpreter.registerCommand( "!lookup", acceptedMessages, this, "handleLookup", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!last", acceptedMessages, this, "handleLast", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "mainHelpScreen", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!claimhelp", acceptedMessages, this, "claimHelpScreen", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!mystats", acceptedMessages, this, "handleMystats", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand( "!hosted", acceptedMessages, this, "handleDisplayHosted", OperatorList.ZH_LEVEL );
        
        // Smod
        m_commandInterpreter.registerCommand( "!say", acceptedMessages, this, "handleSay", OperatorList.SMOD_LEVEL );
        
        // Sysop+ 
        m_commandInterpreter.registerCommand( "!reload", acceptedMessages, this, "handleReload", OperatorList.SYSOP_LEVEL );
        m_commandInterpreter.registerCommand( "!die", acceptedMessages, this, "handleDie", OperatorList.SYSOP_LEVEL );
        
        
        acceptedMessages = Message.CHAT_MESSAGE;
        
        // ER+
        m_commandInterpreter.registerCommand( "!claimhelp", acceptedMessages, this, "claimHelpScreen", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!repeat", acceptedMessages, this, "handleRepeat", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!warn", acceptedMessages, this, "handleWarn", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!tell", acceptedMessages, this, "handleTell", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!ban", acceptedMessages, this, "handleBan", OperatorList.ER_LEVEL );
        //m_commandInterpreter.registerCommand( "!google", acceptedMessages, this, "handleGoogle", OperatorList.ER_LEVEL );
        m_commandInterpreter.registerCommand( "!wiki",  acceptedMessages, this, "handleWikipedia", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!status", acceptedMessages, this, "handleStatus", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!dictionary", acceptedMessages, this, "handleDictionary", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!thesaurus", acceptedMessages, this, "handleThesaurus", OperatorList.ZH_LEVEL );
        m_commandInterpreter.registerCommand( "!javadocs", acceptedMessages, this, "handleJavadocs", OperatorList.ZH_LEVEL );
        
		if (!m_strictOnIts)
            m_commandInterpreter.registerDefaultCommand( acceptedMessages, this, "handleChat" );
		
        acceptedMessages = Message.ARENA_MESSAGE;

        m_commandInterpreter.registerCommand( "Ban", acceptedMessages, this, "handleBanNumber" );
    }

    public void handleStatus( String name, String message ){

        if( !m_botAction.SQLisOperational() ){
            m_botAction.sendChatMessage( "NOTE: The database connection is down. Some other bots might  experience problems too." );
            return;
        }
        try {
            m_botAction.SQLQueryAndClose( mySQLHost, "SELECT * FROM tblCall LIMIT 0,1" );
            m_botAction.sendChatMessage( "Statistic Recording: Operational" );
        } catch (Exception e ) {
            m_botAction.sendChatMessage( "NOTE: The database connection is down. Some other bots might experience problems too." );
        }
    }

    /**
     * Forms an appropriate query to dictionary.reference.com.
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleDictionary( String name, String message ) {
        m_botAction.sendChatMessage( "Dictionary definition:  http://dictionary.reference.com/search?q=" + message.replaceAll("\\s", "%20") );
    }

    /**
     * Forms an appropriate query to thesaurus.reference.com.
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleThesaurus( String name, String message ) {
        m_botAction.sendChatMessage( "Thesaurus entry:  http://thesaurus.reference.com/search?q=" + message.replaceAll("\\s", "%20") );
    }

    /**
     * Forms an appropriate query to javadocs.org
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleJavadocs( String name, String message ) {
        m_botAction.sendChatMessage( "Javadocs entry:  http://javadocs.org/" + message.replaceAll("\\s", "%20") );
    }
    
    /**
     * Forms an appropriate query to google.com
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleGoogle( String name, String message ) {
    	m_botAction.sendChatMessage( "Google search: http://www.google.com/search?q=" + message.replaceAll("\\s", "%20"));
    }
    
    /*public void handleGoogle( String name, String message ){
    m_botAction.sendChatMessage( "Google search results for " + message + ": " + doGoogleSearch( message ) );
}*/
    
    /**
     * Forms an appropriate query to wikipedia.org
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleWikipedia( String name, String message ) {
    	m_botAction.sendChatMessage( "Wikipedia search: http://en.wikipedia.org/wiki/" + message.replaceAll("\\s", "%20"));
    }

/*    public String doGoogleSearch( String searchString ){

        try {

            GoogleSearch s = new GoogleSearch();
            s.setKey( m_botSettings.getString( "GoogleKey" ) );
            //s.setKey( "EsAMyNxQFHLUiEnJqdsU1IKpEMl0yiDl" );
            s.setQueryString( searchString );
            s.setMaxResults( 1 );
            GoogleSearchResult r = s.doSearch();
            GoogleSearchResultElement[] elements = r.getResultElements();
            return elements[0].getURL();
        } catch( Exception e ){
        	Tools.printStackTrace(e);
            return new String( "Nothing found." );
       }
   }*/
    
    public void handleSay(String name, String msg){

    m_botAction.sendSmartPrivateMessage(name, "Command removed due to abuse. -Dezmond");}

   public void handleBanNumber( String name, String message ){
        String number;

        if( message.startsWith( "activated #" ) ){
            number = message.substring( message.indexOf( ' ' ) );
            if( m_banPending == true && number != null ){
                m_botAction.sendUnfilteredPublicMessage( "?bancomment " + number + " Ban by " + m_lastBanner + " for abusing the alert commands.  Mod: CHANGE THIS COMMENT & don't forget to ADD YOUR NAME." );
                m_banPending = false;
                m_lastBanner = null;
            }
        }
    }

    public void populateSearch(){
        search = new SearchableStructure();
        rawData = new TreeMap<String, String>();
        try {
        	FileReader reader = new FileReader( m_botAction.getDataFile( "HelpResponses.txt" ) );
            BufferedReader in = new BufferedReader( reader );
            String line;
            
            int i = 0;
            do{
                line = in.readLine();
                try{
                    if ( line != null){
                        line = line.trim();
                        int indexOfLine = line.indexOf( '|' );
                        if( indexOfLine != -1 && line.startsWith("#") == false){
                            String key = line.substring( 0, indexOfLine );
                            String response = line.substring( indexOfLine + 1 );
                            search.add( response, key );
                            rawData.put( response, key );
                        }
                    }
		        } catch( Exception e ){
		            System.out.println( "Error in HelpResponses.txt near: Line " + i );
		        }
		        i++;
            } while( line!=null );

            in.close();
            reader.close();
        } catch( IOException e ){
            Tools.printStackTrace( e );
        }
    }

    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena( "#robopark" );
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Staff Chat" ) + "," + ALERT_CHAT );
        m_botAction.sendUnfilteredPublicMessage( "?blogin " + m_botSettings.getString( "Banpassword" ) );
        m_botAction.ipcSubscribe(ZONE_CHANNEL);
    }

    /**
     * Intended for receiving reliable zone messages from ZonerBot, rather than
     * picking them apart from handleZone.
     * @param event IPC event to handle
     */
    public void handleEvent( InterProcessEvent event ) {
      IPCMessage ipcMessage = (IPCMessage) event.getObject();
      
      String message = ipcMessage.getMessage();
      try {
          if (message.startsWith("alert")) {
              handleNewPlayer(message.substring(6));
          } else {
              String parts[] = message.toLowerCase().split( "@ad@" );
              String host = parts[0];
              String arena = parts[1];
              String advert = parts[2];

              storeAdvert( host, arena, advert );
              m_botAction.sendSmartPrivateMessage(host, "Advert for '"+arena+"' registered."); 
          }
      } catch (Exception e ) {
    	  Tools.printStackTrace(e);
      }
    }
    
    public void handleNewPlayer(String message) {
        String player = message.substring(message.indexOf("): ") + 3);
        boolean send = false;
        try {
            ResultSet alerts = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCallNewb WHERE fcUserName = '" + Tools.addSlashesToString(player) + "'");
            if (alerts.next()) {
                if (alerts.getInt("fnTaken") == 0) {
                    send = true;
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallNewb SET fdCreated = NOW() WHERE fnAlertID = " + alerts.getInt("fnAlertID"));
                }
            } else {
                send = true;
                m_botAction.SQLQueryAndClose(mySQLHost, "INSERT INTO tblCallNewb (fcUserName, fdCreated) VALUES ('" + Tools.addSlashesToString(player) + "', NOW())");
            }
            m_botAction.SQLClose(alerts);
        } catch (Exception e ) { Tools.printLog( "Could not insert new player alert record." ); }
        
        if (send) {
            newbs.add(new NewPlayer(player));
            m_botAction.sendChatMessage(2, message + " [Use 'on that' to take this call]");
        }
    }
    
    // This is to catch any backgroundqueries even though none of them need to be catched to do something with the results
    public void handleEvent(SQLResultEvent event) {
    	if (event.getIdentifier().equals("robohelp"))
    		m_botAction.SQLClose( event.getResultSet() );
    }

    /**
     * Stores data of an advert into the database.
     * @param host Host of the event
     * @param arena Arena where it was hosted
     * @param advert Text of the advert
     */
    public void storeAdvert( String host, String arena, String advert ) {
        //Gets time;
        Calendar thisTime = Calendar.getInstance();
        java.util.Date day = thisTime.getTime();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( day );
        //Adds to buffer of sorts.
        if( !arena.equals( "" ) && !arena.equals( "elim" ) && !arena.equals( "baseelim" ) && !arena.equals( "tourny" ) ) {
            if( eventList.size() > 511 ) eventList.remove( 0 );
            eventList.addElement( new EventData( arena, new java.util.Date().getTime() ) );
            String query = "INSERT INTO `tblAdvert` (`fcUserName`, `fcEventName`, `fcAdvert`, `fdTime`) VALUES ";
            query += "('"+Tools.addSlashesToString(host)+"', '"+arena+"', '"+Tools.addSlashesToString(advert)+"', '"+time+"')";
            try {
                m_botAction.SQLQueryAndClose( mySQLHost, query );
            } catch (Exception e ) { Tools.printLog( "Could not insert advert record." ); }
        }
    }

    public void getArenaSize(int adID, String arena) {
		findPopulation = arena;
		setPopID = adID;
		m_botAction.requestArenaList();
	}

    public void handleDisplayHosted( String name, String message ) {
        int span = 1;
        try {
            span = Integer.parseInt( message );
            span = Math.max( Math.min( span, 24 ), 1 );
        } catch (Exception e) {}

        events.clear();
        long time = new java.util.Date().getTime();

        for( int i = eventList.size() - 1; i >= 0; i-- ) {
            EventData dat = eventList.elementAt( i );
            if( dat.getTime() + span*3600000 > time ) {
                if( events.containsKey( dat.getArena() ) ) {
                    EventData d = events.get( dat.getArena() );
                    d.inc();
                } else  events.put( dat.getArena(), new EventData( dat.getArena() ) );
            }
        }

        m_botAction.sendSmartPrivateMessage( name, "Events hosted within the last " + span + " hour(s): " );
        int numHosted = 0;
        Iterator<String> i = events.keySet().iterator();
        while (i.hasNext()) {
            String curEvent = i.next();
            EventData d = events.get( curEvent );
            m_botAction.sendSmartPrivateMessage( name, trimFill( curEvent ) + d.getDups() );
            numHosted += d.getDups();
        }
        m_botAction.sendSmartPrivateMessage( name, "----- Total: " + numHosted + "-----");
    }

    public String trimFill( String line ) {
        if(line.length() < 25) {
            for(int i=line.length();i<25;i++)
                line += " ";
        }
        return line;
    }

    class EventData {

        String  arena;
        long    time;
        int     dups;
        int     callID;

        public EventData( String a ) {
            arena = a;
            dups  = 1;
        }

        public EventData( long t ) {
            time = t;
        }

        public EventData( int id, long t ) {
            time = t;
            callID = id;
        }

        public EventData( String a, long t ) {
            arena = a;
            time = t;
        }

        public void inc() {
            dups++;
        }

        public int getID() { return callID; }
        public String getArena() { return arena; }
        public long getTime() { return time; }
        public int getDups() { return dups; }
    }

    public void handleAdvert ( String playerName, String message ){
        // HelpRequest     helpRequest;
        PlayerInfo info;

        lastHelpRequestName = playerName;

        if( opList.isBot( playerName ) ){
            return;
        }

        callList.addElement( new EventData( new java.util.Date().getTime() ) ); //For Records
        info = m_playerList.get( playerName.toLowerCase() );
        if( info == null ){
            info = new PlayerInfo( playerName );
            m_playerList.put( playerName.toLowerCase(), info );
        }
        if( info.AdvertTell() == true ){
        	m_botAction.sendChatMessage( "NOTICE: " + playerName + " has used ?advert before. Please use !warn if needed." );
        }
        else {
        	m_botAction.sendRemotePrivateMessage( playerName, "Please do not use ?advert. "
                +"If you would like to request an event, please use the ?help command." );
        	info.setAdvertTell( true );
        	m_botAction.sendChatMessage( playerName + " has been notified that ?advert is not for non-staff." );
        }

    }

    public void handleCheater( String playerName, String message ) {
        HelpRequest     help;
        PlayerInfo      info;

        lastHelpRequestName = playerName;

        info = m_playerList.get(playerName.toLowerCase());
        if (info == null)
            info = new PlayerInfo(playerName.toLowerCase());
        help = new HelpRequest(playerName, message, null, 2);
        help = storeHelp(help);
        info.addCall(help.getID());
        calls.add(help.getID());
        m_playerList.put(playerName.toLowerCase(), info);
        m_botAction.sendChatMessage("Call #" + help.getID() + "  (!claimhelp)");
    }

    public HelpRequest storeHelp(HelpRequest help) {
        if( !m_botAction.SQLisOperational())
            return help;
        
        String player = help.getPlayername();
        String msg = help.getQuestion();
        
        try {
            m_botAction.SQLQueryAndClose(mySQLHost, "INSERT INTO tblCallHelp (fcUserName, fcMessage, fdCreated, fnType) VALUES('" + Tools.addSlashesToString(player) + "', '" + Tools.addSlashesToString(msg) + "', NOW(), " + help.getType() + ")");
            ResultSet callid = m_botAction.SQLQuery(mySQLHost, "SELECT LAST_INSERT_ID()");
            if (callid.next())
                help.setID(callid.getInt(1));
            m_botAction.SQLClose(callid);
            helpList.put(help.getID(), help);
            return help;
        } catch ( Exception e ) {
            Tools.printStackTrace(e);
        }
        
        return help;
    }
    
    public void handleHelp(String playerName, String message) {
        String[] response;
        HelpRequest helpRequest;
        PlayerInfo info;

        if (playerName.compareTo(m_botAction.getBotName()) == 0) {
            return;
        }

        lastHelpRequestName = playerName;

        if (opList.isZH(playerName)) {
            String tempMessage = "Staff members: Please use :" + m_botAction.getBotName() + ":!lookup instead of ?help!";

            response = new String[1];
            response[0] = tempMessage;

            m_botAction.sendRemotePrivateMessage(playerName, tempMessage);

            return;
        }

        response = search.search(message);

        info = m_playerList.get(playerName.toLowerCase());
        helpRequest = new HelpRequest(playerName, message, response, 0);
        if (info == null) {
            info = new PlayerInfo(playerName);
            helpRequest = storeHelp(helpRequest);
            info.addCall(helpRequest.getID());
            m_playerList.put(playerName.toLowerCase(), info);
        } else {
            helpRequest.setAllowSummons(false);
            helpRequest.setQuestion(message, response);
            helpRequest.reset();
            helpRequest = storeHelp(helpRequest);
            info.addCall(helpRequest.getID());
            m_playerList.put(playerName.toLowerCase(), info);
        }

        calls.add(helpRequest.getID());

        if (response.length <= 0) {
            m_botAction.sendChatMessage("Call #" + helpRequest.getID() + "  (!claimhelp)");
        } else {
            m_botAction.sendChatMessage("I'll take it! (Call #" + helpRequest.getID() + ")");
            m_botAction.remotePrivateMessageSpam(playerName, helpRequest.getNextResponse());
            m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fcTakerName = '" + Tools.addSlashesToString("RoboHelp") + "', fnTaken = 1 WHERE fnCallID = " + helpRequest.getID());

            if (helpRequest.hasMoreResponses() == false) {
                helpRequest.setAllowSummons(true);
                m_botAction.sendRemotePrivateMessage(playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!summon to request live help.");
            } else if (response.length > 0) {
                m_botAction.sendRemotePrivateMessage(playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!next to retrieve the next response.");
            }
        }
    }

    public void handleNext( String playerName, String message ){
        String[]        response;
        HelpRequest     helpRequest;
        PlayerInfo      info;

        info = m_playerList.get( playerName.toLowerCase() );

        if( info == null || info.getLastCall() == -1){
            m_botAction.sendRemotePrivateMessage( playerName, "If you have a question, ask with ?help <question>" );
        } else {
            helpRequest = helpList.get(info.getLastCall());
            if( helpRequest.isValidHelpRequest() == true ){
                response = helpRequest.getNextResponse();
                if( response == null ){
                    helpRequest.setAllowSummons( true );
                    m_botAction.sendRemotePrivateMessage( playerName, "I have no further information.  If you still need help, message me with !summon to get live help." );
                } else {
                    m_botAction.remotePrivateMessageSpam( playerName, response );
                    if( helpRequest.hasMoreResponses() == false ){
                        m_botAction.sendRemotePrivateMessage( playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!summon to request live help." );
                    } else {
                        m_botAction.sendRemotePrivateMessage( playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!next to retrieve the next response." );
                    }
                }
            } else {
                m_botAction.sendRemotePrivateMessage( playerName, "I haven't given you any information yet.  Use ?help <question> if you need help." );
            }
        }
    }

    public void handleSummon( String playerName, String message ){
        HelpRequest     helpRequest;
        PlayerInfo      info;

        info = m_playerList.get( playerName.toLowerCase() );

        if( info == null || info.getLastCall() == -1){
            m_botAction.sendRemotePrivateMessage( playerName, "If you have a question, ask with ?help <question>" );
        } else {
            helpRequest = helpList.get(info.getLastCall());
            if( helpRequest.isValidHelpRequest() == true ){
                if( helpRequest.getAllowSummons() == false ){
                    if( helpRequest.hasMoreResponses() == true ){
                        m_botAction.sendRemotePrivateMessage( playerName, "I have more information.  Message me with !next to see it." );
                    } else {
                        m_botAction.sendRemotePrivateMessage( playerName, "I feel that you were given suitable information already." );
                    }
                } else {
                    m_botAction.sendRemotePrivateMessage( playerName, "A staff member may or "
                    +"may not be available.  If you do not get a response within 5 "
                    +"minutes, feel free to use ?help again." );
                    m_botAction.sendUnfilteredPublicMessage( "?help The player \""
                    + playerName + "\" did not get a satisfactory response.  Please respond "
                    +"to this player.");
                    helpRequest.setAllowSummons( false );
                }
            } else {
                m_botAction.sendRemotePrivateMessage( playerName, "I haven't given you any information yet.  Use ?help <question> if you need help." );
            }
        }
    }

    public void handleLast( String playerName, String message ){
        if (!opList.isBot(playerName))
          return;
        String[]        responses;
        HelpRequest     helpRequest;
        PlayerInfo      info;

        if( lastHelpRequestName == null ){
            m_botAction.sendRemotePrivateMessage( playerName, "No one has done a help call yet!" );
        }

        info = m_playerList.get( lastHelpRequestName.toLowerCase() );
        if( info == null || info.getLastCall() == -1){
            m_botAction.sendRemotePrivateMessage( playerName, "No response was given." );
        } else {
            helpRequest = helpList.get(info.getLastCall());
            responses = helpRequest.getAllResponses();
            if( responses == null ){
                m_botAction.sendRemotePrivateMessage( playerName, "No response was given." );
            } else {
                m_botAction.sendRemotePrivateMessage( playerName, "The responses available to " + lastHelpRequestName + " are:" );
                displayResponses(playerName, responses);
            }
        }
    }

    public void handleTell( String messager, String message ){
        String          name;
        String          keyword;
        int             seperator;
        PlayerInfo      info;

        seperator = message.indexOf( ':' );
        if( seperator == -1 ){
            name = lastHelpRequestName;
            keyword = message;
            
            // If the ?help caller is a bot, try to extract the name from its message
            // cheater: (PubBot6) (Public 4): TK Report: 51xg35qas8ghc is reporting BadBoyTKer for intentional TK. (150 total TKs)
            // (Not racism calls, you don't want to do !tell onit on the one who is doing racist words)
            if(m_botAction.getOperatorList().isBotExact(name) && m_playerList.containsKey(lastHelpRequestName.toLowerCase())) { 
                info = m_playerList.get(lastHelpRequestName.toLowerCase());
                String lastHelpMessage = null;
                if (info != null && info.getLastCall() > -1) {
                    lastHelpMessage = helpList.get(info.getLastCall()).getQuestion();
                }
            	
            	if(lastHelpMessage != null && lastHelpMessage.contains("TK Report: ")) {
            		name = lastHelpMessage.substring(lastHelpMessage.indexOf("TK Report: ")+11, lastHelpMessage.indexOf(" is reporting"));
            	}
            }
            
        } else {
            name = message.substring( 0, seperator ).trim();
            keyword = message.substring( seperator + 1 ).trim();
        }

        if( name != null ){
            if( messager.equalsIgnoreCase( name ) || messager.toLowerCase().startsWith( name.toLowerCase() ) ){
                m_botAction.sendChatMessage( "Use :" + m_botAction.getBotName() + ":!lookup <keyword> instead."  );
                
/*            } else if( keyword.toLowerCase().startsWith( "google " ) ){
                String     query;

                query = keyword.substring( 6 ).trim();
                if( query.length() == 0 ){
                    m_botAction.sendChatMessage( "Specify something to google for." );
                } else {
                    String result = doGoogleSearch( query );
                    m_botAction.sendRemotePrivateMessage( name, "Google says: " + result );
                    m_botAction.sendChatMessage( "Told " + name + " that Google says: " + result );
                }*/
            } else if( name.startsWith("#") ) {
            	m_botAction.sendChatMessage( "Invalid name. Please specify a different name." );
            	
            } else if( keyword.toLowerCase().startsWith( "dictionary " ) ){
                String     query;

                query = keyword.substring( 10 ).trim();
                if( query.length() == 0 ){
                    m_botAction.sendChatMessage( "Specify a word to reference." );
                } else {
                    m_botAction.sendRemotePrivateMessage( name, "Definition of " + query + ":  http://dictionary.reference.com/search?q=" + query );
                    m_botAction.sendChatMessage( "Gave " + name + " the definition of " + query + " at http://dictionary.reference.com/search?q=" + query );
                }
            } else if( keyword.toLowerCase().startsWith( "thesaurus " ) ){
                String     query;

                query = keyword.substring( 9 ).trim();
                if( query.length() == 0 ){
                    m_botAction.sendChatMessage( "Specify a word to reference." );
                } else {
                    m_botAction.sendRemotePrivateMessage( name, "Thesaurus entry for " + query + ":  http://thesaurus.reference.com/search?q=" + query );
                    m_botAction.sendChatMessage( "Gave " + name + " the thesaurus entry for " + query + " at http://thesaurus.reference.com/search?q=" + query );
                }
            } else if( keyword.toLowerCase().startsWith( "javadocs " ) ){
                String     query;

                query = keyword.substring( 8 ).trim();
                if( query.length() == 0 ){
                    m_botAction.sendChatMessage( "Specify something to look up the JavaDocs on." );
                } else {
                    m_botAction.sendRemotePrivateMessage( name, "Javadocs for " + query + ":  http://javadocs.org/" + query );
                    m_botAction.sendChatMessage( "Gave " + name + " the Javadocs entry for " + query + " at http://javadocs.org/" + query );
                }
            } else {
                String[] responses = search.search( keyword );

                if( responses.length == 0 ){
                    m_botAction.sendChatMessage( "Sorry, no matches."  );
                } else if( responses.length > 3 ){
                    m_botAction.sendChatMessage( "Too many matches.  Please be more specific."  );
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "This message has been sent by " + messager + ":");
                    displayResponses(name, responses);
                    m_botAction.sendSmartPrivateMessage(name, "If you have any other questions regarding this issue, please use :" + messager + ":<Message>.");
                    m_botAction.sendChatMessage( "Told " + name + " about " + keyword + "."  );
                }
            }
        }
    }

    public void handleLookup( String name, String message ){
        String[] resp = search.search( message );

        if( resp.length == 0 ){
            m_botAction.sendRemotePrivateMessage( name, "Sorry, no matches."  );
        } else {
            displayResponses(name, resp);
        }
    }

    public void handleReload( String name, String message ){
        if( opList.isSysop( name ) ){
            populateSearch();
            m_botAction.sendRemotePrivateMessage( name, "Reloaded." );
        }
    }
    
    public void handleDie( String name, String message ) {
    	if( opList.isSysop( name ) ) {
    		m_botAction.cancelTasks();
    		m_botAction.die("RoboHelp disconnected by "+name);
    	}
    }

    public void handleWarn( String playerName, String message ){
        String          name;
        PlayerInfo      info;

        if( message == null ){
            name = lastHelpRequestName;
        } else {
            if( message.trim().length() == 0 ){
                name = lastHelpRequestName;
            } else {
                name = message.trim();
            }
        }

        if( name == null ){
            m_botAction.sendChatMessage( "There hasn't been a help call yet." );
        } else if( name.length() == 0 ){
            m_botAction.sendChatMessage( "There hasn't been a help call yet." );
        } else {
            info = m_playerList.get( name.toLowerCase() );
            if( info == null || info.getLastCall() == -1 ){
                m_botAction.sendChatMessage( name + " hasn't done a help call yet." );
            } else {
                if( info.getBeenWarned() == true ) {
                    m_botAction.sendChatMessage( name + " has already been warned, no warning given." );
                } else {
                    info.setBeenWarned( true );
                    if( info.AdvertTell() == true ){
                        m_botAction.sendRemotePrivateMessage( name, "WARNING: Do NOT use the ?advert "
                        +"command.  It is for Staff Members only, and is punishable by a ban. Further abuse "
                        +"will not be tolerated!", 1 );
                        m_botAction.sendChatMessage( name + " has been warned for ?advert abuse." );

                        Calendar thisTime = Calendar.getInstance();
                        java.util.Date day = thisTime.getTime();
                        String warntime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( day );
                        String[] paramNames = { "name", "warning", "staffmember", "timeofwarning" };
                        String date = new java.sql.Date( System.currentTimeMillis() ).toString();
                        String[] data = { name.toLowerCase().trim(), new String( warntime + ": Warning to " + name + " from Robohelp for advert abuse.  !warn ordered by " + playerName ), playerName.toLowerCase().trim(), date };

                        m_botAction.SQLInsertInto( mySQLHost, "tblWarnings", paramNames, data );

                     } else {

                        m_botAction.sendRemotePrivateMessage( name, "WARNING: We appreciate "
                        +"your input.  However, your excessive abuse of the ?cheater or ?help command will "
                        +"not be tolerated further! Further abuse will result in a ban from the zone.", 1 );
                        m_botAction.sendChatMessage( name + " has been warned." );

                        Calendar thisTime = Calendar.getInstance();
                        java.util.Date day = thisTime.getTime();
                        String warntime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( day );
                        String[] paramNames = { "name", "warning", "staffmember", "timeofwarning" };
                        String date = new java.sql.Date( System.currentTimeMillis() ).toString();
                        String[] data = { name.toLowerCase().trim(), new String( warntime + ": Warning to " + name + " from Robohelp for help/cheater abuse.  !warn ordered by " + playerName ), playerName.toLowerCase().trim(), date };

                        m_botAction.SQLInsertInto( mySQLHost, "tblWarnings", paramNames, data );
                     }
                }
            }
        }
    }

    public void handleBan( String playerName, String message ){
        String          name;
        PlayerInfo      info;

        if( message == null ){
            name = lastHelpRequestName;
        } else {
            if( message.trim().length() == 0 ){
                name = lastHelpRequestName;
            } else {
                name = message.trim();
            }
        }

        if( name == null ){
            m_botAction.sendChatMessage( "There hasn't been a help call yet." );
        } else if( name.length() == 0 ){
            m_botAction.sendChatMessage( "There hasn't been a help call yet." );
        } else {
            if( !opList.isER( playerName ) ){
                m_botAction.sendChatMessage( "Only ER's and above are authorized to ban." );
            } else if( opList.isSmod( playerName ) && (opList.isBot( name ) && !opList.isSysop(name)) ){
                m_lastBanner = playerName;
                m_banPending = true;
                m_botAction.sendRemotePrivateMessage( name, "You have been banned for abuse as staff member. Depending on the Dean of Staff's decisions further action will be taken!" );
                m_botAction.sendUnfilteredPublicMessage( "?removeop " + name );
                m_botAction.sendUnfilteredPublicMessage( "?ban -a3 -e30 " + name );
                m_botAction.sendChatMessage( "Staffer \"" + name + "\" has been banned for abuse." );
                m_playerList.remove( name );
            }
            else if( opList.isBot( name ) ){
                m_botAction.sendChatMessage( "Are you nuts?  You can't ban a staff member!" );
            } else {
                info = m_playerList.get( name.toLowerCase() );
                if( info == null || info.getLastCall() == -1 ){
                    m_botAction.sendChatMessage( name + " hasn't done a help call yet." );
                } else {
                    if( info.getBeenWarned() == false ){
                        m_botAction.sendChatMessage( name + " hasn't been warned yet." );
                    } else {
                        m_lastBanner = playerName;
                        m_banPending = true;
                        m_botAction.sendRemotePrivateMessage( name, "You have been banned for "
                                +"abuse of the alert commands.  I am sorry this had to happen.  Your ban "
                                +"will likely expire in 24 hours.  Goodbye!" );
                        m_botAction.sendUnfilteredPublicMessage( "?ban -e1 " + name );
                        m_botAction.sendChatMessage( "Player \"" + name + "\" has been "
                                +"banned." );
                        m_playerList.remove( name );
                    }
                }
            }
        }
    }

    public void handleRepeat( String playerName, String message ){
        String          name;
        HelpRequest     helpRequest;
        PlayerInfo      info;

        if( message == null ){
            name = lastHelpRequestName;
        } else {
            if( message.trim().length() == 0 ){
                name = lastHelpRequestName;
            } else {
                name = message.trim();
            }
        }

        if( name == null ){
            m_botAction.sendChatMessage( "There hasn't been a help call yet." );
        } else if( name.length() == 0 ){
            m_botAction.sendChatMessage( "There hasn't been a help call yet." );
        } else {
            info = m_playerList.get( name.toLowerCase() );
            if( info == null || info.getLastCall() == -1 ){
                m_botAction.sendChatMessage( name + " hasn't done a help call yet." );
            } else {
                helpRequest = helpList.get(info.getLastCall());
                if( helpRequest.getFirstResponse() != null ) {
                    helpRequest.setAllowSummons( false );
                    m_botAction.sendRemotePrivateMessage( name, "I believe that the previous "
                            +"response you received was a sufficient answer to your question." );
                    m_botAction.remotePrivateMessageSpam( name, helpRequest.getFirstResponse() );
                    m_botAction.sendChatMessage( "The last response has been repeated to " + name );
                } else {
                    m_botAction.sendChatMessage( "Error repeating response to '" + name + "': response not found.  Please address this call manually.");
                }
            }
        }
    }
    
    public void handleThat(String name) {
        boolean record = false;
        Date now = new Date();
        String player = "";
        Iterator<NewPlayer> i = newbs.iterator();
        while (i.hasNext()) {
            NewPlayer np = i.next();
            if (!record && now.getTime() < np.getTime() + CALL_EXPIRATION_TIME) {
                record = true;
                player = np.getName();
                i.remove();
            } else if (now.getTime() >= np.getTime() + CALL_EXPIRATION_TIME) {
                i.remove();
            }
        }
        
        if (record) {
            m_botAction.sendRemotePrivateMessage(name, "Call claim of the new player '" + player + "' recorded.");
            updateStatRecordsONTHAT(name, player);            
        } else
            m_botAction.sendRemotePrivateMessage(name, "The call expired or no call found to match your claim.");
    }

    /**
     * For strict onits, requiring the "on it" to be at the start of the message.
     * For strict gotits, requiring the "got it" to be at the start of the message.
     * 
     * @param name Name of person claiming a call by saying 'on it' or 'got it'
     * @param message Message the chat message
     */
    public void handleClaim(String name, String message) {
        long now = System.currentTimeMillis();
        
        Iterator<Integer> i = calls.iterator();
        int id = -1;
        while (i.hasNext()) {
            id = i.next();
            HelpRequest call = helpList.get(id);
            if (now - call.getTime() > CALL_EXPIRATION_TIME) {
                i.remove();
            } else if (!call.isTaken()) {
                if (message.startsWith("on")) {
                    message = "on";
                } else if (message.startsWith("got"))
                    message = "got";
                handleClaims(name, message + " #" + id);
                i.remove();
                return;
            } else {
                i.remove();
            }
        }

        // A staffer did "on it" while there was no call to take (or all calls were expired).
        if(this.lastStafferClaimedCall != null && this.lastStafferClaimedCall.length() > 0)
            m_botAction.sendRemotePrivateMessage(name, "The call expired or was already taken. The last person to claim a call was "+this.lastStafferClaimedCall+".");
        else
            m_botAction.sendRemotePrivateMessage(name, "The call expired or no call was found to match your claim.");

    }
    
    public void handleClean(String name, String message) {
        int id = -1;
        if (message.contains("#")) {
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be cleaned.");
                return;
            }
        } else if (message.length() > 5) {
            try {
                id = Integer.valueOf(message.substring(5).trim());
            } catch (NumberFormatException e) {
                return;
            }
        }
        
        if (id > -1 && !helpList.containsKey(id)) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " could not be found.");
            return;            
        }
        
        if (id == -1) {
            if (lastHelpRequestName == null) {
                m_botAction.sendSmartPrivateMessage(name, "The last call could not be found.");
                return;                
            }
            String player = lastHelpRequestName;
            PlayerInfo info = m_playerList.get(player.toLowerCase());
            if (info == null || info.getLastCall() == -1) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be cleaned.");
                return;
            }
            id = info.getLastCall();            
        }

        HelpRequest last = helpList.get(id);
        if (last.getType() != 2) {
            m_botAction.sendSmartPrivateMessage(name, "Only cheater calls can be cleaned.");
            return;
        }
        if (!last.isTaken()) {
            last.claim();
            calls.removeElement(last.getID());
            m_botAction.sendSmartPrivateMessage(name, "Call #" + last.getID() + " cleaned.");
            m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fnTaken = 1 WHERE fnCallID = " + last.getID());
        } else
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed.");
    }
    
    public void handleForget(String name, String message) {
        int id = -1;
        if (message.contains("#")) {
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be forgotten.");
                return;
            }
        } else if (message.length() > 6) {
            try {
                id = Integer.valueOf(message.substring(6).trim());
            } catch (NumberFormatException e) {
                return;
            }
        }
        
        if (id > -1 && !helpList.containsKey(id)) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " could not be found.");
            return;            
        }
        
        if (id == -1) {
            if (lastHelpRequestName == null) {
                m_botAction.sendSmartPrivateMessage(name, "The last call could not be found.");
                return;                
            }
            String player = lastHelpRequestName;
            PlayerInfo info = m_playerList.get(player.toLowerCase());
            if (info == null || info.getLastCall() == -1) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be forgotten.");
                return;
            }
            id = info.getLastCall();            
        }

        HelpRequest last = helpList.get(id);
        if (!last.isTaken()) {
            last.claim();
            calls.removeElement(last.getID());
            m_botAction.sendSmartPrivateMessage(name, "Call #" + last.getID() + " forgotten.");
            m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fnTaken = 1 WHERE fnCallID = " + last.getID());
        } else
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed.");

    }
    
    public void handleMine(String name, String message) {
        int id = -1;
        if (message.contains("#")) {
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be claimed.");
                return;
            }
        } else if (message.length() > 4) {
            try {
                id = Integer.valueOf(message.substring(4).trim());
            } catch (NumberFormatException e) {
                return;
            }
        }
        
        if (id > -1 && !helpList.containsKey(id)) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " could not be found.");
            return;    
        }
        
        if (id == -1) {
            if (lastHelpRequestName == null) {
                m_botAction.sendSmartPrivateMessage(name, "The last call could not be found.");
                return;                
            }
            String player = lastHelpRequestName;
            PlayerInfo info = m_playerList.get(player.toLowerCase());
            if (info == null || info.getLastCall() == -1) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be claimed.");
                return;
            }
            id = info.getLastCall();            
        }

        HelpRequest last = helpList.get(id);
        if (!last.isTaken()) {
            last.setTaker(name);
            calls.removeElement(last.getID());
            m_botAction.sendSmartPrivateMessage(name, "Call #" + last.getID() + " claimed for you but not counted.");
            m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fnTaken = 1, fcTakerName = '" + Tools.addSlashesToString(name) + "' WHERE fnCallID = " + last.getID());
        } else
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed.");

        
    }
    
    
    public void handleClaims(String name, String message) {
        long now = System.currentTimeMillis();
        int id = -1;
        
        if (message.contains("#") && message.indexOf("#") < 13) {
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Invalid call number.");
                return;
            }
        } else if (message.equalsIgnoreCase("on it") || message.equalsIgnoreCase("got it")) {
            handleClaim(name, message);
            return;
        } else if (message.startsWith("on it ") || message.startsWith("got it ")) {
            try {
                id = Integer.valueOf(message.substring(message.indexOf("it") + 3));
            } catch (NumberFormatException e) {
                handleClaim(name, message);
                return;
            }
            
        } else {
            try {
                id = Integer.valueOf(message.substring(message.indexOf(" ") + 1));
            } catch (NumberFormatException e) {
                return;
            }            
        }
        
        HelpRequest help = helpList.get(id);
        if (help == null) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " not found.");
            return;
        } else {
            if (help.isTaken()) {
                m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed by " + help.getTaker() + ".");
            } else if (now - help.getTime() > CALL_EXPIRATION_TIME) {
                m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has expired.");
            } else {
                help.setTaker(name);
                if (message.startsWith("got")) 
                    help.setType(1);
                else if (message.startsWith("on") && help.getType() == -1) {
                    help.setType(0);
                }
                lastStafferClaimedCall = name;
                m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " claimed.");
                recordHelp(help);
            }
        }
    }
    

    public void recordHelp(HelpRequest help) {
        if (!m_botAction.SQLisOperational())
            return;

        String time = new SimpleDateFormat("yyyy-MM").format(Calendar.getInstance().getTime()) + "-01";
        if (help.getType() == 0 || help.getType() == 2) {
            try {
                ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker()) + "' AND fnType = 0 AND fdDate = '" + time + "'");
                if (result.next()) {
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker()) + "' AND fnType = 0 AND fdDate = '" + time + "'");
                } else {
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('" + Tools.addSlashesToString(help.getTaker()) + "', '1', '0', '" + time + "')");
                }
                m_botAction.SQLClose(result);
                
                m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fcTakerName = '" + Tools.addSlashesToString(help.getTaker()) + "', fnTaken = 1 WHERE fnCallID = " + help.getID());
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
        } else if (help.getType() == 1) {
            try {
                ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker()) + "' AND fnType = 1 AND fdDate = '" + time + "'");
                if (result.next()) {
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker()) + "' AND fnType = 1 AND fdDate = '" + time + "'");
                } else {
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('" + Tools.addSlashesToString(help.getTaker()) + "', '1', '1', '" + time + "')");
                }
                m_botAction.SQLClose(result);
                
                m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fcTakerName = '" + Tools.addSlashesToString(help.getTaker()) + "', fnTaken = 1, fnType = 1 WHERE fnCallID = " + help.getID());
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
        }
    }
    

    /**
     * Returns the call statistics of the <name> staffer.
     * @param name
     * @param message
     */
    public void handleMystats( String name, String message ) {
    	// 1. Check the level of the staff member - Mod / ER / ZH
    	// 2. Query the database
    	
    	// Only staff allowed to do this command
    	if(opList.isBot(name)==false)
    		return;
    	
    	String date = new SimpleDateFormat("yyyy-MM").format( Calendar.getInstance().getTime() );
    	String displayDate = new SimpleDateFormat("dd MMM yyyy HH:mm zzz").format( Calendar.getInstance().getTime() );
    	String query = null, rankQuery = null, title="", title2="";
    	HashMap<String, String> stats = new HashMap<String, String>();
    	ArrayList<String> rank = new ArrayList<String>();
    	message = message.trim().toLowerCase();
    	boolean showPersonalStats = true, showTopStats = true, showSingleStats = false;
    	int topNumber = 5;
    	
    	
    	// !mystats <month>-<year> in format of m-yyyy or mm-yyyy
    	String[] parameters = message.trim().split(" ");
    	if(parameters[0] != null && parameters[0].contains("-")) {
    		String[] dateParameters = parameters[0].split("-");
    		if(Tools.isAllDigits(dateParameters[0]) && Tools.isAllDigits(dateParameters[1])) {
        		int month = Integer.parseInt(dateParameters[0]);
        		int year = Integer.parseInt(dateParameters[1]);
        		
        		Calendar tmp = Calendar.getInstance();
        		tmp.set(year, month-1, 25, 23, 59, 59 );
        		
        		date = new SimpleDateFormat("yyyy-MM").format( tmp.getTime() );
        		displayDate = new SimpleDateFormat("MMMM yyyy").format( tmp.getTime() );
        		message = message.substring(parameters[0].length()).trim();
    		}
    	}
    	
    	
    	if((opList.isModerator(name) && message.length() == 0) || message.startsWith("mod")) {
    		// Staffer> !mystats
        	// Staffer> !mystats mod
    		
    		date = date + "-01";
    		query 		= "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='"+date+"' AND fcUserName NOT LIKE '%<ZH>%' AND fcUserName NOT LIKE '%<ER>%' ORDER BY fcUserName, fnType";
    		rankQuery 	= "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='"+date+"' AND fnType=0 AND fcUserName NOT LIKE '%<ZH>%' AND fcUserName NOT LIKE '%<ER>%' ORDER BY fnCount DESC";
    		title =  "Top 5 call count | "+displayDate;
    		title2 = "Your call count";
    		showTopStats = true;
    		showPersonalStats = true;
    		showSingleStats = false;
			topNumber = 5;
    		
    		if(message.length() > 3) {
    			// Staffer> !mystats mod #
    			String number = message.substring(4);
        		if(Tools.isAllDigits(number)) {
        			topNumber = Integer.parseInt(number);
        			title =  "Top "+topNumber+" call count | "+displayDate;
        		}
        		showPersonalStats = false;
    		}
    		
    	} else if((opList.isERExact(name) && message.length() == 0) || message.startsWith("er")) { 	
    		// Staffer> !mystats
        	// Staffer> !mystats er
    		query 	  = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '"+date+"%' GROUP BY fcUserName ORDER BY count DESC";
    		rankQuery = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '"+date+"%' GROUP BY fcUserName ORDER BY count DESC";
    		title =  "Top 5 advert count | "+displayDate;
    		title2 = "Your advert count";
    		showPersonalStats = true;
    		showTopStats = true;
    		showSingleStats = false;
    		topNumber = 5;
    		
    		if(message.length() > 2) {
        		// Staffer> !mystats er #
    			String number = message.substring(3);
    			if(Tools.isAllDigits(number)) {
        			topNumber = Integer.parseInt(number);
        			title =  "Top "+topNumber+" advert count | "+displayDate;
        		}
    			showPersonalStats = false;
    		}
    		
    	} else if((opList.isBotExact(name) && message.length() == 0) || message.startsWith("zh")) {
    		// Staffer> !mystats
        	// Staffer> !mystats zh
    		date = date + "-01";
    		query 		= "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='"+date+"' AND fcUserName LIKE '%<zh>%' ORDER BY fcUserName, fnType";
    		rankQuery 	= "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='"+date+"' AND fnType=0 AND fcUserName LIKE '%<zh>%' ORDER BY fnCount DESC";
    		title =  "Top 5 call count | "+displayDate;
    		title2 = "Your call count";
    		showPersonalStats = true;
    		showTopStats = true;
    		showSingleStats = false;
    		topNumber = 5;
    		
    		if(message.length() > 2) {
    			// Staffer> !mystats zh #
    			String number = message.substring(3);
    			if(Tools.isAllDigits(number)) {
        			topNumber = Integer.parseInt(number);
        			title =  "Top "+topNumber+" call count | "+displayDate;
        		}
    			showPersonalStats = false;
    		}
    		
		} else {
			// Staffer> !mystats <name>
			String playername = message;
			
			if(opList.isBot(playername)) {
				if(opList.isERExact(playername)) {
					query 	  = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '"+date+"%' AND fcUserName LIKE '"+playername+"' GROUP BY fcUserName";
		    		rankQuery = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '"+date+"%' AND fcUserName LIKE '"+playername+"' GROUP BY fcUserName";
				} else {
					date = date + "-01";
					query 		= "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='"+date+"' AND fcUserName LIKE '"+playername+"' ORDER BY fnType";
		    		rankQuery 	= "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='"+date+"' AND fnType=0 AND fcUserName LIKE '"+playername+"' ORDER BY fnCount DESC";
				}
			} else {
				m_botAction.sendSmartPrivateMessage(name, "No staff member with the name '"+playername+"' found.");
			}
			
			showPersonalStats = false;
    		showTopStats = false;
    		showSingleStats = true;
		}
    		
		// Order and create a list out of the results
    	if(query == null || rankQuery == null)
    		return;
    	
		try {
			ResultSet results = m_botAction.SQLQuery(mySQLHost, query);
			while(results != null && results.next()) {
				String staffer = results.getString(1);
				String count = results.getString(2);
				
				if(stats.containsKey(staffer)) {
					// query sets the fnType=1 as second, so this is the "got it"s
					stats.put(staffer, stats.get(staffer)+" ("+count+")");
				} else {
					// query sets the fnType=0 as first, so this is the "on it"s
					stats.put(staffer, Tools.formatString(count,3));
				}
			}
            m_botAction.SQLClose(results);
		} catch(Exception e) { Tools.printStackTrace( e ); }
		
		// Determine the rank
		try {
			ResultSet results = m_botAction.SQLQuery(mySQLHost, rankQuery);
			while(results != null && results.next()) {
				rank.add(results.getString(1));
			}
            m_botAction.SQLClose(results);
		} catch(Exception e) { Tools.printStackTrace( e ); }
    	    	

		// Return the top 5
		if(showTopStats) {
	    	m_botAction.sendSmartPrivateMessage(name, title);
	    	m_botAction.sendSmartPrivateMessage(name, "------------------");
	    	for(int i = 0 ; i < topNumber ; i++) {
	    		if(i < rank.size())
	    			m_botAction.sendSmartPrivateMessage(name, " "+
	    					Tools.formatString((i+1)+")", 5)+
	    					Tools.formatString(rank.get(i),20)+" "+
	    					stats.get(rank.get(i)));
	    	}
		}

		// Return your position, one previous and one next
		if(showPersonalStats) {
	    	int yourPosition = -1;
	    	
	    	// Determine your position in the rank
	    	for(int i = 0 ; i < rank.size(); i++) {
	    		if(rank.get(i).equalsIgnoreCase(name)) {
	    			yourPosition = i;
	    			break;
	    		}
	    	}
	    
	    	// Response
	    	m_botAction.sendSmartPrivateMessage(name, "    "); // spacer
	    	m_botAction.sendSmartPrivateMessage(name, title2);
	    	m_botAction.sendSmartPrivateMessage(name, "-----------------");
	    	if(yourPosition == -1) {
	    		m_botAction.sendSmartPrivateMessage(name, " There is no statistic from your name found.");
	    	} else {
		    	for( int i = yourPosition-1; i < yourPosition+2; i++) {
		    		if(i > -1 && i < rank.size())
		    			m_botAction.sendSmartPrivateMessage(name, " "+
		    					Tools.formatString((i+1)+")", 5)+
		    					Tools.formatString(rank.get(i),20)+" "+
		    					stats.get(rank.get(i)));
		    	}
	    	}
		}
		
		if(showSingleStats) {
			if(stats.size( )> 0 && rank.size() > 0) {
				m_botAction.sendSmartPrivateMessage(name, " "+
		    		Tools.formatString(rank.get(0),20)+" "+
		    		stats.get(rank.get(0)));
			} else {
				m_botAction.sendSmartPrivateMessage(name,"No statistic of "+message+" found.");
			}
		}
    	
    }
    

    public void updateStatRecordsONTHAT( String name, String player ) {
        if( !m_botAction.SQLisOperational())
            return;
        
        try {
            m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallNewb SET fnTaken = 1, fcTakerName = '" + Tools.addSlashesToString(name) + "' WHERE fcUserName = '" + Tools.addSlashesToString(player) + "'");
            String time = new SimpleDateFormat("yyyy-MM").format( Calendar.getInstance().getTime() ) + "-01";
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '"+name+"' AND fnType = 2 AND fdDate = '"+time+"'" );
            if(result.next()) {
                m_botAction.SQLBackgroundQuery( mySQLHost, "robohelp", "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '"+name+"' AND fnType = 2 AND fdDate = '"+time+"'" );
            } else {
                m_botAction.SQLBackgroundQuery( mySQLHost, "robohelp", "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('"+name+"', '1', '2', '"+time+"')" );
            }
            m_botAction.SQLClose( result );
        } catch ( Exception e ) {
            //m_botAction.sendChatMessage(2, "Error occured when registering call claim from '"+name+"' :"+e.getMessage());
            Tools.printStackTrace(e);
        }
    }
    

    public void mainHelpScreen( String playerName, String message ){
        final String[] helpText = {
            "Chat commands:",
            " !repeat <optional name>                   - Repeats the response to the specified name.  If no",
            "                                             name is specified, the last response is repeated.",
            " !tell <name>:<keyword>                    - Private messages the specified name with the",
            "                                             response to the keyword given.",
            " !warn <optional name>                     - Warns the specified player.  If no name is given,",
            "                                             warns the last person.",
            " !ban <optional name>                      - Bans the specified player.  If no name is given,",
            "                                             bans the last person. (ER+)",
            " !status                                   - Gives back status from systems.",
//            " !google search - Returns first page found by Googling the search term.",
            " !dictionary word                          - Returns a link for a definition of the word.",
            " !thesaurus word                           - Returns a link for a thesaurus entry for the word.",
            " !javadocs term                            - Returns a link for a javadocs lookup of the term.",
            " !google word                              - Returns a link for a google search of the word.",
            " !wiki word                                - Returns a link for a wikipedia search of the word.",
            " ",
            "PM commands:",
            " !lookup <keyword>                         - Tells you the response when the specified key word",
            "                                             is given",
            " !last <optional name>                     - Tells you what the response to the specified",
            "                                             player was. If no name is specified, the last",
            "                                             response is given.",
            " !hosted <hours>                           - Displays the hosted events in the last specified",
            "                                             <hours>, <hours> can be ommitted.",
            " !mystats                                  - Returns the top 5 call count and your call stats",
            " !mystats mod/er/zh [#]                    - Returns the top # of moderators / ERs / ZHs.",
            "                                             If # is not specified, shows top 5.",
            " !mystats <name>                           - Returns the call count of <name>",
            " !mystats <month>-<year> [above arguments] - Returns the top/call count from specified",
            "                                             month-year. F.ex: !mystats 08-2007 mod 50"
        };
        if( m_botAction.getOperatorList().isZH( playerName ) )
            m_botAction.remotePrivateMessageSpam( playerName, helpText );
                
        String[] SysopHelpText = {
            " !reload                                   - Reloads the HelpResponses database from file",
            " !die                                      - Disconnects this bot"
        };
        if( m_botAction.getOperatorList().isSysop( playerName ))
        	m_botAction.remotePrivateMessageSpam( playerName, SysopHelpText );
    }
    
    public void claimHelpScreen( String playerName, String message ){
        final String[] helpText = {
            "Claim commands (in staff chat):",
            " on it                                     - Same as before, claims the earliest non-expired call",
            " on #<id>, on <id>, on it #<id>            - Claims Call #<id> if it hasn't expired",
            " got it                                    - Same as before, claims the earliest non-expired call",
            " got #<id>, got <id>, got it #<id>         - Claims Call #<id> if it hasn't expired",
            " mine                                      - Claims the most recent call but does not affect staff stats",
            " mine #<id>, mine <id>                     - Claims Call #<id> but does not affect staff stats",
            "                                              Used to prevent a call from being counted as unanswered",
            " clean                                     - Clears the most recent false positive racism alert",
            " clean #<id>, clean <id>                   - Clears Call #<id> due to a false positive racism alert",
            " forget                                    - Prevents the most recent call from being counted as unanswered",
            " forget #<id>, forget <id>                 - Prevents Call #<id> from being counted as unanswered"
        };
        if( m_botAction.getOperatorList().isZH( playerName ) )
            m_botAction.remotePrivateMessageSpam( playerName, helpText );
    }
    

    private int indexNotOf(String string, char target, int fromIndex)
    {
      for(int index = fromIndex; index < string.length(); index++)
        if (string.charAt(index) != target)
          return index;
      return -1;
    }
    

    private int getBreakIndex(String string, int fromIndex)
    {
      if(fromIndex + LINE_SIZE > string.length())
        return string.length();
      int breakIndex = string.lastIndexOf((int) ' ', fromIndex + LINE_SIZE);
      if ("?".equals(string.substring(breakIndex+1, breakIndex+2)) || "*".equals(string.substring(breakIndex+1, breakIndex+2))) {
          breakIndex = string.lastIndexOf((int) ' ', fromIndex + LINE_SIZE - 3);
      }
      if(breakIndex == -1)
        return fromIndex + LINE_SIZE;
      return breakIndex;
    }
    

    private String[] formatResponse(String response)
    {
      LinkedList<String> formattedResp = new LinkedList<String>();
      int startIndex = indexNotOf(response, ' ', 0);
      int breakIndex = getBreakIndex(response, 0);

      while(startIndex != -1)
      {
        formattedResp.add(response.substring(startIndex, breakIndex));
        startIndex = indexNotOf(response, ' ', breakIndex);
        breakIndex = getBreakIndex(response, startIndex);
      }
      return formattedResp.toArray(new String[formattedResp.size()]);
    }

    
    private void displayResponses(String name, String[] responses)
    {
      for(int counter = 0; counter < responses.length; counter++)
        m_botAction.remotePrivateMessageSpam(name, formatResponse(responses[counter]));
    }

    public void handleEvent( Message event ){

        m_commandInterpreter.handleEvent( event );

        if( event.getMessageType() == Message.ALERT_MESSAGE ){
            String command = event.getAlertCommandType().toLowerCase();
            if( command.equals( "help" )){
                handleHelp( event.getMessager(), event.getMessage() );
            } else if( command.equals( "cheater" )){
                handleCheater( event.getMessager(), event.getMessage() );
            } else if( command.equals( "advert" )){
                handleAdvert( event.getMessager(), event.getMessage() );
            }
        }
        else if (event.getMessageType() == Message.CHAT_MESSAGE) {
        	String message = event.getMessage().toLowerCase().trim();
        	if (!message.contains("that") && !message.contains("it") && (message.startsWith("on") || message.startsWith("got") || message.startsWith("claim") || message.startsWith("have")) && opList.isZH(event.getMessager()))
        	    handleClaims(event.getMessager(), message);
        	else if (!message.contains("that") && message.contains("#") && (message.startsWith("on") || message.startsWith("got") || message.startsWith("claim") || message.startsWith("have")) && opList.isZH(event.getMessager()))
                handleClaims(event.getMessager(), message);
        	else if ((message.startsWith("on it") || message.startsWith("got it")) && opList.isZH(event.getMessager()))
        		handleClaims(event.getMessager(), message);
        	else if (message.startsWith("on that") || message.startsWith("got that"))
        	    handleThat(event.getMessager());
            else if (message.startsWith("clean"))
                handleClean(event.getMessager(), event.getMessage());
            else if (message.startsWith("forget"))
                handleForget(event.getMessager(), event.getMessage());
            else if (message.startsWith("mine"))
                handleMine(event.getMessager(), event.getMessage());
        }
    }
    
    
    class NewPlayer {
        long time;
        String name;
        
        public NewPlayer(String p) {
            this.name = p;
            this.time = System.currentTimeMillis();
        }
        
        public long getTime() {
            return time;
        }
        
        public String getName() {
            return name;
        }
    }


    class PlayerInfo {
        String m_playerName;
        boolean m_beenWarned;
        boolean m_advertTell;
        int m_lastCall;
        Vector<Integer> m_calls;
        
        public PlayerInfo(String name) {
            m_playerName = name;
            m_beenWarned = false;
            m_advertTell = false;
            m_lastCall = -1;
            m_calls = new Vector<Integer>();
        }
        
        public void addCall(int id) {
            m_calls.add(id);
            m_lastCall = id;
        }
        
        public Vector<Integer> getCalls() {
            return m_calls;
        }
        
        public int getLastCall() {
            return m_lastCall;
        }
        
        public void setBeenWarned( boolean beenWarned ){
            m_beenWarned = beenWarned;
        }

        public boolean getBeenWarned(){
            return m_beenWarned;
        }

        public void setAdvertTell( boolean advertTell ){
            m_advertTell = advertTell;
        }

        public boolean AdvertTell(){
            return m_advertTell;
        }

    }
    
    
    class HelpRequest {

        long        m_time;
        String      m_question;
        String[]    m_responses;
        String      m_playerName;
        int         m_nextResponse;
        boolean     m_allowSummons;
        int         m_callID;
        int         m_type;
        boolean     m_claimed;
        String      m_claimer;

        public HelpRequest( String playerName, String question, String[] responses ){

            m_callID = -1;
            m_nextResponse = 0;
            m_question = question;
            m_allowSummons = false;
            m_responses = responses;
            m_playerName = playerName;
            m_time = System.currentTimeMillis();
            m_type = -1;
            m_claimed = false;
        }

        public HelpRequest( int id, String playerName, String question, String[] responses ){

            m_callID = id;
            m_nextResponse = 0;
            m_question = question;
            m_allowSummons = false;
            m_responses = responses;
            m_playerName = playerName;
            m_time = System.currentTimeMillis();
            m_type = -1;
            m_claimed = false;
        }

        public HelpRequest( String playerName, String question, String[] responses, int type ){

            m_callID = -1;
            m_nextResponse = 0;
            m_question = question;
            m_allowSummons = false;
            m_responses = responses;
            m_playerName = playerName;
            m_time = System.currentTimeMillis();
            m_type = type;
            m_claimed = false;
        }
        
        public void reset() {
            m_claimer = "";
            m_claimed = false;
        }
        
        public void setTaker(String name) {
            m_claimer = name;
            m_claimed = true;
        }
        
        public String getTaker() {
            return m_claimer;
        }
        
        public void claim() {
            m_claimed = true;
        }
        
        public boolean isTaken() {
            return m_claimed;
        }
        
        public void setType(int type) {
            m_type = type;
        }
        
        public int getType() { return m_type; }
        
        public int getID() { return m_callID; }
        
        public void setID(int id) {
            m_callID = id;
        }

        public void setQuestion( String question, String[] responses ){

            m_nextResponse = 0;
            m_question = question;
            m_responses = responses;
            m_time = System.currentTimeMillis();
        }


        public void setAllowSummons( boolean allowSummons ){

            m_allowSummons = allowSummons;
        }

        public boolean getAllowSummons(){

            return m_allowSummons;
        }

        public boolean isValidHelpRequest(){

            if( m_question != null ){
                return true;
            } else {
                return false;
            }
        }

        public String[] getAllResponses(){

            return m_responses;
        }

        public String[] getFirstResponse(){

            if( m_responses == null ){
                return null;
            } else {
                if( m_responses.length != 0 )
                    return formatResponse(m_responses[0]);
                else
                    return null;
            }
        }

        public String[] getLastResponse(){

            if( m_nextResponse <= 0 || m_responses == null ){
                return null;
            } else {
                return formatResponse(m_responses[m_nextResponse - 1]);
            }
        }

        public String[] getNextResponse(){

            if( m_responses == null ){
                return null;
            }

            if( m_nextResponse >= m_responses.length ){
                return null;
            } else {
                return formatResponse(m_responses[m_nextResponse++]);
            }
        }
        
        public boolean hasMoreResponses(){

            if( m_responses == null ){
                return false;
            }

            if( m_nextResponse >= m_responses.length ){
                return false;
            } else {
                return true;
            }
        }
        
        public String getPlayername() {
        	return this.m_playerName;
        }
        
        public long getTime() {
        	return this.m_time;
        }
        
        public String getQuestion() {
        	return this.m_question;
        }
    }

}



