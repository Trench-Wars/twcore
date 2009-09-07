package twcore.core.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.lang.reflect.InvocationTargetException;

import twcore.core.BotAction;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * A simple alternative to manually handling commands.  Command triggers (such
 * as !help) are registered with the bot, along with message types that are
 * accepted, and the method that should be called when the command is triggered
 * Optionally you can also include a minimum access level required to use the
 * command, and/or a help message used to generate automated help displays based
 * on the access level of the person requesting help.
 * <p>
 * To use the CommandInterpreter: instantiate a CommandInterpreter in your class;
 * register the commands using the registerCommand method; send all Message events
 * received by your bot to the CommandInterpreter for handling.
 * <p>
 * Some examples of how to use various methods:<p><code><pre>
 * registerCommand( "!help", Message.PRIVATE_MESSAGE, this, "cmdHelp" );
 * registerCommand( "!die", Message.PUBLIC_MESSAGE, this, "cmdDie", "!die - Kills bot" );
 * registerCommand( "8th", Message.CHAT_MESSAGE, this, "cmdSell", "8th - sell", OperatorList.HIGHMOD_LEVEL );
 * </pre></code>
 * <p>
 * Here is how the CommandInterpreter algorithm goes:<p>
 * 1. A Message event is fired within a class that is running this CommandInterpreter.<br>
 * 2. If the Message matches a trigger of a registered command, continue.<br>
 * 3. If the Message type is one of the accepted types of the matched command, continue.<br>
 * 4. If the sender is at least of the access level required for the command, continue.<br>
 * 5. Execute the method specified of the class specified in the matched command.
 * <p>
 * For more advanced command handling, see the /misc/Command/AdvancedCommandHandler class.
 * @author Jeremy
 * @version 1.5
 */
public class CommandInterpreter {
    private Map<String, Command>    m_commands;         // (String)Command trigger -> (Command)
    private BotAction               m_botAction;        // BotAction reference
    private int                     m_allCommandTypes;  // Bitvector holding all msg types handled

    /**
     * Creates a new instance of CommandInterpreter.
     * @param botAction Reference to BotAction
     */
    public CommandInterpreter( BotAction botAction ){
        m_allCommandTypes = 0;
        m_botAction = botAction;
        m_commands = Collections.synchronizedMap( new HashMap<String, Command>() );
    }

    /**
     * Registers a command with a provided trigger, message type, class, and method
     * name.  The trigger is automatically defined as the default help message, and
     * the required access level will be set by default to 0 (no special access required).
     *
     * @param trigger The text trigger of the command, such as "!help" or "!go"
     * @param messageTypes Message types accepted as defined in Message; use | (bit OR) to combine multiple types
     * @param methodClass Class to register the command with (use <code>this</code>)
     * @param methodName Method of methodClass to be called if the command's trigger and type match
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName, trigger.toLowerCase(), 0 ) );
    }

    /**
     * Registers a command with a provided trigger, message type, class, method
     * name, and help message.  Required access level will default to 0 (no special
     * access required).
     *
     * @param trigger The text trigger of the command, such as "!help" or "!go"
     * @param messageTypes Message types accepted as defined in Message; use | (bit OR) to combine multiple types
     * @param methodClass Class to register the command with (use <code>this</code>)
     * @param methodName Method of methodClass to be called if the command's trigger and type match
     * @param helpMessage Help message used in getCommandHelps() and getCommandHelpsForAccessLevel()
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName, String helpMessage ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName, helpMessage, 0 ) );
    }

    /**
     * Registers a command with a provided trigger, message type, class, method
     * name, and minimum necessary access level to execute.  The trigger is
     * automatically defined as the default help message.
     *
     * @param trigger The text trigger of the command, such as "!help" or "!go"
     * @param messageTypes Message types accepted as defined in Message; use | (bit OR) to combine multiple types
     * @param methodClass Class to register the command with (use <code>this</code>)
     * @param methodName Method of methodClass to be called if the command's trigger and type match
     * @param opLevelReq Minimum access level needed to access this command as defined in OperatorList
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName, int opLevelReq ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName, trigger.toLowerCase(), opLevelReq ) );
    }

    /**
     * Registers a command with a provided trigger, message type, class, method
     * name, help message, and minimum necessary access level to execute.
     *
     * @param trigger The text trigger of the command, such as "!help" or "!go"
     * @param messageTypes Message types accepted as defined in Message; use | (bit OR) to combine multiple types
     * @param methodClass Class to register the command with (use <code>this</code>)
     * @param methodName Method of methodClass to be called if the command's trigger and type match
     * @param helpMessage Help message used in getCommandHelps() and getCommandHelpsForAccessLevel()
     * @param opLevelReq Minimum access level needed to access this command as defined in OperatorList
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName, String helpMessage, int opLevelReq ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName, helpMessage, opLevelReq ) );
    }

    /**
     * Registers a default command.  A default command is the command that executes when
     * no other command is found for the specified message type.
     *
     * @param messageType Message type accepted as defined in Message (use only one)
     * @param methodClass Class to register the command with (use <code>this</code>)
     * @param methodName Method of methodClass to be called when no trigger that accepts the message type matches
     */
    public void registerDefaultCommand( int messageType, Object methodClass, String methodName ){
        registerCommand( "default" + messageType, messageType, methodClass, methodName );
    }

