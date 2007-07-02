package twcore.core;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;

import twcore.core.game.Arena;
import twcore.core.game.Ship;
import twcore.core.net.GamePacketGenerator;
import twcore.core.net.GamePacketInterpreter;
import twcore.core.net.Receiver;
import twcore.core.net.ReliablePacketHandler;
import twcore.core.net.SSEncryption;
import twcore.core.net.Sender;
import twcore.core.util.Tools;

/**
 * Main class of a bot session, holding a reference to all pieces necessary
 * for internal/behind-the-scenes operation.  Instantiates SubspaceBot,
 * the class inherited by all external/front-end bots.
 */
public class Session extends Thread {

    private Ship            m_ship;
    private String          m_name;
    private int             m_state;
    private int				m_botNumber;
    private ThreadGroup     m_group;
    private Timer           m_timer;
    private DatagramSocket  m_socket;
    private PrintWriter		m_chatLog;
    private CoreData        m_coreData;
    private String          m_password;
    private BotAction       m_botAction;
    private EventRequester  m_requester;
    private Class<? extends SubspaceBot> m_roboClass;
    private long            m_initialTime;
    private SubspaceBot     m_subspaceBot;
    private Arena           m_arenaTracker;
    private SSEncryption    m_ssEncryption;
    private Receiver        m_inboundQueue;
    private Sender          m_outboundQueue;
    private String			m_ipAddress;
    private String			m_sysopPassword;
    private int				m_serverPort;

    private GamePacketGenerator     m_packetGenerator;
    private GamePacketInterpreter   m_packetInterpreter;
    private ReliablePacketHandler   m_reliablePacketHandler;

    public static int RUNNING = 2;
    public static int STARTING = 1;
    public static int NOT_RUNNING = 0;
    public static int INVALID_CLASS = (-1);

    public Session( CoreData cdata, Class<? extends SubspaceBot> roboClass, String name, String password, int botNum, ThreadGroup parentGroup ){
        m_group = new ThreadGroup( parentGroup, name );
        m_requester = new EventRequester();
        m_roboClass = roboClass;
        m_coreData = cdata;
        m_name = name;
        m_state = STARTING;
        m_password = password;
        m_botNumber = botNum;
        m_timer = new Timer();
        m_chatLog = null;
        m_ipAddress = m_coreData.getServerName();
        m_serverPort = m_coreData.getServerPort();
        m_sysopPassword = m_coreData.getGeneralSettings().getString( "Sysop Password" );
    }

    public Session( CoreData cdata, Class<? extends SubspaceBot> roboClass, String name, String password, int botNum, ThreadGroup parentGroup, String altIP, int altPort, String altSysop ){
        m_group = new ThreadGroup( parentGroup, name );
        m_requester = new EventRequester();
        m_roboClass = roboClass;
        m_coreData = cdata;
        m_name = name;
        m_state = STARTING;
        m_password = password;
        m_botNumber = botNum;
        m_timer = new Timer();
        m_chatLog = null;
        m_ipAddress = altIP;
        m_sysopPassword = altSysop;
        m_serverPort = altPort;
    }

    public void prepare(){
        try {
            InetAddress inet = InetAddress.getByName( m_ipAddress );
            m_socket = new DatagramSocket();
            m_socket.connect( inet, m_serverPort );

            m_outboundQueue = new Sender( m_group, m_socket );
            m_inboundQueue = new Receiver( m_group, m_socket );
        } catch( Exception e ){
            Tools.printStackTrace( e );
            Tools.printLog( "Exited." );
            System.exit( 1 );
        }

        m_ssEncryption = new SSEncryption();
        m_packetGenerator = new GamePacketGenerator( m_outboundQueue, m_ssEncryption, m_timer );
        m_arenaTracker = new Arena( m_packetGenerator, m_coreData );
        String login = m_password;
        if(m_sysopPassword.trim().length() > 0)
        	login += "*" + m_sysopPassword;

        m_packetInterpreter =
            new GamePacketInterpreter(
                this,
                m_packetGenerator,
                m_ssEncryption,
                m_arenaTracker,
                m_name,
                login );

        m_botAction = new BotAction( m_packetGenerator, m_arenaTracker, m_timer, m_botNumber, this );
        m_reliablePacketHandler = new ReliablePacketHandler( m_packetGenerator, m_packetInterpreter, m_ssEncryption );

        m_packetInterpreter.setReliablePacketHandler( m_reliablePacketHandler );
        m_packetGenerator.setReliablePacketHandler( m_reliablePacketHandler );

        try {
        	// Private Message logging
        	int logpvt = m_coreData.getGeneralSettings().getInt("LogPrivateMessages");
        	if(logpvt == 1)
        	{
	        	String botName = m_roboClass.getName();
	        	if (botName.indexOf(".") != -1 ) {
	            	botName = botName.substring(botName.lastIndexOf(".") + 1);
	        	}
	        	String filename = m_coreData.getGeneralSettings().getString("Core Location");
	        	filename += "/logs/"+ botName + ".log";
	        	m_chatLog = new PrintWriter(new FileWriter(filename, true));
	        }

            Class[] parameterTypes = { m_botAction.getClass() };
            Object[] args = { m_botAction };
            m_subspaceBot = m_roboClass.getConstructor( parameterTypes ).newInstance( args );
            m_ship = new Ship( m_group, m_packetGenerator );
            m_ship.start();
        } catch( Exception e ){
            Tools.printStackTrace( e );
        }
    }

    public BotAction getBotAction(){
        return m_botAction;
    }

    public GamePacketInterpreter getGamePacketInterpreter(){
        return m_packetInterpreter;
    }

