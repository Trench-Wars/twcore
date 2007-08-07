package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 *Modified the hotspots module with a better UI and fixed
 *the timer task errors. Should be working properly.
 */

public class utilhotspots extends MultiUtil {

    Vector <HotSpot>hotSpots;
    HotSpot watch;
    TimerTask changeTask;
    boolean watching;
    

    private int switchTime = 500;

    /**
     * initializes.
     */
    
    public void init() {
        hotSpots = new Vector<HotSpot>();
        // Turn off updating -- we will do it ourselves
        m_botAction.setPlayerPositionUpdating(0);
        watching = false;
    }
    
    /**
     * requests events.
     */
    
    public void requestEvents( ModuleEventRequester modEventReq ) {
    	modEventReq.request(this, EventRequester.PLAYER_POSITION );
    }

    /**
     * Handles messages
     */
    
    public void handleEvent( Message event ){

        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ))
                handleCommand( name, message.toLowerCase() );
        }
    }

    /**
     * handles commands
     * @param sender is the user of the bot.
     * @param message is the command.
     */
    
    public void handleCommand( String sender, String message ) {

        if( message.startsWith( "!addspot " ) ) 
        	do_addHotSpot( sender, message.substring( 9, message.length() ) ); 
        if( message.startsWith( "!prizespot " ) )
            do_addPrizeHotSpot( sender, message.substring( 11, message.length() ) );
        if( message.startsWith( "!switchtime " ) )
            do_switchTime( sender, message.substring( 12, message.length() ) );
        if (message.startsWith( "!listspots"))
        	do_ListSpot(sender);
        if( message.startsWith( "!clearspots" ) )
            do_clearHotSpots( sender );
        if( message.toLowerCase().startsWith( "!watch" ) ) {
        	if (!watching)	{
        		watch();
        		m_botAction.sendPrivateMessage(sender, "watching on");
            }
        	else
        		m_botAction.sendPrivateMessage(sender, "Already watching spots!");
        }	
    }

    /**
     * Handles player positions
     */
    
    public void handleEvent( PlayerPosition event ) {

        Player p = m_botAction.getPlayer( event.getPlayerID() );
        String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( watch != null )
                if( p != null )
                    if( watch.inside( p.getXLocation(), p.getYLocation() ) ) {
                        m_botAction.warpTo( name, watch.getX2(), watch.getY2() );
                        if( watch.getPrize() != -1 )
                            m_botAction.specificPrize( name, watch.getPrize() );
                    }
    }

    /**
     * initializes timer task.
     */
    
    public void watch()	{
    	changeTask = new TimerTask() {
            public void run() {
                try {
                    watch = hotSpots.elementAt(0);
                    hotSpots.removeElementAt(0);
                    hotSpots.addElement( watch );
                } catch (Exception e) {
                	m_botAction.sendPublicMessage("Concurrent Error!");
                }
                m_botAction.moveToTile(watch.getX(), watch.getY());
            }
        };
        m_botAction.scheduleTaskAtFixedRate( changeTask, 2000, switchTime );
        watching = true;
    }
    
    /**
     * Lists spots.
     * @param sender is the user of the bot
     */
    
    public void do_ListSpot(String sender)	{
    	Iterator<HotSpot> it = hotSpots.iterator();
    	if(!it.hasNext()) {m_botAction.sendPrivateMessage(sender,"No spots loaded!");return;}
    	while(it.hasNext())	{
    		m_botAction.sendPrivateMessage(sender, (it.next()).toString());
    	}
    		
    }
    
    /**
     * Adds a spot
     * @param sender is the user of the bot.
     * @param message is the hotspot.
     */
    
    public void do_addHotSpot( String sender, String message ) {

        String pieces[] = message.split( " " );
        if( pieces.length != 5 ) return;

        int values[] = new int[5];
        try {
            for( int i = 0; i < 5; i++ )
                values[i] = Integer.parseInt( pieces[i] );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( sender, "Input error.  Check and try again." );
            return;
        }

        hotSpots.add( new HotSpot( values ) );
        m_botAction.sendPrivateMessage( sender, "Hotspot added." );
    }

    /**
     * Adds a prized hotspot
     * @param sender is the user of the bot.
     * @param message is the hotspot.
     */
    
    public void do_addPrizeHotSpot( String sender, String message ) {

        String pieces[] = message.split( " " );
        if( pieces.length != 6 ) return;

        int values[] = new int[6];
        try {
            for( int i = 0; i < 6; i++ )
                values[i] = Integer.parseInt( pieces[i] );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( sender, "Input error.  Check and try again." );
            return;
        }

        hotSpots.add( new HotSpot( values ) );
        m_botAction.sendPrivateMessage( sender, "Prize hotspot added." );
    }

    /**
     * Clears all hotspots.
     * @param name
     */
    public void do_clearHotSpots( String name ) {
        hotSpots.clear();
        changeTask.cancel();
        watching = false;
        m_botAction.sendPrivateMessage( name, "All hotspots cleared." );
    }

    /**
     * Switches time and resets timer task to that interval.
     * @param sender is the user of the bot
     * @param message is the command
     */
    
    public void do_switchTime( String sender, String message ) {
        int time = switchTime;
        try {
            time = Integer.parseInt( message );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( sender, "Input error.  Need a number!" );
            return;
        }
        if( time < 200 ) {
            m_botAction.sendPrivateMessage( sender, "Time can not be less than 200ms." );
            return;
        }
        
        switchTime = time;
        if(watching)	{
        	changeTask.cancel();
            watch();
        }
        m_botAction.sendPrivateMessage( sender, "Switch time set to " + time );
    }

    /**
     * Returns help message
     */
    
    public String[] getHelpMessages() {
        String help[] = {
                "HotSpot Module          - Enter one coordinate, warp to another; maybe get a prize.'",
                "!addspot                - Adds a new hotspot.<warpx> <warpy> <radius> <destx> <desty>",
                "                        - Players will warp to the coord <destx>,<desty>",
                "                        - when they enter within <radius> of <warpx>,<warpy>.",
                "!prizespot              - Same as !addspot, but players are given <prize> after warping.",
                "                        - <warpx> <warpy> <radius> <destx> <desty> <prize>",
                "                        - Same as !addspot, but players are given <prize> after warping.",
                "!switchtime <ms>        - How long to watch each spot before moving to the next",
                "!watch                  - Begin watching all hotspots.",
                "!clearspots             - Remove all hotspots.",
                "!listspots              - Lists your hotspots"
        };
        return help;
    }

    public void cancel() {
        m_botAction.setPlayerPositionUpdating(5000);
    }
}

/**
 *Hot spot class that holds all related values
 */

class HotSpot {

    int x;
    int y;
    int r;
    int x2;
    int y2;
    int prize;

    public HotSpot( int values[] ) {
        if(values.length == 5) {
            x = values[0];
            y = values[1];
            r = values[2];
            x2 = values[3];
            y2 = values[4];
            prize = -1;
        } else {
            x = values[0];
            y = values[1];
            r = values[2];
            x2 = values[3];
            y2 = values[4];
            prize = values[5];
        }
    }

    public boolean inside( int playerX, int playerY ) {

        double dist = Math.sqrt( Math.pow( x*16 - playerX , 2 ) + Math.pow( y*16 - playerY , 2 ) );
        if( dist < r*16 ) return true;
        else return false;
    }
    
    public String toString()	{
    	if(prize == -1)
    		return ("X:" + x + " Y:" + y + " Radius:" + r + " destX:" + x2 + " desty:" + y2);
    	else
    		return ("X:" + x + " Y:" + y + " Radius:" + r + " destX:" + x2 + " desty:" + y2 + " Prize Number:" + prize);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getPrize() { return prize; }
}