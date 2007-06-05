package twcore.core.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

/**
 * Cluttered but somewhat useful toolkit of common operations.  Should be referenced
 * statically only.
 */
public class Tools {
    public static boolean debugging = true;
    public static String exceptionLogFilePath = null;

    /**
     * Chops a string into pieces around a given character.  Very similar to
     * String's split method, only slower and less powerful.  Nearly useless.
     * Also replaces ? and ! with whitespace, for who knows what reason.
     * @param input Input string
     * @param delimiter Character to split around
     * @return String array containing pieces
     */
    public static String[] stringChopper( String input, char delimiter ){
        LinkedList <String>list = new LinkedList<String>();

        int nextSpace = 0;
        int previousSpace = 0;

        if( input == null ){
            return null;
        }

        do{
            previousSpace = nextSpace;
            nextSpace = input.indexOf( delimiter, nextSpace + 1 );

            if ( nextSpace!= -1 ){
                String stuff = input.substring( previousSpace, nextSpace ).trim().toLowerCase();
                stuff=stuff.replace('?',' ').replace('!',' ');
                if( stuff!=null && !stuff.equals("") )
                    list.add( stuff );
            }

        }while( nextSpace != -1 );
        String stuff = input.substring( previousSpace ).toLowerCase();
        stuff=stuff.replace('?',' ').replace('!',' ').trim();
        if (stuff.length() > 0) list.add( stuff );

        return list.toArray(new String[list.size()]);
    }

    /**
     * Chops a string into pieces around a given character.  Very similar to
     * String's split method, only slower and less powerful.  Nearly useless.
     * @param input Input string
     * @param delimiter Character to split around
     * @return String array containing pieces
     */
    public static String[] cleanStringChopper ( String input, char delimiter ) {
        ArrayList <String>list = new ArrayList<String>();
        int startpos = 0;

        for (int i=0; i<input.length(); i++) {
            if (input.charAt(i) == delimiter) {
                list.add(input.substring(startpos, i));
                startpos = i + 1;
            }
        }

        if ((startpos != input.length()-1) || startpos == 0) list.add(input.substring(startpos));

        return list.toArray(new String[list.size()]);
    }

    /**
     * Chops a string into pieces around a given character.  Very similar to
     * String's split method, only slower and less powerful.  Nearly useless.
     * Also replaces ? and ! with whitespace, for who knows what reason.
     * @param input Input string
     * @param delimiter Character to split around
     * @return String array containing pieces
     */
    public static LinkedList linkedStringChopper( String input, char deliniator ){
        LinkedList <String>list = new LinkedList<String>();

        int nextSpace = 0;
        int previousSpace = 0;

        do{
            previousSpace = nextSpace;
            nextSpace = input.indexOf( deliniator, nextSpace + 1 );

            if ( nextSpace!= -1 ){
                String stuff = input.substring( previousSpace, nextSpace ).trim().toLowerCase();
                stuff=stuff.replace('?',' ').replace('!',' ');
                if( stuff!=null && !stuff.equals("") )
                    list.add( stuff );
            }

        }while( nextSpace != -1 );
        String stuff = input.substring( previousSpace ).toLowerCase();
        stuff=stuff.replace('?',' ').replace('!',' ').trim();
        list.add( stuff );

        return list;
    }

    /**
     * Prints to the log (console) a message with a preformatted timestamp.
     * For this to work one should redirect output to a file.
     * @param value Message
     */
    public static void printLog( String value ){
        String output = getTimeStamp() + "   " + value;
        System.out.println( output );

    }

    /**
     * Prints to specified PrintWriter a message with a preformatted timestamp.
     * @param value Message
     * @param writer Writer to use
     */
    public static void printLog( String value, PrintWriter writer ){
        String output = getTimeStamp() + "   " + value;
        writer.println( output );
    }

