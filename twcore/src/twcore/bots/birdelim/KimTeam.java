/**
    @(#)KimTeam.java


    @author
    @version 1.00 2007/9/3
*/
package twcore.bots.birdelim;

import java.util.ArrayList;
import java.util.Iterator;
import java.lang.Iterable;

final class KimTeam implements Iterable<KimPlayer> {

    ArrayList<KimPlayer>    m_players;
    int                     m_freq;
    boolean                 m_isOut = false;

    KimTeam(int capacity) {
        m_players = new ArrayList<KimPlayer>(capacity);
    }

    void add(KimPlayer kp) {
        m_players.add(kp);
    }

    void setFreq(int freq) {
        for(KimPlayer kp : m_players) {
            kp.m_freq = freq;
        }

        m_freq = freq;
    }

    KimPlayer removeOne() {
        return m_players.remove(m_players.size() - 1);
    }

    boolean contains(KimPlayer kp) {
        return m_players.contains(kp);
    }

    int size() {
        return m_players.size();
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean showRec) {
        if(m_players.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(Math.min(256, 32 * size()));
        boolean many = m_players.size() > 1;
        Iterator<KimPlayer> iter = m_players.iterator();

        //sb.append("Grp").append(m_freq % 4).append(":Frq").append(m_freq).append(':');
        while(iter.hasNext()) {
            KimPlayer kp = iter.next();

            if(many && !iter.hasNext()) {
                sb.append("and ");
            }

            sb.append(kp.m_name);

            if(showRec) {
                sb.append(" (").append(kp.m_kills).append('-').append(kp.m_deaths);
                sb.append(kp.m_isOut ? " out)" : ')');
            }

            if(iter.hasNext()) {
                sb.append(", ");
            }

            if(sb.length() > 240) {
                break;
            }
        }

        return sb.toString();
    }

    public Iterator<KimPlayer> iterator() {
        return m_players.iterator();
    }

    ArrayList<KimPlayer> getPlayers() {
        return m_players;
    }

    synchronized boolean isOut() {
        if(m_isOut) {
            return true;
        }

        for(KimPlayer kp : m_players) {
            if(!kp.m_isOut) {
                return false;
            }
        }

        return m_isOut = true;
    }

    int getTotalKillsDeaths() {
        int sumKills = 0;
        int sumDeaths = 0;

        for(KimPlayer kp : m_players) {
            sumKills += kp.m_kills;
            sumDeaths += kp.m_deaths;
        }

        return sumKills << 16 | sumDeaths;
    }
}