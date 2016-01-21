package twcore.bots.multibot.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import twcore.bots.MultiUtil;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.command.ItemCommand;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.Tools;
import twcore.core.util.ModuleEventRequester;

/**
    2nd Generation Spec module for TWBot.

    This module allows the user 5 methods of speccing, in order of precedence:
    1) speccing a specific player
    2) speccing a specific ship on a specific freq
    3) speccing a specific ship
    5) speccing a specific freq
    4) speccing all playing players

    To utilize internal kill/death tracking that is independent of a player's
    score and therefore requires no score reset, make sure !RecordDeaths is on.
    Don't forget to !ResetDeaths before each round begins

    The syntax for adding or modifying a task (terms in brackets are optional):

    !Spec [deaths=...] [ship=...] [freq=...] [player=...]
    !SpecEdit <Task Number> [deaths=...] [ship=...] [freq=...] [player=...]

    You are allowed to add multiple spec tasks to the bot, but duplicate tasks
    will automatically be removed. If there are multiple tasks that apply
    to a player, the above priority will be used. If a task is specified with
    a player and a ship/freq the ship/freq will be removed from the task. If
    the death limit is not specified, it will be set to 10, though no task will
    be created without specifying parameters ("!Spec").

    Example rule setups:

    All players at 10 deaths
    !Spec deaths=10

    Spiders at 7 deaths
    !Spec ship=3 deaths=7

    Freq 0 at 5 deaths
    !Spec freq=0 deaths=5

    All javelins on team 1 at 10 deaths
    !Spec freq=1 ship=2

    A specific player at 11 deaths (partial names can be used)
    !Spec player=D1st0rt deaths=11

    @author D1st0rt
    @version 06.10.01
*/
public class utilspec2 extends MultiUtil
{
    /** Stores kills/deaths when on internal tracking mode. */
    private Map<Short, PlayerData> playerData;
    /** Manages the individual Spec Tasks. */
    private ItemCommand<SpecTask> specTasks;
    /** The status of internal death tracking. */
    private boolean recordDeaths;
    /** Whether remaining life notification per death is on or not. */
    private boolean notifyLives;

