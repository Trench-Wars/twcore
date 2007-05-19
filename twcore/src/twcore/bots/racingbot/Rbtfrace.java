/*
 *Team Freq Race Module
 *
 *Created By: Jacen Solo
 *
 *Created: 05/24/04 at
 */

package twcore.bots.racingbot;

import java.util.Iterator;

import twcore.core.events.Message;
import twcore.core.events.SoccerGoal;
import twcore.core.game.Player;

public class Rbtfrace extends RBExtender
{

	int ship = 4;
	Player p;
	int k = 0;

	public Rbtfrace()
	{

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
            if(m_opList.isER(name) || m_rbBot.twrcOps.contains(name.toLowerCase()))
            	handleCommand(name, message);
        }
    }

    public void handleEvent(SoccerGoal event)
    {
    	int winfreq = event.getFrequency();
    	m_botAction.sendArenaMessage("Congratulations to freq " + winfreq + " they have scored and won the game!!!", 5);
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
    		catch(Exception e) {}
    		ship = Ship;
			m_botAction.changeAllShips(ship);
    		m_botAction.sendArenaMessage("Rules: Take the ball through your section of the course and pass the ball to your teammate at the end of your section",2);
    		m_botAction.sendArenaMessage("The first team to score wins, good luck and have fun!");
    		freq0();
    		freq1();
    		freq2();
    		freq3();
    		m_botAction.sendArenaMessage("Gooooo go go go gooooooo", 104);
    	}

    	if(message.toLowerCase().startsWith("!stop"))
    	{
    		m_botAction.sendArenaMessage("This race has been stopped by: " + name);
    		m_botAction.sendArenaMessage("Have fun in Trench Wars", 19);
    	}
    	if(message.toLowerCase().startsWith("!ballplace"))
    	{
    		String pieces2[] = message.split(" ");
    		try
    		{
    			if(Integer.parseInt(pieces2[1]) == 0)
    				m_botAction.warpTo(name,453,312);
    			if(Integer.parseInt(pieces2[1]) == 1)
    				m_botAction.warpTo(name,453,512);
    			if(Integer.parseInt(pieces2[1]) == 2)
    				m_botAction.warpTo(name,453,712);
    			if(Integer.parseInt(pieces2[1]) == 3)
    				m_botAction.warpTo(name,453,912);
    		}
    		catch(Exception e) {m_botAction.sendPrivateMessage(name,"Wrong format for this method");}
    	}
    	if(message.toLowerCase().startsWith("!balls"))
    	{
    		m_botAction.warpTo(name,512,512);
    	}
    }

    public void freq0()
    {
    	Iterator it = m_botAction.getPlayingPlayerIterator();
    	while(it.hasNext())
    	{
    		p = (Player) it.next();
    		if(p.getFrequency() == 0)
    		{
    			if(k == 0)
    			{
    				m_botAction.warpTo(p.getPlayerID(),475,312);
    			}
    			if(k == 1)
    			{
    				m_botAction.warpTo(p.getPlayerID(),463,245);
    			}
    			if(k == 2)
    			{
    				m_botAction.warpTo(p.getPlayerID(),287,206);
    			}
    			if(k == 3)
    			{
    				m_botAction.warpTo(p.getPlayerID(),250,264);
    			}
    			k++;
    		}
    	}
    	k = 0;
    }

    public void freq1()
    {
    	Iterator it = m_botAction.getPlayingPlayerIterator();
    	while(it.hasNext())
    	{
    		p = (Player) it.next();
    		if(p.getFrequency() == 1)
    		{
    			if(k == 0)
    			{
    				m_botAction.warpTo(p.getPlayerID(),475,512);
    			}
    			if(k == 1)
    			{
    				m_botAction.warpTo(p.getPlayerID(),463,445);
    			}
    			if(k == 2)
    			{
    				m_botAction.warpTo(p.getPlayerID(),287,406);
    			}
    			if(k == 3)
    			{
    				m_botAction.warpTo(p.getPlayerID(),250,464);
    			}
    			k++;
    		}
    	}
    	k = 0;
    }

    public void freq2()
    {
    	Iterator it = m_botAction.getPlayingPlayerIterator();
    	while(it.hasNext())
    	{
    		p = (Player) it.next();
    		if(p.getFrequency() == 2)
    		{
    			if(k == 0)
    			{
    				m_botAction.warpTo(p.getPlayerID(),475,712);
    			}
    			if(k == 1)
    			{
    				m_botAction.warpTo(p.getPlayerID(),463,645);
    			}
    			if(k == 2)
    			{
    				m_botAction.warpTo(p.getPlayerID(),287,606);
    			}
    			if(k == 3)
    			{
    				m_botAction.warpTo(p.getPlayerID(),250,664);
    			}
    			k++;
    		}
    	}
    	k = 0;
    }

    public void freq3()
    {
    	Iterator it = m_botAction.getPlayingPlayerIterator();
    	while(it.hasNext())
    	{
    		p = (Player) it.next();
    		if(p.getFrequency() == 3)
    		{
    			if(k == 0)
    			{
    				m_botAction.warpTo(p.getPlayerID(),475,912);
    			}
    			if(k == 1)
    			{
    				m_botAction.warpTo(p.getPlayerID(),463,845);
    			}
    			if(k == 2)
    			{
    				m_botAction.warpTo(p.getPlayerID(),287,806);
    			}
    			if(k == 3)
    			{
    				m_botAction.warpTo(p.getPlayerID(),250,864);
    			}
    			k++;
    		}
    	}
    	k = 0;
    }

    public String[] getHelpMessages()
	{
		String[] help = {
		"!start                         - starts race with wb's",
		"!start <#>                     - starts race with that ship #.",
		"!stop                          - stops the race",
		"!ballplace <0-3>               - warps you to the spot to place the ball",
		"!balls                         - warps you to balls default location"
		};
		return help;
	}
}