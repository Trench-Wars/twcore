package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x02) Event called when a bot enters the arena. <code>
    +-----------------------------+
    | Offset  Length  Description |
    +-----------------------------+
    | 0       1       Type Byte   |
    +-----------------------------+</code>
    This packet lets you know that you can start sending position packets
*/
public class ArenaJoined extends SubspaceEvent {

    /**
        Creates a new instance of ArenaJoined, this is called by GamePacketInterpreter
        when it recieves the packet.
        @param array the ByteArray containing the packet data
    */
    public ArenaJoined( ByteArray array ) {

    }
}
