/*
 * BotSettings.java
 *
 * Created on January 8, 2002, 11:02 PM
 */

/**
 *
 * @author  harvey
 */
package twcore.core;

import java.io.*;
import java.util.*;
public class BotSettings {
    
    private String              m_fileName;
    private HashMap             m_data;
    //private javax.swing.Timer   m_backupTimer;
    
    /** Creates a new instance of PersistantStorage */
    public BotSettings(){
        m_data = new HashMap();
    }
    
    public BotSettings(String fileName) {
        this( new File( fileName ));
    }
    
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
    
    public void put( String keyName, int data ){
        
        m_data.put( keyName.toLowerCase(), new String( "" + data ) );
    }
    
    public void put( String keyName, String data ){
        
        m_data.put( keyName.toLowerCase(), new String( data ) );
    }
    
    public void put( String keyName, double data ){
        
        m_data.put( keyName.toLowerCase(), new String( "" + data ) );
    }
    
    public int getInt( String keyName ){
        String      value = (String)m_data.get( keyName.toLowerCase() );
        
        if( value != null ){
            return Integer.valueOf( value ).intValue();
        } else {
            return 0;
        }
    }
    public String getString( String keyName ){
        String      value = (String)m_data.get( keyName.toLowerCase() );
        
        if( value != null ){
            return new String( value );
        } else {
            return null;
        }
    }
    public double getDouble( String keyName ){
        String      value = (String)m_data.get( keyName.toLowerCase() );
        
        if( value != null ){
            return Double.valueOf( value ).doubleValue();
        } else {
            return 0;
        }
    }
    
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
    };
}
