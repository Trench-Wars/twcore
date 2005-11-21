package twcore.bots.twbot;

import twcore.bots.shared.TWBotExtension;
import twcore.core.Player;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;

/** runs RaceElim
 * @author Jacen Solo
 * @version 1.0
 */
public class twbotraceelim extends TWBotExtension
{
	boolean isRunning = false;
	int requiredWins;
	int requiredLosses;
	
	public twbotraceelim()
	{
	}
	
	/** checks if the person has the required ammounts of kills or deaths for the win
	 */
	public void handleEvent(PlayerDeath event)
	{
		if(isRunning)
		{
			Player killer = m_botAction.getPlayer(event.getKillerID());
			Player killee = m_botAction.getPlayer(event.getKilleeID());
			if(killer.getWins() >= requiredWins)
				handleWin(m_botAction.getPlayerName(event.getKillerID()), "getting " + requiredWins + " kills");
			if(killee.getLosses() >= requiredLosses && requiredLosses > 0)
				handleWin(m_botAction.getPlayerName(event.getKilleeID()), "dying " + requiredLosses + " times");
		}
	}
	
	/** handles a message event and calls handleCommand method if they are ER+ and it's a pm
	 */
	public void handleEvent(Message event)
	{
		String message = event.getMessage();
        if(event.getMessageType() == Message.PRIVATE_MESSAGE)
        {
            String name = m_botAction.getPlayerName(event.getPlayerID());
            if(m_opList.isER(name))
            	handleCommand(name, message);
        }
    }
    
    /** handles a command from the message event
     * @param name is the name of the person
     * @param is the person's message
     */
    public void handleCommand(String name, String message)
    {
    	if(message.toLowerCase().startsWith("!start"))
    	{
    		String pieces[] = message.split(" ");
    		int rW = 10;
    		int rL = 10;
    		try{
    			rW = Integer.parseInt(pieces[1]);
    			rL = Integer.parseInt(pieces[2]);
    		}catch(Exception e) {}
    		requiredWins = rW;
    		requiredLosses = rL;
    		isRunning = true;
    		m_botAction.toggleLocked();
    		m_botAction.sendUnfilteredPublicMessage("*scorereset");
    		m_botAction.createRandomTeams(1);
    		m_botAction.sendPrivateMessage(name, "This arena has been autolocked for the game, please *lock it when you are done or between matches or if you !stop the game");
            if( rL == 0 )
    			m_botAction.sendArenaMessage(name + " has started RaceElim.  You win at " + requiredWins + " kills.");
		else
    			m_botAction.sendArenaMessage(name + " has started RaceElim.  You win at " + requiredWins + " kills or " + requiredLosses + " deaths.");

    	}
    	if(message.toLowerCase().startsWith("!stop"))
    	{
    		isRunning = false;
    		m_botAction.sendArenaMessage(name + " has stopped RaceElim.");
    	}
    }
    
    /** handles the win
     * @param name is the person that won
     * @param how is how they won
     */
    public void handleWin(String name, String how)
    {
    	isRunning = false;
    	m_botAction.sendArenaMessage(name + " has won the game by " + how + ".");
    }
    
    /** returns the help message
     */
    public String[] getHelpMessages()
    {
        String[] RaceElimHelp = {
            "!start <x> 0                       - Starts race elim w/ x wins required",
            "!start <x> <y>                     - Starts race elim w/ x wins required or y losses required",
            "!stop                              - Stops race elim"
        };
        return RaceElimHelp;
    }
    
    /** required thing
     */
    public void cancel()
    {
    }
}