package twcore.core;

public class FileArrived extends SubspaceEvent {

    String          fileName;

    public FileArrived( ByteArray array ){

        fileName = array.readString( 0, array.size() );
    }

    public String getFileName(){

        return new String( fileName );
    }
}
