package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import twcore.core.OperatorList;
import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 *Modified the hotspots module with a better UI and fixed
 *the timer task errors. Should be working properly.
 */
public class utilhotspots extends MultiUtil {

	OperatorList opList;
	ArrayList<String> recentContacts;
    Vector <HotSpot>hotSpots;
    HotSpot watch;
    TimerTask changeTask;
    boolean watching;
    int repeatTime = 3000;
    
    private int switchTime = 500;

    /**
     * initializes.
     */
    public void init() {
    	recentContacts = new ArrayList<String>();
    	opList = m_botAction.getOperatorList();
        hotSpots = new Vector<HotSpot>();
        // Turn off updating -- we will do it ourselves
        m_botAction.setPlayerPositionUpdating(0);
        m_botAction.stopSpectatingPlayer();
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
                handleCommand( name, message );
        }
    }

    /**
     * handles commands
     * @param sender is the user of the bot.
     * @param message is the command.
     */
    public void handleCommand( String sender, String message ) {
    	String msg = message;
    	message = message.toLowerCase();
        if( message.startsWith( "!addspot " ) )
        	do_addHotSpot( sender, message.substring( 9, message.length() ) );
        if( message.startsWith( "!addmsg " ) )
        	do_addMessage( sender, msg.substring( 8 ));
        if( message.startsWith( "!removemsg " ) )
        	do_removeMessage( sender, message.substring( 11 ));
        if( message.startsWith( "!listmsg " ) )
        	do_listMessages( sender, message.substring( 9 ));
        if( message.startsWith( "!switchtime " ) )
            do_switchTime( sender, message.substring( 12, message.length() ) );
        if( message.startsWith( "!repeattime " ) )
            do_repeatTime( sender, message.substring( 12 ) );
        if (message.startsWith( "!listspots"))
        	do_ListSpot(sender);
        if( message.startsWith( "!clearspots" ) )
            do_clearHotSpots( sender );
        if( message.toLowerCase().startsWith( "!watch" ) ) {
        	if (!watching)	{
        		watch();
        		m_botAction.sendPrivateMessage(sender, "Watching [ON]");
            }
        	else
        		m_botAction.sendPrivateMessage(sender, "Already watching spots!");
        }
    }

    /**
     * Handles player positions
     */
    public void handleEvent( PlayerPosition event ) {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        if(name == null || recentContacts.contains(name))return;
        new ContactTimer(name, repeatTime);
            if( watch != null ) {
                if( watch.inside( event.getXLocation(), event.getYLocation() )) {
                	//if( watch.needsWarp() )
                		//m_botAction.warpTo( event.getPlayerID(), watch.getX2(), watch.getY2() );
                	if( watch.getMessages() != null){
                    	Iterator<String> i = watch.getMessages().iterator();
                    	while( i.hasNext() ){
                    		String msg = i.next();
                    		msg = replaceKeys(name, msg);
                    		m_botAction.sendUnfilteredPrivateMessage( event.getPlayerID(), msg );
                    	}
                    }
                }
            }
    }

    /**
	 * Replaces escape keys when messaging a user who has used this command
	 * @param name - The user accessing this command
	 * @param message - The message/action to be sent to the user
	 * @return A string with objects instead of escape keys
	 */
	public String replaceKeys(String name, String message){
		Player p = m_botAction.getPlayer(name);
		if(message.contains("&player"))
			message = message.replace("&player", name);
		if(p == null)return message;
		if(message.contains("&freq")){
			message = message.replace("&freq", p.getFrequency() + "");			
		}
		if(message.contains("&ship")){
			message = message.replace("&ship", Tools.shipName(p.getShipType()));
		}
		if(message.contains("&shipslang")){
			message = message.replace("&shipslang", Tools.shipNameSlang(p.getShipType()));
		}
		if(message.contains("&wins")){
			message = message.replace("&wins", p.getWins() + "");
		}
		if(message.contains("&losses")){
			message = message.replace("&losses", p.getLosses() + "");
		}
		if(message.contains("&bounty")){
			message = message.replace("&bounty", p.getBounty() + "");
		}
		if(message.contains("&squad")){
			message = message.replace("&squad", p.getSquadName());
		}
		if(message.contains("&x")){
			message = message.replace("&x", p.getXLocation()/16 + "");
		}
		if(message.contains("&y")){
			message = message.replace("&y", p.getYLocation()/16 + "");
		}			
		//TODO: Feel free to add more escape keys.
		return message;
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
    		HotSpot hs = it.next();
    		m_botAction.sendPrivateMessage(sender, hotSpots.indexOf(hs) + ") " + hs.toString());
    	}

    }
    
    /**
     * Adds a message to a spot
     * @param sender is the user of the bot
     * @param message is the index of the spot and the message to add
     */   
    public void do_addMessage( String sender, String message ) {
    	String spot = message.substring(0, 1);
    	if(message.indexOf(" ") == -1)return;
    	String msg = message.substring(message.indexOf(" "));
    	
    	
    	int index;
    	try {
    		index = Integer.parseInt( spot );
    	} catch( Exception e ) {
    		m_botAction.sendPrivateMessage( sender, "Input error.  Check and try again." );
            return;
    	}
    	if(index >= hotSpots.size() || index < 0){
    		m_botAction.sendPrivateMessage( sender, "The given HotSpot does not exist.");
    		return;
    	}
    	if(msg.startsWith("*")){
			if(!isAllowed(msg) && !opList.isSmod(sender)){
				m_botAction.sendSmartPrivateMessage( sender, "Command not added; Restricted or unknown.");
				return;
			}
		}
    	hotSpots.elementAt(index).addMessage(msg);
    	m_botAction.sendSmartPrivateMessage( sender, "Message added to HotSpot at index " + index );
    }
    
    /**
	 * A white-list of allowed custom commands.
	 * @param s - The string
	 * @return true if the string is allowed. else false.
	 */
	public boolean isAllowed(String s){
		if(s.startsWith("*setship")   ||
		   s.startsWith("*setfreq")   ||
		   s.startsWith("*warpto")    ||
		   s.equals("*scorereset")    ||
		   s.equals("*spec")          ||
		   s.equals("*prize #4")      ||//Stealth
		   s.equals("*prize #5")      ||//Cloak
		   s.equals("*prize #6")      ||//X-radar
		   s.equals("*prize #7")      ||//Warp
		   s.equals("*prize #13")     ||//Full charge
		   s.equals("*prize #14")     ||//Engine shutdown
		   s.equals("*prize #15")     ||//Multi-fire
		   s.equals("*prize #17")     ||//Super
		   s.equals("*prize #18")     ||//Shields
		   s.equals("*prize #19")     ||//Shrapnel
		   s.equals("*prize #20")     ||//Anti-warp
		   s.equals("*prize #21")     ||//Repel
		   s.equals("*prize #22")     ||//Burst
		   s.equals("*prize #23")     ||//Decoy
		   s.equals("*prize #24")     ||//Thor
		   s.equals("*prize #25")     ||//Multi-prize
		   s.equals("*prize #26")     ||//Brick
		   s.equals("*prize #27")     ||//Rocket
		   s.equals("*prize #28")     ||//Portal
		   s.equals("*prize #-4")     ||//Negative Stealth
		   s.equals("*prize #-5")     ||//Negative Cloak
		   s.equals("*prize #-6")     ||//Negative X-radar
		   s.equals("*prize #-7")     ||//Negative Warp
		   s.equals("*prize #-13")    ||//Negative Full charge
		   s.equals("*prize #-14")    ||//Negative Engine shutdown
		   s.equals("*prize #-15")    ||//Negative Multi-fire
		   s.equals("*prize #-17")    ||//Negative Super
		   s.equals("*prize #-18")    ||//Negative Shields
		   s.equals("*prize #-19")    ||//Negative Shrapnel
		   s.equals("*prize #-20")    ||//Negative Anti-warp
		   s.equals("*prize #-21")    ||//Negative Repel
		   s.equals("*prize #-22")    ||//Negative Burst
		   s.equals("*prize #-23")    ||//Negative Decoy
		   s.equals("*prize #-24")    ||//Negative Thor
		   s.equals("*prize #-25")    ||//Negative Multi-prize
		   s.equals("*prize #-26")    ||//Negative Brick
		   s.equals("*prize #-27")    ||//Negative Rocket
		   s.equals("*prize #-28"))     //Negative Portal
		return true;
		else return false;
	}
    
    /**
     * Removes a message from a spot
     * @param sender is the user of the bot
     * @param message is the index of the spot and the message to add
     */
    public void do_removeMessage( String sender, String message ) {

    	 String pieces[] = message.split( " " );
         if( pieces.length != 2 ) return;

         int values[] = new int[2];
         try {
             for( int i = 0; i < 2; i++ )
                 values[i] = Integer.parseInt( pieces[i] );
         } catch (Exception e) {
             m_botAction.sendPrivateMessage( sender, "Input error.  Check and try again." );
             return;
         }
         if(values[0] >= hotSpots.size() || values[0] < 0){
     		m_botAction.sendPrivateMessage( sender, "The given HotSpot does not exist.");
     		return;
     	 }
         if(hotSpots.elementAt(values[0]).getMessage(values[1]) == null){
        	 m_botAction.sendPrivateMessage( sender, "There is no message at the specified index.");
         } else {
        	 hotSpots.elementAt(values[0]).removeMessage(values[1]);
        	 m_botAction.sendPrivateMessage( sender, "Message " + values[1] + " of HotSpot " + values[0] + " removed.");
         }
    }
    
    /**
     * Lists the messages of a spot
     * @param sender is the user of the bot
     * @param message is the index of the spot
     */  
    public void do_listMessages( String sender, String message ) {
    	int index;
    	try {
    		index = Integer.parseInt(message);
    	} catch( Exception e ) {
    		m_botAction.sendPrivateMessage( sender, "Input error.  Check and try again." );
            return;
    	}
    	if(index >= hotSpots.size() || index < 0){
    		m_botAction.sendPrivateMessage( sender, "The given HotSpot does not exist.");
    		return;
    	}
    	
    	if( hotSpots.elementAt(index).getMessages() == null ) {
    		m_botAction.sendPrivateMessage( sender, "There are no messages assigned to this spot." );
    	} else {
    		int i = 0;
    		Iterator<String> it = hotSpots.elementAt(index).getMessages().iterator();
    		while( it.hasNext() ){
    			String msg = it.next();
    			m_botAction.sendPrivateMessage( sender, i + ") " + msg );
    			i++;
    		}
    	}
    		
    }
    
    /**
     * Adds a spot
     * @param sender is the user of the bot.
     * @param message is the hotspot.
     */
    public void do_addHotSpot( String sender, String message ) {

        String pieces[] = message.split( " " );
        if( pieces.length < 3 || pieces.length == 4 || pieces.length > 6) return;

        int values[] = new int[pieces.length];
        try {
            for( int i = 0; i < pieces.length; i++ )
                values[i] = Integer.parseInt( pieces[i] );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( sender, "Input error.  Check and try again." );
            return;
        }

		HotSpot newSpot = new HotSpot(values);
		if(watch == null) {
			watch = newSpot;
		}
        hotSpots.add(newSpot);
        m_botAction.sendPrivateMessage( sender, "Hotspot added." );
    }

    /**
     * Clears all hotspots.
     * @param name
     */
    public void do_clearHotSpots( String name ) {
        hotSpots.clear();
        if(changeTask != null) {
	        changeTask.cancel();
        }
        watching = false;
        watch = null;
        if(name != null) {
	        m_botAction.sendPrivateMessage( name, "All hotspots cleared." );
        }
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
     * Changes the amount of time a user must wait to get a response from a hotspot after an initial response.
     * @param sender is the user of the bot
     * @param message is the command
     */
    public void do_repeatTime( String sender, String message ) {
    	int time = repeatTime;
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

        repeatTime = time;
        m_botAction.sendPrivateMessage( sender, "Repeat time set to " + time );
    }
    
    /**
     * Returns help message
     */
    public String[] getHelpMessages() {
        String help[] = {
                "HotSpot Module          - Enter one coordinate, warp to another; maybe get a prize.'",
                "!addspot                - Adds a new hotspot.<x> <y> <radius>",// <destx> <desty>",
                //"                        - Players will warp to the coord <destx>,<desty>",
                //"                        - when they enter within <radius> of <warpx>,<warpy>.",
                "!addmsg                 - Adds a message/command to send the player in contact with this spot",
                "                        - <HotSpot index>:<message>",
                "!removemsg              - Removes a message from a hotspot. <HotSpot index> <Message index>",
                "!listmsg                - Lists all messages for the specified HotSpot. <HotSpot index>",
                "!repeattime <ms>        - How long to pause between contact responses",
                "!switchtime <ms>        - How long to watch each spot before moving to the next",
                "!watch                  - Begin watching all hotspots.",
                "!clearspots             - Remove all hotspots.",
                "!listspots              - Lists your hotspots"
        };
        return help;
    }

    public void cancel() {
        do_clearHotSpots(null);
        m_botAction.resetReliablePositionUpdating();
    }
    
    /**
     * A timer object created so that players sitting on a hotspot will not cause packet flooding.
     */
    private class ContactTimer {
    	private Timer clock;
    	private TimerTask task;
    	private String name;
    	public ContactTimer(String playerName, int time){
    		this.name = playerName;
    		recentContacts.add(name);
    		clock = new Timer();
    		task = new TimerTask(){
    			public void run(){
    				if(recentContacts.contains(name))
    					recentContacts.remove(name);
    			}
    		};
    		clock.schedule(task, time);
    	}
    }
    
}

