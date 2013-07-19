package twcore.bots.multibot.pointrace;

import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.ScoreUpdate;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class pointrace extends MultiModule {

    private PointTimer timer;
    private int[] points;
    private BotAction ba = m_botAction;

    @Override
    public void init() {
        points = new int[] {0, 0};
        timer = null;
    }

    @Override
    public void requestEvents(ModuleEventRequester eventRequester) {
        eventRequester.request(this, EventRequester.MESSAGE);
        eventRequester.request(this, EventRequester.SCORE_UPDATE);
    }
    
    public void handleEvent(ScoreUpdate event) {
        if (timer == null) return;
        if (ba.getPlayer(event.getPlayerID()).getFrequency() == 0)
            points[0] += event.getFlagPoints() + event.getKillPoints();
        else
            points[1] += event.getFlagPoints() + event.getKillPoints();
    }
    
    public void handleEvent(Message event) {
        int type = event.getMessageType();
        String msg = event.getMessage().toLowerCase();

        if (type == Message.PRIVATE_MESSAGE || type == Message.REMOTE_PRIVATE_MESSAGE) {
            String name = ba.getPlayerName(event.getPlayerID());
            if (name == null)
                name = event.getMessager();
            if (ba.getOperatorList().isER(name)) {
                if (msg.startsWith("!start "))
                    cmd_start(name, msg);
                else if (msg.equals("!stop"))
                    cmd_stop(name);
                else if (msg.equals("!score"))
                    cmd_score(name);
            }
        }
    }
    
    /**
     * Starts a new point race.
     * 
     * @param cmd
     *          total time in minutes.
     * @param name
     */
    private void cmd_start(String name, String cmd) {
        int time;
        try {
            time = Integer.valueOf(cmd.substring(cmd.indexOf(" ") + 1));
        } catch (NumberFormatException e) {
            return;
        }
        if (time < 1 || time > 60) {
            ba.sendSmartPrivateMessage(name, "Invalid time value.");
            return;
        }
        points = new int[] {0, 0};
        timer = new PointTimer();
        ba.scheduleTask(timer, time * Tools.TimeInMillis.MINUTE);
        ba.sendArenaMessage("Point race to " + time + " minutes. GO GO GO!!!", 105);
        ba.setTimer(time);
    }
    
    /**
     * Stops the current game.
     * 
     * @param name
     */
    private void cmd_stop(String name) {
        if (timer != null) {
            ba.cancelTask(timer);
            ba.sendArenaMessage("This game has been brutally killed by " + name + "!");
        }
        timer = null;
    }
    
    /**
     * Gets the current score.
     * 
     * @param name
     */
    private void cmd_score(String name) {
        if (timer != null)
            ba.sendSmartPrivateMessage(name, "Score: " + points[0] + " - " + points[1]);
        else
            ba.sendSmartPrivateMessage(name, "No game in progress.");
    }

    @Override
    public String[] getModHelpMessage() {
        String[] help = {
                "!start <t>   - Starts a point race that ends after <t> minutes.",
                "!stop        - Stops a game currently in progress.",
                "!score       - Displays score.",
        };
        return help;
    }

    @Override
    public boolean isUnloadable() {
        return timer == null;
    }
    
    @Override
    public void cancel() {
        if (timer != null)
            ba.cancelTask(timer);
        timer = null;
    }
    
    /**
     * PointTimer announces the winning freq after the game timer expires.
     *
     * @author WingZero
     */
    private class PointTimer extends TimerTask {
        
        @Override
        public void run() {
            if (points[0] > points[1])
                ba.sendArenaMessage("Freq 0 wins! Score: " + points[0] + " - " + points[1], 5);
            else if (points[1] > points[0])
                ba.sendArenaMessage("Freq 1 wins! Score: " + points[0] + " - " + points[1], 5);
            else
                ba.sendArenaMessage("TIE! Score: " + points[0] + " - " + points[1], 5);
            timer = null;
        }
        
    }

}
