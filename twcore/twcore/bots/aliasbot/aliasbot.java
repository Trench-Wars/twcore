/**
 * - player can reset their own name.
 * - players can check to see if names are registered
 * - players can register their own name through the bot
 */

package twcore.bots.aliasbot;

import java.util.*;
import twcore.core.*;
import twcore.misc.database.*;

import java.sql.*;

public class aliasbot extends SubspaceBot {

    private OperatorList m_opList;
    private String register = "";
    private HashMap m_access;
    private HashMap m_waitingAction;

    public aliasbot( BotAction botAction ) {
        super( botAction );

        //Request the events that need to be handled
        EventRequester req = botAction.getEventRequester();
        req.request( EventRequester.MESSAGE );

        //Get operator list
        m_opList = m_botAction.getOperatorList();
        m_access = new HashMap();
        m_waitingAction = new HashMap();
    }

    public void handleEvent( Message event ) {
        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName(event.getPlayerID());
<<<<<<< aliasbot.java
                        

            if( m_opList.isSmod( name ) || m_access.containsKey( name.toLowerCase() ) ) {
	            //Operator commands
	            if( message.startsWith( "!resetname " ) )
	                commandResetName( name, message.substring( 11 ), false );
	            else if( message.startsWith( "!enablename " ) )
	                commandEnableName( name, message.substring( 12 ) );
	            else if( message.startsWith( "!disablename " ) )
	                commandDisableName( name, message.substring( 13 ) );
	            else if( message.startsWith( "!info " ) )
	                commandDisplayInfo( name, message.substring( 6 ) );
	            else if( message.startsWith( "!register " ) )
	                commandRegisterName( name, message.substring( 10 ), false );
	            else if( message.startsWith( "!registered " ) )
	            	commandCheckRegistered( name, message.substring( 12 ) );
	            else if( message.startsWith( "!ipcheck " ) )
	                commandIPCheck( name, message.substring( 9 ) );
	            else if( message.startsWith( "!midcheck " ) )
	                commandMIDCheck( name, message.substring( 10 ) );
	            else if( message.startsWith( "!go " ) )
	                m_botAction.changeArena( message.substring( 4 ) );
	            else if( message.startsWith( "!help" ) )
	                commandDisplayHelp( name, false );
	    	} else {
	    		//Player commands
	            if( message.equals( "!resetname" ) )
	            	commandResetName( name, name, true);
	            else if( message.equals( "!registered" ) )
	            	commandCheckRegistered( name, name );
	            else if( message.startsWith( "!registered " ) )
	            	commandCheckRegistered( name, message.substring( 12 ) );
	            else if( message.equals( "!register" ) )
	            	commandRegisterName( name, name, true );
	            else if( message.equals( "!help" ) )
	            	commandDisplayHelp( name, true );
	    	}
=======
            if( ! m_opList.isSmod( name ) && ! name.equalsIgnoreCase("british") && ! name.equalsIgnoreCase("worstplayerever") && ! name.equalsIgnoreCase("s4j3n") ) return;
            if( message.startsWith( "!resetname " ) )
                commandResetName( name, message.substring( 11 ) );
            else if( message.startsWith( "!enablename " ) )
                commandEnableName( name, message.substring( 12 ) );
            else if( message.startsWith( "!disablename " ) )
                commandDisableName( name, message.substring( 13 ) );
            else if( message.startsWith( "!info " ) )
                commandDisplayInfo( name, message.substring( 6 ) );
            else if( message.startsWith( "!register " ) )
                commandRegisterName( name, message.substring( 10 ) );
            else if( message.startsWith( "!ipcheck " ) )
                commandIPCheck( name, message.substring( 9 ) );
            else if( message.startsWith( "!midcheck " ) )
                commandMIDCheck( name, message.substring( 10 ) );
            else if( message.startsWith( "!go " ) )
                m_botAction.changeArena( message.substring( 4 ) );
            else if( message.startsWith( "!help" ) )
                commandDisplayHelp( name );
>>>>>>> 1.8
        } else if( event.getMessageType() == Message.ARENA_MESSAGE ) {
            if( message.startsWith( "IP:" ) )
                parseIP( message );
        }
    }

    public void handleEvent( LoggedOn event ) {
        
        BotSettings m_botSettings = m_botAction.getBotSettings();
        String initialArena = m_botSettings.getString( "Arena" );
        String accessList = m_botSettings.getString( "AccessList" );
        
        //Parse accesslist
        String pieces[] = accessList.split( "," );
        for( int i = 0; i < pieces.length; i++ )
        	m_access.put( pieces[i].toLowerCase(), pieces[i] );
        
        m_botAction.joinArena( initialArena );
    }

	public void commandCheckRegistered( String name, String message ) {
		
		DBPlayerData dbP = new DBPlayerData( m_botAction, "local", message );
		
		if( dbP.isRegistered() )
			m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has been registered." );
		else
			m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has NOT been registered." );
	}

    public void commandResetName( String name, String message, boolean player ) {

        DBPlayerData dbP = new DBPlayerData( m_botAction, "local", message );

        if( !dbP.isRegistered() ) {
        	if( player )
        		m_botAction.sendSmartPrivateMessage( name, "Your name '"+message+"' has not been registered." );
            else
            	m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has not been registered." );
            return;
        }

        if( !dbP.resetRegistration() ) {
        	if( player )
        		m_botAction.sendSmartPrivateMessage( name, "Unable to reset name, please contact a TWD Op." );
        	else
            	m_botAction.sendSmartPrivateMessage( name, "Error resetting name '"+message+"'" );
            return;
        }
        
        if( player )
        	m_botAction.sendSmartPrivateMessage( name, "Your name has been reset." );
        else
        	m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has been reset." );
    }

    public void commandEnableName( String name, String message ) {

        DBPlayerData dbP = new DBPlayerData( m_botAction, "local", message );

        if( !dbP.isRegistered() ) {
            m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has not been registered." );
            return;
        }

        if( dbP.isEnabled() ) {
            m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' is already enabled." );
            return;
        }

        if( !dbP.enableName() ) {
            m_botAction.sendSmartPrivateMessage( name, "Error enabling name '"+message+"'" );
            return;
        }
        m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has been enabled." );
    }

    public void commandDisableName( String name, String message ) {

        DBPlayerData dbP = new DBPlayerData( m_botAction, "local", message );

        if( !dbP.isRegistered() ) {
            m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has not been registered." );
            return;
        }

        if( !dbP.isEnabled() ) {
            m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' is already disabled." );
            return;
        }

        if( !dbP.disableName() ) {
            m_botAction.sendSmartPrivateMessage( name, "Error disabling name '"+message+"'" );
            return;
        }
        m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has been disabled." );
    }

    public void commandDisplayInfo( String name, String message ) {

        DBPlayerData dbP = new DBPlayerData( m_botAction, "local", message );

        if( !dbP.isRegistered() ) {
            m_botAction.sendSmartPrivateMessage( name, "The name '"+message+"' has not been registered." );
            return;
        }
        String status = "ENABLED";
        if( !dbP.isEnabled() ) status = "DISABLED";
        m_botAction.sendSmartPrivateMessage( name, "'"+message+"'  IP:"+dbP.getIP()+"  MID:"+dbP.getMID()+"  "+status );
    }

    public void commandRegisterName( String name, String message, boolean p ) {

        String player = m_botAction.getFuzzyPlayerName( message );
        if( message == null ) {
            m_botAction.sendSmartPrivateMessage( name, "Unable to find "+message+" in the arena." );
            return;
        }

        DBPlayerData dbP = new DBPlayerData( m_botAction, "local", player );

        if( dbP.isRegistered() ) {
            m_botAction.sendSmartPrivateMessage( name, "This name has already been registered." );
            return;
        }
        register = name;
        if( p )
        	m_waitingAction.put( player, "register" );
        else
        	m_waitingAction.put( player, "forceregister" );
        m_botAction.sendUnfilteredPrivateMessage( player, "*info" );
    }

    public void commandIPCheck( String name, String ip ) {

        try {
            String query = "SELECT fcUserName, fcIP, fnMID FROM tblAliasSuppression AS A, ";
            query += " tblUser AS U WHERE A.fnUserID = U.fnUserID AND fcIP LIKE '"+ip+"%'";
            ResultSet result = m_botAction.SQLQuery( "local", query );
            while( result.next () ) {
                String out = result.getString( "fcUserName" ) + "  ";
                out += "IP:" + result.getString( "fcIP" ) + "  ";
                out += "MID:" + result.getString( "fnMID" );
                m_botAction.sendSmartPrivateMessage( name, out );
            }
        } catch (Exception e) {
            Tools.printStackTrace( e );
            m_botAction.sendSmartPrivateMessage( name, "Error doing IP check." );
        }
    }

    public void commandMIDCheck( String name, String mid ) {

        try {
            String query = "SELECT fcUserName, fcIP, fnMID FROM tblAliasSuppression AS A, ";
            query += " tblUser AS U WHERE A.fnUserID = U.fnUserID AND fnMID = "+mid;
            ResultSet result = m_botAction.SQLQuery( "local", query );
            while( result.next () ) {
                String out = result.getString( "fcUserName" ) + "  ";
                out += "IP:" + result.getString( "fcIP" ) + "  ";
                out += "MID:" + result.getString( "fnMID" );
                m_botAction.sendSmartPrivateMessage( name, out );
            }
        } catch (Exception e) {
            Tools.printStackTrace( e );
            m_botAction.sendSmartPrivateMessage( name, "Error doing MID check." );
        }
    }

    public void commandDisplayHelp( String name, boolean player ) {
        String help[] = {
            "--------- ACCOUNT MANAGEMENT COMMANDS ------------------------------------------------",
            "!resetname <name>       - resets the name (unregisters it)",
            "!enablename <name>      - enables the name so it can be used in TWD/TWL games",
            "!disablename <name>     - disables the name so it can not be used in TWD/TWL games",
            "!register <name>        - force registers that name, that player must be in the arena",
            "!registered <name>      - checks if the name is registered",
            "--------- ALIAS CHECK COMMANDS -------------------------------------------------------",
            "!info <name>            - displays the IP/MID that was used to register this name",
            "!ipcheck <IP>           - looks for matching records based on <IP>",
            "!midcheck <MID>         - looks for matching records based on <MID>",
            "!ipidcheck <IP> <MID>   - looks for matching records based on <IP> and <MID>",
            "         <IP> can be partial address - ie:  192.168.0.",
            "--------- MISC COMMANDS --------------------------------------------------------------",
            "!go <arena>             - moves the bot"
        };
        String help2[] = {
        	"!resetname              - resets your name",
        	"!register               - registers your name",
            "!registered <name>      - checks if the name is registered"
        };
        
        if( player )
        	m_botAction.privateMessageSpam( name, help2 );
        else
        	m_botAction.privateMessageSpam( name, help );
    }

    public void parseIP( String message ) {

        String[] pieces = message.split("  ");
        String name = pieces[3].substring(10);
        String ip = pieces[0].substring(3);
        String mid = pieces[5].substring(10);

        DBPlayerData dbP = new DBPlayerData( m_botAction, "local", name );

		//If an info action wasn't set don't handle it
		if( !m_waitingAction.containsKey( name ) ) return;
		
		String option = (String)m_waitingAction.get( name );
		m_waitingAction.remove( name );

        //Note you can't get here if already registered, so can't match yourself.
        if( dbP.aliasMatch( ip, mid ) ) {
        	
        	if( option.equals("register") ) {
        		m_botAction.sendSmartPrivateMessage( name, "Please contact a TWD op to register this name." );
        		return;
        	} else
            	m_botAction.sendSmartPrivateMessage( register, "WARNING: Another account may have been registered on that connection." );
        }

        if( !dbP.register( ip, mid ) ) {
            m_botAction.sendSmartPrivateMessage( register, "Unable to register name." );
            return;
        }
        m_botAction.sendSmartPrivateMessage( register, "Registration successful." );


    }
}
