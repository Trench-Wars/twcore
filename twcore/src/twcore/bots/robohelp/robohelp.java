package twcore.bots.robohelp;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.sql.*;
// import com.google.soap.search.*;
import twcore.core.*;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.IPCMessage;
import twcore.core.util.SearchableStructure;
import twcore.core.util.Tools;

public class robohelp extends SubspaceBot {
    static int TIME_BETWEEN_ADS = 390000;//6.5 * 60000;
    public static final int LINE_SIZE = 100;
    public static final int CALL_EXPIRATION_TIME = 90000;  // Time after which a call can't
                                                           // can't be claimed (onit/gotit)
    public static final String ZONE_CHANNEL = "Zone Channel";

    boolean             m_banPending = false;
    boolean             m_zonerbotForAdverts = true;
    boolean             m_strictOnIts = true;
    String              m_lastBanner = null;
    BotSettings         m_botSettings;

    SearchableStructure search;
    OperatorList        opList;
    TreeMap             rawData;
    Map                 m_playerList;

    CommandInterpreter  m_commandInterpreter;
    String              lastHelpRequestName = null;

    boolean             backupAdvertiser = false;

    final String        mySQLHost = "local";
    Vector              eventList = new Vector();
    TreeMap             events = new TreeMap();
    Vector              callList = new Vector();

    String				findPopulation = "";
	int					setPopID = -1;
	
	String	 			lastStafferClaimedCall;

    public robohelp( BotAction botAction ){
        super( botAction );

        m_botSettings = m_botAction.getBotSettings();

        populateSearch();
        opList = botAction.getOperatorList();
        m_playerList = Collections.synchronizedMap( new HashMap() );

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

        m_commandInterpreter.registerCommand( "!backupadv", acceptedMessages, this, "handleEnableBackup" );
        m_commandInterpreter.registerCommand( "!adv", acceptedMessages, this, "handleAdv" );
        m_commandInterpreter.registerCommand( "!time", acceptedMessages, this, "handleTime" );
        m_commandInterpreter.registerCommand( "!hosted", acceptedMessages, this, "handleDisplayHosted" );
        m_commandInterpreter.registerCommand( "!saychat", acceptedMessages, this, "handleSayChat" );

        acceptedMessages = Message.CHAT_MESSAGE;

        m_commandInterpreter.registerCommand( "!repeat", acceptedMessages, this, "handleRepeat" );
        m_commandInterpreter.registerCommand( "!warn", acceptedMessages, this, "handleWarn" );
        m_commandInterpreter.registerCommand( "!tell", acceptedMessages, this, "handleTell" );
        m_commandInterpreter.registerCommand( "!ban", acceptedMessages, this, "handleBan" );
//      m_commandInterpreter.registerCommand( "!google", acceptedMessages, this, "handleGoogle" );
        m_commandInterpreter.registerCommand( "!status", acceptedMessages, this, "handleStatus" );
        m_commandInterpreter.registerCommand( "!dictionary", acceptedMessages, this, "handleDictionary" );
        m_commandInterpreter.registerCommand( "!thesaurus", acceptedMessages, this, "handleThesaurus" );
        m_commandInterpreter.registerCommand( "!javadocs", acceptedMessages, this, "handleJavadocs" );
		if (!m_strictOnIts)
            m_commandInterpreter.registerDefaultCommand( acceptedMessages, this, "handleChat" );

        acceptedMessages = Message.ARENA_MESSAGE;

        m_commandInterpreter.registerCommand( "Ban", acceptedMessages, this, "handleBanNumber" );

        if(m_zonerbotForAdverts == false ) {
        	m_commandInterpreter.registerDefaultCommand( Message.ARENA_MESSAGE, this, "handleZone" );
        }
    }

    AdvertisementThread advertisementThread;

    class AdvertisementThread extends Thread{
        String advertisement;
        String name;
        long lastAdvTime = System.currentTimeMillis();// - 600000;

