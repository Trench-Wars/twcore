package twcore.bots.racebot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import twcore.core.events.FlagClaimed;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.Tools;

public class RbTrackManager extends RaceBotExtension {


    HashMap<Integer, Integer> checkPointList;
    String action = "";

    int    checkPoint;
    int    state;
    // 2 = createTrack (marking checkpoints)
    // 1 = endCheckpoint (flags were marked, waiting confirmation)
    // 0 = no actions pending

    int    trackID = -1;    // used for remembering for what track <id> the user is registering pits flag
    HashSet<Integer> pitFlags;

    public RbTrackManager() {
        checkPointList = new HashMap<Integer, Integer>();
        pitFlags = new HashSet<Integer>();
    }

    public void handleEvent( Message event ) {
        try {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if(m_botAction.getOperatorList().isSmod(name) || m_bot.isOperator(name))
            {
                String message = event.getMessage().toLowerCase();

                if( message.equals( "yes" ) )
                    handleAnswer( name, true );
                else if( message.equals( "no" ) )
                    handleAnswer( name, false );
                else if( message.equals( "!help" ) ) {
                    String help[] = {
                        "------------- Track Manager Help Menu ---------------------------",
                        "| !newtrack      - starts the process of setting up a new track |",
                        "| !done          - used when done setting current checkpoint    |",
                        "| !name <name>   - sets arena name to <name>                    |",
                        "| !nametrack <id>:<name> - sets track <id> to <name>            |",
                        "| !setships <id>:<ships> - sets ships for track <id>            |",
                        "|                          seperate multiple ships by a space   |",
                        "| !setwarp <id>  - sets the warp point for track <id> from      |",
                        "|                  your current position                        |",
                        "| !testwarp <id> - tests a warp point already setup             |",
                        "| !setpits <id>  - sets the pits flags for track <id>           |",
                        "| !donepits      - saves the pits flags after !setpits <id>     |",
                        "| !delete <id>   - removes entire track saved at <id>           |",
                        "----------------------------------------------------------------|"
                    };
                    m_botAction.privateMessageSpam( name, help );
                }
                else if( message.startsWith( "!newtrack" ) )
                    createTrack( message );
                else if( message.startsWith( "!name " ) )
                    setArenaName( event.getMessage().substring( 6, message.length() ) );
                else if( message.startsWith( "!nametrack " ) )
                    setTrackName( event.getMessage().substring( 11, message.length() ), name );
                else if( message.startsWith( "!setships " ) )
                    setShipList( message.substring( 10, message.length() ), name );
                else if( message.startsWith( "!setwarp " ) )
                    setWarpPoint( message.substring( 9, message.length() ), name );
                else if( message.startsWith( "!testwarp " ) )
                    testWarp( message.substring( 10, message.length() ), name );
                else if( message.startsWith( "!delete "))
                    deleteTrack( message.substring( 8), name );
                else if( message.startsWith( "!setpits "))
                    setPits( message.substring(9), name );
                else if( message.startsWith( "!donepits"))
                    donePits( name );
                else if( message.startsWith( "!done" ) )
                    endCheckPoint();
            }
        } catch(Exception e) {}

    }

    public void handleAnswer( String sender, boolean yes ) {

        if( action.equals( "arena" ) )
            if( yes ) createArena();
            else m_botAction.sendPrivateMessage( sender, "k" );
        else if( action.equals( "setpoint" ) )
            if( yes ) newCheckPoint();
            else resetLastPoint();
        else if( action.equals( "endtrack" ) )
            if( yes ) endTrack();
            else resetLastPoint();
        else if( action.equals( "abortpitflags" ) )
            if( yes ) {
                pitFlags.clear();
                trackID = -1;
                m_botAction.sendPrivateMessage( sender, "k" );
            } else {
                pitFlags.clear();
                m_botAction.resetFlagGame();
                m_botAction.sendPrivateMessage(sender, "Capture the flags for the pits. PM !donepits when done.");
            }
        else if( action.equals( "pitflags" ) )
            if( yes ) {
                savePits( sender );
            } else {
                pitFlags.clear();
                m_botAction.resetFlagGame();
                m_botAction.sendPrivateMessage(sender, "Capture the flags for the pits. PM !donepits when done.");
            }

        action = "";

    }