    /**
     * Registers an automated !help command that PMs a help display to a player
     * based on access level.  If you do not define access levels for commands,
     * all commands will be displayed.
     *
     * @param messageTypes Message types accepted as defined in Message; use | (bit OR) to combine multiple types
     * @param methodClass Class to register the command with (use <code>this</code>)
     */
    public void registerHelpCommand( int messageTypes, Object methodClass ){
        registerCommand( "!help", messageTypes, methodClass, "__autohelp" );
    }

    /**
     * Returns all registered help messages, regardless of access level.  Use if you
     * are not specifying minimum access levels for your commands.
     * @return Vector as a collection containing the strings of all help messages
     */
    public Collection<String> getCommandHelps(){
        Vector<String>   helps = new Vector<String>( m_commands.size() );
        Iterator<String> i = m_commands.keySet().iterator();
        Command         command;

        while( i.hasNext() ){
            String      commandKey = i.next();
            command = m_commands.get( commandKey );
            if( command != null ){
                if( !command.getHelpString().equals( "" ) ){
                    helps.add( command.getHelpString() );
                }
            }
        }

        helps.trimToSize();
        return helps;
    }

    /**
     * Returns all help messages for commands available to a given access level.
     * @return Vector as a collection containing the strings of all help msgs for a given access level
     */
    public Collection<String> getCommandHelpsForAccessLevel( int accessLevel ) {
        Vector<String>  helps = new Vector<String>( m_commands.size() );
        Iterator<String> i = m_commands.keySet().iterator();
        Command         command;

        while( i.hasNext() ){
            String      commandKey = i.next();
            command = m_commands.get( commandKey );
            if( command != null ){
                if( !command.getHelpString().equals( "" ) && !command.getHelpString().startsWith( "default" ) &&
                        accessLevel >= command.getOpLevelReq() ){
                    helps.add( command.getHelpString() );
                }
            }
        }

        helps.trimToSize();
        return helps;
    }
    
    /**
     * Returns the amount of commands that are allowed for the specified accesslevel (excluding the default commands)
     * 
     * @param accesslevel
     * @return
     */
    public int getAllowedCommandsCount( int accesslevel ) {
    	int count = 0;
    	
    	for( Command command : m_commands.values()) {
    		if(!command.getHelpString().startsWith("default") && command.getOpLevelReq() <= accesslevel) {
    			count++;
    		}
    	}
    	return count;
    }
    
    /**
     * Returns the amount of commands (excluding the default commands)
     * @return
     */
    public int getCommandsCount() {
    	int count = 0;
    	
    	for ( Command command : m_commands.values()) {
    		if(!command.getHelpString().startsWith("default"))
    			count++;
    	}
    	
    	return count;
    }

    /**
     * Gets a particular help message for a particular command, as specified by
     * its trigger.
     * @param trigger The text trigger of the command, such as "!help" or "!go"
     * @return The string of the help message registered with the command
     */
    public String getCommandHelp( String trigger ){
        String      help = "";
        Command     command = m_commands.get( trigger );
        if( command != null ){
            help = command.getHelpString();
        }

        return help;
    }

