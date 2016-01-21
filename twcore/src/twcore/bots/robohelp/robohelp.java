package twcore.bots.robohelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.SQLResultEvent;
import twcore.core.util.SearchableStructure;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;

/*
    IMPORTANT NOTICE: In effort to make call claiming easier, call ID# 'aliases'
*/

public class robohelp extends SubspaceBot {
    public static final String ALERT_CHAT = "training"; // chat that the new player alerts get sent to

    static int TIME_BETWEEN_ADS = 390000;//6.5 * 60000;
    public static final int CALL_INTERVAL = 1;
    public static final String CALL_AD = "  (!claimhelp)";
    public static final int LINE_SIZE = 100;
    public static final int CALL_EXPIRATION_TIME = 3 * Tools.TimeInMillis.MINUTE; // Time after which a call can't
    // can't be claimed (onit/gotit)
    public static final int NEWB_EXPIRATION_TIME = 7 * Tools.TimeInMillis.MINUTE; // Time after which a call can't
    public static final String ZONE_CHANNEL = "Zone Channel";
    public static final String WBOT = "TW-WelcomeBot";

    boolean m_banPending = false;
    boolean twdchat = false;
    boolean m_strictOnIts = true;
    String m_lastBanner = null;
    BotSettings m_botSettings;

    SearchableStructure search;
    OperatorList opList;
    TreeMap<String, String> rawData;
    Map<String, PlayerInfo> m_playerList;

    CommandInterpreter m_commandInterpreter;
    String lastHelpRequestName = null;
    String lastNewPlayerName = "";

    final String mySQLHost = "website";
    Vector<EventData> callEvents = new Vector<EventData>();
    Vector<NewbCall> newbs = new Vector<NewbCall>();
    HashMap<String, String> banned = new HashMap<String, String>();
    LinkedList<String> alert = new LinkedList<String>(); // new player alert pms
    Vector<String> newbNames = new Vector<String>(); // holds last 20 new
    // players
    TreeMap<String, NewbCall> newbHistory = new TreeMap<String, NewbCall>();

    /** Wing's way */
    TreeMap<Integer, Call> callList = new TreeMap<Integer, Call>();
    Vector<Integer> calls = new Vector<Integer>();
    long lastAlert;
    int callsUntilAd = CALL_INTERVAL;
    int currentID = 1;
    String findPopulation = "";
    int setPopID = -1;
    boolean timeFormat = false;
    boolean ellipsisAfterCalls = true;        // Robohelp> ... after calls w/ no #

    String lastStafferClaimedCall;

    Random random;
    String[] mag8;

    HashSet<String> triggers;

    public robohelp(BotAction botAction) {
        super(botAction);

        m_botSettings = m_botAction.getBotSettings();

        lastAlert = System.currentTimeMillis();
        populateSearch();
        opList = botAction.getOperatorList();
        m_playerList = Collections.synchronizedMap(new HashMap<String, PlayerInfo>());

        m_commandInterpreter = new CommandInterpreter(m_botAction);
        registerCommands();
        m_botAction.getEventRequester().request(EventRequester.MESSAGE);
        loadBanned();
        random = new Random();
        mag8 = new String[] { "It is certain.", "It is decidedly so.", "Without a doubt.", "Yes, definitely.", "You may rely on it.", "As I see it, yes.", "Most likely.", "Outlook good.", "Yes.",
                              "All signs point to yes.", "Reply hazy, try again.", "Ask again later.", "Better not tell you now.", "Cannot predict now.", "Concentrate and ask again.", "Don't count on it.",
                              "My reply is no.", "My sources say no.", "Outlook not so good.", "Very doubtful."
                            };
        triggers = new HashSet<String>();
    }

    private void loadBanned() {
        try {
            BotSettings m_botSettings = m_botAction.getBotSettings();
            banned.clear();
            //
            String ops[] = m_botSettings.getString("Bad People").split(",");

            for (int i = 0; i < ops.length; i++)
                banned.put(ops[i].toLowerCase(), ops[i]);
        } catch (Exception e) {
            Tools.printStackTrace("Method Failed: ", e);
        }

    }

