package twcore.bots.multibot.gangsoftw;

import static twcore.core.EventRequester.PLAYER_DEATH;
import static twcore.core.EventRequester.PLAYER_LEFT;
import static twcore.core.EventRequester.FREQUENCY_SHIP_CHANGE;
import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.game.Player;
import twcore.core.util.StringBag;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.LinkedList;

/**
 * Gangs of TW.  A selective and sophisticated version of conquer.
 */
public class gangsoftw extends MultiModule {
    private final static int DEFAULT_LEADER_DEATHS = 5;
    int m_maxLeaderDeaths = 5;
    boolean isRunning = false;
    boolean DEBUG = false;
    
    HashMap <Integer,GangOwner>m_gangOwners = new HashMap<Integer,GangOwner>();
    TimerTask m_checkForWinnersTask;
    
    StringBag winnerNames = new StringBag();
    StringBag killNames   = new StringBag();

    // ***** SETUP *****
    
    public void init() {
        winnerNames.add( "Legendary" );
        winnerNames.add( "Notorious" );
        winnerNames.add( "Infamous" );
        winnerNames.add( "Perfidious" );
        winnerNames.add( "Odious" );
        winnerNames.add( "Execrable" );
        winnerNames.add( "Abominable" );
        winnerNames.add( "Insidious" );
        winnerNames.add( "Pernicious" );
        winnerNames.add( "Malevolent" );
        winnerNames.add( "Maleficent" );
        winnerNames.add( "Pestilent" );
        winnerNames.add( "Very Rude" );
        winnerNames.add( "Noxious" );
        winnerNames.add( "Rather Unwholesome" );
        winnerNames.add( "Unsavory" );
        winnerNames.add( "Foul" );
        winnerNames.add( "Insalubrious" );
        winnerNames.add( "Unreasonable" );
        winnerNames.add( "Irresponsible" );
        
        killNames.add("eliminated");
        killNames.add("bumped off");        
        killNames.add("rubbed out");
        killNames.add("slain");
        killNames.add("forcibly eviscerated");
        killNames.add("stabbed to death");
        killNames.add("torn to pieces");
        killNames.add("shot in the face");
        killNames.add("thrown off a tall building");
        killNames.add("sent to Davy Jones' locker");
        killNames.add("poisoned");
        killNames.add("beheaded");
        killNames.add("torched with gasoline");
        killNames.add("beaten to death with a rusty pipe");
    }

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, PLAYER_DEATH);
        events.request(this, PLAYER_LEFT);
        events.request(this, FREQUENCY_SHIP_CHANGE);
    }
    
    public String[] getModHelpMessage() {
        String[] help = {
                "Gangs of TW                                   -  qan  1/1/09",
                "NOTE: This is a basic event module done in the old-school style.",
                "      When you start it, it assumes you have set all ships,",
                "      done a score reset, and placed everyone on their own freq.",
                "      However, it will announce the winner (so you can see stats).",
                "!start     - starts Gangs of TW (w/ " + DEFAULT_LEADER_DEATHS + " leader deaths)",
                "!start <#> - starts Gangs of TW w/ custom # gang leader deaths",
                "!stop      - stops Gangs of TW",
                "!rules     - send massive rule/game info spam in arena msgs"
        };
        return help;
    }

    public String[] getPlayerHelpMessage() {
        String[] help = {
                "....................................................................",
                ".  Gangs of TW                                         qan  1/1/09 .",
                "....................................................................",
                ".     You've started a nasty gang.  As a gang leader, if you beat  .",
                ".  another aspiring gang leader with no followers, they become     .",
                ".  your cronie and will fight for you.  If they have their own     .",
                ".  cronies and you beat them, you can steal the cronies away.      .",
                ".  You can have up to 4 cronies, swapping out the less skilled     .",
                ".  for better ones when you acquire them.  But if you get beaten   .",
                ".  too many times as leader, you'll be eliminated from the game.   .",
                ".     On the other hand, cronies can take a beating -- when they   .",
                ".  die, it doesn't count toward their elimination deaths.  The     .",
                ".  cronies, then, can afford to get their feet wet.  If a gang     .",
                ".  leader happens to die, the cronie with the largest number of    .",
                ".  kills becomes the next leader.  The dream lives on...           .",
                ".  COMMANDS:  1 2 3 4 - swap waiting cronie   g - gang list        .",
                ".             l       - show leaders of all gangs                  .",
                ".             w #     - who's who on freq #   w name - playerinfo  .",
        };
        return help;
    }
    
    public boolean isUnloadable() {
        return true;
    }

    public void cancel() {
    }

    
    // ***** EVENT HANDLING *****
    
    public void handleEvent( PlayerLeft event ) {
        if( !isRunning )
            return;
        GangOwner leavingG = m_gangOwners.get( new Integer(event.getPlayerID()) );
        handleLeavingPlayer( leavingG );
    }
    
    public void handleEvent( FrequencyShipChange event ) {
        if( !isRunning )
            return;
        if( event.getShipType() == 0 ) {
            GangOwner leavingG = m_gangOwners.get( new Integer(event.getPlayerID()) );
            handleLeavingPlayer( leavingG );
        }
    }
    
    public void handleLeavingPlayer( GangOwner leavingG ) {
        if( leavingG != null ) {
            int leaderID = leavingG.getCronieTo();
            if( leaderID != -1 ) {
                GangOwner leaderG = m_gangOwners.get( new Integer( leaderID ) );
                if( leaderG != null ) {
                    if( leaderG.removeSpecificCronie( leavingG.getLeaderID() ) ) {
                        m_botAction.sendPrivateMessage( leaderID, leavingG.getName() + " has left your gang." );
                    }
                }
            } else {
                leavingG.assignBestCronieToTakeOver();
            }
        }
        m_gangOwners.remove( new Integer(leavingG.getLeaderID()) );
    }


    public void handleEvent( PlayerDeath event ){
        if( !isRunning ) {
            return;
        }

        GangOwner victorG = m_gangOwners.get( new Integer(event.getKillerID()) );
        GangOwner loserG  = m_gangOwners.get( new Integer(event.getKilleeID()) );
        if( victorG == null ) {
            if( DEBUG )
                Tools.printLog( "Winner not retrieved on death" );
            return;
        }
        if( loserG == null ) {
            if( DEBUG )
                Tools.printLog( "Loser not retrieved on death" );
            return;
        }
                
        // Victor is a gang leader
        if( victorG.isGangActive() ) {
            victorG.addLeaderKill();
            if( loserG.isGangActive() ) {
                /*
                Player victorP = m_botAction.getPlayer( event.getKillerID() );
                */
                Player loserP  = m_botAction.getPlayer( event.getKilleeID() );
                if( loserP == null ) {
                    if( DEBUG )
                        Tools.printLog( "Loser not retrieved in player record on death" );
                    return;
                }
                
                if( !loserG.addLeaderDeath() )
                    return;
                    
                int stolenCronie = loserG.removeWorstCronie();
                
                // If player has a cronie to steal
                if( stolenCronie != -1 ) {
                    GangOwner stolenG    = m_gangOwners.get( new Integer(stolenCronie) );
                    Player stolenCronieP = m_botAction.getPlayer( stolenCronie );
                    if( stolenCronieP == null ) {
                        if( DEBUG )
                            Tools.printLog( "Stolen cronie found, but player object came up null." );
                        return;
                    }
                    boolean success = victorG.addNewCronie( stolenCronie );
                    if( success ) {
                        m_botAction.sendArenaMessage( victorG.getName() + " recruited " + stolenCronieP.getPlayerName() + " from " + loserP.getPlayerName() + ".");
                        stolenG.makeCronieOf( victorG.getLeaderID() );
                        m_botAction.setFreq( stolenCronie, victorG.getGangFrequency() );
                    } else {
                        m_botAction.sendArenaMessage( victorG.getName() + " liberated " + stolenCronieP.getPlayerName() + " from " + loserP.getPlayerName() + " for the time being." );
                        loserG.makeCronieOf( victorG.getLeaderID() );
                        victorG.setCronieInWaiting( stolenCronie );
                        m_botAction.sendPrivateMessage( victorG.getLeaderID(), stolenCronieP.getPlayerName() + " (" + stolenCronieP.getWins() + " kills) now available.  Type 1/2/3/4 to swap for current cronie (type 'c' for list)" );
                        // Put person on their own freq for now (in case they won't use them)
                        stolenG.makeGangActive();
                        m_botAction.setFreq( stolenCronie, stolenG.getGangFrequency() );
                    }
                    
                // Player has no cronie to steal; can be recruited & gang made inactive
                } else {
                    boolean success = victorG.addNewCronie( loserP.getPlayerID() );
                    if( success ) {
                        m_botAction.sendArenaMessage( victorG.getName() + " recruited " + loserP.getPlayerName() + ".");
                        m_botAction.sendPrivateMessage( loserP.getPlayerID(), victorG.getName() + " has recruited you; you no longer lead a gang, but also can not be eliminated." );
                        loserG.makeCronieOf( victorG.getLeaderID() );
                        m_botAction.setFreq( loserP.getPlayerID(), victorG.getGangFrequency() );
                    } else {
                        m_botAction.sendPrivateMessage( loserP.getPlayerID(), victorG.getName() + " now has the option to recruit you (has a full gang)." );
                        loserG.makeCronieOf( victorG.getLeaderID() );
                        victorG.setCronieInWaiting( loserP.getPlayerID() );
                        m_botAction.sendPrivateMessage( victorG.getLeaderID(), loserP.getPlayerName() + " (" + loserP.getWins() + " kills) now available.  Type 1/2/3/4 to swap for current cronie (type 'c' for list)" );
                    }
                }
            }           
        
        // Victor is not a gang leader --
        //    just check if loser is a leader to increment deaths/eliminate
        } else {
            victorG.addCronieKill();
            if( loserG.isGangActive() ) {
                loserG.addLeaderDeath();
            }
        }        
    }
    
    
    // ***** COMMAND HANDLING *****

    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( opList.isER( name ))
                handleCommand( name, message );
            else
                if( message.equals("!help") )
                    m_botAction.privateMessageSpam(event.getPlayerID(), getPlayerHelpMessage() );
        } else {
            if( event.getMessageType() == Message.PUBLIC_MESSAGE ){
                if( message.length() == 1 ) {
                    if( message.equals("1") )
                        cmdSwap( event.getPlayerID(), 1 );
                    else if( message.equals("2") ) 
                        cmdSwap( event.getPlayerID(), 2 );
                    else if( message.equals("3") ) 
                        cmdSwap( event.getPlayerID(), 3 );
                    else if( message.equals("4") ) 
                        cmdSwap( event.getPlayerID(), 4 );
                    else if( message.equals("g") ) 
                        cmdListCronies( event.getPlayerID() );
                    else if( message.startsWith("l") )
                        cmdLeaders( event.getPlayerID() );
                } else {
                    if( message.startsWith("w ") )
                        cmdWho( event.getPlayerID(), message.substring( 2 ) );
                }
            }
        }
    }
    
    public void handleCommand( String name, String message ){
        if( message.startsWith( "!start ") )
            cmdStart( name, message.substring(7) );
        else if( message.startsWith( "!start" ))
            cmdStart( name, DEFAULT_LEADER_DEATHS );            
        else if( message.startsWith( "!stop" )) {
            if( !isRunning ) {            
                m_botAction.sendPrivateMessage(name, "Gangs of TW already stopped." );
                return;
            }
            m_botAction.sendArenaMessage( "Gangs of TW deactivated by " + name + "." );
            m_gangOwners.clear();
            try {
                if( m_checkForWinnersTask != null ) {
                    m_botAction.cancelTask( m_checkForWinnersTask );
                }
            } catch(Exception e) {
                try {
                    m_checkForWinnersTask.cancel();
                } catch( Exception e2 ) {
                }
            }
            isRunning = false;
        } else if( message.startsWith("!rules") ) {
            m_botAction.arenaMessageSpam( getPlayerHelpMessage() );
        }
    }
    
    public void cmdSwap( int id, int slot ) {
        GangOwner g = m_gangOwners.get(id);
        if( g != null ) {
            Player swapIn = m_botAction.getPlayer( g.getCronieInWaiting() );
            Player swapOut = m_botAction.getPlayer( g.swapCronie( slot ) );
            if( swapOut == null ) 
                m_botAction.sendPrivateMessage( id, "Swap failed.  Check your cronies again (type c for a list)." );
            else {
                if( swapIn == null || swapOut == null )
                    m_botAction.sendPrivateMessage( id, "Swap successful.  Good riddance." );
                else
                    m_botAction.sendPrivateMessage( id, swapIn.getPlayerName() + " swapped in for " + swapOut.getPlayerName() + "." );
            }
                
        }
    }

    public void cmdListCronies( int id ) {
        GangOwner g = m_gangOwners.get(id);
        LinkedList<String> display;
        if( g.getCronieTo() != -1 ) {
            g = m_gangOwners.get( g.getCronieTo() );
            if( g == null ) {
                m_botAction.sendPrivateMessage( id, "You are a cronie in a gang, but your leader can't be found.  Report to a staff member!" );
                return;
            }
            display = getGangStatus( g, true );
            if( display != null ) {
                m_botAction.privateMessageSpam( id, display );
            }
            m_botAction.sendPrivateMessage( id, "You are a cronie in this gang.  Protect your leader; your deaths do not count against you." );
        } else {
            display = getGangStatus( g, true );
            if( display != null ) {
                m_botAction.privateMessageSpam( id, display );
            }
        }        
    }
        
    public void cmdWho( int id, String message ) {
        int matchID = -1;
        if( Tools.isAllDigits(message) ) {
            Integer freq = Integer.parseInt(message);
            Iterator<Integer> i = m_botAction.getFreqIDIterator(freq);
            if( i.hasNext() ) {
                matchID = i.next();
            } else {
                m_botAction.sendPrivateMessage( id, "Freq " + freq + " not found." );               
            }
            
        } else {
            Player p = m_botAction.getFuzzyPlayer( message );
            if( p == null ) {
                m_botAction.sendPrivateMessage( id, "Player '" + message + "' not found." );
                return;
            }
            matchID = p.getPlayerID();
        }
        if( matchID == -1 )
            return;
        GangOwner go = m_gangOwners.get( new Integer(matchID) );
        if( go == null ) { 
            m_botAction.sendPrivateMessage( id, "Does not appear to be playing." );
            return;
        }
        
        if( !go.isGangActive() ) {
            matchID = go.getCronieTo();
            go = m_gangOwners.get( new Integer(matchID) );
        }
        if( go == null ) { 
            m_botAction.sendPrivateMessage( id, "Does not appear to be playing." );
            return;
        }
        if( !go.isGangActive() ) {
            m_botAction.sendPrivateMessage( id, "Does not appear to be playing." );
            return;
        }
        
        LinkedList <String>display = getGangStatus( go, false );
        
        if( display == null ) {
            m_botAction.sendPrivateMessage( id, "Does not appear to be playing." );
        } else {
            m_botAction.privateMessageSpam( id, display );          
        }
    }
    
    public LinkedList<String> getGangStatus( GangOwner g, boolean isInGang ) {
        if( g != null ) {
            LinkedList<String> display = new LinkedList<String>();            
            display.add( "LEADER:  " + Tools.formatString( g.getName(), 30 ) + "  -  Leader K/D:  " + g.getLeaderKills() + "-" + g.getLeaderDeaths() );
            for( int i=0; i<g.cronies.length; i++) {
                GangOwner c = m_gangOwners.get( g.cronies[i] );
                if( c != null )
                    display.add( "Slot " + (i+1) + ")   " + Tools.formatString( c.getName(), 30 ) + "  -  " + c.getTotalKills() + " kills");
            }
            if( isInGang ) {
                GangOwner waitingG = m_gangOwners.get( g.getCronieInWaiting() );
                if( waitingG != null ) {
                    display.add( "  Swap)   " + Tools.formatString( waitingG.getName(), 30 ) + "  -  " + waitingG.getTotalKills() + " kills  (type slot # to swap in)");                
                }
            }
            return display;
        } else {
            return null;
        }
    }
    
    public void cmdLeaders( int id ) {
        LinkedList<String> leaderDisplay = new LinkedList<String>();
        leaderDisplay.add("Gang Leaders        [Display format:  freq#:name(#deaths) ]");
        String line = "";

        int listingColumn = 0;
        for( GangOwner g : m_gangOwners.values() ) {
            if( g != null && g.isGangActive() ) {
                String dispAdd = Tools.formatString( ( g.getGangFrequency() + ":" + g.getName() + "(" + g.getLeaderDeaths() + ")" ), 20 ) + "  "; 
                if( listingColumn % 4 == 3 ) {
                    leaderDisplay.add( line );
                    line = dispAdd;
                } else {
                    line += dispAdd;
                }
                listingColumn++;
            }
        }
        m_botAction.privateMessageSpam(id, leaderDisplay);
    }
    
    public void cmdStart( String name, String deaths ) {
        if( Tools.isAllDigits(deaths) ) {
            cmdStart( name, Integer.parseInt( deaths ));
        } else {
            m_botAction.sendPrivateMessage(name, "Syntax: !start <#>, where # is the number of gangleader deaths to play to." );
        }
    }
    
    public void cmdStart( String name, int deaths ) {
        if( isRunning ) {            
            m_botAction.sendPrivateMessage(name, "Gangs of TW already started." );
            return;
        }
        m_maxLeaderDeaths = deaths;
        m_gangOwners.clear();
        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        while( i.hasNext() ) {
            Player p = i.next();
            if( p != null ) {
                GangOwner pg = new GangOwner( p.getPlayerID(), p.getFrequency() );
                m_gangOwners.put( new Integer( p.getPlayerID() ), pg );
            }
        }
            
        try {
            if( m_checkForWinnersTask != null ) {
                m_botAction.cancelTask( m_checkForWinnersTask );
            }
        } catch(Exception e) {
            try {
                m_checkForWinnersTask.cancel();
            } catch( Exception e2 ) {
                m_botAction.sendPrivateMessage(name, "Unable to check for win condition.  You'll have to monitor it yourself.  Please notify bot coders or make a new task on twcore.org." );
            }
        }
        
        m_checkForWinnersTask = new TimerTask() {
            public void run() {
                if( !isRunning ) {
                    m_botAction.cancelTask( this );
                    return;
                }
                
                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                boolean multiFreqs = false;
                int firstFreq = -1;
                while( i.hasNext() && multiFreqs == false ) {
                    Player p = i.next();
                    if( p != null ) {
                        if( firstFreq == -1 )
                            firstFreq = p.getFrequency();
                        else {
                            if( p.getFrequency() != firstFreq )
                                multiFreqs = true;
                        }
                    }
                }
                
                // WINRAR!
                if( multiFreqs == false && firstFreq != -1 ) {
                    int leaderID = -1;
                    for( int id : m_gangOwners.keySet() ) {
                        GangOwner ga = m_gangOwners.get( id );
                        if( ga.getGangFrequency() == firstFreq ) {
                            if( ga.isGangActive() ) {
                                leaderID = ga.getLeaderID();
                                break;
                            } else {
                                leaderID = ga.getCronieTo();
                                break;
                            }
                        }
                    }
                    //Player p = m_botAction.getPlayer( leaderID );
                    GangOwner g = m_gangOwners.get( leaderID );
                    if( g != null ) {
                        LinkedList <String>display = new LinkedList<String>();
                        display.add( Tools.formatString("", 70, ".") );
                        display.add( Tools.formatString(".   GANGS OF TW - Endgame.", 69 ) + "." );
                        display.add( Tools.formatString(".", 69 ) + "." );
                        display.add( Tools.formatString(".     WINNER:  The " + winnerNames.grab() + " " + g.getName(), 69 ) + "." );
                        display.add( Tools.formatString(".              Kills as leader: " + g.leaderKills + "  Deaths: " + g.leaderDeaths, 69 ) + "." );
                        display.add( Tools.formatString(".              Kills as cronie: " + g.cronieKills, 69 ) + "." );
                        for( int j=0; j<g.cronies.length; j++ ) {
                            GangOwner c = m_gangOwners.get( g.cronies[j] );
                            if( c != null )
                                display.add( Tools.formatString(".     Cronie " + (j+1) + ", " + c.getName() + ".  Leader K/D: " + c.leaderKills + "-" + c.leaderDeaths + "  CronieKs: " + c.cronieKills, 69 ) + "." );
                        }
                        display.add( Tools.formatString(".", 69 ) + "." );
                        display.add( Tools.formatString("", 70, ".") );
                        for( int j=0; j<display.size(); j++ )
                            m_botAction.sendArenaMessage( display.get(j) );
                    } else {
                        m_botAction.sendArenaMessage( "Gangs of TW has ended.", Tools.Sound.HALLELUJAH );                        
                    }
                    m_gangOwners.clear();
                    isRunning = false;
                }
            }
        };
        m_botAction.scheduleTask(m_checkForWinnersTask, 5000, 5000 );
        
        m_botAction.sendArenaMessage( "Gangs of TW activated by " + name + "!  Gang leaders removed at " + deaths + " deaths." );
        isRunning = true;            
    }

    
    // ***** GANG CLASS *****
    
    /**
     * Keeps track of gang-related information for a particular person.  If the
     * person becomes someone else's cronie, much of this information becomes
     * irrelevant, lying dormant until they start their own gang.
     */
    public class GangOwner {
        final static int NUM_CRONIES = 4;
        int   leaderID;
        String leaderName;
        int   leaderFreq;   // Freq player is on when gang is active
        int[] cronies = new int[NUM_CRONIES];
        int   swapCronie;   // ID of person most recently killed that you couldn't add;
                            //   they can be added as a cronie until someone else adds
                            //   them or you kill another person.
        int leaderDeaths;
        int leaderKills;
        int cronieKills;
        boolean isActive;   // Whether or not this gang is "active"
                            //   (if the player actually "owns" his/her own gang)
        int cronieTo;       // ID of player this player is a cronie to; -1 if none
        
        public GangOwner( int leaderID, int leaderFreq ) {
            isActive = true;
            this.leaderID = leaderID;
            this.leaderFreq = leaderFreq;
            for( int i=0; i<cronies.length; i++ )
                cronies[i] = -1;
            swapCronie = -1;
            leaderDeaths = 0;
            leaderKills  = 0;
            cronieTo = -1;
            Player p = m_botAction.getPlayer( leaderID );
            if( p != null )
                leaderName = p.getPlayerName();
            else
                leaderName = "(Anonymous)";
        }
        
        /**
         * Removes the worst cronie (least kills) from the gang.
         * @return ID of cronie removed; -1 if no cronies to remove
         */
        public int removeWorstCronie() {
            int worstID = -1;
            int lowestKills = 0;
            for( int i=0; i<cronies.length; i++ ) {
                if( cronies[i] != -1 ) {
                    Player p = m_botAction.getPlayer(cronies[i]);
                    if( p != null ) {
                        if( p.getWins() < lowestKills || lowestKills == 0 ) {
                            worstID = cronies[i];
                            lowestKills = p.getWins();
                        }
                    }
                }
            }
            if( worstID != -1 ) {
                for( int i=0; i<cronies.length; i++ ) {
                    if( cronies[i] == worstID ) {
                        Player p = m_botAction.getPlayer(worstID);
                        if( p != null ) {
                            cronies[i] = -1;
                            if( swapCronie != -1 )
                                swapCronie( i );
                            return worstID;
                        }
                    }
                }
            }
            return -1;
        }
        
        public boolean removeSpecificCronie( int id ) {
            for( int i=0; i<cronies.length; i++ ) {
                if( cronies[i] == id ) {
                    cronies[i] = -1;
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Assigns best cronie (most kills) to take over gang after leader dies.
         */
        public void assignBestCronieToTakeOver() {
            int bestslot = -1;
            int highestKills = -1;
            int totalCronies = 0;
            for( int i=0; i<cronies.length; i++ ) {
                if( cronies[i] != -1 ) {
                    totalCronies++;
                    Player p = m_botAction.getPlayer(cronies[i]);
                    if( p != null ) {
                        if( p.getWins() > highestKills ) {
                            bestslot = i;
                            highestKills = p.getWins();
                        }
                    }
                }
            }
            if( bestslot != -1 ) {
                GangOwner bestC = m_gangOwners.get( cronies[bestslot] );
                if( bestC != null ) {
                    bestC.makeGangActive();
                    if( totalCronies == 1 ) {
                        m_botAction.sendPrivateMessage( bestC.getLeaderID(), "The " + winnerNames.grab() + " " + leaderName + " was " + killNames.grab() + "!  You're alone once more...", 1 );
                        return;
                    }
                    for( int i=0; i<cronies.length; i++ ) {
                        // Give best cronie the other cronies.
                        if( cronies[i] != -1 && i != bestslot ) {
                            GangOwner c = m_gangOwners.get( cronies[i] );
                            if( c != null ) {
                                bestC.addNewCronie( c.getLeaderID() );
                                c.cronieTo = bestC.getLeaderID();
                                m_botAction.sendOpposingTeamMessage(bestC.getLeaderID(), "The " + winnerNames.grab() + " " + leaderName + " was " + killNames.grab() + "!  Now, " + bestC.getName() + " is our leader.", 1);
                            }
                        }
                    }
                }                
            }
            
        }
        
        /**
         * Get first open cronie slot.
         * @return Empty slot number; -1 if no open slots. 
         */
        public int getOpenCronieSlot() {
            for( int i=0; i<cronies.length; i++ )
                if( cronies[i] == -1 )
                    return i;
            return -1;
        }
        
        /**
         * Adds a new cronie to the player's gang.
         * @param cronieID ID of player being added
         * @return True if successful (had open slot); false if no slots (set as cronie to swap)
         */
        public boolean addNewCronie( int cronieID ) {
            for( int i=0; i<cronies.length; i++ ) {
                if( cronies[i] == -1 ) {
                    cronies[i] = cronieID;
                    m_botAction.setFreq( cronieID, leaderFreq );
                    return true;
                }
            }
            swapCronie = cronieID;
            return false;
        }

        /**
         * @param cronieID
         * @return True if a given cronie is in this gang.
         */
        public boolean isCronieInGang( int cronieID ) {
            for( int i=0; i<cronies.length; i++ ) {
                if( cronies[i] == cronieID ) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Removes cronie-in-waiting from being able to be swapped in
         * (occurs over all gangs whenever a cronie joins a new gang).
         * @param swapID
         * @return
         */
        public boolean removeMatchedWaitingCronie( int swapID ) {
            if( swapCronie == swapID ) {
                swapCronie = -1;
                m_botAction.sendPrivateMessage( leaderID, "Your cronie-in-waiting joined another gang." );
                return true;
            }
            return false;
        }
        
        /**
         * Swaps out cronie in given slot number for cronie-in-waiting.
         * @param slot
         * @return
         */
        public int swapCronie( int slot ) {
            if( swapCronie == -1 )
                return -1;
            slot--;
            if( slot < 0 || slot >= cronies.length )
                return -1;
            Player swapIn = m_botAction.getPlayer( swapCronie );
            if( swapIn == null || swapIn.getShipType() < 1 ) {
                m_botAction.sendPrivateMessage( leaderID, "That cronie is no longer available." );
                swapCronie = -1;
                return -1;
            }
            int swappedOutCronie = cronies[slot];
            cronies[slot] = swapCronie;
            swapCronie = -1;
            Player leaderP = m_botAction.getPlayer(leaderID);
            if( leaderP != null ) {
                m_botAction.sendPrivateMessage( cronies[slot], "You have replaced a member of " + leaderName + "'s gang!" );
                m_botAction.sendPrivateMessage( swappedOutCronie, "You have been kicked out of " + leaderName + "'s gang.  You lead your own gang again." );
            }
            return swappedOutCronie;
        }
                
        public void setCronieInWaiting( int id ) {
            swapCronie = id;
        }
                
        public void makeCronieOf( int newid ) {
            if( cronieTo != -1 && newid != cronieTo ) {                
                GangOwner curLeader = m_gangOwners.get( cronieTo );
                if( curLeader != null )
                    curLeader.removeMatchedWaitingCronie( leaderID );
            }
            cronieTo = newid;
            makeGangInactive();
        }
        
        public void makeGangActive() {
            if( isActive )
                return;
            isActive = true;
            cronieTo = -1;
            m_botAction.setFreq(leaderID, leaderFreq);
        }
        
        public void makeGangInactive() {
            if( !isActive )
                return;
            isActive = false;
            swapCronie = -1;
            for( int i=0; i<cronies.length; i++ )
                cronies[i] = -1;
        }
        
        /**
         * @return True if player owns their own gang; false if they're someone's cronie
         */
        public boolean isGangActive() {
            return isActive;
        }
        
        /**
         * Add a death, and eliminate the person if over the death limit.
         * @return True if still kicking; false if eliminated
         */
        public boolean addLeaderDeath() {
            leaderDeaths++;
            if( leaderDeaths < m_maxLeaderDeaths )
                return true;
            m_botAction.specWithoutLock( leaderID );
            m_botAction.sendArenaMessage( leaderName + " eliminated.  Leader stats: " + leaderKills + " kills, " + leaderDeaths + " deaths.  (" + cronieKills + " kills as cronie.)");
            assignBestCronieToTakeOver();
            return false;
        }
        
        public void addLeaderKill() {
            leaderKills++;
        }
        public void addCronieKill() {
            cronieKills++;
        }
        
        public int getLeaderID() {
            return leaderID;
        }
        public String getName() {
            return leaderName;
        }
        public int getGangFrequency() {
            return leaderFreq;
        }
        public int getCronieInWaiting() {
            return swapCronie;
        }
        public int getLeaderKills() {
            return leaderKills;
        }
        public int getLeaderDeaths() {
            return leaderDeaths;
        }
        public int getCronieKills() {
            return cronieKills;
        }
        public int getTotalKills() {
            return leaderKills + cronieKills;
        }
        public int getCronieTo() {
            return cronieTo;
        }
        
    }
}
