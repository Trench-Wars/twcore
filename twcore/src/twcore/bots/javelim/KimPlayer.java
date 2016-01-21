package twcore.bots.javelim;

final class KimPlayer {
    String m_name;
    String m_lcname;
    int m_kills = 0;
    int m_deaths = 0;
    int m_freq;
    int m_lagoutsLeft = 3;
    long m_timeOutside = 0;
    long m_timeLastPosUpdate = 0;
    boolean m_notWarnedYet = true;
    boolean m_isOut = false;

    KimPlayer(String name, int freq) {
        m_name = name;
        m_lcname = name.toLowerCase();
        m_freq = freq;
    }

    void resetTime() {
        m_timeOutside = 0;
        m_notWarnedYet = true;
        m_timeLastPosUpdate = System.currentTimeMillis();
    }

    public String toString() {
        throw new RuntimeException("toString() shouldn't be called");
    }
}
