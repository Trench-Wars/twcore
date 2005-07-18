package twcore.core;

import java.util.*;

/**
 * This class is made to store command information
 *
 * @author Jeremy
 * @version 1.3
 */
class Command {

    private Object      m_methodClass;
    private String      m_methodName;
    private String      m_helpString; //added a help string to be inbuild into the command
    private int         m_messageTypes;
    private int         m_opLevelReq;

    public Command( int messageTypes, Object methodClass, String methodName ){

        m_methodName = methodName;
        m_methodClass = methodClass;
        m_messageTypes = messageTypes;
        m_helpString = "";
        m_opLevelReq = 0;
    }

    /**
     * @author FoN This constructor allows the command to store the help string
     *         for this command
     * @param messageTypes
     *            Allowable message types
     * @param methodClass
     *            The class the method is located in
     * @param methodName
     *            The name of the method the command is registered to
     * @param helpString
     *            The message if !help is issued
     */
    public Command( int messageTypes, Object methodClass, String methodName, String helpString ){
        m_methodName = methodName;
        m_methodClass = methodClass;
        m_messageTypes = messageTypes;
        m_helpString = helpString;
        m_opLevelReq = 0;
    }

    /**
     * Constructor addition of access levels
     * @param messageTypes    Allowable message types
     * @param methodClass     The class the method is located in
     * @param methodName      The name of the method the command is registered to
     * @param helpString      The message if !help is issued
     * @param opStatus  The minimum access level needed to use this command
     */
    public Command( int messageTypes, Object methodClass, String methodName, int opLevelReq ){

        m_methodName = methodName;
        m_methodClass = methodClass;
        m_messageTypes = messageTypes;
        m_helpString = "";
        m_opLevelReq = opLevelReq;
    }

    /**
     * Constructor addition of access levels (with help string input)
     * @param messageTypes    Allowable message types
     * @param methodClass     The class the method is located in
     * @param methodName      The name of the method the command is registered to
     * @param helpString      The message if !help is issued
     * @param opStatus  The minimum access level needed to use this command
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

/**
 * Meant to aggregate all the commands and parse message sent to appropriate
 * methods The commands are registred to the intrepretor with a method name. As
 * a message is received, the command intrepreter parses the message for a
 * command and directs to the appropriate method as required
 *
 * @author Jeremy
 * @version 1.3
 */

public class CommandInterpreter {
    private Map         m_commands;
    private BotAction   m_botAction;
    private int         m_allCommandTypes;

    public CommandInterpreter( BotAction botAction ){

        m_allCommandTypes = 0;
        m_botAction = botAction;
        m_commands = Collections.synchronizedMap( new HashMap() );
    }

    /**
     * Registered the command into the map with its allocated methodName
     *
     * @param trigger the command name
     * @param messageTypes Different message types like private message or arena message etc
     * @param methodClass The class to register the command with
     * @param methodName The method in teh class to register the command with
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName ) );
    }

    /**
     * Overloaded previous constructor with help message as optional
     *
     * @param trigger the command name
     * @param messageTypes Different message types like private message or arena message etc
     * @param methodClass The class to register the command with
     * @param methodName The method in teh class to register the command with
     * @param helpMessage The help message associated with this command
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName, String helpMessage ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName, helpMessage ) );
    }

    /**
     * Overloaded constructor with op level required to execute
     *
     * @param trigger the command name
     * @param messageTypes Different message types like private message or arena message etc
     * @param methodClass The class to register the command with
     * @param methodName The method in teh class to register the command with
     * @param opLevelReq The help message associated with this command
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName, int opLevelReq ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName, opLevelReq ) );
    }

    /**
     * Overloaded constructor with help message and op level required to execute
     *
     * @param trigger the command name
     * @param messageTypes Different message types like private message or arena message etc
     * @param methodClass The class to register the command with
     * @param methodName The method in teh class to register the command with
     * @param helpMessage The help message associated with this command
     */
    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName, String helpMessage, int opLevelReq ){
        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName, helpMessage, opLevelReq ) );
    }


    /**
     * Registers default command if nothing matches
     *
     * @param messageType The message type ie arena message or private message etc
     * @param methodClass The class the command belongs to
     * @param methodName The method in the class the command is registered to
     */
    public void registerDefaultCommand( int messageType, Object methodClass, String methodName ){
        registerCommand( "default" + messageType, messageType, methodClass, methodName );
    }

    /**
     * Gets all the help messages registered.
     * @return Vector as a collection containing the strings of all available help msgs
     */
    public Collection getCommandHelps(){
        Vector      helps = new Vector( m_commands.size() );
        Iterator    i = m_commands.keySet().iterator();
        Command     command;

        while( i.hasNext() ){
            String      commandKey = (String)i.next();
            command = (Command)m_commands.get( commandKey );
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
     * Gets all the help messages registered available to the provided access level.
     * @return Vector as a collection containing the strings of all available help msgs (for given access level)
     */
    public Collection getCommandHelpsForAccessLevel( int accessLevel ) {
        Vector      helps = new Vector( m_commands.size() );
        Iterator    i = m_commands.keySet().iterator();
        Command     command;

        while( i.hasNext() ){
            String      commandKey = (String)i.next();
            command = (Command)m_commands.get( commandKey );
            if( command != null ){
                if( !command.getHelpString().equals( "" ) && accessLevel >= command.getOpLevelReq() ){
                    helps.add( command.getHelpString() );
                }
            }
        }

        helps.trimToSize();
        return helps;
    }

    /**
     * Get a particular help message for a particular command
     * @param trigger the command name for the help wanted
     * @return The string of the help message registered to the command
     */
    public String getCommandHelp( String trigger ){
        String      help = "";
        Command     command = (Command)m_commands.get( trigger );
        if( command != null ){
            help = command.getHelpString();
        }

        return help;
    }

    /**
     * Handles the Message event and parses and directs the command if available to the appropriate method
     * @param event Message event
     * @return true if it succeeded and false if it didn't
     */
    public boolean handleEvent( Message event ){
        boolean         result;
        String          trigger;
        Command         command;
        int             seperatorIndex;

        String          message;
        String          messager;
        String          methodName;
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

        command = (Command)m_commands.get( trigger );
        if( command == null ){
            seperatorIndex = 0;
            command = (Command)m_commands.get( "default" + messageType );
        }

        if( command != null ){
            if( (command.getMessageTypes() & messageType) != 0 ){
                message = message.substring( seperatorIndex ).trim();
                messager = event.getMessager();

                if( messager == null ){
                    messager = m_botAction.getPlayerName( event.getPlayerID() );
                }

                if( messager == null ){
                    messager = "";
                    // If we can't get the name and the command needs a certain access level, dump
                    if( command.getOpLevelReq() > 0 )
                        return false;
                }

                if( m_botAction.getOperatorList().getAccessLevel( messager ) >= command.getOpLevelReq() ) {
                    try {
                        Object parameters[] = { messager, message };
                        Object methodClass = command.getMethodClass();
                        Class parameterTypes[] = { messager.getClass(), message.getClass() };

                        methodClass.getClass().getMethod( command.getMethodName(), parameterTypes ).invoke( methodClass, parameters );
                        result = true;
                    } catch( Exception e ){
                        Tools.printStackTrace( e );
                    }
                }
            }
        }

        return result;
    }
}