        public void setNameandMsg( String newName, String message ){
            message = message.trim();
            if( message.length() == 0 ){
            } else if( name == null ){
                name = newName;
                advertisement = message;
                m_botAction.sendRemotePrivateMessage( name, "Advertisement set to: " + message );
            } else if( name.equals( newName )){
                name = newName;
                advertisement = message;
                m_botAction.sendRemotePrivateMessage( name, "Advertisement changed to: " + message );
            } else {
                m_botAction.sendRemotePrivateMessage( name, "Sorry, but " + name + " has this ad." );
            }
            m_botAction.sendRemotePrivateMessage( name, timeToNext() );
        }

        public String timeToNext(){
            //10 minutes - difference betweeen current time and previous time
            long difference = TIME_BETWEEN_ADS - (System.currentTimeMillis() - lastAdvTime);
            int minutes = (int)Math.floor( difference / 60000 );
            int seconds = (int)Math.round( (difference % 60000)/1000 );
            return "There are " + minutes + " minutes, " + seconds + " seconds until the next advertisement is free";
        }

        public void run(){
            while( backupAdvertiser ){
                if( name != null && advertisement != null ){
                    m_botAction.sendZoneMessage( advertisement + " -" + name, 2 );
                    m_botAction.sendChatMessage( "The next advertisement is free." );
                    advertisement = null;
                    name = null;
                    lastAdvTime = System.currentTimeMillis();
                }
                try{
                    Thread.sleep( TIME_BETWEEN_ADS ); //10 minutes
                } catch( Exception e ){
                }
            }
        }

    }

    public void handleSayChat( String name, String message ) {
        if(!opList.isSmod( name )) {
	  return;
	  } else {
          m_botAction.sendChatMessage( 1, message );
	}
    }

