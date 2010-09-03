package twcore.bots.multibot.chaos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.StringBag;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.util.Tools;

/**
 * The old zombies module by harvey yau; modded by dugwyler and others
 * Implemented items, money and experience with other features as well!
 *      Original zombies style with RPG features
 *      
 * Updates:
 * August 30, 2010 - Removed tips and updates.
 *                 - Added the command list before game starts.
 *                 TODO instead of a limit to buy, have a timer!
 * August 26, 2010 - Added an auto-warp command. (warps players to base)
 *                 - Displays story before game starts. (pre-game)
 * August 25, 2010 - Added tips command and updates command.
 */

public class chaos extends MultiModule {

    public void init() {
        killmsgs = new StringBag();
        killmsgs.add( "was consumed by the overwhelming darkness!" );
        setup();
    }

    public void requestEvents(ModuleEventRequester events)  {
        events.request(this, EventRequester.PLAYER_DEATH);
        events.request(this, EventRequester.PLAYER_ENTERED);
        events.request(this, EventRequester.PLAYER_POSITION);
        events.request(this, EventRequester.PLAYER_LEFT);
        events.request(this, EventRequester.FREQUENCY_CHANGE);
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
    }

    HashSet <Integer>m_srcship = new HashSet<Integer>();
    BotSettings m_botSettings = moduleSettings;
    int m_humanfreq;
    int m_killerShip;
    int m_zombiefreq;
    int m_zombieship;
    int m_lives;
    StringBag killmsgs;
    boolean isRunning = false;
    boolean isStarting = false;
    boolean modeSet = false;
    boolean isAuto = false;
    
    final int ITEM_THOR1 = 1;
    final int ITEM_ATTACH = 2;
    final int ITEM_MULTI = 3;       
    final int ITEM_THOR2 = 4;
    final int ITEM_BRICK = 5;
    final int ITEM_NOATTACH = 6;
    final int SPECIAL_FURY = 7;

    private int BONUS_RANGE = 60;
    
    ItemDrops itemdrop = new ItemDrops();
    ItemDatabase itemdb = new ItemDatabase();
    
    private HashMap<String, PlayerDatabase> player;
    private HashMap<Integer, ItemDatabase> items;
    private HashMap<Integer, ItemDrops> itemdrops;
    private HashMap<String, Long> entryTimes;
    
    public void setMode( int srcfreq, int srcship, int destfreq, int destship, int lives ){
        m_humanfreq = srcfreq;
        m_srcship.add(new Integer(srcship));
        m_zombiefreq = destfreq;
        m_zombieship = destship;
        m_lives = lives;
        modeSet = true;
    }

    public void deleteKillMessage( String name, int index ){

        ArrayList<String> list = killmsgs.getList();

        if( !( 1 <= index && index <= list.size() )){
            m_botAction.sendPrivateMessage( name, "Error: Can't find the index" );
            return;
        }

        index--;

        if( list.size() > 1 )
            m_botAction.sendPrivateMessage( name, "Removed: " + (String)list.remove( index ));
        else {
            m_botAction.sendPrivateMessage( name, "Sorry, but there must be at least one kill message loaded at all times." );
        }
    }

    public void listKillMessages( String name ){
        m_botAction.sendPrivateMessage( name, "The following messages are in my posession: " );
        ArrayList<String> list = killmsgs.getList();
        for( int i = 0; i < list.size(); i++ ){
            if( ((String)list.get( i )).startsWith( "'" )){
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name>" + (String)list.get( i ));
            } else {
                m_botAction.sendPrivateMessage( name, i + 1 + ". " + "<name> " + (String)list.get( i ));
            }
        }
    }

    public void handleEvent( Message event ){
        int messageType = event.getMessageType();
        String message = event.getMessage().toLowerCase().trim();
        String name = m_botAction.getPlayerName(event.getPlayerID());
        
        if(messageType == Message.PRIVATE_MESSAGE)
            handlePublicCommand(name, message);
        if(opList.isER(name))
            if(messageType == Message.PRIVATE_MESSAGE)
                handleModCommand(name, message);
    }
    
