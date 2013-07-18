//Make the package name the same as the bot's name so you can spawn it
package twcore.bots.baseduel;
//Import all of the TWCore classes so you can use them
import twcore.core.*;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.ArenaJoined;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.events.PlayerPosition;
import twcore.core.events.Prize;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;

import twcore.core.util.Tools;
import java.util.Iterator;
import java.util.TimerTask;

public class baseduel extends SubspaceBot
{   
    //Requests all of the events from the core
    private EventRequester events;
    //Handles all commands sent to bot by players
    private CommandInterpreter cmds;
    //Stores Staff Access Levels
    private OperatorList oplist;
    private Player player;
    boolean game = false;
    // Scores     F0, F1, Win Point
    int[] score = {0, 0, 5};
    String currentscore = "0 - 0 Tied!";
    private Countdown countdown;
    //               |F0X||F1X|
    int[] currentx = {157, 280};
    //               |F0Y||F1Y|
    int[] currenty = {202, 286};
    char xCoord;
    int yCoord;
    int coordTime = 20 * Tools.TimeInMillis.SECOND;
    int x;
    int y;
    //Creates a new mybot
    public baseduel(BotAction botAction)
    {
        //This instantiates your BotAction
        super(botAction);
        
        //Player
        
        //Instantiate your EventRequester
        events = m_botAction.getEventRequester();
        events.request(EventRequester.PLAYER_DEATH);
        events.request(EventRequester.PLAYER_POSITION);
        //Request PlayerEntered events
        events.request(EventRequester.PLAYER_ENTERED);
        //Request chat message events
        events.request(EventRequester.MESSAGE);
        //Instantiate your CommandInterpreter
        cmds = new CommandInterpreter(m_botAction);
        //Instantiate your Operator List
        oplist = m_botAction.getOperatorList();
        //Set up your interpreter
        addCommands();
    }

    //What to do when the bot logs on
    public void handleEvent(LoggedOn event)
    {
        //Get the data from mybot.cfg
        BotSettings config = m_botAction.getBotSettings();
        //Get the initial arena from config and enter it
        String initial = config.getString("InitialArena");
        m_botAction.joinArena(initial);
        //NOTE: m_botAction is inherited from SubspaceBot
    }

    //What to do when a player enters the arena
    public void handleEvent(PlayerEntered event)
    {
        //Get the name of player that just entered
        String name = event.getPlayerName();
        //Greet them
        if (game == true)
        {
        m_botAction.sendPrivateMessage(name,"Welcome to baseduel arena we are playing baseduel Freq 0 vs Freq 1 Scores: "+currentscore+"!");
        }
        else
        {
        	m_botAction.sendPrivateMessage(name,"Welcome to baseduel arena ::!help to check baseduel commands!");
        }
    }
    public void handleEvent(PlayerDeath event) {
    	if (game == true)
    	{
        String killer = m_botAction.getPlayerName(event.getKillerID());
        String killee = m_botAction.getPlayerName(event.getKilleeID());
        if (killer != null && killee != null) {
        addstats(killer, 1, 0);
        addstats(killee, 0, 1);
        }
        }
    }
  /*  public void handleEvent(PlayerPosition event) {
    	        boolean isSafe = false;
    	        int freq = -1;
    	        int xLoc = -1;
    	        int yLoc = -1;
    	
    		        Player p;
    	        String playerName;
    	
    	        isSafe = event.isInSafe();
    	        xLoc = event.getXLocation() / 16; //theses thing return coords in pixels. map is 16*1024 pixels.
    		        yLoc = event.getYLocation() / 16; //so divide by 16 to get regular coords!
    	
    	        p = m_botAction.getPlayer(event.getPlayerID());
    	
    	        if (p == null)
    	           return;
    		
    	        freq = p.getFrequency();
    		        playerName = m_botAction.getPlayerName(p.getPlayerID());
    	        if (playerName == null)
    		            return;
    	
    	        if (game) {
    	               if (isSafe && xLoc > 512 && xLoc < 215 && yLoc > 530 && yLoc < 250) {
    	                  m_botAction.sendPrivateMessage(playerName, "*warpto 512 512");
    	                }
 
    		        }
    		    }*/
    //What to do when somebody says something
    public void handleEvent(Message event)
    {
        //Pass it to the interpreter
        cmds.handleEvent(event);
    }

