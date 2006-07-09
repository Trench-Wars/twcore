package twcore.bots.twbot;

import java.util.*;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.command.*;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;

/**
 * 2nd Generation Spec module for TWBot.
 *
 * This module allows the user 5 methods of speccing, in order of precedence:
 * 1) speccing a specific player
 * 2) speccing a specific ship on a specific freq
 * 3) speccing a specific ship
 * 5) speccing a specific freq
 * 4) speccing all playing players
 *
 * To utilize internal kill/death tracking that is independent of a player's
 * score and therefore requires no score reset, make sure !RecordDeaths is on.
 * Don't forget to !ResetDeaths before each round begins
 *
 * The syntax for adding or modifying a task (terms in brackets are optional):
 *
 * !Spec [deaths=...] [ship=...] [freq=...] [player=...]
 * !SpecEdit <Task Number> [deaths=...] [ship=...] [freq=...] [player=...]
 *
 * You are allowed to add multiple spec tasks to the bot, but duplicate tasks
 * will automatically be removed. If there are multiple tasks that apply
 * to a player, the above priority will be used. If a task is specified with
 * a player and a ship/freq the ship/freq will be removed from the task. If
 * the death limit is not specified, it will be set to 10, though no task will
 * be created without specifying parameters ("!Spec").
 *
 * Example rule setups:
 *
 * All players at 10 deaths
 * !Spec deaths=10
 *
 * Spiders at 7 deaths
 * !Spec ship=3 deaths=7
 *
 * Freq 0 at 5 deaths
 * !Spec freq=0 deaths=5
 *
 * All javelins on team 1 at 10 deaths
 * !Spec freq=1 ship=2
 *
 * A specific player at 11 deaths (partial names can be used)
 * !Spec player=D1st0rt
 *
 * @author D1st0rt
 * @version 06.07.08
 */
public class twbotspec2 extends TWBotExtension
{
    /** Stores kills/deaths when on internal tracking mode. */
    private Map<Integer, PlayerData> playerData;
    /** Manages the individual Spec Tasks. */
    private ItemCommand<SpecTask> specTasks;
    /** The status of internal death tracking. */
    private boolean recordDeaths;

    /** The help message to be sent to bot operators */
    private final String[] helpMessage =
    {
        "+-------------------Extended Spec Module-------------------+",
        "|  Release 1.0 [07/08/06] - http://d1st0rt.sscentral.com   |",
        "+----------------------------------------------------------+",
        "! !recordDeaths - If this is on, the bot will track kills  |",
        "|                 and deaths internally separate from      |",
        "|                 player records. (no scorereset needed)   |",
        "| !resetDeaths  - Clears internally tracked death counts.  |",
        "|                                                          |",
        "| !Spec         - Create a new rule with given parameters  |",
        "|   |           All parameters are optional as long as you |",
        "|   |           at least specify one of them.              |",
        "|   |                                                      |",
        "|   +- Player=<name>, can be partial. With spaces, use \"\"s.|",
        "|   +- Freq=<0-9999>, Freq and Ship can be used separately |",
        "|   +- Ship=<1-8>,    or together when making a rule.      |",
        "|   +- Deaths=<1-999>, default value is 10 if unspecified. |",
        "|                                                          |",
        "| !SpecEdit <rule number> (then use above parameter syntax)|",
        "| !SpecList     - Displays all created rules.              |",
        "| !SpecDel <#>  - Removes a specified rule.                |",
        "| !SpecOff      - Removes all rules currently in use.      |",
        "|                                                          |",
        "| !AddLife <name>      - These both modify the amount of   |",
        "| !SubtractLife <name>   remaining lives a player has by 1.|",
        "+----------------------------------------------------------+"
    };

