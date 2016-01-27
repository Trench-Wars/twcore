/*
    CommandParameter.java

    Created on March 21, 2004, 1:08 AM
*/

package twcore.core.command;

import twcore.core.util.Tools;

/**

    @author  Stefan / Mythrandir


*/
public class CommandParameterDefinition {
    String m_defaultValue;
    String m_key;
    String m_type;
    boolean m_isRequired = false;

    /** Creates a new instance of CommandParameter */
    public CommandParameterDefinition(String type, String key, String defaultValue) {
        setType(type);
        m_key = key;
        m_defaultValue = defaultValue;
    }

    public CommandParameterDefinition(String type, String key, boolean isRequired) {
        this(type, key, null);
        m_isRequired = true;
    }


    /*  extract the parameter definition out of a String like i.e. "Integer 'duration' required"
        - parameter type (Integer / Double / String)
        - parameter name enclosed by single quotes (')
        and then either 'required' specifying that this is a required parameters or another value enclosed by single quotes
        defining the default value if it's not specified with.
    */
    public CommandParameterDefinition(String s) {
        try {
            setType(s.substring(0, s.indexOf(' ')));
            int firstquote = s.indexOf('\'');
            int secondquote = s.indexOf('\'', firstquote + 1);
            m_key = s.substring(firstquote + 1, secondquote);

            if (s.indexOf("required", secondquote) != -1 && s.lastIndexOf('\'') == secondquote)
                m_isRequired = true;
            else {
                if (s.lastIndexOf('\'') != secondquote)  {
                    firstquote = s.indexOf('\'', secondquote + 1);
                    secondquote = s.indexOf('\'', firstquote + 1);
                    m_defaultValue = s.substring(firstquote + 1, secondquote);
                }
            }

        } catch (Exception e) {
            Tools.printLog("Could not figure out the following CommandParameterDefinition: " + s + " ---> ");
            Tools.printLog(e.getMessage());
            Tools.printStackTrace(e);
        }
    }


    public void setType(String s) {
        if (s.equals("String") || s.equals("Integer") || s.equals("Double")) m_type = s;
        else m_type = "String";
    }

    public String getType() {
        return m_type;
    }
    public boolean isRequired() {
        return m_isRequired;
    }
    public String getKey() {
        return m_key;
    }
    public String getDefaultValue() {
        return m_defaultValue;
    }


}
