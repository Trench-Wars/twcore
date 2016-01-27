package twcore.bots.multibot.bountyhunter;

import static twcore.core.EventRequester.PLAYER_DEATH;
import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;

/**
    Bounty Hunter module

    Built by: Jacen Solo
*/
public class bountyhunter extends MultiModule
{
    boolean isRunning = false;
    int bounty = 50;

    public void init()
    {
    }

    public void requestEvents(ModuleEventRequester events)
    {
        events.request(this, PLAYER_DEATH);
    }

    public void handleEvent(Message event)
    {
        String message = event.getMessage();

        if(event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            String name = m_botAction.getPlayerName(event.getPlayerID());

            if(opList.isER(name))
                handleCommand(name, message);
        }
    }

    public void handleEvent(PlayerDeath event)
    {
        if(!isRunning) return;

        Player killer = m_botAction.getPlayer(event.getKillerID());
        Player killee = m_botAction.getPlayer(event.getKilleeID());
        int killerBounty = killer.getBounty();
        int killeeBounty = killee.getBounty();

        if(killerBounty >= bounty && killeeBounty < bounty) {
            m_botAction.sendUnfilteredPrivateMessage(killee.getPlayerName(), "*spec");
            m_botAction.sendUnfilteredPrivateMessage(killee.getPlayerName(), "*spec");
            m_botAction.sendArenaMessage(killee.getPlayerName() + " has been hunted by " + killer.getPlayerName());
        }
    }

    public void handleCommand(String name, String message)
    {
        if(message.toLowerCase().startsWith("!start") && !isRunning)
            handleStart(name, message);
        else if(message.toLowerCase().startsWith("!stop") && isRunning)
            handleStop(name);
    }

    public void handleStart(String name, String message) {
        String pieces[] = message.split(" ", 2);

        if(pieces.length == 2) {
            try {
                bounty = Integer.parseInt(pieces[1]);
            } catch(Exception e) {}
        }

        m_botAction.sendArenaMessage("Bounty Hunter mode enabled, bounty required: " + bounty, 2);
        isRunning = true;
    }

    public void handleStop(String name) {
        m_botAction.sendArenaMessage("Bounty Hunter mode disabled.", 13);
        isRunning = false;
    }

    public String[] getModHelpMessage()
    {
        String[] help = {
            "!start <#>                     - Starts bounty hunter mode",
            "!stop                          - Stops bounty hunter mode"
        };
        return help;
    }

    public boolean isUnloadable()
    {
        return true;
    }

    public void cancel()
    {
    }
}