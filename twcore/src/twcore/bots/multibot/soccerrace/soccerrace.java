package twcore.bots.multibot.soccerrace;

import java.util.Iterator;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Player;

/*
    Soccer Race Module - race to score one goal.

    Created By: Jacen Solo

    Created: 05/24/04 at
*/
public class soccerrace extends MultiModule
{

    int ship = 4;
    Player p;

    public void init() { }

    public void requestEvents(ModuleEventRequester events)  {
        events.request(this, EventRequester.SOCCER_GOAL);
    }

    public void cancel()
    {
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

    public void handleEvent(SoccerGoal event)
    {
        int winfreq = event.getFrequency();
        m_botAction.sendArenaMessage("Congratulations to Freq " + winfreq + " - They have scored and won the game!", 5);
        m_botAction.toggleLocked();
    }

    public void handleCommand(String name, String message)
    {
        String pieces[] = message.split(" ");
        int Ship = 1;

        if(message.toLowerCase().startsWith("!start"))
        {
            try
            {
                Ship = Integer.parseInt(pieces[1]);
            }
            catch(Exception e) {
                return;
            }

            m_botAction.sendUnfilteredPublicMessage("*restart");
            ship = Ship;
            m_botAction.changeAllShips(ship);
            m_botAction.sendArenaMessage("Rules: Take the ball through your section of the course and pass the ball to your teammate at the end of your section", 2);
            m_botAction.sendArenaMessage("The first team to score wins, good luck and have fun!");

            Iterator<Player> it = m_botAction.getPlayingPlayerIterator();

            for(int i = 0; it.hasNext(); i++) {
                Player p = (Player)it.next();

                if(i < 4) freq0(p, (i % 4));
                else if(i < 8) freq1(p, (i % 4));
                else if(i < 12) freq2(p, (i % 4));
                else freq3(p, (i % 4));
            }

            m_botAction.sendArenaMessage("Gooooo go go go gooooooo!", 104);
        }

        if(message.toLowerCase().startsWith("!stop"))
        {
            m_botAction.sendArenaMessage("This race has been stopped by: " + name);
        }

        if(message.toLowerCase().startsWith("!ballplace"))
        {
            String pieces2[] = message.split(" ");

            try
            {
                if(Integer.parseInt(pieces2[1]) == 0)
                    m_botAction.warpTo(name, 453, 312);

                if(Integer.parseInt(pieces2[1]) == 1)
                    m_botAction.warpTo(name, 453, 512);

                if(Integer.parseInt(pieces2[1]) == 2)
                    m_botAction.warpTo(name, 453, 712);

                if(Integer.parseInt(pieces2[1]) == 3)
                    m_botAction.warpTo(name, 453, 912);
            }
            catch(Exception e) {
                m_botAction.sendPrivateMessage(name, "Wrong format for this method");
            }
        }

        if(message.toLowerCase().startsWith("!balls"))
        {
            m_botAction.warpTo(name, 512, 512);
        }
    }

    public void freq0(Player p, int k)
    {
        if(k == 0)
        {
            m_botAction.setFreq(p.getPlayerName(), 0);
            m_botAction.warpTo(p.getPlayerID(), 475, 312);
        }

        if(k == 1)
        {
            m_botAction.setFreq(p.getPlayerName(), 0);
            m_botAction.warpTo(p.getPlayerID(), 463, 245);
        }

        if(k == 2)
        {
            m_botAction.setFreq(p.getPlayerName(), 0);
            m_botAction.warpTo(p.getPlayerID(), 287, 206);
        }

        if(k == 3)
        {
            m_botAction.setFreq(p.getPlayerName(), 0);
            m_botAction.warpTo(p.getPlayerID(), 250, 264);
        }
    }

    public void freq1(Player p, int k)
    {
        if(k == 0)
        {
            m_botAction.setFreq(p.getPlayerName(), 1);
            m_botAction.warpTo(p.getPlayerID(), 475, 512);
        }

        if(k == 1)
        {
            m_botAction.setFreq(p.getPlayerName(), 1);
            m_botAction.warpTo(p.getPlayerID(), 463, 445);
        }

        if(k == 2)
        {
            m_botAction.setFreq(p.getPlayerName(), 1);
            m_botAction.warpTo(p.getPlayerID(), 287, 406);
        }

        if(k == 3)
        {
            m_botAction.setFreq(p.getPlayerName(), 1);
            m_botAction.warpTo(p.getPlayerID(), 250, 464);
        }
    }

    public void freq2(Player p, int k)
    {
        if(k == 0)
        {
            m_botAction.setFreq(p.getPlayerName(), 2);
            m_botAction.warpTo(p.getPlayerID(), 475, 712);
        }

        if(k == 1)
        {
            m_botAction.setFreq(p.getPlayerName(), 2);
            m_botAction.warpTo(p.getPlayerID(), 463, 645);
        }

        if(k == 2)
        {
            m_botAction.setFreq(p.getPlayerName(), 2);
            m_botAction.warpTo(p.getPlayerID(), 287, 606);
        }

        if(k == 3)
        {
            m_botAction.setFreq(p.getPlayerName(), 2);
            m_botAction.warpTo(p.getPlayerID(), 250, 664);
        }
    }

    public void freq3(Player p, int k)
    {
        if(k == 0)
        {
            m_botAction.setFreq(p.getPlayerName(), 3);
            m_botAction.warpTo(p.getPlayerID(), 475, 912);
        }

        if(k == 1)
        {
            m_botAction.setFreq(p.getPlayerName(), 3);
            m_botAction.warpTo(p.getPlayerID(), 463, 845);
        }

        if(k == 2)
        {
            m_botAction.setFreq(p.getPlayerName(), 3);
            m_botAction.warpTo(p.getPlayerID(), 287, 806);
        }

        if(k == 3)
        {
            m_botAction.setFreq(p.getPlayerName(), 3);
            m_botAction.warpTo(p.getPlayerID(), 250, 864);
        }
    }

    public String[] getModHelpMessage()
    {
        String[] help = {
            "Soccer race -- 4 teams race to score a goal.  (Requires specific arena -- tfrace?)",
            "!start                         - starts race with wb's",
            "!start <#>                     - starts race with that ship #.",
            "!stop                          - stops the race",
            "!ballplace <0-3>               - warps you to the spot to place the ball",
            "!balls                         - warps you to balls default location"
        };
        return help;
    }

    public boolean isUnloadable()   {
        return true;
    }
}