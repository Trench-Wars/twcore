package twcore.bots.relaybot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * To assist in hosting radio (while not requiring a host to have staff access). 
 */
public class relaybot extends SubspaceBot {
    private EventRequester m_req;
    private LinkedList loggedInList;
    private String currentPassword;
    private LinkedList alreadyZoned;
    private String currentHost = "", comment = "";
    private Poll currentPoll = null;
    private boolean announcing = false;
    private long timeStarted = System.currentTimeMillis();
    private RadioStatusServer serv;
    private boolean someoneHosting = false;

    /** Creates a new instance of relaybot */
    public relaybot( BotAction botAction ){
        super( botAction );
        m_req = botAction.getEventRequester();
        m_req.request( EventRequester.LOGGED_ON );
        m_req.request( EventRequester.MESSAGE );
        loggedInList = new LinkedList();
        alreadyZoned = new LinkedList();
        currentPassword = m_botAction.getBotSettings().getString( "ServPass" );
    }

    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena( "radio" );
        m_botAction.setMessageLimit( 8 );
        try{
            serv = new RadioStatusServer( currentPassword, "memorex", this );
        } catch( IOException ioe ){
            Tools.printLog( "Unable to start RadioBot server!" );
        }
    }

    public void sendArenaMessage( String arenaMsg ){
        m_botAction.sendArenaMessage( arenaMsg + " -" + currentHost, 2 );
    }

    public void sendSmartPrivateMessage( String name, String message ){
        m_botAction.sendSmartPrivateMessage( name + " -" + currentHost, message );
    }

    public void handleEvent( Message event ){
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){

            String name = m_botAction.getPlayerName( event.getPlayerID() );

            //Transmit data over the server...

            transmitPrivateMessageToBot( name, event.getMessage() );

            if( loggedInList.contains( name ) ){
                handleStaffMessage( name, event.getPlayerID(),
                event.getMessage() );
            }
            handlePrivateMessage( name, event.getPlayerID(),
            event.getMessage() );
            if( currentPoll != null ){
                currentPoll.handlePollCount( name, event.getMessage() );
            }
        } else if( event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE ){
            transmitPrivateMessageToBot( event.getMessager(), event.getMessage() );
        } else if( event.getMessageType() == Message.ARENA_MESSAGE ){
            handleInfoMessage( event.getMessage() );
        }
    }

    public void handleInfoMessage( String message ){
        if( message.startsWith( "IP:" ) ) {
            String[] pieces = message.split("  ");
            String ip = pieces[0].substring(3).trim();
            String name = pieces[3].substring(10).trim();
            System.out.println( "\"" + ip + "\"" );
            if( name.equals( currentHost )){
                try{
                    serv.authenticateHost( ip, name, comment );
                } catch( IOException ioe ){}
            }
        }
    }

    public void transmitPrivateMessageToBot( String name, String message ){
        if( message.startsWith( "!shoutout " )
        || message.startsWith( "!request " )
        || message.startsWith( "!topic " )
        || message.startsWith( "!question " ) ){
            try{
                serv.sendPlayerCommand( name, message );
            } catch( IOException ioe ){}
            m_botAction.sendSmartPrivateMessage( name, "Your message has been received." );
        }
    }

    public void handleStaffMessage( String name, int id, String message ){
        long time = System.currentTimeMillis();
        if( time >= 21600000 + timeStarted ){
            alreadyZoned.clear();
            timeStarted = time;
        }
        if( message.startsWith( "!help" )){
            m_botAction.privateMessageSpam( id, staffHelp );
        } else if( message.equals( "!host" )){
            if( !someoneHosting ){
                currentHost = name;
                comment = "No Comment.";
                m_botAction.sendArenaMessage( "Current Host: " + name + " ("
                + comment + ")" );

                if( !announcing ){
                    String[] str = {
                        "Current Host: " + name + " (" + comment + ")" };
                        m_botAction.scheduleTaskAtFixedRate(
                        new AnnounceTask( str ), 30000, 150000 );
                        m_botAction.scheduleTaskAtFixedRate(
                        new AnnounceTask( announcement ), 30000, 150000 );
                        announcing = true;
                }
                m_botAction.sendUnfilteredPrivateMessage( id, "*info" );
                someoneHosting = true;
            } else {
                m_botAction.sendPrivateMessage( id, "Sorry, you must !unhost "
                + "before you !host." );
            }
        } else if( message.startsWith( "!host " )){
            if( !someoneHosting ){
                comment = message.substring( 6 );
                currentHost = name;
                m_botAction.sendArenaMessage( "Current Host: " + name + " ("
                + comment + ")" );
                if( !announcing ){
                    String[] str = {
                        "Current Host: " + name + " (" + comment + ")" };
                        m_botAction.scheduleTaskAtFixedRate(
                        new AnnounceTask( str ), 30000, 150000 );
                        m_botAction.scheduleTaskAtFixedRate(
                        new AnnounceTask( announcement ), 30000, 150000 );
                        announcing = true;
                }
                m_botAction.sendUnfilteredPrivateMessage( id, "*info" );
                someoneHosting = true;
            } else {
                m_botAction.sendPrivateMessage( id, "Sorry, you must !unhost "
                + "before you !host." );
            }

        } else if( message.startsWith( "!unhost" )){
            if( someoneHosting ){
                m_botAction.cancelTasks();
                announcing = false;
                m_botAction.sendArenaMessage( currentHost + " has signed off the "
                + "radio, thank you for listening!" );
                currentHost = "";
                comment = "";
                someoneHosting = false;
                try{
                serv.unauthenticateHost();
                } catch( IOException ioe ){}
            } else {
                m_botAction.sendPrivateMessage( id, "There is no current host." );
            }
        } else if( message.startsWith( "!who" )){
            m_botAction.sendPrivateMessage( id, "Radio hosts who are logged "
            + "in:" );
            Iterator i = loggedInList.iterator();
            while( i.hasNext() ){
                m_botAction.sendPrivateMessage( id, (String)i.next() );
            }
        } else {
            handleCurrentHostOnly( name, id, message );
        }

    }

    public void handleCurrentHostOnly( String name, int id, String message ){
        if( !currentHost.equals( name )){
            if( message.startsWith( "!poll " )
            || message.startsWith( "!arena " )
            || message.startsWith( "!zone " )
            || message.startsWith( "!endpoll" )) {
                m_botAction.sendPrivateMessage( id, "Sorry, only the current "
                + "radio host may use that command.  If you are hosting, use "
                + "the !host command." );
            }
            return;
        }

        if( message.startsWith( "!poll " )){
            if( currentPoll != null ){
                m_botAction.sendPrivateMessage( name, "A poll is currently in "
                + "session.  End this poll before beginning another one." );
                return;
            }
            StringTokenizer izer = new StringTokenizer( message.substring( 6 ),
            ":" );
            int tokens = izer.countTokens();
            if( tokens < 2 ){
                m_botAction.sendPrivateMessage( id, "Sorry but the poll format "
                + "is wrong." );
                return;
            }

            String[] polls = new String[tokens];
            int i = 0;
            while( izer.hasMoreTokens() ){
                polls[i] = izer.nextToken();
                i++;
            }

            currentPoll = new Poll( polls );
        } else if( message.startsWith( "!endpoll" )){
            if( currentPoll == null ){
                m_botAction.sendPrivateMessage( id,
                "There is no poll running right now." );
            } else {
                currentPoll.endPoll();
                currentPoll = null;
            }

        } else if( message.startsWith( "!arena " )){
            m_botAction.sendArenaMessage( message.substring( 7 ) + " -"
            + name, 2 );
        } else if( message.startsWith( "!zone " )){
            if( alreadyZoned.contains( name )){
                m_botAction.sendPrivateMessage( id,
                "Sorry, you used your zone message today." );
            } else {
                m_botAction.sendZoneMessage( message.substring( 6 )
                + " -" + name, 2 );
                alreadyZoned.add( name );
            }
        }
    }


    public void handlePrivateMessage( String name, int id, String message ){
        if( message.startsWith( "!help" )){
            m_botAction.privateMessageSpam( id, pubHelp );
        } else if( message.startsWith( "!login " )){
            if( currentPassword.equals( message.substring( 7 ))){
                if( !loggedInList.contains( m_botAction.getPlayerName( id ))){
                    loggedInList.add( name );
                    m_botAction.sendPrivateMessage( id, "Login Successful" );
                }
            } else {
                m_botAction.sendPrivateMessage( id, "Incorrect password" );
            }
        }
    }

    public class Poll{

        private String[] poll;
        private int range;
        private HashMap votes;

        public Poll( String[] poll ){
            this.poll = poll;
            votes = new HashMap();
            range = poll.length - 1;
            m_botAction.sendArenaMessage( "Poll: " + poll[0] );
            for( int i = 1; i < poll.length; i++ ){
                m_botAction.sendArenaMessage( i + ": " + poll[i] );
            }
            m_botAction.sendArenaMessage(
            "Private message your answers to RadioBot" );
        }

        public void handlePollCount( String name, String message ){
            try{
                if( !Tools.isAllDigits( message )){
                    return;
                }
                int vote;
                try{
                    vote = Integer.parseInt( message );
                } catch( NumberFormatException nfe ){
                    m_botAction.sendSmartPrivateMessage( name, "Invalid vote.  "
                    + "Your vote must be a number corresponding to the choices "
                    + "in the poll." );
                    return;
                }

                if( !(vote > 0 && vote <= range )) {
                    return;
                }

                votes.put( name, new Integer( vote ));

                m_botAction.sendSmartPrivateMessage( name, "Your vote has been "
                + "counted." );

            } catch( Exception e ){
                m_botAction.sendArenaMessage( e.getMessage() );
                m_botAction.sendArenaMessage( e.getClass().getName() );
            }
        }

        public void endPoll(){
            m_botAction.sendArenaMessage( "The poll has ended! Question: "
            + poll[0] );

            int[] counters = new int[range+1];
            Iterator iterator = votes.values().iterator();
            while( iterator.hasNext() ){
                counters[((Integer)iterator.next()).intValue()]++;
            }
            for( int i = 1; i < counters.length; i++ ){
                m_botAction.sendArenaMessage( i + ". " + poll[i] + ": "
                + counters[i] );
            }
        }

    }

    public class AnnounceTask extends TimerTask {
        private String[] announcement;
        public AnnounceTask( String[] announcement ){
            this.announcement = announcement;
        }

        public void run() {
            for( int i = 0; i < announcement.length; i++ ){
                m_botAction.sendArenaMessage( announcement[i] );
            }
        }

    }

    public void handleDisconnect(){
        serv.die();
    }

    static String[] staffHelp = {
        "Radio Staff Help: ",
        "!who               - Shows who is currently logged into the bot",
        "!host <comment>    - Announces that you are hosting, allows you to use Current Radio Host Only commands",
        "!unhost            - Logs you out of the radio",
        "Current Radio Host Only",
        "!arena <message>   - Sends an arena message.",
        "!zone <message>    - Limited to once a day.",
        "!poll <Topic>:<answer1>:<answer2> (and on and on) - Starts a poll.",
        "!endpoll           - Ends a poll and tallies the results.",
        "If you abuse the green messages, or write something inappropriate, your radio hosting priviliges will be revoked."
    };

    static String[] pubHelp = {
        "Help: ",
        "!topic <topic>       - Sends your idea for a topic to the radio host.",
        "!request <artist>    - <title>  - Command to request a song.",
        "!question <question>  - Asks the radio host a question, to be answered the air.",
        "!shoutout <shoutout> - Sends your request for a shoutout to the radio host.",
        " -- ",
        "!login <password>    - If you are a current radio host, please log into the bot.",
        "If you would like to become a radio host, read this: http://forums.trenchwars.org/showthread.php?threadid=5060"
    };

    static String[] announcement = {
        "Trench Wars Radio! To listen, visit http://radio.trenchwars.org:8000 (Modem) http://radio.trenchwars.org:9000 (Cable/DSL) and click \"Listen\"",
        "If the high speed (9000) feed buffers too much for you, or doesn't work at all, try the low speed (8000) feed."
    };
}

