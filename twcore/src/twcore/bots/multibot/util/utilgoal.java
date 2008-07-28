package twcore.bots.multibot.util;

import twcore.bots.MultiUtil;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;
import twcore.core.events.Message;

/**
 * Allows some aesthetic additions for soccer goals
 * on the ball scorer.
 * 
 * Prize code borrowed from utilprizes.
 * @author Ayano
 *
 */

public class utilgoal extends MultiUtil {
	
	private static final int[] RESTRICTED_PRIZES = { Tools.Prize.RECHARGE,
        Tools.Prize.ENERGY,
        Tools.Prize.ROTATION,
        Tools.Prize.BOUNCING_BULLETS,
        Tools.Prize.THRUST,
        Tools.Prize.TOPSPEED,
        Tools.Prize.PROXIMITY };
	
	private static final int MAX_PRIZE = 28;
	private static final int MIN_PRIZE = 1;
	
	
	private String goalmessage;
	private int[] goalwarp = null;
	private int goalobjon = -1,
				goalprize = 0,
				goalsound = 0;
	
	 public void requestEvents( ModuleEventRequester modEventReq ) {
	 }
	 
	 public void handleEvent(Message event) {
		 String playerName = m_botAction.getPlayerName(event.getPlayerID());
		 String message = event.getMessage();
			if(event.getMessageType() == Message.PRIVATE_MESSAGE 
					&& m_opList.isER(playerName))
				handleCommand(playerName, message.toLowerCase());
			else if(event.getMessageType() == Message.ARENA_MESSAGE)
				handleArenaMessage(message);
	 	}
	 
	 public void handleArenaMessage(String message)	{
		 String goalscorer;
		 m_botAction.sendPublicMessage("I got an arena message! : -" + message + "-");
		 if (message.startsWith("Enemy Goal!") || message.startsWith("Team Goal!"))	{
			 goalscorer = getScorer(message);
			 
			 if (goalmessage != null)	{
				 	if (goalsound !=0)
				 		m_botAction.sendPrivateMessage(
							 goalscorer, goalmessage, goalsound);
				 	else
				 		m_botAction.sendPrivateMessage(goalscorer, goalmessage);
			 }	if (goalwarp != null)	{
				 	if (goalwarp.length > 2)
				 		m_botAction.warpTo(
				 				goalscorer, goalwarp[0], goalwarp[1], goalwarp[2]);
				 	else
				 		m_botAction.warpTo(goalscorer, goalwarp[0], goalwarp[1]);
			 }	if (goalobjon != -1)	{
				 m_botAction.sendUnfilteredPrivateMessage(
						 goalscorer, "*objon " + goalobjon);
			 }	if (goalprize != 0)
				 m_botAction.sendUnfilteredPrivateMessage(
						 goalscorer, "*prize #" + goalprize);
		 }
	 }
	 
	 /**
	   * Checks to see if a given prize number is restricted from use.
	   * @param prizeNum Prize to check
	   * @return True if prize is not allowed
	   */
	  public boolean isRestricted(int prizeNum) {
		  for(int i = 0; i < RESTRICTED_PRIZES.length; i++ )
			  if( RESTRICTED_PRIZES[i] == prizeNum )
				  return true;
		  return false;
	  }
	  
	  public String getScorer(String argString)	{
		  String scorer = argString.substring(argString.indexOf("by")+3);
		  if (scorer.indexOf("Reward") != -1)
			  scorer = scorer.substring(0,scorer.indexOf("Reward")-2);
		  return scorer;
		  
	  }
	 
	 public void doGoalMessage(String sender, String argString)	{
		 if(argString.equals("~*"))	{
			 goalmessage = null;
			 goalsound = 0;
			 m_botAction.sendPrivateMessage(sender, "Goal message has been erased.");
		 }	else	{
			 String[] parts = argString.split(":");
			 if (parts.length > 2)
				 throw new IllegalArgumentException(
						 "Invalid syntax, use: <message>:<#>");
			 if (parts.length == 2)	{
				 goalsound = Integer.parseInt(parts[1]);
				 goalmessage = parts[0];
			 } else	{
				 goalsound = 0;
				 goalmessage = argString;
			 }
			 
			 m_botAction.sendPrivateMessage(sender, 
					 "Goal message is now : \"" + goalmessage + 
					 (goalsound !=0 ? "\" %" + goalsound : "\""));
		 }
	 }
	 