    public void createTrack( String message ) {

        //Must create an arena before you can add tracks to it.
        if( !sql_arenaExists() ) {
            setupNewArena();
            return;
        }

        int id = sql_getNextArenaTrackID();

        if( id == -1 ) {
            m_botAction.sendPublicMessage( "Unable to create new track" );
            return;
        }

        checkPoint = 1;
        state = 2;
        m_botAction.resetFlagGame();
        m_botAction.sendPublicMessage( "Please mark the start/finish checkpoint." );
        m_botAction.sendPublicMessage( "NOTE: If the pits flags are parallel to the start/finish checkpoint, mark these aswell.");
        m_botAction.sendPublicMessage( "When done marking points use !done" );
    }

    public void endCheckPoint() {

        if( state == 0 ) return;

        Iterator<Integer> it = checkPointList.keySet().iterator();
        int count = 0;

        while( it.hasNext() ) {
            Integer point = it.next();

            if( checkPointList.get(point).intValue() == checkPoint ) count++;
        }

        if( count == 0 ) {
            m_botAction.sendPublicMessage( "No points were marked. Total number of checkpoints: " + (checkPoint - 1) );
            m_botAction.sendPublicMessage( "End track? yes/no");
            action = "endtrack";
            state = 1;
            return;
        }

        m_botAction.sendPublicMessage( "Points: " + count + " marked." );
        m_botAction.sendPublicMessage( "Is this correct? yes/no" );
        action = "setpoint";
        state = 1;
    }

    public void newCheckPoint() {
        m_botAction.sendPublicMessage( "If you are done use !done to end the track." );
        state = 2;
        m_botAction.resetFlagGame();
        checkPoint++;
        m_botAction.sendPublicMessage( "Please mark checkpoint " + checkPoint );
    }

    public void resetLastPoint() {
        m_botAction.resetFlagGame();

        Iterator<Integer> it = checkPointList.keySet().iterator();

        while( it.hasNext() ) {
            Integer point = it.next();

            if( checkPointList.get(point).intValue() == checkPoint ) it.remove();
        }

        m_botAction.sendPublicMessage( "Last checkpoint was not correct, please mark that checkpoint again." );
        state = 2;
    }