    /**
     * Creates a new instance of twbotspec2.
     * @throws Exception if ItemCommand has access problems in the reflection
     *         the module will fail to load.
     */
    public twbotspec2() throws Exception
    {
        //playerData = Collections.synchronizedMap(new HashMap<Integer, PlayerData>());
        playerData = new HashMap<Integer, PlayerData>();

        BotAction botAction = BotAction.getBotAction();

        //default values
        SpecTask defaults = new SpecTask();
        defaults.deaths = 10;
        defaults.freq = -1;
        defaults.ship = -1;
        defaults.player = null;

        specTasks = new ItemCommand<SpecTask>(botAction, defaults);
        specTasks.restrictSetting("freq", 0, 9999);
        specTasks.restrictSetting("ship", 1, 8);
        specTasks.restrictSetting("deaths", 1, 999);

        recordDeaths = false;
    }

    /**
     * Gets the help message for this module
     * @return A string array containing the help information
     */
    public String[] getHelpMessages()
    {
        return helpMessage;
    }

    /**
     * This private method is called when a spec task is added or changed. It
     * makes minor corrections to and removes duplicate rules, then specs any
     * players that are currently over the loss limit.
     * @param task the task to adjust (if applicable)
     */
    private void updateSpec(SpecTask task)
    {
        Iterator iterator = m_botAction.getPlayingPlayerIterator();
        Player player;
        int playerID;
        int freq;
        int ship;
        int deaths;
        SpecTask bestTask;

        // only perform corrections when a task is provided
        if(task != null)
        {
            // if a task is defined for a player, don't define it for a ship/freq
            if(task.player != null)
            {
                task.freq = -1;
                task.ship = -1;
            }

            // don't keep duplicate tasks (delete the old one)
            for(SpecTask task2 : specTasks)
            {
                if(task.ship == task2.ship && task.freq == task2.freq &&
                   task.player == task2.player && task != task2)
                {
                    specTasks.remove(task2);
                    break;
                }
            }
        }

        // check for any players over the new limits
        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            playerID = player.getPlayerID();
            freq = player.getFrequency();
            ship = player.getShipType();
            deaths = 0;
            PlayerData data = null;

            if(recordDeaths)
            {
                data = playerData.get(playerID);
                if(data != null)
                {
                	deaths = data.getDeaths();
                }
            }
            else
            {
                deaths = player.getLosses();
            }


          bestTask = getBestSpecTask(freq, ship, playerID);

          if(bestTask != null && bestTask.deaths <= deaths)
            specPlayer(player, data);
        }
    }

    /**
     * Command: !Spec <variable arguments>
     * Adds a new spec rule.
     */
    private void c_Spec(String name, String message)
    {
        SpecTask task = specTasks.c_Add(name, message);
        updateSpec(task);
        m_botAction.sendArenaMessage(task.toString() + " -" + name);
    }

    /**
     * Command: !SpecEdit <rule number> <variable arguments>
     * Modifies an existing spec rule.
     */
    private void c_SpecEdit(String name, String message)
    {
        SpecTask task = specTasks.c_Edit(name, message);
        if(task != null)
        {
            updateSpec(task);
            m_botAction.sendArenaMessage(task.toString() + " -" + name);
        }
    }

    /**
     * Command: !AddLife <player>
     * Gives one extra life to the target player.
     */
    private void c_AddLife(String name, String message)
    {
        String playerName = m_botAction.getFuzzyPlayerName(message);
        if(playerName != null)
        {
            Player player = m_botAction.getPlayer(playerName);
            int freq = player.getFrequency();
            int ship = player.getShipType();
            int playerID = player.getPlayerID();

            SpecTask specTask = getBestSpecTask(freq, ship, playerID);
            if(specTask == null)
            {
                m_botAction.sendPrivateMessage(name, "Player not currently being watched.");
            }
            else
            {
                int deaths = specTask.deaths + 1;

                SpecTask newTask = new SpecTask();
                newTask.deaths = deaths;
                newTask.player = player;

                specTasks.add(newTask);
                m_botAction.sendArenaMessage(playerName + " granted one extra life.  Now being specced at " + deaths + " deaths.");
                updateSpec(newTask);
            }
        }
        else
        	m_botAction.sendPrivateMessage(name, "Player not found.");
    }

    /**
     * Command: !SubtractLife <player>
     * Takes one life away from the target player.
     */
    private void c_SubtractLife(String name, String message)
    {
        String playerName = m_botAction.getFuzzyPlayerName(message);
        if(playerName != null)
        {
            Player player = m_botAction.getPlayer(playerName);
            int freq = player.getFrequency();
            int ship = player.getShipType();
            int playerID = player.getPlayerID();

            SpecTask specTask = getBestSpecTask(freq, ship, playerID);
            if(specTask == null)
            {
                m_botAction.sendPrivateMessage(name, "Player not currently being watched.");
            }
            else
            {
                int deaths = specTask.deaths - 1;

                SpecTask newTask = new SpecTask();
                newTask.deaths = deaths;
                newTask.player = player;

                specTasks.add(newTask);
                m_botAction.sendArenaMessage(playerName + " penalized one life.  Now being specced at " + deaths + " deaths.");
                updateSpec(newTask);
            }
        }
        else
        	m_botAction.sendPrivateMessage(name, "Player not found.");
    }

    /**
     * Command: !SpecList
     * Displays all currently active rules.
     */
    private void c_SpecList(String name, String message)
    {
        specTasks.c_Display(name, message);
    }

    /**
     * Command: !SpecDel
     * Removes a specified rule.
     */
    private void c_SpecDel(String name, String message)
    {
        SpecTask task = specTasks.c_Remove(name, message);
        if(task != null)
        {
            m_botAction.sendArenaMessage("Removing Task: " + task.toString() + " -" + name);
        }
    }

    /**
     * Command: !SpecOff
     * Removes all rules.
     */
    private void c_SpecOff(String name, String message)
    {
        if(specTasks.size() == 0)
            m_botAction.sendPrivateMessage(name, "No spec tasks to clear.");
        else
        {
            specTasks.clear();
            m_botAction.sendArenaMessage("Removing all spec tasks. -" + name);
        }
    }

    /**
     * Command: !RecordDeaths
     * Toggles internal death tracking.
     */
    private void c_RecordDeaths(String name, String message)
    {
        if(recordDeaths = !recordDeaths)
            m_botAction.sendPrivateMessage(name, "Keeping track of deaths internally.");
        else
            m_botAction.sendPrivateMessage(name, "Using player's score for death count.");
    }

	/**
     * Command !ResetDeaths
     * Clears any internally tracked deaths.
     */
    private void c_ResetDeaths(String name, String message)
    {
        if(recordDeaths)
        {
            playerData.clear();
            m_botAction.sendPrivateMessage(name, "Internal scores reset.");
        }
        else
        {
            m_botAction.sendPrivateMessage(name, "Not tracking scores internally");
        }
    }

    /**
	 * Takes a command and passes it to the proper command function
	 * @param name the name of the sender
	 * @param message the text of the chat message
	 */
    private void delegateCommand(String name, String message)
    {
        if(message.startsWith("!spec "))
            c_Spec(name, message.substring(6).trim());
        else if(message.startsWith("!specedit "))
            c_SpecEdit(name, message.substring(10).trim());
        else if(message.equalsIgnoreCase("!speclist"))
            c_SpecList(name, message);
        else if(message.startsWith("!specdel "))
            c_SpecDel(name, message.substring(9).trim());
        else if(message.equalsIgnoreCase("!specoff"))
            c_SpecOff(name, message);
        else if(message.equalsIgnoreCase("!recordDeaths"))
            c_RecordDeaths(name, message);
        else if(message.equalsIgnoreCase("!resetDeaths"))
            c_ResetDeaths(name, message);
        else if(message.startsWith("!addlife "))
            c_AddLife(name, message.substring(9).trim());
        else if(message.startsWith("!subtractlife "))
        	c_SubtractLife(name, message.substring(14).trim());
    }

    /**
	 * Event: Message
	 * Check and pass to appropriate commands.
	 */
    public void handleEvent(Message event)
    {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        String message = event.getMessage().toLowerCase().trim();

        if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(name))
            delegateCommand(name, message);
    }

    /**
     * Event: PlayerDeath
     * Check to see if player should be specced.
     */
    public void handleEvent(PlayerDeath event)
    {
        int playerID = event.getKilleeID();
        Player player = m_botAction.getPlayer(playerID);
        int freq = player.getFrequency();
        int ship = player.getShipType();
        int deaths = 0;
        PlayerData data = null;

        if(recordDeaths)
        {
            data = playerData.get(playerID);
            if(data == null)
            {
                data = new PlayerData();
            }

            data.addDeath();
            deaths = data.getDeaths();
            playerData.put(playerID, data);

            PlayerData kdata = playerData.get(event.getKillerID());
            if(kdata == null)
            {
                kdata = new PlayerData();
            }

            kdata.addKill();
            playerData.put((int)event.getKillerID(), kdata);
        }
        else
        {
            deaths = player.getLosses();
        }

        SpecTask specTask = getBestSpecTask(freq, ship, playerID);

        if(specTask != null && specTask.deaths <= deaths)
            specPlayer(player, data);
    }

    /**
     * Retrieves the highest priority SpecTask for a given player's info.
     *
     * @param freq is the freq of the player.
     * @param ship is the ship of the player.
     * @param playerID is the ID of the player.
     * @return The appropriate spec task is returned.
     */
    private SpecTask getBestSpecTask(int freq, int ship, int playerID)
    {
        int priority = -1;
        SpecTask bestTask = null;

        // now search for tasks based on ship/freq too
        for(SpecTask task : specTasks)
        {
            int result = task.isApplicable(freq, ship, playerID);
            if(result > priority)
            {
                bestTask = task;
                priority = result;
            }
        }

        return bestTask;
    }

    /**
     * Specs a player and announces it to the arena.
     *
     * @param player is the player to be specced.
     * @param data the won/loss record of the player if using internal tracking
     */
    private void specPlayer(Player player, PlayerData data)
    {
        String playerName = player.getPlayerName();
        int wins = player.getWins();
        int losses = player.getLosses();

        if(recordDeaths)
        {
        	// for whatever reason, the line below was always returning null
            //PlayerData data = playerData.get(player.getPlayerID());
            if(data != null)
            {
                wins = data.getKills();
                losses = data.getDeaths();
            }
        }

        m_botAction.sendArenaMessage(playerName + " is out.  " + wins + " wins, " + losses + " losses.");
        m_botAction.spec(playerName);
        m_botAction.spec(playerName);
    }

    /**
     * This method clears everything before the module unloads.
     */
    public void cancel()
    {
    	specTasks.clear();
    	playerData.clear();
    }

	/**
     * Storage for basic kill/death records used in internal tracking mode.
     *
     * @author D1st0rt
     * @version 06.07.08
     */
    private class PlayerData
    {
    	/** The recorded number of kills. */
        private short kills;
        /** The recorded number of deaths. */
        private short deaths;

		/**
		 * Creates a new instance of PlayerData, kills and deaths start at 0.
		 */
        PlayerData()
        {
            kills = 0;
            deaths = 0;
        }

		/**
		 * Increases the player's kill count.
		 */
        void addKill()
        {
            kills++;
        }

		/**
		 * Increases the player's death count.
		 */
        void addDeath()
        {
            deaths++;
        }

		/**
		 * Gets the player's kill count.
		 * @return the number of kills recorded for this player.
		 */
        short getKills()
        {
            return kills;
        }

		/**
		 * Gets the player's death cound.
		 * @return the number of deaths recorded for this player.
		 */
        short getDeaths()
        {
            return deaths;
        }
    }
}