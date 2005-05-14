package twcore.core;
import java.io.*;

public class Start {

    public static void main( String args[] ){

        File    config = null;
        if( args.length == 0 ){
            File        file = new File( "setup.cfg" );
            if( file.exists() ){
                startHub( file );
            } else {
                System.out.println( "Please specify the location of setup.cfg" );
                System.out.println( "Usage: java Start </path/to/twcore/setup.cfg>" );
                //System.out.println( "Usage: java Start </path/to/twcore/setup.cfg> <bottypes> ..." );
            }
        } else if( args.length == 1 ){
            if( args[0].endsWith( ".cfg" ) ){
                startHub( new File( args[0] ) );
            } else {
                System.out.println( "This is not a valid config file, please try again" );
            }
        } else if( args.length >= 2 ){
            if( args[0].endsWith( ".cfg" ) ){
                for( int i = 1; i < args.length; i++ ){
                    Tools.printLog( "Individually loading bot of type: " + args[i] );
                    //startHub( args[i], new File( args[0] ));
                    try {
                        Thread.sleep( 20000 );
                    } catch( InterruptedException ie ){
                    }
                }
            } else {
                System.out.println( "This setup file is not a valid config file, please try again" );
            }
        }
    }

    static void startHub( File setupFile ){
        CoreData        coreData = new CoreData( setupFile );
        BotSettings     generalSettings = coreData.getGeneralSettings();
        
        Tools.exceptionLogFilePath = generalSettings.getString( "Exception Log" );
        Tools.debugging = generalSettings.getInt( "DebugOutput" ) != 0;

        String          botName = generalSettings.getString( "Main Login" );
        String          botPassword = generalSettings.getString( "Main Password" );

        while( true ){
            Tools.printLog( "Attempting to connect to ss://" + coreData.getServerName() + ":" + coreData.getServerPort() );

            ThreadGroup     group = new ThreadGroup( "Main" );

            Class           roboClass = null;

            try {
                roboClass = ClassLoader.getSystemClassLoader().loadClass( "twcore.core.HubBot" );
            } catch( ClassNotFoundException cnfe ){
                System.err.println( "Class not found: HubBot" );
                System.exit( 1 );
            }

            Session kingBot =
                new Session(
                    coreData,
                    roboClass,
                    coreData.getGeneralSettings().getString( "Main Login" ),
                    coreData.getGeneralSettings().getString( "Main Password" ),
                    1,
                    group );

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
                Tools.printLog( "Exception encountered, exited." );
                System.exit( 1 );
            }
        }
    }
}
