package twcore.bots.twbot;

import twcore.core.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class twbotart extends TWBotExtension
{
	public twbotart()
	{
	}
	
	public void handleEvent(Message event)
	{
		String message = event.getMessage();
		String name = "";
		
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			name = m_botAction.getPlayerName(event.getPlayerID());
		else if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
			name = event.getMessager();
			
		if(m_botAction.getOperatorList().isZH(name))
			handleCommand(name, message);
	}
	
	public void handleCommand(String name, String message)
	{
		if(message.toLowerCase().startsWith("!draw "))
			download(name, message.substring(6));
		else if(message.toLowerCase().startsWith("!specbot"))
		{
			m_botAction.spec(m_botAction.getBotName());
			m_botAction.spec(m_botAction.getBotName());
		}
	}
	
	public void download(String name, String message)
	{
		try {
			Ship ship = m_botAction.getShip();
			URL url = new URL(message);
			URLConnection URLC = url.openConnection();
			URLC.connect();
			BufferedReader in = new BufferedReader(new InputStreamReader(URLC.getInputStream()));
			in.mark(0);
			String inString;
			int chars = 0;
			while((inString = in.readLine()) != null)
			{
				inString = inString.replaceAll(" ", "");
				chars += inString.length();
			}
			if(chars < 1000)
			{
				ship.setShip(1);
				URLC = url.openConnection();
				URLC.connect();
				in = new BufferedReader(new InputStreamReader(URLC.getInputStream()));
				int xNormal = ship.getX();
				while((inString = in.readLine()) != null)
				{
					for(int k = 0;k < inString.length();k++)
					{
						if(inString.charAt(k) == ' ')
							ship.move(ship.getX() + 16, ship.getY() + 0);
						else
						{
							ship.fire(-32700);
							ship.move(ship.getX() + 16, ship.getY() + 0);
						}
					}
					ship.move(xNormal, ship.getY() + 16);
				}
			}
			else
				m_botAction.sendPrivateMessage(name, "That file contains too many mines. You should try reducing it so I can draw all of it.");
		} catch(Exception e) {m_botAction.sendPrivateMessage(name, "error... check URL and try again."); e.printStackTrace();}
	}
	
	public void cancel()
	{
	}
	
	public String[] getHelpMessages()
	{
		String[] blah = {
			"!draw <url>        -Draws the text file at <url>.",
			"!specbot           -Puts bot into spectator mode."
		};
		return blah;
	}
}



