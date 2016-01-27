package twcore.bots;

import java.sql.ResultSet;
import java.util.TreeMap;
import java.util.Iterator;

import java.io.*;

import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.OperatorList;

/**
    @author milosh
*/
public class TWScript extends MultiUtil {

    public OperatorList opList;
    public String database = "website";
    public int ACCESS_LEVEL = 0;

    public TreeMap<String, String> variables = new TreeMap<String, String>();
    public TreeMap<String, String> constants = new TreeMap<String, String>();

    /**
        Initializes.
    */
    public void init() {
        opList = m_botAction.getOperatorList();
        ACCESS_LEVEL = OperatorList.ER_LEVEL;
        addConstantVariables();
    }

    /**
        Required method that returns this help menu.
    */
    public String[] getHelpMessages() {
        String[] message = {
            "+========================== TWSCRIPT ==========================+",
            "| !setup              - Loads the default setup for this arena.|",
            "| !mysetup            - Loads your personal setup.             |",
            "| !setup <name>       - Loads <name>'s personal setup.         |",
            "| !addvar <n>:<v>     - Adds a variable named <n> of value <v>.|",
            "| !setvar <n>:<v>     - Sets variable <n> to value <v>.        |",
            "| !removevar <n>      - Removes variable <n>.                  |",
            "| !listvar            - Lists the current variables.           |",
            "| !email <r>:<t>      - Email's <r> recipient with <t> text.   |",
            "| !constants          - Lists TWScript constants.              |",
            "| !listkeys           - Lists private TWScript escape keys.    |",
            "| !listpubkeys        - Lists public TWScript escape keys.     |",
            "| !smodlogin          - Log in for Smods.                      |",
            "| !sysoplogin         - Log in for Sysops.                     |",
            "+==============================================================+"
        };
        return message;
    }

    /**
        Gets a help message of all replacement keys
        @see twcore.bots.multibot.twscript.CodeCompiler#replaceKeys
        @return - A help message displaying key types.
    */
    public static String[] getPrivateKeysMessage() {
        String msg[] = {
            "+================ Private Escape Keys ================+",
            "| @name             - The player's name.              |",
            "| @wins             - The player's wins.              |",
            "| @losses           - The player's losses.            |",
            "| @frequency        - The player's frequency.         |",
            "| @oplevel          - The player's op access level.   |",
            "| @id               - The player's id(not userid)     |",
            "| @botname          - The bot's name.                 |",
            "| @shipnum          - The player's ship number.       |",
            "| @shipname         - The player's ship.              |",
            "| @shipslang        - Player's ship in vernacular.    |",
            "| @arenaname        - The arena's name.               |",
            "| @arenasize        - Number of players in arena.     |",
            "| @playingplayers   - Number of players in a ship.    |",
            "| @mvp              - Player with best K:D ratio.     |",
            "| @freqsize(#)      - Number of players on freq       |",
            "| @pfreqsize(#)     - Num. of players playing on freq |",
            "| @shipsonfreq(#,#) - Num of players in a certain ship|",
            "|                       on freq. (ship type, freq #)  |",
            "| @squad            - The player's squad.             |",
            "| @bounty           - The player's bounty.            |",
            "| @kpoints          - Points earned by kills.         |",
            "| @fpoints          - Points earned by flags.         |",
            "| @points           - The sum of kpoints and fpoints. |",
            "| @x                - X Location(Tiles)               |",
            "| @y                - Y Location(Tiles)               |",
            "| @randomfreq       - A random number(0 - 9998)       |",
            "| @randomship       - A random number(1-8)            |",
            "| @randomtile       - A random number(1-1022)         |",
            "| @randomsound      - A random ALLOWED sound number.  |",
            "| @randomplayer     - A random player in the arena.   |",
            "| @ping             - The player's ping in ms.        |",
            "| @date             - The current date.               |",
            "| @time             - The current time.               |",
            "| @!command@@       - Issues a command to the bot, but|",
            "|                      the player receives no message.|",
            "+=====================================================+",
        };
        return msg;
    }

    public static String[] getPublicKeysMessage() {
        String msg[] = {
            "+================= Public Escape Keys ================+",
            "| @botname          - The bot's name.                 |",
            "| @arenaname        - The arena's name.               |",
            "| @arenasize        - Number of players in arena.     |",
            "| @playingplayers   - Number of players in a ship.    |",
            "| @freqsize(#)      - Number of players on freq #.    |",
            "| @pfreqsize(#)     - Num. of players in ship. Freq # |",
            "| @shipsonfreq(#,#) - Num of players in a certain ship|",
            "|                       on freq. (ship type, freq #)  |",
            "| @randomfreq       - A random number(0 - 9998)       |",
            "| @randomship       - A random number(1-8)            |",
            "| @randomtile       - A random number(1-1022)         |",
            "| @randomsound      - A random ALLOWED sound number.  |",
            "| @randomplayer     - A random player in the arena.   |",
            "| @date             - The current date.               |",
            "| @time             - The current time.               |",
            "+=====================================================+",
        };
        return msg;
    }

