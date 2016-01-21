package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x1D) Event called when player changes ship (and possibly frequency) <code><pre>
    +------------------------------+
    | Offset  Length  Description  |
    +------------------------------+
    | 0       1       Type Byte    |
    | 1       1       Ship Type    |
    | 2       2       Player ID    |
    | 4       2       Frequency    |
    +------------------------------+</code></pre>

    Since this is the only packet associated with ship changing, getting this
    doesn't necessarily mean that they changed frequency too.
*/
public class FrequencyShipChange extends SubspaceEvent {

    //Variable Declarations
    short           m_playerID;
    short           m_freq;
    byte            m_shipType;

    /**
        Creates a new instance of FrequencyShipChange, this is called by
        GamePacketInterpreter when it receives the packet
        @param array the ByteArray containing the packet data
    */
    public FrequencyShipChange(ByteArray array) {
        m_shipType = array.readByte( 1 );
        m_playerID = array.readLittleEndianShort( 2 );
        m_freq = array.readLittleEndianShort( 4 );
    }

    /**
        This gets the id of the player that just changed ship/freq
        @return the id of the changing player
    */
    public short getPlayerID() {
        return m_playerID;
    }

    /**
        This gets the player's new ship type using intuitive numbering
        ie. 0=Spec, 1=Warbird, 2=Javelin, ...
        @return the type of ship the player is in
    */
    public byte getShipType() {
        return (byte)((m_shipType + 1) % 9);
    }

    /**
        This gets the player's new ship type using SS protocol numbering
        ie. 0=Warbird, 1=Javelin, ..., 8=Spec
        @return the type of ship the player is in
    */
    public byte getShipTypeRaw() {
        return m_shipType;
    }

    /**
        This gets the player's new frequency
        @return the frequency the player is on
    */
    public short getFrequency() {
        return m_freq;
    }
}