    public void handleEnableBackup( String name, String message ){
        if( !opList.isSmod( name )) return;
        if( backupAdvertiser ){
            backupAdvertiser = false;
            m_botAction.sendRemotePrivateMessage( name, "Backup Advertisements Disabled" );
        } else {
            backupAdvertiser = true;
            advertisementThread = new AdvertisementThread();
            advertisementThread.start();
            m_botAction.sendRemotePrivateMessage( name, "Backup Advertisements Enabled" );
        }
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

/*    public void handleGoogle( String name, String message ){

        m_botAction.sendChatMessage( "Google search results for " + message + ": " + doGoogleSearch( message ) );

    }*/

    /**
     * Forms an appropriate query to dictionary.reference.com.
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleDictionary( String name, String message ) {

        m_botAction.sendChatMessage( "Dictionary definition:  http://dictionary.reference.com/search?q=" + message );

    }

    /**
     * Forms an appropriate query to thesaurus.reference.com.
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleThesaurus( String name, String message ) {

        m_botAction.sendChatMessage( "Thesaurus entry:  http://thesaurus.reference.com/search?q=" + message );

    }

    /**
     * Forms an appropriate query to javadocs.org
     * @param name Name of individual querying
     * @param message Query
     */
    public void handleJavadocs( String name, String message ) {

        m_botAction.sendChatMessage( "Javadocs entry:  http://javadocs.org/" + message );

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

        String        number;

        if( message.startsWith( "activated #" ) ){
            number = message.substring( message.indexOf( ' ' ) );
            if( m_banPending == true && number != null ){
                m_botAction.sendUnfilteredPublicMessage( "?bancomment " + number + " Ban by " + m_lastBanner + " for abusing the alert commands" );
                m_banPending = false;
                m_lastBanner = null;
            }
        }
    }

    public void handleAdv( String name, String message ){
        if( advertisementThread == null ) return;
        if( !opList.isER( name ) || !backupAdvertiser ) return;
        advertisementThread.setNameandMsg( name, message );
    }

    public void handleTime( String name, String message ){
        if( advertisementThread == null ) return;
        if( !opList.isER( name ) || !backupAdvertiser ) return;
        m_botAction.sendRemotePrivateMessage( name, advertisementThread.timeToNext() );
    }

    public void populateSearch(){
        search = new SearchableStructure();
        rawData = new TreeMap();
        try {
            BufferedReader in = new BufferedReader(new FileReader( m_botAction.getDataFile( "HelpResponses.txt" )));
            String line;
        int i = 0;
            do{
                line = in.readLine();
        try{
                    if ( line != null ){
                        line = line.trim();
                        int indexOfLine = line.indexOf( '|' );
                        if( indexOfLine != -1 ){
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
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Staff Chat" ) + "," + m_botAction.getGeneralSettings().getString( "Chat Name" ) );
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
      } catch (Exception e ) {
    	  Tools.printStackTrace(e);
      }
    }

    public void handleZone( String name, String message ) {
        if( !m_botAction.SQLisOperational() ){
            return;
        }
        try {
            String pieces[] = message.toLowerCase().split( " " );
            //Gets arena;
            String arena = "";
            for( int i = 0; i < pieces.length; i++ )
                if( pieces[i].equals( "?go" ) ) arena = pieces[Math.min( i+1, pieces.length)];
            for( int i = 0; i < arena.length(); i++ )
                if( !Character.isLetterOrDigit( arena.charAt( i ) ) && arena.charAt( i ) != '#' )
                    arena = arena.substring( 0, i );
            //Gets host;
            String host = "Unknown Host";
            int start = message.length()-1;
            for( int i = message.length()-1; i >= 0; i-- ) {
                String pName = message.substring( i, message.length() ).trim();
                if( opList.isZH( pName ) ) {
                    host = pName;
                    start = i;
                    i = -1;
                }
            }
            //Gets advert;
            for( int i = start; i >= 0; i-- )
                if( message.charAt( i ) == '-' ) {
                    start = i;
                    i = -1;
                }
            String advert = message.substring( 0, start ).trim();

            storeAdvert( host, arena, advert );
        } catch (Exception e ) {
        	Tools.printStackTrace(e);
        }

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
            String query = "INSERT INTO `tblAdvert` (`fnAdvertID`, `fcUserName`, `fcEventName`, `fcAdvert`, `fdTime`) VALUES ";
            query += "('', '"+Tools.addSlashesToString(host)+"', '"+arena+"', '"+Tools.addSlashesToString(advert)+"', '"+time+"')";
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
            EventData dat = (EventData)eventList.elementAt( i );
            if( dat.getTime() + span*3600000 > time ) {
                if( events.containsKey( dat.getArena() ) ) {
                    EventData d = (EventData)events.get( dat.getArena() );
                    d.inc();
                } else  events.put( dat.getArena(), new EventData( dat.getArena() ) );
            }
        }

        m_botAction.sendSmartPrivateMessage( name, "Events hosted within the last " + span + " hour(s): " );
        Set set = events.keySet();
        Iterator i = set.iterator();
        while (i.hasNext()) {
            String curEvent = (String)i.next();
            EventData d = (EventData)events.get( curEvent );
            m_botAction.sendSmartPrivateMessage( name, trimFill( curEvent ) + d.getDups() );
        }
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
        String[]        response2;
        HelpRequest     helpRequest;

        lastHelpRequestName = playerName;

        if( opList.isZH( playerName ) ){
            return;
        }

        callList.addElement( new EventData( new java.util.Date().getTime() ) ); //For Records
        helpRequest = (HelpRequest)m_playerList.get( playerName.toLowerCase() );
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



 //       m_botAction.sendRemotePrivateMessage( playerName, "WARNING: Do NOT use the ?advert "
 //               +"command.  It is for Staff Members only, and is punishable by a ban. Further abuse "
 //               +"will not be tolerated!", 1 );
 //       m_botAction.sendChatMessage( "NOTICE: "+ playerName + " has been warned for ?advert abuse." );
 //
 //       Calendar thisTime = Calendar.getInstance();
 //       java.util.Date day = thisTime.getTime();
 //       String warntime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( day );
 //       String[] paramNames = { "name", "warning", "staffmember", "timeofwarning" };
 //       String date = new java.sql.Date( System.currentTimeMillis() ).toString();
 //       String[] data = { playerName.toLowerCase().trim(), new String( warntime + ": Warning to " + playerName + " from Robohelp for ?advert abuse."), "RoboHelp", date };
 //
 //      m_botAction.SQLInsertInto( "local", "tblWarnings", paramNames, data );

    }

    public void handleCheater( String playerName, String message ){
        HelpRequest     helpRequest;

        lastHelpRequestName = playerName;

        callList.addElement( new EventData( new java.util.Date().getTime() ) ); //For Records
        helpRequest = (HelpRequest)m_playerList.get( playerName.toLowerCase() );
        if( helpRequest == null ){
            helpRequest = new HelpRequest( playerName, null, null );
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

        if( opList.isZH( playerName ) ){
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

        helpRequest = (HelpRequest)m_playerList.get( playerName.toLowerCase() );
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
            m_botAction.sendChatMessage( "On it!" );
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

        helpRequest = (HelpRequest)m_playerList.get( playerName.toLowerCase() );

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

        helpRequest = (HelpRequest)m_playerList.get( playerName.toLowerCase() );

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
        if (!opList.isZH(playerName))
          return;
        String[]        responses;
        HelpRequest     helpRequest;

        if( lastHelpRequestName == null ){
            m_botAction.sendRemotePrivateMessage( playerName, "No one has done a help call yet!" );
        }

        helpRequest = (HelpRequest)m_playerList.get( lastHelpRequestName.toLowerCase() );
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
        } else {
            name = message.substring( 0, seperator ).trim();
            keyword = message.substring( seperator + 1 ).trim();
        }

        if( name != null ){
            if( messager.toLowerCase().compareTo( name.toLowerCase() ) == 0 ||
                messager.toLowerCase().startsWith( name.toLowerCase() ) ){

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
            helpRequest = (HelpRequest)m_playerList.get( name.toLowerCase() );
            if( helpRequest == null ){
                m_botAction.sendChatMessage( name + " hasn't done a help call yet." );
            } else {
                if( helpRequest.getBeenWarned() == true ) {
                    m_botAction.sendChatMessage( "NOTICE: " + name + " has already been warned." );
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

                        m_botAction.SQLInsertInto( "local", "tblWarnings", paramNames, data );

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

                        m_botAction.SQLInsertInto( "local", "tblWarnings", paramNames, data );
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
            } else if( opList.isSmod( playerName ) && (opList.isZH( name ) && !opList.isSysop(name)) ){
                m_lastBanner = playerName;
                m_banPending = true;
                m_botAction.sendRemotePrivateMessage( name, "You have been banned for abuse as staff member. Depending on the Dean of Staff's decisions further action will be taken!" );
                m_botAction.sendUnfilteredPublicMessage( "?removeop " + name );
                m_botAction.sendUnfilteredPublicMessage( "?ban -a3 -e30 " + name );
                m_botAction.sendChatMessage( "Staffer \"" + name + "\" has been banned for abuse." );
                m_playerList.remove( name );
            } else if( opList.isZH( name ) ){
                m_botAction.sendChatMessage( "Are you nuts?  You can't ban a staff member!" );
            } else {
                helpRequest = (HelpRequest)m_playerList.get( name.toLowerCase() );
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
            helpRequest = (HelpRequest)m_playerList.get( name.toLowerCase() );
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
     * @param name Name of person saying on it
     * @param message Message containing on it
     */
    public void handleOnIt( String name, String message ) {
        boolean recorded = false;
        int i = 0;
        
        if(callList.size()==0) {
        	// A staffer did "on it" while there was no call to take.
        	if(this.lastStafferClaimedCall != null && this.lastStafferClaimedCall.length() > 0)
        		m_botAction.sendRemotePrivateMessage(name, "No call was found to match your claim. The last person to claim a call was "+this.lastStafferClaimedCall+".");
        	else 
        		m_botAction.sendRemotePrivateMessage(name, "No call was found to match your claim.");
        }
        
        while( !recorded && i < callList.size() ) {
            EventData e = (EventData)callList.elementAt( i );
            if( new java.util.Date().getTime() < e.getTime() + CALL_EXPIRATION_TIME ) {
                updateStatRecordsONIT( name );
                callList.removeElementAt( i );
                recorded = true;
                this.lastStafferClaimedCall = name;
            } else {
            	callList.removeElementAt( i );
            }
            i++;
        }
    }

    /**
     * For strict gotits, requiring the "got it" to be at the start of the message.
     * @param name Name of person saying got it
     * @param message Message containing got it
     */
    public void handleGotIt( String name, String message ) {
        boolean recorded = false;
        int i = 0;
        
        if(callList.size()==0) {
        	// A staffer did "on it" while there was no call to take.
        	if(this.lastStafferClaimedCall != null && this.lastStafferClaimedCall.length() > 0)
        		m_botAction.sendRemotePrivateMessage(name, "No call was found to match your claim. The last person to claim a call was "+this.lastStafferClaimedCall+".");
        	else 
        		m_botAction.sendRemotePrivateMessage(name, "No call was found to match your claim.");
        }
        
        while( !recorded && i < callList.size() ) {
            EventData e = (EventData)callList.elementAt( i );
            if( new java.util.Date().getTime() < e.getTime() + CALL_EXPIRATION_TIME ) {
                updateStatRecordsGOTIT( name );
                callList.removeElementAt( i );
                recorded = true;
            } else callList.removeElementAt( i );
            i++;
        }
    }

    // NOT USED
    /*public void handleChat( String name, String message ) {
        try {
            message = message.toLowerCase();
            boolean isON = false, isIT = false;
            for( int i = 0; i < message.length(); i++ ) {
                if( message.charAt( i ) == 'o' ) {
                    if( message.charAt( Math.min( i+1, message.length() - 1 ) ) == 'n' ) isON = true;
                }
                if( message.charAt( i ) == 'i' ) {
                    if( message.charAt( Math.min( i+1, message.length() - 1 ) ) == 't' ) isIT = true;
                }
            }

            boolean isGOT = false, isIT2 = false;
            for( int i = 0; i < message.length(); i++ ) {
                if( message.charAt( i ) == 'g' ) {
                    if( message.charAt( Math.min( i+1, message.length() - 1 ) ) == 'o' && message.charAt( Math.min( i+2, message.length() - 1 ) ) == 't') isGOT = true;
                }
                if( message.charAt( i ) == 'i' ) {
                    if( message.charAt( Math.min( i+1, message.length() - 1 ) ) == 't' ) isIT2 = true;
                }
            }

            if( isON && isIT && !name.toLowerCase().equals( m_botAction.getBotName().toLowerCase() ) ) {
                boolean recorded = false;
                int i = 0;
                while( !recorded && i < callList.size() ) {
                    EventData e = (EventData)callList.elementAt( i );
                    if( new java.util.Date().getTime() < e.getTime() + CALL_EXPIRATION_TIME ) {
                        updateStatRecordsONIT( name );
                        callList.removeElementAt( i );
                        recorded = true;
                    } else callList.removeElementAt( i );
                    i++;
                }
            }

            if( isGOT && isIT2 && !name.toLowerCase().equals( m_botAction.getBotName().toLowerCase() ) ) {
                boolean recorded = false;
                int i = 0;
                while( !recorded && i < callList.size() ) {
                    EventData e = (EventData)callList.elementAt( i );
                    if( new java.util.Date().getTime() < e.getTime() + CALL_EXPIRATION_TIME ) {
                        updateStatRecordsGOTIT( name );
                        callList.removeElementAt( i );
                        recorded = true;
                    } else callList.removeElementAt( i );
                    i++;
                }
            }

        } catch (Exception e) {}
    }*/

    public void updateStatRecordsONIT( String name ) {
        try {
            Calendar thisTime = Calendar.getInstance();
            java.util.Date day = thisTime.getTime();
            String time = new SimpleDateFormat("yyyy-MM").format( day ) + "-01";
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '"+name+"' AND fnType = 0 AND fdDate = '"+time+"'" );
            if(result.next()) {
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '"+name+"' AND fnType = 0 AND fdDate = '"+time+"'" );
            } else {
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('"+name+"', '1', '0', '"+time+"')" );
            }
            m_botAction.SQLClose( result );
        } catch ( Exception e ) {
            System.out.println( "Could not update Stat Records" );
        }
    }

    public void updateStatRecordsGOTIT( String name ) {
        if( !m_botAction.SQLisOperational() ){
            return;
        }

        try {
            Calendar thisTime = Calendar.getInstance();
            java.util.Date day = thisTime.getTime();
            String time = new SimpleDateFormat("yyyy-MM").format( day ) + "-01";
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '"+name+"' AND fnType = 1 AND fdDate = '"+time+"'" );

            if(result.next()) {
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '"+name+"' AND fnType = 1 AND fdDate = '"+time+"'" );
            } else 
                m_botAction.SQLBackgroundQuery( mySQLHost, null, "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`)  VALUES ('"+name+"', '1', '1', '"+time+"')" );
        } catch ( Exception e ) { System.out.println( "Could not update Stat Records" ); }
    }

    public void mainHelpScreen( String playerName, String message ){
        final String[] helpText = {
            "Commands given in the staff chat:",
            "!repeat <optional name> - Repeats the response to the specified name.  If no name is specified, the last response is repeated.",
            "!tell <name>:<keyword> - Private messages the specified name with the response to the keyword given.",
            "!warn <optional name> - Warns the specified player.  If no name is given, warns the last person.",
            "!ban <optional name> - Bans the specified player.  If no name is given, bans the last person.",
            "!status - Gives back status from systems.",
//            "!google search - Returns first page found by Googling the search term.",
            "!dictionary word - Returns a link for a definition of the word.",
            "!thesaurus word - Returns a link for a thesaurus entry for the word.",
            "!javadocs term - Returns a link for a javadocs lookup of the term.",
            "Commands sent via private message to me:",
            "!lookup <keyword> - Tells you the response when the specified key word is given",
            "!last <optional name> - Tells you what the response to the specified player was.  If no name is specified, the last response is given.",
            "!hosted <hours> - Displays the hosted events in the last specified <hours>, <hours> can be ommitted."
        };

        if( m_botAction.getOperatorList().isZH( playerName ) ){
            m_botAction.remotePrivateMessageSpam( playerName, helpText );
        }
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
      LinkedList formattedResp = new LinkedList();
      int startIndex = indexNotOf(response, ' ', 0);
      int breakIndex = getBreakIndex(response, 0);

      while(startIndex != -1)
      {
        formattedResp.add(response.substring(startIndex, breakIndex));
        startIndex = indexNotOf(response, ' ', breakIndex);
        breakIndex = breakIndex = getBreakIndex(response, startIndex);
      }
      return (String[]) formattedResp.toArray(new String[formattedResp.size()]);
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
        	String message = event.getMessage().toLowerCase();
        	if (message.startsWith("on it"))
        		handleOnIt(event.getMessager(), event.getMessage());
        	else if (message.startsWith("got it"))
        		handleGotIt(event.getMessager(), event.getMessage());
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
    }

}



