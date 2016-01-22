package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x31) Event fired when the login sequence completes. <code>
    +------------------------------+
    | Offset  Length  Description  |
    +------------------------------+
    | 0       1       Type Byte    |
    +------------------------------+</code>
    As far as I (D1st0rt) can tell, this packet is no longer used outside of the VIE protocol.
    ASSS and Continuum don't use it and so twcore no longer listens for it.
*/
public class LoggedOn extends SubspaceEvent {

    /**
        Creates a new instance of LoggedOn, this is called by GamePacketInterpreter
        when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public LoggedOn( ByteArray array ) {

    }
}
