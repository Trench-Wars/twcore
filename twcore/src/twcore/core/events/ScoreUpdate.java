package twcore.core.events;

import twcore.core.util.ByteArray;

/**
 * (S2C 0x09) Event fired whenever a player's score is changed. <code><pre>
 *
 * +-------------------------+
 * |Field Length Description |
 * +-------------------------+
 * |0       1    Type byte	 |
 * |1       2    Player ident|
 * |7       4    Kill points |
 * |3       4    Flag points |
 * |11      2    Wins        |
 * |13      2    Losses      |
 * +-------------------------+</code></pre>
 */
public class ScoreUpdate extends SubspaceEvent {
    short m_playerID;   // ID of player whose score changed
    int   m_flagPoints; // Number of FlagPoints the player has after a score change
    int   m_killPoints; // Number of KillPoints the player has after a score change
    short m_wins;       // Number of wins the player has after a score change
    short m_losses;     // Number of losses the player has after a score change

    /**
     * Creates a new instance of ScoreUpdate; this is called by
     * GamePacketInterpreter when it receives the packet.
     * @param array the ByteArray containing the packet data
     */
    public ScoreUpdate(ByteArray array){
        m_playerID = array.readLittleEndianShort( 1 );
        m_killPoints = array.readLittleEndianInt( 3 );
        m_flagPoints = array.readLittleEndianInt( 7 );
        m_wins = array.readLittleEndianShort( 11 );
        m_losses = array.readLittleEndianShort( 13 );
    }

    /**
     * Gets the Id of the player whose score changed.
     * @return PlayerID
     */
    public short getPlayerID(){
        return m_playerID;
    }

    /**
     * Gets the number of Flag Points the player has after
     * score change.
     * @return FlagPoints
     */
    public int getFlagPoints(){
        return m_flagPoints;
    }

    /**
     * Gets the number of Kill Points the player has after
     * a score change.
     * @return KillPoints
     */
    public int getKillPoints(){
        return m_killPoints;
    }

    /**
     * Gets the number of wins the player has after
     * a score change.
     * @return Wins
     */
    public short getWins(){
        return m_wins;
    }

    /**
     * Gets the number of losses the player has after
     * a score change.
     * @return Losses
     */
    public short getLosses(){
        return m_losses;
    }
}
