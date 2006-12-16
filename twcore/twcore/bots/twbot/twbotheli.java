package twcore.bots.twbot;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.events.Message;
import twcore.core.game.Ship;

import java.util.*;
import java.io.*;
import java.net.*;

public class twbotheli extends TWBotExtension
{

	int move = 16;
	int speed = 100;
	TimerTask nextWall;
	TimerTask nextBarrier;
	Random rand = new Random();
	int y = 0;
	int x = 0;

	public twbotheli()
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
		if(message.toLowerCase().startsWith("!start"))
			startThing();
		else if(message.toLowerCase().startsWith("!specbot"))
		{
			m_botAction.spec(m_botAction.getBotName());
			m_botAction.spec(m_botAction.getBotName());
		} else if(message.toLowerCase().startsWith("!setmove ")) {
			try {
				move = Integer.parseInt(message.substring(9));
			} catch(Exception e) {}
		} else if(message.toLowerCase().startsWith("!setspeed ")) {
			try {
				speed = Integer.parseInt(message.substring(10));
			} catch(Exception e) {}
		}
	}

	public void startThing()
	{
		Ship ship = m_botAction.getShip();
		y = ship.getY();
		x = ship.getX();
		nextWall = new TimerTask() {
			public void run() {
				nextWall();
			}
		};
		nextBarrier = new TimerTask() {
			public void run() {
				nextBarrier();
			}
		};
		m_botAction.scheduleTaskAtFixedRate(nextWall, speed, speed);
		m_botAction.scheduleTaskAtFixedRate(nextBarrier, speed*4, speed*4);
	}
	
	public void nextWall() {
		int slope = rand.nextInt(6) - 3;
		slope *= 16;
		int distance = rand.nextInt(5);
		Ship ship = m_botAction.getShip();
		for(int k = 0;k < distance;k++) {
			ship.moveAndFire(x, y, getWeapon('#'));
			ship.moveAndFire(x, y + 25*16, getWeapon('#'));
			x += 16;
			y -= slope;
			if(x > (1024 * 16)) {
				m_botAction.cancelTasks();
			}
		}
	}
	
	public void nextBarrier() {
		Ship ship = m_botAction.getShip();
		int tiles = rand.nextInt(10);
		int length = rand.nextInt(3) + 2;
		for(int k = 0;k < length;k++) {
			ship.moveAndFire(x, y + tiles*16 + k * 16, getWeapon('*'));
		}
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