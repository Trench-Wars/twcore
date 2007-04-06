package twcore.bots.twbot;

import java.util.*;
import java.sql.*;
import java.text.*;

import twcore.bots.TWBotExtension;
import twcore.core.*;
import twcore.core.events.Message;
import twcore.core.util.Tools;


public class twbotdonations extends TWBotExtension {

    public twbotdonations() {
    }


    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isSmod( name )) handleCommand( name, message );
        }
    }

    public void handleCommand( String name, String message ) {
        if( message.toLowerCase().startsWith( "!listdonated" ) ) {
            do_listDonations( name, message );
        } else if( message.toLowerCase().startsWith( "!donated " ) ) {
            do_addDonation( name, message.substring( 9, message.length() ) );
        } else if( message.toLowerCase().startsWith( "!removedonated " ) ) {
            do_removeDonation( name, message.substring( 15, message.length() ) );
        }
    }

    public void do_listDonations( String name, String message ) {
        try {
            ResultSet result = m_botAction.SQLQuery( "website", "SELECT * FROM tblDonation ORDER BY fnDonationID DESC LIMIT 10" );
            while( result.next() ) {
                int donationId =  result.getInt( "fnDonationID" );
                String userName = sql_getUserName( result.getInt( "fnUserID" ) );
                int amount = result.getInt( "fnAmount" );
                m_botAction.sendSmartPrivateMessage( name, "ID# " + donationId + "  :  " + formatString( userName, 26 ) + "$" + amount );
            }
            m_botAction.SQLClose( result );
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage( name, "Unable to list donations." );
            m_botAction.sendSmartPrivateMessage( name, e.getMessage() );
        }
    }

    public void do_addDonation( String name, String message ) {
        Calendar thisTime = Calendar.getInstance();
        java.util.Date day = thisTime.getTime();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( day );
        String pieces[] = message.split( ":" );
        try {
            int id = sql_getPlayerId( pieces[0] );
            if( id == -1 ) {
                m_botAction.SQLQueryAndClose( "website", "INSERT INTO tblUser (fcUserName, fdSignedUp) VALUES ('"+Tools.addSlashesToString(pieces[0])+"', NOW())");
                id = sql_getPlayerId( pieces[0] );
            }
            m_botAction.SQLQueryAndClose( "website", "INSERT INTO tblDonation (fnUserID, fnAmount, fdDonated) VALUES ('"+id+"', '"+pieces[1]+"', '"+time+"')" );
            m_botAction.sendSmartPrivateMessage( name, "Donation Added:  " + pieces[0] + "    $" + pieces[1] );
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage( name, "Unable to add donation entry.");
            m_botAction.sendSmartPrivateMessage( name, e.getMessage());
        }

    }

    public void do_removeDonation( String name, String message ) {
        try {
            int i = Integer.parseInt( message );
            m_botAction.SQLQueryAndClose( "website", "DELETE FROM tblDonation WHERE fnDonationID = "+i );
            m_botAction.sendSmartPrivateMessage( name, "Donation #" + i +" + deleted." );
        } catch (Exception e) { m_botAction.sendSmartPrivateMessage( name, "Unable to remove donation" ); }
    }

    public String sql_getUserName( int id ) {
        try {
            ResultSet result = m_botAction.SQLQuery( "website", "SELECT fcUserName FROM tblUser WHERE fnUserID = '"+id+"'" );
            String username = "Unknown"; 
            if( result.next() )
                username = result.getString( "fcUserName" );
            m_botAction.SQLClose( result );
            return username;
        } catch (Exception e) { return "Unknown"; }
    }

    public int sql_getPlayerId( String player ) {
        try {
            ResultSet result = m_botAction.SQLQuery( "website", "SELECT fnUserID FROM tblUser WHERE fcUserName = '"+Tools.addSlashesToString(player)+"'" );
            int id = -1;
            if( result.next() )
                id = result.getInt( "fnUserID" );
            m_botAction.SQLClose( result );
            return id;
        } catch (Exception e) { return -1; }
    }

    public static String formatString(String fragment, int length) {
        String line;
        if(fragment.length() > length)
            fragment = fragment.substring(0,length-1);
        else {
            for(int i=fragment.length();i<length;i++)
                fragment = fragment + " ";
        }
        return fragment;
    }

    public void cancel() {
    }

    public String[] getHelpMessages() {
        String help[] = {
            "!listdonated                   - list the last 10 donations",
            "!donated   <name>:<amount>     - adds a donation record",
            "!removedonated <num>           - removes the record with ID <num>"
        };
        return help;
    }
}