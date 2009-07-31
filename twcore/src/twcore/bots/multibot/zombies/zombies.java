package twcore.bots.multibot.zombies;

import java.util.HashSet;
import java.util.ArrayList;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;

/**
 * The old Zombies module, with features for new times.
 * 
 * @author harvey yau; modded by dugwyler and others
 */
public class zombies extends MultiModule {

    public void init() {
        killmsgs = new StringBag();
        killmsgs.add( "dies a miserable horrible death at the hands of a Zombie!" );
    }

    public void requestEvents(ModuleEventRequester events)	{
		events.request(this, EventRequester.PLAYER_DEATH);
	}

    HashSet <Integer>m_srcship = new HashSet<Integer>();
    int m_humanfreq;
    int m_killerShip;
    int m_zombiefreq;
    int m_zombieship;
    int m_lives;
    int m_rebirthkills;
    StringBag killmsgs;
    boolean isRunning = false;
    boolean modeSet = false;
    boolean killerShipSet = false;
    boolean rebK = false;

    public void setMode( int srcfreq, int srcship, int destfreq, int destship, int lives, int rebirthkills ){
        m_humanfreq = srcfreq;
        m_srcship.add(new Integer(srcship));
        m_zombiefreq = destfreq;
        m_zombieship = destship;
        m_lives = lives;
        m_rebirthkills = rebirthkills;
        if(m_rebirthkills > 0)
        	rebK = true;
        else
        	rebK = false;
        modeSet = true;
    }

    public void addShip(int srcship, String name)
    {
    	if(!(m_srcship.contains(new Integer(srcship))))
    	{
    		m_botAction.sendPrivateMessage(name, "Ship added.");
   			m_srcship.add(new Integer(srcship));
   		}
    }

    public void delShip(int srcship, String name)
    {
    	if(m_srcship.contains(new Integer(srcship)))
    	{
    		m_botAction.sendPrivateMessage(name, "Ship removed.");
    		m_srcship.remove(new Integer(srcship));
    	}
    }

    public void deleteKillMessage( String name, int index ){

        ArrayList<String> list = killmsgs.getList();

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

    public void listKillMessages( String name ){
        m_botAction.sendPrivateMessage( name, "The following messages are in my posession: " );
        ArrayList<String> list = killmsgs.getList();
        for( int i = 0; i < list.size(); i++ ){
            if( ((String)list.get( i )).startsWith( "'" )){
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name>" + (String)list.get( i ));
            } else {
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name> " + (String)list.get( i ));
            }
        }
    }

    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( opList.isER( name )) handleCommand( name, message );
        }
    }

    public int getInteger( String input ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return 1;
        }
    }

    public void start( String name, String[] params ){
        try{
            if( params.length == 6 ){
                int srcfreq = Integer.parseInt(params[0]);
                int srcship = Integer.parseInt(params[1]);
                int destfreq = Integer.parseInt(params[2]);
                int destship = Integer.parseInt(params[3]);
                int lives = Integer.parseInt(params[4]);
                int rebirthkills = Integer.parseInt(params[5]);
                setMode( srcfreq, srcship, destfreq, destship, lives, rebirthkills );
                isRunning = true;
                modeSet = true;                
            } else if( params.length == 5 ){
                int srcfreq = Integer.parseInt(params[0]);
                int srcship = Integer.parseInt(params[1]);
                int destfreq = Integer.parseInt(params[2]);
                int destship = Integer.parseInt(params[3]);
                int lives = Integer.parseInt(params[4]);
                setMode( srcfreq, srcship, destfreq, destship, lives, 0 );
                isRunning = true;
                modeSet = true;
            } else if( params.length == 1 ){
                int lives = Integer.parseInt(params[0]);
                setMode( 0, 1, 2, 3, lives, 0 );
                isRunning = true;
                modeSet = true;
            }
        }catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Sorry, you made a mistake; please try again." );
            isRunning = false;
            modeSet = false;
        }
    }

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
            m_botAction.scoreResetAll();
            m_botAction.sendPrivateMessage( name, "Zombies mode started" );
        } else if( message.startsWith( "!start" )){
            setMode( 0, 1, 2, 3, 1, 0 );
            isRunning = true;
            modeSet = true;
            m_botAction.scoreResetAll();
            m_botAction.sendPrivateMessage( name, "Zombies mode started" );
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
          	m_killerShip = ship;
          	killerShipSet = true;
          	m_botAction.sendPrivateMessage(name, "Ship " + m_killerShip + " will now be given to anyone who kills a zombie.");
          }

