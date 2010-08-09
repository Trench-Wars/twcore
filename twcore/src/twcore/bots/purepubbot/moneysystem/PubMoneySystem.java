package twcore.bots.purepubbot.moneysystem;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.purepubbot.PubException;
import twcore.bots.purepubbot.moneysystem.item.PubCommandItem;
import twcore.bots.purepubbot.moneysystem.item.PubItem;
import twcore.bots.purepubbot.moneysystem.item.PubItemRestriction;
import twcore.bots.purepubbot.moneysystem.item.PubPrizeItem;
import twcore.bots.purepubbot.moneysystem.item.PubShipItem;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.util.Point;
import twcore.core.util.PointLocation;
import twcore.core.util.Tools;

/**
 * This class wrap the money system to keep the PurePubBot clean
 * Everything related to the money system should be implemented here.
 */
public class PubMoneySystem {

	private OperatorList opList;
	private BotAction m_botAction;
	private BotSettings m_botSettings;
	
	private PubStore store;
    private Map<String, PubPlayer> players;
    
    // These variables are used to calculate the money earned for a kill
    private Map<Integer, Integer> shipKillerPoints;
    private Map<Integer, Integer> shipKilledPoints;
    private LinkedHashMap<PubLocation, Integer> locationPoints;
    
    private LvzMoneyPanel lvzPubPointsHandler;
	
    public PubMoneySystem(BotAction botAction) {

    	m_botAction = botAction;
    	m_botSettings = botAction.getBotSettings();
    	opList = m_botAction.getOperatorList();
    	
    	lvzPubPointsHandler = new LvzMoneyPanel(m_botAction);

        this.store = new PubStore();
        try {
        	initializeStore();
        } catch (Exception e) {
			Tools.printStackTrace("Error while initializing the store", e);
		}
        
        this.players = new HashMap<String, PubPlayer>();
        
        this.shipKillerPoints = new HashMap<Integer, Integer>();
        this.shipKilledPoints = new HashMap<Integer, Integer>();
        this.locationPoints = new LinkedHashMap<PubLocation, Integer>();
        try {
        	initializePoints();
	    } catch (Exception e) {
	    	Tools.printStackTrace("Error while initializing the money system", e);
		}
    	
    }

    private void initializeStore() {
    	
        if (m_botSettings.getInt("store_enabled")==0) {
        	store.turnOff();
    	}
        
        String[] itemTypes = { "item_prize", "item_ship", "item_command" };
        for(String type: itemTypes) {
        	
	    	String[] items = m_botSettings.getString(type).split(",");
	    	for(String number: items) {
	    		
	    		String[] data = m_botSettings.getString(type+number).split(",");
	    		
	    		int optionPointer = 0;
	    		PubItem item = null;
	    		if ("item_prize".equals(type)) {
	    			item = new PubPrizeItem(data[0], data[1], Integer.parseInt(data[2]), Integer.parseInt(data[3]));
	    			optionPointer = 4;
	    		} 
	    		else if ("item_ship".equals(type)) {
	    			item = new PubShipItem(data[0], data[1], Integer.parseInt(data[2]), Integer.parseInt(data[3]));
	    			optionPointer = 4;
	    		}
	    		else if ("item_command".equals(type)) {
	    			item = new PubCommandItem(data[0], data[1], Integer.parseInt(data[2]), data[3]);
	    			optionPointer = 4;
	    		}
	    		store.addItem(item, data[0]);
	
	    		// Options?
	    		if (data.length > optionPointer) {
	    			PubItemRestriction r = new PubItemRestriction();
	    			for(int i=optionPointer; i<data.length; i++) {
	    				String option = data[i];
	    				if(option.startsWith("!s")) {
	    					int ship = Integer.parseInt(option.substring(2));
	    					r.addShip(ship);
	    				} else if(option.startsWith("!mp")) {
	    					int max = Integer.parseInt(option.substring(3));
	    					r.setMaxPerLife(max);
	    				} else if(option.startsWith("!mc")) {
	    					int max = Integer.parseInt(option.substring(3));
	    					r.setMaxConsecutive(max);
	    				} else if(option.startsWith("!adm")) {
	    					int max = Integer.parseInt(option.substring(4));
	    					r.setMaxArenaPerMinute(max);
	    				} else if(option.startsWith("!arena")) {
	    					item.setArenaItem(true);
	    				}
	    			}
	    			item.setRestriction(r);
	    		}
	    	}
        }
        
    }
    
