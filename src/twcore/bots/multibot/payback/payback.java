package twcore.bots.multibot.payback;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.game.Player;

import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.FrequencyShipChange;

public class payback extends MultiModule {

    public OperatorList opList;

    public TreeMap<String, PaybackPlayer> playerMap = new TreeMap<String, PaybackPlayer>();
    public boolean isRunning = false;
    public int m_time = 30, m_frequency = 0;
    public TimerTask update;

    public String[] helpmsg = {
        "+----------------------Payback-----------------------+",
        "| !start -- Starts the game.                         |",
        "| !stop  -- Stops the game.                          |",
        "| !time # -- Changes the amount of time a player has |",
        "|             to avenge himself to #.                |",
        "+----------------------------------------------------+"
    };

    public String[] rulesmsg = {
        "+----------------Rules of Payback----------------+",
        "| Revenge is sweet. If you're killed you have a  |",
        "| limited amount of time to avenge yourself! The |",
        "| last man standing wins.                        |",
        "+------------------------------------------------+"
    };
    public void init() {
        opList = m_botAction.getOperatorList();
    }
    public void requestEvents(ModuleEventRequester req) {
        req.request(this, EventRequester.PLAYER_LEFT);
        req.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(this, EventRequester.PLAYER_DEATH);
    }

    public void handleEvent(Message event) {
        String message = event.getMessage();
        String name = m_botAction.getPlayerName(event.getPlayerID());
        int messageType = event.getMessageType();

        if(messageType == Message.PRIVATE_MESSAGE && opList.isER(name))
            handleCommands(name, message);

        if(messageType == Message.PRIVATE_MESSAGE && message.equalsIgnoreCase("!lagout"))
            doLagOut(name);

        if(messageType == Message.PRIVATE_MESSAGE && message.equalsIgnoreCase("!rules"))
            m_botAction.privateMessageSpam(name, rulesmsg);
    }

    public void handleCommands(String name, String msg) {
        if(msg.equalsIgnoreCase("!start"))
            doStartGame(name);
        else if(msg.equalsIgnoreCase("!stop"))
            doStopGame(name);
        else if(msg.startsWith("!settime "))
            doSetTime(name, msg.substring(9));
        else if(msg.equalsIgnoreCase("!displayrules"))
            m_botAction.arenaMessageSpam(rulesmsg);
    }

    public void handleEvent(PlayerLeft event) {
        if(!isRunning)return;

        String name = m_botAction.getPlayerName(event.getPlayerID());

        if(playerMap.containsKey(name)) {
            doRemovePlayer(name);
            m_botAction.sendArenaMessage(name + " is out!");

            if(playerMap.size() == 1) {
                doEndGame(playerMap.firstKey());
            }
        }
    }

    public void handleEvent(FrequencyShipChange event) {
        if(!isRunning)return;

        String name = m_botAction.getPlayerName(event.getPlayerID());

        if(!playerMap.containsKey(name))
            playerMap.put(name, new PaybackPlayer(name));

        if(event.getShipType() == 0) {
            doRemovePlayer(name);
            m_botAction.sendArenaMessage(name + " is out!");

            if(playerMap.size() == 1) {
                doEndGame(playerMap.firstKey());
            }
        }
    }

    public void handleEvent(PlayerDeath event) {
        if(!isRunning)return;

        String killerName = m_botAction.getPlayerName(event.getKillerID());
        String killedName = m_botAction.getPlayerName(event.getKilleeID());
        PaybackPlayer killer = playerMap.get(killerName);
        PaybackPlayer killed = playerMap.get(killedName);

        if(killer.needsRevengeOn(killedName)) {
            killer.gotRevengeOn(killedName);
            m_botAction.sendSmartPrivateMessage( killerName, "You got revenge on " + killedName + "!");
            m_botAction.sendSmartPrivateMessage( killedName, killerName + " got revenge on you!");
        }
        else if(!killed.needsRevengeOn(killerName)) {
            killed.addPayBack(killerName, System.currentTimeMillis());
            m_botAction.sendSmartPrivateMessage( killerName, "You've committed murder! " + killedName + " has " + m_time + " seconds to get you back!");
            m_botAction.sendSmartPrivateMessage( killedName, killerName + " killed you! You have " + m_time + " seconds to pay back the favor!");
        }
        else if(killed.needsRevengeOn(killerName)) {
            m_botAction.sendSmartPrivateMessage( killedName, "Stop letting that sucker get you! Avenge yourself!");
        }
    }