	 public void doGoalWarp(String sender, String argString) {
		 if (argString.equals("~*"))	{
			 goalwarp = null;
			 m_botAction.sendPrivateMessage(sender, "Goal warp point has been erased.");
		 }	else	{
			 String[] parts = argString.split(":");
			 
			 if (parts.length < 2 || parts.length > 3)
				 throw new IllegalArgumentException(
						 "Invalid syntax, use: <xi>:<yi>");
			int coord[] = new int[parts.length];
			for (int i = 0; i < parts.length; i++)
                coord[i] = Integer.parseInt(parts[i]);
			//redundant array copy
			goalwarp = new int[coord.length];
			for (int i = 0; i < coord.length; i++)
                goalwarp[i] = coord[i];
			m_botAction.sendPrivateMessage(sender, "Goal warp point set");
		 }
	 }
	 
	 public void doGoalObjon(String sender, String argString)	{
		 if ( argString.equals("~*") )	{
			 m_botAction.sendPrivateMessage(sender, "Goal objon erased.");
			 goalobjon = -1;
		 }	else	{
			 goalobjon = Integer.parseInt(argString);
			 if (goalobjon < 0)	{
				 goalobjon = -1;
				 throw new IllegalArgumentException("Objon's can't be negative.");
			 }	m_botAction.sendPrivateMessage(sender, "Goal objon is now: \"" + 
					 goalobjon + "\"");
		 	}
	 }
	 
	 public void doGoalPrize(String sender, String argString)	{
		 if ( argString.equals("~*") )	{
			 m_botAction.sendPrivateMessage(sender, "Goal prize erased.");
			 goalprize = 0;
		 } else	{
			 int negativePrizing = 1;

			 if(argString.startsWith("-"))	{
			      negativePrizing = -1;
			      argString = argString.substring(1).trim();
			    }
			 
			 int prizeNumber = Integer.parseInt(argString);
			 
			 if( Math.abs(prizeNumber) > MAX_PRIZE || 
					 Math.abs(prizeNumber) < MIN_PRIZE )
			      throw new IllegalArgumentException("That prize does not exist.");
			 if ( isRestricted(prizeNumber) )
				 throw new IllegalArgumentException("That prize is restricted!");
			 
			 goalprize = prizeNumber * negativePrizing;
			 m_botAction.sendPrivateMessage(sender, "Goal prize is now \""
					 + goalprize + "\"");
		 }
	 }
	 
	 public void doGoalDetails(String sender)	{
		 m_botAction.sendPrivateMessage(sender, "Goal message: \"" 
				 + goalmessage + "\" " + (goalsound == 0 ? "" : " %" + goalsound));
		 m_botAction.sendPrivateMessage(sender, "Goal warp point: " 
				 + (goalwarp != null ? goalwarp[0] + ":" + goalwarp[1] 
				 + (goalwarp.length == 3 ? " Radius: " + goalwarp[2]: "") : "none"));
		 m_botAction.sendPrivateMessage(sender, "Goal objon: " 
				 + (goalobjon ==-1 ? "none" : goalobjon));
		 m_botAction.sendPrivateMessage(sender, "Goal prize: " + 
				 (goalprize == 0 ? "none" : goalprize));
	 }
	 
	 public void handleCommand(String sender,String message)	{
		 try	{
			 if (message.startsWith("!goalmessage"))
				 doGoalMessage(sender,message.substring(13));
			 else if (message.startsWith("!goalwarp"))
				 	doGoalWarp(sender,message.substring(10));
			 else if (message.startsWith("!goalobjon"))
				 	doGoalObjon(sender,message.substring(11));
			 else if (message.startsWith("!goalprize"))
				 	doGoalPrize(sender,message.substring(11));
			 else if (message.startsWith("!goaldetails"))
			 		doGoalDetails(sender);
		 }	catch (NumberFormatException nfe)	{
			 	m_botAction.sendPrivateMessage(sender, 
			 			"syntax must be numerical.");
		 }	catch (IllegalArgumentException iae)	{
			 	m_botAction.sendPrivateMessage(sender, iae.getMessage());
		 }
	 }
	 
	 public void init() {
	    }
	 
	 
	 public String[] getHelpMessages() {
	        String[] helps = {
	        		"=GOAL=====================================================GOAL=",
	    			"----------This utility sends messages,objons,warps or----------",
	    			"-------------prizes to a player who scores a goal -------------",
	    			"!goalmessage <message>:<#>            -- Sets the message      ",
	    			"!goalwarp    <x>:<y>:<r>   <r>optional-- Sets the warp point   ",
	    			"!goalobjon   <objon#>                 -- Sets the objon        ",
	    			"!goalprize   <prize#>                 -- Sets the prize        ",
	    			"!goaldetails                          -- Shows the details     ",
	    			"++                                                           ++",
	    			"Note: sending \" ~* \" rather than the syntax erases the setting",
	    			"=GOAL=====================================================GOAL="
	        };
	        return helps;
	    }
}


