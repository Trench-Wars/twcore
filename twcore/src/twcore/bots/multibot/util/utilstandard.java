package twcore.bots.multibot.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.game.Ship;
import twcore.core.util.Tools;

/**
 * twbotstandard.java - a general utility module for event refs, haphazardly-arranged.
 * Contains some unlisted commands for controlling bot in-game (with some problems).
 * <revision date='December 19, 2007 2:25 PM' by='Pio'>
 * - Added a few confirmation messages for commands (e.g. !dolock, !dounlock)
 * </revision>
 * @author  harvey
 * @version 1.1

 */
public class utilstandard extends MultiUtil {
   // LinkedList arenaTasks = new LinkedList(); Unreferenced?
    HashMap<String,StoredPlayer> storedPlayers = new HashMap<String,StoredPlayer>();
    // Lock toggle, for internal use.  0=ignore lock msgs; 1=lock arena; 2=unlock arena
    int doLock = 0;

    private final static String[] help = {
            "!random <sizeOfFreqs>   - Randomizes players to freqs of a given size.",
            "!increment <sizeOfFreqs> <initialFreq> <freqIncrement>",
            "                        - Randomizes players to freqs with size and freq increment",
            "!teams <numberTeams>    - Makes the requested number of teams.",
            "!door <-2 to 255>       - Changes door mode.  -2 and -1 are random/on-off modes.",
            "!restart                - Restarts the ball game. (*restart)",
            "!warprandom             - Warps everyone in the arena to a random location",
            "!dolock                 - Locks the arena (will guarantee lock; is NOT a toggle)",
            "!dounlock               - Unlocks the arena (will guarantee unlock; is NOT a toggle)",
            "!where                  - Robo will tell you his location. Remote PM only.",            
            "!setship <ship>         - Changes everyone to <ship>",
            "!setship <freq> <ship>  - Changes everyone on <freq> to <ship>",
            "!setfreq <freq>         - Changes everyone to <freq>",
            "!setfreq <ship> <freq>  - Changes everyone in <ship> to <freq> (-<ship> for 'other than')",
            "!merge <freq1> <freq2>  - Changes everyone on <freq1> to <freq2>",
            "!teamsspec <numTeams>   - Makes requested # of teams, specs all, & keeps freqs.   *",
            "!speckeepfreq <freq>    - Specs everyone on <freq>, but keeps them on their freq. *",
            "!specallkeepfreqs       - Specs everyone, but keeps them on their freq.           *",
            "!restore                - Setship/setfreq to prior state anyone spec'd by these.  ^",
            "!clearinfo              - Clear all information stored about player freqs & ships."
    };


    /**
     * Init method, called on load.
     */
    public void init() {
    }