    //Set up commands
    public void addCommands()
    {
        //Allowed message types for commands
        int ok = Message.PRIVATE_MESSAGE | Message.PUBLIC_MESSAGE;
        //Add any commands as you see fit
        cmds.registerCommand("!help",ok,this,"help");
        cmds.registerCommand("!die",ok,this,"die");
        cmds.registerCommand("!go",ok,this,"go");
        cmds.registerCommand("!score",ok,this,"score");
        cmds.registerCommand("!ssc",ok,this,"ssc");
        cmds.registerCommand("!start",ok,this,"start");
        cmds.registerCommand("!stats",ok,this,"stats");
        cmds.registerCommand("!stop",ok,this,"stop");
        cmds.registerCommand("!getx",ok,this,"getXplease");
    }
    public void getXplease(String name, String msg)
    {
    	
       m_botAction.sendPrivateMessage(name, "X:"+getCoordsX(m_botAction.getPlayer(name))+" Y:"+getCoordsY(m_botAction.getPlayer(name)));
    }
    //Got command: !help
    public int getCoordsX(Player p) {
    	 // method that will generate in-game coordinates (A1, T20, etc)
    	      m_botAction.spectatePlayer(p.getPlayerName());
    	      x = p.getXTileLocation();
    	      return x;
    }
    public int getCoordsY(Player p) {
	      m_botAction.spectatePlayer(p.getPlayerName());
	      y = p.getYTileLocation();
	      return y;
    }
    public void start(String name, String msg)
    {
        if(oplist.isER(name))
        {
    	if (game == true)
    	{
    	m_botAction.sendPrivateMessage(name, "Game is Up now!");
    	}
    	else
    	{
                int seconds = 10;

                if(countdown != null)
                    countdown.cancel();

                countdown = new Countdown(seconds);
                m_botAction.scheduleTaskAtFixedRate(countdown, 1000, 1000);
                m_botAction.sendArenaMessage("Starting baseduel in 10 seconds -"+name, 2);
        }
    	}
    }
    public void clearscores()
    {
    score[0] = 0;
    score[1] = 0;
    countdown.cancel();
    currentscore = "0 - 0 Tied!";
    }
    public void stop(String name, String msg)
    {
    	game = false;
    	score[0] = 0;
    	score[1] = 0;
    	currentscore = "0 - 0 Tied!";
    	countdown.cancel();
    	m_botAction.sendArenaMessage("Stopped Game -"+name, 1);
    	m_botAction.warpAllToLocation(512, 272);
    }
    public void ssc(String name, String msg)
    {
    	String[] val = msg.split(",");
        score[0] = Integer.valueOf(val[0]);
        score[1] = Integer.valueOf(val[1]);
			if (score[0] > score[1])
			{
			currentscore = score[0] + " - " + score[1] + " Lead Team 0";
			}
			else if (score[0] < score[1])
			{
				currentscore = score[1] + " - " + score[0] + " Lead Team 1";
			}
			else if (score[1] == score[0])
			{
				currentscore = score[1] + " - " + score[0] + " Tied!";
			}
    	m_botAction.sendArenaMessage("Set Scores to "+currentscore+" -"+name, 2);
    }
    public void score(String name, String msg)
    {
    	m_botAction.sendPrivateMessage(name, currentscore);
    }
    public void stats(String name, String msg)
    {
    	if (msg.length() == 0)
    	{
    	m_botAction.sendSmartPrivateMessage(name, checkstats(name));
    	}
    	else
    	{
    		m_botAction.sendSmartPrivateMessage(name, checkstats(msg));
    	}
    }
    public void help(String name, String msg)
    {
        //Help Message
        String[] help =
        {"+--------------------------------------------------------------+",
         "|                     B A S E D U E L           Auther:Ahmad~  |",
         "|  Player Commands:                                            |",
         "|  !stats          - Check Your stats or player stats          |",
         "|  !score          - Current Scores                            |"};
        String[] Ophelp = {"|  Staffs Commands:                                            |",
         "|  !start          - Start a game                              |",
         "|  !stop           - Stop/Reset Game                           |",
         "|  !ssc F0#,F1#    - Set Scores for freq 0, Freq 1: !ssc 3,1   |",
         "|  !die            - Kill Bot!                                 |",
         "|  !go             - Send Bot to any arena                     |"};
        String[] lastline = {"+--------------------------------------------------------------+"};
        m_botAction.privateMessageSpam(name, help);
        if(oplist.isER(name))
        {
        	m_botAction.privateMessageSpam(name, Ophelp);
        }
        m_botAction.privateMessageSpam(name, lastline);
    }

