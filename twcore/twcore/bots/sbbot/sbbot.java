package twcore.bots.sbbot;
import twcore.core.*;
import java.util.*;
import java.io.*;
import java.lang.*;

/**
 * A few notes to my future self and other bot coders:
 *     I have a bunch of debugging messages built into the bot.. it will attempt to pm me
 *     about thrown exceptions and some of the stat events so that I can observe it while
 *     it's in action.
 * 
 *     Right now the architecture is very strange.. this is due to the fact that I was
 *     unfamiliar with the specifics of generics, and assumed the compiler had a compile-time
 *     type inference engine similar to OCAML's.  Unfortunately, as I learned while trying
 *     to implement everything, it does not.  I decided to go ahead and kludge the message
 *     passing architecture on anyway, though it would be better implemented as individual
 *     message queues rather than with centralized messagepassing objects.  As it stands now,
 *     a centralized messagepassing object receives notifications of events (such as a command
 *     being received, a goal being scored, signals indicating that it is time to advance the
 *     state of the game, etc.), and dispatches the notification to all objects that have
 *     registered to listen for that specific event.
 *
 *     The bot will break in several places if the team freqs are randomized/made any other
 *     0 and 1.  At the moment I know it will break the SBStatKeeper in at least getGoalGraph
 *     (it uses the freq as an array index).  It might also possibly break the SBRosterManager.
 *   
 *         --Arceo
 */
public class sbbot extends SSEventForwarder {
    private BotSettings m_botSettings;
    private BotSettings parameters;
    private BotSettings SBOpSettings;
    private CommandInterpreter CI;
    private static HashSet<String> bigBallers;
    private static OperatorList opList;
    private BotCommandOperator operator;
    private SBMatchCoordinator coordinator;
    
    public sbbot(BotAction botAction) {
	super(botAction);
	operator = new BotCommandOperator();
	loadSettings();
	getOperator().addListener(LOGGEDON, new LogonHandler());
	getOperator().addListener(MESSAGE, new CIMessagePasser());
	//getOperator().addListener(SENDARENAMESSAGE, new ArenaMessenger());
	//getOperator().addListener(SENDPRIVATEMESSAGE, new PrivateMessenger());
	CI = new CommandInterpreter(botAction);
	registerCommands();
	coordinator = new SBMatchCoordinator(operator,botAction);
    }

    protected BotCommandOperator getOperator() { return operator; }
    
