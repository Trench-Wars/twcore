package twcore.bots.multibot.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeSet;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.events.ArenaList;
import twcore.core.events.PlayerPosition;
import twcore.core.game.Ship;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;


/**
 * Etc.  A collection of small utilities that are not used very often.
 * 
 * Compiled from twbot modules list, art, remote, donations, and part of flags. 
 */
public class utiletc extends MultiUtil {
    private String requester = null;
    private int move = 16;
    private boolean printCoords = false;
    public static final int MIN_COORD = 0;
    public static final int MAX_COORD = 1024;
    boolean turret = true;
    boolean fire = false;
    double speed = 4000;
    int weapon = 1;
    int     id = -1;
    long time = 0;
    int ourX = 0, ourY = 0;
    Random generator = new Random();

    String database = "website";
    
    public void init() {
    }

    /**
     * Requests events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.ARENA_LIST);
        modEventReq.request(this, EventRequester.PLAYER_POSITION );
    }

    public void handleEvent(ArenaList event) {
    	TreeSet<String> arenaSet = new TreeSet<String>();
		String[] arenas = event.getArenaNames();
		for(int k = 0;k < arenas.length;k++) {
			arenaSet.add(arenas[k]);
			Tools.printLog("Arena: "+arenas[k]);
		}
				
		if(requester != null && arenaSet.isEmpty()==false) {
			Iterator<String> it = arenaSet.iterator();
			while(it.hasNext()) {
				m_botAction.sendSmartPrivateMessage(requester, it.next());
			}
			m_botAction.sendSmartPrivateMessage(requester, "Arenas listed.");
			
			arenaSet.clear();
			arenas = null;
			requester = null;
		}
    	
    }

    public void handleEvent(PlayerPosition event) {
        if( fire ) { 
            if( id == event.getPlayerID() ) {
                ourX = event.getXLocation();
                ourY = event.getYLocation();
                m_botAction.getShip().move( event.getXLocation(), event.getYLocation(), event.getXVelocity(), event.getYVelocity() );
            }

            if( (int)(System.currentTimeMillis()/100) - time > 2 ) {
                time = (int)(System.currentTimeMillis()/100);
                if( !fire || id == event.getPlayerID() ) return;
                int degrees = 0;
                degrees += (int)Math.toDegrees((Math.atan( (event.getYLocation() - ourY + 0.0)/(event.getXLocation()-ourX+0.0) )));
                int newDegree = (int)Math.toDegrees((Math.atan( (event.getYLocation()+event.getYVelocity() - ourY + 0.0)/(event.getXLocation()+event.getXVelocity()-ourX+0.0) )));
                int doppler = getDoppler( event, degrees );
                if( ourX > event.getXLocation() ) { degrees += 180; newDegree += 180; }
                int adjust = (int)Math.toDegrees( Math.atan( doppler / speed ) );
                m_botAction.getShip().rotateDegrees( degrees+adjust );
                m_botAction.getShip().fire( weapon );
            }
        }

        if( printCoords ) {
            int playerID = event.getPlayerID();
            int xCoord = event.getXLocation();
            int yCoord = event.getYLocation();
            String playerName = m_botAction.getPlayerName(playerID);
            m_botAction.sendChatMessage(playerName + " is at: " + xCoord + ", " + yCoord + ".");
        }
    }
    
    public int getDoppler( PlayerPosition event, double d ) {
        int x = (int)(Math.sin( Math.toRadians(d) ) * event.getXVelocity() );
        int y = (int)(Math.cos( Math.toRadians(d) ) * event.getYVelocity() );
        int dop = (int)Math.sqrt( Math.pow( x, 2 ) + Math.pow( y, 2 ) );
        return dop;
    }

    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER(name))
                handleCommand( name, message );
        }
    }

    public void handleCommand( String name, String message ) {
        if( message.toLowerCase().startsWith( "!botattach" )) turretPlayer( message.substring( 11, message.length() ) );
        if( message.toLowerCase().startsWith( "!botunattach" )) unAttach();
        if( message.toLowerCase().startsWith( "!setbotship" )) setShip( message );
        if( message.toLowerCase().startsWith( "!setbotfreq" )) setFreq( message );
        if( message.toLowerCase().startsWith( "!fire" ) ) fire = !fire;
        if( message.toLowerCase().startsWith( "!shoot" ) ) shoot( message );
        if( message.toLowerCase().startsWith( "!speed" ) ) {
            try { speed = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e ){}
        }
        if( message.toLowerCase().startsWith( "!weapon" ) ) {
            try { weapon = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e ){}
        }

        if(message.toLowerCase().equalsIgnoreCase("!list")) {
            requester = name;
            m_botAction.requestArenaList();
        } else if(message.toLowerCase().startsWith("!draw ")) {
            download(name, message.substring(6));
        } else if(message.toLowerCase().startsWith("!specbot")) {
            m_botAction.spec(m_botAction.getBotName());
            m_botAction.spec(m_botAction.getBotName());
            m_botAction.sendSmartPrivateMessage( name, "Spec'd." );
        } else if(message.toLowerCase().startsWith("!setdist ")) {
            try {
                move = Integer.parseInt(message.substring(9));
                m_botAction.sendSmartPrivateMessage( name, "Distance set to " + move );
            } catch(Exception e) {}
        } else if(message.toLowerCase().startsWith("!movebot ")) {
            doMoveBotCmd(name, message.substring(9));
        } else if(message.toLowerCase().startsWith("!printcoords")) {
            printCoords = !printCoords;
            if( printCoords )
                m_botAction.sendSmartPrivateMessage( name, "Now printing coordinates to chat." );
            else
                m_botAction.sendSmartPrivateMessage( name, "No longer printing coordinates to chat." );
        }
            

        if( m_opList.isSmod(name) ) {
            if( message.toLowerCase().startsWith( "!listdonated" ) ) {
                do_listDonations( name, message );
            } else if( message.toLowerCase().startsWith( "!donated " ) ) {
                do_addDonation( name, message.substring( 9, message.length() ) );
            } else if( message.toLowerCase().startsWith( "!removedonated " ) ) {
                do_removeDonation( name, message.substring( 15, message.length() ) );
            }
        }
    }

    public void do_listDonations( String name, String message ) {
        try {
            ResultSet result = m_botAction.SQLQuery( database, "SELECT * FROM tblDonation ORDER BY fnDonationID DESC LIMIT 10" );
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
                m_botAction.SQLQueryAndClose( database, "INSERT INTO tblUser (fcUserName, fdSignedUp) VALUES ('"+Tools.addSlashesToString(pieces[0])+"', NOW())");
                id = sql_getPlayerId( pieces[0] );
            }
            m_botAction.SQLQueryAndClose( database, "INSERT INTO tblDonation (fnUserID, fnAmount, fdDonated) VALUES ('"+id+"', '"+pieces[1]+"', '"+time+"')" );
            m_botAction.sendSmartPrivateMessage( name, "Donation Added:  " + pieces[0] + "    $" + pieces[1] );
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage( name, "Unable to add donation entry.");
            m_botAction.sendSmartPrivateMessage( name, e.getMessage());
        }

    }

    public void do_removeDonation( String name, String message ) {
        try {
            int i = Integer.parseInt( message );
            m_botAction.SQLQueryAndClose( database, "DELETE FROM tblDonation WHERE fnDonationID = "+i );
            m_botAction.sendSmartPrivateMessage( name, "Donation #" + i +" + deleted." );
        } catch (Exception e) { m_botAction.sendSmartPrivateMessage( name, "Unable to remove donation" ); }
    }

    public String sql_getUserName( int id ) {
        try {
            ResultSet result = m_botAction.SQLQuery( database, "SELECT fcUserName FROM tblUser WHERE fnUserID = '"+id+"'" );
            String username = "Unknown"; 
            if( result.next() )
                username = result.getString( "fcUserName" );
            m_botAction.SQLClose( result );
            return username;
        } catch (Exception e) { return "Unknown"; }
    }

    public int sql_getPlayerId( String player ) {
        try {
            ResultSet result = m_botAction.SQLQuery( database, "SELECT fnUserID FROM tblUser WHERE fcUserName = '"+Tools.addSlashesToString(player)+"'" );
            int id = -1;
            if( result.next() )
                id = result.getInt( "fnUserID" );
            m_botAction.SQLClose( result );
            return id;
        } catch (Exception e) { return -1; }
    }

    public static String formatString(String fragment, int length) {
        if(fragment.length() > length)
            fragment = fragment.substring(0,length-1);
        else {
            for(int i=fragment.length();i<length;i++)
                fragment = fragment + " ";
        }
        return fragment;
    }

    public void download(String name, String message)
    {
        try {
            Ship ship = m_botAction.getShip();
            URL url = new URL(message);
            URLConnection URLC = url.openConnection();
            URLC.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(URLC.getInputStream()));
            in.mark(0);
            String inString;
            int chars = 0;
            while((inString = in.readLine()) != null)
            {
                inString = inString.replaceAll(" ", "");
                chars += inString.length();
            }
            if(chars < 1000)
            {
                ship.setShip(1);
                URLC = url.openConnection();
                URLC.connect();
                in = new BufferedReader(new InputStreamReader(URLC.getInputStream()));
                int xNormal = ship.getX();
                while((inString = in.readLine()) != null)
                {
                    if(inString.length() != 0)
                    {
                        ship.fire(getWeapon(inString.charAt(0)));
                        for(int k = 1;k < inString.length();k++)
                        {
                            int temp = 0;
                            if(inString.charAt(k) == ' ') {
                                for(temp = 0;(temp + k) < inString.length() && inString.charAt((temp + k)) == ' ';temp++) { }
                                k += (temp - 1);
                                ship.move(ship.getX() + (move * temp), ship.getY());
                            } else if(inString.charAt(k) == '?') {
                                ship.move(0, ship.getX(), ship.getY(), 0, 0, 4, 1200, 3);
                                ship.move(0, ship.getX(), ship.getY(), 0, 0, 0, 1200, 3);
                            } else {
                                ship.moveAndFire(ship.getX() + move, ship.getY() + 0, getWeapon(inString.charAt(k)));
                            }
                        }
                        ship.move(xNormal, ship.getY() + move);
                    }
                }
            }
            else
                m_botAction.sendPrivateMessage(name, "That file contains too many mines. You should try reducing it so I can draw all of it.");
        } catch(Exception e) {m_botAction.sendPrivateMessage(name, "error... check URL and try again."); e.printStackTrace();}
    }

    public int getWeapon(char c) {
        Ship s = m_botAction.getShip();

        if(c == '.') return s.getWeaponNumber((byte)3, (byte)0, false, false, true, (byte)8, true);
        if(c == '*') return s.getWeaponNumber((byte)3, (byte)1, false, false, true, (byte)8, true);
        if(c == '#') return s.getWeaponNumber((byte)3, (byte)2, false, false, true, (byte)8, true);
        if(c == '^') return s.getWeaponNumber((byte)3, (byte)3, false, false, true, (byte)8, true);
        if(c == '1') return s.getWeaponNumber((byte)4, (byte)0, false, false, true, (byte)8, true);
        if(c == '2') return s.getWeaponNumber((byte)4, (byte)1, false, false, true, (byte)8, true);
        if(c == '3') return s.getWeaponNumber((byte)4, (byte)2, false, false, true, (byte)8, true);
        if(c == '4') return s.getWeaponNumber((byte)4, (byte)3, false, false, true, (byte)8, true);
        return 0;
    }

    /**
     * This method performs the move bot command.  It moves the bot to certain
     * coords, and can be used for speccing certain coords as well.
     *
     * @param sender is the player that sent the command.
     * @param argString are the arguments passed into the command.
     */
    public void doMoveBotCmd(String sender, String argString)
    {
        StringTokenizer argTokens = new StringTokenizer(argString);
        int xCoord;
        int yCoord;

        if(argTokens.countTokens() != 2)
            throw new IllegalArgumentException("Please use the following format: !View <x> <y>");
        xCoord = parseCoord(argTokens.nextToken());
        yCoord = parseCoord(argTokens.nextToken());
        m_botAction.moveToTile(xCoord, yCoord);
    }

