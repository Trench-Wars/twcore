/*
 * spreebot.java
 *
 * Created on May 10, 2003, 1:00 PM
 */

/**
 *
 * @author  Harvey
 */
package twcore.bots.spreebot;

import java.util.*;
import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;

public class spreebot extends SubspaceBot {
    HashMap<String, SpreeInfo> map;
    int spreeBegin = 3;
    int spreeInterval = 3;
    String largestSpreeName = "Noone";
    int largestSpree = 0;
    int announceTime = 5;
    long lastAnnounceTime;

    String  sMessages[] = {
        " On Fire!",
        " Killing Spree!",
        " Rampage!",
        " Dominating!",
        " Unstoppable!",
        " God Like!",
        " Cheater!",
    };

    /** Creates a new instance of spreebot */
    public spreebot( BotAction botAction ) {
        super( botAction );
        map = new HashMap<String, SpreeInfo>();
        EventRequester req = botAction.getEventRequester();
        req.request( EventRequester.MESSAGE );
        req.request( EventRequester.PLAYER_ENTERED );
        req.request( EventRequester.PLAYER_DEATH );
    }

    public void handleEvent( LoggedOn event ){
        m_botAction.joinArena( "spree" );
        lastAnnounceTime = System.currentTimeMillis();
        SummaryTask task = new SummaryTask();
        int spaminterval = 60000 * announceTime;
        m_botAction.scheduleTaskAtFixedRate( task, spaminterval, spaminterval );
        m_botAction.scheduleTaskAtFixedRate( new ReminderTask(),
        spaminterval - 60000, spaminterval );
    }

    public void handleEvent( PlayerEntered event ){
        System.out.println( "Added a spree info:" + event.getPlayerName() );
        map.put( event.getPlayerName(), new SpreeInfo() );
    }

    public void handleEvent( PlayerDeath event ){
        String killerName = m_botAction.getPlayerName( event.getKillerID() );
        String killeeName = m_botAction.getPlayerName( event.getKilleeID() );
        SpreeInfo info = (SpreeInfo)map.get( killerName );
        info.incrementTotalKills();
        info.incrementKillsInARow();
        SpreeInfo info2 = (SpreeInfo)map.get( killeeName );
        String message = " On Fire!";
        if( isNewSpree( info.getKillsInARow() )){
            m_botAction.sendArenaMessage( "Streak!: " + killerName + " (" +
            + info.getKillsInARow() + ":0) " + message );

            if( info.getKillsInARow() > largestSpree ){
                largestSpreeName = killerName;
                largestSpree = info.getKillsInARow();
                m_botAction.sendArenaMessage( largestSpreeName
                + " now has the largest winning streak for this period!", 2 );
            }
        }

        if( isNewSpree( info2.getKillsInARow() ) ){
            m_botAction.sendArenaMessage( killerName + " ruined " + killeeName + "'s spree of "
            + info2.getKillsInARow());
        }

        info2.reset();
    }

    public boolean isNewSpree( int spree ){
        spree -= spreeBegin;
        if( spree == 0 ) return true;
        else if( spree < 0 ) return false;
        else {
            return( spree % spreeInterval == 0 );
        }
    }

    public void handleEvent( Message event ){
        long temp = lastAnnounceTime + 60000 * announceTime;
        temp -= System.currentTimeMillis();
        temp /= 1000;

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            if( event.getMessage().equalsIgnoreCase( "!time" )){
                m_botAction.sendPrivateMessage( event.getPlayerID(),
                "The next announcement will occur in " + temp/60 + " minutes "
                + temp%60 + " seconds." );
            } else {
                m_botAction.sendPrivateMessage( event.getPlayerID(),
                "Type !time to get the time of the next announcement" );
            }
        }
    }

    class SummaryTask extends TimerTask {
        public SummaryTask(){
        }

        public void run() {
            m_botAction.sendArenaMessage( "--==-- Announcement --==-- ", 2 );
            m_botAction.sendArenaMessage( "The longest winning streak this period was done by " + largestSpreeName );
            m_botAction.sendArenaMessage( largestSpreeName + " had " + largestSpree + " kills in a row, and has received a Super! prize." );
            //m_botAction.sendArenaMessage(  );

            Iterator keyIterator = map.keySet().iterator();
            String topKillName;
            int topKillNumber = 0;
            int leastKillNumber = Integer.MAX_VALUE;
            String leastKillName = "";
            while( keyIterator.hasNext() ){
                String name = (String)keyIterator.next();
                SpreeInfo info = (SpreeInfo)map.get( name );
                if( info.getTotalKills() > topKillNumber ){
                    topKillNumber = info.getTotalKills();
                    topKillName = name;
                }
                if( info.getTotalKills() < leastKillNumber ){
                    leastKillNumber = info.getTotalKills();
                    leastKillName = name;
                }
            }
            keyIterator = map.keySet().iterator();

            while( keyIterator.hasNext() ){
                String name = (String)keyIterator.next();
                SpreeInfo info = (SpreeInfo)map.get( name );
                if( info.getTotalKills() == topKillNumber ){
                    m_botAction.sendArenaMessage( "The highest total number of kills was "
                    + leastKillNumber + " by " + name );
                }
            }
            m_botAction.sendArenaMessage( "The lowest total number of kills was "
            + leastKillNumber + " by " + leastKillName );
            m_botAction.sendArenaMessage( leastKillName + " gets an engine shutdown!" );
            m_botAction.sendArenaMessage( "The next " + announceTime + " minute period has begun!" );
            m_botAction.sendUnfilteredPrivateMessage( largestSpreeName, "*prize #17" );
            m_botAction.sendUnfilteredPrivateMessage( leastKillName, "*prize #-14" );
            //fix stuff
            largestSpreeName = "Noone";
            largestSpree = 0;
            lastAnnounceTime = System.currentTimeMillis();
        }
    }

    class ReminderTask extends TimerTask {
        public ReminderTask(){
        }

        public void run() {
            m_botAction.sendArenaMessage( "The next announcement will occur in ONE minute!", 2 );
        }
    }


    static class SpreeInfo{
        int killsInARow = 0;
        int totalKills = 0;
        public SpreeInfo(){
        }

        public void incrementTotalKills(){
            totalKills++;
        }
        public void incrementKillsInARow(){
            killsInARow++;
        }

        public int getKillsInARow(){
            return killsInARow;
        }

        public int getTotalKills(){
            return totalKills;
        }

        public void reset(){
            killsInARow = 0;
            totalKills = 0;
        }
    }
}
