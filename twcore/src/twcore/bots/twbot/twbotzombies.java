/*
 * twbotzombies.java
 *
 * Date (dd-mm-yyyy)	Author			Change / Comment
 * -------------------- -------------- -----------------------~
 * 21-03-2002			Harvey			Created class
 * 09-07-2006			MMaverick		Added !allowtk command
 */

/**
 * Zombies module for TWBot. A game of humans (ship 1) vs zombies (ship 3). 
 * If a human gets killed by a zombie it will turn the human into a zombie. 
 *
 * @author  harvey, MMaverick
 */
package twcore.bots.twbot;

import java.util.HashSet;
import java.util.List;

import twcore.bots.TWBotExtension;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;

public class twbotzombies extends TWBotExtension {
    
	/** Constructor of this class, creates a new instance of twbotzombies */
    public twbotzombies() {
        killmsgs = new StringBag();
        killmsgs.add( "dies a miserable horrible death at the hands of a Zombie!" );
    }

    // Mode variables
    HashSet<Integer> m_srcship = new HashSet<Integer>();
    int m_srcfreq;
    int m_destfreq;
    int m_destship;
    int m_lives;
    
    // String array of kil messages
    StringBag killmsgs;
    int killerShip;
    
    boolean isRunning = false;
    boolean modeSet = false;
    boolean killerShipSet = false;
    boolean allowTk = false;

    /**
     * Sets the mode of this zombie game into variables (class-wide)
     * 
     * @param srcfreq the source frequency of the humans
     * @param srcship the source ship number of the humans
     * @param destfreq the destination frequency of the zombies
     * @param destship the destination ship of the zombies
     * @param lives the number of lives the humans have before they are turned into zombies
     */
    public void setMode( int srcfreq, int srcship, int destfreq, int destship, int lives ){
        m_srcfreq = srcfreq;
        m_srcship.add(new Integer(srcship));
        m_destfreq = destfreq;
        m_destship = destship;
        m_lives = lives;
        modeSet = true;
    }

    /**
     * Adds a ship to the list of human ships
     * @param srcship The number of the human ship
     * @param name The name of the user who initiated this change
     */
    public void addShip(int srcship, String name)
    {
    	if(!(m_srcship.contains(new Integer(srcship))))
    	{
    		m_botAction.sendPrivateMessage(name, "Ship added.");
   			m_srcship.add(new Integer(srcship));
   		}
    }

    /**
     * Deletes ship from list of human ships
     * @param srcship The number of the human ship
     * @param name The name of the user who initiated this change
     */
    public void delShip(int srcship, String name)
    {
    	if(m_srcship.contains(new Integer(srcship)))
    	{
    		m_botAction.sendPrivateMessage(name, "Ship removed.");
    		m_srcship.remove(new Integer(srcship));
    	}
    }

    /**
     * Deletes a kill message from the list of kill messages
     * @param name The name of the user who initiated this change
     * @param index The index number of the kill message
     */
    public void deleteKillMessage( String name, int index ){

        List list = killmsgs.getList();

        if( !( 1 <= index && index <= list.size() )){
            m_botAction.sendPrivateMessage( name, "Error: Can't find the index" );
            return;
        }

        index--;

        if( list.size() > 1 )
            m_botAction.sendPrivateMessage( name, "Removed: " + (String)list.remove( index ));
        else {
            m_botAction.sendPrivateMessage( name, "Sorry, but there must be at least one kill message loaded at all times." );
        }
    }

    /**
     * Lists all of the kill messages currently in the killmsgs StringBag
     * @param name The name of the user
     */
    public void listKillMessages( String name ){
        m_botAction.sendPrivateMessage( name, "The following messages are in my posession: " );
        List list = killmsgs.getList();
        for( int i = 0; i < list.size(); i++ ){
            if( ((String)list.get( i )).startsWith( "'" )){
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name>" + (String)list.get( i ));
            } else {
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name> " + (String)list.get( i ));
            }
        }
    }

