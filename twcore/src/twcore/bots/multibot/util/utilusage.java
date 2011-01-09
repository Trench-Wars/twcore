package twcore.bots.multibot.util;

import java.util.Iterator;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * Used to restrict players without a certain amount of usage from playing
 * @author WingZero
 */

public class utilusage extends MultiUtil {
    private final String[] helpMessage =
    {
        "--Usage Limiter--",
        "  !usage <hours>          -  Allows only players with more than <hours> usage to enter",
        "  !usage <hours>:<hours>  -  Allows only players with between <hours>: and :<hours> usage to enter",
        "  !usage 0                -  Turns off the usage limiter",
    };
    
    private int usageLow;
    private int usageHi;
    private boolean limit;
    private String currentName;

    @Override
    public void init() {
        usageLow = -1;
        usageHi = -1;
        limit = false;
        currentName = "";
    }

    @Override
    public String[] getHelpMessages() {
        return helpMessage;
    }

    @Override
    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
    }
    
    public void handleEvent(Message event)  {
        String msg = event.getMessage();
        if(event.getMessageType() == Message.PRIVATE_MESSAGE)   {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if(m_opList.isER(name) && msg.startsWith("!usage")) {
                handleUsage(name, msg);
            }
        } else if (event.getMessageType() == Message.ARENA_MESSAGE) {
            if (limit && msg.contains("TypedName:")) {
                currentName = msg.substring(msg.indexOf("TypedName:")+10);
                currentName = currentName.substring(0, currentName.indexOf("Demo:")).trim();
            } else if (limit && msg.contains("TIME: Session:") && m_botAction.getPlayer(currentName).getShipType() != 0) {
                msg = msg.substring(msg.indexOf("Total:"));
                msg = msg.substring(msg.indexOf(":") + 1, msg.indexOf("Created"));
                msg = msg.trim();
                String[] time = msg.split(":");
                if (time.length == 3) {
                    int hours = Integer.valueOf(time[0]);
                    if (usageHi > -1) {
                        if (hours < usageLow || hours > usageHi) {
                            m_botAction.specWithoutLock(currentName);
                            m_botAction.sendSmartPrivateMessage(currentName, "Sorry, but this event requires a usage between " + usageLow + " and " + usageHi + " hours");
                        }
                    } else {
                        if (hours < usageLow) {
                            m_botAction.specWithoutLock(currentName);
                            m_botAction.sendSmartPrivateMessage(currentName, "Sorry, but this event requires a usage of " + usageLow + " hours");
                        }
                    }
                }
            }
        }
    }
    
    public void handleUsage(String name, String usage) {
        if (usage.length() < 8) {
            m_botAction.sendSmartPrivateMessage(name, "Invalid syntax! Please check help for the proper syntax usage.");
            return;
        } else {
            usage = usage.substring(usage.indexOf(" ") + 1);
            
            if (usage.indexOf(":") > 0) {
                try {
                    String[] nums = usage.split(":");
                    usageLow = Integer.valueOf(nums[0]);
                    usageHi = Integer.valueOf(nums[1]);
                    limit = true;
                } catch (NumberFormatException e) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid syntax! Please check help for the proper syntax usage.");
                    return;                    
                }
            } else {
                try {
                    usageLow = Integer.valueOf(usage);
                    usageHi = -1;
                    limit = true;
                    if (usageLow == 0) {
                        limit = false;
                        usageLow = -1;
                    }
                } catch (NumberFormatException e) {
                    m_botAction.sendSmartPrivateMessage(name, "Invalid syntax! Please check help for the proper syntax usage.");
                    return;                    
                }
            }
            
            if (limit) {
                String msg = "";
                if (usageHi == -1)
                    msg = "Usage limit set at " + usageLow;
                else
                    msg = "Usage limit set between " + usageLow + " and " + usageHi;
                m_botAction.sendArenaMessage(msg, Tools.Sound.CROWD_GEE);
                
                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
                Player p;
                while (i.hasNext()) {
                    p = i.next();
                    m_botAction.sendUnfilteredPrivateMessage(p.getPlayerID(), "*info");
                }
            } else 
                m_botAction.sendArenaMessage("Usage limiting disabled!", Tools.Sound.CROWD_OOO);
        }
    }
    
    public void handleEvent(FrequencyShipChange event) {
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if (limit && p.getShipType() != 0) {
            m_botAction.sendUnfilteredPrivateMessage(p.getPlayerID(), "*info");
        }
    }
}