    /**
     * This method converts a string to a coord.  If the string is NAN then an
     * exception is thrown.  If the coordinate is smaller than MIN_COORD or
     * larger than MAX_COORD, then an exception is thrown.
     *
     * @param string is the String to parse.
     * @returns the coordinate is returned.
     */
    public int parseCoord(String string)
    {
        int coord;

        try
        {
            coord = Integer.parseInt(string);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid coordinate specified.");
        }

        if(coord < MIN_COORD || coord > MAX_COORD)
            throw new IllegalArgumentException("Coordinates must be between " + MIN_COORD + " and " + MAX_COORD + ".");
        return coord;
    }   
    
    public void shoot( String message ) {
        int direction = 0;
        try { direction = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e) {}
        m_botAction.getShip().rotateDegrees( direction );
        m_botAction.getShip().fire( weapon );

    }

    public void turretPlayer( String name ) {
        turret = true;
        m_botAction.getShip().attach( m_botAction.getPlayerID( name ) );
        id = m_botAction.getPlayerID( name );
    }

    public void unAttach() {
        turret = false;
        m_botAction.getShip().unattach();
    }

    public void setShip( String message ) {
        int x = 0;
        try { x = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e) {}
        m_botAction.getShip().setShip( x );
    }

    public void setFreq( String message ) {
        int x = 0;
        try { x = Integer.parseInt( message.split( " " )[1] ); } catch (Exception e) {}
        m_botAction.getShip().setFreq( x );
    }

    public void cancel() {
    }

    public String[] getHelpMessages() {
        String help[] = {
                "!movebot <x> <y>          - Moves bot to a location.  If in spec, bot will spec",
                "                            this location and receive player position info.",
                "!printcoords              - Toggles whether to print player coords rcv'd or not.",
                "!specbot                  - Puts bot in spectator mode.",
                "!setbotship <ship>        - Place the bot in a ship",
                "!setbotfreq <freq>        - Set the bot to a specific freq",
                "!botattach <player>       - Attach bot to player, if in game",
                "!botunattach              - Detach the bot",
                "!fire                     - Toggles firing at close players",
                "!shoot <degree>           - Fires in direction of degree (0-359)",
                "!speed <speed>            - Sets how fast bot tracks using !fire",
                "!weapon <weapon>          - Changes to weapon # (16 bit vector)",
                "!list                     - Lists all arenas to you in PM.",
                "!draw <url>               - Draws pic from text file at url containing chars .*#^",
                "!setdist                  - Sets distance between mines drawn (def: 16; one tile)",
                "!listdonated              - List the last 10 donations",
                "!donated <name>:<amount>  - Adds a donation record",
                "!removedonated <num>      - Removes the record with ID <num>"
        };
        return help;
    }
}