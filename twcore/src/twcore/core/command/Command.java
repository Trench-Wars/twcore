/*
    Command.java

    Created on March 17, 2004, 2:00 PM
*/

package twcore.core.command;

import java.util.ArrayList;

import twcore.core.BotAction;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**

    @author  Stefan / Mythrandir
*/
public class Command {
    int m_messageType, m_playerID;
    String m_name, m_message;
    ArrayList<Object> m_params;

    CommandDefinition m_commandDefinition;
    ArrayList<CommandParameterDefinition> m_cDefList;

    boolean m_isInvalidCommand = false;
    String m_invalidMessage;



    /** Creates a new instance of Command */
    public Command(Message m, CommandDefinition commandDefinition) {

        m_commandDefinition = commandDefinition;
        m_cDefList = m_commandDefinition.getCommandParameterDefinitions();
        m_messageType = m.getMessageType();
        m_playerID = m.getPlayerID();
        m_name = m.getMessager() != null ? m.getMessager() : BotAction.getBotAction().getPlayerName(m.getPlayerID());
        m_message = m.getMessage();

        //handleParams
        try {
            handleParams();
        } catch (InvalidCommandException ice) {
            m_isInvalidCommand = true;
            m_invalidMessage = ice.getMessage();
        } catch (Exception e) {
            Tools.printStackTrace(e);
        }
    }


    private void handleParams() throws Exception {
        // get the param string array
        String      paramstring     = m_message.substring(m_commandDefinition.getTrigger().length()).trim();
        String[]    paramsa         = Tools.cleanStringChopper(paramstring, m_commandDefinition.getDelimiter());
        String      spawntype;
        String      spawnvalue;
        String      spawnkey        = "";
        int         loopTo          = paramsa.length > m_cDefList.size() ? paramsa.length : m_cDefList.size();
        m_params                    = new ArrayList<Object>();
        CommandParameterDefinition cpd;

        if (paramstring.equals("")) paramsa = new String[0];

        // loop over all params
        for (int i = 0; i < loopTo; i++) {
            // determine parameter value
            if (i < paramsa.length) spawnvalue = paramsa[i].trim();
            else spawnvalue = null;

            // determine parameter type
            if (i < m_cDefList.size()) {
                cpd = (CommandParameterDefinition)m_cDefList.get(i);
                spawntype = cpd.getType();
                spawnkey = cpd.getKey();


                // if parameter value was null, see if there's a default value, or if it's allowed at all
                if (spawnvalue == null) {
                    if (cpd.isRequired())
                        throw new InvalidCommandException("Incorrect syntax, parameter #" + (i + 1) + " should be specified.");
                    else spawnvalue = cpd.getDefaultValue();
                }

            } else spawntype = "String";

            // create the object of class spawntype with value spawnvalue and add it to the parameter list
            if (spawntype.equals("String")) m_params.add(spawnvalue);

            try {
                if (spawntype.equals("Integer")) m_params.add(new Integer(spawnvalue));

                if (spawntype.equals("Double")) m_params.add(new Double(spawnvalue));
            } catch (NumberFormatException nfe) {
                throw new InvalidCommandException("Incorrect value: " + spawnvalue + ", '" + spawnkey + "' should be numeric.");
            }

            spawnvalue = null;
            spawntype = null;
            spawnkey = "";
        }
    }


    public boolean isInvalid() {
        return m_isInvalidCommand;
    }
    public String getInvalidMessage() {
        return m_invalidMessage;
    }
    public String getName() {
        return m_name;
    }
    public ArrayList<Object> getParameters() {
        return m_params;
    }

    public Object get(int n) {
        if (n < m_params.size())
            return m_params.get(n);
        else
            return null;
    }

    public Object get(String key) {
        for (int i = 0; i < m_cDefList.size(); i++) {
            if (((CommandParameterDefinition)m_cDefList.get(i)).getKey().equalsIgnoreCase(key)) {
                if (i < m_params.size())
                    return m_params.get(i);
                else
                    return null;
            }
        }

        return null;
    }

}

@SuppressWarnings("serial")
class InvalidCommandException extends Exception {
    public InvalidCommandException(String msg) {
        super(msg);
    }
}
