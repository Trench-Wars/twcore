package twcore.core;

/**
 * Event called when a team wins a flag jackpot.
 * <br><font size="+2"><b>FlagReward: <font color="blue">S2C 0x14</b></font></font>
 * <table border="1" cellspacing="0">
 * <tr> <th>Offset</th><th>Length</th><th>Description</th> </tr>
 * <tr> <td>0</td> <td>1</td> <td>Type Byte</td> </tr>
 * <tr> <td>1</td> <td>2</td> <td>Freq</td> </tr>
 * <tr> <td>3</td> <td>4</td> <td>Jackpot Size</td> </tr>
 * </table><br></html>
 */
public class FlagVictory extends SubspaceEvent {

    //Variable Declarations
    int         m_reward;
    short       m_frequency;

    /**
     * Creates a new instance of FlagVictory, this is called by GamePacketInterpreter
     * when it receives the packet
     * @param array the ByteArray containing the packet data
     */
    public FlagVictory( ByteArray array ){

        m_frequency = array.readLittleEndianShort( 1 );
        m_reward = array.readLittleEndianInt( 3 );
    }

    /**
     * This gets the frequency that won the points
     * @return the freq that won the points
     */
    public short getFrequency(){
        return m_frequency;
    }

    /**
     * This gets the size of the jackpot (number of points won)
     * @return the jackpot size
     */
    public int getReward(){
        return m_reward;
    }

}
