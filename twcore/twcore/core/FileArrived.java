package twcore.core;

 /**
  * Event called when the bot finishes the download of a file.
  * <br><font size="+2"><b>FileArrived: <font color="blue">S2C 0x10</b></font></font>
  * <table border="1" cellspacing="0">
  * <tr> <th>Offset</th><th>Length</th><th>Description</th> </tr>
  * <tr> <td>0</td> <td>1</td> <td>Type Byte</td> </tr>
  * <tr> <td>1</td> <td>16</td> <td>Name of File</td> </tr>
  * <tr> <td>17</td> <td>?</td> <td>File Data</td> </tr>
  * </table><br>
  * If the file name isn't specified, it's the news.txt file and has to be
  * decompressed. All other files are sent uncompressed
  */
public class FileArrived extends SubspaceEvent
{
    //Variable Declarations
    String fileName;

    /**
     * Creates a new instance of FileArrived, this is called by GamePacketInterpreter
     * when it recieves the packet.
     * @param array the ByteArray containing the packet data
     */
    public FileArrived(ByteArray array)
    {
        m_byteArray = array;
        m_eventType = EventRequester.FILE_ARRIVED; //sets the event type in the superclass

        fileName = array.readString(0, array.size());
    }

    /**
     * This gets the file name from the file that just downloaded
     * @return the file name of the file
     */
    public String getFileName()
    {
        return new String(fileName);
    }
}
