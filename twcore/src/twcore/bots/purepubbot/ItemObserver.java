package twcore.bots.purepubbot;

import java.util.List;

import twcore.core.BotAction;
import twcore.core.SubspaceBot;

public abstract class ItemObserver extends SubspaceBot {
    
    protected List<String> allowedPlayersToUseItem;
    
    public ItemObserver(BotAction botAction) {
        super(botAction);
        // TODO Auto-generated constructor stub
    }
    public abstract void update(String playerName);
    public abstract void update(String playerName, String whatToEnableDisable, int time);
    public abstract void update(boolean nextCanBuy);
    public abstract void doSetCmd(String sender, String argString);
}