    /**
     * Gets default settings for the points: area and ship
     * By points, we mean "money".
     * */
    private void initializePoints(){

        String[] pointsLocation = m_botSettings.getString("point_location").split(",");
        for(String number: pointsLocation) {
        	String[] data = m_botSettings.getString("point_location"+number).split(",");
        	String name = data[0];
        	int points = Integer.parseInt(data[1]);
        	Vector<Point> listPoints = new Vector<Point>();
        	for(int i=2; i<data.length; i++) {
        		String[] coords = data[i].split(":");
        		int x = Integer.parseInt(coords[0]);
        		int y = Integer.parseInt(coords[1]);
        		listPoints.add(new Point(x, y));
        	}
        	PointLocation p = new PointLocation(listPoints, false);
        	PubLocation location = new PubLocation(p, name);
        	locationPoints.put(location, points);
        }
        
        String[] pointsKiller = m_botSettings.getString("point_killer").split(",");
        String[] pointsKilled = m_botSettings.getString("point_killed").split(",");
        
        for(int i=1; i<=8; i++) {
        	shipKillerPoints.put(i, Integer.parseInt(pointsKiller[i-1]));
        	shipKilledPoints.put(i, Integer.parseInt(pointsKilled[i-1]));
        }

    }
    

    private void buyItem(String playerName, String itemName, String params, int shipType){
        try{

            if (players.containsKey(playerName)){
            	
            	PubPlayer player = players.get(playerName);
            	
            	int moneyBeforeBuy = players.get(playerName).getMoney();
                PubItem item = store.buy(itemName, players.get(playerName), shipType);
                int moneyAfterBuy = player.getMoney();

                if (item instanceof PubPrizeItem) {
                	m_botAction.specificPrize(player.getPlayerName(), ((PubPrizeItem) item).getPrizeNumber());
                }
                else if (item instanceof PubCommandItem) {
                	String command = ((PubCommandItem)item).getCommand();
            		Method method = this.getClass().getDeclaredMethod("itemCommand"+command, String.class, String.class);
            		method.invoke(this, playerName, params);
                } 
                else if (item instanceof PubShipItem) {
                	
                } 
                
                if (item.isArenaItem()) {
                	 m_botAction.sendArenaMessage(playerName + " just bought a " + item.getDisplayName() + " for $" + item.getPrice() + ".",21);
                }
                
                int playerId = m_botAction.getPlayerID(playerName);
                this.lvzPubPointsHandler.updatePanel(playerId, String.valueOf(moneyBeforeBuy), String.valueOf(moneyAfterBuy), false);
  
            } 
            else {
                m_botAction.sendPrivateMessage(playerName, "You're not in the system to use !buy.");
            }
            
        }
        catch(PubException e) {
        	 m_botAction.sendPrivateMessage(playerName, e.getMessage());
        }
        catch(Exception e){
            Tools.printStackTrace(e);
        }
        
    }
    

    private void doCmdBuy(String sender, String command){
        Player p = m_botAction.getPlayer(sender);
        if(p == null)
            return;
        command = command.substring(command.indexOf(" ")).trim();
        if (command.indexOf(" ")!=-1) {
        	String params = command.substring(command.indexOf(" ")).trim();
        	command = command.substring(0, command.indexOf(" ")).trim();
        	buyItem(sender, command, params, p.getShipType());
        } else {
        	buyItem(sender, command, "", p.getShipType());
        }
    }
    