    /**
     * Overridden method from TWBotExtension, handling events
     * @param event
     */
    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name )) handleCommand( name, message );
        }
    }

    /**
     * Returns the number from the string
     * @param input string containing a number
     * @return integer from the string
     */
    public int getInteger( String input ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return 1;
        }
    }

    /**
     * Initiates a game of zombies
     * @param name Name of the user
     * @param params String array with parameters for the zombie game
     */
    public void start( String name, String[] params ){
        try{
            if( params.length == 5 ){
                int srcfreq = Integer.parseInt(params[0]);
                int srcship = Integer.parseInt(params[1]);
                int destfreq = Integer.parseInt(params[2]);
                int destship = Integer.parseInt(params[3]);
                int lives = Integer.parseInt(params[4]);
                setMode( srcfreq, srcship, destfreq, destship, lives );
                isRunning = true;
                modeSet = true;
            } else if( params.length == 1 ){
                int lives = Integer.parseInt(params[0]);
                setMode( 0, 1, 2, 3, lives );
                isRunning = true;
                modeSet = true;
            }
        }catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Sorry, you made a mistake, please try again." );
            isRunning = false;
            modeSet = false;
        }
    }

    /**
     * Handles commands given from the user
     * @param name of the user
     * @param message containing the command
     */
    public void handleCommand( String name, String message ){
        if( message.startsWith( "!list" )){
            listKillMessages( name );
        } else if( message.startsWith( "!add " )){
            addKillMessage( name, message.substring( 5 ));
        } else if( message.startsWith( "!stop" )){
            m_botAction.sendPrivateMessage( name, "Zombies mode stopped" );
            isRunning = false;
        } else if( message.startsWith( "!start " )){
            String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
            start( name, parameters );
            
            if(this.allowTk)
            	m_botAction.sendPrivateMessage( name, "Zombies mode started with teamkills ALLOWED" );
            else
            	m_botAction.sendPrivateMessage( name, "Zombies mode started with no action on teamkills" );
            	
        } else if( message.startsWith( "!start" )){
            setMode( 0, 1, 2, 3, 1 );
            isRunning = true;
            modeSet = true;
            
            if(this.allowTk)
            	m_botAction.sendPrivateMessage( name, "Zombies mode started with teamkills ALLOWED" );
            else
            	m_botAction.sendPrivateMessage( name, "Zombies mode started with no action on teamkills" );
            
        } else if( message.startsWith( "!del " ))
            deleteKillMessage( name, getInteger( message.substring( 5 )));
          else if(message.startsWith("!addship "))
          {
          	String pieces[] = message.split(" ");
          	int ship = 1;
             try {
             	ship = Integer.parseInt(pieces[1]);
             } catch(Exception e) {}
             if(ship > 8 || ship < 1)
             	ship = 1;
             addShip(ship, name);
          }
          else if(message.startsWith("!delship "))
          {
          	String pieces[] = message.split(" ");
          	int ship = 1;
          	try {
          		ship = Integer.parseInt(pieces[1]);
          	} catch(Exception e) {}
          	if(ship > 8 || ship < 1)
          		ship = 1;
          	delShip(ship, name);
          }
          else if(message.startsWith("!killership "))
          {
          	String pieces[] = message.split(" ");
          	int ship = 1;
          	try {
          		ship = Integer.parseInt(pieces[1]);
          	} catch(Exception e) {}
          	if(ship > 8 || ship < 1)
          		ship = 1;
          	killerShip = ship;
          	killerShipSet = true;
          	m_botAction.sendPrivateMessage(name, "Ship " + killerShip + " has been set for killing a zombie.");
          }
          else if(message.startsWith("!allowtk ")) 
          {
        	String parameters[] = message.split(" ");
        	
        	if(parameters[1].equals("yes")) {
        		this.allowTk = true;
        		m_botAction.sendPrivateMessage(name, "Teamkills are ALLOWED from now on (humans will be turned into zombies on teamkill)");
        	} else if(parameters[1].equals("no")) {
        		this.allowTk = false;
        		m_botAction.sendPrivateMessage(name, "Teamkills are DISALLOWED from now on (humans won't be changed to zombies on a teamkill)");
        	} else {
        		m_botAction.sendPrivateMessage(name, "You can only use 'yes' or 'no' with the !allowtk command.");
        	}
        	   
          }
          else if(message.equals("!allowtk"))
          {
        	m_botAction.sendPrivateMessage(name, "The !allowtk command lets you specify if teamkills are allowed for turning humans into zombies.");
        	m_botAction.sendPrivateMessage(name, "(Human vs. Human) Allowed parameters: 'yes' / 'no' .");
          }

/*        } else if( message.startsWith( "!setupwarp2" )){
            m_botAction.warpFreqToLocation( 0, 800, 240 );
            m_botAction.warpFreqToLocation( 2, 270, 840 );
        } else if( message.startsWith( "!setupwarp" )){
            m_botAction.warpFreqToLocation( 0, 870, 450 );
            m_botAction.warpFreqToLocation( 2, 900, 900 );*/
    }


    /**
     * Adds a kill message to the StringBag with kill messages
     * @param name of the user
     * @param killMessage the new kill message
     */
    public void addKillMessage( String name, String killMessage ){
        killmsgs.add( killMessage );
        m_botAction.sendPrivateMessage( name, "<name> " + killMessage + " Added" );
    }

    /**
     * Overridden command from TWBotExtension class, handling the event of a death of a player
     * @param event
     */
    public void handleEvent( PlayerDeath event ){
        if( modeSet && isRunning ){
            Player p = m_botAction.getPlayer( event.getKilleeID() );
            Player p2 = m_botAction.getPlayer( event.getKillerID() );
            
            //Check for a teamkill and stop the action if teamkills are not allowed
            if(p.getFrequency() == p2.getFrequency()) { // If it's a teamkill
            	if(this.allowTk == false) {			// Check if teamkills are disallowed
            		return;								// Abort the action if it is
            	}										// Else continue with the continue (if teamkills are allowed)
            }
            
            try {
                if( p.getLosses() >= m_lives && m_srcship.contains(new Integer(p.getShipType())) && p.getFrequency() == m_srcfreq ){
                    m_botAction.setShip( event.getKilleeID(), m_destship );
                    m_botAction.setFreq( event.getKilleeID(), m_destfreq );
                    String killmsg = killmsgs.toString();
                    int soundPos = killmsg.indexOf('%');
                    int soundCode = 0;

                    if( soundPos != -1){
                        try{
                            soundCode = Integer.parseInt(killmsg.substring(soundPos + 1));
                        } catch( Exception e ){
                            soundCode = 0;
                        }
                        if(soundCode == 12) {soundCode = 1;} //no naughty sounds
                    }

                    if( killmsg.startsWith( "'" ) == false){
                        killmsg = " " + killmsg;
                    }

                    if( soundCode > 0 ){
                        killmsg = killmsg.substring(0, soundPos + 1);
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + killmsg, soundCode );
                    } else {
                        m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + killmsg );
                    }

                }
                if(m_srcship.contains(new Integer(p2.getShipType())) && p2.getShipType() != m_destship && killerShipSet)
                {
                    if(p2.getShipType() != killerShip)
                        m_botAction.setShip(event.getKillerID(), killerShip);
                }
            } catch (Exception e) {

            }
        }
    }

    /**
     * Returns a string array with the !help menu
     * @return String[] array
     */
    public String[] getHelpMessages() {
        String[] ZombiesHelp = {
            "!list               - Lists the currently loaded kill messages",
            "!add <Kill Message> - Adds a kill message. Use %%<num> at the end to add a sound.",
//            "!setupwarp          - Warps everyone to their proper start locations in a standard zombies game",
//            "!setupwarp2         - Warps everyone to their proper start locations in a zombies2 game",
            "!del <index>        - Deletes a kill message.  The number for the index is taken from !list",
            "!stop               - Shuts down zombies mode",
            "!start              - Starts a standard zombies mode",
            "!start <srcfreq> <srcship> <destfreq> <destship> <lives> - Starts a special zombies mode",
            "!addship            - Adds a ship to the list of human ships.",
            "!delship            - Deletes ship from list of human ships.",
            "!killership         - Sets the ship for a human that kills a zombie.",
            "!allowtk yes/no     - Allows (yes) or disallows (no) teamkills for making humans into zombies"
        };
        return ZombiesHelp;
    }
    
    /**
     * Required overridden method from TWBotExtension
     */
    public void cancel() {
    }
}
