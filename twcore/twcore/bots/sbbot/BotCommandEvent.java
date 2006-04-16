package twcore.bots.strikeballbot;
import twcore.core.*;

public class BotCommandEvent extends twcore.bots.strikeballbot.Message {
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