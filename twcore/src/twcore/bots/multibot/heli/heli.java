package twcore.bots.multibot.heli;

import java.util.Random;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.OperatorList;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.game.Ship;

public class heli extends MultiModule{

	public OperatorList opList;
	int move = 48;
	int speed = 100;
	TimerTask nextWall;
	TimerTask nextBarrier;
	Random rand = new Random();
	int y = 0;
	int x = 0;
	int Slope = 3;
	int yDiff = 0;

	public void init(){
		opList = m_botAction.getOperatorList();
	}
	public String[] getModHelpMessage(){
		String[] blah = {
				"!start             -DURRRRRRRRRRRRRRRRRRRRR",
				"!setmove #         -Sets distance between barrier mines.",
				"!setslope #        -Sets difficulty increases in difficulty as you get higher.",
				"!setspeed #        -Sets bot's speed in milliseconds.",
				"!stop              -DURRRRRRRRRRRRRRRRRRRRR",
				"!specbot           -Puts bot into spectator mode."
			};
			return blah;
	}
	public void handleEvent(Message event)
	{
		String message = event.getMessage();
		String name = "";

		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			name = m_botAction.getPlayerName(event.getPlayerID());
		else if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
			name = event.getMessager();

		if(opList.isER(name))
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
		} else if(message.toLowerCase().startsWith("!setslope ")) {
			try {
				Slope = Integer.parseInt(message.substring(10));
			} catch(Exception e) {}
		} else if(message.toLowerCase().startsWith("!stop")) {
			m_botAction.cancelTasks();
		}
	}

	public void startThing()
	{
		Ship ship = m_botAction.getShip();
		y = ship.getY();
		x = ship.getX();
		yDiff = 0;
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
		m_botAction.scheduleTaskAtFixedRate(nextBarrier, speed*8 + speed/2, speed*8);
	}
	
	public void nextWall() {
		int slope = (rand.nextInt(200) - 100) / (100 / Slope);
		if(yDiff > 25 && slope > 0) {
			slope *= -1;
		} else if(yDiff < -25 && slope < 0) {
			slope *= -1;
		}
		slope *= 16;
		int distance = rand.nextInt(50) / 10;
		Ship ship = m_botAction.getShip();
		for(int k = 0;k < distance;k++) {
			ship.moveAndFire(x, y, getWeapon('#'));
			ship.moveAndFire(x, y + 25*16, getWeapon('#'));
			x += move;
			y -= slope;
			yDiff += slope / 16;
			if(x > (1024 * 16)) {
				m_botAction.cancelTasks();
			}
		}
	}
	
	public void nextBarrier() {
		Ship ship = m_botAction.getShip();
		int tiles = rand.nextInt(25);
		int length = 6;
		if(19 - tiles < 0) length += 19 - tiles;
		int yStart = y;
		int xStart = x;
		for(int k = 0;k < length;k++) {
			ship.moveAndFire(xStart, yStart + tiles*16 + k * 16, getWeapon('*'));
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
	public boolean isUnloadable(){return true;}
	public void requestEvents(ModuleEventRequester req){}
	public void cancel(){}
}