/*
 * ultrabot.java
 *
 * Created on March 17, 2004, 11:45 AM
 *
 * This is the most simple form of a bot. You can use this template whenever
 * you create a new bot
 */

package twcore.bots.basicbot;

import twcore.core.*;
import twcore.core.events.ArenaJoined;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;

/**
 *
 * @author  Stefan / Mythrandir
 */
public class basicbot extends SubspaceBot {

    //
    private BotSettings m_botSettings;


    /** Creates a new instance of ultrabot */
    public basicbot(BotAction botAction) {
        super(botAction);
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


    /** write an event handler for each requested packet */
    public void handleEvent(Message event) {
        // retreive name. If the message is remote then event.getMessager() returns null, and event.getPlayerID returns a value
        // if the message is from the same arena, event.getMessager returns a string, and event.getPlayerID will return 0
        String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
        if (name == null) name = "-anonymous-";

        m_botAction.sendPublicMessage(  "I received a Message event type ("+ event.getMessageType()+") from " + name +
                                        " containing the following text: " + event.getMessage());


        /* most important command: !die */
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!die")) {
            m_botAction.sendPublicMessage(name + " commanded me to die. Disconnecting...");
            try { Thread.sleep(50); } catch (Exception e) {};
            m_botAction.die();
        }

    }


    /* when the bot logs on, you have to manually send it to an arena */
    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("arena"));
    }


    /* set ReliableKills 1 (*relkills 1) to make sure your bot receives every packet */
    public void handleEvent(ArenaJoined event) {
        m_botAction.setReliableKills(1);
    }




}


