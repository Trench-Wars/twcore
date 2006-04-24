package twcore.bots.twbot;

import twcore.core.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class twbotart extends TWBotExtension
{
	
	int move = 16;
	
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
			
		if(m_opList.isZH(name))
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
		} else if(message.toLowerCase().startsWith("!setmove ")) {
			try {
				move = Integer.parseInt(message.substring(9));
			} catch(Exception e) {}
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
					if(inString.length() != 0)
					{
						ship.fire(getWeapon(inString.charAt(0)));
						for(int k = 1;k < inString.length();k++)
						{
							int temp = 0;
							if(inString.charAt(k) == ' ') {
								for(temp = 0;(temp + k) < inString.length() && inString.charAt((temp + k)) == ' ';temp++) { }
								k += (temp - 1);
								ship.move(ship.getX() + (move * temp), ship.getY());
							} else if(inString.charAt(k) == '?') {
								ship.move(0, ship.getX(), ship.getY(), 0, 0, 4, 1200, 3);
								ship.move(0, ship.getX(), ship.getY(), 0, 0, 0, 1200, 3);
							} else {
								ship.moveAndFire(ship.getX() + move, ship.getY() + 0, getWeapon(inString.charAt(k)));
							}
						}
						ship.move(xNormal, ship.getY() + move);
					}
				}
			}
			else
				m_botAction.sendPrivateMessage(name, "That file contains too many mines. You should try reducing it so I can draw all of it.");
		} catch(Exception e) {m_botAction.sendPrivateMessage(name, "error... check URL and try again."); e.printStackTrace();}
	}
	
	public int getWeapon(char c) {
		Ship s = m_botAction.getShip();
		
		if(c == '.') return s.getWeaponNumber((byte)3, (byte)0, false, false, true, (byte)8, true);
		if(c == '*') return s.getWeaponNumber((byte)3, (byte)1, false, false, true, (byte)8, true);
		if(c == '#') return s.getWeaponNumber((byte)3, (byte)2, false, false, true, (byte)8, true);
		if(c == '^') return s.getWeaponNumber((byte)3, (byte)3, false, false, true, (byte)8, true);
		if(c == '1') return s.getWeaponNumber((byte)4, (byte)0, false, false, true, (byte)8, true);
		if(c == '2') return s.getWeaponNumber((byte)4, (byte)1, false, false, true, (byte)8, true);
		if(c == '3') return s.getWeaponNumber((byte)4, (byte)2, false, false, true, (byte)8, true);
		if(c == '4') return s.getWeaponNumber((byte)4, (byte)3, false, false, true, (byte)8, true);
		return 0;
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