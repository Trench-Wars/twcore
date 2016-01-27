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
    Etc.  A collection of small utilities that are not used very often.

    Compiled from twbot modules list, art, remote, donations, and part of flags.
*/
public class utiletc extends MultiUtil {

    private static final double SS_CONSTANT     = 0.1111111;    // Constant related to angles.
    private static final double REACTION_TIME   = 0.075;        // Reaction time of the bot?
    private static final int    MIN_COORD       = 0;            // Minimum X or Y in tiles.
    private static final int    MAX_COORD       = 1023;         // Maximum X or Y in tiles.

    Random generator = new Random();
    private String database = "website";
    private String requester = null;
    private String setting = null;

    private long lastRotation = 0;          // Time tracker for last rotation update.
    private long lastFire = 0;              // Time tracker for last fire update.
    private int rotSpeed = 100;             // Rate of rotation/tracking, in ms.
    private int fireSpeed = 1000;           // Rate of fire, in ms.
    private int move = 16;                  // Distance between mines for !draw in pixels.
    private int delayMS = 20;               // Delay for !draw between moves.
    private int weapon = 1;                 // Weapon the bot will use to fire.
    private int attachID = -1;

    private boolean printCoords = false;    // Whether or not coords of players in range will be messaged.
    private boolean fire = false;           // Will the bot auto-fire on potential targets?

    public void init() {
    }

