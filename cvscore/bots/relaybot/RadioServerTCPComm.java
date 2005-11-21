package twcore.bots.relaybot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;

public class RadioServerTCPComm extends Thread {
    
    static String protVer = RadioStatusServer.protVer;
    
    private DataOutputStream    out;
    private BufferedReader      in;
    private LinkedList          tracker;
    private Socket              sock;
    private boolean             running = true;
    private RadioStatusServer   radio;
    private boolean             authenticated = true;
    
    public RadioServerTCPComm( RadioStatusServer radio, Socket sock,
    LinkedList tracker) throws IOException{
        
        this.radio = radio;
        this.sock = sock;
        this.tracker = tracker;
        
        sock.setSoTimeout( 500 );
        sock.setKeepAlive( true );
        
        in = new BufferedReader( new InputStreamReader(
        sock.getInputStream() ));
        out = new DataOutputStream( sock.getOutputStream() );
        
    }

    public void run(){
        //Client has 500 milliseconds to reach OK status after the handshake.
        //before the client is dropped.
        
        long time = System.currentTimeMillis();
        
        //Wait for client to initiate contact...
        try{
            
            waitFor( protVer + ": I am docks radio client" );
            send( protVer + ": Server! I'm a server! Yay!" );
            //Wait for password
            String str;
            boolean admin = false;
            
            while( running ){
                
                str = in.readLine();
                
                if( !str.startsWith( protVer + ":pass:" )) continue;
                
                if( str.equals( protVer + ":pass:"
                + radio.getHashedPassword() )){
                    
                    send( protVer + ": Password Accepted" );
                    break;
                    
                } else if( str.equals( protVer + ":pass:"
                + radio.getHashedAdminPassword() )){
                    
                    send( protVer + ": Password Accepted" );
                    admin = true;
                    break;
                    
                } else {
                    
                    send( protVer + ": Access Denied" );
                    throw new IOException( "Invalid Password, disconnecting" );
                }
            }
            
            waitFor( protVer + ": Ok!" );
            
            sock.setSoTimeout( 10000 );
            
            if( admin ){
                radio.authenticateHost( sock.getInetAddress().getHostAddress(),
                "Radio God", "Radio Administrator" );
            }
            
            //Monitor for communications...
            str = in.readLine();
            
            while( str != null && running ){
                
                if( str.equals( protVer + ": Ok!" )){
                    send( protVer + ": Ok!" );
                } else if( str.startsWith( protVer + ":spm:" )){
                    send( protVer + ": Ok!" );
                    sendPrivateMessage( str.substring( 14 ));
                } else if( str.startsWith( protVer + ":arena:" )){
                    send( protVer + ": Ok!" );
                    sendArenaMessage( str.substring( 16 ));
                } else if( str.startsWith( protVer + ":whohost" )){
                    //send( protVer + ":host::"
                    //+ "Nobody" + "::" + "No Comment." );
                } else if( str.startsWith( protVer + ":botcmd:" )){
                    sendBotCommand( str.substring( 17 ));
                }
                
                str = in.readLine();
                
            }
            tracker.remove( this );
            
        } catch( IOException ioe ){
            tracker.remove( this );
            System.out.println( tracker.size() + " Clients, Disconnecting from "
            + sock.getInetAddress().getHostName() + ":" + sock.getPort() );
        }
    }
    
    public void sendBotCommand( String command ){
        if( !authenticated ) return;
        System.out.println( "Bot Command Support NOT implemented!: " + command );
    }
    
    public void sendArenaMessage( String protocol ){
        if( authenticated ){
            radio.getRadioBot().sendArenaMessage( protocol );
        }
    }
    
    public void sendPrivateMessage( String protocol ){
        if( authenticated ){
            String[] split = protocol.split( "::" );
            if( split.length != 2 ) return;
            radio.getRadioBot().sendSmartPrivateMessage( split[0], split[1] );
        }
    }
    
    public void waitFor( String wait ) throws IOException {
        
        int i = 0;
        String str = in.readLine();
        while( str != null && !str.startsWith( wait ) && running ){
            
            if( i == 2){
                sock.close();
                break;
            }
            str = in.readLine();
            i++;
            
        }
        if( str == null ) throw new IOException( "Die Die Die" );
    }
    
    public String getIP(){
        
        return sock.getInetAddress().getHostAddress();
    }
    
    public boolean isAuthenticated(){
        return authenticated;
    }
    
    public void setAuthenticated( boolean b ){
        authenticated = b;
    }
    
    public void die(){
        
        System.out.println( "Attempting to kill client connection..." );
        
        try{
            send( protVer + ":die" );
        } catch( IOException ioe ){}
        
        running = false;
    }
    
    public void send( String data ) throws IOException {
        
        out.writeUTF( "\n" + data + "\n" );
    }
}
