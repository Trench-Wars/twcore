package twcore.bots.multibot.util;

import java.util.HashSet;
import java.util.Vector;

import twcore.bots.MultiUtil;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;

/**
 * AutoPilot.  For the lazy mod.
 *
 * Allows Mod+ to create lists of commands (tasks) that players can then vote on
 * to execute.  Useful for creating an automated event bot that can wait in an
 * arena for enough players to join.
 *
 * @author dugwyler
 */
public class utilautopilot extends MultiUtil
{
    boolean isEnabled = false;      // Whether or not tasks are being processed.
    Vector <AutoTask>tasks = new Vector<AutoTask>();

    /**
     * Constructor.
     */
    public void init() {
    }

    /**
     * Requests events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.PLAYER_ENTERED );
        modEventReq.request(this, EventRequester.PLAYER_LEFT );
    }

    /**
     * Handle Messages.
     */
    public void handleEvent(Message event) {
        if( event.getMessageType() != Message.PRIVATE_MESSAGE && event.getMessageType() != Message.REMOTE_PRIVATE_MESSAGE )
            return;
        Player p = m_botAction.getPlayer( event.getPlayerID() );
        if( p == null ) return;
        String name = p.getPlayerName();
        String msg = event.getMessage();
        if( m_opList.isER( name ) ) {
            if( msg.startsWith("!autoon") ) {
                cmdOn( name );
            } else if( msg.startsWith("!autooff") ) {
                cmdOff( name );
            } else if( msg.startsWith("!listtasks") ) {
                cmdList( name );
            } else if( msg.startsWith("!addtask ") ) {
                cmdAdd( name, msg.substring(9) );
            } else if( msg.startsWith("!removetask ") ) {
                cmdRemove( name, msg.substring(12) );
            } else if( msg.startsWith("!modinfo") ) {
                cmdModInfo( name );
            }
        }
        if( msg.startsWith("!info") ) {
            cmdInfo( name );
        } else {
            cmdDefault( name, msg );
        }
    }


    /**
     * PMs players about Autopilot.
     */
    public void handleEvent(PlayerEntered event) {
        if( isEnabled )
            m_botAction.sendPrivateMessage(event.getPlayerName(), "Autopilot is engaged.  PM !info to me to see how *YOU* can control me.  (Type :" + m_botAction.getBotName() + ":!info)" );
    }


    /**
     * Removes votes of players who have left.
     */
    public void handleEvent(PlayerLeft event) {
        if( !isEnabled ) return;
        if( tasks.size() == 0 ) return;
        Player p = m_botAction.getPlayer(event.getPlayerID());
        if( p == null ) return;

        for( int i = 0; i < tasks.size(); i++ ) {
            AutoTask c = tasks.get( i );
            if( c != null )
                c.removeVote( p.getPlayerName() );
        }
    }


    /**
     * Adds a new task.
     * @param name Name of op
     * @param msg Command parameters
     */
    public void cmdAdd( String name, String msg ) {
        String[] args = msg.split( ";" );
        if( args.length != 3 ) {
            m_botAction.sendSmartPrivateMessage(name, "Incorrect number of arguments found.  Use ; to separate each.  Ex: !add !start;8;Starts the game." );
            return;
        }

        try {
            String command = args[0];
            int votesReq = Integer.parseInt(args[1]);
            String display = args[2];
            AutoTask c = new AutoTask( command, votesReq, display );
            tasks.add(c);
            m_botAction.sendSmartPrivateMessage( name, "Task added: "+ command );
            String s = "Autopilot: " + name + " has added this task: " + command;
            // Display to chat for people who are not extremely important
            if( m_opList.getAccessLevel(name) < OperatorList.SMOD_LEVEL ) {
                m_botAction.sendChatMessage( s );
            }
        } catch (Exception e) {
            m_botAction.sendSmartPrivateMessage(name, "Couldn't parse that.  Ex: !add dolock,setship 2,spec 10;8;Starts the game." );
        }
    }