    /**
     * Print exception to the log (console) with formatted output and timestamp.
     * @param e Exception
     */
    public static void printStackTrace( Exception e ){
        if( !debugging ){
            printLog( "Warning! Exception message:" );
            System.out.println( "     " + e.getClass().getName() + ": "
            + e.getMessage() );
        } else {
            e.printStackTrace();
            if( exceptionLogFilePath == null ) return;
            try{
                PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( exceptionLogFilePath, true )));
                out.println();
                out.println( "-----------------" );
                out.println( getTimeStamp() );
                out.println();
                e.printStackTrace( out );
                out.close();
            } catch( IOException ioe ){
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Print exception to the log (console) with formatted output, timestamp,
     * and the specified note.
     * @param note Note to print
     * @param e Exception
     */
    public static void printStackTrace( String note, Exception e ){
        if( !debugging ){
            printLog( "Warning! " + note + ":" );
            System.out.println( "     " + e.getClass().getName() + ": "
            + e.getMessage() );
        } else {
            e.printStackTrace();
            if( exceptionLogFilePath == null ) return;
            try{
                PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( exceptionLogFilePath, true )));
                out.println();
                out.println( "-----------------" );
                out.println( getTimeStamp() + " " + note );
                out.println();
                e.printStackTrace( out );
                out.close();
            } catch( IOException ioe ){
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Our signature timestamp.
     * @return Formatted timestamp
     */
    public static String getTimeStamp(){
		return new SimpleDateFormat("dd MMM yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    /**
     * Returns ship name based on number provided, as found in-game (and not the packet).
     * 1-8 are in-game ships, 0 is spec, and 9+ is unknown.
     * @param shipNumber Number of ship to identify
     * @return String containing name of ship
     */
    public static String shipName( int shipNumber ){
        switch( shipNumber ){
            case 0:
                return "Spectator";
            case 1:
                return "Warbird";
            case 2:
                return "Javelin";
            case 3:
                return "Spider";
            case 4:
                return "Leviathan";
            case 5:
                return "Terrier";
            case 6:
                return "Weasel";
            case 7:
                return "Lancaster";
            case 8:
                return "Shark";
            default: case 9:
                return "UFO";
        }
    }

    /**
     * Returns true if a String is all digits.
     * @param str String to determine if all digits.
     * @return True if String is all digits.
     */
    public static boolean isAllDigits( String str ){
        try {
            for( int i = 0; i < str.length(); i++ ){
                if( !Character.isDigit( str.charAt( i ))){
                    return false;
                }
            }
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }

    /**
     * Add slashes to a String as is required for a database query.
     * @param t String to format
     * @return String formatted with slashes
     */
    public static String addSlashesToString( String t) {
        String n = null;
        if (t != null) {
            n = "";
            for (int i = 0; i < t.length(); i++) {
                if (t.charAt(i) == '\'') n = n + "\\\'"; else
                    if (t.charAt(i) == '\"') n = n + "\\\\\""; else
                        if (t.charAt(i) == '\\') n = n + "\\\\\\\\"; else
                            n = n + t.charAt(i);
            }
        }
        return n;
    }

    //returns null if not found, a file if found

    /**
     * Recursively searches for a file in directory.  Null if the file can't be found.
     * @param directory Directory to begin recursive search
     * @param fileName Name of file to search for
     */
    public static File getRecursiveFileInDirectory( File directory, String fileName ){
        File[] files = directory.listFiles();
        for( int i = 0; i < files.length; i++ ){
            if( files[i].isDirectory() ){
                File f = getRecursiveFileInDirectory( files[i], fileName );
                if( f != null ){
                    return f;
                }
            } else {
                String filename = files[i].getName();
                if( filename.equals( fileName )){
                    return files[i];
                }
            }
        }
        return null;
    }

    /**
     * Formats a String to a particular length by padding it with spaces at its end.
     * @param fragment Fragment to pad
     * @param length Length to pad to
     * @return Padded string
     */
    public static String formatString( String fragment, int length ) {
        return formatString( fragment, length, " " );
    }

    /**
     * Formats a String to a particular length by padding it with a provided character
     * at its tail.
     * @param fragment Fragment to pad
     * @param length Length to pad to
     * @param padding String to pad with (usually 1 character)
     * @return Padded string
     */
    public static String formatString( String fragment, int length, String padding ) {
        if(fragment.length() > length)
            fragment = fragment.substring(0,length-1);
        else {
            for(int i=fragment.length();i<length;i++)
                fragment = fragment + padding;
        }
        return fragment;
    }

    /**
     * Center a String over a given number of characters, padded on the left
     * and right with spaces.
     * @param fragment Fragment to center
     * @param length Total length of returned string
     * @return Centered string
     */
    public static String centerString (String fragment, int length) {
        return centerString(fragment, length, ' ');
    }

    /**
     * Center a String over a given number of characters, padded on the left
     * and right with the provided character
     * @param fragment Fragment to center
     * @param length Total length of returned string
     * @param padding Character to pad with
     * @return Centered string
     */
    public static String centerString (String fragment, int length, char padding) {
        int curLength = fragment.length(),
            startPos = (length / 2) - (curLength/2);
        String result = "";

        for (int i=0; i < startPos; i++) result = result + padding;
        result = result + fragment;
        for (int j=result.length(); j < length; j++) result = result + padding;

        return result;
    }
}