    /**
     * Handles the Message event for a bot to interpret any incoming commands.
     * In order for CommandInterpreter to work properly, this method must be
     * called every time the bot received a Message event.
     *
     * The method checks the message text against its list of triggers, and if
     * message types & required access levels match, executes the command's
     * registered method.
     * @param event Message event
     * @return true if it succeeded and false if it didn't
     */
    public boolean handleEvent( Message event ){
        boolean         result;
        String          trigger;
        Command         command;
        int             seperatorIndex;

        String          message;
        String          messager = null;
        int             messageType;

        result = false;

        message = event.getMessage();
        messageType = event.getMessageType();

        if( (m_allCommandTypes & messageType) == 0 ){
            return false;
        }

        seperatorIndex = message.indexOf( ' ' );
        if( seperatorIndex == -1 ){
            trigger = message.toLowerCase();
            seperatorIndex = message.length();
        } else {
            trigger = message.substring( 0, seperatorIndex ).toLowerCase();
        }

        command = m_commands.get( trigger );
        if( command == null ){
            seperatorIndex = 0;
            command = m_commands.get( "default" + messageType );
        }

        if( command != null ){
            if( (command.getMessageTypes() & messageType) != 0 ){
                if(messageType == Message.REMOTE_PRIVATE_MESSAGE || messageType == Message.CHAT_MESSAGE)
                    messager = event.getMessager();
                else
                    messager = m_botAction.getPlayerName( event.getPlayerID() );

                if( messager == null ){
                    messager = "";
                    // If we can't get the name and the command needs a certain access level, dump
                    if( command.getOpLevelReq() > 0 )
                        return false;
                }

                message = message.substring( seperatorIndex ).trim();

                if( m_botAction.getOperatorList().getAccessLevel( messager ) >= command.getOpLevelReq() ) {
                    try {
                        Object parameters[] = { messager, message };
                        Object methodClass = command.getMethodClass();
                        if( command.getMethodName() == "__autohelp" ) {
                            // Autohelp
                            Collection<String> msgs = getCommandHelpsForAccessLevel( m_botAction.getOperatorList().getAccessLevel( messager ) );
                            m_botAction.privateMessageSpam( messager, msgs );
                        } else {
                            // Standard command execution
                            Class<?> parameterTypes[] = { messager.getClass(), message.getClass() };
                            methodClass.getClass().getMethod( command.getMethodName(), parameterTypes ).invoke( methodClass, parameters );
                        }
                        result = true;
                    } catch( InvocationTargetException e ) {
                        if( e.getCause() != null && e.getCause().getMessage() != null ) {
                            if( e.getCause() instanceof TWCoreException )
                                m_botAction.sendSmartPrivateMessage( messager, e.getCause().getMessage() );
                            else
                                Tools.printStackTrace( (Exception)e.getCause() );
                        } else {
                            Tools.printStackTrace( e );
                        }
                    } catch( Exception e ){
                        Tools.printStackTrace( e );
                    }
                }
            }
        }

        return result;
    }


    /**
     * Stores all needed information on a given command.
     *
     * @author Jeremy
     * @version 1.5
     */
    class Command {

        private Object      m_methodClass;      // Reference to class command is reg'd with
        private String      m_methodName;       // Method to call in class on command match
        private String      m_helpString;       // Text returned for help on this command
        private int         m_messageTypes;     // Message types accepted for this cmd (bitvector; see Message)
        private int         m_opLevelReq;       // Minimum access level required to use cmd (see OperatorList)


        /**
         * Create a new Command with provided information.
         * @param messageTypes    Allowable message types (bitvector; see Message)
         * @param methodClass     Class the method is located in
         * @param methodName      Name of the method the command is registered to
         * @param helpString      Message used by the help methods
         * @param opStatus        Minimum access level needed to use this command (see OperatorList)
         */
        public Command( int messageTypes, Object methodClass, String methodName, String helpString, int opLevelReq ){
            m_methodName = methodName;
            m_methodClass = methodClass;
            m_messageTypes = messageTypes;
            m_helpString = helpString;
            m_opLevelReq = opLevelReq;
        }


        //Getter Methods
        public Object getMethodClass(){

            return m_methodClass;
        }

        public String getMethodName(){

            return m_methodName;
        }

        public int getMessageTypes(){

            return m_messageTypes;
        }

        public String getHelpString(){
            return m_helpString;
        }

        public int getOpLevelReq(){
            return m_opLevelReq;
        }
    }
}
