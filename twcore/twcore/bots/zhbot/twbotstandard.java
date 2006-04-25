/*
 * portabotTestModule.java
 *
 * Created on March 21, 2002, 4:14 PM
 */

/**
 *
 * @author  harvey
 */

package twcore.bots.zhbot;

import java.util.*;
import twcore.core.*;

public class twbotstandard extends TWBotExtension {
    int specPlayers = 0; //if >0, spec player at X deaths
    LinkedList arenaTasks = new LinkedList();

    /** Creates a new instance of portabotTestModule */
    public twbotstandard() {
    }

    public void handleEvent( Message event ){

        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name )) handleCommand( name, message );
        }
    }

    public int getInteger( String input ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return 1;
        }
    }

    public int getInteger( String input, int defValue ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return defValue;
        }
    }

    public void handleCommand( String name, String message ){
        if( message.startsWith( "!random " )){
            m_botAction.createRandomTeams( getInteger( message.substring(8) ));
        } else if( message.startsWith( "!teams " )){
        	createNumberofTeams( getInteger( message.substring(7) ));
        } else if( message.startsWith( "!setship " )){
            String[] parameters = Tools.stringChopper( message.substring( 8 ).trim(), ' ' );
            try{
                if( parameters.length == 2 ){
                    int freq = Integer.parseInt(parameters[0]);
                    int shiptype = Integer.parseInt(parameters[1]);
                    m_botAction.changeAllShipsOnFreq( freq, shiptype );
                } else if( parameters.length == 1 ){
                    int shiptype = Integer.parseInt( parameters[0] );
                    m_botAction.changeAllShips( shiptype );
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!setfreq " )){
            String[] parameters = Tools.stringChopper( message.substring( 8 ).trim(), ' ' );
            try{
                if( parameters.length == 2 ){
                    int freq = Integer.parseInt(parameters[0]);
                    int shiptype = Integer.parseInt(parameters[1]);
                    m_botAction.setFreqtoFreq( freq, shiptype );
                } else if( parameters.length == 1 ){
                    int freq = Integer.parseInt( parameters[0] );
                    m_botAction.setAlltoFreq( freq );
                }
            }catch( Exception e ){}
        } else if( message.startsWith( "!restart" )){
            m_botAction.sendUnfilteredPublicMessage("*restart");
        } else if( message.startsWith( "!merge ")) {
            String[] parameters = Tools.stringChopper( message.substring( 7 ).trim(), ' ' );
            try{
                if( parameters.length == 2 ){
                    m_botAction.setFreqtoFreq(Integer.parseInt(parameters[0]), Integer.parseInt(parameters[1]));
                }
            }catch( Exception e ){}
        } 
    }
    
    public void killEasy() {
    	Iterator it = m_botAction.getPlayingPlayerIterator();
    	while(it.hasNext()) {
    		Player p = (Player)it.next();
    		Ship s = m_botAction.getShip();
    		s.moveAndFire(p.getXLocation(), p.getYLocation(), s.getWeaponNumber((byte)4, (byte)2, false, false, true, (byte)8, false));
    	}
    //	m_botAction.setPlayerPositionUpdating(201);
    	m_botAction.setPlayerPositionUpdating(0);
    }

    /** Creates the requested number of teams if possible
	 * @param 
	 */
	private void createNumberofTeams( int _teams ) {
		
		int current = 0;
		int howmany = _teams - 1;
		
		Iterator i = m_botAction.getPlayingPlayerIterator();
		while( i.hasNext() ) {
			
			if(current > howmany)
				current = 0;
			Player p = (Player)i.next();
			m_botAction.setFreq( p.getPlayerID(), current );
			current++;
		}
	}

	public void handleEvent( PlayerDeath event ){
        if( specPlayers <= 0 ) return;

        Player p = m_botAction.getPlayer( event.getKilleeID() );

        if( p.getLosses() >= specPlayers ){
            m_botAction.spec( event.getKilleeID() );
            m_botAction.spec( event.getKilleeID() );

            m_botAction.sendArenaMessage( m_botAction.getPlayerName( event.getKilleeID() ) + " is out with " + p.getWins() + " wins, " + p.getLosses() + " losses." );
        }
    }

    public void handleWarp( String name, String message ){
/*          "!warp                     - Warps everyone to a random location",
            "!warp <freq>              - Warps everyone on <freq> to a random location",
            "!warpto <x> <y>           - Warps everyone in the arena to x,y",
            "!warpto <freq> <x> <y>    - Warps everyone on freq <freq> to x,y",
            "!warpto <area>            - Warps everyone in the arena to predefined area <area>",
            "!warpto <freq> <area>     - Warps everyone on freq <freq> to predefined area <area>",
            "!where                    - The bot will tell you where you are on the map (DEBUGGING)"*/

        String[] parameters = Tools.stringChopper( message.trim().substring( 7 ), ' ' );

        if( parameters == null ){
            return;
        }

        try{
            if( parameters.length == 2 ){
                int x = Integer.parseInt(parameters[0]);
                int y = Integer.parseInt(parameters[1]);
                m_botAction.warpAllToLocation( x, y );
            } else if( parameters.length == 3 ){
                int freq = Integer.parseInt( parameters[0] );
                int x = Integer.parseInt( parameters[1] );
                int y = Integer.parseInt( parameters[2] );
                m_botAction.warpFreqToLocation( freq, x, y );
            }
        }catch( Exception e ){}
    }

    public void handleAddMsg( String name, String message ){
        int soundCode;
        int pos;
        int interval = 0;

        if( arenaTasks.size() >= 15 ){
            m_botAction.sendPrivateMessage( name, "There is a max of 15 arena messages. You will need to remove some if you wish to use another");
            return;
        }

        message = message.trim();
        if( message != null ){
            pos = message.indexOf(',');
            if( pos != -1 ){
                interval = getInteger( message.substring( 0, pos ), 0 );
                if( interval < 1 || interval > 3600 ){
                    m_botAction.sendPrivateMessage( name, "Invalid interval. It must be between 1 and 3600 seconds" );
                    return;
                }

                message = message.substring( pos + 1 );
                message = message.trim();
                interval = (interval * 1000);
            } else {
                m_botAction.sendPrivateMessage( name, "Invalid interval. Use !addmsg <interval>,<message>" );
                return;
            }

            if( message.equals("") || message == null ){
                m_botAction.sendPrivateMessage( name, "Please use !addmsg <interval>,<message>" );
                return;
            }

            pos = message.lastIndexOf('%');
            if( pos != -1 ){
                int temp = (message.length() - pos);
                if( temp <= 4 ){
                    soundCode = getInteger( message.substring( pos + 1 ), 0 );
                    if( soundCode < 1 || soundCode > 999 ){
                        m_botAction.sendPrivateMessage( name, "Invalid sound number" );
                        return;
                    }

                    message = message.substring( 0, pos );

                    arenaTasks.add( new ArenaMsgTask( message, soundCode ) );
                    m_botAction.scheduleTaskAtFixedRate( (ArenaMsgTask)arenaTasks.getLast(), 0, interval );
                } else {
                    m_botAction.sendPrivateMessage( name, "Invalid sound number" );
                    return;
                }
            } else {
                arenaTasks.add( new ArenaMsgTask( message ) );
                m_botAction.scheduleTaskAtFixedRate( (ArenaMsgTask)arenaTasks.getLast(), 0, interval );
            }
        } else {
            m_botAction.sendPrivateMessage( name, "Please use !addmsg <interval>,<message>" );
        }
    }

    public void handleListMsg( String name ){
        ArenaMsgTask task;

        if( arenaTasks.size() == 0 ){
            m_botAction.sendPrivateMessage( name, "There currently are no scheduled messages" );
        }

        for( int i = 0; i < arenaTasks.size(); i++ ){
            task = (ArenaMsgTask)arenaTasks.get(i);
            String message = i + ". " + task.getMessage();
            if( task.getSoundCode() != 0 ){
                message += "%" + task.getSoundCode();
            }
            m_botAction.sendPrivateMessage( name, message );
        }
    }

    public void handleClearMsgs( String name ){
        clearArenaMsgs();
        m_botAction.sendPrivateMessage( name, "All repeating arena messages have been cleared" );
    }

    public void clearArenaMsgs(){
        for( int i = 0; i < arenaTasks.size(); i++){
            ((ArenaMsgTask)arenaTasks.get(i)).cancel();
        }
        arenaTasks.clear();
    }

    public void handleDelMsg( String name, int index ){
        ArenaMsgTask task;

        try{
            task = (ArenaMsgTask)arenaTasks.get(index);
        } catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Invalid message number" );
            return;
        }

        arenaTasks.remove(index);
        task.cancel();

        String message = "Removed: " + task.getMessage();
        if( task.getSoundCode() != 0 ){
            message += "%" + task.getSoundCode();
        }
        m_botAction.sendPrivateMessage( name, message );
    }

    public class ArenaMsgTask extends TimerTask{
        String message;
        int soundCode = 0;

        public ArenaMsgTask( String newMessage ){
            message = newMessage;
        }

        public ArenaMsgTask( String newMessage, int newSoundCode ){
            message = newMessage;
            soundCode = newSoundCode;
        }

        public void run(){
            if( soundCode != 0 ){
                m_botAction.sendArenaMessage( message, soundCode );
            } else {
                m_botAction.sendArenaMessage( message );
            }
        }

        public String getMessage(){
            return message;
        }

        public int getSoundCode(){
            return soundCode;
        }
    }

    public String[] getHelpMessages() {
        String[] help = {
            "!random <numberFreqs>     - Makes random freqs of a particular size.",
			"!teams <numberTeams>      - Makes the requested number of teams.",
            "!where                    - Robo will tell you his location. Remote PM or squad msg only.",
            "!setship <ship>           - Changes everyone to <ship>",
            "!setship <freq> <ship>    - Changes everyone on <freq> to <ship>",
            "!restart                  - Restarts the ball game",
            "!merge <freq1> <freq2>    - Changes everyone on <freq1> to <freq2>"
        };
        return help;
    }

    public void cancel() {
        clearArenaMsgs();
    }
}