    void registerCommands() {
        int acceptedMessages;

        // *** Any source cmds
        acceptedMessages = Message.REMOTE_PRIVATE_MESSAGE | Message.PRIVATE_MESSAGE | Message.CHAT_MESSAGE;

        m_commandInterpreter.registerCommand("!claimhelp", acceptedMessages, this, "claimHelpScreen", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!calls", acceptedMessages, this, "handleCalls", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!stats", acceptedMessages, this, "handleStats", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!newbs", acceptedMessages, this, "handleNewbs", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!false", acceptedMessages, this, "handleFalseNewb", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!last", acceptedMessages, this, "handleLast", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!help", acceptedMessages, this, "mainHelpScreen", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!lookup", acceptedMessages, this, "handleLookup", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!mystats", acceptedMessages, this, "handleMystats", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!hourlystats", acceptedMessages, this, "handleHourlyStats", OperatorList.ZH_LEVEL);

        // *** PM cmds
        acceptedMessages = Message.REMOTE_PRIVATE_MESSAGE | Message.PRIVATE_MESSAGE;

        // Player commands
        m_commandInterpreter.registerCommand("!next", acceptedMessages, this, "handleNext");
        m_commandInterpreter.registerCommand("!summon", acceptedMessages, this, "handleSummon");

        // ZH+ commands
        //m_commandInterpreter.registerCommand("!hosted", acceptedMessages, this, "handleDisplayHosted", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!alert", acceptedMessages, this, "toggleAlert", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!time", acceptedMessages, this, "changeTimeFormatP", OperatorList.ZH_LEVEL);

        // Mod
        m_commandInterpreter.registerCommand("!trigger", acceptedMessages, this, "handleTrigger", OperatorList.MODERATOR_LEVEL);

        // Smod
        m_commandInterpreter.registerCommand("!truncate", acceptedMessages, this, "handleTruncate", OperatorList.SMOD_LEVEL);
        m_commandInterpreter.registerCommand("!say", acceptedMessages, this, "handleSay", OperatorList.SMOD_LEVEL);
        m_commandInterpreter.registerCommand("!banned", acceptedMessages, this, "handleListTellBanned", OperatorList.SMOD_LEVEL);
        m_commandInterpreter.registerCommand("!unban", acceptedMessages, this, "unbanTell", OperatorList.SMOD_LEVEL);
        m_commandInterpreter.registerCommand("!addban", acceptedMessages, this, "handleAddBan", OperatorList.SMOD_LEVEL);
        m_commandInterpreter.registerCommand("!die", acceptedMessages, this, "handleDie", OperatorList.SMOD_LEVEL);
        m_commandInterpreter.registerCommand("!reload", acceptedMessages, this, "handleReload", OperatorList.SMOD_LEVEL);

        // Sysop+
        m_commandInterpreter.registerCommand("!loadnewbs", acceptedMessages, this, "loadLastNewbs", OperatorList.SYSOP_LEVEL);

        // Special (via bot)
        m_commandInterpreter.registerCommand(">echo<", acceptedMessages, this, "echoMessageToChat", OperatorList.BOT_LEVEL);

        // *** Chat-only cmds
        acceptedMessages = Message.CHAT_MESSAGE;

        m_commandInterpreter.registerCommand("!8ball", acceptedMessages, this, "handle8Ball", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!repeat", acceptedMessages, this, "handleRepeat", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!warn", acceptedMessages, this, "handleWarn", OperatorList.ZH_LEVEL);
        //m_commandInterpreter.registerCommand("!ban", acceptedMessages, this, "handleBan", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!wiki", acceptedMessages, this, "handleWikipedia", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!status", acceptedMessages, this, "handleStatus", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!dictionary", acceptedMessages, this, "handleDictionary", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!thesaurus", acceptedMessages, this, "handleThesaurus", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!javadocs", acceptedMessages, this, "handleJavadocs", OperatorList.ZH_LEVEL);
        m_commandInterpreter.registerCommand("!time", acceptedMessages, this, "changeTimeFormatC", OperatorList.ZH_LEVEL);
        // ER+
        m_commandInterpreter.registerCommand("!google", acceptedMessages, this, "handleGoogle", OperatorList.ER_LEVEL);
        m_commandInterpreter.registerCommand("!tell", acceptedMessages, this, "handleTell", OperatorList.ER_LEVEL);

        /*  This cmd is not registered
            if (!m_strictOnIts)
            m_commandInterpreter.registerDefaultCommand(acceptedMessages, this, "handleChat");
        */

        acceptedMessages = Message.ARENA_MESSAGE;

        m_commandInterpreter.registerCommand("Ban", acceptedMessages, this, "handleBanNumber");
    }

    public void handleStatus(String name, String message) {

        if (!m_botAction.SQLisOperational()) {
            m_botAction.sendChatMessage("NOTE: The database connection is down. Some other bots might  experience problems too.");
            return;
        }

        try {
            m_botAction.SQLQueryAndClose(mySQLHost, "SELECT * FROM tblCall LIMIT 0,1");
            m_botAction.sendChatMessage("Statistic Recording: Operational");
        } catch (Exception e) {
            m_botAction.sendChatMessage("NOTE: The database connection is down. Some other bots might experience problems too.");
        }
    }

    /**
        Forms an appropriate query to dictionary.reference.com.

        @param name
                  Name of individual querying
        @param message
                  Query
    */
    public void handleDictionary(String name, String message) {
        m_botAction.sendChatMessage("Dictionary definition:  http://dictionary.reference.com/search?q=" + message.replaceAll("\\s", "%20"));
    }

    /**
        Forms an appropriate query to thesaurus.reference.com.

        @param name
                  Name of individual querying
        @param message
                  Query
    */
    public void handleThesaurus(String name, String message) {
        m_botAction.sendChatMessage("Thesaurus entry:  http://thesaurus.reference.com/search?q=" + message.replaceAll("\\s", "%20"));
    }

    /**
        Forms an appropriate query to javadocs.org

        @param name
                  Name of individual querying
        @param message
                  Query
    */
    public void handleJavadocs(String name, String message) {
        m_botAction.sendChatMessage("Javadocs entry:  http://javadocs.org/" + message.replaceAll("\\s", "%20"));
    }

    /**
        Forms an appropriate query to google.com

        @param name
                  Name of individual querying
        @param message
                  Query
    */
    public void handleGoogle(String name, String message) {
        m_botAction.sendChatMessage("Google search: http://lmgtfy.com/?q=" + message.replaceAll("\\s", "+"));
    }

    /*  public void handleGoogle( String name, String message ){
        m_botAction.sendChatMessage( "Google search results for " + message + ": " + doGoogleSearch( message ) );
        }*/

    /**
        Forms an appropriate query to wikipedia.org

        @param name
                  Name of individual querying
        @param message
                  Query
    */
    public void handleWikipedia(String name, String message) {
        m_botAction.sendChatMessage("Wikipedia search: http://en.wikipedia.org/wiki/" + message.replaceAll("\\s", "%20"));
    }

    /*    public String doGoogleSearch( String searchString ){

            try {

                GoogleSearch s = new GoogleSearch();
                s.setKey( m_botSettings.getString( "GoogleKey" ) );
                //s.setKey( "EsAMyNxQFHLUiEnJqdsU1IKpEMl0yiDl" );
                s.setQueryString( searchString );
                s.setMaxResults( 1 );
                GoogleSearchResult r = s.doSearch();
                GoogleSearchResultElement[] elements = r.getResultElements();
                return elements[0].getURL();
            } catch( Exception e ){
                Tools.printStackTrace(e);
                return new String( "Nothing found." );
           }
        }*/

    /**
        Because we're definitely too immature NOT to have this command.
        @param name
        @param msg
    */
    public void handleSay(String name, String msg) {
        // Let's cause mischief with mischief-causers.
        int i = random.nextInt(15);

        if( i == 0 ) {
            i = random.nextInt(5);

            switch (i) {
            case 0:
                m_botAction.sendChatMessage( name + "> im a pretty pretty princess");
                break;

            case 1:
                m_botAction.sendChatMessage( "Oh, god ... it smells like " + name + " in here.");
                break;

            case 2:
                m_botAction.sendChatMessage( "Anyone else think " + name + " should be axed?");
                break;

            case 3:
                m_botAction.sendChatMessage( name + "> ITTY BITTY BABY ITTY BITTY BOAT");
                break;

            case 4:
                m_botAction.sendChatMessage( "When I think about " + name + ", I touch myself.");
                break;
            }
        } else {
            echoMessageToChat(name, msg);
        }

    }

    /**
        Echoes provided message to chat. Used by PubSystem for the event buy system, but can be used elsewhere too.
        @param name
        @param msg
    */
    public void echoMessageToChat(String name, String msg) {
        m_botAction.sendChatMessage(msg);
    }

    public void handleBanNumber(String name, String message) {
        String number;

        if (message.startsWith("activated #")) {
            number = message.substring(message.indexOf(' '));

            if (m_banPending == true && number != null) {
                m_botAction.sendUnfilteredPublicMessage("?bancomment " + number + " Ban by " + m_lastBanner
                                                        + " for abusing the alert commands.  Mod: CHANGE THIS COMMENT & don't forget to ADD YOUR NAME.");
                m_banPending = false;
                m_lastBanner = null;
            }
        }
    }

    public void populateSearch() {
        search = new SearchableStructure();
        rawData = new TreeMap<String, String>();

        try {
            FileReader reader = new FileReader(m_botAction.getDataFile("HelpResponses.txt"));
            BufferedReader in = new BufferedReader(reader);
            String line;

            int i = 0;

            do {
                line = in.readLine();

                try {
                    if (line != null) {
                        line = line.trim();
                        int indexOfLine = line.indexOf('|');

                        if (indexOfLine != -1 && line.startsWith("#") == false) {
                            String key = line.substring(0, indexOfLine);
                            String response = line.substring(indexOfLine + 1);
                            search.add(response, key);
                            rawData.put(response, key);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error in HelpResponses.txt near: Line " + i);
                }

                i++;
            } while (line != null);

            in.close();
            reader.close();
        } catch (IOException e) {
            Tools.printStackTrace(e);
        }
    }

    @Override
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena("#robopark");
        m_botAction.sendUnfilteredPublicMessage("?chat=" + m_botAction.getGeneralSettings().getString("Staff Chat") + "," + ALERT_CHAT + "," + ba.getBotSettings().getString("TWDChat"));
        m_botAction.sendUnfilteredPublicMessage("?blogin " + m_botSettings.getString("Banpassword"));
        m_botAction.sendUnfilteredPublicMessage("?bantext If you would like to contest your ban, please send a ticket to www.trenchwars.org/support");
        m_botAction.ipcSubscribe(ZONE_CHANNEL);
    }

    /**
        Now used for new players only.

        @param event
                  IPC event to handle
    */
    @Override
    public void handleEvent(InterProcessEvent event) {
        if (event.getChannel().equals(ZONE_CHANNEL) && event.getObject() instanceof IPCMessage) {
            IPCMessage ipc = (IPCMessage) event.getObject();
            String msg = ipc.getMessage();

            if (ipc.getSender().equalsIgnoreCase("ZonerBot")) {
                String[] args = msg.split(",");

                if (args[0].equals("noaccess"))
                    m_botAction.sendSmartPrivateMessage(args[1], "You must be a staff trainer to use this command.");
                else
                    m_botAction.ipcSendMessage(ZONE_CHANNEL, "newb:" + args[1] + "," + args[2], WBOT, m_botAction.getBotName());
            } else if (ipc.getSender().equalsIgnoreCase(WBOT)) {
                String[] args = msg.split(":");
                m_botAction.sendSmartPrivateMessage(args[0], args[1]);

                if (args.length == 3)
                    triggers.add(args[2].toLowerCase());
            }
        }

        /*  welcomebot will do this from now on
            try {
            if (message.startsWith("alert"))
                handleNewPlayer(message.substring(6));
            } catch (Exception e) {
            Tools.printStackTrace(e);
            }
        */
    }

    public void handleTrigger(String name, String msg) {
        if (name.equalsIgnoreCase(msg) || opList.isZH(msg)) {
            m_botAction.sendSmartPrivateMessage(name, "Alias cannot be a staff member.");
            return;
        }

        m_botAction.ipcSendMessage(ZONE_CHANNEL, "check:" + name + "," + msg, "ZonerBot", m_botAction.getBotName());
    }

    // This is to catch any backgroundqueries even though none of them need to be catched to do something with the results
    @Override
    public void handleEvent(SQLResultEvent event) {
        m_botAction.SQLClose(event.getResultSet());
    }

    public void getArenaSize(int adID, String arena) {
        findPopulation = arena;
        setPopID = adID;
        m_botAction.requestArenaList();
    }

    public String trimFill(String line) {
        if (line.length() < 25)
            for (int i = line.length(); i < 25; i++)
                line += " ";

        return line;
    }

    public void handleTruncate(String name, String msg) {
        m_botAction.sendSmartPrivateMessage(name, "Reducing helpList from a size of " + callList.size() + " to a maximum of 100.");
        Iterator<Integer> i = callList.descendingKeySet().iterator();
        int index = 0;

        while (i.hasNext()) {
            i.next();

            if (index > 99)
                i.remove();

            index++;
        }

        m_botAction.sendSmartPrivateMessage(name, "Done, now size: " + callList.size());
    }

    public void handleAdvert(String playerName, String message) {
        // HelpRequest     helpRequest;
        PlayerInfo info;

        lastHelpRequestName = playerName;

        if (opList.isBot(playerName))
            return;

        callEvents.addElement(new EventData(new java.util.Date().getTime())); //For Records
        info = m_playerList.get(playerName.toLowerCase());

        if (info == null) {
            info = new PlayerInfo(playerName);
            m_playerList.put(playerName.toLowerCase(), info);
        }

        if (info.AdvertTell() == true)
            m_botAction.sendChatMessage("NOTICE: " + playerName + " has used ?advert before. Please use !warn if needed.");
        else {
            m_botAction.sendRemotePrivateMessage(playerName, "Please do not use ?advert. " + "If you would like to request an event, please use the ?help command.");
            info.setAdvertTell(true);
            m_botAction.sendChatMessage(playerName + " has been notified that ?advert is not for non-staff.");
        }

    }

    public void handleCheater(String playerName, String message) {
        Call help;
        PlayerInfo info;

        long now = System.currentTimeMillis();
        lastHelpRequestName = playerName;

        info = m_playerList.get(playerName.toLowerCase());

        if (info == null)
            info = new PlayerInfo(playerName.toLowerCase());

        help = new CheaterCall(playerName, message, 2);
        help = storeHelp(help);
        info.addCall(help.getID());
        calls.add(help.getID());
        m_playerList.put(playerName.toLowerCase(), info);
        callsUntilAd--;
        String msg = "";

        if (playerName.startsWith("MatchBot") && message.contains("blueout")) {
            msg += "Auto-forgot #" + help.getID();
            handleForget(m_botAction.getBotName(), "forget #" + help.getID());
        } else if (callsUntilAd == 0) {
            callsUntilAd = CALL_INTERVAL;
            msg += "Call #" + help.getID() + CALL_AD;
        } else if (now - lastAlert < CALL_EXPIRATION_TIME) {
            msg += "Call #" + help.getID();
        } else if( ellipsisAfterCalls ) {
            msg = "...";
        }

        if (msg.length() > 0)
            m_botAction.sendChatMessage(msg);

        lastAlert = now;
    }

    public void handleHelp(String playerName, String message) {
        String[] response;
        HelpCall helpRequest;
        PlayerInfo info;

        if (playerName.compareTo(m_botAction.getBotName()) == 0)
            return;

        lastHelpRequestName = playerName;

        if (opList.isZH(playerName)) {
            String tempMessage = "Staff members: Please use :" + m_botAction.getBotName() + ":!lookup instead of ?help!";
            response = new String[1];
            response[0] = tempMessage;
            m_botAction.sendRemotePrivateMessage(playerName, tempMessage);
            return;
        }

        response = search.search(message);

        info = m_playerList.get(playerName.toLowerCase());
        helpRequest = new HelpCall(playerName, message, response, 0);

        if (info == null) {
            info = new PlayerInfo(playerName);
            helpRequest = (HelpCall) storeHelp(helpRequest);
            info.addCall(helpRequest.getID());
            m_playerList.put(playerName.toLowerCase(), info);
        } else {
            helpRequest.setAllowSummons(false);
            helpRequest.setQuestion(message, response);
            helpRequest.reset();
            helpRequest = (HelpCall) storeHelp(helpRequest);
            info.addCall(helpRequest.getID());
            m_playerList.put(playerName.toLowerCase(), info);
        }

        calls.add(helpRequest.getID());
        long now = System.currentTimeMillis();

        if (response.length <= 0) {
            callsUntilAd--;
            String msg = "";

            if (callsUntilAd == 0) {
                callsUntilAd = CALL_INTERVAL;
                msg += "Call #" + helpRequest.getID() + CALL_AD;
            } else if (now - lastAlert < CALL_EXPIRATION_TIME) {
                msg += "Call #" + helpRequest.getID();
            } else if( ellipsisAfterCalls ) {
                msg = "...";
            }

            if (msg.length() > 0)
                m_botAction.sendChatMessage(msg);

            lastAlert = now;
        } else {
            callsUntilAd--;
            String msg = "I'll take it! (Call #" + helpRequest.getID();

            if (callsUntilAd == 0) {
                callsUntilAd = CALL_INTERVAL;
                msg += "  See !claimhelp for detailed claim info)";
            } else
                msg += ")";

            m_botAction.sendChatMessage(msg);
            helpRequest.setTaker("RoboHelp");
            lastAlert = now;
            m_botAction.remotePrivateMessageSpam(playerName, helpRequest.getNextResponse());
            m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fcTakerName = '" + Tools.addSlashesToString("RoboHelp") + "', fnTaken = 1 WHERE fnCallID = "
                                           + helpRequest.getCallID());
            String m = helpRequest.getMessage();

            if (m != null && m.lastIndexOf("): ") > 0) {
                m = m.substring(m.lastIndexOf("): ") + 2);
                m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "INSERT INTO tblCallAuto (fnCallID, fcPlayer, fcQuestion, fcResponse) VALUES(" +
                                               helpRequest.getCallID() + ", '" + Tools.addSlashesToString(playerName) + "', '" +
                                               Tools.addSlashesToString(m) + "', '" + Tools.addSlashesToString(helpRequest.getLastResponse()[0]) + "')");
            }

            if (helpRequest.hasMoreResponses() == false) {
                helpRequest.setAllowSummons(true);
                m_botAction.sendRemotePrivateMessage(playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!summon to request live help.");
            } else if (response.length > 0)
                m_botAction.sendRemotePrivateMessage(playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!next to retrieve the next response.");
        }
    }

    public void handleNewPlayer(String sender, String message) {
        String name = message.substring(12);

        if (lastNewPlayerName.equalsIgnoreCase(name)) return;

        if (!opList.isBotExact(sender)) {
            callEvents.addElement(new EventData(new java.util.Date().getTime())); //For Records
            PlayerInfo info = m_playerList.get(sender.toLowerCase());

            if (info == null) {
                info = new PlayerInfo(sender);
                m_playerList.put(sender.toLowerCase(), info);
            }

            if (info.NewplayerTell() == true)
                m_botAction.sendChatMessage("NOTICE: " + sender + " has used ?newplayer before. Please use !warn if needed.");
            else {
                m_botAction.sendRemotePrivateMessage(sender, "Please do not use ?newplayer. " + "It is for staff bot use only.");
                info.setNewplayerTell(true);
                m_botAction.sendChatMessage(sender + " has been notified that ?newplayer is not to be used.");
            }

            return;
        }

        NewbCall newb = new NewbCall(name);
        lastNewPlayerName = name;
        newbs.add(newb);
        newbHistory.put(name.toLowerCase(), newb);
        newbNames.add(0, name.toLowerCase());

        while (newbNames.size() > 20)
            newbHistory.remove(newbNames.remove(20).toLowerCase());

        PlayerInfo info;
        info = m_playerList.get(sender.toLowerCase());

        if (info == null)
            info = new PlayerInfo(sender.toLowerCase());

        newb.setID(getNextCallID());
        calls.add(newb.getID());
        callList.put(newb.getID(), newb);
        info.addCall(newb.getID());
        long now = System.currentTimeMillis();
        callsUntilAd--;
        String msg = "";

        if (callsUntilAd == 0) {
            callsUntilAd = CALL_INTERVAL;
            msg += "Call #" + newb.getID() + CALL_AD;
        } else if (now - lastAlert < CALL_EXPIRATION_TIME) {
            msg += "Call #" + newb.getID();
        } else if( ellipsisAfterCalls ) {
            msg = "...";
        }

        if (msg.length() > 0)
            m_botAction.sendChatMessage(msg);

        lastAlert = now;

    }

    public void toggleAlert(String name, String msg) {
        if (!alert.remove(name.toLowerCase())) {
            alert.add(name.toLowerCase());
            m_botAction.sendSmartPrivateMessage(name, "Personal new player alerts ENABLED");
        } else
            m_botAction.sendSmartPrivateMessage(name, "Personal new player alerts DISABLED");
    }

    public Call storeHelp(Call help) {
        if (!m_botAction.SQLisOperational())
            return help;

        String player = help.getPlayername();

        // removed the message (question) recording
        try {
            m_botAction.SQLQueryAndClose(mySQLHost, "INSERT INTO tblCallHelp (fcUserName, fdCreated, fnType) VALUES('" + Tools.addSlashesToString(player) + "', NOW(), " + help.getCallType() + ")");
            ResultSet callid = m_botAction.SQLQuery(mySQLHost, "SELECT LAST_INSERT_ID()");

            if (callid.next())
                help.setID(callid.getInt(1), getNextCallID());

            m_botAction.SQLClose(callid);
            callList.put(help.getID(), help);
            return help;
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }

        return help;
    }

    private int getNextCallID() {
        int id = currentID;
        currentID++;
        return id;
    }

    public void recordHelp(Call help) {
        if (!m_botAction.SQLisOperational())
            return;

        String time = new SimpleDateFormat("yyyy-MM").format(Calendar.getInstance().getTime()) + "-01";

        if (help.getCallType() == 0 || help.getCallType() == 2)
            try {
                ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker()) + "' AND fnType = 0 AND fdDate = '" + time
                                                        + "'");

                if (result.next())
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker())
                                                   + "' AND fnType = 0 AND fdDate = '" + time + "'");
                else
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('" + Tools.addSlashesToString(help.getTaker())
                                                   + "', '1', '0', '" + time + "')");

