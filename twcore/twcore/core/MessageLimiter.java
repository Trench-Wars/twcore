package twcore.core;

import java.util.*;
public class MessageLimiter {
    private BotAction m_botAction;
    private SubspaceBot m_bot;
    private HashMap m_timeMap;
    private int m_rate = 6;
    private long lastCheckTime;
    private long time;
    /** Creates a new instance of MessageLimiter */
    public MessageLimiter( BotAction botAction, SubspaceBot bot, int rate ) {
        m_botAction = botAction;
        m_timeMap = new HashMap();
        m_rate = rate + 1;
        m_bot = bot;
        lastCheckTime = System.currentTimeMillis();
        time = Math.round(1000d*60d/(double)m_rate);
    }

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
        
        recordTime( name );
        
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastCheckTime;
        if( diff > time ){
            lastCheckTime = currentTime;
            reduce( diff );
        }
        
        Integer val = (Integer)m_timeMap.get( name );
        if( val == null || val.intValue() < m_rate ){
            m_bot.handleEvent( event );
        }
    }
    
    public void reduce( long difference ){
        long times = difference/time;
        for( int j = 0; j < times && j < m_rate; j++ ){
            Iterator i = m_timeMap.keySet().iterator();
            while( i.hasNext() ){
                String name = (String)i.next();
                Integer theInteger = (Integer)m_timeMap.get( name );
                int intval = theInteger.intValue();
                if( intval == 1 || intval == 0 ){
                    i.remove();
                } else {
                    m_timeMap.put( name, new Integer( intval - 1 ));
                }
            }
        }
    }
    
    public void recordTime( String name ){
        if( m_timeMap.containsKey( name )){
            int value = ((Integer)m_timeMap.get( name )).intValue();
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