    /**
        Requests events.
    */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.ARENA_LIST);
        modEventReq.request(this, EventRequester.PLAYER_POSITION );
    }

    public void handleEvent(ArenaList event) {
        TreeSet<String> arenaSet = new TreeSet<String>();
        String[] arenas = event.getArenaNames();

        for(int k = 0; k < arenas.length; k++) {
            arenaSet.add(arenas[k]);
        }

        if(requester != null && arenaSet.isEmpty() == false) {
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
        int pID = event.getPlayerID();
        int xCoord = event.getXLocation();
        int yCoord = event.getYLocation();

        if( fire ) {
            if( attachID == event.getPlayerID() ) {
                m_botAction.getShip().move( xCoord, yCoord, event.getXVelocity(), event.getYVelocity() );
            }
            else {
                if( m_botAction.getPlayer(pID).getFrequency() != m_botAction.getPlayer(m_botAction.getBotName()).getFrequency()
                        && System.currentTimeMillis() - lastRotation > rotSpeed ) {
                    lastRotation = System.currentTimeMillis();

                    double diffY, diffX, angle;

                    diffX = (xCoord + (event.getXVelocity() * REACTION_TIME)) - m_botAction.getShip().getX();
                    diffY = (yCoord + (event.getYVelocity() * REACTION_TIME)) - m_botAction.getShip().getY();
                    angle = (180 - (Math.atan2(diffX, diffY) * 180 / Math.PI)) * SS_CONSTANT;

                    m_botAction.getShip().setRotation( (int) angle );
                }

                if( System.currentTimeMillis() - lastFire > fireSpeed ) {
                    lastFire = System.currentTimeMillis();
                    m_botAction.getShip().fire( weapon );
                }
            }
        }

        if( printCoords ) {
            String playerName = m_botAction.getPlayerName(pID);
            m_botAction.sendPublicMessage(playerName + " is at: " + xCoord + ", " + yCoord + ".");
        }
    }

    public void handleEvent( Message event ) {
        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( m_opList.isER(name))
                handleCommand( name, message );
        } else if( event.getMessageType() == Message.ARENA_MESSAGE ) {
            if( requester != null && setting != null ) {
                if( message.startsWith( setting ) ) {
                    m_botAction.sendPrivateMessage( requester, message );
                    requester = null;
                    setting = null;
                }
            }
        }
    }

    public void handleCommand( String name, String message ) {
        String cmd = message.toLowerCase();

        if( m_opList.isSmod(name) || m_opList.isER(name) ) {
            if( cmd.startsWith( "!botattach" )) {
                turretPlayer( message.substring( 10, message.length() ).trim() );
            } else if( cmd.startsWith( "!botattachme" )) {
                turretPlayer( name );
            } else if( cmd.startsWith( "!botunattach" )) {
                unAttach();
            } else if( cmd.startsWith( "!setbotship" )) {
                setShip( message );
            } else if( cmd.startsWith( "!setbotfreq" )) {
                setFreq( message );
            } else if( cmd.equals( "!fire" ) ) {
                fire = !fire;
            } else if( cmd.startsWith( "!shoot" ) ) {
                shoot( message );
            } else if( cmd.startsWith( "!speed" ) ) {
                try {
                    int temp = Integer.parseInt( message.split( " " )[1] );

                    if( temp < 10 )
                        rotSpeed = 10;
                    else if(temp > 100 * Tools.TimeInMillis.SECOND)
                        rotSpeed = 100 * Tools.TimeInMillis.SECOND;
                    else
                        rotSpeed = temp;
                } catch ( Exception e ) {}
            } else if( cmd.startsWith( "!firespeed" )) {
                try {
                    int temp = Integer.parseInt( message.split( " " )[1] );

                    if(temp < 100)
                        fireSpeed = 100;
                    else if(temp > 100 * Tools.TimeInMillis.SECOND)
                        fireSpeed = 100 * Tools.TimeInMillis.SECOND;
                    else
                        fireSpeed = temp;
                } catch ( Exception e ) {}
            } else if( cmd.startsWith( "!setweapon" )) {
                cmd_setWeapon(name, cmd.substring(10).trim());
            } else if(cmd.equalsIgnoreCase("!list")) {
                requester = name;
                m_botAction.requestArenaList();
            } else if(cmd.startsWith("!draw ")) {
                download(name, message.substring(6));
            } else if(cmd.startsWith("!setdelay ")) {
                try {
                    delayMS = Integer.parseInt( message.split( " " )[1] );
                    m_botAction.sendSmartPrivateMessage(name, "Draw delay set to " + delayMS + "ms. (Default: 20)");
                } catch (Exception e ) {}
            } else if(cmd.startsWith("!specbot")) {
                m_botAction.spec(m_botAction.getBotName());
                m_botAction.spec(m_botAction.getBotName());
                m_botAction.sendSmartPrivateMessage( name, "Spec'd." );
            } else if(cmd.startsWith("!setdist ")) {
                try {
                    move = Integer.parseInt(message.substring(9));
                    m_botAction.sendSmartPrivateMessage( name, "Distance set to " + move );
                } catch(Exception e) {}
            } else if(cmd.startsWith("!movebot ")) {
                doMoveBotCmd(name, message.substring(9));
            } else if(cmd.startsWith("!printcoords")) {
                printCoords = !printCoords;

                if( printCoords )
                    m_botAction.sendSmartPrivateMessage( name, "Now printing coordinates to chat." );
                else
                    m_botAction.sendSmartPrivateMessage( name, "No longer printing coordinates to public chat." );
            }
        }

        if( m_opList.isSmod(name) ) {
            if( cmd.startsWith( "!listdonated" ) ) {
                do_listDonations( name, message );
            } else if( cmd.startsWith( "!donated " ) ) {
                do_addDonation( name, message.substring( 9, message.length() ) );
            } else if( cmd.startsWith( "!removedonated " ) ) {
                do_removeDonation( name, message.substring( 15, message.length() ) );
            } else if( cmd.startsWith( "!getset " ) ) {
                do_getSetting( name, message.substring( 8, message.length() ) );
            } else if( cmd.startsWith( "!weapon " ) ) {
                try {
                    weapon = Integer.parseInt( message.split( " " )[1] );
                }
                catch (Exception e ) {}
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
                m_botAction.SQLQueryAndClose( database, "INSERT INTO tblUser (fcUserName, fdSignedUp) VALUES ('" + Tools.addSlashesToString(pieces[0]) + "', NOW())");
                id = sql_getPlayerId( pieces[0] );
            }

            m_botAction.SQLQueryAndClose( database, "INSERT INTO tblDonation (fnUserID, fnAmount, fdDonated) VALUES ('" + id + "', '" + pieces[1] + "', '" + time + "')" );
            m_botAction.sendSmartPrivateMessage( name, "Donation Added:  " + pieces[0] + "    $" + pieces[1] );
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage( name, "Unable to add donation entry.");
            m_botAction.sendSmartPrivateMessage( name, e.getMessage());
        }

    }

    public void do_removeDonation( String name, String message ) {
        try {
            int i = Integer.parseInt( message );
            m_botAction.SQLQueryAndClose( database, "DELETE FROM tblDonation WHERE fnDonationID = " + i );
            m_botAction.sendSmartPrivateMessage( name, "Donation #" + i + " + deleted." );
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage( name, "Unable to remove donation" );
        }
    }

    public void do_getSetting( String name, String message ) {
        m_botAction.sendUnfilteredPublicMessage( "?get " + message );
        requester = name;
        setting = message;
    }

    public String sql_getUserName( int id ) {
        try {
            ResultSet result = m_botAction.SQLQuery( database, "SELECT fcUserName FROM tblUser WHERE fnUserID = '" + id + "'" );
            String username = "Unknown";

            if( result.next() )
                username = result.getString( "fcUserName" );

            m_botAction.SQLClose( result );
            return username;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public int sql_getPlayerId( String player ) {
        try {
            ResultSet result = m_botAction.SQLQuery( database, "SELECT fnUserID FROM tblUser WHERE fcUserName = '" + Tools.addSlashesToString(player) + "'" );
            int id = -1;

            if( result.next() )
                id = result.getInt( "fnUserID" );

            m_botAction.SQLClose( result );
            return id;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String formatString(String fragment, int length) {
        if(fragment.length() > length)
            fragment = fragment.substring(0, length - 1);
        else {
            for(int i = fragment.length(); i < length; i++)
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

                        for(int k = 1; k < inString.length(); k++)
                        {
                            int temp = 0;

                            if(inString.charAt(k) == ' ') {
                                for(temp = 0; (temp + k) < inString.length() && inString.charAt((temp + k)) == ' '; temp++) { }

                                k += (temp - 1);
                                ship.move(ship.getX() + (move * temp), ship.getY());
                            } else if(inString.charAt(k) == '?') {
                                ship.move(0, ship.getX(), ship.getY(), 0, 0, 4, 1200, 3);
                                ship.move(0, ship.getX(), ship.getY(), 0, 0, 0, 1200, 3);
                            } else {
                                ship.moveAndFire(ship.getX() + move, ship.getY() + 0, getWeapon(inString.charAt(k)));
                            }

                            try {
                                Thread.sleep(delayMS);
                            } catch (Exception e) {}
                        }

                        ship.move(xNormal, ship.getY() + move);
                    }
                }
            }
            else
                m_botAction.sendPrivateMessage(name, "That file contains too many mines. You should try reducing it so I can draw all of it.");
        } catch(Exception e) {
            m_botAction.sendPrivateMessage(name, "error... check URL and try again.");
            e.printStackTrace();
        }
    }

    public int getWeapon(char c) {
        Ship s = m_botAction.getShip();

        if(c == '.') return s.getWeaponNumber((byte)3, (byte)0, false, (byte)3, (byte)8, true);

        if(c == '*') return s.getWeaponNumber((byte)3, (byte)1, false, (byte)3, (byte)8, true);

        if(c == '#') return s.getWeaponNumber((byte)3, (byte)2, false, (byte)3, (byte)8, true);

        if(c == '^') return s.getWeaponNumber((byte)3, (byte)3, false, (byte)3, (byte)8, true);

        if(c == '1') return s.getWeaponNumber((byte)4, (byte)0, false, (byte)3, (byte)8, true);

        if(c == '2') return s.getWeaponNumber((byte)4, (byte)1, false, (byte)3, (byte)8, true);

        if(c == '3') return s.getWeaponNumber((byte)4, (byte)2, false, (byte)3, (byte)8, true);

        if(c == '4') return s.getWeaponNumber((byte)4, (byte)3, false, (byte)3, (byte)8, true);

        return 0;
    }

    /**
        This method performs the move bot command.  It moves the bot to certain
        coords, and can be used for speccing certain coords as well.

        @param sender is the player that sent the command.
        @param argString are the arguments passed into the command.
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
        This method converts a string to a coord.  If the string is NAN then an
        exception is thrown.  If the coordinate is smaller than MIN_COORD or
        larger than MAX_COORD, then an exception is thrown.

        @param string is the String to parse.
        @return the coordinate is returned.
    */
    public int parseCoord(String string)
    {
        int coord;

        try {
            coord = Integer.parseInt(string);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate specified.");
        }

        if(coord < MIN_COORD || coord > MAX_COORD)
            throw new IllegalArgumentException("Coordinates must be between " + MIN_COORD + " and " + MAX_COORD + ".");

        return coord;
    }

    public void shoot( String message ) {
        int direction = 0;

        try {
            direction = Integer.parseInt( message.split( " " )[1] );
        }
        catch (Exception e) {}

        m_botAction.getShip().rotateDegrees( direction );
        m_botAction.getShip().fire( weapon );

    }

    public void turretPlayer( String name ) {
        attachID = m_botAction.getPlayerID( name );

        if (attachID != -1)
            m_botAction.getShip().attach( attachID );
    }

    public void unAttach() {
        m_botAction.getShip().unattach();
        attachID = -1;
    }

    public void setShip( String message ) {
        int x = 0;

        try {
            x = Integer.parseInt( message.split( " " )[1] );
        }
        catch (Exception e) {}

        m_botAction.getShip().setShip( x );

        if(attachID != -1)
            m_botAction.getShip().attach( attachID );
    }

    public void setFreq( String message ) {
        int x = 0;

        try {
            x = Integer.parseInt( message.split( " " )[1] );
        }
        catch (Exception e) {}

        m_botAction.getShip().setFreq( x );

        if(attachID != -1)
            unAttach();
    }

    public void cmd_setWeapon( String name, String args) {
        if( args == null || args.isEmpty() ) {
            setWeaponHelp(name);
            return;
        }

        byte weaponType = 0;
        byte weaponLevel = 0;
        boolean shrapBouncing = false;
        byte shrapLevel = 0;
        byte shrapAmount = 0;
        boolean alternate = false;

        int temp = 0;

        String[] splitArgs = args.split(":");

        try {
            switch(splitArgs.length) {
            // Intended fall-throughs
            default:
            case 6:
                if(splitArgs[5].equals("1"))
                    alternate = true;
                else
                    alternate = false;

            case 5:
                temp = Integer.parseInt(splitArgs[4]);

                if(temp < 0)
                    shrapAmount = 0;
                else if(temp > 31)
                    shrapAmount = 0x1F;
                else
                    shrapAmount = (byte) temp;

            case 4:
                temp = Integer.parseInt(splitArgs[3]);

                if(temp < 0)
                    shrapLevel = 0x00;
                else if(temp > 3)
                    shrapLevel = 0x03;
                else
                    shrapLevel = (byte) temp;

            case 3:
                if(splitArgs[2].equals("1"))
                    shrapBouncing = true;
                else
                    shrapBouncing = false;

            case 2:
                temp = Integer.parseInt(splitArgs[1]);

                if(temp < 0)
                    weaponLevel = 0x00;
                else if(temp > 3)
                    weaponLevel = 0x03;
                else
                    weaponLevel = (byte) temp;

            case 1:
                temp = Integer.parseInt(splitArgs[0]);

                if(temp < 0)
                    weaponType = 0x00;
                else if(temp > 31)
                    weaponType = 0x1F;
                else
                    weaponType = (byte) temp;

                break;

            case 0:
                m_botAction.sendSmartPrivateMessage(name, "Invalid parameters provided. Please consult !setweapon without giving parameters.");
                return;
            }
        } catch (NumberFormatException e) {
            m_botAction.sendSmartPrivateMessage(name, "Invalid parameters provided. Please consult !setweapon without giving parameters.");
            return;
        }

        weapon = m_botAction.getShip().getWeaponNumber(weaponType, weaponLevel,
                 shrapBouncing, shrapLevel, shrapAmount, alternate);

        m_botAction.sendSmartPrivateMessage(name, "Weapon set to given options.");
    }

    public void setWeaponHelp(String name) {
        String[] help = {
            "!setweapon <type>:<level>:<sbounce>:<slevel>:<samount>:<alt-fire>",
            " <type>':      - Weapon type of the following list:",
            "                   0: None               3: Bomb             6: Decoy",
            "                   1: Bullet             4: Proximity Bomb   7: Burst",
            "                   2: Bouncing bullet    5: Repel            8: Thor",
            " <level>^:     - Weapon level: 0: L1, 1: L2, 2: L3, 3: L4",
            " <sbounce>*:   - Bouncing shrap: 0: disabled, 1: enabled",
            " <slevel>*:    - Shrap level. 0: L1, 1: L2, 2: L3, 3: L4",
            " <samount>*:   - Amount of shrap. (0 - 31)",
            " <alt-fire>*:  - Alternative fire mode. 0: disabled, 1: enabled",
            "                   Changes: Bombs -> Mines, Bullets -> Multifire",
            " Legend:       ' Required, ^ Required for types 1-4, * optional",
            " When specifying an optional paramater, all preceding optional parameters must be specified!"
        };
        m_botAction.smartPrivateMessageSpam(name, help);
    }

    public void cancel() {
    }

    public String[] getHelpMessages() {
        String help[] = {
            "...ER+ CMDS...",
            "!movebot <x> <y>          - Moves bot to a location.  If in spec, bot will spec",
            "                            this location and receive player position info.",
            "!printcoords              - Toggles whether to print player coords rcv'd or not.",
            "!specbot                  - Puts bot in spectator mode.",
            "!setbotship <ship>        - Place the bot in a ship (0: WB .. 7: Shark, 8: Spec)",
            "!setbotfreq <freq>        - Set the bot to a specific freq",
            "!botattach <player>       - Attach bot to player, if in game",
            "!botattachme              - Attach bot to player giving command",
            "!botunattach              - Detach the bot",
            "!fire                     - Toggles firing at close players",
            "!shoot <degree>           - Fires in direction of degree (0-359)",
            "!speed <speed>            - Sets how fast bot tracks using !fire (>10, in ms)",
            "!firespeed <speed>        - How fast the bot fires using !fire (>100, in ms)",
            "!setweapon [options]      - Sets bot's weapon, use without options for full details",
            "!list                     - Lists all arenas to you in PM.",
            "!draw <url>               - Draws pic from text file at url containing chars .*#^",
            "!setdist                  - Sets distance between mines drawn (def: 16; one tile)",
            "...SMOD ONLY CMDS...",
            "!listdonated              - List the last 10 donations",
            "!donated <name>:<amount>  - Adds a donation record",
            "!removedonated <num>      - Removes the record with ID <num>",
            "!weapon <weapon>          - Changes to weapon # (16 bit vector) ",
            "!getset <setting>         - Gets a CFG setting, such as Misc:LevelFiles"
        };
        return help;
    }
}
