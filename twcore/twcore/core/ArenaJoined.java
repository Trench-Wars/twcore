package twcore.core;

/**
 * <font size="+2"><b>ArenaJoined: <font color="blue">S2C 0x02</b></font></font>
 * <table border="1" cellspacing="0">
 * <tr> <th>Offset</th><th>Length</th><th>Description</th> </tr>
 * <tr> <td>0</td> <td>1</td> <td>Type Byte</td> </tr>
 * </table><br>
 * This packet lets you know that you can start sending position packets
 */
public class ArenaJoined extends SubspaceEvent
{
    /**
     * Creates a new instance of ArenaJoined, this is called by GamePacketInterpreter
     * when it recieves the packet.
     * @param array the ByteArray containing the packet data
     */
    public ArenaJoined(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.ARENA_JOINED; //sets the event type in the superclass
    }
}