    /**
     * Removes a command.
     * @param name Name of op
     * @param msg Command parameters
     */
    public void cmdRemove( String name, String msg ) {
        try {
            int num = Integer.parseInt(msg);
            AutoTask c = tasks.remove( num - 1 );
            if( c != null ) {
                m_botAction.sendSmartPrivateMessage(name, "Removed command: " + c.getCommand() + "  (" + c.getDisplay() + ")" );
            } else {
                m_botAction.sendSmartPrivateMessage(name, "Couldn't find that number.  Use !list to verify." );
            }
        } catch (Exception e ) {
            m_botAction.sendSmartPrivateMessage(name, "Couldn't parse that.  Do !list to verify your number.  Ex: !remove 1" );
        }

    }


    /**
     * Lists all commands.
     * @param name Name of op
     * @param msg Command parameters
     */
    public void cmdList( String name ) {
        if( tasks.size() == 0 ) {
            m_botAction.sendSmartPrivateMessage(name, "No commands entered yet." );
            return;
        }
        for( int i = 0; i < tasks.size(); i++ ) {
            AutoTask c = tasks.get( i );
            if( c != null )
                m_botAction.sendSmartPrivateMessage(name, (i + 1) + ") '" + c.getCommand() + "'  Votes: " + c.getVotes() + " of " + c.getVotesReq() + " needed.  Text: " + c.getDisplay() );
        }
    }


    /**
     * Displays list of commands available to players.
     * @param name Name of player
     * @param msg Command parameters
     */
    public void cmdInfo( String name ) {
        if( !isEnabled ) {
            m_botAction.sendSmartPrivateMessage(name, "Autopilot is not currently enabled.  Please hang up and try again later." );
            return;
        }

        m_botAction.sendSmartPrivateMessage(name, "Autopilot - The automated event host controlled by players." );
        m_botAction.sendSmartPrivateMessage(name, "If you wish for the bot to run one of the following tasks, send the command" );
        m_botAction.sendSmartPrivateMessage(name, "next to it (!task#).  Your vote will be counted.  When enough votes have been" );
        m_botAction.sendSmartPrivateMessage(name, "reached, the task will run.  If you leave the arena all your votes are removed." );

        m_botAction.sendSmartPrivateMessage(name, "List of tasks available to run:" );
        if( tasks.size() == 0 )
            m_botAction.sendSmartPrivateMessage(name, "No tasks have been entered yet." );

        for( int i = 0; i < tasks.size(); i++ ) {
            AutoTask c = tasks.get( i );
            if( c != null )
                m_botAction.sendSmartPrivateMessage(name, "!task" + (i + 1) + " (" + c.getVotes() + " of " + c.getVotesReq() + " votes needed)  " + c.getDisplay() );
        }
    }


    /**
     * Default command.  Checks for players voting for tasks, and executes the
     * task if enough votes have been reached.
     * @param name Name of player
     * @param msg Command parameters
     */
    public void cmdDefault( String name, String msg ) {
        if( !isEnabled || tasks.size() == 0 || !msg.startsWith("!task" ))
            return;
        try {
            int num = Integer.parseInt(msg.substring(5).trim()) - 1;
            AutoTask c = tasks.get(num);
            String execute = c.doVote(name);
            // Execute if number of votes have been reached
            if( execute != null ) {
                if( execute.contains(",") ) {
                    String cmds[] = execute.split(",");
                    for( int i = 0; i<cmds.length; i++ ) {
                        if( cmds[i].startsWith("-arena")) {
                            String[] args = cmds[i].split(" ", 2);
                            if( args.length == 2 )
                                m_botAction.sendArenaMessage( args[1] + " -" + m_botAction.getBotName() );
                        } else if( cmds[i].startsWith("-wait") ) {
                            String[] args = cmds[i].split(" ", 2);
                            if( args.length == 2 ) {
                                try {
                                    Integer wait = Integer.getInteger(args[1]);
                                    if( wait <= 60 )
                                        this.wait( wait * 1000 );
                                } catch (Exception e) {
                                }
                            }
                        } else {
                            m_botAction.sendPrivateMessage(m_botAction.getBotName(), "!" + cmds[i]);
                        }
                    }
                } else {
                    m_botAction.sendPrivateMessage(m_botAction.getBotName(), "!" + execute);
                }
            }
        } catch (Exception e ) {
            m_botAction.sendSmartPrivateMessage(name, "Which task do you want to vote for?  Please try !info to verify this task still exists." );
        }
    }


