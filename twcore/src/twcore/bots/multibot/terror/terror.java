package twcore.bots.multibot.terror;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.TreeMap;


import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * Terror a.k.a. "Don't kill the terr!"
 * 
 * @author fantus
 * 
 */
public class terror extends MultiModule {
    private int state;
    private int deaths;
    private int ship;
    
    private TreeMap<String, Integer> lates;
    
    private String terrier;
    private String host;
    
    private String mvpName;
    private int mvpWins;

    private static final String RULES =
	    "Rules: There are teams in this deathmatch style game. " +
	    "Each team is given a varied amount of lives. " +
	    "However there's a twist! " +
	    "If you kill the terr you will automatically lose. " +
	    "Last surviving team wins. " +
	    "Enter if playing!!";
	
    private static final int OFF = 0;
    private static final int RUNNING = 1;
    
    private static final int TERR_FREQ = 7322; //7322 is leet speak for TERR ;p

    public void cancel() {}

    public String[] getModHelpMessage() {
        String[] stepElimHelp = {
                "!start <name>                      -- Starts a game of Terror, with <name> as terr",
                "!setteam <#>                       -- Set <#> number of teams",
                "!setship <#>                       -- Set everyone to ship <#>",
                "!setdeath <#>                      -- Set death limit",
                "!spam rules                        -- Displays the rules in a arena message",
                "!subterr <name>                    -- Substitute the terrier with <name>",
                "!add <name>:<# deaths>:<# freq>    -- Adds <name> with <# deaths> on <# freq>",
                "NOTICE: The bot does NOT lock the arena. Adding lates is OK (use !add)"
        };
        return stepElimHelp;
    }

    public void init() {
        lates = new TreeMap<String, Integer>();
        reset();
    }