    public static String[] getConstantsMessage() {
        String msg[] = {
            "+============================== Constants ==============================+",
            "|     Constant          Value       |     Constant          Value       |",
            "| ----------- Op Levels ----------- | ------------- Prizes ------------ |",
            "|    _PLAYER_             0         |    _STEALTH_            4         |",
            "|    _BOT_                1         |    _CLOAK_              5         |",
            "|    _OUTSIDER_           2         |    _XRADAR_             6         |",
            "|    _ER_                 3         |    _WARP_               7         |",
            "|    _MODERATOR_          4         |    _FULLCHARGE_         13        |",
            "|    _HIGHMOD_            5         |    _ENGINESHUTDOWN_     14        |",
            "|    _DEV_                6         |    _MULTIFIRE_          15        |",
            "|    _SMOD_               7         |    _SUPER_              17        |",
            "|    _SYSOP_              8         |    _SHIELDS_            18        |",
            "|    _OWNER_              9         |    _SHRAPNEL_           19        |",
            "| ----------- Math/Time ----------- |    _ANTIWARP_           20        |",
            "|    _PI_              3.14159265   |    _REPEL_              21        |",
            "|    _E_               2.71828183   |    _BURST_              22        |",
            "|    _MOL_             [6.022*10^23]|    _DECOY_              23        |",
            "|    _SECOND_          1000         |    _THOR_               24        |",
            "|    _MINUTE_          60000        |    _MULTIPRIZE_         25        |",
            "|    _HOUR_            3600000      |    _BRICK_              26        |",
            "|    _DAY_             86400000     |    _ROCKET_             27        |",
            "|    _WEEK_            604800000    |    _PORTAL_             28        |",
            "+=======================================================================+",
        };
        return msg;
    }

    /**
        Handles messaging.
    */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String name = m_botAction.getPlayerName(event.getPlayerID());
        Player p = m_botAction.getPlayer(event.getPlayerID());

        if (name == null || p == null)
            return;

