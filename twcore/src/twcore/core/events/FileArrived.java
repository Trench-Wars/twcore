package twcore.core.events;

import twcore.core.util.ByteArray;

/**
    (S2C 0x10) Event called when the bot finishes the download of a file. <code><pre>
    +-----------------------------+
    | Offset  Length  Description |
    +-----------------------------+
    | 0       1       Type Byte   |
    | 1       16      Name of File|
    | 17      ?       File Data   |
    +-----------------------------+</code></pre>

    If the file name isn't specified, it's the news.txt file and has to be
    decompressed. All other files (except map?) are sent uncompressed
*/
public class FileArrived extends SubspaceEvent {

    //Variable Declarations
    String          fileName;

    /**
        Creates a new instance of FileArrived, this is called by GamePacketInterpreter
        when it recieves the packet.
        @param array the ByteArray containing the packet data
    */
    public FileArrived( ByteArray array ) {

        fileName = array.readString( 0, array.size() );
    }

    /**
        This gets the file name from the file that just downloaded
        @return the file name of the file
    */
    public String getFileName() {

        return new String( fileName );
    }
}
