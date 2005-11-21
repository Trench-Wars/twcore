/*  Directions for adding into relaybot:
 *  a) Allow access to relaybot, then finish implementing
 *          RadioServerTCPComm.sendArenaMessage
 *          RadioServerTCPComm.sendPrivateMessage
 *  b) Add IP/Name verification to !host
 *  c) Map !host and !unhost to auth/unauth, protver + ":whohost"
 *  d) Forward all appropriate messages to RadioStatusServer for distribution
 *  e) Update the !help file
 *  f) Update RadioCenter to automatically connect to 24.187.121.81:7000
 *  g) Install and use a cool looking Look and Feel
 *  h) Create a web installer for it
 *  i) Install bot command protocol, test popups and polls
 */

package twcore.bots.relaybot;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class RadioStatusServer extends Thread {
    
    public static String protVer = "pokeme1.0";
    
    private ServerSocket        servsock;
    private relaybot            relaybot;
    private String              serverPass;
    private String              adminPass;
    private boolean             running = true;
    private HashMap             map = new HashMap();
    private Timer               timer = new Timer();
    private LinkedList          tracker = new LinkedList();
    
    /** Creates a new instance of RadioStatusServer */
    public RadioStatusServer( String serverPass, String adminPass,
    relaybot relaybot ) throws IOException {
        
        this.serverPass = serverPass;
        this.adminPass = adminPass;
        this.relaybot = relaybot;
        
        timer.scheduleAtFixedRate( new DecrementTask(), 20000, 20000 );
        
        servsock = new ServerSocket( 7000 );
        servsock.setSoTimeout( 10000 );
        System.out.println( "Socket bound, server started." );
        
        start();
    }
    
    public void run(){
        
        while( running ){
            RadioServerTCPComm rcomm = null;
            
            try{
                Socket socket = servsock.accept();

                String host = socket.getInetAddress().getHostName();
                
                if( !map.containsKey( host )){
                    map.put( host, new Integer( 1 ));
                } else if( ((Integer)map.get( host )).intValue() >= 2 ){
                    socket.close();
                    continue;
                } else {
                    map.put( host, new Integer(
                    ((Integer)map.get( host )).intValue() + 1 ));
                }
                
                rcomm =  new RadioServerTCPComm( this, socket, tracker );
                tracker.add( rcomm );
                
                System.out.println( tracker.size() + " Clients, Connecting to "
                + host + ":" + socket.getPort() );
                
                rcomm.start();
                
            } catch( SocketTimeoutException ste ){
                continue;
            } catch( IOException ioe ){
                System.out.println( "TCP Communication Error" );
                tracker.remove( rcomm );
            }
        }
    }
    
    public void die(){
        
        System.out.println( "Attempting to kill all connections..." );
        running = false;
        try{
            servsock.close();
        } catch( IOException ioe ){
            System.out.println( "Closed the block accept thread." );
        }
        for( Iterator i = tracker.iterator(); i.hasNext(); ){
            ((RadioServerTCPComm)i.next()).die();
            timer.cancel();
        }
    }
    
    public void sendPlayerCommand( String name, String command ) throws IOException{
        
        for( Iterator i = tracker.iterator(); i.hasNext(); ){
            ((RadioServerTCPComm)i.next()).send( protVer + ":cmd:" + name
            + "::" + command );
        }
    }
    
    public class DecrementTask extends TimerTask {
        
        public void run() {
            
            Iterator i = map.keySet().iterator();
            while( i.hasNext() ){
                
                String key = (String)i.next();
                Integer currentValue = (Integer)map.get( key );
                
                if( currentValue.intValue() <= 1 ){
                    i.remove();
                } else {
                    map.put( key, new Integer( currentValue.intValue() - 1 ));
                }
            }
        }
    }
    
    //Called by !host
    //Purpose: To only allow the person who is !host access to the send
    //functions of the relaybot.
    public void authenticateHost( String ip, String name, String comment )
    throws IOException {
        
        for( Iterator i = tracker.iterator(); i.hasNext(); ){
            
            RadioServerTCPComm rcomm = (RadioServerTCPComm)i.next();
            
            if( ip.equals( rcomm.getIP() ) || ip.equals( "192.168.1.1" )){
                rcomm.send( protVer + ":auth::" + name + "::" + comment );
                System.out.println( "Authenticating " + name + " on " + ip );
                rcomm.setAuthenticated( true );
            } else {
                rcomm.send( protVer + ":host::" + name + "::" + comment );
                System.out.println( "Notifying " + rcomm.getIP() );
            }
            
        }
    }
    
    //Called by !unhost
    public void unauthenticateHost() throws IOException{
        
        for( Iterator i = tracker.iterator(); i.hasNext(); ){
            
            RadioServerTCPComm rcomm = (RadioServerTCPComm)i.next();
            if( rcomm.isAuthenticated() ){
                rcomm.send( protVer + ":unauth" );
                rcomm.setAuthenticated( false );
            } else {
                rcomm.send( protVer + ":unhost" );
            }
        }
    }
    
    public relaybot getRadioBot(){
        
        return relaybot;
    }
    
    public String getHashedPassword(){
        
        return serverPass.hashCode() + "";
    }
    
    public String getHashedAdminPassword(){
        
        return adminPass.hashCode() + "";
    }
    
}