    // --- Command Implementations ---
    public void commandDie(String name, String args) {
	try {
	    if(!opList.isER(name)) return;
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.DIE, new BotCommandEvent(p));
	    m_botAction.die();
	} catch (Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandHelp(String name, String args) {
	try {
	    showHelp();
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.HELP, new BotCommandEvent(p, args));
	} catch (Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandGame(String name, String args) {
	if(!isSBOp(name)) return;
	//	try {
	    Player p = m_botAction.getPlayer(name);
	    pm("arceo","name: " + name);
	    operator.notifyEvent(BotCommandOperator.STARTGAME, new BotCommandEvent(p));
	    //	} catch (Exception e) {
	    //	    pmStackTrace("arceo", e);
	    //	}
    }

    public void commandAdd(String name, String args) {
	//	try {
	    Player p = m_botAction.getPlayer(name);
	    if(m_botAction.getBotName().equalsIgnoreCase(m_botAction.getFuzzyPlayer(args).getPlayerName())) {
		m_botAction.sendSmartPrivateMessage(name, "Sorry, but I'm too busy running the match to play :(.");
		return;
	    }
	    operator.notifyEvent(BotCommandOperator.ADD, new BotCommandEvent(p, args));
	    //	} catch (Exception e) {
	    //	    pmStackTrace("arceo", e);
	    //	}
    }

    public void commandSub(String name, String args) {
	try {
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.SUB, new BotCommandEvent(p, args));
	} catch (Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandNotPlaying(String name, String args) {
	try {
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.NOTPLAYING, new BotCommandEvent(p, args));
	} catch (Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandT1SetCaptain(String name, String args) {
	//	try {
	    if(!isSBOp(name)) return;
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.T1SETCAP, new BotCommandEvent(p, args));
	    //	} catch (Exception e) {
	    //	    pmStackTrace("arceo", e);
	    //	}
    }

    public void commandT2SetCaptain(String name, String args) {
	try {
	    if(!isSBOp(name)) return;
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.T2SETCAP, new BotCommandEvent(p, args));
	} catch(Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandCap(String name, String args) {
	try {
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.CAP, new BotCommandEvent(p, args));
	} catch(Exception e) {
	    pmStackTrace("arceo", e);
	}
    }
    
    public void commandList(String name, String args) {
	try {
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.LIST, new BotCommandEvent(p, args));
	} catch(Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandLagout(String name, String args) {
	try {
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.LAGOUT, new BotCommandEvent(p, args));
	} catch (Exception e) {
	    pm("arceo", "!lagout: " + e.toString());
	}
    }
    public void commandStartPick(String name, String args) {
	try {
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.STARTPICK, new BotCommandEvent(p, args));
	} catch(Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandRemove(String name, String args) {
	try {
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.REMOVE, new BotCommandEvent(p, args));
	} catch(Exception e) {
	    pmStackTrace("arceo", e);
	}
    }

    public void commandKillGame(String name, String args) {
	try {
	    if(!opList.isER(name)) return;
	    Player p = m_botAction.getPlayer(name);
	    operator.notifyEvent(BotCommandOperator.KILLGAME, new BotCommandEvent(p, args));
	} catch(Exception e) {
	    pmStackTrace("arceo", e);
	}	    
    }

    public void commandReady(String name, String args) {
	Player p = m_botAction.getPlayer(name);
	operator.notifyEvent(BotCommandOperator.READY, new BotCommandEvent(p, args));
    }
    
    private void loadSettings() {
	m_botSettings = m_botAction.getBotSettings();
	opList = m_botAction.getOperatorList();
	parameters = new BotSettings( m_botSettings.getString( "paramfile" ) );
	System.out.println("Paramfile: " + m_botSettings.getString( "paramfile" ));
	SBOpSettings = new BotSettings( m_botSettings.getString( "opfile" ) );
	bigBallers = new HashSet<String>();
	try {
	    for( String x : Tools.cleanStringChopper( SBOpSettings.getString( "ops" ), ';') ) {
		bigBallers.add(x.toLowerCase());
	    }
	} catch (Exception e){
	    pmStackTrace("arceo", e);
	}
    }
    
    private class LogonHandler extends SSEventListener {
	public void notify(SSEventMessageType type, LoggedOn event) {
	    m_botAction.joinArena(m_botSettings.getString("Arena"));
	    m_botAction.sendUnfilteredPublicMessage("?chat=ballers,ballerradio");

	}
    }

    private class CIMessagePasser extends SSEventListener {
	public void notify(SSEventMessageType type, twcore.core.Message event) {
	    CI.handleEvent(event);
	}
    }

    private class ArenaMessenger extends BotCommandListener {
	public void notify(BotCommandType type, BotCommandEvent event) {
	    
	}
    }

    private void registerCommands() {
	int mtype = twcore.core.Message.PRIVATE_MESSAGE;
	CI.registerCommand("!help", mtype, this, "commandHelp");
	CI.registerCommand("!die", mtype, this, "commandDie");
	CI.registerCommand("!add", mtype, this, "commandAdd");
	CI.registerCommand("!sub", mtype, this, "commandSub");
	CI.registerCommand("!notplaying", mtype, this, "commandNotPlaying");
	CI.registerCommand("!game", mtype, this, "commandGame");
	CI.registerCommand("!t1-setcaptain", mtype, this, "commandT1SetCaptain");
	CI.registerCommand("!t2-setcaptain", mtype, this, "commandT2SetCaptain");
	CI.registerCommand("!lagout", mtype, this, "commandLagout");
	CI.registerCommand("!cap", mtype, this, "commandCap");
	CI.registerCommand("!list", mtype, this, "commandList");
	CI.registerCommand("!startpick", mtype, this, "commandStartPick");
	CI.registerCommand("!remove", mtype, this, "commandRemove");
	CI.registerCommand("!ready", mtype, this, "commandReady");
	CI.registerCommand("!killgame", mtype, this, "commandKillGame");
    }

    private void showHelp() {

    }

    public static boolean isSBOp( String name ) {
	if( opList.isER( name ) ) return true;
	if( bigBallers == null ) return false;
	return bigBallers.contains( name.toLowerCase() );
    }

    private void pm(String name, String message) {
	m_botAction.sendSmartPrivateMessage(name,message);
    }

    private void pmStackTrace(String name, Exception e) {
	for(StackTraceElement el : e.getStackTrace()) {
	    pm(name, el.toString());
	}
    }
}