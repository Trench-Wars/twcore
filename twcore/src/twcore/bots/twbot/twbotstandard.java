package twcore.bots.twbot;

import java.util.*;

import twcore.bots.TWBotExtension;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.game.Ship;
import twcore.core.util.Tools;

/**
 * twbotstandard.java - a general utility module for event refs, haphazardly-arranged.
 * Contains some unlisted commands for controlling bot in-game (with some problems).
 *
 * @author  harvey
 */
public class twbotstandard extends TWBotExtension {
    LinkedList arenaTasks = new LinkedList();
    HashMap<String,StoredPlayer> storedPlayers = new HashMap<String,StoredPlayer>();

    /** Creates a new instance of the TWBot standard module */
    public twbotstandard() {
    }

    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name )) handleCommand( name, message );
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

    public void handleCommand( String name, String message ){
        if( message.startsWith( "!random " )){
            m_botAction.createRandomTeams( getInteger( message.substring(8) ));
        } else if( message.startsWith( "!teams " )){
            createNumberofTeams( getInteger( message.substring(7) ));
        } else if( message.startsWith( "!door " )){
            m_botAction.setDoors( getInteger( message.substring( 6 )));
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
        } else if( message.startsWith( "!specallkeepfreqs" )){
            storePlayerInfo( -1 );
            m_botAction.specAllAndKeepFreqs();
        } else if( message.startsWith( "!specfreq " )){
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
            m_botAction.setPlayerPositionUpdating(200);
            m_botAction.scheduleTask(ezT, 5000);
        }
    }

    public void killEasy() {
        m_botAction.setPlayerPositionUpdating(200);
        Iterator it = m_botAction.getPlayingPlayerIterator();
        while(it.hasNext()) {
            Player p = (Player)it.next();
            Ship s = m_botAction.getShip();
            s.moveAndFire(p.getXLocation(), p.getYLocation(), s.getWeaponNumber((byte)4, (byte)2, false, false, true, (byte)8, false));
        }
        m_botAction.setPlayerPositionUpdating(0);
    }
    
    /**
     * Stores ship and freq info on a freq specified (or -1 for all players
     * currently in the game).  The information can later be used to !restore
     * the previous status of the stored players.
     * @param freq Frequency to store; -1 for all players 
     */
    private void storePlayerInfo( int freq ) {
        Iterator i;
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
        Iterator i = storedPlayers.keySet().iterator();
        
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

        Iterator i = m_botAction.getPlayingPlayerIterator();
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
        String[] help = {
            "!random <sizeOfFreqs>   - Randomizes players to freqs of a given size.",
            "!teams <numberTeams>    - Makes the requested number of teams.",
            "!door <-2 to 255>       - Changes door mode.  -2 and -1 are random/on-off modes.",
            "!restart                - Restarts the ball game.",
            "!where                  - Robo will tell you his location. Remote PM or squad msg only.",
            "!setship <ship>         - Changes everyone to <ship>",
            "!setship <freq> <ship>  - Changes everyone on <freq> to <ship>",
            "!setfreq <freq>         - Changes everyone to <freq>",
            "!setfreq <ship> <freq>  - Changes everyone in <ship> to <freq>",
            "!merge <freq1> <freq2>  - Changes everyone on <freq1> to <freq2>.",
            "!teamsspec <numTeams>   - Makes requested # of teams, specs all, & keeps freqs.   *",
            "!specfreq <freq>        - Specs everyone on <freq>, but keeps them on their freq. *",
            "!specallkeepfreqs       - Specs everyone, but keeps them on their freq.           *",
            "!restore                - Setship/setfreq to prior state anyone spec'd by these.  ^",
            "!clearinfo              - Clear all information stored about player freqs & ships."
        };
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