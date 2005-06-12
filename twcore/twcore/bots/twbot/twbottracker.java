package twcore.bots.twbot;

import twcore.core.*;

/** Bot that follows you around and shoots when you shoot!
 * @author Jacen Solo
 * @version 1.x
 */
public class twbottracker extends TWBotExtension
{
	String trackee = "";

	public twbottracker(){	}
	
	/** Gives the bot to the person that killed the bot's master
	 */
	public void handleEvent(PlayerDeath event)
	{
		if(m_botAction.getPlayerName(event.getKilleeID()).toLowerCase().equals(trackee.toLowerCase()))
		{
			if(!m_botAction.getPlayerName(event.getKillerID()).toLowerCase().equals(m_botAction.getBotName().toLowerCase()))
			{
				trackee = m_botAction.getPlayerName(event.getKillerID());
				Player p = m_botAction.getPlayer(trackee);
				m_botAction.setFreq(m_botAction.getBotName(), p.getFrequency());
				m_botAction.setShip(m_botAction.getBotName(), p.getShipType());
			}
		}
	}
	
	/** sets the bot's ship and freq to match the person it is following
	 */
	public void handleEvent(FrequencyShipChange event)
	{
		if(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase().equals(trackee.toLowerCase()))
		{
			m_botAction.setFreq(m_botAction.getBotName(), event.getFrequency());
			m_botAction.setShip(m_botAction.getBotName(), event.getShipType());
		}
	}
	
	/** handles messages from a ER+
	 */
	public void handleEvent(Message event)
	{
		if( event.getMessageType() == Message.PRIVATE_MESSAGE )
		{
			String message = event.getMessage();
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(m_botAction.getOperatorList().isER(name))
			{
				if(message.startsWith("!track"))
				{
					String pieces[] = message.split(" ",2);
					String tempT = name;
					try {
						tempT = pieces[1];
					} catch(Exception e) {}
					if(tempT.equals(m_botAction.getBotName()))
						tempT = "";
					trackee = tempT;
					try{
					Player p = m_botAction.getPlayer(trackee);
					m_botAction.setFreq(m_botAction.getBotName(), p.getFrequency());
					m_botAction.setFreq(trackee, p.getFrequency());
					m_botAction.setShip(m_botAction.getBotName(), p.getShipType());
					} catch(Exception e) {}
				}
			}
		}
	}
	
	/** makes the bot follow everything the person does if it is "tracking" the person
	 */
	public void handleEvent(PlayerPosition event)
	{
		if(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase().equals(trackee.toLowerCase()))
		{
			int x = event.getXLocation();
			int y = event.getYLocation();
			int xVelo = event.getXVelocity();
			int yVelo = event.getYVelocity();
			int rotate = event.getRotation();
			Ship s = m_botAction.getShip();
			s.move(x, y, xVelo, yVelo);
			s.setRotation(rotate);
		}
	}
	
	/** makes the bot fire when the person fires
	 */	
	public void handleEvent(WeaponFired event)
	{
		if(m_botAction.getPlayerName(event.getPlayerID()).toLowerCase().equals(trackee.toLowerCase()))
		{
			int wep = event.getWeaponType();
			int lvl = event.getWeaponLevel();
			Ship s = m_botAction.getShip();
			if(lvl == 2)
				wep += 32;
			if(lvl == 3)
				wep += 64;
			s.fire(wep);
		}
	}
	
	public void cancel()
	{
	}
	
	public String[] getHelpMessages() {
		String helps[] = { "!track <name>      -Sets name of the player the bot is tracking." };
		return helps;
	}
}