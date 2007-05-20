package twcore.bots.twbot;

import java.util.HashSet;
import java.util.Vector;

import twcore.bots.TWBotExtension;
import twcore.core.OperatorList;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;

/**
 * AutoPilot.  For the lazy mod.
 * 
 * Allows Mod+ to create lists of commands (tasks) that players can then vote on
 * to execute.  Useful for creating an automated event bot that can wait in an
 * arena for enough players to join. 
 * 
 * @author dugwyler
 */
public class twbotauto extends TWBotExtension
{
    boolean isEnabled = false;      // Whether or not tasks are being processed.
    Vector <AutoTask>tasks = new Vector<AutoTask>();
    
    
    /**
     * Constructor.
     */
    public twbotauto() {
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
        if( m_opList.isModerator( name ) ) {
            if( msg.startsWith("!autoon") ) {
                cmdOn( name );
            } else if( msg.startsWith("!autooff") ) {
                cmdOff( name );
            } else if( msg.startsWith("!list") ) {
                cmdList( name );
            } else if( msg.startsWith("!add ") ) {
                cmdAdd( name, msg.substring(5) );
            } else if( msg.startsWith("!remove ") ) {
                cmdRemove( name, msg.substring(8) );
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
            m_botAction.sendPrivateMessage(event.getPlayerName(), "Autopilot is engaged.  PM !list to me to see how *YOU* can control me." );        
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
            m_botAction.sendSmartPrivateMessage(name, "Couldn't parse that.  Ex: !add lock,setship 2,spec 10;8;Starts the game." );            
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
            int num = Integer.parseInt(msg.substring(5));
            AutoTask c = tasks.get(num);
            String execute = c.doVote(name);
            // Execute if number of votes have been reached
            if( execute != null ) {
                if( execute.contains(",") ) {
                    String cmds[] = execute.split(",");
                    for( int i = 0; i<cmds.length; i++ )
                        m_botAction.sendPrivateMessage(m_botAction.getBotName(), cmds[i]);
                } else {
                    m_botAction.sendPrivateMessage(m_botAction.getBotName(), execute);
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
     * @param msg Command parameters
     */
    public void cmdOff( String name ) {
        isEnabled = false;
        m_botAction.sendSmartPrivateMessage(name, "Autopilot disengaged." );
    }

    
    /**
     * Return help message.
     */
    public String[] getHelpMessages()
    {
        String[] help = {
                "Autopilot, by dugwyler - executes a task when enough players vote for it.",
                "This module sends commands to other loaded modules, allowing you to create",
                "automatically-hosted arenas if desired.  Provide a list of commands the task",
                "will execute (separated by commas) WITHOUT the !, number of votes required to",
                "run the task, and the description players will see when they use !info.",
                "Sample usage:   !add lock,setship 2,spec 10;8;Starts a game of twisted.",
                "!add <cmd1,cmd2,cmd3,etc>;<#VotesRequired>;<Description>     - See above.",
                "!remove <Task#>       - Removes task <Task#> as found in !list.",
                "!list                 - Lists all tasks currently set up.",
                "!info                 - (For players) Shows tasks & number of votes needed to run.",
                "!autoon               - Starts player vote monitoring and task execution.",
                "!autooff              - Stops player vote monitoring and task execution."
        };
        return help;
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
            voters.remove(name);
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