    /**
     * Turns on Autopilot.
     * @param name Name of op
     * @param msg Command parameters
     */
    public void cmdOn( String name ) {
        isEnabled = true;
        m_botAction.sendSmartPrivateMessage(name, "Autopilot engaged." );
    }


    /**
     * Turns off Autopilot.
     * @param name Name of op
     */
    public void cmdOff( String name ) {
        isEnabled = false;
        m_botAction.sendSmartPrivateMessage(name, "Autopilot disengaged." );
    }


    /**
     * Sends mod info
     * @param name
     */
    public void cmdModInfo( String name ) {
        m_botAction.smartPrivateMessageSpam(name, getModInfoMessage());
    }


    /**
     * Return help message.
     */
    public String[] getHelpMessages()
    {
        String[] help = {
        		"!modinfo              - Displays information on module use.",
                "!addtask <cmd1,cmd2,cmd3,etc>;<#VotesRequired>;<Description>.",
                "!removetask <Task#>   - Removes task <Task#> as found in !list.",
                "!listtasks            - Lists all tasks currently set up.",
                "!info                 - (For players) Shows tasks & number of votes needed to run.",
                "!autoon               - Starts player vote monitoring and task execution.",
                "!autooff              - Stops player vote monitoring and task execution.",
                "!help                 - displays this."
        };
        return help;
    }

    public String[] getModInfoMessage(){
    	String[] info = {
    			"Autopilot, by dugwyler - executes a task when enough players vote for it.",
                "This module sends commands to other loaded modules, allowing you to create",
                "automatically-hosted arenas if desired.  Provide a list of commands the task",
                "will execute (separated by commas) WITHOUT the !, number of votes required to",
                "run the task, and the description players will see when they use !info.",
                "Sample usage:   !add dolock,setship 2,spec 10;8;Starts a game of twisted.",
                "(Use cmds -arena <msg> and -wait <seconds> to send arena msgs/have the bot wait).",
    	};
    	return info;
    }


    /**
     * NOP.
     */
    public void cancel()
    {
    }


    /**
     * Stores command data.
     */
    public class AutoTask {
        String cmd;
        int votesReq;
        int votes;
        String display;
        HashSet <String>voters;

        public AutoTask( String cmd, int votesReq, String display ) {
            this.cmd = cmd;
            this.votesReq = votesReq;
            this.display = display;
            voters = new HashSet<String>();
            votes = 0;
        }

        public String doVote( String name ) {
            if( voters.contains(name) ) {
                m_botAction.sendSmartPrivateMessage(name, "Removing your vote for: \"" + display + "\"" );
                removeVote(name);
                return null;
            }
            voters.add(name);
            votes++;
            if( votes >= votesReq ) {
                m_botAction.sendArenaMessage( "Autopilot executing task: \"" + display + "\" ...  " + votesReq + " needed to re-execute.", 1 );
                votes = 0;
                voters.clear();
                return cmd;
            } else {
                m_botAction.sendSmartPrivateMessage(name, "Vote added.  " + (votesReq - votes) + " more votes are needed to run this task.  (Leaving the arena will remove your vote.)" );
                return null;
            }
        }

        public void removeVote( String name ) {
            if( voters.remove(name) )
                if( votes > 0 )
                    votes--;
        }

        public String getCommand() {
            return cmd;
        }

        public int getVotesReq() {
            return votesReq;
        }

        public int getVotes() {
            return votes;
        }

        public String getDisplay() {
            return display;
        }
    }
}