    public void handleEvent( PlayerPosition event ){
        String name = m_botAction.getPlayerName(event.getPlayerID());
        if(name == null)
            return;
        Player sender = m_botAction.getPlayer(name);
        PlayerDatabase p = this.player.get(name);

        if(!player.containsKey(name) && isRunning)
            player.put( name, new PlayerDatabase(name) );
        
        try {
            if(p.isSafePlayer())
                p.addSafePlayer(false);
            
            if(safeSpec(name) && !p.isSafePlayer() && event.isInSafe() && isRunning && sender.getFrequency() == m_humanfreq) {
                if(p.getExperience() >= 200) {
                    m_botAction.sendUnfilteredPrivateMessage(name, "*objon 3");
                    m_botAction.sendArenaMessage(name+" has survived! [$"+p.getMoney()+"] ["+p.getExperience()+" EXP]", 14);
                    m_botAction.specWithoutLock(name);
                    m_botAction.setFreq(name, 1337);
                } else if(p.getExperience() < 200) {
                    if(p.safeAttempt() < 2) {
                        m_botAction.specificPrize(name, 7);
                        m_botAction.specificPrize(name, 13);
                        if(p.safeAttempt() == 0)
                            m_botAction.sendSmartPrivateMessage(name, "You dare to escape?! Come back again when you have fought enough.", 17);
                        else if(p.safeAttempt() == 1) {
                            m_botAction.sendSmartPrivateMessage(name, "Come back one more time and I will kill you myself!", 17);
                        }
                    } else if(p.safeAttempt() == 2) {
                        m_botAction.sendUnfilteredPrivateMessage(name, "*objon 2");
                        m_botAction.warpTo(name, 109, 322);
                        m_botAction.specificPrize(name, 14);
                        m_botAction.getShip().setShip(0);
                        m_botAction.getShip().setFreq(m_zombiefreq);
                        try { Thread.sleep(400); } catch (InterruptedException e) {}
                        m_botAction.getShip().move(109*16+8, 329*16+8);
                        m_botAction.getShip().sendPositionPacket();
                        for(int j = 0; j < 5; j++) {
                            m_botAction.getShip().fire(1);
                            try { Thread.sleep(50); } catch (InterruptedException e) {}
                        }
                        m_botAction.sendArenaMessage(name+" died alone in the deepest depths of hell.", 13);
                        try { Thread.sleep(300); } catch (InterruptedException e) {}
                        m_botAction.setShip( name, m_zombieship );
                        m_botAction.setFreq( name, m_zombiefreq );
                        
                        m_botAction.getShip().setShip(8);
                    }
                }
                p.addSafePlayer(true);
            }
            if(event.containsEnergy() && p.isDead()) {
                for( int i = 0; i < p.getPlayerItemSize(); i++ ) {
                    int[] prize = p.prizePlayerItems();
                    ItemDrops itemdrop2 = this.itemdrops.get(prize[i]);
                    if(prize[i] == 2)
                        i++;
                    m_botAction.specificPrize(p.getPlayerName(), itemdrop2.getDropId());
                }
                p.playerDied(false);
            }
            
        } catch(Exception e) {}
    }
    
    public void handleEvent(PlayerLeft event)
    {
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        if(playerName == null)
            return;
        PlayerDatabase p = this.player.get(playerName);
        ItemDatabase item = this.items.get(SPECIAL_FURY);
        try {
            if(isRunning) {
                if(p.hasAOE())
                    item.hasBeenBought(false);
                player.remove(playerName);
            }
        } catch(Exception e) {}
    }
    
    public void handleEvent(FrequencyChange event)
    {
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        if(playerName == null)
            return;
        PlayerDatabase p = this.player.get(playerName);
        ItemDatabase item = this.items.get(SPECIAL_FURY);
        try {
            if(isRunning) {
                if(p.hasAOE())
                    item.hasBeenBought(false);
                player.remove(playerName);
            }
        } catch(Exception e) {}
    }
    
    public void handleEvent(FrequencyShipChange event)
    {
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        if(playerName == null)
            return;
        PlayerDatabase p = this.player.get(playerName);
        ItemDatabase item = this.items.get(SPECIAL_FURY);
        try {
            if(isRunning) {
                if(p.hasAOE())
                    item.hasBeenBought(false);
                player.remove(playerName);
            }
        } catch(Exception e) {}
    }
    
