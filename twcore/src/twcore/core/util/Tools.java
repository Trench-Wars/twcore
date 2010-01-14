package twcore.core.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import twcore.core.OperatorList;

/**
 * Cluttered but somewhat useful toolkit of common operations.  Should be referenced
 * statically only.
 */
public final class Tools {
    public static boolean debugging = true;
    public static String exceptionLogFilePath = null;

    private static boolean connectionLogEnabled = false;
    private static File connectionLogFile = new File("connection.log");

    private Tools() {
    	/* private default constructor to prevent accidentally instantiating this class */
    }

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
    public static LinkedList<String> linkedStringChopper( String input, char deliniator ){
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

    public static void printConnectionLog( String line ) {
        if(!connectionLogEnabled)
            return;

        try {
            FileWriter fileWriter = new FileWriter(connectionLogFile, true);
            fileWriter.write(line + "\n");
            fileWriter.close();
        } catch(IOException ioe) {
            System.err.println("Unable to write to connection log file. Please specify a correct file name in the setup.cfg : "+connectionLogFile);
            connectionLogEnabled = false;
            return;
        }
    }

    public static void setConnectionLog( boolean state , File connectionLogFile) {
        Tools.connectionLogFile = connectionLogFile;
        Tools.connectionLogEnabled = state;
    }

    /**
     * Does a spam (multiline) print to log.
     * @param msg
     */
    public static void spamLog( String[] msg ) {
        for( int i = 0; i<msg.length; i++ )
            printLog( msg[i] );
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
            	FileWriter fileWriter = new FileWriter( exceptionLogFilePath, true );
            	BufferedWriter buffer = new BufferedWriter( fileWriter );
                PrintWriter out = new PrintWriter( buffer );
                out.println();
                out.println( "-----------------" );
                out.println( getTimeStamp() );
                out.println();
                e.printStackTrace( out );
                
                out.close();
                buffer.close();
                fileWriter.close();
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
            	FileWriter writer = new FileWriter( exceptionLogFilePath, true );
            	BufferedWriter bufferWriter = new BufferedWriter( writer );
                PrintWriter out = new PrintWriter( bufferWriter );
                
                
                out.println();
                out.println( "-----------------" );
                out.println( getTimeStamp() + " " + note );
                out.println();
                e.printStackTrace( out );
                
                
                out.close();
                bufferWriter.close();
                writer.close();
            } catch( IOException ioe ){
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Our signature timestamp.  (07 May 2007 14:12:32)
     * @return Formatted timestamp
     */
    public static String getTimeStamp(){
		return new SimpleDateFormat("dd MMM yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    /**
     * Returns ship name based on number provided as found in-game (and not the packet).
     * 1-8 are in-game ships, 0 is spec, and 9+ is unknown.
     * @param shipNumber Number of ship to identify
     * @return String containing name of ship
     */
    public static String shipName( int shipNumber ){
        switch( shipNumber ){
            case Tools.Ship.SPECTATOR:
                return "Spectator";
            case Tools.Ship.WARBIRD:
                return "Warbird";
            case Tools.Ship.JAVELIN:
                return "Javelin";
            case Tools.Ship.SPIDER:
                return "Spider";
            case Tools.Ship.LEVIATHAN:
                return "Leviathan";
            case Tools.Ship.TERRIER:
                return "Terrier";
            case Tools.Ship.WEASEL:
                return "Weasel";
            case Tools.Ship.LANCASTER:
                return "Lancaster";
            case Tools.Ship.SHARK:
                return "Shark";
            case 9:
            	return "Any ship";
            default:
                return "UFO";
        }
    }

    /**
     * Returns short ship name based on number provided as found in-game (and not the packet).
     * 1-8 are in-game ships, 0 is spec, and 9+ is unknown.  The names returned are the "slang"
     * terms used in TW, and not the official names of the ships.  In particular ship 6 ("X")
     * doesn't make sense except in TW.
     * @param shipNumber Number of ship to identify
     * @return String containing name of ship
     */
    public static String shipNameSlang( int shipNumber ){
        switch( shipNumber ){
            case Tools.Ship.SPECTATOR:
                return "Spec";
            case Tools.Ship.WARBIRD:
                return "WB";
            case Tools.Ship.JAVELIN:
                return "Jav";
            case Tools.Ship.SPIDER:
                return "Spid";
            case Tools.Ship.LEVIATHAN:
                return "Levi";
            case Tools.Ship.TERRIER:
                return "Terr";
            case Tools.Ship.WEASEL:
                return "X";
            case Tools.Ship.LANCASTER:
                return "Lanc";
            case Tools.Ship.SHARK:
                return "Shark";
            default:
                return "UFO";
        }
    }

    public static String staffName( int operatorLevel ) {
    	switch( operatorLevel ) {
    		case OperatorList.OWNER_LEVEL: return "Owner";
    		case OperatorList.SYSOP_LEVEL: return "System Operator";
    		case OperatorList.SMOD_LEVEL: return "Super Moderator";
    		case OperatorList.DEV_LEVEL: return "Developer";
    		case OperatorList.HIGHMOD_LEVEL: return "High Moderator";
    		case OperatorList.MODERATOR_LEVEL: return "Moderator";
    		case OperatorList.ER_LEVEL: return "Event Referee";
    		case OperatorList.OUTSIDER_LEVEL: return "Outsider";
    		case OperatorList.BOT_LEVEL: return "Bot";
    		case OperatorList.PLAYER_LEVEL: return "Player";
    		default: return "Unknown";
    	}
    }

    public static String staffNameShort( int operatorLevel ) {
    	switch( operatorLevel ) {
			case OperatorList.OWNER_LEVEL: return "Owner";
			case OperatorList.SYSOP_LEVEL: return "Sysop";
			case OperatorList.SMOD_LEVEL: return "SMod";
			case OperatorList.DEV_LEVEL: return "Dev";
			case OperatorList.HIGHMOD_LEVEL: return "HMod";
			case OperatorList.MODERATOR_LEVEL: return "Mod";
			case OperatorList.ER_LEVEL: return "ER";
			case OperatorList.OUTSIDER_LEVEL: return "Outs";
			case OperatorList.BOT_LEVEL: return "Bot";
			case OperatorList.PLAYER_LEVEL: return "Player";
			default: return "?";
		}
    }

    /**
     * Returns true if a String is all digits.
     * @param str String to determine if all digits.
     * @return True if String is all digits.
     */
    public static boolean isAllDigits( String str ){
        if( str == null || str.equals("") )
            return false;
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
     * Returns true if a String is composed completely of 0 and 1 and its length is a multiple of 4.
     * @param s String to determine if binary.
     * @return True if String is binary. Else false.
     */
    public static boolean isBinary( String s ){
    	try {

            for( int i = 0; i < s.length(); i++ ){
            	if(java.lang.Character.digit(s.charAt(i), 2) == -1)
            		return false;
            }
            if( s.length() % 4 != 0)
            	return false;
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }

    /**
     * Add slashes to a String as is required for a database query. (buggy)
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

    /**
     * Adds backslashes to a String as is required for a database query.
     * A backslash is inserted before these chars: ' " \
     * @param str String to process
     * @return String with backslashes inserted
     */
    public static String addSlashes(String str) {
    	int len = str.length();
    	StringBuilder sb = new StringBuilder(len * 2);

    	for(int i = 0; i < len; i++) {
    		char c = str.charAt(i);
    		switch(c) {
    			case '\'': sb.append("\\\'"); break;
    			case '\"': sb.append("\\\""); break;
    			case '\\': sb.append("\\\\"); break;
    			default: sb.append(c);
    		}
    	}
    	return sb.toString();
    }



    /**
     * Recursively searches for a file in directory.  Null if the file can't be found.
     * @param directory Directory to begin recursive search
     * @param fileName Name of file to search for
	 * @return a file if found, null if not found
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
        if( fragment == null )
            fragment = "";
        if(fragment.length() > length)
            fragment = fragment.substring(0,length);
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
        if( fragment == null )
            fragment = "";
        int curLength = fragment.length(),
            startPos = (length / 2) - (curLength/2);
        String result = "";

        for (int i=0; i < startPos; i++) result = result + padding;
        result = result + fragment;
        for (int j=result.length(); j < length; j++) result = result + padding;

        return result;
    }

    /**
     * Aligns a String to the right over a given length and which characters to fill with.
     *
     * @param fragment
     * @param length
     * @param padding
     * @return
     */
    public static String rightString (String fragment, int length, char padding) {
    	if( fragment == null )
            fragment = "";
        if(fragment.length() > length)
            fragment = fragment.substring(length,-length);
        else {
            for(int i=fragment.length();i<length;i++)
                fragment = padding + fragment;
        }
        return fragment;
    }

    /**
     * Aligns a String to the right over a given number of characters, padded on the right with spaces.
     *
     * @param fragment
     * @param length
     * @return
     */
    public static String rightString (String fragment, int length) {
    	return rightString(fragment, length, ' ');
    }


    /**
     * Based on a date given in milliseconds, return a String describing the
     * difference between that time and the present time, such as:<p>
     * <code>  2 days, 1 hour, 31 minutes and 45 seconds.</code><p>
     * If abbrev is set to true, then the String is much less verbose:<p>
     *  <code>  3d:14h:30m:21s</code>
     * @param dateInMillis A date given in milliseconds, can be either in future or past
     * @param abbrev Whether or not to abbreviate the time string.
     * @return
     */
    public static String getTimeDiffString( long dateInMillis, boolean abbrev ) {
    	long diffTime = 0;

    	if(dateInMillis <= new Date().getTime())	// timestamp is in past
    		diffTime = Math.round((new Date().getTime() - dateInMillis )/1000);
    	else	// timestamp is in future
    		diffTime = Math.round((dateInMillis - new Date().getTime())/1000);

        String response = new String();
        if(diffTime > (24*60*60)) {   // Days
            int days = Math.round(diffTime / (24*60*60));
            diffTime = diffTime - (days * (24*60*60));
            if( abbrev )
                response += days + "d:";
            else
                response += days+" day" + (days==1?"":"s") +", ";
        }
        if(diffTime > (60*60)) {    // Hours
            int hours = Math.round(diffTime / (60*60));
            diffTime = diffTime - (hours * (60*60));
            if( abbrev )
                response += hours + "h:";
            else
            response += hours+" hour" + (hours==1?"":"s") +", ";
        }
        if(diffTime > 60) {         // Minutes
            int minutes = Math.round(diffTime / 60);
            diffTime = diffTime - (minutes * 60);
            if( abbrev )
                response += minutes + "m:";
            else
            response += minutes+" minute" + (minutes==1?"":"s");
        }
        if( abbrev ) {
            response += diffTime + "s";
        } else {
            if( diffTime != 0 ) {
                if( !response.equals("") )
                    response += " and ";
                response += diffTime + " second" + (diffTime==1?"":"s");
            } else {
                if( !response.equals("") )
                    response += " exactly";
                else
                    response = "0 seconds";
            }
        }
        return response;
    }

    // *** ENUMS ***

    // Prizes
    public static class Prize {
        /**1*/  public static final int RECHARGE = 1;
        /**2*/  public static final int ENERGY = 2;
        /**3*/  public static final int ROTATION = 3;
        /**4*/  public static final int STEALTH = 4;
        /**5*/  public static final int CLOAK = 5;
        /**6*/  public static final int XRADAR = 6;
        /**7*/  public static final int WARP = 7;
        /**8*/  public static final int GUNS = 8;
        /**9*/  public static final int BOMBS = 9;
        /**10*/ public static final int BOUNCING_BULLETS = 10;
        /**11*/ public static final int THRUST = 11;
        /**12*/ public static final int TOPSPEED = 12;
        /**13*/ public static final int FULLCHARGE = 13;
        /**-13*/public static final int ENERGY_DEPLETED = -13;          // Puts energy to 0
        /**14*/ public static final int ENGINE_SHUTDOWN = 14;           // Shuts down engines for Prize:EngineShutdownTime
        /**-14*/public static final int ENGINE_SHUTDOWN_EXTENDED = -14; // Shuts down engines for Prize:EngineShutdownTime * 3
        /**15*/ public static final int MULTIFIRE = 15;
        /**16*/ public static final int PROXIMITY = 16;
        /**17*/ public static final int SUPER = 17;
        /**18*/ public static final int SHIELDS = 18;
        /**19*/ public static final int SHRAPNEL = 19;
        /**20*/ public static final int ANTIWARP = 20;
        /**21*/ public static final int REPEL = 21;
        /**22*/ public static final int BURST = 22;
        /**23*/ public static final int DECOY = 23;
        /**24*/ public static final int THOR = 24;
        /**25*/ public static final int MULTIPRIZE = 25;
        /**26*/ public static final int BRICK = 26;
        /**27*/ public static final int ROCKET = 27;
        /**28*/ public static final int PORTAL = 28;
    }

    // Sounds
    public static class Sound {
        /**1*/  public static final int BEEP1 = 1;
        /**2*/  public static final int BEEP2 = 2;
        /**3*/  public static final int NOT_DEALING_WITH_ATT = 3;
        /**4*/  public static final int VIOLENT_CONTENT = 4;
        /**5*/  public static final int HALLELUJAH = 5;
        /**6*/  public static final int REAGAN = 6;
        /**7*/  public static final int INCONCEIVABLE = 7;
        /**8*/  public static final int WINSTON_CHURCHILL = 8;
        /**9*/  public static final int LISTEN_TO_ME = 9;
        /**10*/ public static final int CRYING = 10;
        /**11*/ public static final int BURP = 11;
        /**12*/ public static final int ORGASM_DO_NOT_USE = 12;
        /**13*/ public static final int SCREAM = 13;
        /**14*/ public static final int FART1 = 14;
        /**15*/ public static final int FART2 = 15;
        /**16*/ public static final int PHONE_RING = 16;
        /**17*/ public static final int UNDER_ATTACK = 17;
        /**18*/ public static final int GIBBERISH = 18;
        /**19*/ public static final int CROWD_OOO = 19;
        /**20*/ public static final int CROWD_GEE = 20;
        /**21*/ public static final int CROWD_OHH = 21;
        /**22*/ public static final int CROWD_AWW = 22;
        /**23*/ public static final int GAME_SUCKS = 23;
        /**24*/ public static final int SHEEP = 24;
        /**25*/ public static final int CANT_LOG_IN = 25;
        /**26*/ public static final int BEEP3 = 26;
        /**100*/public static final int START_MUSIC = 100;
        /**101*/public static final int STOP_MUSIC = 101;
        /**102*/public static final int PLAY_MUSIC_ONCE = 102;
        /**103*/public static final int VICTORY_BELL = 103;
        /**104*/public static final int GOAL = 104;
        /**104*/public static final int GOGOGO = 104; // Just in case :P
        /**1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 102, 103, 104*/
        public static final int[] allowedSounds = { 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 102, 103, 104 };
        /**6, 12, 26, 100, 101*/
        public static final int[] restrictedSounds = { 6, 12, 26, 100, 101 };
        /**
         * Disabled sounds( 6, 12, 26, 100, 101 )
         * @param sound The sound to check
         * @return Whether or not the sound is allowed
         */
        public static boolean isAllowedSound(int sound){
            if(sound < 1 || (sound > 25 && sound < 102) || sound > 104)
                return false;
            else if(sound == REAGAN || sound == ORGASM_DO_NOT_USE)
                return false;
            else return true;
        }

        /**
         * Disabled sounds( 6, 12, 26, 100, 101 )
         * @param soundString The sound to check
         * @return -1 if the sound is not allowed. Else return the sound number.
         */
        public static int isAllowedSound(String soundString){
            try{
                int sound = Integer.parseInt(soundString);
                if(sound < 1 || (sound > 25 && sound < 102) || sound > 104)
                    return -1;
                else if(sound == REAGAN || sound == ORGASM_DO_NOT_USE)
                    return -1;
                else return sound;
            }catch(NumberFormatException e){
                return -1;
            }
        }
    }

    // Ships (not to be confused with numbering system used in Ship, which is for internal/packet use only)
    public static class Ship {
        /**0*/public static final int SPECTATOR = 0;
        /**1*/public static final int WARBIRD = 1;
        /**2*/public static final int JAVELIN = 2;
        /**3*/public static final int SPIDER = 3;
        /**4*/public static final int LEVIATHAN = 4;
        /**5*/public static final int TERRIER = 5;
        /**6*/public static final int WEASEL = 6;
        /**7*/public static final int LANCASTER = 7;
        /**8*/public static final int SHARK = 8;
    }

    // Time defines for conversion to milliseconds.  If this is to be found somewhere else
    // more handy, that can be used instead -- merely saw a static reference to the number
    // representing "one day" in StaffBot and thought it was a little sloppy.
    public static class TimeInMillis {
        /**1000*/     public static final int SECOND = 1000;
        /**60000*/    public static final int MINUTE = 60000;
        /**3600000*/  public static final int HOUR   = 3600000;
        /**86400000*/ public static final int DAY    = 86400000;
        /**604800000*/public static final int WEEK   = 604800000;
    }

    /** Weapons - These are the <b>STANDARD</b> weapon speed settings in SSCU Trench Wars
     */
    public static class Weapon {
        /**5000.0*/ public static final double WARBIRD_BULLET_SPEED = 5000.0;
    	/**-900.0*/ public static final double JAVELIN_BULLET_SPEED = -900.0;
    	/**4000.0*/ public static final double SPIDER_BULLET_SPEED = 4000.0;
    	/**1000.0*/ public static final double LEVIATHAN_BULLET_SPEED = 1000.0;
    	/**1300.0*/ public static final double TERRIER_BULLET_SPEED = 1300.0;
    	/**800.0*/  public static final double WEASEL_BULLET_SPEED = 800.0;
    	/**3650.0*/ public static final double LANCASTER_BULLET_SPEED = 3650.0;
    	/**1.0*/    public static final double SHARK_BULLET_SPEED = 1.0;

    	/**4000.0*/ public static final double WARBIRD_BOMB_SPEED = 4000.0;
    	/**2250.0*/ public static final double JAVELIN_BOMB_SPEED = 2250.0;
    	/**1.0*/    public static final double SPIDER_BOMB_SPEED = 1.0;
    	/**4000.0*/ public static final double LEVIATHAN_BOMB_SPEED = 4000.0;
    	/**1.0*/    public static final double TERRIER_BOMB_SPEED = 1.0;
    	/**30000.0*/public static final double WEASEL_BOMB_SPEED = 30000.0;
    	/**1.0*/    public static final double LANCASTER_BOMB_SPEED = 1.0;
    	/**1.0*/    public static final double SHARK_BOMB_SPEED = 1.0;

    }

}
