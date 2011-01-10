package twcore.bots.elimbot;

import twcore.bots.elimbot.elimsystem.ElimSystem;
import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
/**
 * 
 * New elim
 * 
 * @author quiles/dexter
 * @see http://forums.trenchwars.org/showthread.php?32249-Elim-ranks-and-records
 * 
 * AveK = Kills/Games
 * AveD = Deaths/Games
 * rating = AveK / AveD * Ave
 * 
 * everyone starts with Ave of 300
 * 
 * Ave = ave1+ave2+ave3+...+aven/n where avei is the ave of person I killed and n is the number of kills
 * First game everyone keeps ave of 300 because 300+300+300+...+300 / n = 300n/n = 300
 * Then first game the rating would depend on k/d only.
 * And from 2nd game on, Ave = previous rating, decreasing or increasing it.
 * 
 * A player can only get a rank when his kills+death >= 500
 * 
 * Might make logn the formula to get stability of ratings
 * 
 * Idea of hot or not by Crome - last 5 days
 * 
 * Rating = Rating + (Rating*(Wins/Games)/100)
 * Rating = log(
 * */
public class elimbot
        extends SubspaceBot {

    private ElimSystem elimSystem;
    
    public elimbot(BotAction botAction) {
        super(botAction);
        // TODO Auto-generated constructor stub
        this.elimSystem = new ElimSystem(botAction);
        requestEvents();
    }

    @Override
    public void handleEvent(PlayerDeath event){
        this.elimSystem.handleEvent(event);
    }
    
    
    @Override
    public void handleEvent(PlayerEntered event){
        
    }
    
    @Override
    public void handleEvent(PlayerLeft event){
        
    }
    
    @Override
    public void handleEvent(FrequencyShipChange event){
        
    }
    
    public void handleEvent(Message event)
    {
        String playerName = m_botAction.getPlayerName(event.getPlayerID());
        int messageType = event.getMessageType();
        
        if(messageType == Message.REMOTE_PRIVATE_MESSAGE || messageType == Message.PRIVATE_MESSAGE)
            ;
        
        this.elimSystem.handleEvent(event);
    }
    
    public void handleEvent(LoggedOn event){
        BotSettings botSettings = m_botAction.getBotSettings();
        String initialArena = botSettings.getString("Arena");
        String chat = botSettings.getString("Chat");
        m_botAction.sendUnfilteredPublicMessage("?chat="+chat);
        m_botAction.changeArena(initialArena);
        this.elimSystem.handleEvent(event);
    }
    
    private void requestEvents(){
        EventRequester eventRequester = m_botAction.getEventRequester();
        eventRequester.request(EventRequester.MESSAGE);
        eventRequester.request(EventRequester.ARENA_LIST);
    }
}
