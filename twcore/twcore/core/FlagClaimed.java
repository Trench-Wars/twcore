package twcore.core;

/**
 * Event called when a player picks up or runs over a flag.
 * <br><font size="+2"><b>FlagClaimed: <font color="blue">S2C 0x13</b></font></font>
 * <table border="1" cellspacing="0">
 * <tr> <th>Offset</th><th>Length</th><th>Description</th> </tr>
 * <tr> <td>0</td> <td>1</td> <td>Type Byte</td> </tr>
 * <tr> <td>1</td> <td>2</td> <td>Flag ID</td> </tr>
 * <tr> <td>3</td> <td>2</td> <td>Player ID</td> </tr>
 * </table><br></html>
 * This event occurs for both turf and warzone flags, but players aren't updated as carrying
 * any warzone flags automatically in the core.
 */
public class FlagClaimed extends SubspaceEvent
{
    //Variable Declarations
    private int m_flagID;
    private int m_playerID;

    /**
     * Creates a new instance of FlagClaimed, this is called by GamePacketInterpreter
     * when it recieves the packet.
     * @param array the ByteArray containing the packet data
     */
    public FlagClaimed(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.FLAG_CLAIMED; //sets the event type in the superclass

        m_flagID = (int) array.readLittleEndianShort(1);
        m_playerID = (int) array.readLittleEndianShort(3);
    }

    /**
     * This gets the ID of the flag that was just claimed
     * @return the ID of the claimed flag
     */
    public int getFlagID()
    {
        return m_flagID;
    }

    /**
     * This gets the ID of the player that just claimed the flag
     * @return the ID of the claiming player
     */
    public int getPlayerID()
    {
        return m_playerID;
    }
}