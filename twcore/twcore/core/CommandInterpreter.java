package twcore.core;

import java.util.*;
import java.lang.*;

class Command {

    Object          m_methodClass;
    String          m_methodName;
    int             m_messageTypes;

    public Command( int messageTypes, Object methodClass, String methodName ){

        m_methodName = methodName;
        m_methodClass = methodClass;
        m_messageTypes = messageTypes;
    }

    public Object getMethodClass(){
        
        return m_methodClass;
    }

    public String getMethodName(){
        
        return m_methodName;
    }

    public int getMessageTypes(){
        
        return m_messageTypes;
    }
}

public class CommandInterpreter {
    Map             m_commands;
    BotAction       m_botAction;
    int             m_allCommandTypes;

    public CommandInterpreter( BotAction botAction ){

        m_allCommandTypes = 0;
        m_botAction = botAction;
        m_commands = Collections.synchronizedMap( new HashMap() );
    }

    public void registerCommand( String trigger, int messageTypes, Object methodClass, String methodName ){

        m_allCommandTypes |= messageTypes;
        m_commands.put( trigger.toLowerCase(), new Command( messageTypes, methodClass, methodName ) );
    }

    public void registerDefaultCommand( int messageType, Object methodClass, String methodName ){

        registerCommand( "default" + messageType, messageType, methodClass, methodName );
    }

    public boolean handleEvent( Message event ){
        boolean     result;
        String      trigger;
        Command     command;
        int         seperatorIndex;

        String      message;
        String      messager;
        String      methodName;
        int         messageType;

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
                }

                try {
                    Object      parameters[] = { messager, message };
                    Object      methodClass = command.getMethodClass();
                    Class       parameterTypes[] = { messager.getClass(), message.getClass() }; 

                    methodClass.getClass().getMethod( command.getMethodName(), parameterTypes ).invoke( methodClass, parameters );
                    result = true;
                } catch( Exception e ){
                    Tools.printStackTrace( e );
                }
            }
        }

        return result;
    }
}
