/*
 * Session.java
 *
 * Created on December 12, 2001, 9:10 AM
 */
package twcore.core;
import java.io.*;
import java.net.*;
import java.util.*;

public class Session extends Thread {

    private Ship            m_ship;
    private String          m_name;
    private int             m_state;
    private ThreadGroup     m_group;
    private Timer           m_timer;
    private DatagramSocket  m_socket;
    private CoreData        m_coreData;
    private String          m_password;
    private BotAction       m_botAction;
    private EventRequester  m_requester;
    private Class           m_roboClass;
    private long            m_initialTime;
    private SubspaceBot     m_subspaceBot;
    private Arena           m_arenaTracker;
    private SSEncryption    m_ssEncryption;
    private Receiver        m_inboundQueue;
    private Sender          m_outboundQueue;
    
    private GamePacketGenerator     m_packetGenerator;
    private GamePacketInterpreter   m_packetInterpreter;
    private ReliablePacketHandler   m_reliablePacketHandler;

    public static int RUNNING = 2;
    public static int STARTING = 1;
    public static int NOT_RUNNING = 0;
    public static int INVALID_CLASS = (-1);

    public Session( CoreData cdata, Class roboClass, String name, String password, ThreadGroup parentGroup ){
        m_group = new ThreadGroup( parentGroup, name );
        m_requester = new EventRequester();
        m_roboClass = roboClass;
        m_coreData = cdata;
        m_name = name;
        m_state = STARTING;
        m_password = password;
        m_timer = new Timer();
    }

    public void prepare(){
        try {
            InetAddress inet = InetAddress.getByName( m_coreData.getServerName() );
            m_socket = new DatagramSocket();
            m_socket.connect( inet, m_coreData.getServerPort() );

            m_outboundQueue = new Sender( m_group, m_socket );
            m_inboundQueue = new Receiver( m_group, m_socket );
        } catch( Exception e ){
            Tools.printStackTrace( e );
            Tools.printLog( "Exited." );
            System.exit( 1 );
        }

        m_arenaTracker = new Arena();
        m_ssEncryption = new SSEncryption();
        m_packetGenerator = new GamePacketGenerator( m_outboundQueue, m_ssEncryption, m_timer );
        m_packetInterpreter =
            new GamePacketInterpreter(
                this,
                m_packetGenerator,
                m_ssEncryption,
                m_arenaTracker,
                m_name,
                m_password + "*" + m_coreData.getGeneralSettings().getString( "Sysop Password" ) );

        m_botAction = new BotAction( m_packetGenerator, m_arenaTracker, m_timer, this );
        m_reliablePacketHandler = new ReliablePacketHandler( m_packetGenerator, m_packetInterpreter, m_ssEncryption );

        m_packetInterpreter.setReliablePacketHandler( m_reliablePacketHandler );
        m_packetGenerator.setReliablePacketHandler( m_reliablePacketHandler );

        try {
            Class[] parameterTypes = { m_botAction.getClass() };
            Object[] args = { m_botAction };
            m_subspaceBot = (SubspaceBot)m_roboClass.getConstructor( parameterTypes ).newInstance( args );
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

        m_subspaceBot.handleDisconnect();
        Tools.printLog( m_name + " is disconnecting..." );

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
        Tools.printLog( m_name + " disconnected gracefully." );
        this.interrupt();

        if( m_timer != null ){
            m_timer.cancel();
            m_timer = null;
        }
    }

    public void loggedOn(){
        m_state = RUNNING;
        long time = System.currentTimeMillis() - m_initialTime;
        System.out.println( m_name + " logged in: " + time + " ms." );
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
            while( (m_state == RUNNING || m_state == STARTING) && !interrupted() ){
                currentTime = System.currentTimeMillis();

                if( m_state == STARTING ){
                    if( System.currentTimeMillis() - m_initialTime > 5000 ){
                        Tools.printLog( m_name + " failed to log in.  Login timed out." );
                        disconnect();
                        return;
                    }
                }

                if( m_outboundQueue.isConnected() == false || m_inboundQueue.isConnected() == false ){
                    disconnect();
                    return;
                }

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
                }

                Thread.sleep(5);
            }
        } catch( InterruptedException e ){
            Tools.printLog( "Session destroyed, all threads recovered for " + m_name );
            return;
        } catch( Exception e ){
            disconnect();
            Tools.printStackTrace( e );
        }
    }
}
