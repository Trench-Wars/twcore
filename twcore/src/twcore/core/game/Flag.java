package twcore.core.game;

import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagPosition;
import twcore.core.events.TurfFlagUpdate;

public class Flag {

    private int m_flagID;
    private int m_xLocation;
    private int m_yLocation;
    private int m_team;
    private int m_playerID = -1;
    private boolean m_flagClaimed;

    public Flag( FlagPosition message ) {

        m_flagID = message.getFlagID();
        m_xLocation = message.getXLocation();
        m_yLocation = message.getYLocation();
        m_team = message.getTeam();
    }

    public Flag( FlagClaimed message ) {

        m_flagID = message.getFlagID();
        m_playerID = message.getPlayerID();
        m_flagClaimed = true;
    }

    public Flag( TurfFlagUpdate message ) {

        m_flagID = message.getFlagID();
        m_team = message.getFrequency();
        m_flagClaimed = message.claimed();
    }

    public void processEvent( FlagPosition message ) {

        m_xLocation = message.getXLocation();
        m_yLocation = message.getYLocation();
        m_team = message.getTeam();
    }

    /**
        NOTE: If for some reason the playerID is invalid, m_team will be set to -1.
        This should be accounted for when using flags.
        @param message FlagClaimed event
        @param team Team that claimed the flag; -1 if unable to get it
    */
    public void processEvent( FlagClaimed message, int team ) {

        m_playerID = message.getPlayerID();
        m_flagClaimed = true;
        m_team = team;
    }

    public void processEvent( TurfFlagUpdate message ) {

        m_team = message.getFrequency();
        m_flagClaimed = message.claimed();
    }

    public void dropped() {

        m_playerID = -1;
        m_flagClaimed = false;
    }

    public void setTeam(int freq) {
        m_team = freq;
    }

    public void setPlayerID(int id) {
        m_playerID = id;

        if (id < 0)
            m_flagClaimed = false;
        else
            m_flagClaimed = true;
    }

    public int getFlagID() {
        return m_flagID;
    }
    public int getXLocation() {
        return m_xLocation;
    }
    public int getYLocation() {
        return m_yLocation;
    }
    public int getTeam() {
        return m_team;
    }
    public int getPlayerID() {
        return m_playerID;
    }
    public boolean carried() {
        return m_flagClaimed;
    }

}