                m_botAction.SQLClose(result);

                m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fcTakerName = '" + Tools.addSlashesToString(help.getTaker()) + "', fnTaken = 1 WHERE fnCallID = "
                                               + help.getCallID());
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
        else if (help.getCallType() == 1 || help.getCallType() == 4)
            try {
                ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker()) + "' AND fnType = 1 AND fdDate = '" + time
                                                        + "'");

                if (result.next())
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '" + Tools.addSlashesToString(help.getTaker())
                                                   + "' AND fnType = 1 AND fdDate = '" + time + "'");
                else
                    m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('" + Tools.addSlashesToString(help.getTaker())
                                                   + "', '1', '1', '" + time + "')");

                m_botAction.SQLClose(result);

                m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fcTakerName = '" + Tools.addSlashesToString(help.getTaker())
                                               + "', fnTaken = 1, fnType = 1 WHERE fnCallID = " + help.getCallID());
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }
    }

    public void handleClaims(String name, String message) {
        long now = System.currentTimeMillis();
        int id = -1;

        if (message.contains("#") && message.indexOf("#") < 13)
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Invalid call number.");
                return;
            }
        else if (message.equalsIgnoreCase("on it") || message.equalsIgnoreCase("got it")) {
            handleClaim(name, message);

            if(twdchat == true) {
                m_botAction.sendChatMessage("A TWD-Op has taken call " + id + " Name: " + name);
            }

            return;
        } else if (message.startsWith("on it ") || message.startsWith("got it "))
            try {
                id = Integer.valueOf(message.substring(message.indexOf("it") + 3));
            } catch (NumberFormatException e) {
                handleClaim(name, message);

                if(twdchat == true) {
                    m_botAction.sendChatMessage("A TWD-Op has taken call " + id + " Name: " + name);
                }


                return;
            }
        else
            try {
                id = Integer.valueOf(message.substring(message.indexOf(" ") + 1));
            } catch (NumberFormatException e) {
                return;
            }

        Call help = callList.get(id);

        if (help == null) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " not found.");
            return;
        } else if (help.isTaken())
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed by " + help.getTaker() + ".");
        else if (help instanceof NewbCall && help.isExpired(now, NEWB_EXPIRATION_TIME))
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has expired.");
        else if (!(help instanceof NewbCall) && help.isExpired(now, CALL_EXPIRATION_TIME))
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has expired.");
        else {
            if (!message.startsWith("got") && (help instanceof NewbCall))
                handleThat(name, help.getPlayername());
            else {
                help.claim(name);

                if (help instanceof HelpCall && message.startsWith("got")) {
                    if (help.getCallType() == 2)
                        help.setCallType(4);
                    else if (help.getCallType() == 0)
                        help.setCallType(1);
                    else
                        help.setCallType(5);
                } else if (message.startsWith("on") && help.getCallType() == -1)
                    help.setCallType(0);

                lastStafferClaimedCall = name;
                m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " claimed.");
                recordHelp(help);
            }
        }
    }

    /**
        For strict on its, requiring the "on it" to be at the start of the message. For strict got its, requiring the "got it" to be at the start of
        the message.

        @param name
                  Name of person claiming a call by saying 'on it' or 'got it'
        @param message
                  Message the chat message
    */
    public void handleClaim(String name, String message) {
        long now = System.currentTimeMillis();
        PlayerInfo info;
        String player;
        player = lastHelpRequestName;
        Iterator<Integer> i = calls.iterator();
        int id = -1;

        while (i.hasNext()) {
            id = i.next();
            Call call = callList.get(id);

            if (call.isExpired(now, CALL_EXPIRATION_TIME))
                i.remove();
            else if (!call.isTaken()) {
                if (message.startsWith("on")) {
                    message = "on";

                    if (!(call instanceof NewbCall) && m_botAction.getOperatorList().isBotExact(player) && m_playerList.containsKey(lastHelpRequestName.toLowerCase())) {
                        info = m_playerList.get(lastHelpRequestName.toLowerCase());
                        String lastHelpMessage = null;

                        if (info != null && info.getLastCall() > -1)
                            lastHelpMessage = callList.get(info.getLastCall()).getMessage();


                        if (lastHelpMessage != null && lastHelpMessage.contains("TK Report: ")) {
                            String namep = lastHelpMessage.substring(lastHelpMessage.indexOf("TK Report: ") + 11, lastHelpMessage.indexOf(" is reporting"));
                            m_botAction.sendSmartPrivateMessage(namep, "Hello " + namep + ", we have received your TK Report. Staffer " + name + " will be handling your call. Please use :" + name
                                                                + ": to further contact this staffer.");
                        } else
                            m_botAction.sendSmartPrivateMessage(player, "Hello " + player + ", we have received your ?help/?cheater call. Staffer " + name
                                                                + " will be handling your call. Please use :" + name + ": to further contact this staffer.");
                    }
                } else if (message.startsWith("got"))
                    message = "got";

                if (!(call instanceof NewbCall) && !opList.isBotExact(player))
                    m_botAction.sendSmartPrivateMessage(player, "Hello " + player + ", we have received your ?help/?cheater call. Staffer " + name + " will be handling your call. Please use :" + name
                                                        + ": to further contact this staffer.");

                handleClaims(name, message + " #" + id);
                i.remove();
                return;
            } else
                i.remove();
        }

        // A staffer did "on it" while there was no call to take (or all calls were expired).
        if (this.lastStafferClaimedCall != null && this.lastStafferClaimedCall.length() > 0)
            m_botAction.sendRemotePrivateMessage(name, "The call expired or was already taken. The last person to claim a call was " + this.lastStafferClaimedCall + ".");
        else
            m_botAction.sendRemotePrivateMessage(name, "The call expired or no call was found to match your claim.");
    }

    public void handleThat(String name, String player) {
        boolean record = false;
        Date now = new Date();

        if (player != null && !newbHistory.containsKey(player.toLowerCase()))
            return;
        else if (player == null) {
            Iterator<NewbCall> i = newbs.iterator();

            while (i.hasNext()) {
                NewbCall np = i.next();

                if (!record && now.getTime() < np.getTime() + NEWB_EXPIRATION_TIME) {
                    record = true;
                    player = np.getName();
                    i.remove();
                } else if (now.getTime() >= np.getTime() + NEWB_EXPIRATION_TIME)
                    i.remove();
            }
        } else
            record = true;

        if (record) {
            if (newbHistory.containsKey(player.toLowerCase())) {
                NewbCall newb = newbHistory.get(player.toLowerCase());

                if (newb.getClaimType() != NewbCall.FREE) {
                    m_botAction.sendSmartPrivateMessage(name, "The new player alert was already claimed or falsified.");
                    return;
                }

                newb.claim(name);
            }

            if (triggers.remove(player.toLowerCase()))
                m_botAction.sendSmartPrivateMessage(player, "Alert has been claimed by: " + name);

            m_botAction.sendRemotePrivateMessage(name, "Call claim of the new player '" + player + "' recorded.");
            updateStatRecordsONTHAT(name, player, true);
            lastStafferClaimedCall = name;
        } else
            m_botAction.sendRemotePrivateMessage(name, "The call expired or no call was found to match your claim.");
    }

    public void handleNext(String playerName, String message) {
        String[] response;
        HelpCall helpCall;
        PlayerInfo info;

        info = m_playerList.get(playerName.toLowerCase());

        if (info == null || info.getLastCall() == -1)
            m_botAction.sendRemotePrivateMessage(playerName, "If you have a question, ask with ?help <question>");
        else {
            helpCall = (HelpCall) callList.get(info.getLastCall());

            if (helpCall.isValidHelpRequest() == true) {
                response = helpCall.getNextResponse();

                if (response == null) {
                    helpCall.setAllowSummons(true);
                    m_botAction.sendRemotePrivateMessage(playerName, "I have no further information.  If you still need help, message me with !summon to get live help.");
                } else {
                    m_botAction.remotePrivateMessageSpam(playerName, response);

                    if (helpCall.hasMoreResponses() == false)
                        m_botAction.sendRemotePrivateMessage(playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!summon to request live help.");
                    else
                        m_botAction.sendRemotePrivateMessage(playerName, "If this is not helpful, type :" + m_botAction.getBotName() + ":!next to retrieve the next response.");
                }
            } else
                m_botAction.sendRemotePrivateMessage(playerName, "I haven't given you any information yet.  Use ?help <question> if you need help.");
        }
    }

    public void handleSummon(String playerName, String message) {
        HelpCall helpCall;
        PlayerInfo info;

        info = m_playerList.get(playerName.toLowerCase());

        if (info == null || info.getLastCall() == -1)
            m_botAction.sendRemotePrivateMessage(playerName, "If you have a question, ask with ?help <question>");
        else {
            helpCall = (HelpCall) callList.get(info.getLastCall());

            if (helpCall.isValidHelpRequest() == true) {
                if (helpCall.getAllowSummons() == false) {
                    if (helpCall.hasMoreResponses() == true)
                        m_botAction.sendRemotePrivateMessage(playerName, "I have more information.  Message me with !next to see it.");
                    else
                        m_botAction.sendRemotePrivateMessage(playerName, "I feel that you were given suitable information already.");
                } else {
                    m_botAction.sendRemotePrivateMessage(playerName, "A staff member may or " + "may not be available.  If you do not get a response within 5 "
                                                         + "minutes, feel free to use ?help again.");
                    m_botAction.sendUnfilteredPublicMessage("?help The player \"" + playerName + "\" did not get a satisfactory response.  Please respond " + "to this player.");
                    helpCall.setAllowSummons(false);
                }
            } else
                m_botAction.sendRemotePrivateMessage(playerName, "I haven't given you any information yet.  Use ?help <question> if you need help.");
        }
    }

    public void unbanTell(String playerName, String message) {

        loadBanned();
        BotSettings m_botSettings = m_botAction.getBotSettings();
        String ops = m_botSettings.getString("Bad People");

        int spot = ops.indexOf(message);

        if (spot == 0 && ops.length() == message.length()) {
            ops = "";
            m_botAction.sendSmartPrivateMessage(playerName, "Removed: " + message + " successful");
        } else if (spot == 0 && ops.length() > message.length()) {
            ops = ops.substring(message.length() + 1);
            m_botAction.sendSmartPrivateMessage(playerName, "Removed: " + message + " successful");
        } else if (spot > 0 && spot + message.length() < ops.length()) {
            ops = ops.substring(0, spot) + ops.substring(spot + message.length() + 1);
            m_botAction.sendSmartPrivateMessage(playerName, "Removed: " + message + " successful");
        } else if (spot > 0 && spot == ops.length() - message.length()) {
            ops = ops.substring(0, spot - 1);
            m_botAction.sendSmartPrivateMessage(playerName, "Removed: " + message + " successful");
        } else
            m_botAction.sendSmartPrivateMessage(playerName, "Removed: " + message + " successful");

        m_botSettings.put("Bad People", ops);
        m_botSettings.save();
        loadBanned();
    }

    public void handleAddBan(String playerName, String message) {
        //SMod+ only command.
        if (!opList.isSmod(playerName))
            return;

        BotSettings m_botSettings = m_botAction.getBotSettings();
        String ops = m_botSettings.getString("Bad People");

        if (ops.contains(message)) {
            m_botAction.sendSmartPrivateMessage(playerName, message + " is already on my bad list!");
            return;
        }

        if (ops.length() < 1)
            m_botSettings.put("Bad People", message);
        else
            m_botSettings.put("Bad People", ops + "," + message);

        m_botAction.sendSmartPrivateMessage(playerName, "Added " + message + " to my bad list!");
        m_botSettings.save();
        loadBanned();
    }

    public void handleListTellBanned(String playerName, String message) {
        loadBanned();
        String hops = "People banned from using !tell: ";
        Iterator<String> it1 = banned.values().iterator();

        while (it1.hasNext())
            if (it1.hasNext())
                hops += it1.next() + ", ";
            else
                hops += it1.next();

        m_botAction.sendSmartPrivateMessage(playerName, hops);
    }

    public void handleLast(String playerName, String message) {
        if (!opList.isBot(playerName))
            return;

        String[] responses;
        PlayerInfo info;

        String lastName = lastHelpRequestName;

        if (Tools.isAllDigits(message)) {
            try {
                int id = Integer.valueOf(message);

                if (callList.containsKey(id))
                    lastName = callList.get(id).playerName;
                else throw new NumberFormatException();
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(playerName, "Invalid call number.");
                return;
            }
        } else if (message.length() > 1)
            lastName = message;

        if (lastName == null)
            m_botAction.sendRemotePrivateMessage(playerName, "No one has done a help call yet!");

        info = m_playerList.get(lastName.toLowerCase());

        if (info == null || info.getLastCall() == -1)
            m_botAction.sendRemotePrivateMessage(playerName, "No response was given.");
        else {
            try {
                HelpCall helpCall = (HelpCall) callList.get(info.getLastCall());
                responses = helpCall.getAllResponses();
            } catch (ClassCastException e) {
                m_botAction.sendRemotePrivateMessage(playerName, "That was not a help call, so no response was given.");
                return;
            }

            if (responses == null)
                m_botAction.sendRemotePrivateMessage(playerName, "No response was given.");
            else {
                m_botAction.sendRemotePrivateMessage(playerName, "The responses available to " + lastName + " are:");
                displayResponses(playerName, responses);
            }
        }
    }

    public void handleTell(String messager, String message) {
        if (!banned.containsKey(messager.toLowerCase())) {

            String name;
            String keyword;
            int seperator;
            PlayerInfo info;

            seperator = message.indexOf(':');

            if (seperator == -1) {
                name = lastHelpRequestName;
                keyword = message;

                // If the ?help caller is a bot, try to extract the name from its message
                // cheater: (PubBot6) (Public 4): TK Report: 51xg35qas8ghc is reporting BadBoyTKer for intentional TK. (150 total TKs)
                // (Not racism calls, you don't want to do !tell onit on the one who is doing racist words)
                if (m_botAction.getOperatorList().isBotExact(name) && m_playerList.containsKey(lastHelpRequestName.toLowerCase())) {
                    info = m_playerList.get(lastHelpRequestName.toLowerCase());
                    String lastHelpMessage = null;

                    if (info != null && info.getLastCall() > -1)
                        lastHelpMessage = callList.get(info.getLastCall()).getMessage();

                    if (lastHelpMessage != null && lastHelpMessage.contains("TK Report: "))
                        name = lastHelpMessage.substring(lastHelpMessage.indexOf("TK Report: ") + 11, lastHelpMessage.indexOf(" is reporting"));
                }

            } else {
                name = message.substring(0, seperator).trim();
                keyword = message.substring(seperator + 1).trim();
            }

            if (name != null)
                if (messager.equalsIgnoreCase(name) || messager.toLowerCase().startsWith(name.toLowerCase()))
                    m_botAction.sendChatMessage("Use :" + m_botAction.getBotName() + ":!lookup <keyword> instead.");
                else if (name.startsWith("#"))
                    m_botAction.sendChatMessage("Invalid name. Please specify a different name.");
                else if (keyword.toLowerCase().startsWith("dictionary ")) {
                    String query;

                    query = keyword.substring(10).trim();

                    if (query.length() == 0)
                        m_botAction.sendChatMessage("Specify a word to reference.");
                    else {
                        m_botAction.sendRemotePrivateMessage(name, "Definition of " + query + ":  http://dictionary.reference.com/search?q=" + query);
                        m_botAction.sendChatMessage("Gave " + name + " the definition of " + query + " at http://dictionary.reference.com/search?q=" + query);
                    }
                } else if (keyword.toLowerCase().startsWith("thesaurus ")) {
                    String query;

                    query = keyword.substring(9).trim();

                    if (query.length() == 0)
                        m_botAction.sendChatMessage("Specify a word to reference.");
                    else {
                        m_botAction.sendRemotePrivateMessage(name, "Thesaurus entry for " + query + ":  http://thesaurus.reference.com/search?q=" + query);
                        m_botAction.sendChatMessage("Gave " + name + " the thesaurus entry for " + query + " at http://thesaurus.reference.com/search?q=" + query);
                    }
                } else if (keyword.toLowerCase().startsWith("javadocs ")) {
                    String query;

                    query = keyword.substring(8).trim();

                    if (query.length() == 0)
                        m_botAction.sendChatMessage("Specify something to look up the JavaDocs on.");
                    else {
                        m_botAction.sendRemotePrivateMessage(name, "Javadocs for " + query + ":  http://javadocs.org/" + query);
                        m_botAction.sendChatMessage("Gave " + name + " the Javadocs entry for " + query + " at http://javadocs.org/" + query);
                    }
                } else {
                    String[] responses = search.search(keyword);

                    if (responses.length == 0)
                        m_botAction.sendChatMessage("Sorry, no matches.");
                    else if (responses.length > 3)
                        m_botAction.sendChatMessage("Too many matches.  Please be more specific.");
                    else {
                        m_botAction.sendSmartPrivateMessage(name, "This message has been sent by " + messager + ":");
                        displayResponses(name, responses);
                        m_botAction.sendSmartPrivateMessage(name, "If you have any other questions regarding this issue, please use :" + messager + ":<Message>.");
                        m_botAction.sendChatMessage("Told " + name + " about " + keyword + ".");
                    }
                }
        }
    }

    public void handleLookup(String name, String message) {
        String[] resp = search.search(message);

        if (resp.length == 0)
            m_botAction.sendRemotePrivateMessage(name, "Sorry, no matches.");
        else
            displayResponses(name, resp);
    }

    public void handleHourlyStats(String name, String message)
    {
        ArrayList<String> spam = new ArrayList<String>();

        spam.add("Hourly Call Stats for Current Month");
        spam.add("------------------------------------------------------------");
        spam.add("Hour      Calls Taken     Total Calls       Percentage Taken");
        spam.add("------------------------------------------------------------");

        try {
            ResultSet rs = m_botAction.SQLQuery(mySQLHost, "SELECT DATE_FORMAT(fdCreated, '%h %p') AS time, COUNT(CASE WHEN fnTaken > 0 THEN 1 END) AS taken, "
                                                + "COUNT(fnTaken) AS total FROM `tblCallHelp` WHERE MONTH(fdCreated) = MONTH(NOW()) AND YEAR(fdCreated) = YEAR(NOW()) GROUP BY "
                                                + "DATE_FORMAT(fdCreated, '%h %p') ORDER BY DATE_FORMAT(fdCreated, '%H') ASC");

            while(rs.next()) {
                String time = rs.getString("time"); //hour
                int takenCalls = rs.getInt("taken");
                int totalCalls = rs.getInt("total");
                double percent = 100.0 * takenCalls / totalCalls;
                String percentage;

                if(totalCalls == 0) {
                    percentage = "-";
                } else {
                    percentage = Integer.toString((int)percent) + "%";
                }


                spam.add(Tools.formatString(time, 10) + Tools.formatString(Integer.toString(takenCalls), 16) + Tools.formatString(Integer.toString(totalCalls), 18) + percentage);
            }

            m_botAction.SQLClose(rs);

        } catch(SQLException e) {
            spam.add("Error processing request");
        }

        m_botAction.remotePrivateMessageSpam(name, spam.toArray(new String[spam.size()]));
    }

    public void handleReload(String name, String message) {
        if (opList.isSysop(name)) {
            populateSearch();
            m_botAction.sendRemotePrivateMessage(name, "Reloaded.");
        }
    }

    public void handleDie(String name, String message) {
        m_botAction.cancelTasks();
        m_botAction.die("RoboHelp disconnected by " + name);
    }

    public void handleWarn(String playerName, String message) {
        String name;
        PlayerInfo info;

        if (message == null)
            name = lastHelpRequestName;
        else if (message.trim().length() == 0)
            name = lastHelpRequestName;
        else
            name = message.trim();

        if (name == null)
            m_botAction.sendChatMessage("There hasn't been a help call yet.");
        else if (name.length() == 0)
            m_botAction.sendChatMessage("There hasn't been a help call yet.");
        else {
            info = m_playerList.get(name.toLowerCase());

            if (info == null || info.getLastCall() == -1)
                m_botAction.sendChatMessage(name + " hasn't done a help call yet.");
            else if (info.getBeenWarned() == true)
                m_botAction.sendChatMessage(name + " has already been warned, no warning given.");
            else {
                info.setBeenWarned(true);

                if (info.AdvertTell() == true) {
                    m_botAction.sendRemotePrivateMessage(name, "WARNING: Do NOT use the ?advert " + "command.  It is for Staff Members only, and is punishable by a ban. Further abuse "
                                                         + "will not be tolerated!", 1);
                    m_botAction.sendChatMessage(name + " has been warned for ?advert abuse.");

                    Calendar thisTime = Calendar.getInstance();
                    java.util.Date day = thisTime.getTime();
                    String warntime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(day);
                    String[] paramNames = { "name", "warning", "staffmember", "timeofwarning" };
                    String date = new java.sql.Date(System.currentTimeMillis()).toString();
                    String[] data = { name.toLowerCase().trim(), new String(warntime + ": Warning to " + name + " from Robohelp for advert abuse.  !warn ordered by " + playerName),
                                      playerName.toLowerCase().trim(), date
                                    };

                    m_botAction.SQLInsertInto(mySQLHost, "tblWarnings", paramNames, data);

                } else {

                    m_botAction.sendRemotePrivateMessage(name, "WARNING: We appreciate " + "your input.  However, your excessive abuse of the ?cheater or ?help command will "
                                                         + "not be tolerated further! Further abuse will result in a ban from the zone.", 1);
                    m_botAction.sendChatMessage(name + " has been warned.");

                    Calendar thisTime = Calendar.getInstance();
                    java.util.Date day = thisTime.getTime();
                    String warntime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(day);
                    String[] paramNames = { "name", "warning", "staffmember", "timeofwarning" };
                    String date = new java.sql.Date(System.currentTimeMillis()).toString();
                    String[] data = { name.toLowerCase().trim(), new String(warntime + ": Warning to " + name + " from Robohelp for help/cheater abuse.  !warn ordered by " + playerName),
                                      playerName.toLowerCase().trim(), date
                                    };

                    m_botAction.SQLInsertInto(mySQLHost, "tblWarnings", paramNames, data);
                }
            }
        }
    }

    public void handleBan(String playerName, String message) {
        String name;
        PlayerInfo info;

        if (message == null)
            name = lastHelpRequestName;
        else if (message.trim().length() == 0)
            name = lastHelpRequestName;
        else
            name = message.trim();

        if (name == null)
            m_botAction.sendChatMessage("There hasn't been a help call yet.");
        else if (name.length() == 0)
            m_botAction.sendChatMessage("There hasn't been a help call yet.");
        else if (!opList.isER(playerName))
            m_botAction.sendChatMessage("Only ER's and above are authorized to ban.");
        else if (opList.isSmod(playerName) && (opList.isBot(name) && !opList.isSysop(name))) {
            m_lastBanner = playerName;
            m_banPending = true;
            m_botAction.sendRemotePrivateMessage(name, "You have been banned for abuse as staff member. Depending on the Dean of Staff's decisions further action will be taken!");
            m_botAction.sendUnfilteredPublicMessage("?removeop " + name);
            m_botAction.sendUnfilteredPublicMessage("?ban -a3 -e30 " + name);
            m_botAction.sendChatMessage("Staffer \"" + name + "\" has been banned for abuse.");
            m_playerList.remove(name);
        } else if (opList.isBot(name))
            m_botAction.sendChatMessage("Are you nuts?  You can't ban a staff member!");
        else {
            info = m_playerList.get(name.toLowerCase());

            if (info == null || info.getLastCall() == -1)
                m_botAction.sendChatMessage(name + " hasn't done a help call yet.");
            else if (info.getBeenWarned() == false)
                m_botAction.sendChatMessage(name + " hasn't been warned yet.");
            else {
                m_lastBanner = playerName;
                m_banPending = true;
                m_botAction.sendRemotePrivateMessage(name, "You have been silence for " + "abuse of the alert commands.  I am sorry this had to happen.  Your ban "
                                                     + "will likely expire in 2 hours.  Goodbye!");
                //m_botAction.sendUnfilteredPublicMessage("?ban -e1 " + name);
                m_botAction.sendPrivateMessage("StaffBot", "!silence " + name + ":120:Silenced for abusing help by " + playerName);
                m_botAction.sendChatMessage("Player \"" + name + "\" has been " + "silenced.");
                m_playerList.remove(name);
            }
        }
    }

    public void handleRepeat(String playerName, String message) {
        String name;
        HelpCall helpCall;
        PlayerInfo info;

        if (message == null)
            name = lastHelpRequestName;
        else if (message.trim().length() == 0)
            name = lastHelpRequestName;
        else
            name = message.trim();

        if (name == null)
            m_botAction.sendChatMessage("There hasn't been a help call yet.");
        else if (name.length() == 0)
            m_botAction.sendChatMessage("There hasn't been a help call yet.");
        else {
            info = m_playerList.get(name.toLowerCase());

            if (info == null || info.getLastCall() == -1)
                m_botAction.sendChatMessage(name + " hasn't done a help call yet.");
            else {
                helpCall = (HelpCall) callList.get(info.getLastCall());

                if (helpCall.getFirstResponse() != null) {
                    helpCall.setAllowSummons(false);
                    m_botAction.sendRemotePrivateMessage(name, "I believe that the previous " + "response you received was a sufficient answer to your question.");
                    m_botAction.remotePrivateMessageSpam(name, helpCall.getFirstResponse());
                    m_botAction.sendChatMessage("The last response has been repeated to " + name);
                } else
                    m_botAction.sendChatMessage("Error repeating response to '" + name + "': response not found.  Please address this call manually.");
            }
        }
    }

    public void handleHave(String name, String msg) {
        String player = "";

        if (msg.length() < 6) {
            if (lastNewPlayerName.isEmpty()) {
                m_botAction.sendSmartPrivateMessage(name, "No recent alerts found. You'll have to be specific.");
                return;
            }

            player = lastNewPlayerName;
        } else if (msg.contains(" ") && msg.length() > 6)
            player = msg.substring(msg.indexOf(" ") + 1).trim();

        if (player.length() > 1) {
            if (newbHistory.containsKey(player.toLowerCase())) {
                NewbCall newb = newbHistory.get(player.toLowerCase());

                if (newb.claimType != NewbCall.FREE) {
                    m_botAction.sendSmartPrivateMessage(name, "That new player alert has already been claimed as " + newb.claimer);
                    return;
                }

                newb.claim(name);
            }

            if (triggers.remove(player.toLowerCase()))
                m_botAction.sendSmartPrivateMessage(player, "Alert has been have'd by: " + name);

            updateStatRecordsONTHAT(name, player, false);
            m_botAction.sendSmartPrivateMessage(name, "New Player alert for '" + player + "' has been claimed for you but not counted.");
        }
    }

    public void handleClean(String name, String message) {
        int id = -1;

        if (!message.contains(",") && !message.contains("-") && message.contains("#"))
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be cleaned.");
                return;
            }
        else if (message.startsWith("clean ") && message.contains(",") && !message.contains("-")) {
            String msg = message.substring(message.indexOf(" ") + 1);
            String[] numbas = msg.split(",");

            if (numbas.length > 10) {
                m_botAction.sendSmartPrivateMessage(name, "Call modification limit per command is 10");
                return;
            }

            Integer num;

            for (int i = 0; i < numbas.length; i++)
                try {
                    num = (Integer.valueOf(numbas[i].trim().replaceAll("#", "")));
                    handleClean(name, "clean #" + num);
                } catch (NumberFormatException e) {
                    m_botAction.sendSmartPrivateMessage(name, "Could not find or unable to convert: " + numbas[i]);
                }

            return;
        } else if (message.startsWith("clean ") && !message.contains(",") && message.contains("-")) {
            String msg = message.substring(message.indexOf(" ") + 1);
            msg = msg.replaceAll("#", "");
            String[] numbas = msg.split("-");

            if (numbas.length != 2)
                return;

            int hi, lo;

            try {
                lo = Integer.valueOf(numbas[0]);
                hi = Integer.valueOf(numbas[1]);
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Could not convert: " + msg);
                return;
            }

            if (hi >= lo && hi - lo < 11)
                for (; lo <= hi; lo++)
                    handleClean(name, "clean #" + lo);
            else if (hi < lo)
                m_botAction.sendSmartPrivateMessage(name, "Incorrect syntax (low-high): " + msg);
            else
                m_botAction.sendSmartPrivateMessage(name, "Call modification limit per command is 10");

            return;
        } else if (message.length() > 5)
            try {
                id = Integer.valueOf(message.substring(5).trim());
            } catch (NumberFormatException e) {
                return;
            }

        if (id > -1 && !callList.containsKey(id)) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " could not be found.");
            return;
        }

        if (id == -1) {
            if (lastHelpRequestName == null) {
                m_botAction.sendSmartPrivateMessage(name, "The last call could not be found.");
                return;
            }

            String player = lastHelpRequestName;
            PlayerInfo info = m_playerList.get(player.toLowerCase());

            if (info == null || info.getLastCall() == -1) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be cleaned.");
                return;
            }

            id = info.getLastCall();
        }

        Call last = callList.get(id);

        if (!(last instanceof CheaterCall)) {
            m_botAction.sendSmartPrivateMessage(name, "Only cheater calls can be cleaned.");
            return;
        }

        if (!last.isTaken()) {
            ((CheaterCall) last).clean();
            calls.removeElement(last.getID());
            m_botAction.sendSmartPrivateMessage(name, "Call #" + last.getID() + " cleaned.");
            m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fnTaken = 3, fcTakerName = 'clean' WHERE fnCallID = " + last.getCallID());
        } else
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed.");
    }

    public void handleForget(String name, String message) {
        int id = -1;

        if (!message.contains(",") && !message.contains("-") && message.contains("#"))
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be forgotten.");
                return;
            }
        else if (message.startsWith("forget ") && message.contains(",") && !message.contains("-")) {
            String msg = message.substring(message.indexOf(" ") + 1);
            String[] numbas = msg.split(",");
            Integer num;

            if (numbas.length > 10) {
                m_botAction.sendSmartPrivateMessage(name, "Call modification limit per command is 10");
                return;
            }

            for (int i = 0; i < numbas.length; i++)
                try {
                    num = (Integer.valueOf(numbas[i].trim().replaceAll("#", "")));
                    handleForget(name, "forget #" + num);
                } catch (NumberFormatException e) {
                    m_botAction.sendSmartPrivateMessage(name, "Could not find or unable to convert: " + numbas[i]);
                }

            return;
        } else if (message.startsWith("forget ") && !message.contains(",") && message.contains("-")) {
            String msg = message.substring(message.indexOf(" ") + 1);
            msg = msg.replaceAll("#", "");
            String[] numbas = msg.split("-");

            if (numbas.length != 2)
                return;

            int hi, lo;

            try {
                lo = Integer.valueOf(numbas[0]);
                hi = Integer.valueOf(numbas[1]);
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Could not convert: " + msg);
                return;
            }

            if (hi >= lo && hi - lo < 11)
                for (; lo <= hi; lo++)
                    handleForget(name, "forget #" + lo);
            else if (hi < lo)
                m_botAction.sendSmartPrivateMessage(name, "Incorrect syntax (low-high): " + msg);
            else
                m_botAction.sendSmartPrivateMessage(name, "Call modification limit per command is 10");

            return;
        } else if (message.length() > 6)
            try {
                id = Integer.valueOf(message.substring(6).trim());
            } catch (NumberFormatException e) {
                return;
            }

        if (!name.equals(m_botAction.getBotName()) && opList.isBotExact(name))
            return;

        if (id > -1 && !callList.containsKey(id)) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " could not be found.");
            return;
        }

        if (id == -1) {
            if (lastHelpRequestName == null) {
                m_botAction.sendSmartPrivateMessage(name, "The last call could not be found.");
                return;
            }

            String player = lastHelpRequestName;
            PlayerInfo info = m_playerList.get(player.toLowerCase());

            if (info == null || info.getLastCall() == -1) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be forgotten.");
                return;
            }

            id = info.getLastCall();
        }

        Call last = callList.get(id);

        if (last instanceof HelpCall || last instanceof CheaterCall) {
            if (!last.isTaken()) {
                if (last instanceof HelpCall)
                    ((HelpCall) last).forget();
                else if (last instanceof CheaterCall)
                    ((CheaterCall) last).forget();

                calls.removeElement(last.getID());
                m_botAction.sendSmartPrivateMessage(name, "Call #" + last.getID() + " forgotten.");
                m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fnTaken = 3, fcTakerName = 'forgot' WHERE fnCallID = " + last.getCallID());
            } else
                m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed.");
        }
    }

    public void handleMine(String name, String message) {
        int id = -1;

        if (!message.contains(",") && !message.contains("-") && message.contains("#"))
            try {
                id = Integer.valueOf(message.substring(message.indexOf("#") + 1));
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be claimed.");
                return;
            }
        else if (message.startsWith("mine ") && message.contains(",") && !message.contains("-")) {
            String msg = message.substring(message.indexOf(" ") + 1);
            String[] numbas = msg.split(",");

            if (numbas.length > 10) {
                m_botAction.sendSmartPrivateMessage(name, "Call modification limit per command is 10");
                return;
            }

            Integer num;

            for (int i = 0; i < numbas.length; i++)
                try {
                    num = (Integer.valueOf(numbas[i].trim().replaceAll("#", "")));
                    handleMine(name, "mine #" + num);
                } catch (NumberFormatException e) {
                    m_botAction.sendSmartPrivateMessage(name, "Could not find or unable to convert: " + numbas[i]);
                }

            return;
        } else if (message.startsWith("mine ") && !message.contains(",") && message.contains("-")) {
            String msg = message.substring(message.indexOf(" ") + 1);
            msg = msg.replaceAll("#", "");
            String[] numbas = msg.split("-");

            if (numbas.length != 2)
                return;

            int hi, lo;

            try {
                lo = Integer.valueOf(numbas[0]);
                hi = Integer.valueOf(numbas[1]);
            } catch (NumberFormatException e) {
                m_botAction.sendSmartPrivateMessage(name, "Could not convert: " + msg);
                return;
            }

            if (hi >= lo && hi - lo < 11)
                for (; lo <= hi; lo++)
                    handleMine(name, "mine #" + lo);
            else if (hi < lo)
                m_botAction.sendSmartPrivateMessage(name, "Incorrect syntax (low-high): " + msg);
            else
                m_botAction.sendSmartPrivateMessage(name, "Call modification limit per command is 10");

            return;
        } else if (message.length() > 4)
            try {
                id = Integer.valueOf(message.substring(4).trim());
            } catch (NumberFormatException e) {
                return;
            }

        if (id > -1 && !callList.containsKey(id)) {
            m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " could not be found.");
            return;
        }

        if (id == -1) {
            if (lastHelpRequestName == null) {
                m_botAction.sendSmartPrivateMessage(name, "The last call could not be found.");
                return;
            }

            String player = lastHelpRequestName;
            PlayerInfo info = m_playerList.get(player.toLowerCase());

            if (info == null || info.getLastCall() == -1) {
                m_botAction.sendSmartPrivateMessage(name, "Call could not be claimed.");
                return;
            }

            id = info.getLastCall();
        }

        Call last = callList.get(id);

        if (last instanceof HelpCall || last instanceof CheaterCall) {
            if (!last.isTaken()) {
                if (last instanceof HelpCall)
                    ((HelpCall) last).mine(name);

                if (last instanceof CheaterCall)
                    ((CheaterCall) last).mine(name);

                calls.removeElement(last.getID());
                m_botAction.sendSmartPrivateMessage(name, "Call #" + last.getID() + " claimed for you but not counted.");
                m_botAction.SQLBackgroundQuery(mySQLHost, "robohelp", "UPDATE tblCallHelp SET fnTaken = 2, fcTakerName = '" + Tools.addSlashesToString(name) + "' WHERE fnCallID = " + last.getCallID());
            } else
                m_botAction.sendSmartPrivateMessage(name, "Call #" + id + " has already been claimed.");
        }
    }

    public void handleFalseNewb(String name, String msg) {
        String player = "";

        if (msg.trim().equalsIgnoreCase("!false")) {
            if (lastNewPlayerName.length() > 0)
                player = lastNewPlayerName;
            else {
                m_botAction.sendSmartPrivateMessage(name, "No new player alerts found. You'll have to be more specific.");
                return;
            }
        } else if (msg.contains(" ") && msg.length() > 7)
            player = msg.substring(msg.indexOf(" ") + 1);

        if (player.isEmpty())
            return;

        if (newbHistory.containsKey(player.toLowerCase())) {
            NewbCall newb = newbHistory.get(player.toLowerCase());

            if (newb.claimType == NewbCall.TAKEN && !name.equalsIgnoreCase(newb.claimer)) {
                m_botAction.sendSmartPrivateMessage(name, "The new player alert has already been claimed and only the claimer may falsify it.");
                return;
            }

            newb.falsePos(name);

            if (triggers.remove(player.toLowerCase()))
                m_botAction.sendSmartPrivateMessage(player, "Alert has been false'd by: " + name);
        }

        if (!m_botAction.SQLisOperational()) {
            m_botAction.sendSmartPrivateMessage(name, "Database offline.");
            return;
        }

        m_botAction.SQLBackgroundQuery(mySQLHost, null, "UPDATE tblCallNewb SET fnTaken = 3 WHERE fcUserName = '" + Tools.addSlashesToString(player) + "' ORDER BY fnAlertID DESC");
        m_botAction.sendSmartPrivateMessage(name, "All database entries for '" + player + "' have been falsified.");
        m_botAction.ipcSendMessage(ZONE_CHANNEL, "false: " + player, WBOT, m_botAction.getBotName());
    }

    public void handleUndoFalse(String name, String msg) {
        String player = "";

        if (msg.trim().equalsIgnoreCase("!undo")) {
            if (lastNewPlayerName.length() > 0)
                player = lastNewPlayerName;
            else {
                m_botAction.sendSmartPrivateMessage(name, "No new player alerts found. You'll have to be more specific.");
                return;
            }
        } else if (msg.contains(" ") && msg.length() > 7)
            player = msg.substring(msg.indexOf(" ") + 1);

        if (player.isEmpty())
            return;

        if (newbHistory.containsKey(player.toLowerCase())) {
            NewbCall newb = newbHistory.get(player.toLowerCase());

            if (newb.claimType != NewbCall.FALSE) {
                m_botAction.sendSmartPrivateMessage(name, "Only false positive alerts may be undone!");
                return;
            }

            newb.undoFalse(name);

            if (triggers.remove(player.toLowerCase()))
                m_botAction.sendSmartPrivateMessage(player, "Alert has been undone by: " + name);
        }

        if (!m_botAction.SQLisOperational()) {
            m_botAction.sendSmartPrivateMessage(name, "Database offline.");
            return;
        }

        m_botAction.SQLBackgroundQuery(mySQLHost, null, "UPDATE tblCallNewb SET fnTaken = 2 WHERE fcUserName = '" + Tools.addSlashesToString(player) + "' ORDER BY fnAlertID DESC");
        m_botAction.sendSmartPrivateMessage(name, "All database entries for '" + player + "' have been un-falsified.");
        m_botAction.ipcSendMessage(ZONE_CHANNEL, "undo: " + player, WBOT, m_botAction.getBotName());
    }

    public void handleNewbs(String name, String msg) {
        DateFormat t = new SimpleDateFormat("HH:mm");
        int num = 5;

        if (msg.length() > 0)
            try {
                num = Integer.valueOf(msg.substring(msg.indexOf(" ") + 1).trim());
            } catch (NumberFormatException e) {
                num = 5;
            }

        int size = newbNames.size();

        if (size > 0 && num > 0) {
            if (size < num)
                num = size;

            m_botAction.sendSmartPrivateMessage(name, "Last " + num + " new player alerts:");

            for (int i = 0; i < num; i++) {
                NewbCall newb = newbHistory.get(newbNames.get(i).toLowerCase());

                //TODO Search for the real cause of the NPE. Assuming that the two lists above aren't always synced up.
                if(newb == null)
                    continue;

                String m = "" + t.format(newb.getTime()) + " ";
                m += stringHelper("" + newb.playerName, 23);

                if (newb.claimType == NewbCall.FREE)
                    m += "[MISSED]";
                else if (newb.claimType == NewbCall.TAKEN)
                    m += "(" + newb.claimer + ")";
                else if (newb.claimType == NewbCall.FALSE)
                    m += "{FALSE - " + newb.claimer + "}";
                else
                    m += newb.claimer;

                m_botAction.sendSmartPrivateMessage(name, m);
            }
        } else
            m_botAction.sendSmartPrivateMessage(name, "No alerts found.");
    }

    public void handle8Ball(String name, String msg) {
        if (msg.endsWith("?"))
            m_botAction.sendChatMessage(mag8[random.nextInt(mag8.length)]);
        else
            m_botAction.sendChatMessage("Please rephrase in the form of a question.");
    }

    private String stringHelper(String str, int length) {
        if (length == -1)

            // -1 for defaults dynamic
            if (str.length() < 11)
                length = 10;
            else if (str.length() < 16)
                length = 15;
            else if (str.length() < 21)
                length = 20;
            else
                length = 23;

        for (int i = str.length(); i < length; i++)
            str += " ";

        str = str.substring(0, length);
        return str;
    }

    private String getTimeString(Call call) {
        long time = 0;

        if (timeFormat) {
            DateFormat f = new SimpleDateFormat("HH:mm");
            return f.format(time);
        } else {
            if (call.isTaken())
                time = call.getClaim() - call.getTime();
            else
                time = System.currentTimeMillis() - call.getTime();

            time /= 1000;
            int hour = ((int) (time / 60 / 60) % 60);
            time -= (hour * 60 * 60);
            int min = ((int) time / 60) % 60;
            time -= (min * 60);
            String t = "";

            if (hour > 0)
                t += hour + "h";

            t += min + "m";
            t += time + "s";
            return t;
        }
    }

    public void changeTimeFormatC(String name, String msg) {
        timeFormat = !timeFormat;

        if (timeFormat)
            m_botAction.sendChatMessage("Call list time format changed to birthdate. (HH:mm)");
        else
            m_botAction.sendChatMessage("Call list time format changed to time passed. (#h#m#s)");
    }

    public void changeTimeFormatP(String name, String msg) {
        if (timeFormat)
            m_botAction.sendSmartPrivateMessage(name, "Call list time format changed to birthdate. (HH:mm)");
        else
            m_botAction.sendSmartPrivateMessage(name, "Call list time format changed to time passed. (#h#m#s)");
    }

    public void handleCalls(String name, String message) {
        int count;

        try {
            count = Integer.valueOf(message);
        } catch (NumberFormatException e) {
            count = 5;
        }

        if (callList.size() < count)
            count = callList.size();

        if (count > 60)
            count = 60;

        if (count > 0) {
            if (timeFormat)
                m_botAction.sendSmartPrivateMessage(name, "Last " + count + " calls:");
            else
                m_botAction.sendSmartPrivateMessage(name, "Last " + count + " calls:");

            int id = callList.lastKey();

            do {
                Call call = callList.get(id);
                String msg = "#" + call.getID() + " " + getTimeString(call) + " ";

                if (call instanceof NewbCall) {
                    if (call.isTaken()) {
                        int type = call.getClaimType();
                        String taker = call.getTaker();

                        if (type != NewbCall.FALSE)
                            msg += "- (" + taker + ") -";
                        else
                            msg += "- " + taker + " -";
                    } else if (call.getClaimType() == NewbCall.FREE)
                        msg += "*";
                } else {
                    if (call.isTaken() || call.getTaker().equals("RoboHelp")) {
                        int ct = call.getClaimType();
                        String taker = call.getTaker();

                        if (ct == HelpCall.TAKEN)
                            msg += "- (" + taker + ") -";
                        else if (ct == HelpCall.MINE || call.getTaker().equals("RoboHelp"))
                            msg += "- [" + taker + "] -";
                        else if (ct == HelpCall.FORGOT)
                            msg += "- " + taker + " -";
                        else if (ct == HelpCall.CLEAN)
                            msg += "- " + taker + " -";
                    } else if (call.getClaimType() == HelpCall.FREE)
                        msg += "*";
                }

                if (call instanceof HelpCall)
                    msg += " help: (";
                else if (call instanceof CheaterCall)
                    msg += " cheater: (";
                else if (call instanceof NewbCall)
                    msg += " newplayer: (" + ((NewbCall) call).getName() + ")";
                else
                    msg += " ERROR: (";

                if (!(call instanceof NewbCall))
                    msg += call.getPlayername() + ") " + call.getMessage();

                m_botAction.sendSmartPrivateMessage(name, msg);
                count--;
                id = id - 1;
            } while (count > 0 && callList.containsKey(id));
        } else
            m_botAction.sendSmartPrivateMessage(name, "No calls found.");
    }

    public void handleStats(String name, String message) {
        DateFormat ym = new SimpleDateFormat("yyyy-MM");
        DateFormat my = new SimpleDateFormat("MM-yyyy");
        Calendar cal1 = Calendar.getInstance();
        String date = ym.format(cal1.getTime());

        if (message.length() == 7 && message.contains("-")) {
            String[] dates = message.split("-");

            if (Tools.isAllDigits(dates[0]) && Tools.isAllDigits(dates[1]))
                try {
                    int month = Integer.valueOf(dates[0]);
                    int year = Integer.valueOf(dates[1]);

                    if (month > 0)
                        month--;
                    else if (month == 0)
                        month = 11;

                    cal1.set(year, month, 01);
                    date = new SimpleDateFormat("yyyy-MM").format(cal1.getTime());
                } catch (NumberFormatException e) {
                    date = new SimpleDateFormat("yyyy-MM").format(Calendar.getInstance().getTime());
                }
        }

        Calendar cal2 = Calendar.getInstance();
        cal2.set(cal1.get(Calendar.YEAR), cal1.get(Calendar.MONTH), 01);
        cal2.add(Calendar.MONTH, 1);
        String date2 = ym.format(cal2.getTime());

        int realCalls = 0; // total staff stats from tblCall where fnType = 0 or 1
        int trueOns = 0; // total on its from tblCall, fnType = 0
        int trueGots = 0; // total got its from tblCall, fnType = 1
        int trueNewbs = 0; // total on thats from tblCall, fnType = 2
        int allNewbs = 0; // total newb alerts from tblCallNewb

        int totalCalls = 0; // total of all calls from tblCallHelp
        int takenCalls = 0; // total of all calls from tblCallHelp where fnTaken = 1, 2 or 3
        int lostCalls = 0; // calls from tblCallHelp with fnTaken = 0
        // int notCalls = 0;
        int help_taken = 0; // total helps from tblCallHelp where fnType = 0 and fnTaken = 1, 2, 3
        int help_lost = 0; // total helps from tblCallHelp where fnType = 0 and fnTaken = 0
        int cheat_taken = 0;// total cheaters from tblCallHelp where fnType = 2 and fnTaken = 1, 2, 3
        int cheat_lost = 0; // total cheaters from tblCallHelp where fnType = 2 and fnTaken = 0
        int gotitCalls = 0; // total got its from tblCallHelp where fnType = 1 and fnTaken = 1, 2, 3
        int mineCalls = 0; // total mine from tblCallHelp where fnTaken = 2
        int otherCalls = 0; // total clean/forget from tblCallHelp where fnTaken = 3
        boolean limit = false;

        try {
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT fnType, fnTaken, COUNT(fnCallID) FROM `tblCallHelp` WHERE fdCreated > '" + date + "-01 00:00:00' AND fdCreated < '" + date2
                                                    + "-01 00:00:00' GROUP BY fnTaken, fnType");

            if (result.next())
                do {
                    int taken = result.getInt("fnTaken");
                    int type = result.getInt("fnType");

                    if (taken == 0) {
                        if (type == 0)
                            help_lost = result.getInt(3);
                        else if (type == 2)
                            cheat_lost = result.getInt(3);
                    } else if (type == 0)
                        help_taken += result.getInt(3);
                    else if (type == 1)
                        gotitCalls += result.getInt(3);
                    else if (type == 2)
                        cheat_taken += result.getInt(3);
                } while (result.next());
            else
                limit = true;

            m_botAction.SQLClose(result);

            result = m_botAction.SQLQuery(mySQLHost, "SELECT fnTaken, fcTakerName, COUNT(fnCallID) FROM tblCallHelp WHERE fdCreated > '" + date + "-01 00:00:00' AND fdCreated < '" + date2
                                          + "-01 00:00:00' AND (fnTaken = 2 OR fnTaken = 3) GROUP BY fnTaken");

            if (result.next())
                do {
                    int taken = result.getInt(1);

                    if (taken == 2)
                        mineCalls = result.getInt(3);
                    else if (taken == 3)
                        otherCalls = result.getInt(3);
                } while (result.next());

            m_botAction.SQLClose(result);

            result = m_botAction.SQLQuery(mySQLHost, "SELECT fnType, SUM(fnCount) FROM `tblCall` WHERE fdDate = '" + date + "-01' GROUP BY fnType");

            if (result.next()) {
                do {
                    int type = result.getInt("fnType");

                    if (type == 0)
                        trueOns = result.getInt(2);
                    else if (type == 1)
                        trueGots = result.getInt(2);
                    else if (type == 2)
                        trueNewbs = result.getInt(2);
                } while (result.next());

                m_botAction.SQLClose(result);
            } else {
                m_botAction.sendSmartPrivateMessage(name, "No call information found " + my.format(cal1.getTime()) + ".");
                m_botAction.SQLClose(result);
                return;
            }

            m_botAction.SQLClose(result);
            result = m_botAction.SQLQuery(mySQLHost, "SELECT COUNT(fnAlertID) FROM tblCallNewb WHERE fdCreated > '" + date + "-01 00:00:00' AND fdCreated < '" + date2 + "-01 00:00:00'");

            if (result.next())
                allNewbs = result.getInt(1);

            m_botAction.SQLClose(result);
            result = m_botAction.SQLQuery(mySQLHost, "SELECT COUNT(fnAlertID) FROM tblCallNewb WHERE fnTaken != 0 AND fdCreated > '" + date + "-01 00:00:00' AND fdCreated < '"
                                          + date2 + "-01 00:00:00'");

            if (result.next())
                trueNewbs = result.getInt(1);

            m_botAction.SQLClose(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        totalCalls = help_lost + help_taken + gotitCalls + cheat_taken + cheat_lost;
        takenCalls = help_taken + gotitCalls + cheat_taken;
        lostCalls = totalCalls - takenCalls;
        realCalls = trueOns + trueGots;
        int written = mineCalls + otherCalls;
        int helps = help_lost + help_taken;
        int cheats = cheat_lost + cheat_taken;
        String[] msg;
        DateFormat yr = new SimpleDateFormat("yyyy");

        if (!limit)
            msg = new String[] { "Call Claim Statistics for " + cal1.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) + ", " + yr.format(cal1.getTime()) + ":",
                                 "Real calls taken: " + realCalls, "" + Math.round((double) takenCalls / totalCalls * 100) + "% calls answered (" + takenCalls + ":" + totalCalls + ")",
                                 " Unattended calls:   " + lostCalls, " Written off calls:  " + written, " Got it call total:  " + gotitCalls,
                                 " New player calls:  " + Math.round((double) trueNewbs / allNewbs * 100) + "% (" + trueNewbs + ":" + allNewbs + ")",
                                 " Help calls:        " + Math.round((double) help_taken / helps * 100) + "% (" + help_taken + ":" + helps + ")",
                                 " Cheater calls:     " + Math.round((double) cheat_taken / cheats * 100) + "% (" + cheat_taken + ":" + cheats + ")"
                               };
        else
            msg = new String[] { "LIMITED Call Claim Statistics for " + cal1.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) + ", " + yr.format(cal1.getTime()) + ":",
                                 " Real calls taken: " + realCalls, " On it calls:      " + trueOns, " Got it calls:     " + trueGots, " New player calls: " + trueNewbs
                               };

        m_botAction.smartPrivateMessageSpam(name, msg);
    }

    /**
        Returns the call statistics of the <name> staffer.

        @param name
        @param message
    */
    public void handleMystats(String name, String message) {
        // 1. Check the level of the staff member - Mod / ER / ZH
        // 2. Query the database

        // Only staff allowed to do this command
        if (opList.isBot(name) == false)
            return;

        String date = new SimpleDateFormat("yyyy-MM").format(Calendar.getInstance().getTime());
        //SimpleDateFormat datereal = new SimpleDateFormat("yyyy-MM-dd");
        String displayDate = new SimpleDateFormat("dd MMM yyyy HH:mm zzz").format(Calendar.getInstance().getTime());
        String query = null, rankQuery = null, title = "", title2 = "";
        HashMap<String, String> stats = new HashMap<String, String>();
        ArrayList<String> rank = new ArrayList<String>();
        message = message.trim().toLowerCase();
        boolean showPersonalStats = true, showTopStats = true, showSingleStats = false, er = false;
        int topNumber = 5;

        // !mystats <month>-<year> in format of m-yyyy or mm-yyyy
        String[] parameters = message.trim().split(" ");

        if (parameters[0] != null && parameters[0].contains("-")) {
            String[] dateParameters = parameters[0].split("-");

            if (Tools.isAllDigits(dateParameters[0]) && Tools.isAllDigits(dateParameters[1])) {
                int month = Integer.parseInt(dateParameters[0]);
                int year = Integer.parseInt(dateParameters[1]);

                Calendar tmp = Calendar.getInstance();
                tmp.set(year, month - 1, 25, 23, 59, 59);

                date = new SimpleDateFormat("yyyy-MM").format(tmp.getTime());
                displayDate = new SimpleDateFormat("MMMM yyyy").format(tmp.getTime());
                message = message.substring(parameters[0].length()).trim();
            }
        }

        if ((opList.isModerator(name) && message.length() == 0) || message.startsWith("mod")) {
            // Staffer> !mystats
            // Staffer> !mystats mod

            date = date + "-01";
            query = "SELECT fcUserName, fnCount FROM tblCall WHERE fdDate='" + date + "' AND fcUserName NOT LIKE '%<ZH>%' AND fcUserName NOT LIKE '%<ER>%' AND fnType=0 ORDER BY fcUserName";
            rankQuery = "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='" + date
                        + "' AND fnType=0 AND fcUserName NOT LIKE '%<ZH>%' AND fcUserName NOT LIKE '%<ER>%' ORDER BY fnCount DESC";
            title = "Top 5 call count | " + displayDate;
            title2 = "Your call count";
            showTopStats = true;
            showPersonalStats = true;
            showSingleStats = false;
            topNumber = 5;

            if (message.length() > 3) {
                // Staffer> !mystats mod #
                String number = message.substring(4);

                if (Tools.isAllDigits(number)) {
                    topNumber = Integer.parseInt(number);
                    title = "Top " + topNumber + " call count | " + displayDate;
                }

                showPersonalStats = false;
            }

            // Order and create a list out of the results
            if (query == null || rankQuery == null)
                return;

            try {
                ResultSet results = m_botAction.SQLQuery(mySQLHost, query);

                while (results != null && results.next()) {
                    String staffer = results.getString(1);
                    String count = results.getString(2);
                    stats.put(staffer, Tools.formatString(count + "", 3));
                }

                m_botAction.SQLClose(results);
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }

            // Determine the rank
            try {
                ResultSet results = m_botAction.SQLQuery(mySQLHost, rankQuery);

                while (results != null && results.next())
                    rank.add(results.getString(1));

                m_botAction.SQLClose(results);
            } catch (Exception e) {
                Tools.printStackTrace(e);
            }

            // Return the top 5
            if (showTopStats) {
                m_botAction.sendSmartPrivateMessage(name, title);
                m_botAction.sendSmartPrivateMessage(name, "------------------");

                for (int i = 0; i < topNumber; i++)
                    if (i < rank.size())
                        m_botAction.sendSmartPrivateMessage(name, " " + Tools.formatString((i + 1) + ")", 5) + Tools.formatString(rank.get(i), 20) + " " + stats.get(rank.get(i)));
            }

            // Return your position, one previous and one next
            if (showPersonalStats) {
                int yourPosition = -1;

                // Determine your position in the rank
                for (int i = 0; i < rank.size(); i++)
                    if (rank.get(i).equalsIgnoreCase(name)) {
                        yourPosition = i;
                        break;
                    }

                // Response
                m_botAction.sendSmartPrivateMessage(name, "    "); // spacer
                m_botAction.sendSmartPrivateMessage(name, title2);
                m_botAction.sendSmartPrivateMessage(name, "-----------------");

                if (yourPosition == -1)
                    m_botAction.sendSmartPrivateMessage(name, " There is no statistic from your name found.");
                else
                    for (int i = yourPosition - 1; i < yourPosition + 2; i++)
                        if (i > -1 && i < rank.size())
                            m_botAction.sendSmartPrivateMessage(name, " " + Tools.formatString((i + 1) + ")", 5) + Tools.formatString(rank.get(i), 20) + " " + stats.get(rank.get(i)));
            }

            return;
        } else if ((opList.isERExact(name) && message.length() == 0) || message.startsWith("er")) {
            // Staffer> !mystats
            // Staffer> !mystats er
            er = true;
            query = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '" + date + "%' GROUP BY fcUserName ORDER BY count DESC";
            rankQuery = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '" + date + "%' GROUP BY fcUserName ORDER BY count DESC";
            title = "Top 5 advert count | " + displayDate;
            title2 = "Your advert count";
            showPersonalStats = true;
            showTopStats = true;
            showSingleStats = false;
            topNumber = 5;

            if (message.length() > 2) {
                // Staffer> !mystats er #
                String number = message.substring(3);

                if (Tools.isAllDigits(number)) {
                    topNumber = Integer.parseInt(number);
                    title = "Top " + topNumber + " advert count | " + displayDate;
                }

                showPersonalStats = false;
            }

        } else if ((opList.isZHExact(name) && message.length() == 0) || message.startsWith("zh")) {
            // Staffer> !mystats
            // Staffer> !mystats zh
            date = date + "-01";
            query = "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='" + date + "' AND fcUserName LIKE '%<zh>%' ORDER BY fcUserName, fnType";
            rankQuery = "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='" + date + "' AND fnType=0 AND fcUserName LIKE '%<zh>%' ORDER BY fnCount DESC";
            title = "Top 5 call count | " + displayDate;
            title2 = "Your call count";
            showPersonalStats = true;
            showTopStats = true;
            showSingleStats = false;
            topNumber = 5;

            if (message.length() > 2) {
                // Staffer> !mystats zh #
                String number = message.substring(3);

                if (Tools.isAllDigits(number)) {
                    topNumber = Integer.parseInt(number);
                    title = "Top " + topNumber + " call count | " + displayDate;
                }

                showPersonalStats = false;
            }
        } else if (message.startsWith("that")) {
            // Staffer> !mystats that
            date = date + "-01";
            query = "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='" + date + "' AND fnType=2 ORDER BY fcUserName, fnType";
            rankQuery = "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='" + date + "' AND fnType=2 ORDER BY fnCount DESC";
            title = "Top 5 that count | " + displayDate;
            title2 = "Your that count";
            showPersonalStats = true;
            showTopStats = true;
            showSingleStats = false;
            topNumber = 5;

            if (message.length() > 4) {
                // Staffer> !mystats that #
                String number = message.substring(5);

                if (Tools.isAllDigits(number)) {
                    topNumber = Integer.parseInt(number);
                    title = "Top " + topNumber + " that count | " + displayDate;
                }

                showPersonalStats = false;
            }

        } else {
            // Staffer> !mystats <name>
            String playername = message;

            if (opList.isBot(playername)) {
                if (opList.isERExact(playername)) {
                    er = true;
                    query = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '" + date + "%' AND fcUserName LIKE '" + playername + "' GROUP BY fcUserName";
                    rankQuery = "SELECT fcUserName, COUNT(fnAdvertID) as count FROM tblAdvert WHERE fdTime LIKE '" + date + "%' AND fcUserName LIKE '" + playername + "' GROUP BY fcUserName";
                } else {
                    date = date + "-01";
                    query = "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='" + date + "' AND fcUserName LIKE '" + playername + "' ORDER BY fnType";
                    rankQuery = "SELECT fcUserName, fnCount, fnType FROM tblCall WHERE fdDate='" + date + "' AND fnType=0 AND fcUserName LIKE '" + playername + "' ORDER BY fnCount DESC";
                }
            } else
                m_botAction.sendSmartPrivateMessage(name, "No staff member with the name '" + playername + "' found.");

            showPersonalStats = false;
            showTopStats = false;
            showSingleStats = true;
        }

        // Order and create a list out of the results
        if (query == null || rankQuery == null)
            return;

        try {
            ResultSet results = m_botAction.SQLQuery(mySQLHost, query);

            while (results != null && results.next()) {
                String staffer = results.getString(1);
                String count = results.getString(2);

                if (!er && stats.containsKey(staffer)) {
                    int type = results.getInt("fnType");

                    // query sets the fnType=1 as second, so this is the "got it"s
                    if (type == 2)
                        stats.put(staffer, stats.get(staffer) + " [" + count + "]");
                    else
                        stats.put(staffer, stats.get(staffer) + " (" + Tools.formatString(count + ")", 3));
                } else
                    // query sets the fnType=0 as first, so this is the "on it"s
                    stats.put(staffer, Tools.formatString(count, 3));
            }

            m_botAction.SQLClose(results);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }

        // Determine the rank
        try {
            ResultSet results = m_botAction.SQLQuery(mySQLHost, rankQuery);

            while (results != null && results.next())
                rank.add(results.getString(1));

            m_botAction.SQLClose(results);
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }

        // Return the top 5
        if (showTopStats) {
            m_botAction.sendSmartPrivateMessage(name, title);
            m_botAction.sendSmartPrivateMessage(name, "------------------");

            for (int i = 0; i < topNumber; i++)
                if (i < rank.size())
                    m_botAction.sendSmartPrivateMessage(name, " " + Tools.formatString((i + 1) + ")", 5) + Tools.formatString(rank.get(i), 20) + " " + stats.get(rank.get(i)));
        }

        // Return your position, one previous and one next
        if (showPersonalStats) {
            int yourPosition = -1;

            // Determine your position in the rank
            for (int i = 0; i < rank.size(); i++)
                if (rank.get(i).equalsIgnoreCase(name)) {
                    yourPosition = i;
                    break;
                }

            // Response
            m_botAction.sendSmartPrivateMessage(name, "    "); // spacer
            m_botAction.sendSmartPrivateMessage(name, title2);
            m_botAction.sendSmartPrivateMessage(name, "-----------------");

            if (yourPosition == -1)
                m_botAction.sendSmartPrivateMessage(name, " There is no statistic from your name found.");
            else
                for (int i = yourPosition - 1; i < yourPosition + 2; i++)
                    if (i > -1 && i < rank.size())
                        m_botAction.sendSmartPrivateMessage(name, " " + Tools.formatString((i + 1) + ")", 5) + Tools.formatString(rank.get(i), 20) + " " + stats.get(rank.get(i)));
        }

        if (showSingleStats)
            if (stats.size() > 0 && rank.size() > 0)
                m_botAction.sendSmartPrivateMessage(name, " " + Tools.formatString(rank.get(0), 20) + " " + stats.get(rank.get(0)));
            else
                m_botAction.sendSmartPrivateMessage(name, "No statistic of " + message + " found.");

    }

    public void loadLastNewbs(String sender, String msg) {
        String query = "SELECT fcUserName as newb, fcTakerName as staff, fdCreated as time, fnTaken as taken FROM tblCallNewb ORDER BY fdCreated DESC LIMIT 5";

        try {
            Vector<NewbCall> temp = new Vector<NewbCall>();
            ResultSet rs = m_botAction.SQLQuery(mySQLHost, query);

            while (rs.next()) {
                String newb = rs.getString("newb");
                int taken = rs.getInt("taken");
                long time = rs.getTimestamp("time").getTime();
                NewbCall np = new NewbCall(newb);
                np.timeSent = time;
                np.claimType = taken;

                if (taken == 3)
                    np.claimType = NewbCall.FALSE;
                else if (taken == 1 || taken == 2) {
                    np.claimer = rs.getString("staff");
                    np.claimType = NewbCall.TAKEN;
                }

                temp.add(0, np);
            }

            for (int i = 0; i < temp.size(); i++) {
                NewbCall np = temp.get(i);
                lastNewPlayerName = np.getName();
                newbs.add(np);
                newbHistory.put(lastNewPlayerName.toLowerCase(), np);
                newbNames.add(0, lastNewPlayerName.toLowerCase());
            }

            m_botAction.SQLClose(rs);
            m_botAction.sendSmartPrivateMessage(sender, "Great success!");
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(sender, "Failure :(");
            Tools.printStackTrace(e);
        }
    }

    public void updateStatRecordsONTHAT(String name, String player, boolean record) {
        if (!m_botAction.SQLisOperational())
            return;

        try {
            if (!record) {
                m_botAction.SQLBackgroundQuery(mySQLHost, null, "UPDATE tblCallNewb SET fnTaken = 2, fcTakerName = '" + Tools.addSlashesToString(name) + "' WHERE fcUserName = '"
                                               + Tools.addSlashesToString(player) + "'");
                return;
            }

            m_botAction.SQLBackgroundQuery(mySQLHost, null, "UPDATE tblCallNewb SET fnTaken = 1, fcTakerName = '" + Tools.addSlashesToString(name) + "' WHERE fcUserName = '"
                                           + Tools.addSlashesToString(player) + "'");
            String time = new SimpleDateFormat("yyyy-MM").format(Calendar.getInstance().getTime()) + "-01";
            ResultSet result = m_botAction.SQLQuery(mySQLHost, "SELECT * FROM tblCall WHERE fcUserName = '" + name + "' AND fnType = 2 AND fdDate = '" + time + "'");

            if (result.next())
                m_botAction.SQLBackgroundQuery(mySQLHost, null, "UPDATE tblCall SET fnCount = fnCount + 1 WHERE fcUserName = '" + Tools.addSlashesToString(name) + "' AND fnType = 2 AND fdDate = '"
                                               + time + "'");
            else
                m_botAction.SQLBackgroundQuery(mySQLHost, null, "INSERT INTO tblCall (`fcUserName`, `fnCount`, `fnType`, `fdDate`) VALUES ('" + Tools.addSlashesToString(name) + "', '1', '2', '"
                                               + time + "')");

            m_botAction.SQLClose(result);
        } catch (Exception e) {
            //m_botAction.sendChatMessage(2, "Error occured when registering call claim from '"+name+"' :"+e.getMessage());
            Tools.printStackTrace(e);
        }
    }

    public void mainHelpScreen(String playerName, String message) {
        final String[] helpText = {
            "Chat commands:",
            " !repeat <optional name>              - Repeats the response to the specified name.  If no",
            "                                        name is specified, the last response is repeated.",
            " !tell <name>:<keyword>               - Private messages the specified name with the",
            "                                        response to the keyword given.",
            " !warn <optional name>                - Warns the specified player.  If no name is given,",
            "                                        warns the last person.",
            " !ban <optional name>                 - Bans the specified player.  If no name is given,",
            "                                        bans the last person. (ER+)",
            " !status                              - Gives back status from systems.",
            //            " !google search - Returns first page found by Googling the search term.",
            " !dictionary word                     - Returns a link for a definition of the word.",
            " !thesaurus word                      - Returns a link for a thesaurus entry for the word.",
            " !javadocs term                       - Returns a link for a javadocs lookup of the term.",
            " !google word                         - Returns a link for a google search of the word.",
            " !wiki word                           - Returns a link for a wikipedia search of the word.",
            " !calls                               - Displays the last 5 help and cheater calls",
            " !calls <num>                         - Displays the last <num> help and cheater calls",
            " ",
            "PM commands:",
            " !calls                               - Displays the last 5 help and cheater calls",
            " !calls <num>                         - Displays the last <num> help and cheater calls",
            " !stats                               - Returns call answer stats for this month",
            " !stats <month>-<year>                - Returns call answer stats for the month specified",
            "                                        ex. !stats 01-2011",
            " !lookup <keyword>                    - Tells you the response when the specified key word",
            "                                        is given",
            " !last <optional name>                - Tells you what the response to the specified",
            "                                        player was. If no name is specified, the last",
            "                                        response is given.",
            " !last <call id>                      - Tells you the last response given to the player",
            "                                        associated with the specified call id.",
            " !mystats                             - Returns the top 5 call count and your call stats",
            " !mystats mod/er/zh [#]               - Returns the top # of moderators / ERs / ZHs.",
            "                                        If # is not specified, shows top 5.",
            " !mystats <name>                      - Returns the call count of <name>",
            " !mystats <month>-<year> [above args] - Returns the top/call count from specified",
            "                                        month-year. F.ex: !mystats 08-2007 mod 50",
            " !hourlystats                         - Hourly call stats"
        };

        if (m_botAction.getOperatorList().isZH(playerName))
            m_botAction.remotePrivateMessageSpam(playerName, helpText);

        String[] SMod = { " !banned                              - Who's banned from using tell?!?!", " !addban                              - Add a tell ban",
                          " !unban                               - Remove a tell ban", " !say                                 - SMod fun!"
                        };

        if (m_botAction.getOperatorList().isSmod(playerName))
            m_botAction.remotePrivateMessageSpam(playerName, SMod);

        String[] SysopHelpText = { " !reload                              - Reloads the HelpResponses database from file", " !die                                 - Disconnects this bot" };

        if (m_botAction.getOperatorList().isSysop(playerName))
            m_botAction.remotePrivateMessageSpam(playerName, SysopHelpText);
    }

    public void claimHelpScreen(String playerName, String message) {
        final String[] helpText = { "Chat claim commands:", "  !calls                                   - Displays the last 5 help and cheater calls",
                                    "  !calls <num>                             - Displays the last <num> help and cheater calls",
                                    "  on it                                    - (on)Same as before, claims the earliest non-expired call",
                                    "  on it <id>, on it #<id>,                 - (on)Claims Call <id> if it hasn't expired",
                                    "  got it                                   - (got)Same as before, claims the earliest non-expired call",
                                    "  got it <id>, got it #<id>,               - (got)Claims Call #<id> if it hasn't expired", "Claim modifier commands:",
                                    "  mine                                     - Claims the most recent call but does not affect staff stats",
                                    "  mine #<id>, mine <id>                    - Claims Call #<id> but does not affect staff stats",
                                    "  clean                                    - Clears the most recent false positive racism alert",
                                    "  clean #<id>, clean <id>                  - Clears Call #<id> due to a false positive racism alert",
                                    "  forget                                   - Prevents the most recent call from being counted as unanswered",
                                    "  forget #<id>, forget <id>                - Prevents Call #<id> from being counted as unanswered", "Multiple call claim modification (mine/clean/forget)",
                                    "  - To claim multiple calls at once, just add the call numbers separated by commas", "     ie mine 6,49,3,#4,1",
                                    "  - To claim multiple consecutive calls at once, specify a range using a dash (-)", "     ie forget 5-#32", "New Player commands:",
                                    "  !false                                   - Falsifies the last new player alert so that it won't affect stats",
                                    "  !false <Player>                          - Falsifies all new player alerts for <Player> (doesn't have to be in !newbs)",
                                    "  !undo                                    - Un-falsifies the last new player alert so that it will affect stats",
                                    "  !undo <Player>                           - Un-falsifies all new player alerts for <Player> (doesn't have to be in !newbs)",
                                    "  !newbs                                   - Lists recent new player alerts and claimer information",
                                    "  !newbs <num>                             - Lists the last <nuM> new player alerts and claimer information",
                                    "  ihave                                    - Claims most recent newplayer call but does not affect stats",
                                    "  ihave #<id>, ihave <id>                  - Claims newplayer Call #<id> but does not affect staff stats"
                                  };

        m_botAction.smartPrivateMessageSpam(playerName, helpText);
    }

    private int indexNotOf(String string, char target, int fromIndex) {
        for (int index = fromIndex; index < string.length(); index++)
            if (string.charAt(index) != target)
                return index;

        return -1;
    }

    private int getBreakIndex(String string, int fromIndex) {
        if (fromIndex + LINE_SIZE > string.length())
            return string.length();

        int breakIndex = string.lastIndexOf(' ', fromIndex + LINE_SIZE);

        if ("?".equals(string.substring(breakIndex + 1, breakIndex + 2)) || "*".equals(string.substring(breakIndex + 1, breakIndex + 2)))
            breakIndex = string.lastIndexOf(' ', fromIndex + LINE_SIZE - 3);

        if (breakIndex == -1)
            return fromIndex + LINE_SIZE;

        return breakIndex;
    }

    private String[] formatResponse(String response) {
        LinkedList<String> formattedResp = new LinkedList<String>();
        int startIndex = indexNotOf(response, ' ', 0);
        int breakIndex = getBreakIndex(response, 0);

        while (startIndex != -1) {
            formattedResp.add(response.substring(startIndex, breakIndex));
            startIndex = indexNotOf(response, ' ', breakIndex);
            breakIndex = getBreakIndex(response, startIndex);
        }

        return formattedResp.toArray(new String[formattedResp.size()]);
    }

    private void displayResponses(String name, String[] responses) {
        for (int counter = 0; counter < responses.length; counter++)
            m_botAction.remotePrivateMessageSpam(name, formatResponse(responses[counter]));
    }

    @Override
    public void handleEvent(Message event) {

        m_commandInterpreter.handleEvent(event);

        if (event.getMessageType() == Message.ALERT_MESSAGE) {
            String command = event.getAlertCommandType().toLowerCase();

            //clean msg of ": (" for reuse
            String msg = event.getMessage().replace(": (", ":(");

            if (command.equals("help"))
                handleHelp(event.getMessager(), msg);
            else if (command.equals("cheater"))
                handleCheater(event.getMessager(), msg);
            else if (command.equals("advert"))
                handleAdvert(event.getMessager(), msg);
            else if (command.equals("newplayer"))
                handleNewPlayer(event.getMessager(), msg);
        } else if (event.getMessageType() == Message.CHAT_MESSAGE) {
            String name = event.getMessager();

            if (!opList.isZH(name))
                return;

            String message = event.getMessage().toLowerCase().trim();

            if (message.equalsIgnoreCase("!alert"))
                toggleAlert(name, "");
            else if (message.startsWith("!false"))
                handleFalseNewb(name, message);
            else if (message.startsWith("!undo"))
                handleUndoFalse(name, message);
            else if (!message.contains("that") && !message.contains("it") && (message.startsWith("on") || message.startsWith("got") || message.startsWith("claim") || message.startsWith("have")))
                handleClaims(name, message);
            else if (!message.contains("that") && message.contains("#") && (message.startsWith("on") || message.startsWith("got") || message.startsWith("claim") || message.startsWith("have")))
                handleClaims(name, message);
            else if (message.startsWith("on that") || message.startsWith("got that"))
                handleThat(name, null);
            else if (message.startsWith("ihave"))
                handleHave(name, message);
            else if (message.startsWith("clean"))
                handleClean(name, event.getMessage());
            else if (message.startsWith("forget"))
                handleForget(name, event.getMessage());
            else if (message.startsWith("mine"))
                handleMine(name, event.getMessage());
            else if ((message.startsWith("on it") || message.startsWith("got it")))
                if(event.getChatNumber() == 3) {
                    twdchat = true;
                    handleClaims(name, message);
                } else {
                    twdchat = false;
                    handleClaims(name, message);

                }

        }

    }

    class EventData {

        String arena;
        long time;
        int dups;
        int callID;

        public EventData(String a) {
            arena = a;
            dups = 1;
        }

        public EventData(long t) {
            time = t;
        }

        public EventData(int id, long t) {
            time = t;
            callID = id;
        }

        public EventData(String a, long t) {
            arena = a;
            time = t;
        }

        public void inc() {
            dups++;
        }

        public int getID() {
            return callID;
        }

        public String getArena() {
            return arena;
        }

        public long getTime() {
            return time;
        }

        public int getDups() {
            return dups;
        }
    }

    class PlayerInfo {
        String m_playerName;
        boolean m_beenWarned;
        boolean m_advertTell;
        boolean m_newplayerTell;
        int m_lastCall;
        Vector<Integer> m_calls;

        public PlayerInfo(String name) {
            m_playerName = name;
            m_beenWarned = false;
            m_advertTell = false;
            m_newplayerTell = false;
            m_lastCall = -1;
            m_calls = new Vector<Integer>();
        }

        public void addCall(int id) {
            m_calls.add(id);
            m_lastCall = id;
        }

        public Vector<Integer> getCalls() {
            return m_calls;
        }

        public int getLastCall() {
            return m_lastCall;
        }

        public void setBeenWarned(boolean beenWarned) {
            m_beenWarned = beenWarned;
        }

        public boolean getBeenWarned() {
            return m_beenWarned;
        }

        public void setAdvertTell(boolean advertTell) {
            m_advertTell = advertTell;
        }

        public boolean AdvertTell() {
            return m_advertTell;
        }

        public void setNewplayerTell(boolean newplayerTell) {
            m_newplayerTell = newplayerTell;
        }

        public boolean NewplayerTell() {
            return m_newplayerTell;
        }
    }
}
