package twcore.core;

/**
 * Event called when a player drops a flag they are carrying.
 * <br><font size="+2"><b>FlagClaimed: <font color="blue">S2C 0x13</b></font></font>
 * <table border="1" cellspacing="0">
 * <tr> <th>Offset</th><th>Length</th><th>Description</th> </tr>
 * <tr> <td>0</td> <td>1</td> <td>Type Byte</td> </tr>
 * <tr> <td>1</td> <td>2</td> <td>Player ID</td> </tr>
 * </table><br></html>
 * This event occurs for warzone flags, but players aren't updated as not carrying
 * any warzone flags automatically in the core.
 */
public class FlagDropped extends SubspaceEvent
{
    //Variable Declarations
    private int m_playerID;

    /**
     * Creates a new instance of FlagDropped, this is called by GamePacketInterpreter
     * when it receives the packet
     * @param array the ByteArray containing the packet data
     */
    public FlagDropped(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.FLAG_DROPPED; //sets the event type in the superclass

        m_playerID = (int) array.readLittleEndianShort(1);
    }

    /**
	 * This gets the ID of the player that just dropped the flag
	 * @return the ID of the dropping player
     */
    public int getPlayerID()
    {
        return m_playerID;
    }
}