    /**
     * Requests needed events through supplied ModuleEventRequester rather than
     * standard EventRequester; this allows MultiBot to manage which events are
     * requested as modules are loaded/unloaded.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        // Needs none (Message is included by default)
    }

    public void handleEvent( Message event ){
        if(doLock != 0 && event.getMessageType() == Message.ARENA_MESSAGE) {
            if(doLock == 1 && event.getMessage().equals("Arena UNLOCKED")) {
                m_botAction.toggleLocked();
            } else if(doLock == 2 && event.getMessage().equals("Arena LOCKED")) {
                m_botAction.toggleLocked();
            }
            doLock = 0;
        }

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name )) handleCommand( name, message );
        }
    }

    public void handleCommand( String name, String message ){
        if( message.startsWith( "!random " )){
            m_botAction.createRandomTeams( getInteger( message.substring(8) ));
            m_botAction.sendSmartPrivateMessage(name, "Teams were created successfully.");
        } else if (message.startsWith("!increment ")) {
            String[] msg = message.split(" ");
            if (msg.length == 4) {
                m_botAction.createIncrementingTeams(getInteger(msg[1]), getInteger(msg[2]), getInteger(msg[3]));
                m_botAction.sendSmartPrivateMessage(name, "Teams were created successfully.");
            } else {
                m_botAction.sendSmartPrivateMessage(name, "Team creation failed!");
            }
        } else if( message.startsWith( "!teams " )){
            createNumberofTeams( getInteger( message.substring(7) ));
            m_botAction.sendSmartPrivateMessage(name, "Teams were created successfully.");
        } else if( message.startsWith( "!door " )){
            m_botAction.setDoors( getInteger( message.substring( 6 )));
            m_botAction.sendSmartPrivateMessage(name, "Door mode was set to " + message.substring(6));
        } else if( message.startsWith( "!warprandom" )){
            m_botAction.warpAllRandomly();
        } else if( message.startsWith( "!dolock" )){
            doLock = 1;
            m_botAction.toggleLocked();
            m_botAction.sendArenaMessage("Arena is now locked.");
        } else if( message.startsWith( "!dounlock" )){
            doLock = 2;
            m_botAction.toggleLocked();
            m_botAction.sendArenaMessage("Arena is now unlocked.");
        } else if( message.startsWith( "!setship " )){
            String[] parameters = Tools.stringChopper( message.substring( 8 ).trim(), ' ' );
            try{
                if( parameters.length == 2 ){
                    int freq = Integer.parseInt(parameters[0]);
                    int shiptype = Integer.parseInt(parameters[1]);
                    m_botAction.changeAllShipsOnFreq( freq, shiptype );
                } else if( parameters.length == 1 ){
                    int shiptype = Integer.parseInt( parameters[0] );
                    m_botAction.changeAllShips( shiptype );
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!setfreq " )){
            String[] parameters = Tools.stringChopper( message.substring( 8 ).trim(), ' ' );
            try{
                if( parameters.length == 2 ){
                    int shiptype = Integer.parseInt(parameters[0]);
                    int freq = Integer.parseInt(parameters[1]);
                    m_botAction.changeAllInShipToFreq( shiptype, freq );
                } else if( parameters.length == 1 ){
                    int freq = Integer.parseInt( parameters[0] );
                    m_botAction.setAlltoFreq( freq );
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!merge ")) {
            String[] parameters = Tools.stringChopper( message.substring( 7 ).trim(), ' ' );
            try{
                if( parameters.length == 2 ){
                    m_botAction.setFreqtoFreq(Integer.parseInt(parameters[0]), Integer.parseInt(parameters[1]));
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!restart" )){
            m_botAction.sendUnfilteredPublicMessage("*restart");
            m_botAction.sendSmartPrivateMessage(name, "Restart command successful.");
        } else if( message.startsWith( "!specallkeepfreqs" )){
            storePlayerInfo( -1 );
            m_botAction.specAllAndKeepFreqs();
        } else if( message.startsWith( "!speckeepfreq " )){
            try {
                int i = Integer.parseInt( message.substring(10).trim() );
                storePlayerInfo( i );
                m_botAction.specFreqAndKeepFreq( i );
            } catch (Exception e ) {
                m_botAction.sendSmartPrivateMessage( name, "Input confusing.  Where is your God now?" );
            }
        } else if( message.startsWith( "!teamsspec " )){
            try {
                int i = Integer.parseInt( message.substring(10) );
                createNumberofTeams( i );
                storePlayerInfo( -1 );
                m_botAction.specAllAndKeepFreqs();
            } catch (Exception e ) {
                m_botAction.sendSmartPrivateMessage( name, "Input confusing.  Where is your God now?" );
            }
        } else if( message.startsWith( "!restore" )){
            restorePlayers();
            m_botAction.sendSmartPrivateMessage( name, "Players restored to prior ships and freqs." );
        } else if( message.startsWith( "!clearinfo" )){
            clearStoredInfo();
            m_botAction.sendSmartPrivateMessage( name, "Player ship and frequency data cleared." );

            // ************ "EASTER EGG" COMMANDS *************

        } else if( message.startsWith( "!move " )){
            String[] parameters = Tools.stringChopper( message.substring( 6 ).trim(), ' ' );
            try{
                if( parameters.length == 2 ){
                    int x = Integer.parseInt(parameters[0]);
                    int y = Integer.parseInt(parameters[1]);
                    m_botAction.moveToTile( x, y );
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!rotate " )){
            String[] parameters = Tools.stringChopper( message.substring( 8 ).trim(), ' ' );
            try{
                if( parameters.length == 1 ){
                    int x = Integer.parseInt(parameters[0]);
                    Ship s = m_botAction.getShip();
                    s.setRotation( x );
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!fire " )){
            String[] parameters = Tools.stringChopper( message.substring( 6 ).trim(), ' ' );
            try{
                if( parameters.length == 1 ){
                    int x = Integer.parseInt(parameters[0]);
                    Ship s = m_botAction.getShip();
                    s.fire( x );
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!barrage" )){
            Ship s = m_botAction.getShip();
            for( int i = 0; i < 255; i++ ){
                s.setRotation( i % 40 );
                s.fire( i );
            }
        } else if( message.startsWith("!easy") ) {
            TimerTask ezT = new TimerTask() {
                public void run() {
                    killEasy();
                }
            };
            m_botAction.scheduleTask(ezT, 5000);
        }
    }

    public int getInteger( String input ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return 1;
        }
    }

    public int getInteger( String input, int defValue ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return defValue;
        }
    }

    public void killEasy() {
        m_botAction.setPlayerPositionUpdating(200);
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        while(it.hasNext()) {
            Player p = (Player)it.next();
            Ship s = m_botAction.getShip();
            s.moveAndFire(p.getXLocation(), p.getYLocation(), s.getWeaponNumber((byte)4, (byte)2, false, false, true, (byte)8, false));
        }
        m_botAction.resetReliablePositionUpdating();
    }

    /**
     * Stores ship and freq info on a freq specified (or -1 for all players
     * currently in the game).  The information can later be used to !restore
     * the previous status of the stored players.
     * @param freq Frequency to store; -1 for all players
     */
    private void storePlayerInfo( int freq ) {
        Iterator<Player> i;
        if( freq == -1 )
            i = m_botAction.getPlayingPlayerIterator();
        else
            i = m_botAction.getFreqPlayerIterator( freq );

        while( i.hasNext() ) {
            Player p = (Player)i.next();
            try {
                storedPlayers.put( p.getPlayerName(), new StoredPlayer(p.getShipType(),p.getFrequency()) );
            } catch (Exception e) {}
        }
    }

