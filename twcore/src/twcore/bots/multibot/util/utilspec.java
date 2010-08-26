/**
 * Spec module for TWBot.
 *
 * This module allows the user 4 methods of speccing:
 * 1) speccing everyone
 * 2) speccing specific freqs
 * 3) speccing specific ships
 * 4) speccing specific people
 *
 * The syntax for the spec module is as follows:
 *
 * You are allowed to add multiple spec tasks to the bot.  It will keep track of
 * player deaths and spec players if they get too many losses.  If there are
 * multiple ways a player could be specced, the following order of precedance
 * will be used:
 *
 * 1) !SpecPlayer
 * 2) !SpecShip
 * 3) !SpecFreq
 * 4) !Spec
 *
 * Where 1 is the highest precedance and 4 is the lowest.  For example:
 * !Spec 5 will spec him at 5 deaths, however if you do !SpecFreq 0 10, all
 * players on freq 0 will be specced at 10 deaths because !SpecFreq is ranked
 * higher than !Spec.
 *
 * Another new Feature is the ability to !AddDeath <PlayerName>.  If the player
 * is being currently watched for deaths, this command will give a them one
 * extra life.  !AddDeath does the following:
 *
 * !Spec 10 - specs everyone at 10 deaths.
 * !AddDeath <PlayerName> - Will add 1 death to PlayerName by making a new Spec
 * Task of the player at 11 deaths.
 *
 * Here is the help menu:
 * !Spec <Deaths>                                 -- Specs everyone at <Deaths> deaths.",
 * !SpecFreq <Freq>:<Deaths>                      -- Specs freq <Freq> at <Deaths> deaths.",
 * !SpecShip <Ship>:<Deaths>                      -- Specs ship <Ship> at <Deaths> deaths.",
 * !SpecPlayer <Player>:<Deaths>                  -- Specs player <Player> at <Deaths> deaths.",
 * !AddDeath <Player>                             -- Specs player <Player> at one more death.",
 * !SpecShared <Deaths>                           -- Specs everyone on a freq at COMBINED <Deaths>.",
 * !SpecList                                      -- Shows a list of spec tasks.",
 * !SpecDel <Task Number>                         -- Removes spec task number <Task Number>.",
 * !SaveFreq                                      -- Specs player to a freq he played on [on/off]",
 * !SpecOff                                       -- Stops watching deaths."
 *
 * NOTE: The !spec command is removed from the standard module so please !load
 * spec first before trying to do !spec :P
 *
 * Author: Cpt.Guano!
 * June 18, 2003
 *
 * Updated:
 * August 26, 2010 - Added a !SaveFreq command.
 * July 06, 2003 - Changed the spec message.
 */

package twcore.bots.multibot.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.TimerTask;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.EventRequester;

public class utilspec extends MultiUtil
{
    public static final String COLON = ":";
    private boolean isSaveFreqOn = false;
    
    private Vector <SpecTask>specTasks;
    
    /**
     * Initializes the spec module.
     */
    public void init() {
        specTasks = new Vector<SpecTask>();
    }

    public void requestEvents( ModuleEventRequester modEventReq ) {
    	modEventReq.request(this, EventRequester.PLAYER_DEATH);
    }

