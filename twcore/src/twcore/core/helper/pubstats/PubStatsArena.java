package twcore.core.helper.pubstats;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import twcore.core.game.Player;

public class PubStatsArena {
    Map<Short,PubStatsPlayer> players = Collections.synchronizedMap(new HashMap<Short, PubStatsPlayer>());
    // Short = Player ID
    
    private String name;
    
    public PubStatsArena(String name) {
        this.name = name;
    }
    
    /**
     * Returns a PubStatsPlayer matching the given id
     * @param id
     * @return
     */
    public PubStatsPlayer getPlayer(short id) {
        return players.get(id);
    }
    
    /**
     * Gets a PubStatsPlayer by the player name.
     * Returns the first found PubStatsPlayer where his name starts with the given shortName.
     * 
     * @param shortName the short playername (the name from %tickname/playerlist) 
     * @return
     */
    public PubStatsPlayer getPlayer(String shortName) {
        for(PubStatsPlayer player : players.values()) {
            if(player.getName().startsWith(shortName)) {
                return player;
            }
        }
        
        return null;
    }
    
    /**
     * Gets a PubStatsPlayer by the player name. 
     * Returns the first found PubStatsPlayer where his name is the first part of the given longName.
     * 
     * @param longName the long playername (the name from *info)
     * @return
     */
    public PubStatsPlayer getPlayer2(String longName) {
        for(PubStatsPlayer player : players.values()) {
            if(longName.startsWith(player.getName())) {
                return player;
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
        players.put(p.getPlayerID(), player);
        return player;
    }
    
    public void addPlayer(short playerID, String name, String squad, int flagPoints, int killPoints, int losses, int wins, short ship) {
        PubStatsPlayer player = new PubStatsPlayer(name, squad, ship);
        player.setFlagPoints(flagPoints);
        player.setKillPoints(killPoints);
        player.setLosses(losses);
        player.setWins(wins);
        this.players.put(playerID, player);
    }
    
    public int size() {
        return players.size();
    }
    
    /**
     * Remove all players that haven't been changed since last save. (lastSave == lastUpdated)
     */
    public void cleanOldPlayers() {
        Iterator<PubStatsPlayer> it = players.values().iterator();
        
        while(it.hasNext()) {
            PubStatsPlayer player = it.next();
            
            if(player.getLastSave() > 0 && player.getLastSave() == player.getLastUpdate()) {
                it.remove();
            }
        }
    }
    
    /**
     * @return the players of this arena
     */
    public Map<Short,PubStatsPlayer> getPlayers() {
        return players;
    }
    

}
