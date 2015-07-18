package twcore.bots.ultrabot;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.command.AdvancedCommandInterpreter;
import twcore.core.command.Command;
import twcore.core.command.CommandDefinition;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;

/**
 * A basic form of a bot, but with command interpretation not provided in
 * the "basicbot" class template.  Use this one instead.
 * @author  Stefan / Mythrandir
 */
public class ultrabot extends SubspaceBot {

    //
    private BotSettings m_botSettings;
    private AdvancedCommandInterpreter m_commandInterpreter;
    private BotAction m_botAction;


    /** Creates a new instance of ultrabot */
    public ultrabot(BotAction botAction) {
        super(botAction);
        m_botAction = BotAction.getBotAction();
        requestEvents();

        // m_botSettings contains the data specified in file <botname>.cfg
        m_botSettings = m_botAction.getBotSettings();
    }


    /** Request events that this bot requires to receive.  */
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


    public void registerCommands() {
        m_commandInterpreter = new AdvancedCommandInterpreter();

        // test
        m_commandInterpreter.add(
                new CommandDefinition(this, "handleTest", "!game Integer 'type number' required:String 'name 1' 'freq 1':String 'name 2' 'freq 2'", Message.PRIVATE_MESSAGE,
                    "Start a new game.")
            );

        m_commandInterpreter.add(
            new CommandDefinition(this, "handleHelp", "!help", Message.PRIVATE_MESSAGE, "Show this list")
            );

    }


    /** write an event handler for each requested packet */
    public void handleEvent(Message event) {

        m_commandInterpreter.handleMessage(event);

        /* most important command: !die */
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!die"))
            m_botAction.die( "!die received" );

    }


    /* when the bot logs on, you have to manually send it to an arena */
    public void handleEvent(LoggedOn event) {
        registerCommands();
        m_botAction.joinArena(m_botSettings.getString("arena"));
    }


    /* set ReliableKills 1 (*relkills 1) to make sure your bot receives every packet */
    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }



    /* Handle command */
    public void handleTest(Command c) {
        m_botAction.sendPublicMessage("I received a command from " + c.getName() + " with the following params: ");
        m_botAction.sendPublicMessage(c.get(0) + " and " + c.get(1) + " and " + c.get(2));
    }


    public void handleHelp(Command c) {
        m_botAction.privateMessageSpam(c.getName(), m_commandInterpreter.getHelpListStringArray());
    }
}