    //Got command: !die
    public void die(String name, String msg)
    {
        //Make sure player has clearance
        if(oplist.isER(name))
            //Destroy bot, allows another to be spawned
            m_botAction.die();
    }
    public void addstats(String name, int kills, int losses)
    {
        BotSettings config = m_botAction.getBotSettings();
        //Get the initial arena from config and enter it
        String kills2 = config.getString(name +"-kills");
        //Get the initial arena from config and enter it
        String losses2 = config.getString(name + "-losses");
        int addl = Integer.valueOf(losses2) + losses;
        config.put(name + "-losses", addl);
            addl = Integer.valueOf(kills2) + kills;
            config.put(name + "-kills", addl);
    }
    //Got command: !go
    public void go(String name, String msg)
    {
        //Make sure player has clearance
        if(oplist.isER(name))
            //Join arena specified by player
            m_botAction.changeArena(msg);
    }
        @SuppressWarnings("unused")
		public String checkstats(String name)
        {
            BotSettings config = m_botAction.getBotSettings();
            //Get the initial arena from config and enter it
            String kills = config.getString(name +"-kills");
            //Get the initial arena from config and enter it
            String losses = config.getString(name + "-losses");
        	String stats = "Player name:"+name+" Kills: "+kills+" Losses: "+losses;
        	if (kills == null)
        	{
        	   if (losses == null)
        	   {
        	   stats = "This player don't have any stats in baseduel";
        	   losses = "0";
        	   }
        	   kills = "0";
        	}
        	if (losses == null)
        	{
        		if (kills == null)
        		{
        			 stats = "This player don't have any stats in baseduel";
        			 kills = "0";
        		}
        		losses = "0";
        	}
            return stats;
        }
		public boolean closeto(Player p, int x, int y, int tolerance)
		{
			boolean check = false;
			for (int i = 0; i < tolerance; ++i)
			{
			   int x2 = x + i;
			   for (int l = 0; l < tolerance; ++l)
			   {
				   int y2 = y + l;
			   if (x2 == getCoordsX(p))
			   {
				   if (y2 == getCoordsY(p))
				   {
				   check = true;
				   }
			   }
			   }
			}
            return check;
		}
    public void cancel()
    {
        if(countdown != null)
            countdown.cancel();
    }
    private class Countdown extends TimerTask
    {
        /** The remaining time to count down */
        private int secondsLeft;

        /**
         * Creates a new instance of the Countdown task
         * @param seconds the number of seconds to count down
         */
        public Countdown(int seconds)
        {
            secondsLeft = seconds;
        }

