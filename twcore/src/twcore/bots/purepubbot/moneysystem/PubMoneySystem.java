package twcore.bots.purepubbot.moneysystem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import twcore.bots.purepubbot.PubException;
import twcore.bots.purepubbot.moneysystem.item.PubCommandItem;
import twcore.bots.purepubbot.moneysystem.item.PubItem;
import twcore.bots.purepubbot.moneysystem.item.PubItemDuration;
import twcore.bots.purepubbot.moneysystem.item.PubItemRestriction;
import twcore.bots.purepubbot.moneysystem.item.PubPrizeItem;
import twcore.bots.purepubbot.moneysystem.item.PubShipItem;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.OperatorList;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.SQLResultEvent;
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
	
	private String DATABASE = "local";
	
	private PubStore store;
	private PubPlayerManager manager;
	private PubLottery pubLottery;
    
    private Map<PubPlayer, PubItemDuration> playersWithDurationItem;
    
    // These variables are used to calculate the money earned for a kill
    private Map<Integer, Integer> shipKillerPoints;
    private Map<Integer, Integer> shipKilledPoints;
    private LinkedHashMap<PubLocation, Integer> locationPoints;
    
    private LvzMoneyPanel lvzPubPointsHandler;
	
    public PubMoneySystem(BotAction botAction) {

    	this.m_botAction = botAction;
    	this.m_botSettings = botAction.getBotSettings();
    	this.opList = m_botAction.getOperatorList();
    	
    	this.lvzPubPointsHandler = new LvzMoneyPanel(m_botAction);

        this.store = new PubStore();
        this.manager = new PubPlayerManager(botAction, m_botSettings.getString("database"));
        this.pubLottery = new PubLottery(m_botAction);
        try {
        	initializeStore();
        } catch (Exception e) {
			Tools.printStackTrace("Error while initializing the store", e);
		}

        this.playersWithDurationItem = new HashMap<PubPlayer, PubItemDuration>();
        
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
	    			PubItemDuration d = new PubItemDuration();
	    			
	    			boolean hasRestriction = false;
	    			boolean hasDuration = false;
	    			
	    			for(int i=optionPointer; i<data.length; i++) {
	    				String option = data[i];
	    				if(option.startsWith("!s")) {
	    					int ship = Integer.parseInt(option.substring(2));
	    					r.addShip(ship);
	    					hasRestriction = true;
	    				} else if(option.startsWith("!mp")) {
	    					int max = Integer.parseInt(option.substring(3));
	    					r.setMaxPerLife(max);
	    					hasRestriction = true;
	    				} else if(option.startsWith("!mc")) {
	    					int max = Integer.parseInt(option.substring(3));
	    					r.setMaxConsecutive(max);
	    					hasRestriction = true;
	    				} else if(option.startsWith("!adm")) {
	    					int max = Integer.parseInt(option.substring(4));
	    					r.setMaxArenaPerMinute(max);
	    					hasRestriction = true;
	    				} else if(option.startsWith("!arena")) {
	    					item.setArenaItem(true);
	    				} else if(option.startsWith("!fromspec")) {
	    					r.buyableFromSpec(true);
	    				} else if(option.startsWith("!dd")) {
	    					int death = Integer.parseInt(option.substring(3));
	    					d.setDeaths(death);
	    					hasDuration = true;
	    				} else if(option.startsWith("!dm")) {
	    					int minutes = Integer.parseInt(option.substring(3));
	    					d.setMinutes(minutes);
	    					hasDuration = true;
	    				} else if(option.startsWith("!abbv")) {
	    					String abbv = option.substring(6);
	    					item.addAbbreviation(abbv);
	    				}
	    			}
	    			
	    			if (hasRestriction)
	    				item.setRestriction(r);
	    			if (hasDuration)
	    				item.setDuration(d);
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
    

    private void buyItem(final String playerName, String itemName, String params, int shipType){
    	
        try{

            if (manager.isPlayerExists(playerName)){
            	
            	Player player = m_botAction.getPlayer(playerName);
            	PubPlayer pubPlayer = manager.getPlayer(playerName);
            	
            	
            	int moneyBeforeBuy = manager.getPlayer(playerName).getMoney();
                PubItem item = store.buy(itemName, pubPlayer, shipType);
                int moneyAfterBuy = pubPlayer.getMoney();

                // PRIZE ITEM
                if (item instanceof PubPrizeItem) {
                	
                	m_botAction.specificPrize(pubPlayer.getPlayerName(), ((PubPrizeItem) item).getPrizeNumber());
               
                }
                
                // COMMAND ITEM
                else if (item instanceof PubCommandItem) {
                	
                	String command = ((PubCommandItem)item).getCommand();
            		Method method = this.getClass().getDeclaredMethod("itemCommand"+command, String.class, String.class);
            		method.invoke(this, playerName, params);
                
                } 
                
                // SHIP ITEM
                else if (item instanceof PubShipItem) {
                	
                    if (item.hasDuration()) {
                    	PubItemDuration duration = item.getDuration();
                    	if (duration.hasTime()) {
                    		final int currentShip = (int)player.getShipType();
                        	TimerTask timer = new TimerTask() {
                                public void run() {
                                	m_botAction.setShip(playerName, currentShip);
                                }
                            };
                            m_botAction.scheduleTask(timer, duration.getSeconds()*1000);
                    	}
                    	else if (duration.hasDeaths()) {
                    		playersWithDurationItem.put(pubPlayer, duration);
                    	}
                    }
                    
                    pubPlayer.setShipItem((PubShipItem)item);
                    m_botAction.setShip(playerName, ((PubShipItem) item).getShipNumber());
                	
                } 
                

                if (item.isArenaItem()) {
                	 m_botAction.sendArenaMessage(playerName + " just bought a " + item.getDisplayName() + " for $" + item.getPrice() + ".",21);
                }
                
                int playerId = m_botAction.getPlayerID(playerName);
                this.lvzPubPointsHandler.update(playerId, String.valueOf(moneyBeforeBuy), String.valueOf(moneyAfterBuy), false);
  
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

    private void sendMoneyToPlayer(String playerName, int amount, String message) {
    	PubPlayer player = manager.getPlayer(playerName);
    	player.addMoney(amount);
        if (message!=null) {
        	m_botAction.sendPrivateMessage(playerName, message);
        }
    }
    
    private void doCmdItems(String sender){
    	
        Player p = m_botAction.getPlayer(sender);
        
        ArrayList<String> lines = new ArrayList<String>();
        
        Class currentClass = this.getClass();
        
        if (!store.isOpened())
        {
        	lines.add("The store is closed, no items available.");
    	} 
        else 
    	{
	        for(PubItem item: store.getItems().values()) {
	        	
	        	if (item instanceof PubPrizeItem) {
	        		if (!currentClass.equals(PubPrizeItem.class))
	        			lines.add("== PRIZES ===========================================================");
	        		currentClass = PubPrizeItem.class;
	        	} else if (item instanceof PubShipItem) {
	        		if (!currentClass.equals(PubShipItem.class))
	        			lines.add("== SHIPS ============================================================");
	        		currentClass = PubShipItem.class;
	        	} else if (item instanceof PubCommandItem) {
	        		if (!currentClass.equals(PubCommandItem.class))
	        			lines.add("== SPECIALS =========================================================");
	        		currentClass = PubCommandItem.class;
	        	}
	        	
	        	String abbv = "";
		        if (item.getAbbreviations().size()>0) {
		        	abbv+="  (";
		        	for(String str: item.getAbbreviations()) {
		        		abbv += "!"+str+",";
		        	}
		        	abbv=abbv.substring(0,abbv.length()-1)+")";
	        	}
	        	
		        String line = Tools.formatString("!buy "+item.getName(), 16);
		        line += Tools.formatString(abbv, 12);
	        	line += Tools.formatString("($"+item.getPrice()+")", 10);
	        	
	        	String info = "";
	        	
	        	if (item.isRestricted()) {
	        		PubItemRestriction r = item.getRestriction();
	        		if (r.getRestrictedShips().size()==0) 
	        			info += "All ships";
	        		else if (r.getRestrictedShips().size()==8) {
		        		info += "None"; // Just in case
	        		} else {
	        			String ships = "Ships:";
	        			for(int i=1; i<9; i++) {
	        				if (!r.getRestrictedShips().contains(i)) {
	        					ships += i+",";
	        				}
	        			}
	        			info += ships.substring(0, ships.length()-1);
	        		}
	        		info = Tools.formatString(info, 16);
	        		if (r.getMaxPerLife()!=-1) {
	        			info += r.getMaxPerLife()+" per life. ";
	        		}
	        		if (r.getMaxArenaPerMinute()!=-1) {
	        			info += "1 every "+r.getMaxArenaPerMinute()+" minutes. ";
	        		}
	        		
	        	}
	        	
	        	if (item.hasDuration()) {
	        		PubItemDuration d = item.getDuration();
	        		if (d.getDeaths()!=-1 && d.getSeconds()!=-1) {
	        			info += "Last "+d.getDeaths()+" life(s) or "+(int)(d.getSeconds()/60)+" minute(s). ";
	        		}
	        		else if (d.getDeaths()!=-1) {
	        			info += "Last "+d.getDeaths()+" life(s). ";
	        		}
	        		else if (d.getSeconds()!=-1) {
	        			info += "Last "+(int)(d.getSeconds()/60)+" minute(s). ";
	        		}
	        		
	        	}
	        	
	        	lines.add(line+info);
	        }
	    } 

        m_botAction.smartPrivateMessageSpam(sender, lines.toArray(new String[lines.size()]));
        
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
        if(manager.isPlayerExists(sender)){
            PubPlayer pubPlayer = manager.getPlayer(sender);
            m_botAction.sendPrivateMessage(sender, "You have $"+pubPlayer.getMoney() + " in your bank.");
        }else
            m_botAction.sendPrivateMessage(sender, "You're still not in the system. Wait a bit to be added.");
    }
    
    public boolean isStoreOpened() {
    	return store.isOpened();
    }
    
    public boolean isDatabaseOn() {
    	return m_botSettings.getString("database") != null;
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

    	final Player killer = m_botAction.getPlayer( event.getKillerID() );
        final Player killed = m_botAction.getPlayer( event.getKilleeID() );
       
        if( killer == null || killed == null )
            return;

        try{

            final PubPlayer pubPlayerKilled = manager.getPlayer(killed.getPlayerName());
            pubPlayerKilled.handleDeath(event);
            
            // Duration check for Ship Item
            if (pubPlayerKilled.hasShipItem() && playersWithDurationItem.containsKey(pubPlayerKilled)) {
            	final PubItemDuration duration = playersWithDurationItem.get(pubPlayerKilled);
            	if (duration.getDeaths() <= pubPlayerKilled.getDeathsOnShipItem()) {
            		// Let the player wait before setShip().. (the 4 seconds after each death)
                	final TimerTask timer = new TimerTask() {
                        public void run() {
                    		pubPlayerKilled.resetShipItem();
                    		m_botAction.setShip(killed.getPlayerName(), 1);
                    		playersWithDurationItem.remove(pubPlayerKilled);
                    		m_botAction.sendPrivateMessage(killed.getPlayerName(), "You lost your ship after " + duration.getDeaths() + " death(s).",22);
                        }
                    };
                    m_botAction.scheduleTask(timer, 4300);
            	}
            	else {
            		// TODO - Give the PubShipItemSettings
            		// i.e: A player buys a special ship with 10 repel (PubShipItemSettings)
            		//      for 5 deaths (PubItemDuration)
            	}
            }
 
            int money = 0;

            // Money from the ship
            money += shipKillerPoints.get((int)killer.getShipType());
            money += shipKilledPoints.get((int)killed.getShipType());
            
            // Money from the location
            Point pointXY = new Point(killer.getXTileLocation(), killer.getYTileLocation());
            for(PubLocation location: locationPoints.keySet()) {
            	if (location.isInside(pointXY)) {
            		money += locationPoints.get(location);
            		break;
            	}
            }

            String playerName = killer.getPlayerName();
            PubPlayer pubPlayer = manager.getPlayer(playerName);
            pubPlayer.addMoney(money);

            Tools.printLog("Added "+money+" to "+playerName+" TOTAL POINTS: "+pubPlayer.getMoney());

        } catch(Exception e){
            Tools.printStackTrace(e);
        }
    	
    }
    
    public void handleEvent(PlayerEntered event) {
    	manager.handleEvent(event);
    }
    
    public void handleEvent(FrequencyShipChange event) {
		manager.handleEvent(event);
    }
    
    public void handleEvent(ArenaJoined event){
    	manager.handleEvent(event);
    }

    public void handlePublicCommand(String sender, String command) {
        try {
            if(command.startsWith("!items") || command.startsWith("!i")) {
                doCmdItems(sender);
            }
            else if(command.startsWith("!rich")) {
            	sendMoneyToPlayer(sender,1000000,"You are rich now!");
            }
            else if(command.equals("!$"))
            {
                doCmdDisplayMoney(sender);
                
            }
            else if(command.startsWith("!buy") || command.startsWith("!b")){
                try{
                    doCmdBuy(sender, command);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            else if(command.startsWith("!lottery ") || command.startsWith("!l ")) {
                pubLottery.handleTicket(sender, command);
            }
            else if(command.equals("!jackpot") || command.equals("!jp")) {
                pubLottery.displayJackpot(sender);
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
            if(command.startsWith("!lprice ")){
                pubLottery.setTicketPrice(sender, command);
            }
        } catch(RuntimeException e) {
            m_botAction.sendSmartPrivateMessage(sender, e.getMessage());
        }
    }
    
    public void handleDisconnect() {
    	manager.handleDisconnect();
    }
    
    public void handleEvent(SQLResultEvent event){
        manager.handleEvent(event);
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
    private void itemCommandSuddenDeath(final String sender, String params) throws PubException{

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
    	//m_botAction.getShip().setFreq(9999);
    	m_botAction.getShip().rotateDegrees(90);
    	m_botAction.getShip().sendPositionPacket();
    	//m_botAction.setThorAdjust(5);
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
