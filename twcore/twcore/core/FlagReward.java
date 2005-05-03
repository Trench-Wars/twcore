package twcore.core;

/**
 * Event called when a team gets periodic flag points from holding them.
 * <br><font size="+2"><b>FlagReward: <font color="blue">S2C 0x23</b></font></font>
 * <table border="1" cellspacing="0">
 * <tr> <th>Offset</th><th>Length</th><th>Description</th> </tr>
 * <tr> <td>0</td> <td>1</td> <td>Type Byte</td> </tr>
 * <tr> <td>1</td> <td>2</td> <td>Freq</td> </tr>
 * <tr> <td>3</td> <td>2</td> <td>Points Awarded</td> </tr>
 * </table><br></html>
 * It is important to note that the actual packet can contain more information
 * by repeating the last two fields until all awards have been announced.
 * The GamePacketInterpreter has already split it up into separate events for us though :D
 */
public class FlagReward extends SubspaceEvent {

    //Variable Declarations
    short         m_points;
    short         m_frequency;

    /**
     * Creates a new instance of FlagReward, this is called by GamePacketInterpreter
     * when it receives the packet
     * @param array the ByteArray containing the packet data
     */
    public FlagReward( ByteArray array ){

        m_frequency = array.readLittleEndianShort( 1 );
        m_points = array.readLittleEndianShort( 3 );
    }

    /**
     * This gets the frequency that won the points
     * @return the freq that won the points
     */
    public short getFrequency(){
        return m_frequency;
    }

    /**
     * This gets the number of points won
     * @return the number of points won
     */
    public short getPoints(){
        return m_points;
    }

}