    public GamePacketGenerator getGamePacketGenerator(){
        return m_packetGenerator;
    }

    public EventRequester getEventRequester(){
        return m_requester;
    }

    public CoreData getCoreData(){
        return m_coreData;
    }

    public int getBotState(){
        return m_state;
    }

    public String getBotName(){
        return m_name;
    }

    public Class getBotClass(){
        return m_roboClass;
    }

    public Ship getShip(){
        return m_ship;
    }

    public SubspaceBot getSubspaceBot(){
        return m_subspaceBot;
    }

    public void finalize(){
        Tools.printLog( m_name + " is going away." );
    }

    public int getBotNumber(){
    	return m_botNumber;
    }

    public PrintWriter getChatLog(){
    	return m_chatLog;
    }

    public void disconnect(){
        if( m_state == NOT_RUNNING ){
            return;
        }

        m_coreData.getInterProcessCommunicator().removeFromAll( m_subspaceBot );
        m_packetGenerator.sendDisconnect();
        try {
            Thread.sleep( 100 );
        } catch( InterruptedException e ){
        }

        m_state = NOT_RUNNING;
        String classname = m_subspaceBot.getClass().getSimpleName();
        Tools.printLog( m_name + " (" + classname + ") is disconnecting..." );
        m_subspaceBot.handleDisconnect();

		if(m_chatLog != null)
		{
        	m_chatLog.flush();
        	m_chatLog.close();
		}

        // Experimental fast disconnect mode destroys the socket immediately.
        if( m_coreData.getGeneralSettings().getInt("FastDisconnect") == 1 ) {
            m_group.interrupt();
            m_socket.close();
        // Standard behavior: 30 second shutdown procedure
        } else {
            // All threads in the group should not need to be interrupted.  This group includes
            // every bot spawned, plus BotQueue itself -- it's extremely odd that a bot disconnect
            // order requires that all other bot threads are inactive before executing the order.
            m_group.interrupt();
            while( m_group.activeCount() != 0 ){
                try {
                    m_group.interrupt();
                    Thread.sleep( 100 );
                } catch( InterruptedException except ){
                }
            }
            if( !m_group.isDestroyed() ){
                m_group.destroy();
            }

            m_socket.disconnect();
        }
        Tools.printLog( m_name + " (" + classname + ") disconnected gracefully." );
        this.interrupt();

        if( m_timer != null ){
            m_timer.cancel();
            m_timer = null;
        }
    }

    public void loggedOn(){
        m_state = RUNNING;
        long time = System.currentTimeMillis() - m_initialTime;
        Tools.printLog( m_name + " logged in: " + time + " ms." );
    }

    public void run(){
        prepare();
        m_initialTime = System.currentTimeMillis();
        long currentTime;
        long lastSyncTime = 0;
        final int SYNC_TIME = 30000;

        long lastResendTime = 0;
        final int RESEND_TIME = 750;

        long lastPacketTime = 0;
        final int TIMEOUT_DELAY = 60000;
        int clientKey = (int)(-Math.random() * Integer.MAX_VALUE);

        m_packetInterpreter.setSubspaceBot( m_subspaceBot );

        m_ssEncryption.setClientKey( clientKey );
        m_packetGenerator.sendClientKey( clientKey );

        lastPacketTime = System.currentTimeMillis();

        try {
        	while(m_state == STARTING && !interrupted()) {
                currentTime = System.currentTimeMillis();

                if( currentTime - m_initialTime > 5000 ){
                    Tools.printLog( m_name + " failed to log in.  Login timed out." );
                    disconnect();
                    return;
                }

                if( currentTime - lastResendTime > RESEND_TIME ){
                    lastResendTime = currentTime;
                    m_reliablePacketHandler.resendUnackedPacket();
                }

                if( m_inboundQueue.containsMoreElements() ){
                    lastPacketTime = currentTime;
                    m_packetInterpreter.translateGamePacket( m_inboundQueue.get(), false );
                } else {
	                if(!m_socket.isConnected()){
	                    disconnect();
	                    return;
	                }
	                Thread.sleep(5); //sleep if no packets waiting
                }
        	}

            while( m_state == RUNNING && !interrupted() ){
                currentTime = System.currentTimeMillis();

                if( currentTime - lastPacketTime > TIMEOUT_DELAY ){
                    disconnect();
                    return;
                }

                if( currentTime - lastSyncTime > SYNC_TIME ){
                    lastSyncTime = currentTime;
                    m_packetGenerator.sendSyncPacket( m_outboundQueue.getNumPacketsSent(), m_inboundQueue.getNumPacketsReceived() );
                }

                if( currentTime - lastResendTime > RESEND_TIME ){
                    lastResendTime = currentTime;
                    m_reliablePacketHandler.resendUnackedPacket();
                }

                if( m_inboundQueue.containsMoreElements() ){
                    lastPacketTime = currentTime;
                    m_packetInterpreter.translateGamePacket( m_inboundQueue.get(), false );
                } else {
	                if(!m_socket.isConnected()){
	                    disconnect();
	                    return;
	                }
	                Thread.sleep(5); //sleep if no packets waiting
                }
            }
        } catch( InterruptedException e ){
            // Printing a message here is not necessary, as interrupting a thread in order to
            // shut it down is a completely legitimate (and recommended) activity.  The
            // other alternative is to wait 30 seconds per bot while unneeded packets are
            // sent and received -- an exercise in futility.
            // Tools.printLog( "Session destroyed, all threads recovered for " + m_name );
            return;
        } catch( Exception e ){
            disconnect();
            Tools.printStackTrace( e );
        }
    }
}

