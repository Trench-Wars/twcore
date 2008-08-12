package twcore.bots.multibot.twscript;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.ArrayList;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.events.BallPosition;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.WeaponFired;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.game.Player;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.OperatorList;

/**
 * @author milosh
 */
public class events extends MultiUtil {
    
    public OperatorList opList;
    public static final int SPAWN_TIME = 5005;
    
    //The lists.
    public ArrayList<String> greetMsgs = new ArrayList<String>();
    public ArrayList<String> killMsgs = new ArrayList<String>();
    public ArrayList<String> spawnMsgs = new ArrayList<String>();
    public ArrayList<String> weapMsgs = new ArrayList<String>();
    public ArrayList<String> fClaimMsgs = new ArrayList<String>();
    public ArrayList<String> fDropMsgs = new ArrayList<String>();
    public ArrayList<String> bClaimMsgs = new ArrayList<String>();
    public ArrayList<String> bFiredMsgs = new ArrayList<String>();
    public ArrayList<String> timerMsgs = new ArrayList<String>();
    
    public TreeMap<Byte, Ball> ballMap = new TreeMap<Byte, Ball>();
    
    /**
     * Initializes.
     */
    public void init() {
        opList = m_botAction.getOperatorList();
    }
    
    /**
     * Requests which events this utility watches for.
     */
    public void requestEvents(ModuleEventRequester req) {
        req.request(this, EventRequester.BALL_POSITION);
        req.request(this, EventRequester.PLAYER_DEATH);
        req.request(this, EventRequester.WEAPON_FIRED);
        req.request(this, EventRequester.FLAG_CLAIMED);
        req.request(this, EventRequester.FLAG_DROPPED);
        req.request(this, EventRequester.PLAYER_ENTERED);
    }
    
    /**
     * Required method that returns this utilities help menu.
     */
    public String[] getHelpMessages() {
        String[] message = {
                "+---------------------- Events Utility ----------------------+",
                "| !masspm <msg>     -- Sends a PM to everyone in the arena.  |",
                "| !pub <msg>        -- Sends a public message.               |",
                "| !greetmsg <msg>   -- Adds a greeting message.              |",
                "| !killmsg <msg>    -- Adds a kill message.                  |",
                "| !spawnmsg <msg>   -- Adds a spawn message.                 |",
                "| !weapmsg <msg>    -- Adds a weapon fired message.          |",
                "| !fclaimmsg <msg>  -- Adds a flag claimed message.          |",
                "| !fdropmsg <msg>   -- Adds a flag dropped message.          |",
                "| !bclaimmsg <msg>  -- Adds a ball claimed message.          |",
                "| !bfiredmsg <msg>  -- Adds a ball fired message.            |",
                "| !timermsg <msg>   -- Adds a timer message.                |",
                "| !listmsg          -- Lists all messages.                   |",
                "| !delmsg <#>:<#>   -- Deletes message at index (#, #).      |",
                "| !clearmsg <#>     -- Deletes all messages of type <#>.     |",
                "| !clearallmsg      -- Clears all messages.                  |",
                "| !listkeys         -- Displays all available escape keys.   |",
                "+------------------------------------------------------------+"
        };        
        return message;
    }
    
