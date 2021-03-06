package twcore.bots.multibot.util;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.events.BallPosition;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;

/**
    Utility to drop a ball at a given location. Currently only the first ball
    will be moved.

    @author JoyRider
*/

public class utilballdrop extends MultiUtil {
    private int[] m_lastBallTimestamp;

    /**
        Initializes global variables.
    */
    public void init() {
        m_lastBallTimestamp = new int[8];
    }

    /**
        Requests events.
    */
    public void requestEvents(ModuleEventRequester events) {
        events.request(this, EventRequester.BALL_POSITION);
    }

    /**
        Handles ball position updates, required to get the correct ball timestamp
    */
    public void handleEvent(BallPosition event) {
        m_lastBallTimestamp[event.getBallID()] = event.getTimeStamp();
    }

    /**
        Handles all message events.
    */
    public void handleEvent(Message event) {
        String message = event.getMessage();
        String playerName = m_botAction.getPlayerName(event.getPlayerID());

        if (event.getMessageType() == Message.PRIVATE_MESSAGE) {
            if (m_opList.isER(playerName)) {
                if (message.startsWith("!ball ")) {
                    checkDropBall(playerName, message.substring(6));
                }
            }
        }
    }

    /**
        Checks for correct coordinates for !ball command

        @param sender
                  Player that sends the request
        @param coords
    */
    public void checkDropBall(String sender, String coords) {
        byte id = 0;
        short x = -1, y = -1;
        boolean valid = true;
        String[] vars = coords.split("(:| )");

        if (vars.length < 3)
            valid = false;

        if (valid) {
            try {
                id = Byte.parseByte(vars[0]);
                x = Short.parseShort(vars[1]);
                y = Short.parseShort(vars[2]);
            } catch (NumberFormatException e) {
                valid = false;
            }
        }

        if (valid) {
            if ((id < 0 || id > 7) || (x <= 0 || x > 1024)
                    || (y <= 0 || y > 1024))
                valid = false;
        }

        if (!valid)
            m_botAction
            .sendPrivateMessage(
                sender,
                "Invalid usage, the correct usage is !ball <ball>:<x>:<y>. Coordinates range from 1 to 1024");
        else {
            doDropBall(id, x, y);
            m_botAction.sendPrivateMessage(sender, "Ball dropped at " + x + " "
                                           + y);
        }
    }

    /**
        Executes the ball drop

        @param x
                  X coord of ball drop
        @param y
                  Y coord of ball drop
    */
    public void doDropBall(byte id, short x, short y) {
        m_botAction.getShip().setShip(0);
        m_botAction.getShip().move(x << 4, y << 4);
        m_botAction.getBall(id, m_lastBallTimestamp[id]);
        m_botAction.getShip().setShip(8);
    }

    /**
        Return's the bot's help messages.
    */
    public String[] getHelpMessages() {
        String[] message = { "!ball <ball>:<x>:<y> -- Drops the soccer <ball> at location <x>,<y>. Coordinates range from 1 to 1024", };
        return message;
    }

}