        /**
         * Execute this task: Count down to 0
         */
        public void run()
        {
        	if (game == false){
            if (secondsLeft == 0)
            {
            	game = true;
            	m_botAction.warpFreqToLocation(0, 157, 202);
            	m_botAction.warpFreqToLocation(1, 280, 286);
            	
                currentx[1] = 280;
                currenty[1] = 286;
                currentx[0] = 157;
                currenty[0] = 202;
             	m_botAction.sendArenaMessage("Go Go Go Go Go!", 104);
            }
        	}
        	else if (game == true)
        	{
        		if (secondsLeft == secondsLeft)
        		{
				Iterator<Player> i = m_botAction.getPlayerIterator();
				while (i.hasNext()) {
				
					Player pl = i.next();
					if (!pl.getPlayerName().equals(m_botAction.getBotName())) {
						player = pl;
						//m_botAction.sendArenaMessage("Playername: "+m_botAction.getPlayer(player.getPlayerID()).getPlayerName()+" X:"+getCoordsX(player)+" Y:"+getCoordsY(player));
              	if (closeto(player, 512, 272, 10))
            	{
              	   if (player.getFrequency() == 1)
              	   {
              	   m_botAction.sendPrivateMessage(player.getPlayerName(), "*warpto "+currentx[1]+" "+currenty[1]);
              	   }
              	   else if (player.getFrequency() == 0)
              	   {
              		 m_botAction.sendPrivateMessage(player.getPlayerName(), "*warpto "+currentx[0]+" "+currenty[0]);
              	   }
            	}
              	if (player.isInSafe())
              	{
              		if (closeto(player, currentx[1], currenty[1], 6))
              		{
              			if (player.getFrequency() == 0)
              			{
              			m_botAction.warpAllToLocation(512, 272);
              			score[0] = score[0] + 1;
              		
              			game = false;
              			m_botAction.sendArenaMessage(player.getPlayerName() + " Touch Safe Freq 1");
              			if (score[0] > score[1])
              			{
              			currentscore = score[0] + " - " + score[1] + " Lead Team 0";
              			}
              			else if (score[0] < score[1])
              			{
              				currentscore = score[1] + " - " + score[0] + " Lead Team 1";
              			}
              			else if (score[1] == score[0])
              			{
              				currentscore = score[1] + " - " + score[0] + " Tied!";
              			}
              			m_botAction.sendArenaMessage("Scores: "+currentscore, 103);
              			if (score[0] != score[2])
              			{
                        int seconds = 10;

                        if(countdown != null)
                            countdown.cancel();

                        countdown = new Countdown(seconds);
                        m_botAction.scheduleTaskAtFixedRate(countdown, 1000, 1000);
                        m_botAction.sendArenaMessage("Starting next round in 10 seconds", 5);
              			}
              			if (score[0] == score[2])
              			{
              				m_botAction.sendArenaMessage("Game Over! Team 0 Wins!", 5);
              				clearscores();
              				countdown.cancel();
              			}
              			}
              		}
              		else if (closeto(player, currentx[0], currenty[0], 6))
              		{
              			if (player.getFrequency() == 1)
              			{
                  			m_botAction.warpAllToLocation(512, 272);
                  			score[1] = score[1] + 1;
                  		
                  			game = false;
                  			m_botAction.sendArenaMessage(player.getPlayerName() + " Touch Safe Freq 0");
                  			if (score[0] > score[1])
                  			{
                  			currentscore = score[0] + " - " + score[1] + " Lead Team 0";
                  			}
                  			else if (score[0] < score[1])
                  			{
                  				currentscore = score[1] + " - " + score[0] + " Lead Team 1";
                  			}
                  			else if (score[1] == score[0])
                  			{
                  				currentscore = score[1] + " - " + score[0] + " Tied!";
                  			}
                  			m_botAction.sendArenaMessage("Scores: "+currentscore, 103);
                            if (score[1] != score[2])
                            {
                  			int seconds = 10;

                            if(countdown != null)
                                countdown.cancel();

                            countdown = new Countdown(seconds);
                            m_botAction.scheduleTaskAtFixedRate(countdown, 1000, 1000);
                            m_botAction.sendArenaMessage("Starting next round in 10 seconds", 5);
                            }
                            else if (score[1] == score[2])
                  			{
                  				m_botAction.sendArenaMessage("Game Over! Team 1 Wins!", 5);
                  				countdown.cancel();
                  				clearscores();
                  			}
              			}
              		}
              	}
            }
        	}
				}
			}
            secondsLeft--;
        }
    }
}