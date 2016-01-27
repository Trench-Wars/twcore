package twcore.bots.pushbulletbot;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;
import twcore.core.net.iharder.*;
import twcore.core.util.Tools;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
    @author 24
*/
public class pushbulletbot extends SubspaceBot {

    private BotSettings m_botSettings; // Stores settings for your bot as found in the .cfg file.
    private OperatorList m_opList;
    //BotSettings rules; // In this case, it would be settings from pushbulletbot.cfg

    private String connectionID = "pushbulletbot";
    private PushbulletClient pbClient; // Push to mobile data, private MobilePusher mobilePusher;

    static final String db = "bots";

    static final String[] helpmsg = {
        "Commands:",
        "!signup <email>     -- signs up <email> for notifications",
        "!enable             -- enable alerts to your pushbullet account",
        "!disable            -- disable alerts to your pushbullet account",
        "!push <>            -- Push a test msg to yourself",
        "!beep <>            -- Beep for TWD match. Commands: jd dd bd fd sd any ",
        "!challenge          -- Mimic squad challenge for TWJD",
        "!accept             -- Mimic squad acceptance for TWJD",
        "Standard Bot Commands:",
        "!go <arena>         -- sends the bot to <arena>",
        "!die                -- initiates shut down sequence",
        "!help               -- displays this"
    };