        if (event.getMessageType() == Message.PRIVATE_MESSAGE && (opList.getAccessLevel(name) >= ACCESS_LEVEL || name.equalsIgnoreCase(m_botAction.getBotName())))
            handleCommand(name, message);
    }

    /**
        Handles commands.
        @param name name of sender
        @param cmd command sent
    */
    public void handleCommand(String name, String cmd) {
        if (cmd.equalsIgnoreCase("!setup"))
            doArenaSetup(name, "default");

        if (cmd.startsWith("!setup "))
            doArenaSetup(name, cmd.substring(7));

        if (cmd.equalsIgnoreCase("!mysetup"))
            doArenaSetup(name, name);

        if (cmd.startsWith("!fromfile "))
            doFromFile(name, cmd.substring(10));

        if (cmd.startsWith("!addvar "))
            doAddVar(name, cmd.substring(8));

        if (cmd.startsWith("!setvar "))
            doSetVar(name, cmd.substring(8));

        if (cmd.startsWith("!removevar "))
            doRemoveVar(name, cmd.substring(11));

        if (cmd.equalsIgnoreCase("!listvar"))
            doListVar(name);

        if (cmd.equalsIgnoreCase("!listkeys"))
            m_botAction.smartPrivateMessageSpam( name, getPrivateKeysMessage());

        if (cmd.equalsIgnoreCase("!listpubkeys"))
            m_botAction.smartPrivateMessageSpam( name, getPublicKeysMessage());

        if (cmd.equalsIgnoreCase("!constants"))
            m_botAction.smartPrivateMessageSpam( name, getConstantsMessage());

        if (cmd.equalsIgnoreCase("!sysoplogin"))
            doSysopOverride(name);

        if (cmd.equalsIgnoreCase("!smodlogin"))
            doSmodOverride(name);

        if (cmd.startsWith("!email "))
            doEmail(name, cmd.substring(7));
    }

    public void doEmail(String name, String message) {
        String[] msg = message.split(":");

        if(msg.length != 2) {
            m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !email <address>:<text>");
            return;
        }

        boolean emailSend = m_botAction.sendEmailMessage(msg[0], "TWScript Message", msg[1]);

        if(emailSend)
            m_botAction.sendSmartPrivateMessage( name, "Message sent to " + msg[0] + ".");
        else
            m_botAction.sendSmartPrivateMessage( name, "Delivery failed. Error sending message.");
    }

    /**
        Handles arena setups by querying the database and having the bot PM itself with the commands.
        @param name name of sender
        @param message message sent
    */
    public void doArenaSetup(String name, String message) {
        try {
            ResultSet resultSet = m_botAction.SQLQuery(database,
                                  "SELECT S.fcMessage " + "FROM `tblArena` A, `tblArenaSetup` S "
                                  + "WHERE A.fnArenaID = S.fnArenaID "
                                  + "AND S.fcName = '" + message + "' "
                                  + "AND A.fcArenaName = '"
                                  + m_botAction.getArenaName() + "'");

            if(resultSet.next()) {
                String msg = resultSet.getString("fcMessage");
                String[] msgs = msg.split("\r\n|\r|\n");
                m_botAction.smartPrivateMessageSpam(m_botAction.getBotName(), msgs);
                m_botAction.sendSmartPrivateMessage( name, "Setup complete.");
            }
            else
                m_botAction.sendSmartPrivateMessage( name, "Setup failed; Could not locate setup.");

            m_botAction.SQLClose(resultSet);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    /**
        Handles arena setups from a .txt file. The message displays a location which
        may need forward or back slashes depending on the operating system.
        @param name name of sender
        @param message message sent
    */
    public void doFromFile(String name, String message) {
        try {
            File f = new File(message);

            if(f.exists()) {
                FileReader fr = new FileReader(f);
                BufferedReader x = new BufferedReader(fr);
                String textFile = "";
                String s;

                while((s = x.readLine()) != null)
                    textFile += s + "\r";

                x.close();
                String[] msgs = textFile.split("\r");
                m_botAction.smartPrivateMessageSpam(m_botAction.getBotName(), msgs);
                m_botAction.sendSmartPrivateMessage( name, "Setup complete.");
            }
            else
                m_botAction.sendSmartPrivateMessage( name, "Setup failed; File does not exist.");

        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }

    /**
        Adds constants to variables.
    */
    public void addConstantVariables() {
        //ACCESS LEVELS
        constants.put("_PLAYER_", Integer.toString(OperatorList.PLAYER_LEVEL));
        constants.put("_BOT_", Integer.toString(OperatorList.BOT_LEVEL));
        constants.put("_OUTSIDER_", Integer.toString(OperatorList.OUTSIDER_LEVEL));
        constants.put("_ER_", Integer.toString(OperatorList.ER_LEVEL));
        constants.put("_MODERATOR_", Integer.toString(OperatorList.MODERATOR_LEVEL));
        constants.put("_HIGHMOD_", Integer.toString(OperatorList.HIGHMOD_LEVEL));
        constants.put("_DEV_", Integer.toString(OperatorList.DEV_LEVEL));
        constants.put("_SMOD_", Integer.toString(OperatorList.SMOD_LEVEL));
        constants.put("_SYSOP_", Integer.toString(OperatorList.SYSOP_LEVEL));
        constants.put("_OWNER_", Integer.toString(OperatorList.OWNER_LEVEL));

        //PRIZES
        constants.put("_STEALTH_", Integer.toString(Tools.Prize.STEALTH));
        constants.put("_CLOAK_", Integer.toString(Tools.Prize.CLOAK));
        constants.put("_XRADAR_", Integer.toString(Tools.Prize.XRADAR));
        constants.put("_WARP_", Integer.toString(Tools.Prize.WARP));
        constants.put("_FULLCHARGE_", Integer.toString(Tools.Prize.FULLCHARGE));
        constants.put("_ENGINESHUTDOWN_", Integer.toString(Tools.Prize.ENGINE_SHUTDOWN));
        constants.put("_MULTIFIRE_", Integer.toString(Tools.Prize.MULTIFIRE));
        constants.put("_SUPER_", Integer.toString(Tools.Prize.SUPER));
        constants.put("_SHIELDS_", Integer.toString(Tools.Prize.SHIELDS));
        constants.put("_SHRAPNEL_", Integer.toString(Tools.Prize.SHRAPNEL));
        constants.put("_ANTIWARP_", Integer.toString(Tools.Prize.ANTIWARP));
        constants.put("_REPEL_", Integer.toString(Tools.Prize.REPEL));
        constants.put("_BURST_", Integer.toString(Tools.Prize.BURST));
        constants.put("_DECOY_", Integer.toString(Tools.Prize.DECOY));
        constants.put("_THOR_", Integer.toString(Tools.Prize.THOR));
        constants.put("_MULTIPRIZE_", Integer.toString(Tools.Prize.MULTIPRIZE));
        constants.put("_BRICK_", Integer.toString(Tools.Prize.BRICK));
        constants.put("_ROCKET_", Integer.toString(Tools.Prize.ROCKET));
        constants.put("_PORTAL_", Integer.toString(Tools.Prize.STEALTH));

        //MATH CONSTANTS
        constants.put("_PI_", Double.toString(Math.PI));
        constants.put("_E_", Double.toString(Math.E));
        constants.put("_MOL_", "[6.022*10^23]");

        //TIME CONSTANTS
        constants.put("_SECOND_", Integer.toString(Tools.TimeInMillis.SECOND));
        constants.put("_MINUTE_", Integer.toString(Tools.TimeInMillis.MINUTE));
        constants.put("_HOUR_", Integer.toString(Tools.TimeInMillis.HOUR));
        constants.put("_DAY_", Integer.toString(Tools.TimeInMillis.DAY));
        constants.put("_WEEK_", Integer.toString(Tools.TimeInMillis.WEEK));
    }

    /**
        Adds a variable.
        @param name name of sender
        @param message message sent
    */
    public void doAddVar(String name, String message) {
        String[] msgs = message.split(":");

        if(msgs.length != 2) {
            m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !addvar x:1");
            return;
        }

        if(this.variables.containsKey(msgs[0])) {
            m_botAction.sendSmartPrivateMessage( name, "The variable '" + msgs[0] + "' already exists. Use !setvar <name>:<value> to change its value.");
            return;
        }

        this.variables.put(msgs[0], msgs[1]);
        m_botAction.sendSmartPrivateMessage( name, "Variable '" + msgs[0] + "' has been added.");
    }

    /**
        Sets a variable to a certain value.
        @param name name of sender
        @param message message sent
    */
    public void doSetVar(String name, String message) {
        String[] msgs = message.split(":");

        if(msgs.length != 2) {
            m_botAction.sendSmartPrivateMessage( name, "Incorrect usage. Example: !setvar x:1");
            return;
        }

        if(!this.variables.containsKey(msgs[0])) {
            m_botAction.sendSmartPrivateMessage( name, "The variable '" + msgs[0] + "' does not exist. Use !addvar <name>:<type> to create it.");
            return;
        }

        this.variables.remove(msgs[0]);
        this.variables.put(msgs[0], msgs[1]);
        m_botAction.sendSmartPrivateMessage( name, "Variable '" + msgs[0] + "' has been set to '" + msgs[1] + "'.");
    }

    /**
        Removes a variable.
        @param name name of sender
        @param message message sent
    */
    public void doRemoveVar(String name, String message) {
        if(!this.variables.containsKey(message)) {
            m_botAction.sendSmartPrivateMessage( name, "The variable '" + message + "' does not exist. Use !addvar <name>:<type> to create it.");
            return;
        }

        this.variables.remove(message);
        m_botAction.sendSmartPrivateMessage( name, "Variable '" + message + "' has been removed.");
    }

    /**
        Lists the current variables.
        @param name name of sender
    */
    public void doListVar(String name) {
        if(this.variables.isEmpty()) {
            m_botAction.sendSmartPrivateMessage( name, "There are no variables to list!");
            return;
        }

        m_botAction.sendSmartPrivateMessage( name, "=========== TWScript variables ===========");
        Iterator<String> i = this.variables.keySet().iterator();

        while( i.hasNext() ) {
            String n = i.next();
            String v = this.variables.get(n);
            m_botAction.sendSmartPrivateMessage( name, "| " + n + " : " + v);
        }
    }

    /**
        Toggles Sysop override.
        @param name name of sender
    */
    public void doSysopOverride(String name) {
        if(opList.isSysop(name) && !name.contains("Bot")) {
            if(ACCESS_LEVEL == OperatorList.SYSOP_LEVEL) {
                ACCESS_LEVEL = OperatorList.ER_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Sysop override deactivated.");
            } else {
                ACCESS_LEVEL = OperatorList.SYSOP_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Sysop override activated.");
            }
        } else
            m_botAction.sendSmartPrivateMessage( name, "Only System Operators can use this command.");
    }

    /**
        Toggles Smod override.
        @param name name of sender
    */
    public void doSmodOverride(String name) {
        if(opList.isSmod(name) && !name.contains("Bot")) {
            if(ACCESS_LEVEL == OperatorList.SMOD_LEVEL) {
                ACCESS_LEVEL = OperatorList.ER_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Smod override deactivated.");
            } else {
                ACCESS_LEVEL = OperatorList.SMOD_LEVEL;
                m_botAction.sendSmartPrivateMessage( name, "Smod override activated.");
            }
        } else
            m_botAction.sendSmartPrivateMessage( name, "Only Super Moderators can use this command.");
    }

    public void cancel() {}
    public boolean isUnloadable() {
        return true;
    }
    public void requestEvents(ModuleEventRequester modEventReq) {}

}

