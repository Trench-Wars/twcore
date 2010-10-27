package twcore.bots.elimbot.elimstate;

import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;

public class ElimStateGameRunning
        extends ElimState {

    @Override
    public void handleEvent(PlayerDeath event) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleEvent(Message event) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Game Running State";
    }

    @Override
    public int[] getVotes() {
        // TODO Auto-generated method stub
        return null;
    }

}
