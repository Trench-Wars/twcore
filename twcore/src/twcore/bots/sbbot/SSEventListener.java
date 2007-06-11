package twcore.bots.sbbot;
import twcore.core.events.*;

public class SSEventListener extends Listener {
    public SSEventListener() {}

    public void notify(SSEventMessageType message, SubspaceEvent event) {}
    public void notify(SSEventMessageType message, WatchDamage event) {}
    public void notify(SSEventMessageType message, ScoreReset event) {}
    public void notify(SSEventMessageType message, PlayerEntered event) {}
    public void notify(SSEventMessageType message, twcore.core.events.Message event) {}
    public void notify(SSEventMessageType message, PlayerLeft event) {}
    public void notify(SSEventMessageType message, PlayerPosition event) {}
    public void notify(SSEventMessageType message, PlayerDeath event) {}
    public void notify(SSEventMessageType message, ScoreUpdate event) {}
    public void notify(SSEventMessageType message, WeaponFired event) {}
    public void notify(SSEventMessageType message, FrequencyChange event) {}
    public void notify(SSEventMessageType message, FrequencyShipChange event) {}
    public void notify(SSEventMessageType message, ArenaJoined event) {}
    public void notify(SSEventMessageType message, FileArrived event) {}
    public void notify(SSEventMessageType message, FlagReward event) {}
    public void notify(SSEventMessageType message, FlagVictory event) {}
    public void notify(SSEventMessageType message, FlagPosition event) {}
    public void notify(SSEventMessageType message, FlagClaimed event) {}
    public void notify(SSEventMessageType message, FlagDropped event) {}
    public void notify(SSEventMessageType message, SoccerGoal event) {}
    public void notify(SSEventMessageType message, BallPosition event) {}
    public void notify(SSEventMessageType message, Prize event) {}
    public void notify(SSEventMessageType message, LoggedOn event) {}
    public void notify(SSEventMessageType message, ArenaList event) {}
    public void notify(SSEventMessageType message, InterProcessEvent event) {}
    public void notify(SSEventMessageType message, SQLResultEvent event) {}
    public void notify(SSEventMessageType message, PlayerBanner event) {}
}