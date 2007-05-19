package twcore.bots.sbbot;
import java.util.Date;

import twcore.core.game.Player;

public class SBEvent extends Message {
    public final Player player;
    public final Date time;

    public SBEvent(Player p) {
	player = p;
	time = new Date();
    }

    public SBEvent() {
	player = null;
	time = new Date();
    }
}