package twcore.bots.sbbot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.BallPosition;
import twcore.core.events.FileArrived;
import twcore.core.events.FlagClaimed;
import twcore.core.events.FlagDropped;
import twcore.core.events.FlagPosition;
import twcore.core.events.FlagReward;
import twcore.core.events.FlagVictory;
import twcore.core.events.FrequencyChange;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.SQLResultEvent;
import twcore.core.events.ScoreReset;
import twcore.core.events.ScoreUpdate;
import twcore.core.events.SoccerGoal;
import twcore.core.events.SubspaceEvent;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;

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
}