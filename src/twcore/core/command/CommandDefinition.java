/*
    CommandDefinition.java

    Created on March 17, 2004, 1:08 PM
*/

package twcore.core.command;

import java.util.ArrayList;

import twcore.core.events.Message;
import twcore.core.util.Tools;

/**

    @author  Stefan / Mythrandir

    commandString example:
    game String 'Game name' required:Integer 'Time limit' '30':String 'Team 1 name' 'freq 1':String 'Team 2 name' 'freq 2'

    each commandstring contains the key name and then a list of parameters separated by a ':', following this structure

    - parameter type (Integer / Double / String). Capitalization of the first letter is obligatory
    - parameter name enclosed by single quotes (')
    and then either 'required' specifying that this is a required parameters or another value enclosed by single quotes
    defining the default value if it's not specified with.

    currently supported objects: Integer Double String (make sure to use capital letters correctly)
*/
public class CommandDefinition {

    char m_delimiter = ':';
    String m_trigger = "none";
    String m_helpMessage = "";
    String m_commandString;
    String m_methodName;
    Object m_methodClass;
    ArrayList<CommandParameterDefinition> m_paramTypes;
    int m_messageTypes = Message.PRIVATE_MESSAGE;
    boolean m_enabled = true;


    /** Creates a new instance of CommandDefinition */
    public CommandDefinition(Object methodClass, String methodName, String commandString, int messageTypes, String helpMessage, boolean enabled, char delimiter) {
        m_methodClass = methodClass;
        m_methodName = methodName;
        m_commandString = commandString.trim();
        m_messageTypes = messageTypes;
        m_enabled = enabled;
        m_delimiter = delimiter;
        m_helpMessage = helpMessage;
        m_paramTypes = new ArrayList<CommandParameterDefinition>();
        dismantleCommandString();
    }

    public CommandDefinition(Object methodClass, String methodName, String commandstring, int messageTypes, String helpMessage, boolean enabled) {
        this(methodClass, methodName, commandstring, messageTypes, helpMessage, enabled, ':');
    }

    public CommandDefinition(Object methodClass, String methodName, String commandstring, int messageTypes, String helpMessage) {
        this(methodClass, methodName, commandstring, messageTypes, helpMessage, true, ':');
    }


    public void dismantleCommandString() {
        int trigends = m_commandString.indexOf(' ');

        if (trigends == -1)
            m_trigger = m_commandString.toLowerCase();
        else {
            m_trigger = m_commandString.substring(0, trigends).trim().toLowerCase();
            String paramstring = m_commandString.substring(trigends).trim();
            String[] paramTypesS = Tools.cleanStringChopper(paramstring, ':');

            for (int i = 0; i < paramTypesS.length; i++) m_paramTypes.add(new CommandParameterDefinition(paramTypesS[i]));
        }
    }


    public void enable() {
        m_enabled = true;
    }
    public void disable() {
        m_enabled = false;
    }

    public Object getMethodClass() {
        return m_methodClass;
    }
    public String getMethodName() {
        return m_methodName;
    }
    public String getTrigger() {
        return m_trigger;
    }
    public int getMessageTypes() {
        return m_messageTypes;
    }
    public char getDelimiter() {
        return m_delimiter;
    }
    public boolean isEnabled() {
        return m_enabled;
    }
    public ArrayList<CommandParameterDefinition> getCommandParameterDefinitions() {
        return m_paramTypes;
    }

    public String getFriendlySyntax() {
        String s = m_trigger + " ";

        for (int i = 0; i < m_paramTypes.size(); i++) {
            s = s + m_paramTypes.get(i).getKey();

            if (i < m_paramTypes.size() - 1) s = s + m_delimiter;
        }

        return s;
    }

    public String getHelpMessage() {
        return (Tools.formatString(getFriendlySyntax(), 60) + "- " + m_helpMessage);
    }

}
