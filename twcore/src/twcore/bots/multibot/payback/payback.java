package twcore.bots.multibot.payback;

import static twcore.core.EventRequester.FREQUENCY_SHIP_CHANGE;
import static twcore.core.EventRequester.PLAYER_DEATH;
import static twcore.core.EventRequester.PLAYER_LEFT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotAction;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;

/** 
 * Payback module.
 * 
 * Author: Ikrit
 *
 *  Version: 1.0
 */
public class payback extends MultiModule
{
    HashMap <String,PaybackTask>payback;
    HashMap <String,HashSet<String>>beingTracked;

    int time = 30;

    boolean started = false;

    public void init() {
        payback = new HashMap<String,PaybackTask>();
        beingTracked = new HashMap<String,HashSet<String>>();
    }

    public void requestEvents(ModuleEventRequester events)
    {
        events.request(this, PLAYER_DEATH);
        events.request(this, PLAYER_LEFT);
        events.request(this, FREQUENCY_SHIP_CHANGE);
    }

    public void handleEvent(PlayerDeath event) {
        if(!started) return;

        String killee = m_botAction.getPlayerName(event.getKilleeID()).toLowerCase();
        String killer = m_botAction.getPlayerName(event.getKillerID()).toLowerCase();

        if(payback.containsKey(killee + killer)) {
            PaybackTask pb = (PaybackTask)payback.get(killee + killer);
            m_botAction.cancelTask(pb);
            payback.remove(killee + killer);
            m_botAction.sendPrivateMessage(killer, "You have avenged your death and may continue to play.");
        } else {
            PaybackTask pb = new PaybackTask(killee, m_botAction);
            payback.put(killer + killee, pb);
            if(beingTracked.containsKey(killer)) {
                HashSet <String>killed = beingTracked.get(killer);
                killed.add(killee);
                beingTracked.put(killer, killed);
            } else {
                HashSet <String>killed = new HashSet<String>();
                killed.add(killee);
                beingTracked.put(killer, killed);
            }
            m_botAction.scheduleTask(pb, time * 1000);
            m_botAction.sendPrivateMessage(killee, "How can you continue to play after that? Avenge your death or be spec'd!");
        }
    }

    public void handleEvent(PlayerLeft event) {
        if(!started) return;

        String player = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
        if(beingTracked.containsKey(player)) {
            Iterator<String> it = beingTracked.get(player).iterator();
            while(it.hasNext()) {
                String avenger = it.next();
                if(payback.containsKey(player + avenger)) {
                    PaybackTask pb = (PaybackTask)payback.get(player + avenger);
                    m_botAction.cancelTask(pb);
                    payback.remove(player + avenger);
                    m_botAction.sendPrivateMessage(avenger, player + " has run away like a coward, consider your death avenged.");
                }
            }
        }
    }

    public void handleEvent(FrequencyShipChange event) {
        if(!started) return;

        if(event.getShipType() == 0) {
            String player = m_botAction.getPlayerName(event.getPlayerID()).toLowerCase();
            if(beingTracked.containsKey(player)) {
                Iterator<String> it = beingTracked.get(player).iterator();
                while(it.hasNext()) {
                    String avenger = (String)it.next();
                    if(payback.containsKey(player + avenger)) {
                        PaybackTask pb = (PaybackTask)payback.get(player + avenger);
                        m_botAction.cancelTask(pb);
                        payback.remove(player + avenger);
                        m_botAction.sendPrivateMessage(avenger, player + " has run away like a coward, consider your death avenged.");
                    }
                }
            }
        }
    }

    public void handleEvent(Message event) {
        String message = event.getMessage();
        if(event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if(opList.isER(name))
                handleCommand(name, message);
        }
    }

    public void handleCommand(String name, String message) {
        if(message.toLowerCase().startsWith("!start")) {
            m_botAction.sendArenaMessage("Payback started, GO!", 104);
            started = true;
            cancel();
        } else if(message.toLowerCase().startsWith("!stop")) {
            m_botAction.sendArenaMessage("Payback stopped!", 13);
            cancel();
        } else if(message.toLowerCase().startsWith("!time ")) {
            time = 30;
            try {
                time = Integer.parseInt(message.split(" ", 2)[1]);
            } catch(Exception e) {}
        }
    }

    public String[] getModHelpMessage() {
        String helps[] = {
                "Payback.  [Quickly kill anyone who kills you -- or be spec'd!]",
                "!start        - Starts payback.",
                "!stop         - Stops payback.",
                "!time #       - Sets time a player has to kill anyone who kills them," +
                "                in seconds, before they are spec'd."
        };

        return helps;
    }

    public void cancel() {
        Iterator<PaybackTask> it = payback.values().iterator();
        while(it.hasNext()) {
            PaybackTask pb = it.next();
            if(!pb.isCancelled())
                m_botAction.cancelTask(pb);
        }

        payback.clear();
        beingTracked.clear();
    }

    public boolean isUnloadable() {
        return true;
    }

}

class PaybackTask extends TimerTask {

    String player;
    BotAction m_botAction;
    boolean cancelled = false;

    public PaybackTask(String name, BotAction ba) {
        player = name;
        m_botAction = ba;
    }

    public void run() {
        m_botAction.spec(player);
        m_botAction.spec(player);
        m_botAction.sendPrivateMessage(player, "You're supposed to kill your killer... not sit around and eat grasshoppers in left field.");
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}