/**
 *Hot spot class that holds all related values
 */
class HotSpot {

    int x;
    int y;
    int r;
    ArrayList<String> messages;

    /**
     * Creates a new hotspot with the array of values. You can still use 5/6 values for warping/prizing, but
     * only 3 values are needed to create a working hotspot.
     */
    public HotSpot( int values[] ) {
    	if(values.length == 3){
            x = values[0];
            y = values[1];
            r = values[2];
    	}
    	else if(values.length == 5){
    		x = values[0];
            y = values[1];
            r = values[2];
            addMessage("*warpto " + values[3] + " " + values[4]);
    	}
    	else if(values.length == 6){
    		x = values[0];
            y = values[1];
            r = values[2];
            addMessage("*warpto " + values[3] + " " + values[4]);
            addMessage("*prize #" + values[5]);
    	}
    }
    
    /**
     * Add a message/command a player will receive when coming into contact with this spot.
     */
    public void addMessage( String message ){
    	if(messages == null)
    		messages = new ArrayList<String>();
    	messages.add(message);
    }
    
    /**
     * Get the message at a specified index. If there is none return null.
     */
    public String getMessage( int index ){
    	try{
    		return messages.get(index);
    	}catch(Exception e){return null;}//throws ArrayIndexOutOfBounds
    }
    
    /**
     * Remove a message/command a player will receive when coming into contact with this spot.
     */
    public void removeMessage( int index ){
    	try{
    		messages.remove(index);
    		if(messages.size() == 0)
    			messages = null;
    	} catch(Exception e){}
    }
    
    /**
     * Get the ArrayList of messages for this spot.
     */
    public ArrayList<String> getMessages(){
    	return messages;
    }
    
    /**
     * Return true if the given X,Y is touching this spot. Else return false.
     */
    public boolean inside( int playerX, int playerY ) {
        double dist = Math.sqrt( Math.pow( x*16 - playerX , 2 ) + Math.pow( y*16 - playerY , 2 ) );
        if( Math.round(dist) <= r*16 ) return true;
        else return false;
    }
    
    public String toString()	{
    	return ("X:" + x + " Y:" + y + " Radius:" + r );//+ " destX:" + x2 + " desty:" + y2);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    //public int getX2() { return x2; }
    //public int getY2() { return y2; }
}