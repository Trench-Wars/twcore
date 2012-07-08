package twcore.bots.tictactoe;
//Import all of the TWCore classes so you can use them 
import twcore.core.*;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;

public class tictactoe extends SubspaceBot
{
    //Creates a new mybot 
	
	String x1=" ", x2=" ", x3=" ",x4=" ",x5=" ",x6=" ",x7=" ",x8=" ", x9=" ";
	String turn = "No Turns :)";
	String namex = "No Player X!";
	String nameo = "No Player O!";
	String game_status = "false";
	String games_acpt = "false";
	String[] tic = new String[9];
	
	private EventRequester events;
    //Handles all commands sent to bot by players
    private CommandInterpreter cmds;
    //Stores Staff Access Levels
    private OperatorList oplist;
	private String message;
    
    public tictactoe(BotAction botAction)
    {
        //This instantiates your BotAction
        super(botAction);
        //Instantiate your EventRequester
        events = m_botAction.getEventRequester();
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
        this.message = config.getString("WelcomeMessage");

        //NOTE: m_botAction is inherited from SubspaceBot
    }
    //What to do when a player enters the arena
    public void handleEvent(PlayerEntered event)
    {
        //Get the name of player that just entered
        String name = event.getPlayerName();
        //Greet them
        m_botAction.sendPrivateMessage(name,message);
    }

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
        cmds.registerCommand("!zone",ok,this,"zone");
        cmds.registerCommand("!arena", ok, this,"arena");
        cmds.registerCommand("!rules", ok, this,"rules");
        cmds.registerCommand("!chngwelcome", ok, this, "welcome");
        cmds.registerCommand("!draw1:1", ok, this, "d11");
        cmds.registerCommand("!draw1:2", ok, this, "d12");
        cmds.registerCommand("!draw1:3", ok, this, "d13");
        cmds.registerCommand("!draw2:1", ok, this, "d21");
        cmds.registerCommand("!draw2:2", ok, this, "d22");
        cmds.registerCommand("!draw2:3", ok, this, "d23");
        cmds.registerCommand("!draw3:1", ok, this, "d31");
        cmds.registerCommand("!draw3:2", ok, this, "d32");
        cmds.registerCommand("!draw3:3", ok, this, "d33");
        cmds.registerCommand("!ticstatus", ok, this, "tics");
        cmds.registerCommand("!challenge", ok, this, "chlg");
        cmds.registerCommand("!accept", ok, this, "apt");
        cmds.registerCommand("!resetdraws", ok, this, "reset");
        cmds.registerCommand("!withdraw",ok,this,"wt");
        cmds.registerCommand("!pickx", ok, this, "pickx");
        cmds.registerCommand("!picko", ok, this, "picko");
        cmds.registerCommand("!endgame", ok, this, "endgame");
        cmds.registerCommand("!drawhelp", ok, this, "drawhelp");
        cmds.registerCommand("!say", ok, this, "say");
    }
    public void drawhelp(String name, String msg)
    {
    	m_botAction.sendSmartPrivateMessage(name, "   1   2   3");
    	m_botAction.sendSmartPrivateMessage(name, " +---+---+---+");
    	m_botAction.sendSmartPrivateMessage(name, "1|1:1|1:2|1:3|");
    	m_botAction.sendSmartPrivateMessage(name, " +---+---+---+");
    	m_botAction.sendSmartPrivateMessage(name, "2|2:1|2:2|2:3|");
    	m_botAction.sendSmartPrivateMessage(name, " +---+---+---+");
    	m_botAction.sendSmartPrivateMessage(name, "3|3:1|3:2|3:3|");
    	m_botAction.sendSmartPrivateMessage(name, " +---+---+---+");
    	m_botAction.sendSmartPrivateMessage(name, "!draw-:|");
    }
    public void wt(String name, String msg)
    {
    	if(name.equalsIgnoreCase(nameo) != true)
    		return;
    	if (games_acpt == "true")
    	{
    	     m_botAction.sendSmartPrivateMessage(namex,"You have Withdraw that challenge!");
    	     m_botAction.sendSmartPrivateMessage(name, nameo + " withdraw your challenge!");
    	     namex = "No Player X";
    	     nameo = "No Player O";
    	     games_acpt = "false";
    	     game_status = "false";
                 
    	}
        	else if (games_acpt == "false")
        	{
        		m_botAction.sendPrivateMessage(name, "Game is started with [X]" + namex + " Vs [O]" + nameo + "");
        	}
    }
    public void reset(String name, String msg)
    {
       if(x1 != " ")if(x2 != " ")if(x3 != " ")if(x4 != " ")if(x5 != " ")if(x6 != " ")if(x7 != " ")if(x8 != " ")if(x9 != " ")
       {
    	   x1=" "; x2=" "; x3=" ";x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
   		tic[0] = "+---+---+---+";
   		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
   		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
   		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
   		tic[4] = "| "+x7+" | "+x8+" | "+x9+" |";
   		m_botAction.sendArenaMessage(tic[0]);
   		m_botAction.sendArenaMessage(tic[1]);
   		m_botAction.sendArenaMessage(tic[0]);
   		m_botAction.sendArenaMessage(tic[2]);
   		m_botAction.sendArenaMessage(tic[0]);
   		m_botAction.sendArenaMessage(tic[3]);
   		m_botAction.sendArenaMessage(tic[0]);
   		m_botAction.sendArenaMessage("Turn : " + turn);
       }
       m_botAction.sendSmartPrivateMessage(name, "There Are Many Draws is Empty!");
    }
    public void chlg(String name, String msg)
    {
    	if (game_status == "false")
    	{
    		nameo = msg;
    		namex = name;
    	     m_botAction.sendSmartPrivateMessage(nameo, name + " Challenge you type !accept to accept challenge him!");
    	     m_botAction.sendSmartPrivateMessage(name, "You have send it to " + name +" wait him to accept that challenge!");
            games_acpt = "true";
    	}
    	else if (game_status == "true")
    	{
    		m_botAction.sendPrivateMessage(name, "Game is started with [X]" + namex + " Vs [O]" + nameo + "");
    	}
    }
    public void apt(String name, String msg)
    {
    	if(name.equalsIgnoreCase(nameo) != true)
    		return;
    	if (games_acpt == "true")
    	{
    	     m_botAction.sendSmartPrivateMessage(nameo,"You have accept that challenge!");
    	     m_botAction.sendSmartPrivateMessage(namex, nameo + " Accept your challenge!");
    		tic[0] = "+---+---+---+";
    		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
    		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
    		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
    		tic[4] = "| "+x7+" | "+x8+" | "+x9+" |";
    		m_botAction.sendArenaMessage(tic[0]);
    		m_botAction.sendArenaMessage(tic[1]);
    		m_botAction.sendArenaMessage(tic[0]);
    		m_botAction.sendArenaMessage(tic[2]);
    		m_botAction.sendArenaMessage(tic[0]);
    		m_botAction.sendArenaMessage(tic[3]);
    		m_botAction.sendArenaMessage(tic[0]);
    		turn = namex;
    		m_botAction.sendArenaMessage("Turn : " + turn);
    		game_status = "true";
    		games_acpt = "false";
    	}
        	else if (games_acpt == "false")
        	{
        		m_botAction.sendPrivateMessage(name, "Game is started with [X]" + namex + " Vs [O]" + nameo + "");
        	}
    }
    //Got command: !help
    public void rules(String name, String msg)
    {
        //Help Message
		if(oplist.isER(name))
		{
        String[] rule =
        {
        ".----------------------,",
        "|      Rules           |",
        "|----------------------+------,",
        "| Don't Cheat,Don't Hack      |",
        "| Don't Abuse,Don't Swearing  |",
        "`-----------------------------'"};   
        m_botAction.sendArenaMessage(rule[0]);
        m_botAction.sendArenaMessage(rule[1]);
        m_botAction.sendArenaMessage(rule[2]);
        m_botAction.sendArenaMessage(rule[3]);
        m_botAction.sendArenaMessage(rule[4]);
        m_botAction.sendArenaMessage(rule[5]);
       // m_botAction.sendArenaMessage(rule[6]);
        }
        //Send player the help message
    }
    public void help(String name, String msg)
    {
        //Help Message
    	
    		
        String[] help = 
        	{
        ".----------------------.",
        "| Help Commands        |",
        "+----------------------+-----------------.",
        "| !draw(-:|) - Draw X or O Example:      |",
        "| !draw2:2 in the mid                    |",
        "| !ticstatus - Game Status!              |",
        "| !drawhelp - Show you how to draw!      |",
        "| !challenge <Player> - Challenge for    |",
        "| player                                 |",
        "| !resetdraws - Reset Draws!             |",
        "`----------------------------------------'"};
        m_botAction.privateMessageSpam(name, help);
        if(oplist.isER(name))
        {
        String[] shelp =
        {
        "+----------------------+",
        "| Staff Commands   []  |",
        "+----------------------+--------------------.",
        "| !help            - this message           |",
        "| !die             - kills bot              |",
        "| !go (arena)      - sends bot to arena     |",
        "| !say (Message)   - Send Public Message    |",
        "| !zone (Message)  - Sends Zone Message     |",
        "| !arena (Message) - sends arena message    |",
        "| !rules           - show players rules     |",
        "| !chngwelcome     - Change Welcome Message |",
        "| !endgame         - End Tic-Tac-Toe Game   |",
        "| !pickx <player>  - Pick A Player [X]      |",
        "| !picko <player>  - Pick A player [O]      |",
        "+-------------------------------------------+",
        "| This Module Made By Red Desert=Ahmad~     |",
        "| Thanks to : Dral <ER>, SpookedOne!        |",
        "`-------------------------------------------'"};
        //Send player the help message
        m_botAction.privateMessageSpam(name,shelp);
        }
    }
    public void endgame(String name, String msg)
    {
    	if(oplist.isER(name))
    	{
    		x1=" "; x2=" "; x3=" ";x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
    		turn = "No Turns :)";
    		namex = "No Player X!";
    		nameo = "No Player O!";
    		game_status = "false";
    		games_acpt = "false";
    		tic[0] = "+---+---+---+";
    		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
    		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
    		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
    		m_botAction.sendArenaMessage("Ended Game By " + name,1);
    	}
    }
    public void pickx(String name, String msg)
    {
    	if(oplist.isER(name))
    	{
    		String removed = namex;
    		namex = msg;
    		m_botAction.sendArenaMessage("[Moderator] removed [X][" + removed + "] and add to [X][" + namex + "]");
    	}
    }
    public void picko(String name, String msg)
    {
    	if(oplist.isER(name))
    	{
    		String removed = nameo;
    		nameo = msg;
    		m_botAction.sendArenaMessage("[Moderator] removed [O][" + removed + "] and add to [O][" + nameo + "]");
    	}
    }
    public void arena(String name, String msg)
    {
    	if(oplist.isER(name))
    		m_botAction.sendArenaMessage(msg + " -" + name, 2);
    }
    public void welcome(String name, String msg)
    {
    	if(oplist.isER(name))
    		message = msg;
    		m_botAction.sendSmartPrivateMessage(name, "Welcome Msg: " + message);
    }
    //Got command: !die
    public void die(String name, String msg)
    {
        //Make sure player has clearance
        if(oplist.isER(name))
            //Destroy bot, allows another to be spawned
            m_botAction.die();
    }

    //Got command: !go
    public void go(String name, String msg)
    {
        //Make sure player has clearance
        if(oplist.isER(name))
            m_botAction.changeArena(msg);
            m_botAction.sendRemotePrivateMessage(name, "You have send me to " + msg);
    }
    public void tics(String name, String msg)
    {
    	m_botAction.sendPrivateMessage(name, "Game: [X]" + namex + " Vs [O]" + nameo + "!");
            m_botAction.sendPrivateMessage(name, tic[0]);
            m_botAction.sendPrivateMessage(name, tic[1]);
            m_botAction.sendPrivateMessage(name, tic[0]);
            m_botAction.sendPrivateMessage(name, tic[2]);
            m_botAction.sendPrivateMessage(name, tic[0]);
            m_botAction.sendPrivateMessage(name, tic[3]);
            m_botAction.sendPrivateMessage(name, tic[0]);
            m_botAction.sendPrivateMessage(name, "Turn : " + turn);
    }
    public void d12(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x2 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x2 == " ")
        	{
        		x2 = "X";
        		
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x2 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x2 == " ")
        	{
        		x2 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";game_status = "false";
     	   nameo = "No Player O";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";game_status = "false";
     	   nameo = "No Player O";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d13(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x3 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x3 == " ")
        	{
        		x3 = "X";
        		
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x3 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x3 == " ")
        	{
        		x3 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d22(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x5 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x5 == " ")
        	{
        		x5 = "X";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x5 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x5 == " ")
        	{
        		x5 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d31(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x7 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x7 == " ")
        	{
        		x7 = "X";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x7 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x7 == " ")
        	{
        		x7 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d32(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x8 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x8 == " ")
        	{
        		x8 = "X";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        { 
        	if (x8 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x8 == " ")
        	{
        		x8 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d33(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x9 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x9 == " ")
        	{
        		x9 = "X";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x9 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x9 == " ")
        	{
        		x9 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d23(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x6 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x6 == " ")
        	{
        		x6 = "X";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x6 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x6 == " ")
        	{
        		x6 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d21(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x4 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x4 == " ")
        	{
        		x4 = "X";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x4 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x4 == " ")
        	{
        		x4 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
    }
    public void d11(String name, String msg)
    {
        //Make sure player has clearance
        if(name.equalsIgnoreCase(turn) != true)
        	return;
        if (namex == turn)
        {
        	if (x1 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x1 == " ")
        	{
        		x1 = "X";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = nameo;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        				
        		}
        	}
        if (nameo == turn)
        {
        	if (x1 != " ")
        	{
        		m_botAction.sendSmartPrivateMessage(name, "This draw already taken!");
        	}
        	else if (x1 == " ")
        	{
        		x1 = "O";
        		tic[0] = "+---+---+---+";
        		tic[1] = "| "+x1+" | "+x2+" | "+x3+" |";
        		tic[2] = "| "+x4+" | "+x5+" | "+x6+" |";
        		tic[3] = "| "+x7+" | "+x8+" | "+x9+" |";
        		m_botAction.sendArenaMessage("Game: [X]" + namex + " Vs [O]" + nameo + "!");
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[1]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[2]);
        		m_botAction.sendArenaMessage(tic[0]);
        		m_botAction.sendArenaMessage(tic[3]);
        		m_botAction.sendArenaMessage(tic[0]);
        		turn = namex;
        		m_botAction.sendArenaMessage("Turn : " + turn);
        		}
        	}
        if (x1 == "X")if (x2 == "X")if (x3 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";game_status = "false";
        }
        if (x1 == "O")if (x2 == "O")if (x3 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x4 == "X")if (x5 == "X")if (x6 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
        }
        if (x4 == "O")if (x5 == "O")if (x6 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x3 == "X")if (x5 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x7 == "O")if (x8 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x7 == "X")if (x8 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x3 == "O")if (x5 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x1 == "X")if (x4 == "X")if (x7 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x1 == "O")if (x4 == "O")if (x7 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=""; x2=""; x3=""; x4="";x5="";x6="";x7="";x8=""; x9="";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x3 == "X")if (x6 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x3 == "O")if (x6 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x2 == "X")if (x5 == "X")if (x8 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x2 == "O")if (x5 == "O")if (x8 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x1 == "X")if (x5 == "X")if (x9 == "X")
        {
     	   m_botAction.sendArenaMessage("[X]" + namex + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	  game_status = "false";
        }
        if (x1 == "O")if (x5 == "O")if (x9 == "O")
        {
     	   m_botAction.sendArenaMessage("[O]" + nameo + ": Has Won!");
     	   x1=" "; x2=" "; x3=" "; x4=" ";x5=" ";x6=" ";x7=" ";x8=" "; x9=" ";
     	   turn = "No Turn";
     	   namex = "No Player X";
     	   nameo = "No Player O";
     	   game_status = "false";
        }
    }
    public void zone(String name, String msg)
    {
    	if(oplist.isER(name))
    	{
    	m_botAction.sendZoneMessage(msg + " -" + name, 2);
    	}
    }
    public void say(String name, String msg)
    {
    	if(oplist.isER(name))
    	{
    	m_botAction.sendPublicMessage(msg);
    	}
    }
}