    public int getInteger( String input ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return 1;
        }
    }
    
    public void setup() {
        m_botAction.sendUnfilteredPublicMessage( "?set Warbird:TurretLimit:0" );
        
        player = new HashMap<String, PlayerDatabase>();
        items = new HashMap<Integer, ItemDatabase>();
        itemdrops = new HashMap<Integer, ItemDrops>();
        entryTimes = new HashMap<String, Long>();
        
        setupItemDrops();
        setupBuyItems();
    }
    
    public void stop() {
        m_botAction.sendUnfilteredPublicMessage( "?set Warbird:TurretLimit:0" );
        player.clear();
        items.clear();
    }

    public void start( String name, String[] params ){
        try{
            if( params.length == 5 ){
                int srcfreq = Integer.parseInt(params[0]);
                int srcship = Integer.parseInt(params[1]);
                int destfreq = Integer.parseInt(params[2]);
                int destship = Integer.parseInt(params[3]);
                int lives = Integer.parseInt(params[4]);
                setMode( srcfreq, srcship, destfreq, destship, lives );
                isRunning = true;
                modeSet = true;                
            } else if( params.length == 1 ){
                int lives = Integer.parseInt(params[0]);
                setMode( 0, 1, 2, 3, lives );
                isRunning = true;
                modeSet = true;
            }
        } catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Sorry, you made a mistake; please try again." );
            isRunning = false;
            modeSet = false;
        }
    }

    public void handleModCommand( String name, String message ){
        try {
            if( message.equals( "!list" )){
                listKillMessages( name );
            } else if( message.startsWith( "!add " )){
                addKillMessage( name, message.substring( 5 ));
            } else if( message.equals( "!stop" )){
                if(isStarting) {
                    m_botAction.sendPrivateMessage( name, "This game is starting soon..." );
                    return;
                } else if(!isRunning) {
                    m_botAction.sendPrivateMessage( name, "The game has not started yet." );
                    return;
                }
                m_botAction.sendPrivateMessage( name, "Chaos mode stopped" );
                isRunning = false;
                stop();
            } else if( message.startsWith( "!start " )){
                if(isStarting) {
                    m_botAction.sendPrivateMessage( name, "This game is starting soon..." );
                    return;
                } else if(isRunning) {
                    m_botAction.sendPrivateMessage( name, "This game has already started!" );
                    return;
                }
                String[] parameters = Tools.stringChopper( message.substring( 7 ), ' ' );
                start( name, parameters );
                doPreGame();
            } else if( message.equals( "!start" ) ){
                if(isStarting) {
                    m_botAction.sendPrivateMessage( name, "This game is starting soon..." );
                    return;
                } else if(isRunning) {
                    m_botAction.sendPrivateMessage( name, "This game has already started!" );
                    return;
                }
                setMode( 0, 1, 2, 3, 1 );
                doPreGame();
            } else if( message.startsWith( "!del " )) {
                deleteKillMessage( name, getInteger( message.substring( 5 )));
            } else if( message.startsWith( "!showinfo ") ) {
                getPlayerInfo(name, message.substring(10));
            } else if( message.equals("!destroy") ) {
                doDestroy(name);
            } else if( message.equals( "!autowarp" ) ) {
                handleAutoWarpCmd(name);
            }
        } catch(Exception e) {}
    }
    
    public void doPreGame() {
        isStarting = true;
        m_botAction.arenaMessageSpam(displayIntro());
        m_botAction.sendArenaMessage("-- This event will begin in 15 seconds...", 2);

        TimerTask timer = new TimerTask() {
            public void run() {
                isRunning = true;
                modeSet = true;
                isStarting = false;
                m_botAction.scoreResetAll();
                m_botAction.shipResetAll();
                m_botAction.sendArenaMessage("-- GOGOGOGO!! Our world depends on you!", 104);
                setup();
                
                if(isAuto) {
                    if(m_botAction.getFrequencySize(m_humanfreq) > 0)
                        m_botAction.warpFreqToLocation(m_humanfreq, 779, 553);
                    if(m_botAction.getFrequencySize(m_zombiefreq) > 0)
                        m_botAction.warpFreqToLocation(m_zombiefreq, 195, 407);
                }
            }
        }; m_botAction.scheduleTask(timer, 15000);
    }
    
    public void handlePublicCommand( String name, String message ){
        try {
            if( message.equals( "!help" )) {
                m_botAction.privateMessageSpam(name, displayHelp());
            } else if( message.equals( "!iteminfo" )) {
                m_botAction.privateMessageSpam(name, displayItemInfo());
            } else if( message.equals( "!buy" )){
                m_botAction.privateMessageSpam(name, displayBuy());
            } else if( message.equals( "!rules" )) {
                String rules = "Angels have a certain time period to survive! They must organize themselves"+
                               " in battle to form the best strategy of defense. The key is to fight and"+
                               " survive! Once the timer is over, Angels must head to the safes with atleast"+
                               " 200 experience!";
                m_botAction.sendSmartPrivateMessage(name, rules);
            } else if( message.equals( "!myinfo" )) {
                PlayerDatabase sender = this.player.get(name);
                m_botAction.sendSmartPrivateMessage(name, "You have $"+sender.getMoney()+" and "+sender.getExperience()+" experience.");
            } else if( message.equals( "!aoe" ) ) {
                doAttack(name);
            } else if( message.startsWith("!buy ") ) {
                handleBuyCommand(name, message.substring(5));
            } else if( message.startsWith( "!give " )) {
                handleDonateCommand(name, message);
            }
        } catch(Exception e) {}  
    }
    
    public void addKillMessage( String name, String killMessage ){
        killmsgs.add( killMessage );
        m_botAction.sendPrivateMessage( name, "<name> " + killMessage + " Added" );
    }

    public void handleEvent( PlayerDeath event ){
        int humanwins, zombiewins;
        if( modeSet && isRunning ){
            Player p = m_botAction.getPlayer( event.getKilleeID() );
            Player p2 = m_botAction.getPlayer( event.getKillerID() );
            if( p == null || p2 == null )
                return;
            try {
                if(p.getFrequency() == m_humanfreq && p2.getFrequency() != m_humanfreq){
                    humanwins = p.getWins()/16;
                    zombiewins = p2.getWins()/16;

                    PlayerDatabase killer = this.player.get(p2.getPlayerName());
                    PlayerDatabase killee = this.player.get(p.getPlayerName());
                    
                    int expPoints = (int)(Math.random() * 130 + 50 + zombiewins);
                    int deathPoints = (int)(Math.random() * 10 + 5 + humanwins);
                    int moneyPoints = (int)(Math.random() * 160 + 50 + zombiewins);
                    int getItemId = (int)(Math.random() * 2 + 8);
                    
                    killer.gainExperience(expPoints);
                    killer.gainMoney(moneyPoints);
                    
                    m_botAction.sendUnfilteredPrivateMessage(p2.getPlayerName(), "You have earned $"+moneyPoints+" and "+expPoints+" experience.");
                    ItemDrops itemdrop2 = this.itemdrops.get(getItemId);
                    
                    killer.loadPlayerItems(getItemId);
                    m_botAction.specificPrize(p2.getPlayerName(), itemdrop2.getDropId());
                    m_botAction.sendSmartPrivateMessage(p2.getPlayerName(), "You have obtained ["+itemdrop2.getName()+"]");
                    
                    if(!killee.checkPlayerItems()) {
                        for( int i = 0; i < killee.getPlayerItemSize(); i++ ) {
                            int[] prize = killee.prizePlayerItems();
                            ItemDrops itemdrop = this.itemdrops.get(prize[i]);
                            killer.loadPlayerItems(prize[i]);
                            m_botAction.specificPrize(p2.getPlayerName(), itemdrop.getDropId());
                        }
                        
                        m_botAction.sendSmartPrivateMessage(p2.getPlayerName(), "... and "+killee.getPlayerItemSize()+" items from "+killee.getPlayerName()+"!");
                        killee.removePlayerItems();
                    }

                    int moneyLoss = (int)(moneyPoints/8);
                    if(killee.getMoney()-moneyLoss < 0) {
                        killee.setMoney(0);
                        moneyLoss = 0;
                    }
                    if(moneyLoss != 0)
                        killee.loseMoney(moneyLoss);
                    
                    killee.gainExperience(deathPoints);
                    
                    if(killee.getMoney() == 0)
                        m_botAction.sendSmartPrivateMessage(p.getPlayerName(), "You have earned "+deathPoints+" experience.");
                    else if(killee.getMoney() > 0)
                        m_botAction.sendSmartPrivateMessage(p.getPlayerName(), "You have lost $"+moneyLoss+" but earned "+deathPoints+" experience.");
                }
                
                if( p.getLosses() >= m_lives && m_srcship.contains(new Integer(p.getShipType())) && p.getFrequency() == m_humanfreq && p2.getFrequency() != m_humanfreq){
                    m_botAction.setShip( event.getKilleeID(), m_zombieship );
                    m_botAction.setFreq( event.getKilleeID(), m_zombiefreq );
                    
                    m_botAction.scoreReset( event.getKilleeID() );
                    String killmsg = killmsgs.toString();
                    int soundPos = killmsg.indexOf('%');
                    int soundCode = 0;

                    if( soundPos != -1){
                        try{
                            soundCode = Integer.parseInt(killmsg.substring(soundPos + 1));
                        } catch( Exception e ){
                            soundCode = 0;
                        }
                        if(soundCode == 12) {soundCode = 1;} //no naughty sounds
                    }

                    if( killmsg.startsWith( "'" ) == false){
                        killmsg = " " + killmsg;
                    }

                    if( soundCode > 0 ){
                        killmsg = killmsg.substring(0, soundPos + 1);
                        m_botAction.sendArenaMessage( p.getPlayerName() + killmsg + " (" + p.getWins() + ")", soundCode );
                    } else {
                        m_botAction.sendArenaMessage( p.getPlayerName() + killmsg + " (" + p.getWins() + ")");
                    }
                }
            
                if(p.getFrequency() == m_zombiefreq && p2.getFrequency() != m_zombiefreq){
                    humanwins = p.getWins()/16;
                    zombiewins = p2.getWins()/16;

                    PlayerDatabase killer = this.player.get(p2.getPlayerName());
                    PlayerDatabase killee = this.player.get(p.getPlayerName());
                    
                    int expPoints = (int)(Math.random() * 130 + 50 + humanwins);
                    int deathPoints = (int)(Math.random() * 10 + 5 + zombiewins);
                    int moneyPoints = (int)(Math.random() * 160 + 50 + humanwins);
                    int getItemId = (int)(Math.random() * 7 + 1);
                    
                    ItemDrops itemdrop = this.itemdrops.get(getItemId);
                    
                    killer.gainExperience(expPoints);
                    killer.gainMoney(moneyPoints);
                    
                    m_botAction.sendUnfilteredPrivateMessage(p2.getPlayerName(), "You have earned $"+moneyPoints+" and "+expPoints+" experience.");
                    
                    if( itemdrop.createDropRate(humanwins) > 15) {
                        killer.loadPlayerItems(getItemId);
                        m_botAction.specificPrize(p2.getPlayerName(), itemdrop.getDropId());
                        m_botAction.sendSmartPrivateMessage(p2.getPlayerName(), "You have obtained ["+itemdrop.getName()+"]");
                    }
                    else
                        m_botAction.sendSmartPrivateMessage(p2.getPlayerName(), p.getPlayerName()+" dropped nothing.");
                    
                    int moneyLoss = (int)(moneyPoints/8);
                    if(killee.getMoney()-moneyLoss < 0) {
                        killee.setMoney(0);
                        moneyLoss = 0;
                    }
                    if(moneyLoss != 0)
                        killee.loseMoney(moneyLoss);
                    
                    killee.gainExperience(deathPoints);
                    
                    if(killee.getMoney() == 0)
                        m_botAction.sendSmartPrivateMessage(p.getPlayerName(), "You have earned "+deathPoints+" experience.");
                    else if(killee.getMoney() > 0)
                        m_botAction.sendSmartPrivateMessage(p.getPlayerName(), "You have lost $"+moneyLoss+" but earned "+deathPoints+" experience.");
                    
                    Iterator<Player> it = m_botAction.getFreqPlayerIterator(m_humanfreq);
                    while(it.hasNext()) {
                        Player team = it.next();
                        if(team.getPlayerName().equals(killer.getPlayerName()))
                            if(it.hasNext())
                                team = it.next();
                            else break;
                        
                        double distance = this.getDistance(p2, team);
                        int freqSize = m_botAction.getFrequencySize(m_humanfreq);
                        
                        if(distance < BONUS_RANGE)
                            this.giveBonus(killer.getPlayerName(), team.getPlayerName(), moneyPoints/freqSize);
                    }

                    killee.playerDied(true);
                }
                
            } catch (Exception e) { }
        }
    }
    
    public void setupBuyItems() {
        items.put(1, new ItemDatabase("Blood of Adonis", 300, 375, false));
        items.put(2, new ItemDatabase("Selini's Direction", 200, 250, false));
        items.put(3, new ItemDatabase("Pohja's Strength", 200, 150, false));
        items.put(4, new ItemDatabase("Sephena's Sin", 300, 375, false));
        items.put(5, new ItemDatabase("Demonic Barrier", 20, 0, false));
        items.put(6, new ItemDatabase("Salem's Curse", 200, 550, false));
        items.put(7, new ItemDatabase("Azog's Fury", 100, 0, false));
    }
    
    public void setupItemDrops() {
        itemdrops.put(1, new ItemDrops("Hermes' Breath", 21));
        itemdrops.put(2, new ItemDrops("Hedice's Burst", 27));
        itemdrops.put(3, new ItemDrops("Starlight Lash", 22));
        itemdrops.put(4, new ItemDrops("Aurora's Prediction", 6));
        itemdrops.put(5, new ItemDrops("Camael's Judgement", 20));
        itemdrops.put(6, new ItemDrops("Namila's Soul", 23));
        itemdrops.put(7, new ItemDrops("Shaer's Will", 18));
        itemdrops.put(8, new ItemDrops("Nisrok's Wings", 28));
        itemdrops.put(9, new ItemDrops("Azrael's Blessing", 15));
    }
    
    public void doPrizeItems(String name, int itemId) {
        switch(itemId){
            case ITEM_THOR1:
                m_botAction.sendArenaMessage( "-- Angel's "+name+" has been given the [Blood of Adonis]" );
                m_botAction.sendUnfilteredPrivateMessage( name, "*prize #24" );
                break;
            case ITEM_ATTACH:
                ItemDatabase item = this.items.get(ITEM_ATTACH);
                item.hasBeenBought(true);
                m_botAction.sendUnfilteredPublicMessage( "?set Warbird:TurretLimit:30" );
                m_botAction.sendArenaMessage( "-- Angel's [Selini's Direction] skill has been permitted by "+name+". Angels may now attach to friendly players!" );
                break;
            case ITEM_MULTI:
                m_botAction.sendArenaMessage( "-- Angel's "+name+" was blessed with [Pohja's Strength]" );
                m_botAction.sendUnfilteredPrivateMessage( name, "*prize #15" );
                m_botAction.sendUnfilteredPrivateMessage( name, "*prize #17" );
                break;
            case ITEM_THOR2:
                m_botAction.sendArenaMessage( "-- Devil's "+name+" has been devoured by [Sephena's Sin]" );
                m_botAction.sendUnfilteredPrivateMessage( name, "*prize #24" );
                break;
            case ITEM_BRICK:
                m_botAction.sendArenaMessage( "-- Devil's "+name+" has learned the skill [Demonic Barrier]" );
                m_botAction.sendUnfilteredPrivateMessage( name, "*prize #26" );
                break;
            case ITEM_NOATTACH:
                ItemDatabase item1 = this.items.get(ITEM_NOATTACH);
                item1.hasBeenBought(true);
                m_botAction.sendUnfilteredPublicMessage( "?set Warbird:TurretLimit:0" );
                m_botAction.sendArenaMessage( "-- Devil's "+name+" has activated [Salem's Curse]. Angels are unable to attach!" );
                break;
            case SPECIAL_FURY:
                PlayerDatabase sender = this.player.get(name);
                ItemDatabase item2 = this.items.get(SPECIAL_FURY);
                sender.activateAOE(true);
                item2.hasBeenBought(true);
                m_botAction.sendSmartPrivateMessage( name, "Private message me with !aoe to use this attack! (expires in 30 seconds)");
                m_botAction.sendArenaMessage( "I sense a great power approaching.." );
                break;
            default:
                m_botAction.sendSmartPrivateMessage(name, "Item does not exist. Please try again.");
        }
    }
    
    public void giveBonus(String killer, String player, int moneyBonus) {
        PlayerDatabase teammate = this.player.get(player);
        
        teammate.gainMoney(moneyBonus);
        
        m_botAction.sendSmartPrivateMessage(player, "[Bonus] You have earned $"+Integer.toString(moneyBonus)+" from "+killer+".");
    }
    
    public double getDistance(Player killer, Player teammate) {
        return Math.sqrt(Math.pow(killer.getXLocation()-teammate.getXLocation(),2) + 
                Math.pow(killer.getYLocation()-teammate.getYLocation(),2))/16;
    }

    public boolean safeSpec(String name) {
        boolean delayExceeded = true;
        int delaySeconds = 1;
        
        if(delaySeconds > 0) {
            long currentTime = System.currentTimeMillis();
            if(entryTimes.containsKey(name)) {
                long entryTime = entryTimes.get(name);
                int delta = (int)(currentTime - entryTime);
                delta /= 1000;
                if(delta < delaySeconds)
                    delayExceeded = false;
                else
                    entryTimes.remove(name);
            }
            else {
                entryTimes.put(name, currentTime);
                delayExceeded = false;
            }
        }
        
        return delayExceeded;
    }
    
    public void doAttack( String name ) {
        PlayerDatabase sender = this.player.get(name);
        if(!sender.hasAOE())
            return;
        Player p = m_botAction.getPlayer(name);
        m_botAction.getShip().setShip(0);
        m_botAction.getShip().setFreq(m_humanfreq);
        m_botAction.getShip().moveAndFire(p.getXLocation(), p.getYLocation(), WeaponFired.WEAPON_BURST);
        m_botAction.getShip().sendPositionPacket();
        m_botAction.sendArenaMessage(name+" has released a devasting attack!");
        for(int deg=-90; deg<280; deg+=10) {
            m_botAction.getShip().rotateDegrees(deg);
            for(int j=0; j<5; j++) {
                m_botAction.getShip().fire(1);
                try { Thread.sleep(10); } catch (InterruptedException e) {}
            }
            m_botAction.getShip().fire(WeaponFired.WEAPON_THOR);
        }
        
        TimerTask timer = new TimerTask() {
            public void run() {
                m_botAction.getShip().setShip(8);
                m_botAction.scoreReset(m_botAction.getBotName()); 
            }
        }; m_botAction.scheduleTask(timer, 700);  
        
        sender.activateAOE(false);
        ItemDatabase item = this.items.get(SPECIAL_FURY);
        item.hasBeenBought(false);
    }
    
    public void doDestroy( String name ) {
        m_botAction.getShip().setShip(0);
        m_botAction.getShip().setFreq(0);
        
        for(Iterator<Integer> i = m_botAction.getFreqIDIterator(m_zombiefreq); i.hasNext(); ) {
            int playerID = i.next();
            m_botAction.sendUnfilteredPrivateMessage(playerID, "*objon 2");
            m_botAction.warpTo(playerID, 109, 322);
            m_botAction.specificPrize(playerID, 14);
            try { Thread.sleep(400); } catch (InterruptedException e) {}
            m_botAction.getShip().move(109*16+8, 329*16+8);
            m_botAction.getShip().sendPositionPacket();
            for(int j = 0; j < 5; j++) {
                m_botAction.getShip().fire(1);
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
            try { Thread.sleep(260); } catch (InterruptedException e) {}
        }
        
        m_botAction.getShip().setShip(8);
        m_botAction.sendSmartPrivateMessage(name, "Annihilation complete.");
    }
    
    public void getPlayerInfo( String name, String entry ) {
        String fuzzy = m_botAction.getFuzzyPlayerName(entry);
        Set<String> players = new HashSet<String>();
        String members = null;
        boolean isValid = false;
        
        players = player.keySet();
        Object[] objects = players.toArray();

        for(int i = 0; i < player.size(); i++) {
            members = objects[i].toString();
            if( members.equalsIgnoreCase(fuzzy) ) {
                isValid = true;
                break;
            }
        }
        
        if(!isValid) {
            m_botAction.sendSmartPrivateMessage(name, "Specified player is not in either party!");
            return;
        }
        
        PlayerDatabase sender = this.player.get(members);
        m_botAction.sendSmartPrivateMessage(name, sender.getPlayerName()+" has $"+sender.getMoney()+" and "+sender.getExperience()+" experience.");
    }
    
    public void handleAutoWarpCmd( String name ) {
        if(isRunning) {
            m_botAction.sendSmartPrivateMessage(name, "This game has already started!");
            return;
        }
        
        isAuto = isAuto ? false : true;
        String status = isAuto ? "enabled" : "disabled";
        m_botAction.sendSmartPrivateMessage(name, "Auto-warp is "+status+".");
    }
    
    public void handleBuyCommand( String name, String entry ) {
        Player p = m_botAction.getPlayer(name);
        int id = itemdb.alternateNames(entry);
        if(p.getFrequency() == m_humanfreq) {
            PlayerDatabase sender = this.player.get(name);
            ItemDatabase item = this.items.get(id);
            if(item.getExp() <= sender.getExperience() && item.getPrice() <= sender.getMoney() ) {
                if(id > 0 && id < 4 && !item.isBoughtOnce()) {
                    doPrizeItems(name, id);
                    sender.loseMoney(item.getPrice());
                } else if(id == 7) {
                    if(item.isBoughtOnce()) {
                        m_botAction.sendSmartPrivateMessage( name, "[Azog's Fury] has already been bought. Please wait after it has been used.");
                        return;
                    }
                    doPrizeItems(name, id);
                    sender.loseMoney(item.getPrice());
                } else if(item.isBoughtOnce())
                    m_botAction.sendSmartPrivateMessage(name, "There is not enough power to activate this item!");
                  else if(id > 3 && id < 7)
                    m_botAction.sendSmartPrivateMessage(name, "Request denied; item belongs to the devils.");
            }
            else if(item.getExp() > sender.getExperience() && item.getPrice() > sender.getMoney())
                m_botAction.sendSmartPrivateMessage(name, "You do not have enough money and experience.");
            else if(item.getExp() > sender.getExperience())
                m_botAction.sendSmartPrivateMessage(name, "You do not have enough experience.");
            else if(item.getPrice() > sender.getMoney())
                m_botAction.sendSmartPrivateMessage(name, "You do not have enough money.");
        }
        else if(p.getFrequency() == m_zombiefreq) {
            PlayerDatabase sender = this.player.get(name);
            ItemDatabase item = this.items.get(id);
            ItemDatabase item2 = this.items.get(ITEM_ATTACH);
            if(item.getExp() <= sender.getExperience() && item.getPrice() <= sender.getMoney() ) {
                if(id == 6 && !item2.isBoughtOnce()) {
                    m_botAction.sendSmartPrivateMessage(name, "["+item2.getName()+"] has not yet been activated!");
                } else if(id > 3 && id < 7 && !item.isBoughtOnce()) {
                    doPrizeItems(name, id);
                    sender.loseMoney(item.getPrice());
                } else if(item.isBoughtOnce())
                    m_botAction.sendSmartPrivateMessage(name, "There is not enough power to activate this item!");
                  else if(id > 0 && id < 4)
                    m_botAction.sendSmartPrivateMessage(name, "Request denied; item belongs to the angels.");
            }
            else if(item.getExp() > sender.getExperience() && item.getPrice() > sender.getMoney())
                m_botAction.sendSmartPrivateMessage(name, "You do not have enough money and experience.");
            else if(item.getExp() > sender.getExperience())
                m_botAction.sendSmartPrivateMessage(name, "You do not have enough experience.");
            else if(item.getPrice() > sender.getMoney())
                m_botAction.sendSmartPrivateMessage(name, "You do not have enough money.");
        } else
            m_botAction.sendSmartPrivateMessage(name, "You must be in a party to use this feature.");
    }
    
    public void handleDonateCommand( String name, String message ) {
        PlayerDatabase sender = this.player.get(name);
        Player p = m_botAction.getPlayer( name );
        String _playerName = message.substring(6, message.indexOf(":"));
        String playerName = m_botAction.getFuzzyPlayerName(_playerName);
        String sendee = "", money = null, mail = null;
        boolean isValid = false;
        
        try {
            money = message.substring(message.indexOf(":")+1, message.indexOf(" ", message.indexOf(":")));
        } catch(Exception e) {
            money = message.substring(message.indexOf(":")+1);
        }

        for(Iterator<Player> i = m_botAction.getFreqPlayerIterator(p.getFrequency()); i.hasNext();) {
            Player member = i.next();
            sendee = member.getPlayerName();
            if(member.getPlayerID() == m_botAction.getPlayerID(playerName)) {
                isValid = true;
                break;
            }
        }
        
        if(!isValid) {
            m_botAction.sendSmartPrivateMessage(name, "Could not send money. Specified player is not in your party.");
            return;
        }
        
        // Checks if there is a message that the sender had wrote
        try {
            mail = message.substring(message.indexOf(" ", message.indexOf(":")));
        } catch(Exception e) {
            mail = null;
        }
        
        if(sender.getPlayerName().equalsIgnoreCase(playerName)) {
            m_botAction.sendSmartPrivateMessage(name, "You greedy little..!");
            return;
        }
        
        // They must have atleast $0 in account if they are giving away their money
        int moneyLeft = sender.getMoney()-Integer.parseInt(money);
        if(moneyLeft < 0) {
            m_botAction.sendSmartPrivateMessage(name, "Sorry, you don't have enough money.");
            return;
        }
        
        sender.loseMoney(Integer.parseInt(money));
        PlayerDatabase receiver = this.player.get(sendee);
        receiver.gainMoney(Integer.parseInt(money));
        
        m_botAction.sendSmartPrivateMessage(sender.getPlayerName(), "You sent $"+money+" to "+receiver.getPlayerName()+".");
        m_botAction.sendSmartPrivateMessage(receiver.getPlayerName(), name+" has donated $"+money+" to your account.");
        // Displays line: '<sender> says: ' if the sender has written a message to the receiver
        if(mail != null)
            m_botAction.sendSmartPrivateMessage(receiver.getPlayerName(), sender.getPlayerName()+" says:"+mail);
    }
    
    public String[] getModHelpMessage() {
        String[] help = {
            ",---------------------------------+-----------------------------------------------------.",
            "|                                                     Description                       |",
            "|                          ,------+-----------------------------------------------------+",
            "| Moderator Commands      /       |             Welcome, how may I help you?            |",
            "+------------------------'        |                                                     |",
            "|  !list                          |   - Lists the currently loaded kill messages.       |",
            "|  !add <kill message>            |   - Adds a kill message. %%<num> to add a sound.    |",
            "|  !del <index>                   |   - Deletes a kill message. Index taken from !list  |",
            "|  !stop                          |   - Shuts down zombies mode.                        |",
            "|  !start                         |   - Starts a standard zombies mode.                 |",
            "|  !autowarp                      |   - Warps players to the starting points. [on/off]  |",
            "|  !start <humanfreq> <humanship> <zombfreq> <zombship> <lives>                         |",
            "|  !showinfo <player>             |   - Shows the player's money and experience points. |",
            "|  !destroy                       |   - Assassinates everyone in zombie freq!           |",
            "+---------------------------------+-----------------------------------------------------+",
            "|                          ,------+-----------------------------------------------------+",
            "| Player Commands         /       |          Is there anything I can do for you?        |",
            "+------------------------'        |                                                     |",
            "|  !rules                         |   - Need help? Private message me with !rules       |",
            "|  !myinfo                        |   - Shows your money and experience points.         |",
            "|  !give <player>:<$> <message>   |   - Donates money to desired player.                |",
            "|  !iteminfo                      |   - Don't know what item you got? Check here!       |",
            "|  !buy                           |   - Want to spend your money? I'm your best bet!    |",
            "|  !buy <itemname>                |   - Prizes item; first word of item is acceptable.  |",
            "`---------------------------------+-----------------------------------------------------'"
        };
        return help;
    }
    
    public String[] displayHelp() {
        String[] help = {
                ",---------------------------------+-----------------------------------------------------.",
                "|                                                      Description                      |",
                "|                          ,------+-----------------------------------------------------+",
                "| Player Commands         /       |             Welcome, how may I help you?            |",
                "+------------------------'        |                                                     |",
                "|  !rules                         |   - Need help? Private message me with !rules       |",
                "|  !myinfo                        |   - Shows your money and experience points.         |",
                "|  !give <player>:<$> <message>   |   - Donates money to desired player.                |",
                "|  !iteminfo                      |   - Don't know what item you got? Check here!       |",
                "|  !buy                           |   - Want to spend your money? I'm your best bet!    |",
                "|  !buy <itemname>                |   - Prizes item; first word of item is acceptable.  |",
                "`---------------------------------+-----------------------------------------------------'"
        };
        return help;
    }
    
    public String[] displayBuy() {
        String[] list = {
                ",-----------------------------+----+---------+-------+------------------------------------------.",
                "|                               ID    Price     Exp                    Description              |",
                "|                          ,--+----+---------+-------+------------------------------------------+",
                "| Angel Party             /   |    |         |       |    Please.. help us escape this Hell!    |",
                "+------------------------'    |    |         |       |                                          |",
                "|  Blood of Adonis            | 01 |   $300  |  375  | - Destroys everything around it. [slow]  |",
                "|  Selini's Direction         | 02 |   $200  |  250  | - Enables friendly attach ability. [1x]  |",
                "|  Pohja's Strength           | 03 |   $200  |  150  | - What's this?!                          |",
                "+-----------------------------+----+---------+-------+------------------------------------------+",
                "|                          ,--+----+---------+-------+------------------------------------------+",
                "| Devil Party             /   |    |         |       |    Join us, we will take over Heaven!    |",
                "+------------------------'    |    |         |       |                                          |",
                "|  Sephena's Sin              | 04 |   $300  |  375  | - Destroys everything around it. [fast]  |",
                "|  Demonic Barrier            | 05 |    $20  |    0  | - Blocks anything in its way!            |",
                "|  Salem's Curse              | 06 |   $200  |  550  | - Disables enemy's attach ability. [1x]  |",
                "`-----------------------------+----+---------+-------+------------------------------------------'"
        };
        return list;
    }
    
    public String[] displayItemInfo() {
        String[] list = {
                ",---------------------------------+------------+----------------------------------------------.",
                "|                                      Name                   Description                     |",
                "|                          ,------+------------+----------------------------------------------+",
                "| Angel Party             /       |            |      Could it be.. the legendary items?!     |",
                "+------------------------'        |            |                                              |",
                "|  Hermes' Breath                 |  Repel     | - A breath that causes horrendous winds.     |",
                "|  Hedice's Burst                 |  Rocket    | - The sudden burst of enormous pure energy.  |",
                "|  Starlight Lash                 |  Burst     | - A blinding attack which confuses the foe.  |",
                "|  Aurora's Prediction            |  X-Radar   | - The eye that can predict future events.    |",
                "|  Camael's Judgement             |  Antiwarp  | - Justice will prevail!                      |",
                "|  Namila's Soul                  |  Decoy     | - The ability to separate one's soul.        |",
                "|  Shaer's Will                   |  Shield    | - The powerful will that will never die out! |",
                "+---------------------------------+------------+----------------------------------------------+",
                "|                          ,------+------------+----------------------------------------------+",
                "| Devil Party             /       |            |  At last.. the time has come for our reign.  |",
                "+------------------------'        |            |                                              |",
                "|  Nisrok's Wings                 |  Portal    | - The ability to travel to other dimensions. |",
                "|  Azrael's Blessing              |  Multi     | - A special gift from Azrael!                |",
                "`---------------------------------+------------+----------------------------------------------'"
        };
        return list;
    }
    
    public String[] displayIntro() {
        String[] intro = {
                "-- +- Story -------------------------------------------------------+",
                "-- |   Our world is unbalanced, the darkness grows stronger...     |",
                "-- |   This is the day when the darkness makes their move.         |",
                "-- |   Angels must work together, share their resources, escape    |",
                "-- |   this world controlled by the devils. However, the gates to  |",
                "-- |   the new world will not open until the time is right.        |",
                "-- |   Defend yourselves until the time is right to move on!       |",
                "-- +- Commands ----------------------------------------------------+",
                "-- |   !help    - Shows all the commands you need to know!         |",
                "-- |   !rules   - Need help? Private message me with !rules        |",
                "-- |   !myinfo    - Shows your money and experience points.        |",
                "-- |   !give <player>:<$> <message>  - Donates money to player.    |",
                "-- |   !iteminfo    - Don't know what item you got? Check here!    |",
                "-- |   !buy         - Want to spend your money? I'm your best bet! |",
                "-- |   !buy <item>  - Allows you to purchase an item.              |",
                "-- +- Notice ------------------------------------------------------+",
                "-- |        Private message the Robobot with your commands!        |",
                "-- +---------------------------------------------------------------+"
        };
        return intro;
    }

    public void cancel() {
    }
    
    public boolean isUnloadable()   {
        return true;
    }
}
