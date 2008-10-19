package twcore.core.helper.pubstats;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import twcore.core.game.Player;

public class PubStatsArena {
    Map<String,PubStatsPlayer> players = Collections.synchronizedMap(new HashMap<String, PubStatsPlayer>());
    // String = Playername (%tickname) (lowercase)
    
    private String name;
    
    public PubStatsArena(String name) {
        this.name = name;
    }
    
    /**
     * Gets a PubStatsPlayer by the exact player name (case insensitive).
     * 
     * @param name the playername 
     * @return
     */
    public PubStatsPlayer getPlayer(String name) {
        Collection<PubStatsPlayer> pubstatsPlayers = players.values();
        
        synchronized(players) {
            for(PubStatsPlayer player : pubstatsPlayers) {
                if(player.getName().equals(name)) {
                    return player;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Gets a PubStatsPlayer by the player name (case insensitive).
     * Returns the first found PubStatsPlayer where his name starts with the given shortName.
     * 
     * @param shortName the short playername (the name from %tickname/playerlist) 
     * @return
     */
    public PubStatsPlayer getPlayerOnPartialName(String shortName) {
        Collection<PubStatsPlayer> pubstatsPlayers = players.values();
        
        synchronized(players) {
            for(PubStatsPlayer player : pubstatsPlayers) {
                if(player.getName().startsWith(shortName)) {
                    return player;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Gets a PubStatsPlayer by the player name (case insensitive). 
     * Returns the first found PubStatsPlayer where his name is the first part of the given longName.
     * 
     * @param longName the long playername (the name from *info)
     * @return
     */
    public PubStatsPlayer getPlayerOnPartialName2(String longName) {
        Collection<PubStatsPlayer> pubstatsPlayers = players.values();
        
        synchronized(players) {
            for(PubStatsPlayer player : pubstatsPlayers) {
                if(longName.startsWith(player.getName())) {
                    return player;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Adds a subspace player to the list of players this object contains
     * @param p
     * @return the newly created PubStatsPlayer
     */
    public PubStatsPlayer addPlayer(Player p) {
        PubStatsPlayer player = new PubStatsPlayer(p.getPlayerName(), p.getSquadName(), p.getShipType());
        player.setFlagPoints(p.getFlagPoints());
        player.setKillPoints(p.getKillPoints());
        player.setLosses(p.getLosses());
        player.setWins(p.getWins());
        players.put(p.getPlayerName().toLowerCase(), player);
        return player;
    }
    
    public void addPlayer(String name, String squad, int flagPoints, int killPoints, int losses, int wins, short ship) {
        PubStatsPlayer player = new PubStatsPlayer(name, squad, ship);
        player.setFlagPoints(flagPoints);
        player.setKillPoints(killPoints);
        player.setLosses(losses);
        player.setWins(wins);
        this.players.put(name.toLowerCase(), player);
    }
    
    public int size() {
        return players.size();
    }
    
    /**
     * Remove all players that haven't been changed since last save. (lastSave == lastUpdated)
     */
    public void cleanOldPlayers() {
        Collection<PubStatsPlayer> pubstatsPlayers = players.values();
        
        synchronized(players) {
            Iterator<PubStatsPlayer> it = pubstatsPlayers.iterator();
            
            while(it.hasNext()) {
                PubStatsPlayer player = it.next();
                
                if(player.getLastSave() > 0 && player.getLastSave() == player.getLastUpdate() && player.isPeriodReset() == false) {
                    it.remove();
                }
            }
        }
    }
    
    /**
     * @return the players of this arena
     */
    public Map<String,PubStatsPlayer> getPlayers() {
        return players;
    }
    
    /**
     * Sets periodReset boolean to true of all players
     */
    public void globalScorereset() {
        Collection<PubStatsPlayer> pubstatsPlayers = players.values();
        
        synchronized(players) {
            Iterator<PubStatsPlayer> it = pubstatsPlayers.iterator();
            
            while(it.hasNext()) {
                PubStatsPlayer player = it.next();
                player.setPeriodReset(true);
            }
        }
    }

}
