package twcore.core;

/*
 * InterProcessCommunicator.java
 *
 * Created on October 30, 2002, 7:53 AM
 */

/**
 *
 * @author  harvey
 */
import java.util.*;
public class InterProcessCommunicator {

    private Map channels;

    /** Creates a new instance of InterProcessCommunicator */
    public InterProcessCommunicator() {
        channels = Collections.synchronizedMap(new HashMap());
    }

    public synchronized boolean channelExists( String channelName ){
        return channels.containsKey( channelName );
    }

    public synchronized void broadcast( String channelName, String senderName,
    SubspaceBot bot, Object o ){
        if( !channelExists( channelName )){
            subscribe( channelName, bot );
        }
        InterProcessEvent event = new InterProcessEvent( senderName,
        channelName, o );
        IPCChannel channel = (IPCChannel)channels.get( channelName );
        channel.broadcast( event );
    }

    public synchronized String[] getSubscribedChannels( SubspaceBot bot ){
        synchronized (channels) {
            Iterator i = channels.values().iterator();
            ArrayList list = new ArrayList();
            while( i.hasNext() ){
                IPCChannel ipc = (IPCChannel)i.next();
                if( ipc.isSubscribed( bot )){
                    list.add( ipc.getName() );
                }
            }
            return (String[])list.toArray( new String[ list.size() ]);
        }
    }

    public synchronized void subscribe( String channel, SubspaceBot bot ){
        if( bot == null ){
            Tools.printLog( "IPC Subscribe failed.  Please subscribe your bot "
            + "to IPC in the LoggedOn handler, not in the constructor." );
            return;
        }
        if( !channelExists( channel )){
            channels.put( channel, new IPCChannel( channel ));
        }
        IPCChannel ipcChan = (IPCChannel)channels.get( channel );
        if( !ipcChan.isSubscribed( bot )){
            ipcChan.subscribe( bot );
        }
    }

    public synchronized void unSubscribe( String channel, SubspaceBot bot ){
        if( !channelExists( channel )) return;
        ((IPCChannel)channels.get( channel )).unsubscribe( bot );
    }

    public synchronized void destroy( String channel ){
        channels.remove( channel );
    }

    public synchronized void removeFromAll( SubspaceBot bot ){
        synchronized (channels) {
            Iterator i = channels.values().iterator();
            while( i.hasNext() ){
                IPCChannel channel = (IPCChannel)i.next();
                channel.unsubscribe( bot );
                if ( channel.isEmpty() ) {
                    i.remove( );
                }
            }
        }
    }

    class IPCChannel {
        private List bots;
        private String channel;
        public IPCChannel( String channelName ){
            bots = Collections.synchronizedList(new ArrayList());
            channel = channelName;
        }

        public boolean isSubscribed( SubspaceBot bot ){
            return bots.contains( bot );
        }

        public String getName(){
            return channel;
        }

        public void broadcast( InterProcessEvent e ){
            synchronized (bots) {
                Iterator i = bots.iterator();
                while( i.hasNext() ){
                    ((SubspaceBot)i.next()).handleEvent( e );
                }
            }
        }

        public void subscribe( SubspaceBot bot ){
            if( !bots.contains( bot )){
                bots.add( bot );
            }
        }

        public void unsubscribe( SubspaceBot bot ){
            bots.remove( bot );
        }

        public boolean isEmpty() {
            return bots.size() == 0;
        }

    }

}