    /** The help message to be sent to bot operators */
    private final String[] helpMessage =
    {
        "+-------------------Extended Spec Module-------------------+",
        "|  Release 1.1 [10/01/06] - http://d1st0rt.sscentral.com   |",
        "+----------------------------------------------------------+",
        "| !RecordDeaths - If this is on, the bot will track kills  |",
        "|                 and deaths internally separate from      |",
        "|                 player records. (no scorereset needed)   |",
        "| !ResetDeaths  - Clears internally tracked death counts.  |",
        "| !NotifyLives  - Toggles telling the player how many lives|",
        "|                 they have left after every death         |",
        "|                                                          |",
        "| !Spec         - Create a new rule with given parameters  |",
        "|   |           All parameters are optional as long as you |",
        "|   |           at least specify one of them.              |",
        "|   |                                                      |",
        "|   +- Player=\"<name>\", can be partial names like \"d1st\"   |",
        "|   (You only have to use quotes if the name has spaces)   |",
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

    /** The public help message sent to players. */
    private final String publicHelp =
        "!rec               - Displays your current record and lives left.";

    /**
        Creates a new instance of twbotspec2.
    */
    public void init()
    {
        //playerData = Collections.synchronizedMap(new HashMap<Integer, PlayerData>());
        playerData = new HashMap<Short, PlayerData>();

        BotAction botAction = BotAction.getBotAction();

        //default values
        SpecTask defaults = new SpecTask();
        defaults.deaths = 10;
        defaults.freq = -1;
        defaults.ship = -1;
        defaults.player = null;

        try {
            specTasks = new ItemCommand<SpecTask>(botAction, defaults, defaults.getClass());
        } catch ( Exception e ) {
            Tools.printLog("utilspec2: ItemCommand reflection did not work");
        }

        specTasks.restrictSetting("freq", 0, 9999);
        specTasks.restrictSetting("ship", 1, 8);
        specTasks.restrictSetting("deaths", 1, 999);

        recordDeaths = false;
        notifyLives = false;
    }

    /**
        Requests needed events.
    */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.PLAYER_DEATH );
    }

    /**
        Gets the help message for this module
        @return A string array containing the help information
    */
    public String[] getHelpMessages()
    {
        return helpMessage;
    }

    /**
        This private method is called when a spec task is added or changed. It
        makes minor corrections to and removes duplicate rules, then specs any
        players that are currently over the loss limit.
        @param task the task to adjust (if applicable)
    */
    private void updateSpec(SpecTask task)
    {
        Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
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
        Command: !Spec <variable arguments>
        Adds a new spec rule.
    */
    private void c_Spec(String name, String message)
    {
        SpecTask task = specTasks.c_Add(name, message);
        updateSpec(task);
        m_botAction.sendArenaMessage(task.toString() + " -" + name);
    }

    /**
        Command: !SpecEdit <rule number> <variable arguments>
        Modifies an existing spec rule.
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
        Command: !AddLife <player>
        Gives one extra life to the target player.
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
        Command: !SubtractLife <player>
        Takes one life away from the target player.
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
        Command: !SpecList
        Displays all currently active rules.
    */
    private void c_SpecList(String name, String message)
    {
        specTasks.c_Display(name, message);
    }

    /**
        Command: !SpecDel
        Removes a specified rule.
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
        Command: !SpecOff
        Removes all rules.
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
        Command: !RecordDeaths
        Toggles internal death tracking.
    */
    private void c_RecordDeaths(String name, String message)
    {
        if(recordDeaths = !recordDeaths)
            m_botAction.sendPrivateMessage(name, "Keeping track of deaths internally.");
        else
            m_botAction.sendPrivateMessage(name, "Using player's score for death count.");
    }

    /**
        Command: !ResetDeaths
        Clears any internally tracked deaths.
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
        Command: !NotifyLives
        Toggles telling the player how many lives they have left upon death.
    */
    private void c_NotifyLives(String name, String message)
    {
        if(notifyLives = !notifyLives)
            m_botAction.sendPrivateMessage(name, "Telling lives left every death.");
        else
            m_botAction.sendPrivateMessage(name, "Not telling lives left every death.");
    }

    /**
        Public Command: !Rec
        Displays the player's current record and lives remaining.
    */
    private void c_Rec(short playerID)
    {
        int kills = 0;
        int deaths = 0;
        int lives = 0;
        Player p = m_botAction.getPlayer(playerID);

        if(recordDeaths)
        {
            PlayerData pdata = playerData.get(playerID);

            if(pdata != null)
            {
                kills = pdata.getKills();
                deaths = pdata.getDeaths();
            }
        }
        else
        {

            kills = p.getWins();
            deaths = p.getLosses();
        }

        SpecTask task = getBestSpecTask(p.getFrequency(), p.getShipType(), playerID);

        if(task != null)
            lives = task.deaths - deaths;

        StringBuffer s = new StringBuffer("Your record is ");
        s.append(kills);
        s.append("-");
        s.append(deaths);

        if(lives > 0)
        {
            s.append(" with ");
            s.append(lives);
            s.append(" lives remaining.");
        }

        m_botAction.sendPrivateMessage(playerID, s.toString());
    }

    /**
        Takes a command and passes it to the proper command function
        @param name the name of the sender
        @param message the text of the chat message
    */
    private void delegateCommand(String name, String message)
    {
        if(message.startsWith("!spec "))
            c_Spec(name, message.substring(6).trim());
        else if(message.startsWith("!specedit "))
            c_SpecEdit(name, message.substring(10).trim());
        else if(message.equals("!speclist"))
            c_SpecList(name, message);
        else if(message.startsWith("!specdel "))
            c_SpecDel(name, message.substring(9).trim());
        else if(message.equals("!specoff"))
            c_SpecOff(name, message);
        else if(message.equalsIgnoreCase("!recordDeaths"))
            c_RecordDeaths(name, message);
        else if(message.equalsIgnoreCase("!resetDeaths"))
            c_ResetDeaths(name, message);
        else if(message.startsWith("!addlife "))
            c_AddLife(name, message.substring(9).trim());
        else if(message.startsWith("!subtractlife "))
            c_SubtractLife(name, message.substring(14).trim());
        else if(message.equals("!notifylives"))
            c_NotifyLives(name, message);
    }

    /**
        Event: Message
        Check and pass to appropriate commands.
    */
    public void handleEvent(Message event)
    {
        String name = m_botAction.getPlayerName(event.getPlayerID());
        String message = event.getMessage().toLowerCase().trim();

        if(event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            if(m_opList.isER(name))
                delegateCommand(name, message);

            if(message.equals("!rec"))
                c_Rec(event.getPlayerID());

            if(message.startsWith("!help"))
                m_botAction.sendPrivateMessage(name, publicHelp);
        }
    }

    /**
        Event: PlayerDeath
        Check to see if player should be specced.
    */
    public void handleEvent(PlayerDeath event)
    {
        short playerID = event.getKilleeID();
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
            playerData.put(event.getKillerID(), kdata);
        }
        else
        {
            deaths = player.getLosses();
        }

        SpecTask specTask = getBestSpecTask(freq, ship, playerID);

        if(specTask != null) {
            if(specTask.deaths <= deaths) {
                specPlayer(player, data);
            } else if( notifyLives ) {
                c_Rec(player.getPlayerID());
            }
        }
    }

    /**
        Retrieves the highest priority SpecTask for a given player's info.

        @param freq is the freq of the player.
        @param ship is the ship of the player.
        @param playerID is the ID of the player.
        @return The appropriate spec task is returned.
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
        Specs a player and announces it to the arena.

        @param player is the player to be specced.
        @param data the won/loss record of the player if using internal tracking
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
        This method clears everything before the module unloads.
    */
    public void cancel()
    {
        specTasks.clear();
        playerData.clear();
    }

    /**
        Storage for basic kill/death records used in internal tracking mode.

        @author D1st0rt
        @version 06.07.08
    */
    private class PlayerData
    {
        /** The recorded number of kills. */
        private short kills;
        /** The recorded number of deaths. */
        private short deaths;

        /**
            Creates a new instance of PlayerData, kills and deaths start at 0.
        */
        PlayerData()
        {
            kills = 0;
            deaths = 0;
        }

        /**
            Increases the player's kill count.
        */
        void addKill()
        {
            kills++;
        }

        /**
            Increases the player's death count.
        */
        void addDeath()
        {
            deaths++;
        }

        /**
            Gets the player's kill count.
            @return the number of kills recorded for this player.
        */
        short getKills()
        {
            return kills;
        }

        /**
            Gets the player's death cound.
            @return the number of deaths recorded for this player.
        */
        short getDeaths()
        {
            return deaths;
        }
    }
}