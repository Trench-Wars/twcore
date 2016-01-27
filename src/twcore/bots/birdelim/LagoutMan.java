/**
    @(#)LagoutMan.java


    @author flibb
    @version 1.00 2007/7/5
*/
package twcore.bots.birdelim;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.Iterator;
import java.util.Set;

import twcore.core.BotAction;

final class LagoutMan<K> {
    interface ExpiredLagoutHandler<K> {
        void handleExpiredLagout(K o);
    }

    private long m_expireTimems = 30000;
    private BotAction m_botAction;
    private Map<K, TimerTask> m_lagoutMap = Collections.synchronizedMap(new HashMap<K, TimerTask>());
    private ExpiredLagoutHandler<K> m_bot;

    public LagoutMan(ExpiredLagoutHandler<K> bot) {
        m_botAction = BotAction.getBotAction();
        m_bot = bot;
    }

    public LagoutMan(ExpiredLagoutHandler<K> bot, long expireTimems) {
        this(bot);
        m_expireTimems = expireTimems;
    }

    public void add(K o) {
        if(!m_lagoutMap.containsKey(o)) {
            TimerTask task = new LagoutTask(o);
            m_lagoutMap.put(o, task);
            m_botAction.scheduleTask(task, m_expireTimems);
        }
    }

    /**
        Attempts to remove an item from the lagout list because (ie. a player did !lagout)
        @param o The item associated with this lagout
        @return true if the lagout hasn't expired (ie. the player may be placed back in the game). Returns
        false if the lagout expired or never existed in the first place.
    */
    public boolean remove(K o) {
        TimerTask t = m_lagoutMap.remove(o);

        if(t != null) {
            return m_botAction.cancelTask(t);
        }

        return false;
    }

    public void clear() {
        synchronized(m_lagoutMap) {
            Iterator<TimerTask> iter = m_lagoutMap.values().iterator();

            while(iter.hasNext()) {
                TimerTask t = iter.next();
                m_botAction.cancelTask(t);
                iter.remove();
            }
        }
    }

    public K[] getLaggers(K[] array) {
        synchronized(m_lagoutMap) {
            Set<K> keys = m_lagoutMap.keySet();
            return keys.toArray(array);
        }
    }

    public int size() {
        return m_lagoutMap.size();
    }

    /**
        Checks if an item is in the the list
        @param o the item to check against (ie. name)
        @return true if it is (ie. player is lagged out). false otherwise
    */
    public boolean contains(K o) {
        return m_lagoutMap.containsKey(o);
    }

    private final class LagoutTask extends TimerTask {
        K m_o;

        LagoutTask(K o) {
            m_o = o;
        }

        public void run() {
            if(m_lagoutMap.remove(m_o) != null )
                m_bot.handleExpiredLagout(m_o);
        }
    }
}