/*        } else if( message.startsWith( "!setupwarp2" )){
            m_botAction.warpFreqToLocation( 0, 800, 240 );
            m_botAction.warpFreqToLocation( 2, 270, 840 );
        } else if( message.startsWith( "!setupwarp" )){
            m_botAction.warpFreqToLocation( 0, 870, 450 );
            m_botAction.warpFreqToLocation( 2, 900, 900 );*/
    }


    public void addKillMessage( String name, String killMessage ){
        killmsgs.add( killMessage );
        m_botAction.sendPrivateMessage( name, "<name> " + killMessage + " Added" );
    }

    public void handleEvent( PlayerDeath event ){
        if( modeSet && isRunning ){
            Player p = m_botAction.getPlayer( event.getKilleeID() );
            Player p2 = m_botAction.getPlayer( event.getKillerID() );
            if( p == null || p2 == null )
                return;
            try {
                if( p.getLosses() >= m_lives && m_srcship.contains(new Integer(p.getShipType())) && p.getFrequency() == m_humanfreq && p2.getFrequency() != m_humanfreq){
                    m_botAction.setShip( event.getKilleeID(), m_zombieship );
                    m_botAction.setFreq( event.getKilleeID(), m_zombiefreq );
                    if(!rebK)
                    	m_botAction.scoreReset( event.getKilleeID() );
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
                        m_botAction.sendArenaMessage( p.getPlayerName() + killmsg, soundCode );
                    } else {
                        m_botAction.sendArenaMessage( p.getPlayerName() + killmsg );
                    }
                }
                // Check ship upgrade for a zombie killer
                if(killerShipSet && m_srcship.contains(new Integer(p2.getShipType())) && p2.getShipType() != m_zombieship)
                {
                    if(p2.getShipType() != m_killerShip)
                        m_botAction.setShip(event.getKillerID(), m_killerShip);
                }
                // Rebirth kill check
                if( m_rebirthkills > 0 && m_zombieship == p2.getShipType() && (p2.getWins()) >= m_rebirthkills ) {
                    int shipNum = m_srcship.iterator().next().intValue(); // Get any ship available, as there's no preference
                    m_botAction.setShip(event.getKillerID(), shipNum);
                    m_botAction.scoreReset( p2.getPlayerName() );
                }
            } catch (Exception e) {

            }
        }
    }

    public String[] getModHelpMessage() {
        String[] ZombiesHelp = {
            "!list               - Lists the currently loaded kill messages",
            "!add <Kill Message> - Adds a kill message. Use %%<num> at the end to add a sound.",
//            "!setupwarp          - Warps everyone to their proper start locations in a standard zombies game",
//            "!setupwarp2         - Warps everyone to their proper start locations in a zombies2 game",
            "!del <index>        - Deletes a kill message.  The number for the index is taken from !list",
            "!stop               - Shuts down zombies mode",
            "!start              - Starts a standard zombies mode",
            "!start <humanfreq> <humanship> <zombfreq> <zombship> <lives> [rebirthkills] - Special zombies",
            "       (rebirthkills is optional: # kills needed to turn zombie back to human.  Default 0/off)",
            "!addship            - Adds a ship to the list of human ships.",
            "!delship            - Deletes ship from list of human ships.",
            "!killership         - Sets the ship for a human that kills a zombie."
        };
        return ZombiesHelp;
    }
    public void cancel() {
    }

    public boolean isUnloadable()	{
		return true;
	}
}