    public boolean isUnloadable() {
        if (state == OFF)
            return true;
        else
            return false;
    }
    
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        eventRequester.request(this, EventRequester.PLAYER_DEATH);
        eventRequester.request(this, EventRequester.PLAYER_LEFT);
    }

    public void handleEvent(Message event) {
		String name = m_botAction.getPlayerName(event.getPlayerID());
		String message = event.getMessage();
		
		//Check if the messager is a ER+
		if (!m_botAction.getOperatorList().isER(name))
			return;
		
		host = name;
		
		if (message.equalsIgnoreCase("!spam rules"))
			m_botAction.sendArenaMessage(RULES, 3);
		else if (message.startsWith("!setteam "))
			cmdSetTeam(message.substring(9));
		else if (message.startsWith("!setship "))
			cmdSetShip(name, message.substring(9));
		else if (message.startsWith("!setdeath "))
			cmdSetDeath(name, message.substring(10));
		else if (message.startsWith("!start "))
		    cmdStart(name, message.substring(7));
		else if (message.startsWith("!subterr "))
		    cmdSubTerr(name, message.substring(9));
		else if (message.startsWith("!add "))
		    cmdAdd(name, message.substring(5));
	}
	
    public void handleEvent(PlayerLeft event) {
        if (state == RUNNING)
            checkGameOver();
        
        Player p = null;
        
        try { p = m_botAction.getPlayer(event.getPlayerID()); } catch (Exception e) {}
        
        if (p == null)
            return;
        
        if (!p.getPlayerName().equalsIgnoreCase(terrier))
            return;
        
        m_botAction.sendPrivateMessage(host, "The terrier has left the game! Set someone else to terr.");
    }
    
    public void handleEvent(FrequencyShipChange event) {
        if (state == RUNNING)
            checkGameOver();
        
        Player p = null;
        
        try { p = m_botAction.getPlayer(event.getPlayerID()); } catch (Exception e) {}
        
        if (p == null)
            return;
        
        if (!p.getPlayerName().equalsIgnoreCase(terrier))
            return;
        
        if (p.getShipType() == Tools.Ship.TERRIER)
            return;
        
        m_botAction.sendPrivateMessage(host, "The terrier has left the game! Set someone else to terr.");
    }

    public void handleEvent(PlayerDeath event) {
        if (state != RUNNING)
            return;
        
        Player killer = null;
        Player killed = null;
        
        try {
            killer = m_botAction.getPlayer(event.getKillerID());
            killed = m_botAction.getPlayer(event.getKilleeID());
        } catch (Exception e) { return; }
        
        if (killer == null || killed == null)
            return;
        
        //Check if someone claims mvp
        if (killer.getWins() > mvpWins) {
            mvpWins = killer.getWins();
            mvpName = killer.getPlayerName();
        }
        
        //Check if someone killed the terrier
        if (killed.getPlayerName().equalsIgnoreCase(terrier)) {
            m_botAction.sendArenaMessage(
                    killer.getPlayerName() +
                    " has killed the terr and is out! " +
                    killer.getWins()+ " kills, " +
                    killer.getLosses() + " deaths",
                    Tools.Sound.SCREAM); 
            m_botAction.specWithoutLock(killer.getPlayerName());
        }
        
        //Check if someone is out by deaths
        if (killed.getLosses() >= deaths && !killed.getPlayerName().equalsIgnoreCase(terrier)) {
            m_botAction.sendArenaMessage(
                    killed.getPlayerName() + " is out! " +
                    killed.getWins() + " kills, " +
                    killed.getLosses() + " deaths");
            m_botAction.specWithoutLock(killed.getPlayerName());
        }
        
        //Check if a late is out
        if (lates.containsKey(killed.getPlayerName().toLowerCase())) {
            if (killed.getLosses() >= lates.get(killed.getPlayerName().toLowerCase()) &&
                    killed.getShipType() != Tools.Ship.SPECTATOR &&
                    !killed.getPlayerName().equalsIgnoreCase(terrier)) {
                m_botAction.sendArenaMessage(
                        killed.getPlayerName() + " is out! " +
                        killed.getWins() + " kills, " +
                        killed.getLosses() + " deaths");
                m_botAction.specWithoutLock(killed.getPlayerName());
            }
        }
    }
    
    private void cmdAdd(String name, String parameters) {
        if (state != RUNNING) {
            m_botAction.sendPrivateMessage(name, "Error: Game is not yet running, use *setship and *setfreq");
            return;
        }
        
        String[] parameter= parameters.split(":");
        
        if (parameter.length != 3) {
            m_botAction.sendPrivateMessage(name, "Error: Syntax is wrong, check with !help for the correct syntax");
            return;
        }
        
        String playerName = parameter[0];
        int playerDeath = Integer.parseInt(parameter[1]);
        int playerFreq = Integer.parseInt(parameter[2]);
        
        Player p = m_botAction.getFuzzyPlayer(parameter[0]);
        
        if (p == null) {
            m_botAction.sendPrivateMessage(name, "Error: " + parameter[0] + " not found.");
            return;
        }
        
        playerName = p.getPlayerName();
        
        if (playerDeath < 1) {
            m_botAction.sendPrivateMessage(name, "Error: Use a positive value for deaths");
            return;
        }
        
        if (playerFreq == TERR_FREQ && !playerName.equalsIgnoreCase(terrier)) {
            m_botAction.sendPrivateMessage(name, "Error: Cannot put a none terrier on the terr freq");
            return;
        }
        
        if (p.getShipType() != Tools.Ship.SPECTATOR) {
            m_botAction.sendPrivateMessage(name, "Error: Player already in the game");
            return;
        }
        
        if (playerName.equalsIgnoreCase(terrier)) {
            m_botAction.sendPrivateMessage(name, "Error: Cannot add a terrier with this command, use !subterr <name>");
            return;
        }
        
        m_botAction.scoreReset(playerName);
        m_botAction.setShip(playerName, ship);
        m_botAction.setFreq(playerName, playerFreq);
        lates.put(playerName.toLowerCase(), playerDeath);
        m_botAction.sendArenaMessage(playerName + " added with " + playerDeath + " losses left");
    }
    
    private void cmdSetDeath(String name, String number) {
        int deathLimit = Integer.parseInt(number);
        
		if (deathLimit < 0) {
		    m_botAction.sendPrivateMessage(name, "Error: false death limit");
		    return;
		}
		
		deaths = deathLimit;
		
		m_botAction.sendPrivateMessage(name, "Death limit set to " + deaths);
    }
	
    private void cmdSetTeam(String number) {
        int size = Integer.parseInt(number) - 1;
        int current = 0;
        
	    for (Iterator<Player> it = m_botAction.getPlayingPlayerIterator(); it.hasNext();) {
	        if (current > size)
	            current = 0;
	        
	        m_botAction.setFreq(it.next().getPlayerID(), current);
	        current++;
	    }
	}
	
    private void cmdSetShip(String name, String number) {
        ship = Integer.parseInt(number);
        
        if (ship > 8 || ship < 1 || ship == Tools.Ship.TERRIER) {
            m_botAction.sendPrivateMessage(name, "Error: false ship number");
            return;
        }

        for (Iterator<Player> it = m_botAction.getPlayingPlayerIterator(); it.hasNext();) {
            Player p = it.next();
            if (!p.getPlayerName().equalsIgnoreCase(terrier))
                m_botAction.setShip(p.getPlayerID(), ship);
        }
    }

    private void cmdStart(String name, String playerName) {
        if (state == OFF) {
            if (m_botAction.getFuzzyPlayer(playerName) == null) {
                m_botAction.sendPrivateMessage(name, playerName + " could not be found.");
                return;
            }
            
            setTerr(m_botAction.getFuzzyPlayerName(playerName));
            
            m_botAction.sendArenaMessage("Get ready!! Game will start in 10 seconds..", Tools.Sound.BEEP3);
            
            TimerTask startGame = new TimerTask() {
                public void run() {
                    m_botAction.scoreResetAll();
                    m_botAction.shipResetAll();
                    state = RUNNING;
                    m_botAction.sendArenaMessage("Go go go!!!", Tools.Sound.GOGOGO);
                }
            };
            m_botAction.warpAllRandomly();
            m_botAction.scheduleTask(startGame, Tools.TimeInMillis.SECOND * 10);
        }
    }
    
    private void cmdSubTerr(String name, String playerName) {
        if (m_botAction.getFuzzyPlayer(playerName) == null) {
            m_botAction.sendPrivateMessage(name, playerName + " could not be found.");
            return;
        }
        
        if (m_botAction.getPlayer(terrier) != null)
            m_botAction.specWithoutLock(terrier);
        
        setTerr(m_botAction.getFuzzyPlayerName(playerName));
    }
    
    private void setTerr(String name) {
        terrier = name.toLowerCase();
        
        //Set terrier
        m_botAction.setShip(terrier, Tools.Ship.TERRIER);
        m_botAction.setFreq(terrier, TERR_FREQ);
        
        //Check if frequency is only populated with terriers and spectate all the others
        for (Iterator<Player> it = m_botAction.getPlayingPlayerIterator(); it.hasNext();) {
            Player p = it.next();
            
            if (!p.getPlayerName().equalsIgnoreCase(terrier) && p.getFrequency() == TERR_FREQ)
                m_botAction.specWithoutLock(p.getPlayerName());
        }
    }

    private void checkGameOver() {
        ArrayList<Short> freqs = new ArrayList<Short>();
        for (Iterator<Player> it = m_botAction.getPlayingPlayerIterator(); it.hasNext();) {
            Player p = it.next();
            
            if (!freqs.contains(p.getFrequency()) && p.getShipType() != Tools.Ship.SPECTATOR)
                freqs.add(p.getFrequency());
        }
        
        if (freqs.contains(Short.valueOf((short) TERR_FREQ)))
            freqs.remove(Short.valueOf((short) TERR_FREQ));
        
        if (freqs.size() != 1) 
            return;
        
        state = OFF;
        m_botAction.sendArenaMessage(
                "Freq " + freqs.iterator().next() + " = The Winner of Terror!",
                Tools.Sound.HALLELUJAH);
        m_botAction.sendArenaMessage("MVP: " + mvpName + "!");
        
        reset();
    }
    
    private void reset() {
        lates.clear();
        deaths = 10;
        terrier = "[nobody]";
        mvpName = "";
        mvpWins = 0;
        ship = 1;
        state = OFF;
    }
}