    /**
     * Restores players to prior ship & freq before they were spec'd with one
     * of the memory-enabled commands.
     */
    private void restorePlayers() {
        Iterator<String> i = storedPlayers.keySet().iterator();

        while( i.hasNext() ) {
            String player = (String)i.next();
            if( player != null ) {
                StoredPlayer p = storedPlayers.get( player );
                if( p != null ) {
                    m_botAction.setShip(player, p.ship);
                    m_botAction.setFreq(player, p.freq);
                }
            }
        }

        storedPlayers.clear();
    }

    /**
     * Clears stored ship/freq info on players.
     */
    private void clearStoredInfo() {
        storedPlayers.clear();
    }

    /**
     * Creates the requested number of teams, if possible.
     * @param
     */
    private void createNumberofTeams( int _teams ) {

        int current = 0;
        int howmany = _teams - 1;

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        while( i.hasNext() ) {

            if(current > howmany)
                current = 0;
            Player p = (Player)i.next();
            m_botAction.setFreq( p.getPlayerID(), current );
            current++;
        }
    }

    /**
     * Returns String array of help for this module.
     */
    public String[] getHelpMessages() {
        return help;
    }

    public void cancel() {
    }

    /**
     * Used to store freq and ship info.
     */
    private class StoredPlayer {
        int ship;
        int freq;

        public StoredPlayer( int ship, int freq ) {
            this.ship = ship;
            this.freq = freq;
        }
    }
}