    public void endTrack() {
        state = 0;

        sql_insertNewTrack();
        int id = sql_getMaxTrackID();


        Iterator<Integer> it = checkPointList.keySet().iterator();

        while( it.hasNext() ) {
            int flagId = it.next().intValue();
            int checkPointId = checkPointList.get( new Integer( flagId ) ).intValue();

            try {
                m_botAction.SQLQueryAndClose( m_sqlHost, "INSERT INTO tblRaceCheckPoint (fnTrackID, fnCheckPoint, fnFlagID) VALUES (" + id + ", " + checkPointId + ", " + flagId + ")" );
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
        }

        m_botAction.sendPublicMessage( "Track stored. Track ID = " + (sql_getNextArenaTrackID() - 1) );
        m_botAction.sendPublicMessage( "To name this track use: !nametrack <id>:<name>" );
        checkPointList.clear();

    }

    public void setupNewArena() {
        m_botAction.sendPublicMessage( "Arena has not been setup for racing. Setup now? yes/no" );
        action = "arena";
    }

    public void createArena() {

        try {
            String a = m_botAction.getArenaName();
            ResultSet result = m_botAction.SQLQuery( m_sqlHost, "INSERT INTO tblRace (fcArena, fcName) VALUES ('" + a + "', '" + a + "')" );
            m_botAction.SQLClose( result );
            m_botAction.sendPublicMessage( "Arena setup. To change the name of this arena later please use !name" );
            m_botAction.sendPublicMessage( "Please do !newtrack again to setup tracks for this arena." );
        } catch (Exception e) {
            Tools.printStackTrace(e);
            m_botAction.sendPublicMessage( "Unable to setup arena." );
        }
    }

    public void setArenaName( String name ) {

        //Must create an arena before you can change its name
        if( !sql_arenaExists() ) {
            setupNewArena();
            return;
        }

        try {
            m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRace SET fcName = '" + Tools.addSlashesToString(name) + "' WHERE fcArena = '" + m_botAction.getArenaName() + "'" );
            m_botAction.sendPublicMessage( "Arena Name Changed To: " + name );
        } catch (Exception e) {
            Tools.printStackTrace(e);
            m_botAction.sendPublicMessage( "Unable to change arena name." );
        }
    }

    public void setTrackName( String message, String name ) {

        String pieces[] = message.split(":");
        int id = -1;

        try {
            id = Integer.parseInt( pieces[0] );
        }
        catch (Exception e) {}

        if( id < 0 ) {
            m_botAction.sendPrivateMessage( name, "Unable to name track, track # out of bounds." );
            return;
        }

        try {
            m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRaceTrack SET fcTrackName = '" + Tools.addSlashesToString(pieces[1]) + "' WHERE fnArenaTrackID = " + id + " AND fnRaceID = " + sql_getArenaID());
            m_botAction.sendPublicMessage( "Track #" + id + " name to " + pieces[1] );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Unable to name track." );
            Tools.printStackTrace( e );
        }

    }

    public void setShipList( String message, String name ) {

        String pieces[] = message.split(":");
        int id = -1;

        try {
            id = Integer.parseInt( pieces[0] );
        }
        catch (Exception e) {}

        if( id < 0 ) {
            m_botAction.sendPrivateMessage( name, "Unable to name track, track # out of bounds." );
            return;
        }

        try {
            String chopped[] = pieces[1].split( " " );

            for( int i = 0; i < chopped.length; i++ ) {
                int d = Integer.parseInt( chopped[i] );

                if( d < 1 || d > 8 ) {
                    m_botAction.sendPrivateMessage( name, "Unable to set ships for track, please only use ships 1-8." );
                    return;
                }
            }

            m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRaceTrack SET fcAllowedShips = '" + pieces[1] + "' WHERE fnArenaTrackID = " + id + " AND fnRaceID = " + sql_getArenaID());
            m_botAction.sendPrivateMessage( name, "Ships set to: (" + pieces[1] +  ") :on track #" + id);
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Unable to set ships for track, please use numerical digits 1-8." );
            return;
        }
    }

    public void setWarpPoint( String message, String name ) {
        int id = -1;

        try {
            id = Integer.parseInt( message );
        }
        catch (Exception e) {}

        if( id < 0 ) {
            m_botAction.sendPrivateMessage( name, "Syntax error. Please provide a correct number as track id." );
            return;
        }

        try {
            Player p = m_botAction.getPlayer( name );
            m_botAction.SQLQueryAndClose( m_sqlHost, "UPDATE tblRaceTrack SET fnXWarp = " + (p.getXLocation() / 16) + ", fnYWarp = " + (p.getYLocation() / 16) + " WHERE fnArenaTrackID = " + id + " AND fnRaceID = " + sql_getArenaID());
            m_botAction.sendPrivateMessage( name, "Warppoint set for track #" + id );
        } catch (Exception e) {
            m_botAction.sendPrivateMessage( name, "Unable to set warppoint for track");
        }
    }

    public void testWarp( String message, String name ) {
        int id = -1;

        try {
            id = Integer.parseInt( message );
        }
        catch (Exception e) {}

        if( id < 0 ) {
            m_botAction.sendPrivateMessage( name, "Syntax error. Please provide a correct number as track id." );
            return;
        }

        try {
            ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT fnXWarp, fnYWarp FROM tblRaceTrack WHERE fnArenaTrackID = " + id + " AND fnRaceID = " + sql_getArenaID());

            if( result != null && result.next() ) {
                int warpX = result.getInt("fnXWarp");
                int warpY = result.getInt("fnYWarp");

                if(warpX == 0 || warpY == 0)
                    m_botAction.sendPrivateMessage( name , "No warp points or misconfigured for track #" + id);
                else {
                    m_botAction.warpTo( name, warpX, warpY );
                    m_botAction.sendPrivateMessage( name, "Warped to " + warpX + "," + warpY + "");
                }
            } else {
                m_botAction.sendPrivateMessage( name , "No warp points or misconfigured for track #" + id);
            }

            m_botAction.SQLClose( result );

        } catch (SQLException sqle) {
            m_botAction.sendPrivateMessage(name, "Unexpected error occured during retrieving warp points for track #" + id + ". Please contact a member of TW Bot Development Team.");
        }
    }

    public void deleteTrack( String parameters, String name ) {
        int id = -1;
        int trackID = -1;

        try {
            id = Integer.parseInt( parameters );
        } catch(NumberFormatException nfe) {
            m_botAction.sendPrivateMessage(name, "Syntax error, wrong track id specified. Please type ::!help for command syntax.");
            return;
        }

        if(id < 0) {
            m_botAction.sendPrivateMessage(name, "Syntax error, wrong track id specified. Please type ::!help for command syntax.");
            return;
        }

        // get the raceID of the current arena
        int raceID = sql_getArenaID();

        if(raceID < 0) {
            m_botAction.sendPrivateMessage(name, "The current arena couldn't be found in the database. Are you sure this arena is setup for racing?");
            return;
        }

        // Check if the given track id exists
        trackID = sql_getTrackID(raceID, id);

        if(trackID == -1) {
            m_botAction.sendPrivateMessage(name, "The track id #" + id + " isn't found for this arena. Please specify a different track #id.");
            return;
        }

        try {
            m_botAction.SQLQueryAndClose( m_sqlHost, "DELETE FROM tblRaceTrack WHERE fnArenaTrackID = " + id + " AND fnRaceID = " + raceID);
            m_botAction.SQLQueryAndClose( m_sqlHost, "DELETE FROM tblRaceCheckPoint WHERE fnTrackID = " + trackID);
            m_botAction.SQLQueryAndClose( m_sqlHost, "DELETE FROM tblRaceWinners WHERE trackWon = " + trackID);
            m_botAction.SQLQueryAndClose( m_sqlHost, "DELETE FROM tblRacePits WHERE fnTrackID = " + trackID);
        } catch(SQLException sqle) {
            m_botAction.sendPrivateMessage(name, "Unexpected error occured while deleting the track. Please contact a member of the TW Bot Development Team.");
            return;
        }

        m_botAction.sendPrivateMessage(name, "Track #" + id + " deleted, including checkpoints and winners.");
    }

    public void setPits( String params, String sender ) {
        String id = params.trim();
        int arenaTrackID = -1;
        pitFlags.clear();
        trackID = -1;

        if(id.length() == 0 || !Tools.isAllDigits(id)) {
            m_botAction.sendPrivateMessage(sender, "Please specify a track <id>. Type ::!help for more information.");
            return;
        }

        try {
            arenaTrackID = Integer.parseInt(id);
        } catch(NumberFormatException nfe) {
            m_botAction.sendPrivateMessage(sender, "Syntax error encountered on the specified track <id>. Please specify a valid track <id>.");
            return;
        }

        // get the raceID of the current arena
        int arenaID = sql_getArenaID();

        if(arenaID < 0) {
            m_botAction.sendPrivateMessage(sender, "The current arena couldn't be found in the database. Are you sure this arena is setup for racing?");
            return;
        }

        // Check if the specified track ID exists.
        trackID = sql_getTrackID(arenaID, arenaTrackID);

        if(trackID == -1) {
            m_botAction.sendPrivateMessage(sender, "The track id #" + id + " isn't found for this arena. Please specify a different track #id.");
            return;
        }

        m_botAction.resetFlagGame();
        m_botAction.sendPrivateMessage(sender, "Capture the flags (flags to give FC) for the pits. PM !donepits when done.");
    }

    public void donePits( String sender ) {
        if(trackID == -1) {
            m_botAction.sendPrivateMessage( sender, "PM !setpits <id> first before doing !donepits. PM !help for more information.");
            return;
        }

        if(pitFlags.size() == 0) {
            m_botAction.sendPrivateMessage( sender, "No flags were marked.");
            m_botAction.sendPrivateMessage( sender, "Abort setting pit flags? yes/no");
            action = "abortpitflags";
        } else {
            m_botAction.sendPrivateMessage( sender, "Flags: " + pitFlags.size() + " marked.");
            m_botAction.sendPrivateMessage( sender, "Is this correct? yes/no");
            action = "pitflags";
        }
    }

    public void savePits( String sender ) {

        for(Integer flag : pitFlags) {
            try {
                m_botAction.SQLQueryAndClose( m_sqlHost, "INSERT INTO tblRacePits (fnTrackID, fnFlagID) VALUES (" + trackID + ", " + flag + ")" );
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
        }

        m_botAction.sendPublicMessage( "Pit flags stored." );
        pitFlags.clear();
        trackID = -1;
    }

    public void handleEvent( FlagClaimed event ) {

        if( state >= 2) {
            checkPointList.put( new Integer( event.getFlagID() ), new Integer( checkPoint ) );
        }

        if( trackID > -1) {
            pitFlags.add( new Integer( event.getFlagID() ) );
        }
    }

    /*****************************
    **** SQL RELATED METHODS  ****
    *****************************/

    public boolean sql_insertNewTrack() {

        try {
            m_botAction.SQLQueryAndClose( m_sqlHost, "INSERT INTO tblRaceTrack (fnRaceID, fnArenaTrackID ) VALUES (" + sql_getArenaID() + ", " + sql_getNextArenaTrackID() + ")" );
            return true;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return false;
        }
    }

    public boolean sql_arenaExists() {

        try {
            ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRace WHERE fcArena = '" + m_botAction.getArenaName() + "'" );
            boolean exists = true;

            if( !result.next() )
                exists = false;

            m_botAction.SQLClose( result );
            return exists;
        } catch (Exception e) {
            return false;
        }
    }

    public int sql_getArenaID() {

        try {
            ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT * FROM tblRace WHERE fcArena = '" + m_botAction.getArenaName() + "'" );
            int id = -1;

            if( result.next() )
                id = result.getInt( "fnRaceID" );

            m_botAction.SQLClose( result );
            return id;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return -1;
        }
    }

    public int sql_getMaxTrackID() {

        try {
            ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT MAX(fnTrackID) AS id FROM `tblRaceTrack`" );
            int id = -1;

            if( result != null && result.next() )
                id = result.getInt("id");

            m_botAction.SQLClose( result );
            return id;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return -1;
        }
    }

    public int sql_getNextArenaTrackID() {

        try {
            ResultSet result = m_botAction.SQLQuery( m_sqlHost, "SELECT MAX(fnArenaTrackID) AS id FROM `tblRaceTrack` WHERE fnRaceID = " + sql_getArenaID() );
            int id = 1;

            if( result.next() )
                id = result.getInt("id") + 1;

            m_botAction.SQLClose( result );
            return id;
        } catch (Exception e) {
            Tools.printStackTrace(e);
            return -1;
        }

    }

    /**
        Returns the database track ID of the specified race ID and specified track ID.

        @param raceID
        @param arenaID
        @return
    */
    public int sql_getTrackID(int raceID, int arenaID) {
        ResultSet resultset = null;
        int trackID = -1;

        try {
            resultset = m_botAction.SQLQuery( m_sqlHost, "SELECT fnTrackID FROM tblRaceTrack WHERE fnArenaTrackID = " + arenaID + " AND fnRaceID = " + raceID + " LIMIT 0,1");

            if(resultset != null && resultset.next()) {
                trackID = resultset.getInt(1);
            }
        } catch(SQLException sqle) {
            Tools.printLog("Unexpected error occured on check if the track id exists.");
            Tools.printStackTrace(sqle);
        } finally {
            m_botAction.SQLClose(resultset);
        }

        return trackID;
    }

}