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
    Map<String, HelpRequest> m_playerList;

    CommandInterpreter  m_commandInterpreter;
    String              lastHelpRequestName = null;

    final String        mySQLHost = "website";
    Vector<EventData>   eventList = new Vector<EventData>();
    TreeMap<String, EventData> events = new TreeMap<String, EventData>();
    Vector<EventData>   callList = new Vector<EventData>();

    String				findPopulation = "";
	int					setPopID = -1;

	String	 			lastStafferClaimedCall;

    public robohelp( BotAction botAction ){
        super( botAction );

        m_botSettings = m_botAction.getBotSettings();

        populateSearch();
        opList = botAction.getOperatorList();
        m_playerList = Collections.synchronizedMap( new HashMap<String, HelpRequest>() );

        m_commandInterpreter = new CommandInterpreter( m_botAction );
        registerCommands();
        m_botAction.getEventRequester().request( EventRequester.MESSAGE );
    }

    void registerCommands(){
        int         acceptedMessages;

        acceptedMessages = Message.REMOTE_PRIVATE_MESSAGE | Message.PRIVATE_MESSAGE;

        m_commandInterpreter.registerCommand( "!next", acceptedMessages, this, "handleNext" );
        m_commandInterpreter.registerCommand( "!summon", acceptedMessages, this, "handleSummon" );
        m_commandInterpreter.registerCommand( "!lookup", acceptedMessages, this, "handleLookup" );
        m_commandInterpreter.registerCommand( "!last", acceptedMessages, this, "handleLast" );
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "mainHelpScreen" );
        m_commandInterpreter.registerCommand( "!reload", acceptedMessages, this, "handleReload" );
        m_commandInterpreter.registerCommand( "!mystats", acceptedMessages, this, "handleMystats");
        m_commandInterpreter.registerCommand( "!hosted", acceptedMessages, this, "handleDisplayHosted" );
        
        acceptedMessages = Message.CHAT_MESSAGE;

        m_commandInterpreter.registerCommand( "!repeat", acceptedMessages, this, "handleRepeat" );
        m_commandInterpreter.registerCommand( "!warn", acceptedMessages, this, "handleWarn" );
        m_commandInterpreter.registerCommand( "!tell", acceptedMessages, this, "handleTell" );
        m_commandInterpreter.registerCommand( "!ban", acceptedMessages, this, "handleBan" );
        m_commandInterpreter.registerCommand( "!google", acceptedMessages, this, "handleGoogle" );
        m_commandInterpreter.registerCommand( "!wiki",  acceptedMessages, this, "handleWikipedia" );
        m_commandInterpreter.registerCommand( "!status", acceptedMessages, this, "handleStatus" );
        m_commandInterpreter.registerCommand( "!dictionary", acceptedMessages, this, "handleDictionary" );
        m_commandInterpreter.registerCommand( "!thesaurus", acceptedMessages, this, "handleThesaurus" );
        m_commandInterpreter.registerCommand( "!javadocs", acceptedMessages, this, "handleJavadocs" );
        
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
            BufferedReader in = new BufferedReader(new FileReader( m_botAction.getDataFile( "HelpResponses.txt" )));
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

            if( in != null ){
                in.close();
            }
        } catch( IOException e ){
            Tools.printStackTrace( e );
        }
    }

    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena( "#robopark" );
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Staff Chat" ) );
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
          String parts[] = message.toLowerCase().split( "@ad@" );
          String host = parts[0];
          String arena = parts[1];
          String advert = parts[2];

          storeAdvert( host, arena, advert );
          m_botAction.sendSmartPrivateMessage(host, "Advert for '"+arena+"' registered."); 
      } catch (Exception e ) {
    	  Tools.printStackTrace(e);
      }
    }
    
    // This is to catch any backgroundqueries even though none of them need to be catched to do something with the results
    public void handleEvent( SQLResultEvent event) {}

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

    public void handleAdvert ( String playerName, String message ){
        HelpRequest     helpRequest;

        lastHelpRequestName = playerName;

        if( opList.isBot( playerName ) ){
            return;
        }

        callList.addElement( new EventData( new java.util.Date().getTime() ) ); //For Records
        helpRequest = m_playerList.get( playerName.toLowerCase() );
        if( helpRequest == null ){
            helpRequest = new HelpRequest( playerName, null, null );
            m_playerList.put( playerName.toLowerCase(), helpRequest );
        }
        if( helpRequest.AdvertTell() == true ){
        	m_botAction.sendChatMessage( "NOTICE: " + playerName + " has used ?advert before. Please use !warn if needed." );
        }
        else {
        	m_botAction.sendRemotePrivateMessage( playerName, "Please do not use ?advert. "
                +"If you would like to request an event, please use the ?help command." );
        	helpRequest.setAdvertTell( true );
        	m_botAction.sendChatMessage( playerName + " has been notified that ?advert is not for non-staff." );
        }

    }

    public void handleCheater( String playerName, String message ) {
        HelpRequest     helpRequest;

        lastHelpRequestName = playerName;

        callList.addElement( new EventData( new java.util.Date().getTime() ) ); //For Records
        helpRequest = m_playerList.get( playerName.toLowerCase() );
        if( helpRequest == null ){
            helpRequest = new HelpRequest( playerName, message, null );
            m_playerList.put( playerName.toLowerCase(), helpRequest );
        }
    }

    public void handleHelp( String playerName, String message ){
        String[]        response;
        HelpRequest     helpRequest;

        if( playerName.compareTo( m_botAction.getBotName() ) == 0 ){
            return;
        }

        lastHelpRequestName = playerName;

        if( opList.isBot( playerName ) ){
            String tempMessage = "Staff members: Please use :" + m_botAction.getBotName() + ":!lookup instead of ?help!";

            response = new String[1];
            response[0] = tempMessage;

            m_botAction.sendRemotePrivateMessage( playerName, tempMessage );

            helpRequest = new HelpRequest( playerName, message, response );
            m_playerList.put( playerName.toLowerCase(), helpRequest );
            return;
        }

        callList.addElement( new EventData( new java.util.Date().getTime() ) ); //For Records
        response = search.search( message );

        helpRequest = m_playerList.get( playerName.toLowerCase() );
        if( helpRequest == null ){
            helpRequest = new HelpRequest( playerName, message, response );
            m_playerList.put( playerName.toLowerCase(), helpRequest );
        } else {
            helpRequest.setAllowSummons( false );
            helpRequest.setQuestion( message, response );
        }

        if( response.length <= 0 ){
            m_botAction.sendChatMessage( "..." );
        } else {
            m_botAction.sendChatMessage( "I'll take it!" );
            m_botAction.sendRemotePrivateMessage( playerName, helpRequest.getNextResponse() );

            if( helpRequest.hasMoreResponses() == false ){
                helpRequest.setAllowSummons( true );
                m_botAction.sendRemotePrivateMessage( playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!summon to request live help." );
            } else if( response.length > 0 ){
                m_botAction.sendRemotePrivateMessage( playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!next to retrieve the next response." );
            }
        }
    }

    public void handleNext( String playerName, String message ){
        String          response;
        HelpRequest     helpRequest;

        helpRequest = m_playerList.get( playerName.toLowerCase() );

        if( helpRequest == null ){
            m_botAction.sendRemotePrivateMessage( playerName, "If you have a question, ask with ?help <question>" );
        } else {
            if( helpRequest.isValidHelpRequest() == true ){
                response = helpRequest.getNextResponse();
                if( response == null ){
                    helpRequest.setAllowSummons( true );
                    m_botAction.sendRemotePrivateMessage( playerName, "I have no further information.  If you still need help, message me with !summon to get live help." );
                } else {
                    m_botAction.sendRemotePrivateMessage( playerName, response );
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

        helpRequest = m_playerList.get( playerName.toLowerCase() );

        if( helpRequest == null ){
            m_botAction.sendRemotePrivateMessage( playerName, "If you have a question, ask with ?help <question>" );
        } else {
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

        if( lastHelpRequestName == null ){
            m_botAction.sendRemotePrivateMessage( playerName, "No one has done a help call yet!" );
        }

        helpRequest = m_playerList.get( lastHelpRequestName.toLowerCase() );
        if( helpRequest == null ){
            m_botAction.sendRemotePrivateMessage( playerName, "No response was given." );
        } else {
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

        seperator = message.indexOf( ':' );
        if( seperator == -1 ){
            name = lastHelpRequestName;
            keyword = message;
            
            // If the ?help caller is a bot, try to extract the name from its message
            // cheater: (PubBot6) (Public 4): TK Report: 51xg35qas8ghc is reporting BadBoyTKer for intentional TK. (150 total TKs)
            // (Not racism calls, you don't want to do !tell onit on the one who is doing racist words)
            if(m_botAction.getOperatorList().isBotExact(name) && m_playerList.containsKey(lastHelpRequestName.toLowerCase())) { 
            	String lastHelpMessage = m_playerList.get(lastHelpRequestName.toLowerCase()).getQuestion();
            	
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

                m_botAction.sendChatMessage( "Use :" + m_botAction.getBotName()
                + ":!lookup <keyword> instead."  );
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
        if( opList.isOwner( name ) ){
            populateSearch();
            m_botAction.sendRemotePrivateMessage( name, "Reloaded." );
        }
    }

    public void handleWarn( String playerName, String message ){
        String          name;
        HelpRequest     helpRequest;

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
            helpRequest = m_playerList.get( name.toLowerCase() );
            if( helpRequest == null ){
                m_botAction.sendChatMessage( name + " hasn't done a help call yet." );
            } else {
                if( helpRequest.getBeenWarned() == true ) {
                    m_botAction.sendChatMessage( name + " has already been warned, no warning given." );
                } else {
                    helpRequest.setBeenWarned( true );
                    if( helpRequest.AdvertTell() == true ){
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
                        +"not be tolerated further!", 1 );
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
        HelpRequest     helpRequest;

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
            } else if( opList.isBot( name ) ){
                m_botAction.sendChatMessage( "Are you nuts?  You can't ban a staff member!" );
            } else {
                helpRequest = m_playerList.get( name.toLowerCase() );
                if( helpRequest == null ){
                    m_botAction.sendChatMessage( name + " hasn't done a help call yet." );
                } else {
                    if( helpRequest.getBeenWarned() == false ){
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
            helpRequest = m_playerList.get( name.toLowerCase() );
            if( helpRequest == null ){
                m_botAction.sendChatMessage( name + " hasn't done a help call yet." );
            } else {
                if( helpRequest.getFirstResponse() != null ) {
                    helpRequest.setAllowSummons( false );
                    m_botAction.sendRemotePrivateMessage( name, "I believe that the previous "
                            +"response you received was a sufficient answer to your question." );
                    m_botAction.sendRemotePrivateMessage( name, helpRequest.getFirstResponse() );
                    m_botAction.sendChatMessage( "The last response has been repeated to " + name );
                } else {
                    m_botAction.sendChatMessage( "Error repeating response to '" + name + "': response not found.  Please address this call manually.");
                }
            }
        }
    }

    /**
     * For strict onits, requiring the "on it" to be at the start of the message.
     * For strict gotits, requiring the "got it" to be at the start of the message.
     * 
     * @param name Name of person claiming a call by saying 'on it' or 'got it'
     * @param message Message the chat message
     */
    public void handleClaim( String name, String message) {
        boolean record = false;
        Date now = new Date();
        long callTime =0;
        
        // Clear the queue of expired calls, get the first non-expired call but leave other non-expired calls
        Iterator<EventData> iter = callList.iterator();
        while(iter.hasNext()) {
        	EventData e = iter.next();
        	
        	if( record == false && now.getTime() < e.getTime() + CALL_EXPIRATION_TIME ) {
        		// This is a non-expired call and no call has been counted yet. 
        		record = true;
        		callTime = e.getTime();
        		iter.remove();
        	} else if(now.getTime() >= e.getTime() + CALL_EXPIRATION_TIME) {
        		// This is an expired call
        		iter.remove();
        	}
        }
        
        // if a non-expired call was found, record it to the database
        if(record) {
        	// Save
        	if(message.startsWith("on it"))
        		updateStatRecordsONIT( name );
        	else if(message.startsWith("got it"))
        		updateStatRecordsGOTIT( name );
        	
            this.lastStafferClaimedCall = name;
            
            // Find the playername of the call that was claimed
            Iterator<HelpRequest> i = m_playerList.values().iterator();
            String player = "";
            while(i.hasNext()) {
            	HelpRequest helprequest = i.next();
            	if(helprequest.getTime() == callTime) {
            		player = helprequest.getPlayername();
            		break;
            	}
            }
            
            if(player.length()>0)
            	m_botAction.sendRemotePrivateMessage(name, "Call claim of the player '"+player+"' recorded.");
            else
            	m_botAction.sendRemotePrivateMessage(name, "Call claim recorded.");
            
        } else {
        	// A staffer did "on it" while there was no call to take (or all calls were expired).
        	if(this.lastStafferClaimedCall != null && this.lastStafferClaimedCall.length() > 0)
        		m_botAction.sendRemotePrivateMessage(name, "The call expired or no call found to match your claim. The last person to claim a call was "+this.lastStafferClaimedCall+".");
        	else
        		m_botAction.sendRemotePrivateMessage(name, "The call expired or no call found to match your claim.");
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

    public void updateStatRecordsONIT( String name ) {
    	if( !m_botAction.SQLisOperational())
            return;
    	
        try {
            String time = new SimpleDateFormat("yyyy-MM").format( Calendar.getInstance().getTime() ) + "-01";
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '"+name+"' AND fnType = 0 AND fdDate = '"+time+"'" );
            if(result.next()) {
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '"+name+"' AND fnType = 0 AND fdDate = '"+time+"'" );
            } else {
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('"+name+"', '1', '0', '"+time+"')" );
            }
            m_botAction.SQLClose( result );
        } catch ( Exception e ) {
        	//m_botAction.sendChatMessage(2, "Error occured when registering call claim from '"+name+"' :"+e.getMessage());
        	Tools.printStackTrace(e);
        }
    }

    public void updateStatRecordsGOTIT( String name ) {
        if( !m_botAction.SQLisOperational())
            return;

        try {
            String time = new SimpleDateFormat("yyyy-MM").format( Calendar.getInstance().getTime() ) + "-01";
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '"+name+"' AND fnType = 1 AND fdDate = '"+time+"'" );

            if(result.next()) {
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '"+name+"' AND fnType = 1 AND fdDate = '"+time+"'" );
            } else
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`)  VALUES ('"+name+"', '1', '1', '"+time+"')" );
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
            "                                             bans the last person.",
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
        if( m_botAction.getOperatorList().isBot( playerName ) )
            m_botAction.remotePrivateMessageSpam( playerName, helpText );
                
        String[] OwnerHelpText = {
            " !reload                                   - Reloads the HelpResponses database from file",            
        };
        if( m_botAction.getOperatorList().isOwner( playerName ))
        	m_botAction.remotePrivateMessageSpam( playerName, OwnerHelpText );
    }

    private int indexNotOf(String string, char target, int fromIndex)
    {
      for(int index = fromIndex; index < string.length() || index < 0; index++)
        if (string.charAt(index) != target)
          return index;
      return -1;
    }

    private int getBreakIndex(String string, int fromIndex)
    {
      if(fromIndex + LINE_SIZE > string.length())
        return string.length();
      int breakIndex = string.lastIndexOf((int) ' ', fromIndex + LINE_SIZE);
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
        	if (message.startsWith("on it") || message.startsWith("got it"))
        		handleClaim(event.getMessager(), message);
        }
    }

    class HelpRequest {

        long        m_time;
        String      m_question;
        String[]    m_responses;
        String      m_playerName;
        boolean     m_beenWarned;
        boolean     m_advertTell;
        int         m_nextResponse;
        boolean     m_allowSummons;

        public HelpRequest( String playerName, String question, String[] responses ){

            m_nextResponse = 0;
            m_beenWarned = false;
            m_advertTell = false;
            m_question = question;
            m_allowSummons = false;
            m_responses = responses;
            m_playerName = playerName;
            m_time = System.currentTimeMillis();
        }

        public void setQuestion( String question, String[] responses ){

            m_nextResponse = 0;
            m_question = question;
            m_responses = responses;
            m_time = System.currentTimeMillis();
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

        public String getFirstResponse(){

            if( m_responses == null ){
                return null;
            } else {
                if( m_responses.length != 0 )
                    return m_responses[0];
                else
                    return null;
            }
        }

        public String getLastResponse(){

            if( m_nextResponse <= 0 || m_responses == null ){
                return null;
            } else {
                return m_responses[m_nextResponse - 1];
            }
        }

        public String getNextResponse(){

            if( m_responses == null ){
                return null;
            }

            if( m_nextResponse >= m_responses.length ){
                return null;
            } else {
                return m_responses[m_nextResponse++];
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