    /**
     * This method handles message events.
     *
     * @param event is the message event.
     */
    public void handleEvent(Message event)
    {
        int senderID = event.getPlayerID();
        String sender = m_botAction.getPlayerName(senderID);
        String command = event.getMessage().toLowerCase().trim();

        if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(sender))
            handleCommand(sender, command);
    }

    /**
     * This method handles all of the commands sent to the bot.
     *
     * @sender is the person that messaged the bot.
     * @command is the command string.
     */
    public void handleCommand(String sender, String command)
    {
        try
        {
            if(command.startsWith("!spec "))
                doSpecCmd(sender, command.substring(6).trim());
            if(command.startsWith("!specfreq "))
                doSpecFreqCmd(sender, command.substring(10).trim());
            if(command.startsWith("!specship "))
                doSpecShipCmd(sender, command.substring(10).trim());
            if(command.startsWith("!specplayer "))
                doSpecPlayerCmd(sender, command.substring(12).trim());
            if(command.startsWith("!specnotsafe"))
            	doSpecNotSafe(sender);
            if(command.startsWith("!specshared "))
                doSpecSharedCmd(sender, command.substring(12).trim());
            if(command.startsWith("!adddeath "))
                doAddDeathCmd(sender, command.substring(10).trim());
            if(command.equalsIgnoreCase("!speclist"))
                doSpecListCmd(sender);
            if(command.startsWith("!specdel "))
                doSpecDelCmd(sender, command.substring(9).trim());
            if(command.equalsIgnoreCase("!savefreq"))
                doSaveFreqCmd(sender);
            if(command.equalsIgnoreCase("!specoff"))
                doSpecOffCmd(sender);
            if(command.equalsIgnoreCase("!spec"))
                doSpecOffCmd(sender);
        }
        catch(RuntimeException e)
        {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }

    /**
     * This handles the player death event and speccs if needed.
     *
     * @param event is the player death event.
     */
    public void handleEvent(PlayerDeath event)
    {
        int playerID = event.getKilleeID();
        Player player = m_botAction.getPlayer(playerID);
        int freq = player.getFrequency();
        int ship = player.getShipType();
        int deaths = player.getLosses();
        SpecTask specTask = getValidSpecTask(freq, ship, playerID);

        if(specTask != null) {
            if( specTask.getSpecType() == SpecTask.SPEC_SHARED ) {
                Iterator<Player> i = m_botAction.getFreqPlayerIterator( freq );
                int combineddeaths = 0;
                int combinedkills = 0;
                while( i.hasNext() ) {
                    Player p = (Player)i.next();
                    if( p != null && p.getShipType() != 0 ) {
                        combinedkills += p.getWins();
                        combineddeaths += p.getLosses();
                    }
                }
                if( specTask.getDeaths() <= combineddeaths ) {
                    m_botAction.sendArenaMessage( "[ Freq " + freq + " is out!  " + combinedkills + " wins, " + combineddeaths + " losses. ]");
                    i = null;
                    i = m_botAction.getFreqPlayerIterator( freq );
                    while( i.hasNext() ) {
                        Player p = (Player)i.next();
                        if( p != null && p.getShipType() != 0 ) {
                            specPlayer(p);
                        }
                    }
                }
            } else
                if( specTask.getDeaths() <= deaths )
                    specPlayer(player);
        }
    }

    /**
     * This private method breaks up a string into tokens using the : delimiter if
     * it is present.  If not, it uses whitespace.
     *
     * @param argString is the string to split up.
     * @return A StringTokenizer of the arguments is returned.
     */
    private StringTokenizer getArgTokens(String argString) {
        if(argString.indexOf(COLON) != -1)
            return new StringTokenizer(argString, COLON);
        return new StringTokenizer(argString);
    }

    /**
     * This private method adds a spec task to the task list.  It will spec any
     * players that are over the specified number of deaths.  In addition, it will
     * replace any spec tasks that it might conflict with.
     *
     * @param specTask is the Spec Task to be added.
     */
    private void addTask(SpecTask specTask) {
        int replaceIndex = specTasks.indexOf(specTask);

        if(replaceIndex != -1)
            specTasks.remove(replaceIndex);
        specTasks.add(specTask);
        Collections.sort(specTasks, new SpecTaskComparator());
        updateSpec();
    }

    /**
     * This private method is called when a new spec task is added.  It specs
     * any players that are currently over the loss limit.
     */
    private void updateSpec() {
        Iterator<Player> iterator = m_botAction.getPlayingPlayerIterator();
        Player player;
        int playerID;
        int freq;
        int ship;
        int deaths;
        SpecTask specTask;

        while(iterator.hasNext())
        {
            player = (Player) iterator.next();
            playerID = player.getPlayerID();
            freq = player.getFrequency();
            ship = player.getShipType();
            deaths = player.getLosses();
            specTask = getValidSpecTask(freq, ship, playerID);

            if(specTask != null && specTask.getDeaths() <= deaths)
                specPlayer(player);
        }
    }

    /**
     * This method specs all players at a certain number of deaths.  The syntax is
     * as follows: !SpecAll <Deaths> where <Deaths> is the number of deaths to spec
     * at.
     *
     * @param sender is the person operating the bot.
     * @param argString is the string of the arguments.
     */
    public void doSpecCmd(String sender, String argString)
    {
        StringTokenizer argTokens = getArgTokens(argString);

        if(argTokens.countTokens() != 1)
            throw new IllegalArgumentException("Please use the following format: !Spec <Deaths>.");
        try
        {
            int deaths = Integer.parseInt(argTokens.nextToken());
            SpecTask specTask = new SpecTask(deaths);

            addTask(specTask);
            m_botAction.sendArenaMessage(specTask.toString() + " -" + sender);
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !Spec <Deaths>.");
        }
    }

    /**
     * This method specs players on a certain freq at a certain number of deaths.
     * The syntax is as follows: !SpecFreq <Freq>:<Deaths> where <Freq> is the
     * freq to monitor and <Deaths> is the number of deaths to spec at.
     *
     * @param sender is the person operating the bot.
     * @param argString is the string of the arguments.
     */
    public void doSpecFreqCmd(String sender, String argString)
    {
        StringTokenizer argTokens = getArgTokens(argString);

        if(argTokens.countTokens() != 2)
            throw new IllegalArgumentException("Please use the following format: !SpecFreq <Freq>:<Deaths>.");
        try
        {
            int freq = Integer.parseInt(argTokens.nextToken());;
            int deaths = Integer.parseInt(argTokens.nextToken());;
            SpecTask specTask = new SpecTask(freq, deaths, SpecTask.SPEC_FREQ);

            addTask(specTask);
            m_botAction.sendArenaMessage(specTask.toString() + " -" + sender);
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !SpecFreq <Freq>:<Deaths>.");
        }
    }

    /**
     * This method specs players of a certain ship at a certain number of deaths.
     * The syntax is as follows: !SpecShip <Ship>:<Deaths> where <Ship> is the
     * ship to monitor and <Deaths> is the number of deaths to spec at.
     *
     * @param sender is the person operating the bot.
     * @param argString is the string of the arguments.
     */
    public void doSpecShipCmd(String sender, String argString)
    {
        StringTokenizer argTokens = getArgTokens(argString);

        if(argTokens.countTokens() != 2)
            throw new IllegalArgumentException("Please use the following format: !SpecShip <Ship>:<Deaths>.");
        try
        {
            int ship = Integer.parseInt(argTokens.nextToken());
            int deaths = Integer.parseInt(argTokens.nextToken());
            SpecTask specTask = new SpecTask(ship, deaths, SpecTask.SPEC_SHIP);

            addTask(specTask);
            m_botAction.sendArenaMessage(specTask.toString() + " -" + sender);
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !SpecShip <Ship>:<Deaths>.");
        }
    }

    /**
     * This method specs players of a certain player at a certain number of
     * deaths.  The syntax is as follows: !SpecPlayer <Player>:<Deaths> where
     * <Player> is the player to monitor and <Deaths> is the number of deaths to
     * spec at.
     *
     * @param sender is the person operating the bot.
     * @param argString is the string of the arguments.
     */
    public void doSpecPlayerCmd(String sender, String argString)
    {
        StringTokenizer argTokens = getArgTokens(argString);
        
        if(argTokens.countTokens() != 2)
            throw new IllegalArgumentException("Please use the following format: !SpecPlayer <Player>:<Deaths>.");
        try
        {
            String playerName = m_botAction.getFuzzyPlayerName(argTokens.nextToken());
            int playerID = m_botAction.getPlayerID(playerName);
            int deaths = Integer.parseInt(argTokens.nextToken());
            SpecTask specTask = new SpecTask(playerID, deaths, SpecTask.SPEC_PLAYER);

            if(playerName == null)
                throw new IllegalArgumentException("Player not found in the arena.");
//          check to see if the player is in spec...
            addTask(specTask);
            m_botAction.sendArenaMessage(specTask.toString() + " -" + sender);
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !SpecPlayer <Player>:<Deaths>.");
        }
    }

    public void doSpecNotSafe (String sender)	{
    	// Cycle each player:
    	// - Make the bot spectate each individual player for a small amount of time 
    	//   to determine if the player is in safe or not
    	// - bot specs the player if he's not in safe
    	// - continue on to the next player until all players are done
    	
    	m_botAction.sendPrivateMessage(sender, "Spectating players who are not in safe...");
    	
    	Iterator<Player> players = m_botAction.getPlayingPlayerIterator();
    	while(players.hasNext()) {
    		Player player = players.next();
    		if(player == null) continue;
    		
    		m_botAction.spectatePlayer(player.getPlayerID());
    		
    		SpecNotSafe specNotSafe = new SpecNotSafe();
    		specNotSafe.setPlayer(player);
    		specNotSafe.setThread(Thread.currentThread());
    		m_botAction.scheduleTask(specNotSafe, 500);
    		
        	// Wait for the specNotSafe TimerTask to complete.
        	try {
        		Thread.sleep(1000); // 1 seconds maximum
        		specNotSafe.cancel(); // In case we hit the timeout, cancel the timertask
        	} catch(InterruptedException e) {}
    		
    	}
    	
    	m_botAction.sendPrivateMessage(sender, "Done spectating players who are not in safe.");
    	
    }

    /**
     * This method specs a freq when a certain number of COMBINED deaths is reached.
     * The syntax is as follows: !SpecShared <Deaths> where <Deaths> is the number of
     * combined deaths to spec at.  For example, if this number was 30, and there were
     * 3 people on a freq, one with 3 deaths, one with 7 deaths, and one with 19, the
     * next time any player died, the 30 death limit would be reached and the entire
     * freq would be sent to spec.
     *
     * @param sender is the person operating the bot.
     * @param argString is the string of the arguments.
     */
    public void doSpecSharedCmd(String sender, String argString)
    {
        StringTokenizer argTokens = getArgTokens(argString);

        if(argTokens.countTokens() != 1)
            throw new IllegalArgumentException("Please use the following format: !SpecShared <Deaths>.");
        try
        {
            int deaths = Integer.parseInt(argTokens.nextToken());
            SpecTask specTask = new SpecTask( -1, deaths, SpecTask.SPEC_SHARED);
            addTask(specTask);
            m_botAction.sendArenaMessage(specTask.toString() + " -" + sender);
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !Spec <Deaths>.");
        }
    }

    /**
     * This method makes a player spec one death later.  If the player is not
     * registered with the lagout handler, the command will not execute.  After
     * successful execution of this method, a new SpecTask will be present for the
     * individual player.
     *
     * @param sender is the person that messaged the bot.
     * @param argString is the name of the player.
     */
    public void doAddDeathCmd(String sender, String argString)
    {
        String playerName = m_botAction.getFuzzyPlayerName(argString);
        if(playerName == null)
            throw new IllegalArgumentException("Player not found.");
        Player player = m_botAction.getPlayer(playerName);
        int freq = player.getFrequency();
        int ship = player.getShipType();
        int playerID = player.getPlayerID();

        SpecTask specTask = getValidSpecTask(freq, ship, playerID);
        if(specTask == null)
            throw new IllegalArgumentException("Player not currently being watched.");
        int deaths = specTask.getDeaths() + 1;

        SpecTask newSpecTask = new SpecTask(playerID, deaths, SpecTask.SPEC_PLAYER);
        addTask(newSpecTask);
        m_botAction.sendArenaMessage(playerName + " granted one extra life.  Now being specced at " + deaths + " deaths.");
    }

    /**
     * This method displays a list of all of the spec tasks.
     *
     * @param sender is the person that messaged the bot.
     */
    public void doSpecListCmd(String sender)
    {
        int numTasks = specTasks.size();
        SpecTask specTask;

        if(numTasks == 0)
            m_botAction.sendSmartPrivateMessage(sender, "Bot not monitoring any deaths.");
        else
            for(int index = 0; index < numTasks; index++)
            {
                specTask = specTasks.get(index);
                m_botAction.sendSmartPrivateMessage(sender, "Task " + index + ") " + specTask);
            }
    }

    /**
     * This method removes a spec task.
     *
     * @param sender is the person that messaged the bot.
     * @param argString is the spec task number that is to be removed.
     */
    public void doSpecDelCmd(String sender, String argString)
    {
        StringTokenizer argTokens = getArgTokens(argString);

        if(argTokens.countTokens() != 1)
            throw new IllegalArgumentException("Please use the following format: !SpecDel <Task Number>.");
        try
        {
            int taskNumber = Integer.parseInt(argTokens.nextToken());
            SpecTask specTask = specTasks.get(taskNumber);

            specTasks.remove(taskNumber);
            m_botAction.sendArenaMessage("Removing Task: " + specTask.toString() + " -" + sender);
        }
        catch(NumberFormatException e)
        {
            throw new NumberFormatException("Please use the following format: !SpecDel <Task Number>.");
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            throw new IllegalArgumentException("Invalid Spec Task.");
        }
    }
    
    /**
     * This method determines whether players should be put in their frequency when spec'd.
     * @param sender
     */
    public void doSaveFreqCmd(String sender)
    {
        isSaveFreqOn = isSaveFreqOn ? false : true;
        String status = isSaveFreqOn ? "enabled" : "disabled";
        m_botAction.sendSmartPrivateMessage(sender, "Save frequency is " + status + ".");
    }
    
    /**
     * This method clears all of the spec tasks.
     */
    public void doSpecOffCmd(String sender)
    {
        if(specTasks.isEmpty())
            throw new RuntimeException("No spec tasks to clear.");
        specTasks.clear();
        m_botAction.sendArenaMessage("Removing all spec tasks. -" + sender);
    }

    /**
     * This private method gets the first spec task that affects a player with the
     * given attributes.
     *
     * @param freq is the freq of the player.
     * @param ship is the ship of the player.
     * @param playerID is the ID of the player.
     * @return The appropriate spec task is returned.
     */
    private SpecTask getValidSpecTask(int freq, int ship, int playerID)
    {
        SpecTask specTask;

        for(int index = 0; index < specTasks.size(); index++)
        {
            specTask = specTasks.get(index);
            if(specTask.isSameType(freq, ship, playerID))
                return specTask;
        }
        return null;
    }

    /**
     * This private method specs a player and arenas it.
     *
     * @param player is the player to be specced.
     */
    private void specPlayer(Player player)
    {
        String playerName = player.getPlayerName();
        int wins = player.getWins();
        int losses = player.getLosses();
        int freq = player.getFrequency();

        m_botAction.sendArenaMessage(playerName + " is out.  " + wins + " wins, " + losses + " losses.");
        m_botAction.spec(playerName);
        m_botAction.spec(playerName);
        
        if(isSaveFreqOn)
            m_botAction.setFreq(playerName, freq);
    }

    /**
     * This static method is used by other modules (lagout) to capture the
     * name of the person that got specced.  The string s is the arena message
     * that the spec module sends upon speccing someone.  I know this is coupling
     * at its worst but if someone has a better idea im all ears :P
     *
     * @param s is the arena message to check.
     * @return the player name is returned if the string was valid.  If not, null
     * is returned.
     */
    public static String getSpeccedPlayerName(String s)
    {
        int index = s.indexOf(" is out.  ");
        if(index == -1)
            return null;
        return s.substring(0, index);
    }

    /**
     * Displays a help message.
     */
    public String[] getHelpMessages() {
        String[] message =
        {
                "!Spec <Deaths>                     -- Specs everyone at <Deaths> deaths.",
                "!SpecFreq <Freq>:<Deaths>          -- Specs freq <Freq> at <Deaths> deaths.",
                "!SpecShip <Ship>:<Deaths>          -- Specs ship <Ship> at <Deaths> deaths.",
                "!SpecPlayer <Player>:<Deaths>      -- Specs player <Player> at <Deaths> deaths.",
                "!SpecNotSafe                       -- Specs all players not in safe.",
                "!SpecShared <Deaths>               -- Specs everyone on a freq at COMBINED <Deaths>.",
                "!AddDeath <Player>                 -- Specs player <Player> at one more death.",
                "!SpecList                          -- Shows list of all spec tasks you've entered.",
                "!SpecDel <Task Number>             -- Removes spec task number <Task Number>.",
                "!SaveFreq                          -- Specs player to a freq he played on [on/off]",
                "!SpecOff                           -- Stops watching deaths."
        };
        return message;
    }

    /**
     * This method cancels all of the spec tasks.
     */
    public void cancel()
    {
        specTasks.clear();
    }

    /**
     * This private class is a spec task.  It holds the type of spec task, the
     * number of deaths and who it affects.
     */
    private class SpecTask
    {
        public static final int SPEC_ALL = 0;
        public static final int SPEC_FREQ = 1;
        public static final int SPEC_SHIP = 2;
        public static final int SPEC_PLAYER = 3;
        public static final int SPEC_SHARED = 4;

        public static final int MAX_FREQ = 9999;
        public static final int MIN_FREQ = 0;
        public static final int MAX_SHIP = 8;
        public static final int MIN_SHIP = 1;

        private int specID;
        private int deaths;
        private int specType;

        /**
         * This constructor initializes a spec task of type SPEC_ALL at with a
         * given number of deaths.
         *
         * @param deaths is the number of deaths to spec at.
         */
        public SpecTask(int deaths)
        {
            if(deaths <= 0)
                throw new IllegalArgumentException("Invalid number of deaths to spec at.");
            this.deaths = deaths;
            this.specType = SPEC_ALL;
        }

        /**
         * This constructor initializes a spec task of variable type with the given
         * number of deaths.
         *
         * @param specID who is affected by the spec task.  This could be the freq
         * ID, the ship type or the playerID.
         * @param deaths is the number of deaths to spec at.
         * @param specType this is the type of task and defines who the task
         * affects.
         */
        public SpecTask(int specID, int deaths, int specType)
        {
            if(specType < 0 || specType > 4)
                throw new IllegalArgumentException("ERROR: Unknown Spec Type.");
            if((specID < MIN_FREQ || specID > MAX_FREQ) && specType == SPEC_FREQ)
                throw new IllegalArgumentException("Invalid freq number.");
            if((specID < MIN_SHIP || specID > MAX_SHIP) && specType == SPEC_SHIP)
                throw new IllegalArgumentException("Invalid ship type.");
            if(deaths <= 0)
                throw new IllegalArgumentException("Invalid number of deaths to spec at.");

            this.specID = specID;
            this.deaths = deaths;
            this.specType = specType;
        }

        /**
         * This method gets the spec type.
         *
         * @return the spec type is returned.
         */
        public int getSpecType()
        {
            return specType;
        }

        /**
         * This method gets the number of deaths to spec at.
         *
         * @return the number of deaths to spec at is returned.
         */
        public int getDeaths()
        {
            return deaths;
        }

        /**
         * This method checks to see if a player with the given attributes will
         * be specced by this particular spec task.
         *
         * @param freq is the freq of the player.
         * @param ship is the ship of the player.
         * @playerID is the ID of the player.
         * @return True if the player is affected, false if not.
         */
        public boolean isSameType(int freq, int ship, int playerID)
        {
            switch(specType)
            {
            case SPEC_ALL:
                return true;
            case SPEC_FREQ:
                return freq == specID;
            case SPEC_SHIP:
                return ship == specID;
            case SPEC_PLAYER:
                return playerID == specID;
            case SPEC_SHARED:
                return true;  // Logic is handled in PlayerDeath
            }
            return false;
        }

        /**
         * This method returns a string representation of the spec task.
         *
         * @return a string representation of the spec task is returned.
         */
        public String toString()
        {
            String specTypeName;
            String deathString = "death";

            switch(specType)
            {
            case SPEC_ALL:
                specTypeName = "all players";
                break;
            case SPEC_FREQ:
                specTypeName = "freq " + specID;
                break;
            case SPEC_SHIP:
                specTypeName = "ship " + specID;
                break;
            case SPEC_PLAYER:
                specTypeName = "player " + m_botAction.getPlayerName(specID);
                break;
            case SPEC_SHARED:
                specTypeName = "entire frequency (shared lives)";
                break;
            default:
                specTypeName = "ERROR: Unknown Spec Type";
            }
            if(deaths != 1)
                deathString = deathString + "s";
            return "Removing " + specTypeName + " with " + deaths + " " + deathString + ".";
        }

        /**
         * This overrides the equals method so as to check the spec types and the
         * spec IDs.
         *
         * @param obj is the other spec task to check.
         * @return True if the spec tasks have the same specID and the same
         * specType.  False if not.
         */
        public boolean equals(Object obj)
        {
            SpecTask specTask = (SpecTask) obj;
            return specTask.specID == specID && specTask.specType == specType;
        }
    }

    /**
     * This private class compares two spec tasks and orders them in the following
     * manner:
     * From highest priority to lowest: SPEC_PLAYER, SPEC_SHIP, SPEC_FREQ,
     * SPEC_ALL.  In each subcategory they are ordered in order of descending
     * deaths.
     */
    private class SpecTaskComparator implements Comparator<SpecTask>
    {

        /**
         * This method provides a compare function for two specTasks.  This is used
         * for ordering the tasks in the list.
         *
         * @param obj1 is the first spec task to compare.
         * @param obj2 is the second spec task to compare.
         * @return a numerical representation of the difference of the two spec
         * tasks.
         */
        public int compare(SpecTask task1, SpecTask task2)
        {
            int value1 = task1.getSpecType() * SpecTask.MAX_FREQ + task1.specID;
            int value2 = task2.getSpecType() * SpecTask.MAX_FREQ + task2.specID;

            return value2 - value1;
        }
    }
    
    private class SpecNotSafe extends TimerTask {
    	private Thread multibot;
		private Player player;
		
		public void run()	{
			Iterator<Player> players = m_botAction.getPlayingPlayerIterator();
	    	while(players.hasNext())	{
	    		Player p = (Player)players.next();
	    		if(p != null && p.getPlayerID() == player.getPlayerID() && p.isPlaying() && !p.isInSafe()) {
	    			m_botAction.specWithoutLock(p.getPlayerID());
	    			break;
	    		} else if(p != null && p.getPlayerID() == player.getPlayerID()) {
	    			break;
	    		}
	    	}
	    	multibot.interrupt();
		}
		
		public void setThread(Thread t) {
			this.multibot = t;
		}
		
		public void setPlayer(Player p) {
			this.player = p;
		}
    	
    }
}