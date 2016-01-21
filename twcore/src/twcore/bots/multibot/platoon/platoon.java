package twcore.bots.multibot.platoon;

import static twcore.core.EventRequester.FREQUENCY_SHIP_CHANGE;
import static twcore.core.EventRequester.PLAYER_DEATH;
import static twcore.core.EventRequester.PLAYER_LEFT;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;

/**
    Module for the platoon type games SuperDAVE(postal) created.
    basically an inverted prodem type game where you want to stay alive,
    every time you die you get put down 1 ship until you get to ship 1.
    If you die as ship 1, you're out.

    Created on 27/02/2005

    @author - Jacen Solo
    @version 1.0
*/
public class platoon extends MultiModule
{
    HashSet <Integer>exemptShips = new HashSet<Integer>();
    boolean isRunning = false;
    boolean notReallyRunning = false;

    public void init() {}

    public void requestEvents(ModuleEventRequester events) {
        events.request(this, PLAYER_DEATH);
        events.request(this, PLAYER_LEFT);
        events.request(this, FREQUENCY_SHIP_CHANGE);
    }

    public void handleEvent(Message event)
    {
        String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());

        if (name == null) return;


        String message = event.getMessage();

        if(opList.isER(name))
            handleCommand(name, message);
    }

    public void handleCommand(String name, String message)
    {
        if(message.toLowerCase().startsWith("!start ") && !isRunning)
        {
            updateExempt(name, message);
            startGame(name);
        }
        else if(message.toLowerCase().startsWith("!start") && !isRunning)
            startGame(name);
        else if(message.toLowerCase().startsWith("!cancel") && isRunning)
            cancelGame(name);
        else if(message.toLowerCase().startsWith("!exempt "))
            updateExempt(name, message);
        else if(message.toLowerCase().startsWith("!demote") && !isRunning && !notReallyRunning)
        {
            isRunning = true;
            notReallyRunning = true;
            m_botAction.sendPrivateMessage(name, "Demoting now.");
            m_botAction.sendArenaMessage("Demoting - on");
        }
        else if(message.toLowerCase().startsWith("!stop") && isRunning && notReallyRunning)
        {
            isRunning = false;
            notReallyRunning = false;
            m_botAction.sendPrivateMessage(name, "No longer demoting.");
            m_botAction.sendArenaMessage("Demoting - off");
        }
    }

    public void updateExempt(String name, String message)
    {
        exemptShips = new HashSet<Integer>();

        String pieces[] = message.split(" ", 2);
        String pieces2[] = pieces[1].split(":");

        for(int k = 0; k < pieces2.length; k++)
        {
            try {
                if(Integer.parseInt(pieces2[k]) < 9 && Integer.parseInt(pieces2[k]) > 0)
                    exemptShips.add(new Integer(Integer.parseInt(pieces2[k])));
            } catch(Exception e) {}
        }

        String exemptShipList = "Exempt ships are: ";
        Iterator<Integer> it = exemptShips.iterator();

        while(it.hasNext())
        {
            exemptShipList += " " + ((Integer)it.next()).intValue();
        }

        m_botAction.sendPrivateMessage(name, exemptShipList);
    }

    public void handleEvent(PlayerDeath event)
    {
        if(!isRunning) return;

        int ship = m_botAction.getPlayer(event.getKilleeID()).getShipType();

        if(!exemptShips.contains(new Integer(ship)))
        {
            ship--;

            while(exemptShips.contains(new Integer(ship)))
                ship--;

            if(ship == 0)
            {
                m_botAction.spec(event.getKilleeID());
                m_botAction.spec(event.getKilleeID());
                m_botAction.sendArenaMessage(m_botAction.getPlayerName(event.getKilleeID()) + " is out!");
                Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
                int players = 0;

                while(it.hasNext()) {
                    it.next();
                    players++;
                }

                if(players == 1 && !notReallyRunning)
                    gameOver();
            }
            else
            {
                final int kID = event.getKilleeID();
                final int ship2 = ship;
                TimerTask shipChange = new TimerTask()
                {
                    public void run()
                    {
                        m_botAction.setShip(kID, ship2);
                    }
                };
                m_botAction.scheduleTask(shipChange, 5  * 1000);
            }
        }
    }

    public void handleEvent(PlayerLeft event)
    {
        if(!isRunning)
            return;

        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        int players = 0;

        while(it.hasNext()) {
            it.next();
            players++;
        }

        if(players == 1 && !notReallyRunning)
            gameOver();
    }

    public void handleEvent(FrequencyShipChange event)
    {
        if(!isRunning)
            return;

        if(event.getShipType() == 0)
        {
            Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
            int players = 0;

            while(it.hasNext()) {
                it.next();
                players++;
            }

            if(players == 1 && !notReallyRunning)
                gameOver();
        }
    }

    public void cancelGame(String name)
    {
        m_botAction.sendArenaMessage("This game has been killed by: " + name, 13);
        isRunning = false;
    }

    public void gameOver()
    {
        Iterator<Player> it = m_botAction.getPlayingPlayerIterator();
        String winner = "";

        while(it.hasNext())
        {
            Player p = (Player)it.next();
            winner = p.getPlayerName();
        }

        m_botAction.sendArenaMessage(winner + " has won the game!", 104);
        isRunning = false;
    }

    public void startGame(String name)
    {
        m_botAction.sendArenaMessage(name + " has started platoon mode!");
        m_botAction.sendArenaMessage("Game begins in 10 seconds!", 2);

        TimerTask five = new TimerTask()
        {
            public void run()
            {
                m_botAction.sendArenaMessage("5");
            }
        };
        TimerTask three = new TimerTask()
        {
            public void run()
            {
                m_botAction.sendArenaMessage("3");
            }
        };
        TimerTask two = new TimerTask()
        {
            public void run()
            {
                m_botAction.sendArenaMessage("2");
            }
        };
        TimerTask one = new TimerTask()
        {
            public void run()
            {
                m_botAction.sendArenaMessage("1");
            }
        };
        TimerTask go = new TimerTask()
        {
            public void run()
            {
                m_botAction.sendArenaMessage("GOOO GO GO GO GOOOOO!", 104);
                isRunning = true;
            }
        };
        notReallyRunning = false;

        m_botAction.scheduleTask(five, 5000);
        m_botAction.scheduleTask(three, 7000);
        m_botAction.scheduleTask(two, 8000);
        m_botAction.scheduleTask(one, 9000);
        m_botAction.scheduleTask(go, 10000);
    }

    public void cancel() {}

    public boolean isUnloadable() {
        return true;
    }

    public String[] getModHelpMessage()
    {
        String helps[] = {
            "!start a:b:c:etc       -Starts a game with ships a, b, and c exempt from ship changes.",
            "!start                 -Starts a game with no ships exempt.",
            "!cancel                -Cancels current game.",
            "!exempt a:b:c:d:etc    -Changes ships exempt to a, b, c, and d. You can add as many as you want, they just need to be seperated by :'s",
            "!demote                -Demotes people even when game isn't going.",
            "!stop                  -Stops demoting people."
        };

        return helps;
    }
}