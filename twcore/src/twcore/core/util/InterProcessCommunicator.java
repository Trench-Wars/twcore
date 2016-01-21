package twcore.core.util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import twcore.core.SubspaceBot;
import twcore.core.events.InterProcessEvent;

/**
    Main class of the Inter-process Communication protocol of TWCore, handling routing
    of messages between bots along specific channels.  Also handles channel subscriptions,
    creation/deletion of channels, and firing of InterProcessEvents.

    @author  harvey
*/
public class InterProcessCommunicator {

    private Map<String, IPCChannel> channels;       // (String)Channel name -> IPCChannel

    /** Creates a new instance of InterProcessCommunicator */
    public InterProcessCommunicator() {
        channels = Collections.synchronizedMap(new HashMap<String, IPCChannel>());
    }

    /**
        Check if a channel exists.
        @param channelName Name of channel
        @return True if channel exists
    */

    public synchronized boolean channelExists( String channelName ) {
        return channels.containsKey( channelName );
    }

    /**
        Broadcast a message to a specific IPC channel.
        @param channelName Name of the channel to broadcast to
        @param senderName Name of message sender
        @param bot SubspaceBot object of bot executing command
        @param o Object, generally an IPCMessage, to transmit
    */

    public synchronized void broadcast( String channelName, String senderName,
                                        SubspaceBot bot, Object o ) {
        if( !channelExists( channelName )) {
            subscribe( channelName, bot );
        }

        InterProcessEvent event = new InterProcessEvent( senderName,
                channelName, o );
        IPCChannel channel = channels.get( channelName );
        channel.broadcast( event );
    }

    /**
        Given a SubspaceBot, return bot's subscribed channels.
        @param bot SubspaceBot in question
        @return String[] containing all subscribed channels
    */

    public synchronized String[] getSubscribedChannels( SubspaceBot bot ) {
        synchronized (channels) {
            Iterator<IPCChannel> i = channels.values().iterator();
            ArrayList<String> list = new ArrayList<String>();

            while( i.hasNext() ) {
                IPCChannel ipc = (IPCChannel)i.next();

                if( ipc.isSubscribed( bot )) {
                    list.add( ipc.getName() );
                }
            }

            return list.toArray( new String[ list.size() ]);
        }
    }

    /**
        Subscribe a bot to a given channel.  If the channel does not exist,
        it is created.
        @param channel String containing channel to join
        @param bot SubspaceBot to subscribe
    */

    public synchronized void subscribe( String channel, SubspaceBot bot ) {
        if( bot == null ) {
            Tools.printLog( "IPC Subscribe failed.  Please subscribe your bot "
                            + "to IPC in the LoggedOn handler, not in the constructor." );
            return;
        }

        if( !channelExists( channel )) {
            channels.put( channel, new IPCChannel( channel ));
        }

        IPCChannel ipcChan = channels.get( channel );

        if( !ipcChan.isSubscribed( bot )) {
            ipcChan.subscribe( bot );
        }
    }

    /**
        Unsubscribe a bot from a given channel.
        @param channel String containing channel to unsubscribe from
        @param bot SubspaceBot to unsubscribe
    */

    public synchronized void unSubscribe( String channel, SubspaceBot bot ) {
        if( !channelExists( channel )) return;

        channels.get( channel ).unsubscribe( bot );
    }

    /**
        Kill a given channel.
        @param channel String containing channel to kill.
    */

    public synchronized void destroy( String channel ) {
        channels.remove( channel );
    }

    /**
        Remove bot from all channels.
        @param bot SubspaceBot to unsubscribe.
    */

    public synchronized void removeFromAll( SubspaceBot bot ) {
        synchronized (channels) {
            Iterator<IPCChannel> i = channels.values().iterator();

            while( i.hasNext() ) {
                IPCChannel channel = (IPCChannel)i.next();
                channel.unsubscribe( bot );

                if ( channel.isEmpty() ) {
                    i.remove( );
                }
            }
        }
    }

    /**
        Internal class of InterProcessCommunicator, IPCChannel

        Representation of an IPC communications channel in the IPC message protocol.
    */
    class IPCChannel {
        private List<SubspaceBot> bots;
        private String channel;
        public IPCChannel( String channelName ) {
            bots = Collections.synchronizedList(new ArrayList<SubspaceBot>());
            channel = channelName;
        }

        /**
            Checks subscription status of a bot on this channel.
            @param bot SubspaceBot to check
            @return True if bot is subscribed to this channel
        */
        public boolean isSubscribed( SubspaceBot bot ) {
            return bots.contains( bot );
        }

        /**
            @return name of channel
        */
        public String getName() {
            return channel;
        }

        /**
            Broadcast an InterProcessEvent containing a message over this channel.
            @param e InterProcessEvent to broadcast
        */
        public void broadcast( InterProcessEvent e ) {
            synchronized (bots) {
                Iterator<SubspaceBot> i = bots.iterator();

                while( i.hasNext() ) {
                    (i.next()).handleEvent( e );
                }
            }
        }

        /**
            Subscribe a bot to this channel.
            @param bot SubspaceBot to subscribe
        */
        public void subscribe( SubspaceBot bot ) {
            if( !bots.contains( bot )) {
                bots.add( bot );
            }
        }

        /**
            Unsubscribe a bot from this channel.
            @param bot SubspaceBot to unsubscribe
        */
        public void unsubscribe( SubspaceBot bot ) {
            bots.remove( bot );
        }

        /**
            @return True if no bots are subscribed to this channel
        */
        public boolean isEmpty() {
            return bots.size() == 0;
        }

    }

}
