package twcore.bots.sbbot;
import twcore.core.*;

public class BotCommandEvent extends twcore.bots.sbbot.Message {
    Player sender = null;
    String arguments = "";
    
    public BotCommandEvent(Player p, String args) {
	sender = p;
	arguments = args;
    }

    public BotCommandEvent(Player p) {
	sender = p;
    }

    public Player getSender() { return sender; }
    public String getArgs() { return arguments; }
}