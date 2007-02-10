package twcore.core;

import java.io.*;
import java.util.*;

import twcore.core.util.Tools;

/**
 * Reads, stores, and allows access to the settings in a bot's configuration
 * file.
 */
public class BotSettings {

    private String              m_fileName;     // Name & location of the CFG
    private HashMap             m_data;         // CFG field -> value mapping

    /**
     * Constructs a BotSettings object with no data.
     */
    public BotSettings(){
        m_data = new HashMap();
    }

    /**
     * Constructs a BotSettings object with the data from a properly formed
     * configuration file.  Wrapper for BotSettings(File)
     * @param fileName Text name and location of configuration file
     */
    public BotSettings(String fileName) {
        this( new File( fileName ));
    }

    /**
     * Constructs a BotSettings object with the data from a properly formed
     * configuration file.
     * @param file Reference object to properly formed configuration file
     */
    public BotSettings( File file ) {
        m_fileName = file.getPath();

        m_data = new HashMap();
        try{
            String          key;
            String          line;
            String          value;
            int             equalsIndex;
            BufferedReader  in = new BufferedReader( new FileReader( file ));

            while( (line = in.readLine()) != null ){

                if( line.length() != 0 ){
                    char firstChar = line.trim().charAt( 0 );
                    if( !(firstChar == '#' || firstChar == '[' )){
                        equalsIndex = line.indexOf( '=' );

                        if( equalsIndex != -1 ){
                            key = line.substring( 0, equalsIndex ).toLowerCase();
                            value = line.substring( equalsIndex + 1 );
                            m_data.put( key, value );
                        }
                    }
                }
            }

            in.close();
        }catch(Exception e){
            Tools.printLog( "Failed to read file to memory: " + file.getName() );
        }
    }

    /**
     * Overwrites default loaded data with new data.
     * @param keyName Name of the field to be replaced
     * @param data New data
     */
    public void put( String keyName, int data ){

        m_data.put( keyName.toLowerCase(), new String( "" + data ) );
    }

    /**
     * Overwrites default loaded data with new data.
     * @param keyName Name of the field to be replaced
     * @param data New data
     */
    public void put( String keyName, String data ){

        m_data.put( keyName.toLowerCase(), new String( data ) );
    }

    /**
     * Overwrites default loaded data with new data.
     * @param keyName Name of the field to be replaced
     * @param data New data
     */
    public void put( String keyName, double data ){

        m_data.put( keyName.toLowerCase(), new String( "" + data ) );
    }

    /**
     * Returns data associated with a specified field.
     * @param keyName Field to fetch from
     * @return Data associated with the specified field
     */
    public int getInt( String keyName ){
        String      value = (String)m_data.get( keyName.toLowerCase() );

        if( value != null ){
            return Integer.valueOf( value ).intValue();
        } else {
            return 0;
        }
    }

    /**
     * Returns data associated with a specified field.
     * @param keyName Field to fetch from
     * @return Data associated with the specified field
     */
    public Integer getInteger( String keyName ){
        String      value = (String)m_data.get( keyName.toLowerCase() );

        if( value != null ){
            return Integer.valueOf(value);
        } else {
            return null;
        }
    }

    /**
     * Returns data associated with a specified field.
     * @param keyName Field to fetch from
     * @return Data associated with the specified field
     */
    public String getString( String keyName ){
        String      value = (String)m_data.get( keyName.toLowerCase() );

        if( value != null ){
            return new String( value );
        } else {
            return null;
        }
    }

    /**
     * Returns data associated with a specified field.
     * @param keyName Field to fetch from
     * @return Data associated with the specified field
     */
    public double getDouble( String keyName ){
        String      value = (String)m_data.get( keyName.toLowerCase() );

        if( value != null ){
            return Double.valueOf( value ).doubleValue();
        } else {
            return 0;
        }
    }

    /**
     * Saves the data back to the file (use if altered manually during program
     * execution).
     * @return True if file save succeeds.
     */
    public boolean save() {
        try{
            String          key;
            String          line;
            String          value;
            int             equalsIndex;
            BufferedReader  in = new BufferedReader( new FileReader( m_fileName ));
            PrintWriter     out = new PrintWriter( new BufferedWriter ( new FileWriter(m_fileName + ".tmp")));

            while( (line = in.readLine()) != null ){

                if( line.length() != 0 ){
                    char firstChar = line.trim().charAt( 0 );
                    if( !(firstChar == '#' || firstChar == '[' )){
                        equalsIndex = line.indexOf( '=' );

                        if( equalsIndex != -1 ){
                            key = line.substring( 0, equalsIndex);
                            value = this.getString(key.toLowerCase());
                            line = key + "=" + value;
                        }
                    }
                }
                out.println(line);
            }
            in.close();
            out.close();

            File f = new File(m_fileName);
            if (f.exists()) f.delete();
            (new File(m_fileName + ".tmp")).renameTo(f);
            return true;
        } catch(Exception e){
            Tools.printLog( "Failed to write file: " + e.getMessage() );
            return false;
        }
    }
}
