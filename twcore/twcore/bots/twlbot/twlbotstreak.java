package twcore.bots.twlbot;

import java.util.*;
import twcore.core.*;

/**
 * Streaks can be added by the host in matches
 * 
 * @author - Someone
 * 		modified from twbotStreak.java
 * 		edited for twl by FoN
 * @version 2.0
 */
public class twlbotstreak extends TWLBotExtension
{

    HashMap playerMap;
    boolean running = false;
    int streak = 3, reStreak = 3;
    String sMessages[] = { 	" On Fire!",
            				" Killing Spree!",
            				" Rampage!",
            				" Dominating!",
            				" Too Sexy",
            				" Unstoppable!",
            				" God Like!",
            				" Cheater!" };

    public twlbotstreak()
    {
        playerMap = new HashMap();
    }

    /**
     * Handles the command
     * @param name The host
     * @param message Command
     */
    public void handleCommand(String name, String message)
    {
        if (message.toLowerCase().startsWith("!streakoff"))
        {
            stopStreak(name);
        }
        else if (message.toLowerCase().startsWith("!streak"))
        {
            String pieces[] = message.split(":");
            try
            {
                int i = Integer.parseInt(pieces[1]);
                if (i < 0)
                    i = 1;
                if (i > 100)
                    i = 100;
                int j = Integer.parseInt(pieces[2]);
                if (j < 0)
                    j = 1;
                if (j > 100)
                    j = 100;
                startStreak(name, i, j);
            }
            catch (Exception e)
            {
                m_botAction
                        .sendPrivateMessage(name, "Remember: !streak <start>:<update> (Btw failed to start streak.)");
            }
        }
    }

    public void startStreak(String name, int i, int j)
    {
        if (!running)
        {
            running = true;
            streak = i;
            reStreak = j;
            m_botAction.sendPrivateMessage(name, "Watching for those streaks! Starting streaks at " + i
                    + " and updating at " + j);
        }
        else
            m_botAction.sendPrivateMessage(name, "Already monitoring arena for streaks! $$$");
    }

    public void stopStreak(String name)
    {
        if (running)
        {
            streak = 3;
            reStreak = 3;
            running = false;
            playerMap.clear();
            m_botAction.sendPrivateMessage(name, "I was getting bored of watching them kill each other anyway.");
        }
        else
            m_botAction.sendPrivateMessage(name, "I wasn't looking for streaks, was I suppost to?");
    }

    /**
     * Checks the players streak record and gives out a message
     * @param name Name of player on whom the streak is checked
     * @param kills Number of kills the player has streaked
     */
    public void checkStreak(String name, int kills)
    {
        for (int i = 0; i < sMessages.length; i++)
        {
            int count = streak + i * reStreak;
            if (kills == count)
                m_botAction.sendArenaMessage("Streak!: " + name + " (" + kills + ":0)" + sMessages[i]);
        }
    }

    /**
     * Handle streaks
     */
    public void handleEvent(PlayerDeath event)
    {
        if (running)
        {
            Player theKiller = m_botAction.getPlayer(event.getKillerID());
            Player theKillee = m_botAction.getPlayer(event.getKilleeID());
            String killer = theKiller.getPlayerName();
            String killee = theKillee.getPlayerName();
            if (!playerMap.containsKey(killer))
                playerMap.put(killer, "0");
            if (playerMap.containsKey(killer))
            {
                String ct = (String) playerMap.get(killer);
                int it = Integer.parseInt(ct);
                it++;
                playerMap.remove(killer);
                playerMap.put(killer, "" + it);
                checkStreak(killer, it);
            }
            if (playerMap.containsKey(killee))
                playerMap.remove(killee);

        }
    }

    /**
     * Parse messages for toggling streaks
     */
    public void handleEvent(Message event)
    {
        String message = event.getMessage();
        if (event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if (m_opList.isER(name))
                handleCommand(name, message);
        }
    }

    /**
     * Help messages for the streak Module
     */
    public String[] getHelpMessages()
    {
        String[] messages = {
                "!streak <start>:<update>  - Watches for streaks beginning at <start> and updated every <update>",
                "!streakoff                - Turns off the streak watcher." };
        return messages;
    }

    /**
     * Clean up method
     */
    public void cancel()
    {
        playerMap.clear();
    }

}