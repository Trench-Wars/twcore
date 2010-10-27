package twcore.bots.elimbot.elimstate;

import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;

public abstract class ElimState {
    
    
    public ElimState(){
        
    }
    
    public abstract void handleEvent(PlayerDeath event);
    public abstract void handleEvent(Message event);
    public abstract int[] getVotes();
    
}
