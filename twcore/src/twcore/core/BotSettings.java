package twcore.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import twcore.core.util.Tools;

/**
 * Reads, stores, and allows access to the settings in a bot's configuration
 * file.
 */
public class BotSettings {

    private String              	m_fileName;     // Name & location of the CFG
    private HashMap<String, String> m_data;         // CFG field -> value mapping

    /**
     * Constructs a BotSettings object with no data.
     */
    public BotSettings(){
        m_data = new HashMap<String, String>();
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

        m_data = new HashMap<String, String>();
        try{
            String          key;
            String          line;
            String          value;
            int             equalsIndex;
            FileReader fileReader = new FileReader( file );
            BufferedReader  in = new BufferedReader( fileReader);

            while( (line = in.readLine()) != null ){
                if( line.length() != 0 ){
                    char firstChar = line.trim().charAt( 0 );
                    if( !(firstChar == '#' || firstChar == '[' || firstChar == '/' )){
                        equalsIndex = line.indexOf( '=' );

                        // Allow // comments on the same line as data to make it easier to document CFGs
                        int commentIndex = line.indexOf(" //");
                        if( commentIndex != -1 ) {
                            line = line.substring(0,commentIndex);
                            line.trim();
                        }

                        if( equalsIndex != -1 ){
                            key = line.substring( 0, equalsIndex ).toLowerCase();
                            value = line.substring( equalsIndex + 1 );
                            m_data.put( key, value );
                        }
                    }
                }
            }

            in.close();
            fileReader.close();
        } catch(FileNotFoundException fnfe) {
            Tools.printLog( "BotSettings configuration file ("+file.getName()+") not found: " + fnfe.getMessage());
        } catch(IOException ioe) {
            Tools.printLog( "BotSettings configuration file ("+file.getName()+") I/O exception: " + ioe.getMessage());
        }
    }

    /**
     * Overwrites default loaded data or adds new data.
     * @param keyName Name of the field to be replaced
     * @param data New data
     */
    public void put( String keyName, int data ){

        m_data.put( keyName.toLowerCase(), new String( "" + data ) );
    }

    /**
     * Overwrites default loaded data or adds new data.
     * @param keyName Name of the field to be replaced
     * @param data New data
     */
    public void put( String keyName, String data ){

        m_data.put( keyName.toLowerCase(), new String( data ) );
    }

    /**
     * Overwrites default loaded data or adds new data.
     * @param keyName Name of the field to be replaced
     * @param data New data
     */
    public void put( String keyName, double data ){

        m_data.put( keyName.toLowerCase(), new String( "" + data ) );
    }
    
    /**
     * Overwrites default loaded data or adds new data.
     * @param keyName Name of the field to be replaced
     * @param data New data
     */
    public void put( String keyName, long data ){

        m_data.put( keyName.toLowerCase(), new String( "" + data ) );
    }

    /**
     * Removes specified key.
     *
     * @param keyName
     */
    public void remove( String keyName ) {
        m_data.remove(keyName.toLowerCase());
    }

    /**
     * Returns data associated with a specified field.  getInt attempts to
     * parse the key as an Integer, and then return the associated data.
     * If the key has no value or is not a valid key, a 0 is returned.
     * @param keyName Field to fetch from
     * @return Data associated with the specified field; 0 if not found
     */
    public int getInt( String keyName ){
        String      value = m_data.get( keyName.toLowerCase() );

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
        String      value = m_data.get( keyName.toLowerCase() );

        if( value != null ){
            return Integer.valueOf(value);
        } else {
            return null;
        }
    }
    
    /**
     * Returns data associated with a specified field. 
     * @param keyName Field to fetch from
     * @param delimiter Delimiter to parse integers by from field
     * @return Data associated with a specified field; null if invalid
     */
    public int[] getIntArray( String keyName, String delimiter ){
    	String		value = m_data.get( keyName.toLowerCase() );
    	if(value != null){
    		String[] values = value.split(delimiter);
    		int[] intArray = new int[values.length];
    		try{
    			for(int i=0;i<values.length;i++)
    				intArray[i] = Integer.parseInt(values[i]);
    		}catch(NumberFormatException e){
    			return null;
    		}
    		return intArray;
    	} else {
    		return null;
    	}
    }

    /**
     * Returns data associated with a specified field.
     *
     * @param keyName Field to fetch from
     * @return Data associated with the specified field or NULL if not found
     */
    public String getString( String keyName ){
        String      value = m_data.get( keyName.toLowerCase() );

        if( value != null ){
            return new String( value );
        } else {
            return null;
        }
    }

    /**
     * Returns data associated with a specified field.  If the String is not found,
     * an empty string is returned rather than a null String.
     *
     * @param keyName Field to fetch from
     * @return Data associated with the specified field, or an empty String if not found
     */
    public String getNonNullString( String keyName ){
        String      value = m_data.get( keyName.toLowerCase() );

        if( value != null ){
            return new String( value );
        } else {
            return "";
        }
    }

    /**
     * Returns data associated with a specified field.
     * @param keyName Field to fetch from
     * @return Data associated with the specified field
     */
    public double getDouble( String keyName ){
        String      value = m_data.get( keyName.toLowerCase() );

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
            FileReader reader = new FileReader( m_fileName );
            BufferedReader  in = new BufferedReader( reader );
            FileWriter writer = new FileWriter(m_fileName + ".tmp");
            BufferedWriter bufferWriter = new BufferedWriter( writer );
            PrintWriter     out = new PrintWriter( bufferWriter );
            HashSet <String> existingKeys = new HashSet<String>();

            while( (line = in.readLine()) != null ){

                if( line.length() != 0 ){
                    char firstChar = line.trim().charAt( 0 );
                    if( !(firstChar == '#' || firstChar == '[' )){
                        equalsIndex = line.indexOf( '=' );

                        if( equalsIndex != -1 ){
                            key = line.substring( 0, equalsIndex);
                            value = this.getString(key.toLowerCase());
                            if(value != null) {
                                line = key + "=" + value;
                                existingKeys.add(key.toLowerCase());
                            } else {
                                line = null;
                            }
                        }
                    }
                }

                if(line != null)
                    out.println(line);
            }

            // Write any new key/values to the end of the .cfg file
            for(String newKey:m_data.keySet()) {
                if(!existingKeys.contains(newKey.toLowerCase())) {
                    out.println(newKey+"="+m_data.get(newKey));
                }
            }
            in.close();
            reader.close();
            
            out.close();
            bufferWriter.close();
            writer.close();

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
