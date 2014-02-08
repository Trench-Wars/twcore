/*
 * AdvancedCommandInterpreter.java
 *
 * Created on March 17, 2004, 1:06 PM
 */

package twcore.core.command;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import twcore.core.BotAction;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 *
 * @author  Stefan / Mythrandir
 */
public class AdvancedCommandInterpreter {

    Map<String, CommandDefinition> m_commandDefinitions;

    /** Creates a new instance of AdvancedCommandInterpreter */
    public AdvancedCommandInterpreter() {
        m_commandDefinitions = Collections.synchronizedMap( new HashMap<String, CommandDefinition>() );
    }


    public void add(CommandDefinition d) {
        m_commandDefinitions.put(d.getTrigger(), d);
    }


    public void handleMessage(Message event) {
        // get the first word from m
        String trigger;
        CommandDefinition cd;
        Command c;
        String m = event.getMessage();
        int sepIndex = m.indexOf(' ');

        if (sepIndex == -1)
            trigger = m.trim().toLowerCase();
        else
            trigger = m.substring(0, sepIndex).trim().toLowerCase();

        cd = m_commandDefinitions.get(trigger);

        if (cd != null)
            if ((cd.getMessageTypes() & event.getMessageType()) != 0) {
                c = new Command(event, cd);

                if (c.isInvalid())
                    // report error
                    BotAction.getBotAction().sendPrivateMessage(c.getName(), c.getInvalidMessage());
                else {
                    // invoke
                    try {
                        Object      parameters[] = { c };
                        Object      methodClass = cd.getMethodClass();
                        Class<?>    parameterTypes[] = { c.getClass() };

                        methodClass.getClass().getMethod( cd.getMethodName(), parameterTypes ).invoke( methodClass, parameters );
                    } catch( Exception e ){
                        Tools.printLog("Could not invoke method '" + cd.getMethodName() + "()' in class " + cd.getMethodClass());
                        Tools.printStackTrace( e );
                    }

                }

            } else System.out.println("no command matched " + trigger);


    }


    public ArrayList<String> getHelpList() {
        ArrayList<String> a = new ArrayList<String>();
        Iterator<String> i = m_commandDefinitions.keySet().iterator();
        String s;
        while (i.hasNext()) {
            s = (String)i.next();
            a.add(m_commandDefinitions.get(s).getHelpMessage());
        }
        return a;
    }


    public String[] getHelpListStringArray() {
        ArrayList<String> a = getHelpList();
        return a.toArray(new String[a.size()]);
    }

}