    private void doCmdDisplayMoney(String sender){
        if(players.containsKey(sender)){
            PubPlayer pubPlayer = this.players.get(sender);
            m_botAction.sendPrivateMessage(sender, "You have $"+pubPlayer.getMoney());
        }else
            m_botAction.sendPrivateMessage(sender, "You're still not in the point system. Wait a bit to be added");
    }
 
    public void handleEvent(Message event) {
    	
        String sender = getSender(event);
        int messageType = event.getMessageType();
        String message = event.getMessage().trim();

        if( message == null || sender == null )
            return;

        message = message.toLowerCase();
        if((messageType == Message.PRIVATE_MESSAGE || messageType == Message.PUBLIC_MESSAGE ) )
            handlePublicCommand(sender, message);
        if ( opList.isHighmod(sender) || sender.equals(m_botAction.getBotName()) )
            if((messageType == Message.PRIVATE_MESSAGE || messageType == Message.REMOTE_PRIVATE_MESSAGE) )
                handleModCommand(sender, message);
    	
    }
    
    public void handleEvent(PlayerDeath event) {
    	
    	Player killer = m_botAction.getPlayer( event.getKillerID() );
        Player killed = m_botAction.getPlayer( event.getKilleeID() );
        
        // Do nothing if the bot killed someone
        if (killer.getPlayerName().equals(m_botAction.getBotName()))
        	return;
        
        // Reset item for the killed
        PubPlayer pubPlayerKilled = players.get(killed.getPlayerName());
        pubPlayerKilled.resetItems();
        
        if( killer == null || killed == null )
            return;

        try{
        	
            int money = 0;

            // Points from the ship
            money += shipKillerPoints.get((int)killer.getShipType());
            money += shipKilledPoints.get((int)killed.getShipType());
            
            // Points from the location
            Point pointXY = new Point(killer.getXTileLocation(), killer.getYTileLocation());
            for(PubLocation location: locationPoints.keySet()) {
            	if (location.isInside(pointXY)) {
            		money += locationPoints.get(location);
            		break;
            	}
            }

            String playerName = killer.getPlayerName();
            PubPlayer pubPlayer = players.get(playerName);
            
            int currentMoney = pubPlayer.getMoney();
            pubPlayer.setMoney(currentMoney+money);

            lvzPubPointsHandler.updatePanel(killer.getPlayerID(), String.valueOf(currentMoney), String.valueOf(currentMoney+money), true);
            players.put(playerName, pubPlayer);

            Tools.printLog("Added "+money+" to "+playerName+" TOTAL POINTS: "+pubPlayer.getMoney());

        } catch(Exception e){
            Tools.printStackTrace(e);
        }
    	
    }
    
    public void handleEvent(PlayerEntered event) {
    	
    	Player p = m_botAction.getPlayer(event.getPlayerID());
    	String playerName = p.getPlayerName();
    	
        PubPlayer pubPlayer = this.players.get(playerName); 
        int playerId = m_botAction.getPlayerID(playerName);
        if(pubPlayer == null){
            players.put( playerName, new PubPlayer(playerName) );
            this.lvzPubPointsHandler.updatePanel(playerId, String.valueOf(0), String.valueOf(0), true);
        } else {
            this.lvzPubPointsHandler.updatePanel(playerId, String.valueOf(0), String.valueOf(pubPlayer.getMoney()), true);
        }
    }
    
    public void handleEvent(FrequencyShipChange event) {
		
        int playerID = event.getPlayerID();
        Player p = m_botAction.getPlayer(playerID);
    	
		PubPlayer pubPlayer = players.get(p.getPlayerName());
		if (pubPlayer!=null) {
			pubPlayer.resetItems();
		}
    }