    public pushbulletbot(BotAction botAction) {
        super(botAction);
        requestEvents();

        m_botSettings = m_botAction.getBotSettings();
        m_opList = m_botAction.getOperatorList();
    }

    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        // req.request(EventRequester.ARENA_LIST);
        req.request(EventRequester.ARENA_JOINED);
        // req.request(EventRequester.PLAYER_ENTERED);
        // req.request(EventRequester.PLAYER_POSITION);
        // req.request(EventRequester.PLAYER_LEFT);
        // req.request(EventRequester.PLAYER_DEATH);
        // req.request(EventRequester.PRIZE);
        // req.request(EventRequester.SCORE_UPDATE);
        // req.request(EventRequester.WEAPON_FIRED);
        // req.request(EventRequester.FREQUENCY_CHANGE);
        // req.request(EventRequester.FREQUENCY_SHIP_CHANGE);
        req.request(EventRequester.LOGGED_ON);
        // req.request(EventRequester.FILE_ARRIVED);
        // req.request(EventRequester.FLAG_VICTORY);
        // req.request(EventRequester.FLAG_REWARD);
        // req.request(EventRequester.SCORE_RESET);
        // req.request(EventRequester.WATCH_DAMAGE);
        // req.request(EventRequester.SOCCER_GOAL);
        // req.request(EventRequester.BALL_POSITION);
        // req.request(EventRequester.FLAG_POSITION);
        // req.request(EventRequester.FLAG_DROPPED);
        // req.request(EventRequester.FLAG_CLAIMED);
    }



    public void cmd_help(String name) {
        m_botAction.smartPrivateMessageSpam( name, helpmsg );
    }

    public void cmd_signup(String name, String email) {
        //check if valid email address, if not then exit
        if (!email.contains("@") || !email.contains(".")) {
            // m_botAction.sendPublicMessage("Invalid Email Adress entered!");
            m_botAction.sendSmartPrivateMessage(name, "Invalid Email Adress entered!");
            return;
        }

        //get signup Query
        PreparedStatement ps_signup = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("signup"));

        //put values in prepared statement
        try {
            ps_signup.clearParameters();
            ps_signup.setString(1, Tools.addSlashesToString(name));
            ps_signup.setString(2, Tools.addSlashesToString(email));
            // m_botAction.sendPublicMessage(ps_signup.toString());
            ps_signup.execute();
            pbClient.sendNote( null, getEmailByUserName(name), "", "Reply with 'verify' to complete signup!");
            m_botAction.sendSmartPrivateMessage(name, "Signed Up " + name + " : " + email + " Successfully!");
            m_botAction.sendPublicMessage("Debug: Signed Up " + name + " Successfully!");
        } catch (SQLException | PushbulletException e1) {
            try {
                for (Throwable x : ps_signup.getWarnings()) {
                    if (x.getMessage().toLowerCase().contains("unique")) {
                        // m_botAction.sendPublicMessage(email + " is already registered by " + getUserNameByEmail(email));
                        m_botAction.sendSmartPrivateMessage(name, email + " is already registered by " + getUserNameByEmail(email));
                    } else {
                        m_botAction.sendPublicMessage("Error: " + x.getMessage());
                        e1.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                m_botAction.sendPublicMessage("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void cmd_enable(String name) {
        handleNewPush(name, "enable", true);
    }

    public void cmd_disable(String name) {
        handleNewPush(name, "disable", true);
    }

    public void cmd_push(String name, String msg) {
        try {
            pbClient.sendNote( null, getEmailByUserName(name), "", msg);
            // m_botAction.sendPublicMessage("Private Message: '" + msg + "' Pushed Successfully to " + name + ": " + getEmailByUserName(name));
        } catch( PushbulletException e ) {
            // Huh, didn't work
        }
    }

    public void cmd_beep(String name, String msg) {
        handleNewPush(name, msg, false);
    }

    public void cmd_challenge(String name) {
        String msg = "(MatchBot3)>Axwell is challenging you for a game of 3vs3 TWJD versus Rage. Captains/assistants, ?go twjd and pm me with '!accept Rage'";
        messagePlayerSquadMembers(name, msg);
        m_botAction.sendPublicMessage("Debug: " + msg);
    }

    public void cmd_accept(String name) {
        String msg = "(MatchBot3)>A game of 3vs3 TWJD versus Rage will start in ?go twjd in 30 seconds";
        messagePlayerSquadMembers(name, msg);
        m_botAction.sendPublicMessage("Debug: " + msg);
    }

    public void cmd_go(String name, String arena) {
        m_botAction.sendPublicMessage("!go Doesn't do anything right now...");
    }

    public void cmd_die(String name) {
        try {
            Thread.sleep(50);
        }
        catch (Exception e) {};

        handleDisconnect();

        m_botAction.die("!die by " + name);
    }

    @Override
    public void handleEvent(Message event) {
        String message = event.getMessage();
        Integer type = event.getMessageType();
        String name = null;

        if(type == Message.REMOTE_PRIVATE_MESSAGE || type == Message.CHAT_MESSAGE)
            name = event.getMessager();
        else
            name = m_botAction.getPlayerName( event.getPlayerID() );

        //exit if name isn't returned
        if (name == null) return;

        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE || type == Message.CHAT_MESSAGE) {
            if (message.toLowerCase().startsWith("!signup "))
                cmd_signup(name, message.substring(message.indexOf(" ") + 1));
            else if (message.equalsIgnoreCase("!enable"))
                cmd_enable(name);
            else if (message.equalsIgnoreCase("!disable"))
                cmd_disable(name);
            else if (message.toLowerCase().startsWith("!push "))
                cmd_push(name, message.substring(message.indexOf(" ") + 1));
            else if (message.toLowerCase().startsWith("!beep "))
                cmd_beep(name, message.substring(message.indexOf(" ") + 1));
            else if (message.equalsIgnoreCase("!challenge"))
                cmd_challenge(name);
            else if (message.equalsIgnoreCase("!accept"))
                cmd_accept(name);
            else if (message.toLowerCase().startsWith("!go "))
                cmd_go(name, message.substring(message.indexOf(" ") + 1));
            else if (message.equalsIgnoreCase("!die") && m_opList.isSmod(name))
                cmd_die(name);
            else if (message.equalsIgnoreCase("!help"))
                cmd_help(name);
        }


        /*
            if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!getlink")) {
            String squadChannel = getSquadChannel(name);
                if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel
                m_botAction.sendSmartPrivateMessage(name, "https://www.pushbullet.com/channel?tag=" + squadChannel);
            // String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
            m_botAction.sendPublicMessage(name);
            m_botAction.sendPublicMessage(String.valueOf(event.getPlayerID()));
            }
        */

        /*
            if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!createchannel ")) {
            String squadChannel = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
                if (this.createSquadChannel(name, squadChannel)) {
                    m_botAction.sendPublicMessage(squadChannel + " created successfully!");
                } else {
                    m_botAction.sendPublicMessage(squadChannel + " creation failed!");
                }
            }
        */

        /*  if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!notify ")) {
            String userName = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
            String msg = "Reply to me with commands like 'jd' to beep on channel!";
            try{
                pbClient.sendNote( null, getEmailByUserName(userName), "", msg);
                m_botAction.sendPublicMessage("Private Message: '" + msg + "' Pushed Successfully!");
            } catch( PushbulletException e ){
                // Huh, didn't work
            }
            }*/
        /*
            if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!pushchannel ")) {
            //m_botAction.sendSmartPrivateMessage(name, "Message: '" + msg + "' Pushed Successfully!");
            String msg = event.getMessage().substring(event.getMessage().indexOf(" ") + 1);
            String squadChannel = getSquadChannel(name);
                if (squadChannel == "") {return; } //means player's squad doesn't have a registered channel

            try{
                pbClient.sendChannelMsg(squadChannel, "", msg);
                m_botAction.sendPublicMessage("Channel Message: '" + msg + "' Pushed Successfully!");
                //m_botAction.sendSmartPrivateMessage(name, "Channel Message: '" + msg + "' Pushed Successfully!");
            } catch( PushbulletException e ){
                // Huh, didn't work
            }
            }
        */

        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!pushes")) {
            List<Push> pushes = null;

            try {
                pushes = pbClient.getPushes();
            } catch (PushbulletException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //m_botAction.sendSmartPrivateMessage(name, "Number of pushes: " + pushes.size() );
            //m_botAction.sendSmartPrivateMessage(name, pushes.get(0).toString());
            Push lastPush = pushes.get(0);

            m_botAction.sendSmartPrivateMessage(name, "getBody: " + lastPush.getBody().toString());
            //try {Thread.sleep(100);} catch (InterruptedException e) {// TODO Auto-generated catch block e.printStackTrace();}
            m_botAction.sendSmartPrivateMessage(name, "getIden: " + lastPush.getIden().toString());
            //m_botAction.sendSmartPrivateMessage(name, "getOwner_iden: " + lastPush.getOwner_iden().toString());
            ////m_botAction.sendSmartPrivateMessage(name, "getReceiver_email: " + lastPush.getReceiver_email().toString());
            ////m_botAction.sendSmartPrivateMessage(name, "getReceiver_email_normalized : " + lastPush.getReceiver_email_normalized().toString());
            ////m_botAction.sendSmartPrivateMessage(name, "getReceiver_iden: " + lastPush.getReceiver_iden().toString());
            ////m_botAction.sendSmartPrivateMessage(name, "getSender_email: " + lastPush.getSender_email().toString());
            ////m_botAction.sendSmartPrivateMessage(name, "getSender_email_normalized : " + lastPush.getSender_email_normalized().toString());
            ////m_botAction.sendSmartPrivateMessage(name, "getSender_iden: " + lastPush.getSender_iden().toString());
            //m_botAction.sendSmartPrivateMessage(name, "getTitle: " + lastPush.getTitle().toString());
            m_botAction.sendSmartPrivateMessage(name, "getType: " + lastPush.getType().toString());
            //m_botAction.sendSmartPrivateMessage(name, "getUrl: " + lastPush.getUrl().toString());
            //m_botAction.sendSmartPrivateMessage(name, "getClass: " + lastPush.getClass().toString());
        }

        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!listen")) {
            m_botAction.sendSmartPrivateMessage(name, "Listening Started!");
            pbClient.startWebsocket();
        }


        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().toLowerCase().startsWith("!stoplisten")) {
            m_botAction.sendSmartPrivateMessage(name, "Listening Halted!");
            pbClient.stopWebsocket();
        }

    }

    /**

        @param statementName = signup : @PlayerName, @PushBulletEmail
        @param statementName = createchannel : @PlayerName, @ChannelName
        @param statementName = getusernamebyemail : @PushBulletEmail
        @param statementName = getemailbyusername : @PlayerName

        @return preparedStatement Query
    */
    public String getPreparedStatement(String statementName) {
        String preparedStatement = "";

        switch (statementName.toLowerCase()) {
        case "signup":
            preparedStatement =
                   "SET @PlayerName = ?, @PushBulletEmail = ?;"
                +   "DELETE PBA FROM trench_TrenchWars.tblPBAccount AS PBA WHERE fbVerified = 0 AND TIMESTAMPDIFF(MINUTE, fdCreated ,NOW()) > 30;"
                +   "DELETE PBA FROM trench_TrenchWars.tblPBAccount AS PBA "
                +   "JOIN trench_TrenchWars.tblUser AS U ON U.fnUserID = PBA.fnPlayerID AND U.fcUserName = @PlayerName;"
                +   "INSERT INTO trench_TrenchWars.tblPBAccount (fnPlayerID, fcPushBulletEmail, fdCreated)"
                +   "SELECT fnUserID, @PushBulletEmail, NOW() FROM trench_TrenchWars.tblUser WHERE fcUserName = @PlayerName  AND ISNULL(fdDeleted) LIMIT 1;";
            break;

        /*
            case "createchannel":
            preparedStatement =
                    " SET @PlayerName = ?, @ChannelName = ?;"
                +   " DELETE FROM trench_TrenchWars.tblPBSquadChannel WHERE fnSquadID ="
                +   "   (SELECT T.fnTeamID FROM trench_TrenchWars.tblTeam AS T"
                +   "   JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
                +   "   JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID"
                +   "   WHERE U.fcUserName = @PlayerName AND isnull(T.fdDeleted));"
                +   " INSERT INTO trench_TrenchWars.tblPBSquadChannel (fnSquadID, fcChannelName)"
                +   " SELECT T.fnTeamID, @ChannelName AS fcChannelName"
                +   " FROM trench_TrenchWars.tblTeam AS T"
                +   " JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
                +   " JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID"
                +   " WHERE U.fcUserName = @PlayerName AND isnull(T.fdDeleted);";
            break;
        */
        case "getusernamebyemail": //can't use @Params if expecting recordset results
            preparedStatement =
                " SELECT U.fcUserName FROM trench_TrenchWars.tblPBAccount AS PBA"
                +   " JOIN trench_TrenchWars.tblUser AS U ON PBA.fnPlayerID = U.fnUserID WHERE PBA.fcPushBulletEmail = ?;";
            break;

        case "getemailbyusername": //can't use @Params if expecting recordset results
            preparedStatement =
                " SELECT PBA.fcPushBulletEmail FROM trench_TrenchWars.tblPBAccount AS PBA"
                +   " JOIN trench_TrenchWars.tblUser AS U ON PBA.fnPlayerID = U.fnUserID WHERE U.fcUserName = ?;";
            break;

        case "interpretbeep": //can't use @Params if expecting recordset results
            preparedStatement =
                " SELECT fcCommand, fcCommandShortDescription FROM trench_TrenchWars.tblPBCommands"
                +   " WHERE INSTR(?, fcCommand) > 0;";
            break;

        case "interpretcommand": //can't use @Params if expecting recordset results
            preparedStatement =
                " SELECT fcCommand, fcCommandShortDescription, fnSettingUpdate  FROM trench_TrenchWars.tblPBCommands"
                +   " WHERE (INSTR(?, fcCommand) > 0 AND fnSettingUpdate = 0) OR (? = fcCommand);";
            break;

        /*
            case "getsquadchannel": //can't use @Params if expecting recordset results
            preparedStatement =
                    " SELECT PBS.fcChannelName FROM trench_TrenchWars.tblPBSquadChannel AS PBS"
                +   " JOIN trench_TrenchWars.tblTeam AS T ON T.fnTeamID = PBS.fnSquadID"
                +   " JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
                +   " JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID AND U.fcUserName = ? AND isnull(T.fdDeleted);";
            break;
        */
        case "getplayersquadmembers": //can't use @Params if expecting recordset results
            preparedStatement =
                " SELECT U.fnUserID, U.fcUserName, PBA.fcPushBulletEmail, PBA.fbDisabled, T.fcTeamName FROM trench_TrenchWars.tblUser AS U"
                +   " JOIN trench_TrenchWars.tblTeamUser AS TU ON TU.fnUserID = U.fnUserID"
                +   " JOIN trench_TrenchWars.tblTeam AS T ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
                +   " JOIN (	SELECT T.fnTeamID FROM trench_TrenchWars.tblTeam AS T"
                +   "		JOIN trench_TrenchWars.tblTeamUser AS TU ON T.fnTeamID = TU.fnTeamID AND fnCurrentTeam = 1"
                +   "		JOIN trench_TrenchWars.tblUser AS U ON TU.fnUserID = U.fnUserID AND U.fcUserName = ?"
                +   "	 ) AS SID ON SID.fnTeamID = T.fnTeamID"
                +   " JOIN trench_TrenchWars.tblPBAccount AS PBA ON U.fnUserID = PBA.fnPlayerID;";
            break;

        case "enabledisablepb": //can't use @Params if expecting recordset results
            preparedStatement =
                " SET @PlayerName = ?;"
                +   " UPDATE trench_TrenchWars.tblPBAccount"
                +   " SET fbDisabled = ?"
                +   " WHERE fnPlayerID = (SELECT U.fnUserID FROM trench_TrenchWars.tblUser AS U WHERE U.fcUserName = @PlayerName LIMIT 1);";
            break;

        case "verifyaccount": //can't use @Params if expecting recordset results
            preparedStatement =
                " SET @PlayerName = ?;"
                +   " UPDATE trench_TrenchWars.tblPBAccount"
                +   " SET fbVerified = 1"
                +   " WHERE fnPlayerID = (SELECT U.fnUserID FROM trench_TrenchWars.tblUser AS U WHERE U.fcUserName = @PlayerName LIMIT 1);";
            break;
        }

        return preparedStatement;
    }

    public String getUserNameByEmail(String email) {
        String userName = "";
        PreparedStatement ps_getusernamebyemail = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getusernamebyemail"));

        try {
            ps_getusernamebyemail.clearParameters();
            ps_getusernamebyemail.setString(1, Tools.addSlashesToString(email));
            ps_getusernamebyemail.execute();

            try (ResultSet rs = ps_getusernamebyemail.getResultSet()) {
                if (rs.next()) {
                    userName = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return userName;
    }

    public String getEmailByUserName(String userName) {
        String email = "";
        PreparedStatement ps_getemailbyusername = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getemailbyusername"));

        try {
            ps_getemailbyusername.clearParameters();
            ps_getemailbyusername.setString(1, Tools.addSlashesToString(userName));
            ps_getemailbyusername.execute();

            try (ResultSet rs = ps_getemailbyusername.getResultSet()) {
                if (rs.next()) {
                    email = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return email;
    }

    public ResultSet getInterpretCommand(String userName, String userMsg) {
        //String commandResponseOriginal = commandResponse;
        ResultSet rs = null;
        PreparedStatement ps_getinterpretbeep = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("interpretcommand"));

        try {
            ps_getinterpretbeep.clearParameters();
            ps_getinterpretbeep.setString(1, Tools.addSlashesToString(userMsg));
            ps_getinterpretbeep.setString(2, Tools.addSlashesToString(userMsg));
            ps_getinterpretbeep.execute();
            rs = ps_getinterpretbeep.getResultSet();
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //if (commandResponse == commandResponseOriginal) {commandResponse = "";}
        return rs;
    }

    /*
        public String getSquadChannel(String userName) {
        String squadChannel = "";
        PreparedStatement ps_getsquadchannel = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getsquadchannel"));
        try {
            ps_getsquadchannel.clearParameters();
            ps_getsquadchannel.setString(1, Tools.addSlashesToString(userName));
            ps_getsquadchannel.execute();
            try (ResultSet rs = ps_getsquadchannel.getResultSet()) {
                if (rs.next()) {
                    squadChannel = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return squadChannel;
        }
    */

    /*
        public Boolean createSquadChannel(String userName, String squadChannel) {
        PreparedStatement ps_createsquadchannel = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("createchannel"));
        try {
            ps_createsquadchannel.clearParameters();
            ps_createsquadchannel.setString(1, Tools.addSlashesToString(userName));
            ps_createsquadchannel.setString(2, Tools.addSlashesToString(squadChannel));
            ps_createsquadchannel.execute();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
        }
    */

    public void switchAlertsPB (String userName, Integer Disable) {
        PreparedStatement ps_switchalerts = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("enabledisablepb"));

        try {
            ps_switchalerts.clearParameters();
            ps_switchalerts.setString(1, Tools.addSlashesToString(userName));
            ps_switchalerts.setInt(2, Disable);
            ps_switchalerts.execute();
            //m_botAction.sendPublicMessage(ps_switchalerts.toString());
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Tools.printLog(e.getMessage());
        }
    }

    public void verifyAccount (String userName) {
        PreparedStatement ps_verifyaccount = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("verifyaccount"));

        try {
            ps_verifyaccount.clearParameters();
            ps_verifyaccount.setString(1, Tools.addSlashesToString(userName));
            ps_verifyaccount.execute();
            //m_botAction.sendPublicMessage(ps_verifyaccount.toString());
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            m_botAction.sendPublicMessage(e.getMessage());
        }
    }


    public void messagePlayerSquadMembers(String userName, String msg) {
        String squadName = "";

        if (msg == "") {
            return;
        }

        PreparedStatement ps_messagePlayerSquadMembers = ba.createPreparedStatement(db, connectionID, this.getPreparedStatement("getplayersquadmembers"));

        try {
            ps_messagePlayerSquadMembers.clearParameters();
            ps_messagePlayerSquadMembers.setString(1, Tools.addSlashesToString(userName));
            ps_messagePlayerSquadMembers.execute();

            try (ResultSet rs = ps_messagePlayerSquadMembers.getResultSet()) {
                while (rs.next()) {
                    if (rs.getInt("fbDisabled") != 1) {
                        pbClient.sendNote( null, rs.getString("fcPushBulletEmail"), "", msg);
                        m_botAction.sendPublicMessage("Debug: Pushed to " + rs.getString("fcUserName")); //+ " | " + rs.getString("fcPushBulletEmail") );
                        squadName = rs.getString("T.fcTeamName");
                    }
                }
            } catch (PushbulletException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                m_botAction.sendPublicMessage(e.getMessage());
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            m_botAction.sendPublicMessage(e.getMessage());
        } finally {
            if (squadName != "") {
                m_botAction.sendSquadMessage(squadName, msg);
            }
        }
    }

    public void handleNewPush(String playerName, String userMsg, Boolean allowSystemUpdates) {
        String squadAlert = "";
        Boolean settingChange = false;
        ResultSet rs_InterpretCommand = getInterpretCommand(playerName, userMsg);

        try {
            while (rs_InterpretCommand.next()) {
                if (rs_InterpretCommand.getInt("fnSettingUpdate") == 1 && allowSystemUpdates) {
                    //This is a setting command
                    try {
                        switch (rs_InterpretCommand.getString("fcCommand").toLowerCase()) {
                        case "enable":
                            switchAlertsPB(playerName, 0);
                            settingChange = true;
                            break;

                        case "disable":
                            switchAlertsPB(playerName, 1);
                            settingChange = true;
                            break;

                        case "verify":
                            verifyAccount(playerName);
                            settingChange = true;
                            break;
                        }

                        //if setting change above matches, send personal note to player's pushbullet account letting them know of successful change
                        if (settingChange) {
                            pbClient.sendNote( null, getEmailByUserName(playerName), "", rs_InterpretCommand.getString("fcCommandShortDescription"));
                            m_botAction.sendSmartPrivateMessage(playerName, rs_InterpretCommand.getString("fcCommandShortDescription"));
                        }
                    } catch (PushbulletException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } finally {
                        settingChange = false;
                    }
                } else {
                    //This is a beep
                    if (squadAlert != "") {
                        squadAlert += ",";
                    }

                    squadAlert += rs_InterpretCommand.getString("fcCommandShortDescription");
                }
            }
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        //send a message to everyone on squad (in game) and everyone who has pushbullet setup to alert of beep
        if (squadAlert != "") {
            squadAlert = playerName + " beeped for: " + squadAlert;
            messagePlayerSquadMembers(playerName, squadAlert);
            m_botAction.sendPublicMessage("Debug: " + playerName + " : " + squadAlert);
        } else {
            m_botAction.sendPublicMessage("Filtered Message From " + playerName + " : " + userMsg);
        }
    }


    public void handleDisconnect() {
        //ba.closePreparedStatement(db, connectionID, this.ps_signup);
        ba.cancelTasks();
        pbClient.stopWebsocket();
    }

    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
        String pushAuth = ba.getGeneralSettings().getString("PushAuth");
        pbClient = new PushbulletClient(pushAuth);
        StartPushbulletListener();
    }

    public void StartPushbulletListener() {
        //m_botAction.sendPublicMessage("1");
        pbClient.addPushbulletListener(new PushbulletListener() {
            @Override
            public void pushReceived(PushbulletEvent pushEvent) {
                //This is probably doubling dipping on the rate limit by pulling the message the bot just posted
                //possibly need to change this once we confirm
                List<Push> pushes = null;
                pushes = pushEvent.getPushes();
                Push lastPush = pushes.get(0);
                String userMsg = lastPush.getBody().toString();

                String senderEmail = lastPush.getSender_email().toString();

                if (senderEmail == "") {
                    return;    //means it came from the channel, no need to push it back to the channel
                }

                String playerName = getUserNameByEmail(senderEmail);

                if (playerName == "") {
                    return;    //means it came from the bot account, probably using !push
                }

                //handle push
                handleNewPush(playerName, userMsg, true);
            }

            @Override
            public void devicesChanged(PushbulletEvent pushEvent) {
                m_botAction.sendPublicMessage("devicesChanged PushEvent received: " + pushEvent);
            }

            @Override
            public void websocketEstablished(PushbulletEvent pushEvent) {
                m_botAction.sendPublicMessage("websocketEstablished PushEvent received: " + pushEvent);
            }
        });

        m_botAction.sendPublicMessage("Getting previous pushes to find most recent...");

        try {
            pbClient.getPushes(1);
        } catch (PushbulletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        m_botAction.sendPublicMessage("Starting websocket...try sending a push now.");

        pbClient.startWebsocket();
        m_botAction.sendPublicMessage("Listening Started!");
    }
}