    /**
     * Handles messaging.
     */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String name = m_botAction.getPlayerName(event.getPlayerID());
        int messageType = event.getMessageType();
        if(messageType == Message.PRIVATE_MESSAGE && opList.isER(name))
            handleCommand(name, message);
        else if(messageType == Message.ARENA_MESSAGE && message.equals("NOTICE: Game over")){
            Player p = m_botAction.getPlayer(event.getPlayerID());
            if(p == null)return;
            Iterator<String> i = timerMsgs.iterator();
            while( i.hasNext() )
                CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);        	
        }
    }
    
    /**
     * Handles commands.
     */
    public void handleCommand(String name, String msg) {
        if(msg.startsWith("!masspm "))
            doMassPm(name, msg.substring(8));
        else if(msg.startsWith("!pub "))
            doPub(name, msg.substring(5));
        else if(msg.startsWith("!greetmsg "))
        	doGreetMsg(name, msg.substring(10));
        else if(msg.startsWith("!killmsg "))
            doKillMsg(name, msg.substring(9));
        else if(msg.startsWith("!spawnmsg "))
            doSpawnMsg(name, msg.substring(10));
        else if(msg.startsWith("!weapmsg "))
            doWeapMsg(name, msg.substring(9));
        else if(msg.startsWith("!fclaimmsg "))
            doFlagClaimedMsg(name, msg.substring(11));
        else if(msg.startsWith("!fdropmsg "))
            doFlagDroppedMsg(name, msg.substring(10));
        else if(msg.startsWith("!bclaimmsg "))
            doBallClaimedMsg(name, msg.substring(11));
        else if(msg.startsWith("!bfiredmsg "))
            doBallFiredMsg(name, msg.substring(11));
        else if(msg.startsWith("!timermsg "))
        	doTimerMsg(name, msg.substring(10));
        else if(msg.equalsIgnoreCase("!listmsg"))
            doListMsg(name);
        else if(msg.startsWith("!delmsg "))
            doDeleteMsg(name, msg.substring(8));
        else if(msg.startsWith("!clearmsg "))
            doClearMsg(name, msg.substring(10));
        else if(msg.equalsIgnoreCase("!clearallmsg"))
            doClearAllMsg(name);
        else if(msg.equalsIgnoreCase("!privatekeys"))
            m_botAction.privateMessageSpam(name, CodeCompiler.getPrivateKeysMessage());
        else if(msg.equalsIgnoreCase("!publickeys"))
            m_botAction.privateMessageSpam(name, CodeCompiler.getPublicKeysMessage());
    }

    /**
     * Sends an unfiltered private message to everyone in the arena.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doMassPm(String name, String msg){
        Iterator<Player> i = m_botAction.getPlayerIterator();
        while( i.hasNext() )
            CodeCompiler.handlePrivateTWScript(m_botAction, msg, i.next(), twscript.isSysop);
    }
    
    /**
     * Sends an unfiltered public message to the arena.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doPub(String name, String message){
        CodeCompiler.handlePublicTWScript(m_botAction, message, twscript.isSysop);
    }
    
    /**
     * Adds a message for arena entrance events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doGreetMsg(String name, String message){
        if(message.length() < 220){
            greetMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Greet message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for kill events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doKillMsg(String name, String message){
        if(message.length() < 220){
            killMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Kill message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for spawn events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doSpawnMsg(String name, String message){
        if(message.length() < 220){
            spawnMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Spawn message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for weapon fired events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doWeapMsg(String name, String message){
        if(message.length() < 220){
            weapMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Weapon fired message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for flag claim events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doFlagClaimedMsg(String name, String message){
        if(message.length() < 220){
            fClaimMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Flag claimed message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for flag drop events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doFlagDroppedMsg(String name, String message){
        if(message.length() < 220){
            fDropMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Flag dropped message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for ball claimed events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doBallClaimedMsg(String name, String message){
        if(message.length() < 220){
            bClaimMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Ball claimed message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for ball fired events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doBallFiredMsg(String name, String message){
        if(message.length() < 220){
            bFiredMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Ball fired message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Adds a message for timer events.
     * @param name - The user of the bot
     * @param message - The message to send
     */
    public void doTimerMsg(String name, String message){
        if(message.length() < 220){
            timerMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Timer message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /**
     * Displays a list of active messages
     * @param name - The user
     */
    public void doListMsg(String name){
        m_botAction.sendSmartPrivateMessage( name, "+---------------------Message List----------------------");
        if(greetMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 0) Greet: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 0) Greet: " + greetMsgs.size());
            int index = 0;
            Iterator<String> i = greetMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }    
        }
        if(killMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 1) Kill: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 1) Kill: " + killMsgs.size());
            int index = 0;
            Iterator<String> i = killMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }    
        }
        if(spawnMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 2) Spawn: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 2) Spawn: " + spawnMsgs.size());
            int index = 0;
            Iterator<String> i = spawnMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            } 
        }
        if(weapMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 3) Weapon Fired: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 3) Weapon Fired: " + weapMsgs.size());
            int index = 0;
            Iterator<String> i = weapMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }
        }
        if(fClaimMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 4) Flag Claimed: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 4) Flag Claimed: " + fClaimMsgs.size());
            int index = 0;
            Iterator<String> i = fClaimMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }
        }
        if(fDropMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 5) Flag Dropped: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 5) Flag Dropped: " + fDropMsgs.size());
            int index = 0;
            Iterator<String> i = fDropMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }
        }
        if(bClaimMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 6) Ball Claimed: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 6) Ball Claimed: " + bClaimMsgs.size());
            int index = 0;
            Iterator<String> i = bClaimMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }
        }
        if(bFiredMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 7) Ball Fired: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 7) Ball Fired: " + bFiredMsgs.size());
            int index = 0;
            Iterator<String> i = bFiredMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }
        }
        if(timerMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 8) Timer: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 8) Timer: " + timerMsgs.size());
            int index = 0;
            Iterator<String> i = timerMsgs.iterator();
            while( i.hasNext() ){
                String s = i.next();
                m_botAction.sendSmartPrivateMessage( name, "|     - " + index + ") " + s);
                index++;
            }    
        }
        m_botAction.sendSmartPrivateMessage( name, "+-------------------------------------------------------");
    }
    
    /**
     * Deletes a specific message.
     * @param name - The user
     * @param msg - The message to send
     */
    public void doDeleteMsg(String name, String message){
        int x, y;
        String[] msg = message.split(":");
        try{
            x = Integer.parseInt(msg[0]);
            y = Integer.parseInt(msg[1]);
            switch(x){
            	case 0: greetMsgs.remove(y); break;
                case 1: killMsgs.remove(y); break;
                case 2: spawnMsgs.remove(y); break;
                case 3: weapMsgs.remove(y); break;
                case 4: fClaimMsgs.remove(y); break;
                case 5: fDropMsgs.remove(y); break;
                case 6: bClaimMsgs.remove(y); break;
                case 7: bFiredMsgs.remove(y); break;
                case 8: timerMsgs.remove(y); break;
                default: throw new IndexOutOfBoundsException();
            }
            m_botAction.sendSmartPrivateMessage( name, "Removed message at index ( " + x +  ", " + y + " ).");
        }catch(NumberFormatException e){
            m_botAction.sendSmartPrivateMessage( name, "Correct usage: !delmsg #:#");
        }catch(IndexOutOfBoundsException e){
            m_botAction.sendSmartPrivateMessage( name, "Could not find a message at index ( " + msg[0] + ", " + msg[1] + " ).");
        }
    }
    
    /**
     * Deletes all of a message type
     * @param name - The user
     * @param msg - The message to send
     */
    public void doClearMsg(String name, String msg){
        int x;
        try{
            x = Integer.parseInt(msg);
            switch(x){
            	case 0: greetMsgs.clear(); break;
                case 1: killMsgs.clear(); break;
                case 2: spawnMsgs.clear(); break;
                case 3: weapMsgs.clear(); break;
                case 4: fClaimMsgs.clear(); break;
                case 5: fDropMsgs.clear(); break;
                case 6: bClaimMsgs.clear(); break;
                case 7: bFiredMsgs.clear(); break;
                case 8: timerMsgs.clear(); break;
                default: throw new IndexOutOfBoundsException();
            }
            m_botAction.sendSmartPrivateMessage( name, "Cleared all messages of type " + x + ".");
        }catch(NumberFormatException e){
            m_botAction.sendSmartPrivateMessage( name, "Correct usage: !clearmsg #");
        }catch(IndexOutOfBoundsException e){
            m_botAction.sendSmartPrivateMessage( name, "Please select a number between 0 and 8.");
        }
    }
    
    /**
     * Deletes all messages of all types
     * @param name - The user
     */
    public void doClearAllMsg(String name){
    	greetMsgs.clear();
        killMsgs.clear();
        spawnMsgs.clear();
        weapMsgs.clear();
        fClaimMsgs.clear();
        fDropMsgs.clear();
        bClaimMsgs.clear();
        bFiredMsgs.clear();
        timerMsgs.clear();
        m_botAction.sendSmartPrivateMessage( name, "All messages cleared.");
    }

    /**
     * Handles PlayerEntered events.
     */
    public void handleEvent(PlayerEntered event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = greetMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);    	
    }
    
    /**
     * Handles PowerBall tracking
     */
    public void handleEvent(BallPosition event){
        byte ID = event.getBallID();
        int carrier = event.getCarrier();
        int playerID = event.getPlayerID();
        if(!ballMap.containsKey(ID))
                ballMap.put(ID, new Ball(-1, -1));
        Ball b = ballMap.get(ID);        
        //Ball Fired
        if(carrier == -1 && carrier < b.getCurrentCarrier()){
            Player p = m_botAction.getPlayer(b.getCurrentCarrier());
            Iterator<String> i = bFiredMsgs.iterator();
            while( i.hasNext() )
                CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);
            
        }
        //Ball Caught
        else if(b.getCurrentCarrier() == -1 && b.getCurrentCarrier() < carrier){
            Player p = m_botAction.getPlayer(carrier);
            Iterator<String> i = bClaimMsgs.iterator();
            while( i.hasNext() )
                CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);
        }
        b.updateLastCarrier(playerID);
        b.updateCurrentCarrier(carrier);
    }
    
    /**
     * Handles death events
     */
    public void handleEvent(PlayerDeath event){
        Player killed = m_botAction.getPlayer(event.getKilleeID());
        Player killer = m_botAction.getPlayer(event.getKillerID());
        if(killed == null || killer == null)return;
        Iterator<String> i = killMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), killer, twscript.isSysop);

        new SpawnTimer(killed);
    }
    
    /**
     * Handles weapon events
     */
    public void handleEvent(WeaponFired event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = weapMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);
    }
    
    /**
     * Handles flag capture events
     */
    public void handleEvent(FlagClaimed event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = fClaimMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);
    }
    
    /**
     * Handles flag drop events
     */
    public void handleEvent(FlagDropped event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = fDropMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);
    }
    
    public void cancel() {}    
    
    private class SpawnTimer {
        Player p;
        private TimerTask runIt = new TimerTask() {
            public void run() {
                Iterator<String> i = spawnMsgs.iterator();
                while( i.hasNext() )
                    CodeCompiler.handlePrivateTWScript(m_botAction, i.next(), p, twscript.isSysop);
                
            }
        };
        
        public SpawnTimer(Player p) {
            this.p = p;
            m_botAction.scheduleTask(runIt, SPAWN_TIME);
        }
    }
    
    private class Ball {
        private int lastCarrier;
        private int currentCarrier;
        
        private Ball(int lastCarrier, int currentCarrier){
            this.lastCarrier = lastCarrier;
            this.currentCarrier = currentCarrier;
        }
        
        public int getLastCarrier(){
            return lastCarrier;
        }
        
        public int getCurrentCarrier(){
            return currentCarrier;
        }       
        
        public void updateLastCarrier(int id){
            lastCarrier = id;
        }
        
        public void updateCurrentCarrier(int id){
            currentCarrier = id;
        }
        
    }
}
