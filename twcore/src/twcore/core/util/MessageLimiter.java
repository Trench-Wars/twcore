package twcore.core.util;

import java.util.HashMap;
import java.util.Iterator;

import twcore.core.BotAction;
import twcore.core.SubspaceBot;
import twcore.core.events.Message;

/**
 * Optional class for limiting the number of messages that can be sent to
 * a bot from a given player within 60 seconds.  (Does not apply to staff.)
 * Use BotAction's setMessageLimit(int) to run this class with a bot.
 */
public class MessageLimiter {
    private BotAction   m_botAction;    // Bot utility class
    private SubspaceBot m_bot;          // Bot the limiter applies to
    private HashMap <String,Integer>m_timeMap;  // (String)Name -> (Integer)# times msgd/last min.
    private int         m_rate = 6;     // # msgs allowed per player per minute
    private long        lastCheckTime;  // Last time anyone sent a msg
    private long        time;           // Time allowed per message (from m_rate)
    private boolean     m_staffImmune;  // True if staff are exempt from the limiter

    /**
     * Creates a new instance of MessageLimiter with the specified message
     * limit per minute per person.
     * @param botAction Bot utility class
     * @param bot Bot the limiter should apply to
     * @param rate Number of messages allowed from a person in one minute
     * @param staffImmunity True if staff are not subject to message limitations
     */
    public MessageLimiter( BotAction botAction, SubspaceBot bot, int rate, boolean staffImmunity ) {
        m_botAction = botAction;
        m_timeMap = new HashMap<String,Integer>();
        m_rate = rate + 1;
        m_bot = bot;
        lastCheckTime = System.currentTimeMillis();
        time = Math.round(1000d*60d/(double)m_rate);
        m_staffImmune = staffImmunity;
    }

    /**
     * Handles the message event before the bot so that the bot will not be
     * spammed.  If the PM passes the message limit test, it will go through;
     * otherwise, the player will receive a warning message and the bot will
     * not receive the message.
     * @param event Event/packet provided
     */
    public void handleEvent( Message event ){
        String name = null;
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            name = m_botAction.getPlayerName( event.getPlayerID() );
        } else if( event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE ){
            name = event.getMessager();
        } else {
            m_bot.handleEvent( event );
            return;
        }
        if( m_botAction.getOperatorList().isBot( name ) && m_staffImmune ) {
            m_bot.handleEvent( event );
            return;
        }

        recordTime( name );

        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastCheckTime;
        if( diff > time ){
            lastCheckTime = currentTime;
            reduce( diff );
        }

        Integer val = m_timeMap.get( name );
        if( val == null || val.intValue() < m_rate ){
            m_bot.handleEvent( event );
        }
    }

    /**
     * Reduces the count of messages from all players by the difference
     * supplied (should be difference between current time and last time
     * a message was received).
     * @param difference Difference between current time and last time a msg was received
     */
    public void reduce( long difference ){
        long times = difference/time;
        for( int j = 0; j < times && j < m_rate; j++ ){
            Iterator<String> i = m_timeMap.keySet().iterator();
            while( i.hasNext() ){
                String name = (String)i.next();
                Integer theInteger = m_timeMap.get( name );
                int intval = theInteger.intValue();
                if( intval == 1 || intval == 0 ){
                    i.remove();
                } else {
                    m_timeMap.put( name, new Integer( intval - 1 ));
                }
            }
        }
    }

    /**
     * Record the supplied player name as having sent a message at this time
     * by incrementing their message count.  If over the limit, warn them.
     * @param name
     */
    public void recordTime( String name ){
        if( m_timeMap.containsKey( name )){
            int value = m_timeMap.get( name ).intValue();
            if( value < m_rate ){
                m_timeMap.put( name,  new Integer( ++value ));
                if( value == m_rate ){
                    m_botAction.sendSmartPrivateMessage( name, "You have reached the message limit.  Please do not spam the bot." );
                }
            }
        } else {
            m_timeMap.put( name, new Integer( 1 ));
        }
    }
}
