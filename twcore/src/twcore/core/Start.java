package twcore.core;
import java.io.File;

import twcore.core.util.Tools;

/**
 * Loads the TWCore setup file and instantiates HubBot with appropriate login
 * and server information.
 */
public class Start {

    /**
     * Checks for a valid setup configuration file specified on the command line,
     * or if not specified, checks for the existence of setup.cfg.  Then calls
     * startHub to process the file.
     * @param args Cmdline args passed, if any
     */
    public static void main( String args[] ){

        if( args.length == 0 ){
            File        file = new File( "setup.cfg" );
            if( file.exists() ){
                startHub( file );
            } else {
                System.out.println( "Please specify the location of setup.cfg." );
                System.out.println( "Usage: java Start </path/to/twcore/setup.cfg>" );
            }
        } else if( args.length >= 1 ){
            if( args[0].endsWith( ".cfg" ) ){
                startHub( new File( args[0] ) );
            } else {
                System.out.println( "The file specified does not appear to be a valid config file.  Please verify and try again." );
            }
        }
    }

    /**
     * Attempts to start a copy of the hub, provided a valid configuration file.
     * @param setupFile File identified as a core setup file
     */
    static void startHub( File setupFile ){
        System.out.println();
        System.out.println("+-------------------------+");
        System.out.println("|     TWCore Startup      |");
        System.out.println("+-------------------------+");
        System.out.println();

        CoreData        coreData = new CoreData( setupFile );
        BotSettings     generalSettings = coreData.getGeneralSettings();

        Tools.exceptionLogFilePath = generalSettings.getString( "Exception Log" );
        Tools.debugging = generalSettings.getInt( "DebugOutput" ) != 0;

        /*try {
	    	//System.out.close();
	    	System.err.close();
	    	//File f1 = new File("currentOut.log");
	    	File f2 = new File("currentErr.log");
	    	//f1.delete();
	    	f2.delete();
	    	//f1.createNewFile();
	    	f2.createNewFile();
	    	//System.setOut(new PrintStream(new FileOutputStream(f1)));
	    	System.setErr(new PrintStream(new FileOutputStream(f2)));
	    } catch(Exception e) {}*/

        while( true ){
            System.out.println("=== Starting Hub ...   ===");
            Tools.printLog( "Attempting to connect to server at ss://" + coreData.getServerName() + ":" + coreData.getServerPort() );

            ThreadGroup     group = new ThreadGroup( "Main" );

            Class<? extends SubspaceBot>   roboClass = null;

            try {
                roboClass = ClassLoader.getSystemClassLoader().loadClass( "twcore.core.HubBot" ).asSubclass(SubspaceBot.class);
            } catch( ClassNotFoundException cnfe ){
                System.err.println( "Class not found: HubBot" );
                System.err.println( "Please verify that you have properly compiled using the bld script or your IDE.");
                System.exit( 1 );
            }

            Session kingBot =
                new Session(
                    coreData,
                    roboClass,
                    coreData.getGeneralSettings().getString( "Main Login" ),
                    coreData.getGeneralSettings().getString( "Main Password" ),
                    1,
                    group,
                    true );

            kingBot.start();

            try {
                while( kingBot.getBotState() == Session.STARTING ){
                    Thread.sleep( 5 );
                }

                while( kingBot.getBotState() != Session.NOT_RUNNING ){
                    Thread.sleep( 100 );
                }
                System.exit( 0 );
            } catch( Exception e ){
                Tools.printStackTrace( e );
                Tools.printLog( "An exception was encountered; now exiting." );
                System.exit( 1 );
            }
        }
    }
}
