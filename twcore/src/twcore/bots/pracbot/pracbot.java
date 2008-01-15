package twcore.bots.pracbot;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Timer;
import java.util.Vector;
import java.util.Iterator;

import twcore.bots.pracbot.Projectile;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.Message;
import twcore.core.events.WeaponFired;
import twcore.core.events.LoggedOn;
import twcore.core.events.PlayerPosition;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;
import twcore.core.OperatorList;
import twcore.core.game.Player;
import twcore.core.util.ipc.IPCMessage;

/**
 * The original PracBot.
 * 
 * @author milosh - 1.15.08
 */
public class pracbot extends SubspaceBot {

	private static final double SS_CONSTANT = 0.1111111;
	private static final int SPAWN_TIME = 5005;
	private  double REACTION_TIME = 0.075;
	
    private BotSettings m_botSettings;
    private OperatorList opList;
    private String turret = null;
	LinkedList<Projectile> fired = new LinkedList<Projectile>();
	Vector<RepeatFireTimer> repeatFireTimers = new Vector<RepeatFireTimer>();
    
	boolean isAiming = false, isSpawning = false;
	int botX, botY, botFreq;
	
	String[] helpmsg = {
		"PracBot Commands:",
		"!setship #			-- puts the bot into ship number #",
		"!warpto # #		-- operates like *warpto",
		"!face #			-- changes the bot's direction (0-39)",
		"!aim               -- toggles auto-aiming",
		"!fire #			-- fires a weapon. (Most common: 1 and 3)",
		"!repeatfire # #    -- repeats fire - (weapon):(ms repeat)",
	    "!rfire # #         -- shortcut for !repeatfire # #",
		"!listfire          -- displays a list of all firing tasks",
		"!stopfire #        -- stops firing task of index #",
		"!stopfire          -- stops all firing tasks.",
		"!attach #          -- attach to fuzzy player name",
		"!unattach          -- don't understand? this bot isn't for you",
		"!setaccuracy #     -- set difficulty(0-5) 0 being the highest",
		"!move # # # #      -- move bot to coords (#,#) velocity (#,#)",
		"!spec				-- puts the bot into spectator mode.",
		"Standard Bot Commands:",
		"!go <arena>		-- sends the bot to <arena>",
		"!die				-- initiates shut down sequence",
		"!help				-- displays this"
	};
	
	TimerTask spawned;
	TimerTask updateIt = new TimerTask(){
		public void run(){
			update();
		}
	};
    
    public pracbot(BotAction botAction) {
        super(botAction);
        requestEvents();
        m_botSettings = m_botAction.getBotSettings();
    }
    
