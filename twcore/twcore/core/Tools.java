package twcore.core;

/*
  * Tools.java
  *
  * Created on February 1, 2002, 12:27 PM
  */

/**
 *
 * @author  harvey
 */
import java.util.*;
import java.io.*;
import java.text.*;

public class Tools {
    public static boolean debugging = true;
    public static String exceptionLogFilePath = null;
    public static String[] stringChopper( String input, char deliniator ){
        LinkedList list = new LinkedList();

        int nextSpace = 0;
        int previousSpace = 0;

        if( input == null ){
            return null;
        }

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
        if (stuff.length() > 0) list.add( stuff );

        return (String[])list.toArray(new String[list.size()]);
    }
    
    public static String[] cleanStringChopper ( String input, char delimiter ) {
        ArrayList list = new ArrayList();
        int startpos = 0;
        
        for (int i=0; i<input.length(); i++) {
            if (input.charAt(i) == delimiter) {
                list.add(input.substring(startpos, i));
                startpos = i + 1;
            }
        }
        
        if ((startpos != input.length()-1) || startpos == 0) list.add(input.substring(startpos));
        
        return (String[])list.toArray(new String[list.size()]);
    }
    
    public static LinkedList linkedStringChopper( String input, char deliniator ){
        LinkedList list = new LinkedList();

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

    public static void printLog( String value ){
        String output = getTimeStamp() + " " + value;
        System.out.println( output );

    }

    public static void printLog( String value, PrintWriter writer ){
        String output = getTimeStamp() + " " + value;
        writer.println( output );
    }

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

    public static String getTimeStamp(){
        Calendar calendar = Calendar.getInstance();
        String stamp = "";
        switch( calendar.get( Calendar.DAY_OF_WEEK )){
            case Calendar.MONDAY:
                stamp += "Mon ";
                break;
            case Calendar.TUESDAY:
                stamp += "Tue ";
                break;
            case Calendar.WEDNESDAY:
                stamp += "Wed ";
                break;
            case Calendar.THURSDAY:
                stamp += "Thu ";
                break;
            case Calendar.FRIDAY:
                stamp += "Fri ";
                break;
            case Calendar.SATURDAY:
                stamp += "Sat ";
                break;
            case Calendar.SUNDAY:
                stamp += "Sun ";
                break;
        }
        DecimalFormat format = new DecimalFormat( "00" );

        stamp += format.format( calendar.get( Calendar.MONTH )) + "-";
        stamp += format.format( calendar.get( Calendar.DAY_OF_MONTH )) + "-";
        stamp += format.format( calendar.get( Calendar.YEAR )) + " ";
        stamp += format.format( calendar.get( Calendar.HOUR )) + ":";
        stamp += format.format( calendar.get( Calendar.MINUTE )) + ":";
        stamp += format.format( calendar.get( Calendar.SECOND ));
        return stamp;
    }

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
     * Returns true if a string is all digits.
     * @param str String to determine if all digits.
     * @return True if string is all digits.
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
    public static String addSlashesToString( String t) {
        String n = null;
        if (t != null) {
            n = "";
            for (int i = 0; i < t.length(); i++) {
                if (t.charAt(i) == '\'') n = n + "\\\'"; else
                    if (t.charAt(i) == '\"') n = n + "\\\\\""; else
                        if (t.charAt(i) == '\\') n = n + "\\\\\\\\"; else
                            n = n + t.charAt(i);
            };
        };
        return n;
    };
    //returns null if not found, a file if found
    public static File getRecursiveFileInDirectory( File directory, String fileName ){
        File[] files = directory.listFiles();
        for( int i = 0; i < files.length; i++ ){
            if( files[i].isDirectory() ){
                //System.out.println( "Reading Directory: " + files[i].getAbsolutePath() );
                File f = getRecursiveFileInDirectory( files[i], fileName );
                if( f != null ){
                    return f;
                }
                //System.out.println( "File not found in: " + files[i].getAbsolutePath() );
            } else {
                String filename = files[i].getName();
                if( filename.equals( fileName )){
                    //System.out.println( "Found: " + fileName );
                    return files[i];
                }
            }
        }
        return null;
    }

    public static String formatString( String fragment, int length ) {
        return formatString( fragment, length, " " );
    }

    public static String formatString( String fragment, int length, String padding ) {
        String line;
        if(fragment.length() > length)
            fragment = fragment.substring(0,length-1);
        else {
            for(int i=fragment.length();i<length;i++)
                fragment = fragment + padding;
        }
        return fragment;
    }

    public static String centerString (String fragment, int length) {
        return centerString(fragment, length, ' ');
    }

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
