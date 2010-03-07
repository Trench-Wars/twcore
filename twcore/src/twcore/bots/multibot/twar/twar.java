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

/*--------------- Bot to any TURRETWARs event ------
	. it has !start command which uses *scorereset, *shipreset and *lock 's commands before start.
	. each ER+ has to MANUALLY *arena GO GO GO after the timertask(10 seconds)
	. command to set everyone as a ship is !ship (old !setship)
	. command to set terr is !terr name
	. command to switch terr is !switch name:newTerr
	. command to warpback is !warp, so anyone who detaches terr will get warped and energy depleted.
	. command to ships promotion is !prom ship:kills
	. command to stop game is !stop, so after game ends, it stops the game with 'turretWar = false;' and others booleans too.
	. command to ER+ guide to use this Bot to host is !guide
	. commands of others utilities/bots are OTHERS: !setupwarplist and !setupwarp to set safes warping before game starts and !setship #ship 
	if not using !prom, because you'll set #ship by your own with the old !setship command
	
	. any player can type !lprom to bot so he'll get back the ship promotions list
	--------------- Turret War ----------------------
*/
public class twar
extends MultiModule
implements turretwar
{
    private    boolean     promotion   = false;
	private    boolean     turretWar   = false;
	private    boolean     warpOn      = false;
	/*	private    boolean     freq1Terr   = false;
	private    boolean     freq0Terr   = false;
	*/
	private    int         firstShip   = 0;

	private    ArrayList<shipSettings> listShipSettings = new ArrayList() ;
	
	//guide to ER+s
	private String guideER[] = 
	{
			"1.  Use !terr name to pick terr",
			"2.  Use !switch name:newTerr to switch terrs",
			"3.  Use !prom <#ship>:<#kills> to set a promotion",
			"4.  Use !removep <#ship> to remove a ship promotion",
			"5.  Use !ship <#number> to set everyone in this ship",
			"6.  Use !start to: scorereset, shipreset and lock",
			"7.  Use !warp to make detachers get warped to spawn area - old !warpon command ",
			"8.  do manually the *arena GO GO GO and *timer <#Minutes>",
			"9.  Use !ship <#ship> to set the starter ship (using promotions or not)",
			"10.  Use !setupwarplist to check the warp command",
			"11.  When you use !terr name at first time, it'll automatically !random 2, so don't worry about the old !random 2",
			"12. !prom ship:kills is AUTO-SORTED by KILLS in the LIST !lprom",
			"13. To stop the game use !stop, use it AFTER the game ends too, so you can !unload this module."
	
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
				if(killer.getShipType() != 5)
					handleCustomPromotion(killer.getPlayerName(), (int) killer.getWins());				
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
		try
		{
			if(message.toLowerCase().startsWith("!start"))           cmd_start(name, message);
			else if(message.toLowerCase().startsWith("!ship"))       cmd_firstShip(name, message);
			else if(message.toLowerCase().startsWith("!terr"))       cmd_addTerr(name, message);
			else if(message.toLowerCase().startsWith("!switch"))     cmd_switchTerr(name, message);
			else if(message.toLowerCase().startsWith("!warp"))       cmd_warp(name, message);
			else if(message.toLowerCase().startsWith("!stop"))       cmd_stop(name, message);
            
			else if(message.toLowerCase().startsWith("!prom"))       setPromotion(name, message);
			else if(message.toLowerCase().startsWith("!lprom"))      listPromotion(name, message);
			else if(message.toLowerCase().startsWith("!removep"))    removePromotion(name, message);
			
			else if(message.toLowerCase().startsWith("!guide"))      showGuide(name, message);
			
		}catch(Exception e){}
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
					//	m_botAction.sendArenaMessage(name+" is owning! He just got promoted to "+e.getShipName()+"!", 7);
					// REMOVING the *arena messages (if Nfer kills, it'll N *arena promotions)
					}
			}
			
		 else if(kills >= e.getKill() && kills < listShipSettings.get(i+1).getKill())//to any other promotion(needs 2 limits of kills)
				if(!m_botAction.getPlayer(name).isShip(e.getShip()))
				{
					m_botAction.setShip(name, e.getShip());
				//	m_botAction.sendArenaMessage(name+" got promoted to "+e.getShipName()+"!", 21);
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
        
        shipSettings object = new shipSettings();
        
        object.setShip( Integer.parseInt(messageSplit[0].substring(6)) );
        object.setKill( Integer.parseInt(messageSplit[1]) );
        
        listShipSettings.add(object);
        Collections.sort(listShipSettings);     
    }   
	
    public void listPromotion(String name, String message)
    {
        m_botAction.sendPrivateMessage(name,"------------------------------------------");
        for(shipSettings e:listShipSettings)
            m_botAction.sendPrivateMessage(name, "| be a "+e.getShipName()+ " with "+e.getKill()+" kills");
        
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
    
    @Override
    public void cmd_start(String name, String message) {
        if(!isTurretWar())
        {
            m_botAction.scoreResetAll();
            m_botAction.shipResetAll();
            m_botAction.resetFlagGame();
            setTurretWar(true);
        }
        else m_botAction.sendPrivateMessage(name, "Game has already started, to stop type ::!stop");
    }

	public void cmd_firstShip(String name, String message)
	{
	    //!ship #
	    if(message.length() > 5)
	    {
    	    setFirstShip( Integer.parseInt( message.substring(6) ) );
    	    m_botAction.changeAllShips( getFirstShip() );
    	    m_botAction.scoreResetAll();
    	    m_botAction.shipResetAll();
	    }
	}
	
    @Override
    public void cmd_addTerr(String name, String message) {
      //!terr name
        Player terr = null;
        
        if(message.length() > 5)
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
        
        //ok..the player might be lagged out(work on it still)
        switchTerrs(p1, p2);
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
        setTurretWar(false);
        setPromotion(false);
        setWarpOn(false);
    //  freq1Terr = false;
    //  freq0Terr = false;
        listShipSettings.clear();
        m_botAction.cancelTasks();
        m_botAction.sendPrivateMessage(name, "Game stopped. Now you can make other round by !start");
        m_botAction.sendPrivateMessage(name, "or just !unload twar");
    
    }
    @Override
    public void cmd_removeTerr() {
        // TODO Auto-generated method stub
        
    }
     
	/*public void handleEvent(PlayerLeft event){
	    Player p_left = m_botAction.getPlayer( event.getPlayerID() );
	    TWARPlayer p = new TWARPlayer();
	    
	    if(!p_left.isShip(0))
	    {
	        if(team[p_left.getFrequency()].)
	    }
	}
	 */
		
	public void cancel() {}

	public String[] getModHelpMessage() 
	{
		String erHelp [] = {
			"| --------------------------------------------------------------------------------- |",
			"|	!guide                        - A GUIDE TO HOST TURRETWAR                         |",
			"| !ship number                  - to set everyone in this ship                      |",																				
			"|	!terr name                    - to pick a terr                                    |",
			"|	!switch name:terr             - to switch terrs                                   |",
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
		m_botAction.sendPrivateMessage(name, guideER[0]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[1]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[2]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[3]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[4]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[5]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[6]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[7]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[8]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[9]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[10]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[11]);
		m_botAction.sendPrivateMessage(name, " ");
		m_botAction.sendPrivateMessage(name, guideER[12]);
	}
		
	public void init() {
	}

	public boolean isUnloadable() {
		return !turretWar;
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
        
    }
  
    public void switchTerrs(Player p1, Player p2){
        
        m_botAction.setShip     (p2.getPlayerID(), 5);
        m_botAction.scoreReset  (p2.getPlayerID());
        m_botAction.shipReset   (p2.getPlayerID());
        m_botAction.setShip     (p1.getPlayerID(), getFirstShip() );
        
        m_botAction.sendArenaMessage(p1.getPlayerName()+" (terr) switched with "+p2.getPlayerName());
    }
    
    //getters setters
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
    public boolean isTurretWar() {
        return turretWar;
    }
    public void setTurretWar(boolean turretWar) {
        this.turretWar = turretWar;
    }
    /*
    public boolean isFreq1Terr() {
        return freq1Terr;
    }
    public void setFreq1Terr(boolean freq1Terr) {
        this.freq1Terr = freq1Terr;
    }
    public boolean isFreq0Terr() {
        return freq0Terr;
    }
    public void setFreq0Terr(boolean freq0Terr) {
        this.freq0Terr = freq0Terr;
    }
    */
}