    public void handleEvent(LoggedOn event) {
    	opList = m_botAction.getOperatorList();       
    	m_botAction.joinArena(m_botSettings.getString("Arena"));
    	m_botAction.ipcSubscribe("tutorialbots");
    }
    
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.WEAPON_FIRED);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.PLAYER_POSITION);
        req.request(EventRequester.PLAYER_DEATH);
        req.request(EventRequester.PLAYER_LEFT);
        req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
    }

    public void handleEvent(PlayerLeft event){
    	if(m_botAction.getPlayerName(event.getPlayerID()).equals(turret))doUnAttachCmd();
    }
    
    public void handleEvent(FrequencyShipChange event){
    	if(m_botAction.getPlayerName(event.getPlayerID()).equals(turret) && event.getShipType() == 0)doUnAttachCmd();
    }
    
    public void handleEvent(PlayerDeath event) {
    	if(turret == null)return;
    	String killed = m_botAction.getPlayerName(event.getKilleeID());
    	String killer = m_botAction.getPlayerName(event.getKillerID());
    	//TODO: Check to make sure the killer isn't a bot.
    	if(turret.equalsIgnoreCase(killed)){
    		doUnAttachCmd();
    		doAttachCmd(killer);
    	}
    }
    
    public void handleEvent(WeaponFired event) {
    	if(m_botAction.getShip().getShip() == 8)return;
    	if(turret != null) return;
    	Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null || (p.getFrequency() == botFreq) || event.getWeaponType() > 8 || event.isType(5) || event.isType(6) || event.isType(7))return;
        double pSpeed = p.getWeaponSpeed();
		double bearing = Math.PI * 2 * (double)event.getRotation() / 40.0;
		fired.add(new Projectile(p.getPlayerName(), event.getXLocation() + (short)(10.0 * Math.sin(bearing)), event.getYLocation() - (short)(10.0 * Math.cos(bearing)), event.getXVelocity() + (short)(pSpeed * Math.sin(bearing)), event.getYVelocity() - (short)(pSpeed * Math.cos(bearing)), event.getWeaponType(), event.getWeaponLevel()));
	}
    
    public void handleEvent(PlayerPosition event) {
    	if(m_botAction.getShip().getShip() == 8)return;
    	try{
    	Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null || !isAiming || (p.getFrequency() == botFreq))return;
        double diffY, diffX, distanceInTiles, angle;
    	diffY = (event.getYLocation() + (event.getYVelocity() * REACTION_TIME)) - botY;
    	diffX = (event.getXLocation() + (event.getXVelocity() * REACTION_TIME)) - botX;
    	distanceInTiles = (Math.sqrt(Math.pow(diffY, 2) + Math.pow(diffX, 2))) / 16;
    	angle = (180 - (Math.atan2(diffX, diffY)*180/Math.PI)) * SS_CONSTANT;
    	doFaceCmd(m_botAction.getBotName(), "" + angle);
    	Iterator<RepeatFireTimer> i = repeatFireTimers.iterator();    	
    	if(distanceInTiles < 40){
    		while (i.hasNext()){
        		RepeatFireTimer r = i.next();
        		r.stopSlowStop();
        	}
    	}
    	else{
    		while (i.hasNext()){
    			RepeatFireTimer r = i.next();
    			r.slowStop();
    		}
    	}    	
    	}catch(Exception e){}
    }
    
    public void update(){
     	if(turret == null){
     		ListIterator<Projectile> it = fired.listIterator();
     		while (it.hasNext()) {
     			Projectile b = (Projectile) it.next();     			
     			if (b.isHitting(botX, botY)) {
     				if(!isSpawning){
     					spawned = new TimerTask(){
     						public void run(){
     							isSpawning = false;
     						}
     					};
     					isSpawning = true;
     					m_botAction.scheduleTask(spawned, SPAWN_TIME);
     					IPCMessage ipcMessage = new IPCMessage("hit:" + b.getOwner() + ":" + botX + ":" + botY);
     					m_botAction.ipcTransmit("tutorialbots", ipcMessage);
     					m_botAction.sendDeath(m_botAction.getPlayerID(b.getOwner()), 0);
     					Iterator<RepeatFireTimer> i = repeatFireTimers.iterator();
     					while(i.hasNext())i.next().pause(); 				
     					it.remove();
     				}
     			} 
     			else if (b.getAge() > 5000) {
     				it.remove();
     			}
     		}
		}
     	else{
     		Player p = m_botAction.getPlayer(turret);
     		if(p == null)return;
     		botX = p.getXLocation();
			botY = p.getYLocation();
			int xVel = p.getXVelocity();
			int yVel = p.getYVelocity();
			m_botAction.getShip().move(botX, botY, xVel, yVel);
     	}
    }
    
    public void handleEvent(Message event) {
    	int messageType = event.getMessageType();
    	String msg = event.getMessage();
    	String name = m_botAction.getPlayerName(event.getPlayerID());
    	if(name == null) name = "-anonymous-";
    	if(messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE){
    		if(opList.isER(name)){
    			if(msg.startsWith("!setship "))
    				doSetShipCmd(name,msg.substring(9));
    			else if(msg.startsWith("!warpto "))
    				doWarpToCmd(name,msg.substring(8));
    			else if(msg.startsWith("!face "))
    				doFaceCmd(name,msg.substring(6));
    			else if(msg.startsWith("!fire "))
    				doFireCmd(msg.substring(6));
    			else if(msg.startsWith("!repeatfire "))
    				doRepeatFireCmd(name, msg.substring(12));
    			else if(msg.startsWith("!rfire "))
    				doRepeatFireCmd(name, msg.substring(7));
    			else if(msg.equalsIgnoreCase("!listfire"))
    				doFireListCmd(name);
    			else if(msg.startsWith("!stopfire "))
    				doStopRepeatFireCmd(msg.substring(10));
    			else if(msg.equalsIgnoreCase("!stopfire"))
    				doStopRepeatFireCmd(null);
    			else if(msg.equalsIgnoreCase("!aim"))
    				doAimCmd();
    			else if(msg.startsWith("!attach "))
    				doAttachCmd(msg.substring(8));
    			else if(msg.equalsIgnoreCase("!unattach"))
    				doUnAttachCmd();
    			else if(msg.startsWith("!setaccuracy "))
    				doSetAccuracyCmd(msg.substring(13));
    			else if(msg.startsWith("!move "))
    				doMoveCmd(name,msg.substring(6));
    			else if(msg.startsWith("!go "))
    				doGoCmd(name, msg.substring(4).trim());
    			else if (msg.equalsIgnoreCase("!die"))
                    doDieCmd(name);
    			else if (msg.equalsIgnoreCase("!spec"))
    				doSpecCmd(name);
    			else if (msg.equalsIgnoreCase("!help"))
    				m_botAction.smartPrivateMessageSpam( name, helpmsg ); 
    			/*
    			if(msg.equalsIgnoreCase("!blah"))
    				doBlahCmd();
    			if(msg.equalsIgnoreCase("!blah"))
    				doBlahCmd();
    			if(msg.equalsIgnoreCase("!blah"))
    				doBlahCmd();
    			if(msg.equalsIgnoreCase("!blah"))
    				doBlahCmd();
    			*/
    		}
    	}
    }
    
    public void doAttachCmd(String msg){
    	if(m_botAction.getShip().getShip() == 8)return;
    	String name = m_botAction.getFuzzyPlayerName(msg);
    	if(name==null)return;
    	Player p = m_botAction.getPlayer(name);
    	if(p==null)return;
    	m_botAction.getShip().setFreq(p.getFrequency());
    	botFreq = p.getFrequency();
    	m_botAction.getShip().attach(m_botAction.getPlayerID(name));
    	turret = name;
    }
    
    public void doUnAttachCmd(){
    	if(turret == null)return;
    	m_botAction.getShip().unattach();
    	turret = null;
    }
    
    public void doAimCmd(){
    	isAiming = !isAiming;
    }
    
    private void doDieCmd(String sender) {
        m_botAction.sendSmartPrivateMessage(sender, "Logging Off.");
        m_botAction.scheduleTask(new DieTask(), 500);
    }
    
    private void doGoCmd(String sender, String argString) {
        String currentArena = m_botAction.getArenaName();

        if (currentArena.equalsIgnoreCase(argString))
            throw new IllegalArgumentException("Bot is already in that arena.");
        if (isPublicArena(argString))
            throw new IllegalArgumentException("Bot can not go into public arenas.");
       	m_botAction.changeArena(argString);
        m_botAction.sendSmartPrivateMessage(sender, "Going to " + argString + ".");
    }
    
    private boolean isPublicArena(String arenaName) {
        try {
            Integer.parseInt(arenaName);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public void doSetShipCmd(String name, String message){
    	if(message.trim().length() == 1){
    		try{
    			int ship = Integer.parseInt(message);
    			if(ship <= 9 && ship >= 1){
    				m_botAction.getShip().setShip(ship-1);
    				botX = m_botAction.getShip().getX();
    				botY = m_botAction.getShip().getY();
    				botFreq = 0;
    				m_botAction.getShip().setFreq(0);
    				m_botAction.scheduleTaskAtFixedRate(updateIt, 100, 100);
    			}
    		}catch(Exception e){}    		
    	}
    	else
    		m_botAction.sendSmartPrivateMessage( name, "Incorrect command usage.");
    }
    
    public void doSpecCmd(String name){
    	if(m_botAction.getShip().getShip() == 8)return;
    	m_botAction.cancelTask(updateIt);
    	String myName = m_botAction.getBotName();
    	m_botAction.spec(myName); m_botAction.spec(myName);
    }
    public void doWarpToCmd(String name, String message){
    	if(m_botAction.getShip().getShip() == 8)return;
    	String[] msg = message.split(" ");
    	try {
    		int x = Integer.parseInt(msg[0]) * 16;
    		int y = Integer.parseInt(msg[1]) * 16;
    		m_botAction.getShip().move(x, y);
    		botX = x;
    		botY = y;
    	}catch(Exception e){}
    }
	public void doFaceCmd(String name, String message){
		if(m_botAction.getShip().getShip() == 8)return;
		try{
			float degree = Float.parseFloat(message);
			int l = Math.round(degree);		
			m_botAction.getShip().setRotation(l);
		}catch(Exception e){}
	}
	public void doFireCmd(String msg){
		if(m_botAction.getShip().getShip() == 8)return;
		try{
			int wep = Integer.parseInt(msg);
			m_botAction.getShip().fire(wep);
		}catch(Exception e){}
	}
	public void doRepeatFireCmd(String name, String message){
		try{
			StringTokenizer msg = getArgTokens(message);
			if(msg.countTokens() != 2){m_botAction.sendSmartPrivateMessage( name, "Format: !repeatfire <Weapon>:<Repeat in Miliseconds>");return;}
			int weapon = Integer.parseInt(msg.nextToken());
			int timerStart = 0;
			int repeatTime = Integer.parseInt(msg.nextToken());
			if(repeatTime >= 200){
				if(repeatFireTimers.size() > 2){
					m_botAction.sendSmartPrivateMessage( name, "Please, no more than three firing tasks.");
				}else new RepeatFireTimer(weapon, timerStart, repeatTime);
			}
			else
				m_botAction.sendSmartPrivateMessage( name, "Sending a weapon packet every " + repeatTime + "ms can cause the 0x07 bi-directional packet.");
			
		}catch(Exception e){}
	}
	
	public void doFireListCmd(String name){
		if(repeatFireTimers.size() == 0){			
			m_botAction.sendSmartPrivateMessage( name, "There are currently no firing tasks.");
			return;
		}
		Iterator<RepeatFireTimer> i = repeatFireTimers.iterator();
		while(i.hasNext()){
			m_botAction.sendSmartPrivateMessage( name, i.next().toString());
		}
	}
	
	public void doStopRepeatFireCmd(String message){
		if(repeatFireTimers.size() == 0){			
			return;
		}
		if(message == null){
			Iterator<RepeatFireTimer> i = repeatFireTimers.iterator();
			while(i.hasNext()){
				RepeatFireTimer r = i.next();
				r.cancel();
			}
			repeatFireTimers.clear();
		}
		else{
			try{
				int index = Integer.parseInt(message) - 1;
				repeatFireTimers.elementAt(index).cancel();
				repeatFireTimers.removeElementAt(index);
			}catch(Exception e){}
		}
	}
	
	public void doSetAccuracyCmd(String message){
		try{
			int x = Integer.parseInt(message);
			if(x < 0 || x > 5)return;
			REACTION_TIME = .075;
			for(int i = 0; i < x; i++){
				REACTION_TIME -= .02;
			}			
		}catch(Exception e){}
	}
	
	public void doMoveCmd(String name, String message){
		if(m_botAction.getShip().getShip() == 8)return;
		StringTokenizer argTokens = getArgTokens(message);
	    int numTokens = argTokens.countTokens();
	    if(numTokens != 4 && numTokens != 2)
	    	m_botAction.sendSmartPrivateMessage( name, "Format: !move X:Y:XVelocity:YVelocity");
	    else if(numTokens == 2)
	    	m_botAction.sendSmartPrivateMessage( name, "For a stationary warp use !warpto <X>:<Y>");
	    else if(numTokens == 4){
	    	try{
	    		int x = Integer.parseInt(argTokens.nextToken()) * 16;
	    		int y = Integer.parseInt(argTokens.nextToken()) * 16;
	    		int xVel = Integer.parseInt(argTokens.nextToken());
	    		int yVel = Integer.parseInt(argTokens.nextToken());
	    		m_botAction.getShip().move(x,y,xVel,yVel);
	    	}catch(Exception e){}
	    }
	}
	
	private StringTokenizer getArgTokens(String string)
	  {
	    if(string.indexOf((int) ':') != -1)
	      return new StringTokenizer(string, ":");
	    return new StringTokenizer(string);
	  }
	
	
	
	private class DieTask extends TimerTask {
        public void run() {
            m_botAction.die();
        }
    }
private class RepeatFireTimer{
	private int SPAWN_TIME = 5005;
	public int weapon, delayms, repeatms;
	public boolean isRunning = true, isSlowlyStopping = false;
	public Timer clock = new Timer();
	TimerTask repeat;
	TimerTask slowly;
	
	public RepeatFireTimer(int wep, int delayms, int repeatms){
		this.weapon = wep;
		this.delayms = delayms;
		this.repeatms = repeatms;
		repeatFireTimers.add(this);
		repeat = new TimerTask(){
			public void run(){
				doFireCmd(weapon + "");
			}
		};
		clock.scheduleAtFixedRate(this.repeat, this.delayms, this.repeatms);
	}
	public void cancel(){
		this.clock.cancel();
		isRunning = false;
	}
	public void pause(){
		if(!isRunning)return;
		repeat.cancel();
		repeat = new TimerTask(){
			public void run(){
				doFireCmd(weapon + "");
			}
		};
		clock.scheduleAtFixedRate(this.repeat, this.SPAWN_TIME, this.repeatms);		
	}
	
	public void stop(){
		if(isRunning){
			repeat.cancel();
			isRunning = false;
		}
	}
	public void resume(){
		if(isRunning)return;
		repeat = new TimerTask(){
			public void run(){
				doFireCmd(weapon + "");
			}
		};
		clock.scheduleAtFixedRate(this.repeat, 0, this.repeatms);
		isRunning = true;
	}
	public void slowStop(){
		if(isSlowlyStopping)return;
		slowly = new TimerTask(){
			public void run(){
				repeat.cancel();
				isRunning = false;
			}
		};
		clock.schedule(slowly, 1000);
		isSlowlyStopping = true;
	}
	public void stopSlowStop(){
		if(isSlowlyStopping){
			slowly.cancel();
			if(!isRunning)resume();
			isSlowlyStopping = false;
		}
	}
	public String toString(){
		String s = (repeatFireTimers.indexOf(this)+1) + ") Firing weapon(" + weapon + ") every " + repeatms + " ms." ;		
		return s;
	}
}



}