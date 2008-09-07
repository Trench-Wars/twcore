package twcore.bots.multibot.twscript;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.ArrayList;

import twcore.bots.MultiUtil;
import twcore.bots.TWScript;
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
    public TWScript m_twscript;
    
    public ArrayList<String> greetMsgs = new ArrayList<String>();
    public ArrayList<String> killMsgs = new ArrayList<String>();
    public ArrayList<String> spawnMsgs = new ArrayList<String>();
    public ArrayList<String> fireMsgs = new ArrayList<String>();
    public ArrayList<String> fClaimMsgs = new ArrayList<String>();
    public ArrayList<String> fDropMsgs = new ArrayList<String>();
    public ArrayList<String> bClaimMsgs = new ArrayList<String>();
    public ArrayList<String> bFiredMsgs = new ArrayList<String>();
    public ArrayList<String> timerMsgs = new ArrayList<String>();
    
    public TreeMap<Byte, Integer> ballMap = new TreeMap<Byte, Integer>();
    public static final int SPAWN_TIME = 6010;
    
    public void init() {
        opList = m_botAction.getOperatorList();
    }
    
    public void initializeTWScript(TWScript tws){
		m_twscript = tws;
	}
    
    public String[] getHelpMessages() {
        String[] message = {
        		"+-------------------------------------- Events ---------------------------------------+",
        		"| !masspm <msg>     - Executes a private TWScript messages to everyone in the arena.  |",
        		"| !pub <msg>        - Executes a public TWScript message.                             |",
        		"| !greetmsg <msg>   - Adds a TWScript message for when a player enters.               |",
        		"| !killmsg <msg>    - Adds a TWScript message for when a player gets a kill.          |",
        		"| !spawnmsg <msg>   - Adds a TWScript message for when a player dies.                 |",
        		"| !firemsg <msg>    - Adds a TWScript message for when a player fires.                |",
        		"| !fclaimmsg <msg>  - Adds a TWScript message for when a player claims a flag.        |",
        		"| !fdropmsg <msg>   - Adds a TWScript message for when a player drops a flag.         |",
        		"| !bclaimmsg <msg>  - Adds a TWScript message for when a player picks up a ball.      |",
        		"| !bfiredmsg <msg>  - Adds a TWScript message for when a player fires a ball.         |",
        		"| !timermsg <msg>   - Adds a TWScript message that will execute at 'NOTICE: Game over'|",
        		"| !delmsg <#>:<#>   - Deletes message at index (#, #).                                |",
        		"| !clearmsg <#>     - Deletes all TWScript messages of type <#>.                      |",
        		"| !clearallmsg      - Clears all TWScript messages.                                   |",
        		"| !listmsg          - Displays all message types and their messages.                  |",
        		"+-------------------------------------------------------------------------------------+",
        };        
        return message;
    }
    
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String name = m_botAction.getPlayerName(event.getPlayerID());
        int messageType = event.getMessageType();
        if(messageType == Message.PRIVATE_MESSAGE && opList.getAccessLevel(name) >= m_twscript.ACCESS_LEVEL)
            handleCommands(name, message);
        else if(messageType == Message.ARENA_MESSAGE && message.equals("NOTICE: Game over")){
            Player p = m_botAction.getPlayer(m_botAction.getBotName());
            if(p == null)return;
            Iterator<String> i = timerMsgs.iterator();
            while( i.hasNext() )
                CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);        	
        }
    }
    
    public void handleCommands(String name, String msg) {
        if(msg.startsWith("!masspm "))
            do_massPm(name, msg.substring(8));
        else if(msg.startsWith("!pub "))
            do_pub(name, msg.substring(5));
        else if(msg.startsWith("!greetmsg "))
        	do_greetMsg(name, msg.substring(10));
        else if(msg.startsWith("!killmsg "))
            do_killMsg(name, msg.substring(9));
        else if(msg.startsWith("!spawnmsg "))
            do_spawnMsg(name, msg.substring(10));
        else if(msg.startsWith("!firemsg "))
            do_fireMsg(name, msg.substring(9));
        else if(msg.startsWith("!fclaimmsg "))
            do_flagClaimedMsg(name, msg.substring(11));
        else if(msg.startsWith("!fdropmsg "))
            do_flagDroppedMsg(name, msg.substring(10));
        else if(msg.startsWith("!bclaimmsg "))
            do_ballClaimedMsg(name, msg.substring(11));
        else if(msg.startsWith("!bfiredmsg "))
            do_ballFiredMsg(name, msg.substring(11));
        else if(msg.startsWith("!timermsg "))
        	do_timerMsg(name, msg.substring(10));
        else if(msg.equalsIgnoreCase("!listmsg"))
            do_listMsg(name);
        else if(msg.startsWith("!delmsg "))
            do_deleteMsg(name, msg.substring(8));
        else if(msg.startsWith("!clearmsg "))
            do_clearMsg(name, msg.substring(10));
        else if(msg.equalsIgnoreCase("!clearallmsg"))
            do_clearAllMsg(name);
    }
    
    /** Sends an unfiltered private message to everyone in the arena. */
    public void do_massPm(String name, String msg){
        Iterator<Player> i = m_botAction.getPlayerIterator();
        while( i.hasNext() )
            CodeCompiler.handleTWScript(m_botAction, msg, i.next(), m_twscript, m_twscript.ACCESS_LEVEL);
    }
    
    /** Executes a public TWScript message. */
    public void do_pub(String name, String message){
        CodeCompiler.handleTWScript(m_botAction, message, m_twscript, m_twscript.ACCESS_LEVEL);
    }
    
    /** Adds a message for arena entrance events. */
    public void do_greetMsg(String name, String message){
        if(message.length() < 220){
            greetMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Greet message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for kill events. */
    public void do_killMsg(String name, String message){
        if(message.length() < 220){
            killMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Kill message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for spawn events. */
    public void do_spawnMsg(String name, String message){
        if(message.length() < 220){
            spawnMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Spawn message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for weapon fired events. */
    public void do_fireMsg(String name, String message){
        if(message.length() < 220){
            fireMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Weapon fired message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for flag claim events. */
    public void do_flagClaimedMsg(String name, String message){
        if(message.length() < 220){
            fClaimMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Flag claimed message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for flag drop events. */
    public void do_flagDroppedMsg(String name, String message){
        if(message.length() < 220){
            fDropMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Flag dropped message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for ball claimed events. */
    public void do_ballClaimedMsg(String name, String message){
        if(message.length() < 220){
            bClaimMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Ball claimed message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for ball fired events. */
    public void do_ballFiredMsg(String name, String message){
        if(message.length() < 220){
            bFiredMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Ball fired message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Adds a message for timer events. */
    public void do_timerMsg(String name, String message){
        if(message.length() < 220){
            timerMsgs.add(message);
            m_botAction.sendSmartPrivateMessage( name, "Timer message added.");
        } else
            m_botAction.sendSmartPrivateMessage( name, "Please submit a message of 220 characters or less.");
    }
    
    /** Deletes a specific message. */
    public void do_deleteMsg(String name, String message){
        int x, y;
        String[] msg = message.split(":");
        try{
            x = Integer.parseInt(msg[0]);
            y = Integer.parseInt(msg[1]);
            switch(x){
            	case 0: greetMsgs.remove(y); break;
                case 1: killMsgs.remove(y); break;
                case 2: spawnMsgs.remove(y); break;
                case 3: fireMsgs.remove(y); break;
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
    
    /** Deletes all of a message type */
    public void do_clearMsg(String name, String msg){
        int x;
        try{
            x = Integer.parseInt(msg);
            switch(x){
            	case 0: greetMsgs.clear(); break;
                case 1: killMsgs.clear(); break;
                case 2: spawnMsgs.clear(); break;
                case 3: fireMsgs.clear(); break;
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
    
    /** Deletes all messages */
    public void do_clearAllMsg(String name){
    	greetMsgs.clear();
        killMsgs.clear();
        spawnMsgs.clear();
        fireMsgs.clear();
        fClaimMsgs.clear();
        fDropMsgs.clear();
        bClaimMsgs.clear();
        bFiredMsgs.clear();
        timerMsgs.clear();
        m_botAction.sendSmartPrivateMessage( name, "All messages cleared.");
    }

    /** Displays a list of active messages */
    public void do_listMsg(String name){
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
        if(fireMsgs.isEmpty())
            m_botAction.sendSmartPrivateMessage( name, "| 3) fireon Fired: NONE");
        else{
            m_botAction.sendSmartPrivateMessage( name, "| 3) fireon Fired: " + fireMsgs.size());
            int index = 0;
            Iterator<String> i = fireMsgs.iterator();
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
    
    /** Requests which events this utility watches for. */
    public void requestEvents(ModuleEventRequester req) {
        req.request(this, EventRequester.BALL_POSITION);
        req.request(this, EventRequester.PLAYER_DEATH);
        req.request(this, EventRequester.WEAPON_FIRED);
        req.request(this, EventRequester.FLAG_CLAIMED);
        req.request(this, EventRequester.FLAG_DROPPED);
        req.request(this, EventRequester.PLAYER_ENTERED);
    }
    
    /** Handles PlayerEntered events. */
    public void handleEvent(PlayerEntered event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = greetMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);    	
    }
    
    /** Handles PowerBall tracking */
    public void handleEvent(BallPosition event){
        byte ID = event.getBallID();
        int carrier = event.getCarrier();
        if(!ballMap.containsKey(ID))
                ballMap.put(ID, new Integer(-1));
        int b = ballMap.get(ID);
        //Ball Fired
        if(carrier == -1 && carrier < b){
            Player p = m_botAction.getPlayer(b);
            Iterator<String> i = bFiredMsgs.iterator();
            while( i.hasNext() )
                CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);
            
        }
        //Ball Caught
        else if(b == -1 && b < carrier){
            Player p = m_botAction.getPlayer(carrier);
            Iterator<String> i = bClaimMsgs.iterator();
            while( i.hasNext() )
                CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);
        }
        ballMap.remove(ID);
        ballMap.put(ID, carrier);
    }
    
    /** Handles death events */
    public void handleEvent(PlayerDeath event){
        Player killed = m_botAction.getPlayer(event.getKilleeID());
        Player killer = m_botAction.getPlayer(event.getKillerID());
        if(killed == null || killer == null)return;
        Iterator<String> i = killMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handleTWScript(m_botAction, i.next(), killer, m_twscript, m_twscript.ACCESS_LEVEL);

        new SpawnTimer(killed);
    }
    
    /** Handles weapon fired events */
    public void handleEvent(WeaponFired event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = fireMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);
    }
    
    /** Handles flag capture events */
    public void handleEvent(FlagClaimed event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = fClaimMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);
    }
    
    /** Handles flag drop events */
    public void handleEvent(FlagDropped event){
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if(p == null)return;
        Iterator<String> i = fDropMsgs.iterator();
        while( i.hasNext() )
            CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);
    }
    
private class SpawnTimer {
    private Player p;
    private TimerTask runIt = new TimerTask() {
        public void run() {
            Iterator<String> i = spawnMsgs.iterator();
            while( i.hasNext() )
                CodeCompiler.handleTWScript(m_botAction, i.next(), p, m_twscript, m_twscript.ACCESS_LEVEL);
                
        }
    };
        
    public SpawnTimer(Player p) {
        this.p = p;
        m_botAction.scheduleTask(runIt, SPAWN_TIME);
    }
}
	public void cancel() {}  
}