    public void doLagOut(String name) {
        Player p = m_botAction.getPlayer(name);

        if(p == null)return;

        if(playerMap.containsKey(name) && p.getShipType() == 0) {
            m_botAction.setShip(name, 1);
            m_botAction.setFreq(name, m_frequency);
            m_frequency++;
        }
    }

    public void doStartGame(String name) {
        if(isRunning) {
            m_botAction.sendSmartPrivateMessage( name, "The game has already been started!");
            return;
        }

        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

        while( i.hasNext() ) {
            Player p = i.next();
            playerMap.put(p.getPlayerName(), new PaybackPlayer(p.getPlayerName()));
            m_botAction.setFreq(p.getPlayerName(), m_frequency);
            m_frequency++;
        }

        update = new TimerTask() {
            public void run() {
                Iterator<PaybackPlayer> i = playerMap.values().iterator();

                while( i.hasNext() ) {
                    PaybackPlayer p = i.next();

                    if(p.hasOutstandingPaybacks()) {
                        m_botAction.sendSmartPrivateMessage( p.getName(), "You failed to avenge yourself! Better luck next time.");
                        m_botAction.specWithoutLock(p.getName());
                    }
                    else
                        p.giveWarnings();
                }

                if(playerMap.size() == 1)
                    doEndGame(playerMap.firstKey());
            }
        };
        m_botAction.scheduleTaskAtFixedRate(update, 1000, 1000);
        m_botAction.scoreResetAll();
        m_botAction.shipResetAll();
        m_botAction.sendArenaMessage("Payback has begun!", Tools.Sound.GOGOGO);
        isRunning = true;
    }

    public void doStopGame(String name) {
        if(!isRunning) {
            m_botAction.sendSmartPrivateMessage( name, "There isn't a game in progress!");
            return;
        }

        m_botAction.sendArenaMessage("Game stopped. -" + name);
        m_botAction.toggleLocked();
        cancel();
    }

    public void doEndGame(String name) {
        m_botAction.sendArenaMessage(name + " has won the game!", Tools.Sound.HALLELUJAH);
        m_botAction.toggleLocked();
        cancel();
    }

    public void doRemovePlayer(String name) {
        playerMap.remove(name);
        Iterator<PaybackPlayer> i = playerMap.values().iterator();

        while( i.hasNext() ) {
            PaybackPlayer p = i.next();

            if(p.needsRevengeOn(name))
                p.gotRevengeOn(name);
        }
    }

    public void doSetTime(String name, String msg) {
        if(isRunning) {
            m_botAction.sendSmartPrivateMessage( name, "Please modify the time before the start of the game.");
            return;
        }

        try {
            int time = Integer.parseInt(msg);

            if(m_time >= 15) {
                m_time = time;
                m_botAction.sendSmartPrivateMessage( name, "Time set to " + time + " seconds.");
            }
            else {
                m_time = 15;
                m_botAction.sendSmartPrivateMessage( name, "Time can be set to a minimum of 15 seconds. Time set to 15 seconds.");
            }

        } catch(NumberFormatException e) {
            m_botAction.sendSmartPrivateMessage( name, "You must submit a number!");
        }
    }

    public String[] getModHelpMessage() {
        return helpmsg;
    }
    public boolean isUnloadable() {
        return true;
    }
    public void cancel() {
        playerMap.clear();
        m_time = 30;
        m_frequency = 0;
        isRunning = false;

        try {
            m_botAction.cancelTask(update);
        } catch(Exception e) {}
    }

    private class PaybackPlayer {
        private TreeMap<String, Long> paybackList = new TreeMap<String, Long>();
        private String name;
        private PaybackPlayer(String name) {
            this.name = name;
        }

        private void addPayBack(String name, long time) {
            paybackList.put(name, time);
        }

        private boolean hasOutstandingPaybacks() {
            Iterator<Long> i = paybackList.values().iterator();

            while( i.hasNext() ) {
                long x = i.next();

                if(System.currentTimeMillis() - x > m_time * 1000)
                    return true;
            }

            return false;
        }

        private void giveWarnings() {
            Iterator<Long> i = paybackList.values().iterator();

            while( i.hasNext() ) {
                long x = i.next();
                long t = ((m_time * 1000) - (System.currentTimeMillis() - x));

                if( t < 10500 && t > 9500)
                    m_botAction.sendSmartPrivateMessage( name, "You have 10 seconds to avenge yourself!");
            }
        }

        private boolean needsRevengeOn(String name) {
            if(paybackList.containsKey(name))
                return true;
            else return false;
        }

        private void gotRevengeOn(String name) {
            paybackList.remove(name);
        }

        private String getName() {
            return name;
        }

    }
}