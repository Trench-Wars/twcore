package twcore.bots.multibot.twar;

/**
 * @author dexter
 * Thanks to turban, who gave the idea of ship promotion to turretwars event
 * Thanks to Maverick, who gave me the idea to what ship promotion's command use*/

import java.util.ArrayList;
import java.util.Collections;

import java.util.StringTokenizer;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.TurretEvent;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/*--------------- MultiBot to any TURRETWARs event ------
 * interface: turretwar
 * */
public class twar
extends MultiModule
implements turretwar
{
    private    boolean     promotion   = false;
    private    boolean     warpOn      = false;
	
    private    int         firstShip   = 0;

    private     boolean  isAlreadyPromoted = false;
    private    ArrayList<shipSettings> listShipSettings = new ArrayList() ;
	
	//guide to ER+s
	private String guideER[] = 
	{
			"1.  Use !terr name to pick terr",
			"2.  Use !switch name:newTerr to switch terrs",
			"3.  Use !prom <#ship>:<#kills> to set a promotion",
			"4.  Use !removep <#ship> to remove a ship promotion",
			"5.  Use !ship <#number> to set everyone in this ship (If you don't set this, you won't be able to !switch)",
			"6.  Use !warp to make detachers get warped to spawn area",
			"7.  do manually the *arena GO GO GO and *timer <#Minutes>",
			"8.  Use !setupwarplist to check the warp command",	
			"9. To stop the game use !stop, use it AFTER the game ends too, so you can !unload this module."
	
	};
	
	public void handleEvent(TurretEvent event)
	{
		if(!event.isAttaching()){
			if(isWarpOn()){ //if any player detaches, he'll get warped.
				int idDetacher = event.getAttacherID();
				m_botAction.specificPrize(idDetacher, 7);
				m_botAction.specificPrize(idDetacher, -13);
			}
		}
	}
	
	public void handleEvent(PlayerDeath event)
	{   
	    Player killee  = null;
	    Player killer  = null;
	    
		if(isPromotion())
		{
			killer =  m_botAction.getPlayer(event.getKillerID()) ;
		    killee =  m_botAction.getPlayer(event.getKilleeID()) ;
			
			if(killer.getFrequency() != killee.getFrequency())
			{
				if(killer.getShipType() != 5){
				    if(!this.isAlreadyPromoted()){
				        this.setAlreadyPromoted(true);
    					handleCustomPromotion(killer.getPlayerName(), (int) killer.getWins());				
    					
    					/*
    					 * A fix to *arena spam about promotions*/
    					TimerTask notArenaSpam = new TimerTask(){
    					    public void run(){
    					        setAlreadyPromoted(false);
    					    }
    					};
    					m_botAction.scheduleTask(notArenaSpam, 500);
				    }    
				}
				
			}
		}
	}

	public void handleEvent(Message event)
	{
		
		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
		{
			String message = event.getMessage();
			String name = m_botAction.getPlayerName(event.getPlayerID());
			
			if(opList.isER(name))
				handleCommand(name, message);
			else
			{
				if(event.getMessage().toLowerCase().startsWith("!help"))
				{
					if(promotion)
						m_botAction.sendPrivateMessage(name,"!lprom           - to check ship promotions");
				}
				else if(event.getMessage().toLowerCase().startsWith("!lprom"))
					listPromotion(name, message);
			}
		}
	}

	public void handleCommand(String name, String message)
	{
		    if(message.toLowerCase().startsWith("!ship"))            cmd_firstShip(name, message);
			else if(message.toLowerCase().startsWith("!terr"))       cmd_addTerr(name, message);
			else if(message.toLowerCase().startsWith("!switch"))     cmd_switchTerr(name, message);
			else if(message.toLowerCase().startsWith("!warp"))       cmd_warp(name, message);
			else if(message.toLowerCase().startsWith("!stop"))       cmd_stop(name, message);
            
			else if(message.toLowerCase().startsWith("!prom"))       setPromotion(name, message);
			else if(message.toLowerCase().startsWith("!lprom"))      listPromotion(name, message);
			else if(message.toLowerCase().startsWith("!removep"))    removePromotion(name, message);
			
			else if(message.toLowerCase().startsWith("!guide"))      showGuide(name, message);
			
			else return;
	}
	
	public void handleCustomPromotion(String name, Integer kills)
	{
	    
		for(int i = 0; i < listShipSettings.size() ; i++ )//loop to check promotions in the sorted list by kills
		{
			shipSettings e = listShipSettings.get(i);
			
			if(i == listShipSettings.size() - 1)
			{
				if(kills >= e.getKill()) //this is to the last ship promotion(the last limit of kills)
					if(!m_botAction.getPlayer(name).isShip(e.getShip()))
					{
						m_botAction.setShip(name, e.getShip());
						m_botAction.sendArenaMessage(name+" is owning! He just got promoted to "+e.getShipName()+"!", 7);
					// REMOVING the *arena messages (if Nfer kills, it'll N *arena promotions)
					}
			}
			
		 else if(kills >= e.getKill() && kills < listShipSettings.get(i+1).getKill())//to any other promotion(needs 2 limits of kills)
				if(!m_botAction.getPlayer(name).isShip(e.getShip()))
				{
					m_botAction.setShip(name, e.getShip());
					m_botAction.sendArenaMessage(name+" got promoted to "+e.getShipName()+"!", 21);
				}
		}
	}
	
	public void setPromotion( String name, String message )
    {
        if(!isPromotion())
        {
            setPromotion(true);
            m_botAction.sendArenaMessage("Yes! Ship Promotions ON!", 24);
        }
        
        String messageSplit [] = message.split(":");
        
        shipSettings ss = new shipSettings();
        
        ss.setShip( Integer.parseInt(messageSplit[0].substring(6)) );
        ss.setKill( Integer.parseInt(messageSplit[1]) );
        
        listShipSettings.add(ss);
        m_botAction.sendPrivateMessage(name, "Added "+ss.getShipName()+" at "+ss.getKill()+" kills. Use !lprom to check");
        Collections.sort(listShipSettings);     
    }   
	
    public void listPromotion(String name, String message)
    {
        m_botAction.sendPrivateMessage(name,"------------------------------------------");
        for(shipSettings e:listShipSettings)
            m_botAction.sendPrivateMessage(name, "| be a "+e.getShipName()+ " with more than "+e.getKill()+" kills");
        
        m_botAction.sendPrivateMessage(name,"------------------------------------------");  
    }
    
    public void removePromotion( String name, String message)
    {
        //!removep 1
        Integer removeP = Integer.parseInt( message.substring(9) );
        
        for( shipSettings e:listShipSettings)
            if(e.getShip() == removeP)
            {
                listShipSettings.remove(e);
                break;
            }
     }
    
	public void cmd_firstShip(String name, String message)
	{
	    //!ship #
	    if(message.length() > 5)
	    {
	        int ship = Integer.parseInt( message.substring(6) );
    	    setFirstShip( ship );
    	    m_botAction.changeAllShips( getFirstShip() );
    	    m_botAction.scoreResetAll();
    	    m_botAction.shipResetAll();
    	    m_botAction.sendPrivateMessage(name, "Everyone will start on the ship "+ship+" now. If you want to enable proms, do !prom #ship:#kills");
	    } else{
	        m_botAction.sendPrivateMessage(name, "Please use !ship #shipnumber to set a starter.");
	        return ;
	    }
	        
	}
	
    @Override
    public void cmd_addTerr(String name, String message) {
      //!terr name
        Player terr = null;
        
        if(message.length() < 5){
            m_botAction.sendPrivateMessage(name, "Please use !terr <name> to set a terr.");
            return;
        }
        
        terr = m_botAction.getFuzzyPlayer( message.substring(6) );
    
        if( terr == null)
        {
            m_botAction.sendPrivateMessage(name, message.substring(6)+" is not in arena");
            return ;
        }
        
        putTerr(name, message, terr);
        
    }

    @Override
    public void cmd_switchTerr(String name, String message) {
        //!switch name1:name2
        Player p1 = null;
        Player p2 = null;
        
        if(!message.contains(":"))
            return ;
        
        p1 = m_botAction.getFuzzyPlayer( getOldTerr( message.split(":")) );
        p2 = m_botAction.getFuzzyPlayer( getNewTerr( message.split(":")) );
        
        if( p1 == null )
        {
            m_botAction.sendPrivateMessage(name, getOldTerr( message.split(":"))+" is not here");
            return ;
        }
        if( p2 == null )
        {
            m_botAction.sendPrivateMessage(name, getNewTerr( message.split(":"))+" is not here");
            return ;
        }
        if( !p1.isShip(5)){
            m_botAction.sendPrivateMessage(name, p1.getPlayerName()+
                    " is not a terr. Switch just a terr please.");
            return ;
        }
        if(p2.isShip(5)){
            m_botAction.sendPrivateMessage(name, p1.getPlayerName()+ " is a terr already. Switch just to set a new terr please.");
            return ;
        }
        
        if(p1.getFrequency() != p2.getFrequency()){
            m_botAction.sendPrivateMessage(name, "Can't switch players from different freqs. Switch players from the same team please");
            return ;
        }
        //ok..the player might be lagged out(work on it still)
        switchTerrs(name, p1, p2);
    }
    
	public void cmd_warp(String name, String message)
    {
        if(isWarpOn ()) setWarpOn(false);
        else{
            setWarpOn(true);
            m_botAction.sendPrivateMessage(name, "Allright! any players dettaching will be warped!");
    
        }
    }
    @Override
    public void cmd_stop(String name, String message) {
        setPromotion(false);
        setWarpOn(false);
    
        listShipSettings.clear();
        m_botAction.cancelTasks();
        m_botAction.sendPrivateMessage(name, "Game stopped. Promotions disabled.");
    
    }
  
	public void cancel() {}

	public String[] getModHelpMessage() 
	{
		String erHelp [] = {
			"| --------------------------------------------------------------------------------- |",
			"|	!guide                        - A GUIDE TO HOST TURRETWAR                         |",
			"| !ship number                  - to set everyone in this ship                      |",																				
			"|	!terr name                    - to pick a terr                                    |",
			"|	!switch name:terr             - to put :terr as a terr / switch terrs                                   |",
			"| ------------------ Promotion ---------------------------------------------------  |",
			"| !prom ship:kill               - to enable promotion                               |",	
			"| !lprom                        - to check the list of promotions                   |",
			"| !removep ship                 - removes the promotion of this #ship               |",
			"| --------------------------------------------------------------------------------  |",
			"|	!start                        - to start game (*scorereset, *shipreset and *lock) |",
			"|	!warp                         - to enable warpback                                |",
			"| --- Warping to safes ----                                                         |",
			"| !setupwarplist                - to see warp safes list                            |",
			"| --------------------------------------------------------------------------------- |"
		};
		return erHelp;
	}
	
	//showing Guide to ER+s
	public void showGuide(String name, String message){
		m_botAction.privateMessageSpam(name, guideER);
	}
		
	public void init() {
	}

	public boolean isUnloadable() {
		return true;
	}

	public void requestEvents(ModuleEventRequester eventRequester) {
		eventRequester.request(this, EventRequester.PLAYER_DEATH);
	}
    
    public void putTerr(String name, String message, Player terr)
    {
        m_botAction.setShip(terr.getPlayerID(), 5);
        m_botAction.scoreReset(terr.getPlayerID());
        m_botAction.shipReset(terr.getPlayerID());
        m_botAction.sendArenaMessage(terr.getPlayerName()+" is your terr!");
        m_botAction.sendPrivateMessage(name, terr.getPlayerName()+" has been added as terr successfuly into freq #" +
        		+terr.getFrequency()+".");
    }
  
    public void switchTerrs(String name, Player p1, Player p2){
        
        m_botAction.setShip     (p2.getPlayerID(), 5);
        m_botAction.scoreReset  (p2.getPlayerID());
        m_botAction.shipReset   (p2.getPlayerID());
        
        m_botAction.setShip     (p1.getPlayerID(), getFirstShip() );
        
        m_botAction.sendArenaMessage(p1.getPlayerName()+" (terr) switched with "+p2.getPlayerName());
        m_botAction.sendPrivateMessage( name, "You've switched "+p1.getPlayerName()+
                " with "+p2.getPlayerName());
        m_botAction.sendPrivateMessage(name, "now "+p2.getPlayerName()+
                " is your terr!");
    }
    
    //------------------------- Getters setters --------------------------------
    public String getOldTerr(String split []){
        return split[0].substring(8);
    }
    public String getNewTerr(String split[]){
        return split[1];
    }
    
    public int getFreq( String player )
    {
        return m_botAction.getPlayer(player).getFrequency();
    }

    public boolean isWarpOn() {
        return warpOn;
    }
    public void setWarpOn(boolean warpOn) {
        this.warpOn = warpOn;
    }
    public int getFirstShip(){
        return firstShip;
    }
    public void setFirstShip(int firstShip) {
        this.firstShip = firstShip;
    }
    public boolean isPromotion() {
        return promotion;
    }
    public void setPromotion(boolean promotion) {
        this.promotion = promotion;
    }

    public boolean isAlreadyPromoted() {
        return isAlreadyPromoted;
    }

    public void setAlreadyPromoted(boolean isAlreadyPromoted) {
        this.isAlreadyPromoted = isAlreadyPromoted;
    }
    //-----------------------------------------------
}