    public void handlePublicCommand(String sender, String command) {
        try {
 
            if(command.startsWith("!rich")) {
            	PubPlayer player = players.get(sender);
            	int pts = player.getMoney();
            	player.setMoney( pts + 100000 );
                int playerId = m_botAction.getPlayerID(sender);
                this.lvzPubPointsHandler.updatePanel(playerId, String.valueOf(pts), String.valueOf(pts+100000), true);

            }
            else if(command.equals("!$"))
            {
                doCmdDisplayMoney(sender);
                
            }
            else if(command.startsWith("!b ") || command.startsWith("!buy ")){
                try{
                    doCmdBuy(sender, command);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            
        } catch(RuntimeException e) {
            if( e != null && e.getMessage() != null )
                m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }
    
    public void handleModCommand(String sender, String command) {
        try {
        	/*
            if(command.startsWith("!go "))
                doGoCmd(sender, command.substring(4));
			*/
        } catch(RuntimeException e) {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }
    
    
    private String getSender(Message event)
    {
        if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
            return event.getMessager();

        int senderID = event.getPlayerID();
        return m_botAction.getPlayerName(senderID);
    }
    
    /**
     * Not always working.. need to find out why.
     */
    public void itemCommandSuddenDeath(final String sender, String params) throws PubException{

    	if (params.equals("")) {
    		throw new PubException("You must add 1 parameter when you buy this item.");
    	}
    	
    	final String playerName = params;

    	m_botAction.spectatePlayerImmediately(playerName);
    	
        Player p = m_botAction.getPlayer(playerName);
    	int distance = 10*16; // distance from the player and the bot
    	
    	int x = p.getXLocation();
    	int y = p.getYLocation();
    	int angle = (int)p.getRotation()*9;

    	int bot_x = x + (int)(-distance*Math.sin(Math.toRadians(angle)));
    	int bot_y = y + (int)(distance*Math.cos(Math.toRadians(angle)));

    	m_botAction.getShip().setShip(0);
    	m_botAction.getShip().rotateDegrees(angle-90);
    	m_botAction.getShip().move(bot_x, bot_y);
    	m_botAction.getShip().sendPositionPacket();
    	m_botAction.getShip().fire(WeaponFired.WEAPON_THOR);
    	
    	TimerTask timer = new TimerTask() {
            public void run() {
            	m_botAction.specWithoutLock(m_botAction.getBotName());
            	m_botAction.sendUnfilteredPrivateMessage(playerName, "I've been ordered by " + sender + " to kill you.",7);
            }
        };
        m_botAction.scheduleTask(timer, 500);


    }
    
    
    private void itemCommandNukeBase(String sender, String params) {

	   	Player p = m_botAction.getPlayer(sender);
	   	
	    //m_botAction.setFreq(m_botAction.getBotName(),(int)p.getFrequency());
    	m_botAction.getShip().setShip(1);
    	m_botAction.getShip().rotateDegrees(90);
    	m_botAction.setThorAdjust(10);
    	m_botAction.sendArenaMessage(sender + " has sent a nuke in the direction of the flagroom! Impact is imminent!",17);
        final TimerTask timerFire = new TimerTask() {
            public void run() {
            	//for(int i=0; i<2; i++) { // Number of waves
	            	for(int j=0; j<7; j++) {
		            	m_botAction.getShip().move((482+(j*10))*16+8, 100*16);
		            	m_botAction.getShip().sendPositionPacket();
		            	m_botAction.getShip().fire(WeaponFired.WEAPON_THOR);
		            	try { Thread.sleep(50); } catch (InterruptedException e) {}
	            	}
	            	try { Thread.sleep(50); } catch (InterruptedException e) {}
            	//}
            }
        };
    	timerFire.run();
    	
    	TimerTask timer = new TimerTask() {
            public void run() {
            	m_botAction.specWithoutLock(m_botAction.getBotName());
            }
        };
        m_botAction.scheduleTask(timer, 13000);
    	
    }

    
}
