/*
 * twbothunt.java
 *
 * Created on March 9, 2002, 8:36 PM
 */

/**
 *
 * @author  thomas
 *
 */
package twcore.bots.twbot;

import java.util.*;
import twcore.core.*;
import twcore.core.events.*;

public class twbothunt extends TWBotExtension {
    int specPlayers = 5; //default unless changed by host
    private HashMap data;
    private LinkedList keys;
    private boolean gameStarted = false;
    private String mvpName;
    private int mvpScore;
    private int huntReward = 5; //default unless changed by host
    private int huntPenalty = 2; //default unless changed by host
    private int preyReward = 1; //default unless changed by host
    

     /** Creates a new instance of twbothunt */
    public twbothunt() {
    }
    
    public void startHunt ( String hostName ){
    	Player p;
        String addPlayerName;
        mvpScore = 0; //reset from previous game
    	data = new HashMap();
        keys = new LinkedList();
    	PlayerBag huntPlayerBag = new PlayerBag();
    	
    	Iterator i = m_botAction.getPlayingPlayerIterator();
        if( i == null ) return;
        while( i.hasNext() ){
       	    p = (Player)i.next();
            addPlayerName = p.getPlayerName();
            //HuntPlayer temp;
            
            huntPlayerBag.add(addPlayerName);
        }

        int y = huntPlayerBag.size(); //need to take a snapshot
        for( int x = 0; x < y; x++){
            addPlayerName = huntPlayerBag.grab();
            
            //Store playerName in the HuntPlayer class to preserve caps
            data.put(addPlayerName.toLowerCase(), new HuntPlayer(addPlayerName));
            
            //do it this way or else the list will sort them when it imports and we will lose randomization
            keys.addLast(addPlayerName.toLowerCase());

            //m_botAction.sendChatMessage( "++++++" + addPlayerName );
        }
        
        /*debug code
        HuntPlayer temp;
        for( int x = 0; x <= keys.size() - 1; x++ ){
            temp = (HuntPlayer)data.get(keys.get(x));
            m_botAction.sendChatMessage( x + ": " + keys.get(x).toString() );
        }*/
        
        if ( keys.size() < 2 ){
            m_botAction.sendPrivateMessage( hostName, "Cannot start game with less then 2 people in play." );
            return;
        }
        
        m_botAction.sendArenaMessage( "Hunt mode activated by " + hostName + ". Type :" + m_botAction.getBotName() + ":!prey if you forget who you are hunting." );
        m_botAction.sendArenaMessage( "Removing players with " + specPlayers + " non-hunted deaths." );
        gameStarted = true;
        
        /*causes problem, warps everyone then starts the game right away
          either have the host do it manually or insert a timer.
          
          m_botAction.createRandomTeams( 1 ); //in case host forgot
        */
        m_botAction.sendUnfilteredPublicMessage( "*scorereset" ); //in case host forgot
        tellArenaPreyName();
    }
    
    public void endHunt( String winnerName ){
    	HuntPlayer winner;

        checkMVP(winnerName);
    	winner = (HuntPlayer)data.get( winnerName.toLowerCase() );
    	
        gameStarted = false;
        m_botAction.sendArenaMessage ("GAME OVER!", 5);
        m_botAction.sendArenaMessage ("Survivor is: " + winner.getName() + " (" + winner.getPoints() + " points)");
        m_botAction.sendArenaMessage ("MVP is: " + mvpName + " (" + mvpScore + " points)");
    }

    public Integer findScore( String playerName ){
        if (gameStarted){
            HuntPlayer tempPlayer;
            
            try{
                tempPlayer = (HuntPlayer)data.get( playerName.toLowerCase() );
                return new Integer(tempPlayer.getPoints());
            } catch( Exception e ){
                return null;
            }
    	}
    	return null;
    }

    public void tellScore( String playerName ){
    	tellScore( playerName, playerName );
    }

    public void tellScore( String sendtoName, String lookupName ){
    	Integer score;
        score = findScore( lookupName );

        if(score != null){
            if( lookupName.equals(sendtoName) ){
                m_botAction.sendPrivateMessage( sendtoName, "Your current score: " + score );
            } else {
            	m_botAction.sendPrivateMessage( sendtoName, "Player score: " + score );
            }
        } else {
            m_botAction.sendPrivateMessage( sendtoName, "Player score not found" );
        }    	
    }

    public String findPreyName( String hunterName ){
    	if (gameStarted){
    	    int i;
    	    HuntPlayer prey;
    	
    	    i = keys.indexOf( hunterName.toLowerCase() );
    	    if (i > 0){
    	    	prey = (HuntPlayer)data.get(keys.get( --i ));
    	    	return prey.getName();
    	    } else if (i == 0){
    	    	prey = (HuntPlayer)data.get(keys.get( keys.size() - 1 ));
    	    	return prey.getName();    	        
    	    }    	    
    	}
    	return null;
    }
    
    public void tellPreyName( String playerName ){
    	String preyName;
    	
    	preyName = findPreyName( playerName );
    	if (preyName != null){
    	    m_botAction.sendPrivateMessage( playerName, "Your current prey is: " + preyName);
    	} else {
            m_botAction.sendPrivateMessage( playerName, "You are currently not assigned a prey.");
    	}
    }
    
    public void tellArenaPreyName(){
        for( int i = 0; i < keys.size(); i++){
          tellPreyName( keys.get(i).toString() );	
        }
    }
    
    public void checkMVP( String playerName ){
        HuntPlayer checkPlayer;
        checkPlayer = (HuntPlayer)data.get(playerName.toLowerCase());
    	    	
        if (mvpScore <= checkPlayer.getPoints()){
            mvpName = checkPlayer.getName();
    	    mvpScore = checkPlayer.getPoints();
        }
    }

    public void removeHuntPlayer( String playerName ){
    	int i = keys.indexOf( playerName.toLowerCase() );
    	
    	if (gameStarted){ 
    	    if ( i != -1 ){
    	    	checkMVP( playerName );
    	    	
    	    	keys.remove( playerName.toLowerCase() );
                //m_botAction.sendChatMessage( playerName + " has been removed from the chain." );
                
                if (keys.size() > 1){
                    //In case they are the last person on the list
                    if ( i == keys.size() ) { i = 0; }
                    
                    //No need to go to the next on list since it will replace the one we just removed
                    tellPreyName( keys.get( i ).toString() );
                }
            }
            
            if (keys.size() == 1) {
            	endHunt( keys.getFirst().toString() );
            }
        }
    }
    
    public void handleEvent( Message event ){
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( m_opList.isER( name ) ) {
            	handleCommand( name, message );
            } else {
                handlePublicCommand( name, message );	
            }
        }
    }
    
    public void handleEvent( PlayerDeath event ){
        Player pKilled = m_botAction.getPlayer( event.getKilleeID() );
        Player pKiller = m_botAction.getPlayer( event.getKillerID() );
        
        if( pKilled == null || pKiller == null )
            return;
        
        String killedName = pKilled.getPlayerName();
        String killerName = pKiller.getPlayerName();
        
        if (gameStarted){
            HuntPlayer tempPlayer;

            if ( killedName.equals( findPreyName( killerName ))){ //Hunter kills prey
                m_botAction.spec( event.getKilleeID() );
                m_botAction.spec( event.getKilleeID() );
            
                tempPlayer = (HuntPlayer)data.get( killedName.toLowerCase() );
                if(tempPlayer != null){
                    m_botAction.sendArenaMessage( killedName + " (" + tempPlayer.getPoints() + " points) has been hunted and killed by " + killerName );

                    tempPlayer = (HuntPlayer)data.get( killerName.toLowerCase() );
                    tempPlayer.addHuntKill();
                    tempPlayer.addPoints(huntReward);
                    m_botAction.sendPrivateMessage( killerName, "Successful hunt! " + huntReward + " points added to your score." );
                }
                return;
            } else if( killerName.equals( findPreyName( killedName ) ) ) { //Prey kills hunter
            	tempPlayer = (HuntPlayer)data.get( killerName.toLowerCase() );
            	
            	if(tempPlayer != null){
            	    tempPlayer.addPoints(preyReward);
            	    m_botAction.sendPrivateMessage( killerName, "You killed your hunter! " + preyReward + " points added to your score." );
            	}
            } else { //Player kills wrong person
            	tempPlayer = (HuntPlayer)data.get( killerName.toLowerCase() );
            	
            	if(tempPlayer != null){
            	    tempPlayer.remPoints(huntPenalty);
                    m_botAction.sendPrivateMessage( killerName , "You killed the wrong person! " + huntPenalty + " points deducted from your score." );                    
                }                
            }
            
      	    if ( pKilled.getLosses() >= specPlayers ){
                m_botAction.spec( event.getKilleeID() );
                m_botAction.spec( event.getKilleeID() );

                tempPlayer = (HuntPlayer)data.get( killedName.toLowerCase() );
                m_botAction.sendArenaMessage( killedName + " is out with " + tempPlayer.getPoints() + " points, " + pKilled.getLosses() + " losses." );
            }
        }
    }
    
    public void handleEvent( PlayerLeft event ){
    	if (gameStarted){
            removeHuntPlayer( m_botAction.getPlayerName( event.getPlayerID() ) );
        }
    }
    
    public void handleEvent( FrequencyShipChange event ){
    	if (gameStarted){
    	    String name = m_botAction.getPlayerName( event.getPlayerID() );
    	    int ship = event.getShipType();
    	    if ( ship == 0 ){
    	        removeHuntPlayer( name );	
    	    } else {
    	    	//Keep players out of the game who aren't on the list
    	    	if( keys.indexOf( name.toLowerCase() ) == -1 ){
    	    	    m_botAction.spec( event.getPlayerID() );
    	    	    m_botAction.spec( event.getPlayerID() );
                    m_botAction.sendChatMessage( "Hunt Error: Unregistered player (" + name + ") forced to spec" );
                    m_botAction.sendPrivateMessage( name, "You cannot join a Hunt game while it is in progress" );
    	    	}
    	    }
    	}
    }
    
    public void handleCommand( String name, String message ){
        if( message.toLowerCase().startsWith( "!huntspec " )){
            if(gameStarted){
            	m_botAction.sendPrivateMessage( name, "Cannot change !huntspec while a game is in progress" );
            } else {
            	if( getInteger( message.substring( 10 )) < 1 ){
            	    m_botAction.sendPrivateMessage( name, "The !huntspec cannot be less then 1" );
            	} else {
                    specPlayers = getInteger( message.substring( 10 ));
                    m_botAction.sendPrivateMessage( name, "Non-hunted death limit set to: " + specPlayers );
                }
            }
        } else if( message.toLowerCase().startsWith( "!starthunt" )){
            startHunt( name );
        } else if( message.toLowerCase().equals( "!stophunt" )){
            if(gameStarted){
                gameStarted = false;
                m_botAction.sendArenaMessage( "Hunt mode deactivated by " + name );
            }
        } else if( message.toLowerCase().startsWith( "!huntreward" ) ){
            if(gameStarted){
                m_botAction.sendPrivateMessage( name, "Cannot change !huntreward while a game is in progress" );
            } else {
                huntReward = getInteger( message.substring( 12 ));
                m_botAction.sendPrivateMessage( name, "Hunt kill reward set to: " + huntReward );
            }
        } else if( message.toLowerCase().startsWith( "!huntpenalty" ) ){
            if(gameStarted){
                m_botAction.sendPrivateMessage( name, "Cannot change !huntpenalty while a game is in progress" );
            } else {
                huntPenalty = getInteger( message.substring( 13 ));
                m_botAction.sendPrivateMessage( name, "Non-hunt kill penalty set to: " + huntPenalty );
            }
        } else if( message.toLowerCase().startsWith( "!preyreward" ) ){
            if(gameStarted){
                m_botAction.sendPrivateMessage( name, "Cannot change !preyreward while a game is in progress" );
            } else {
                preyReward = getInteger( message.substring( 12 ));
                m_botAction.sendPrivateMessage( name, "Prey killing hunter reward set to: " + preyReward );
            }
        } else {
            handlePublicCommand( name, message );	
        }
    }
    
    public void handlePublicCommand( String name, String message){
        if( message.toLowerCase().startsWith( "!prey" ) ){
            if(gameStarted){tellPreyName( name );}
        } else if( message.toLowerCase().equals( "!scoreleader" ) ){
            if(gameStarted){
            	if( mvpName != null ){
                    m_botAction.sendPrivateMessage( name, "Current score leader is: " + mvpName + " with: " + mvpScore + " points" );
                } else {
                    m_botAction.sendPrivateMessage( name, "There currently is no leader" );
                }
            };
        } else if( message.toLowerCase().equals( "!score" ) ){
            if(gameStarted){tellScore( name );}
        } else if( message.toLowerCase().startsWith( "!score " ) ){
            if(gameStarted){
            	tellScore( name, message.substring(7).trim() );
            }
        }
    }

    public String[] getHelpMessages() {
        String[] help = {
            "Commands for the hunt module",
            "!huntspec <numdeaths>  - Spec players who reach this death count before being hunted. (Default is 5)",
            "!huntreward <points>   - Set how many points rewarded for a hunt kill. (Default 5)",
            "!huntpenalty <points>  - Set how many points deducted for a non-hunt kill. (Default 2)",
            "!preyreward  <points>  - Set how many points rewarded to the prey for killing their hunter. (Default 1)",
            "!starthunt             - Starts the hunt. Make sure everyone who wishes to play is already in a ship.",
            "                         Do not let people in the game late and do not let lagouts in. It won't work.",
            "!stophunt              - Stops the hunt.",
            "!prey                  - (Public Command) Tells who you should be hunting if you forget.",
            "!score <name>          - (Public Command) Checks your score or score of another if specified.",
            "!scoreleader           - (Public Command) Tells who currently has highest score."
        };
        return help;
    }
    
    public int getInteger( String input ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return 1;
        }
    }

    public void cancel(){
    
    }
}

class HuntPlayer {
    int huntKills = 0;
    int score = 0;
    String name;
    
    public HuntPlayer(){
    }
    
    public HuntPlayer( String playerName ){
    	name = playerName;
    }
    
    public String getName(){
        return name;	
    }
    
    public void addPoints( int amount ){
        score += amount;
    }
    
    public void remPoints( int amount ){
        score -= amount;
    }
    
    public int getPoints(){
        return score;	
    }
    
    public void addHuntKill(){
       huntKills++;
    }
    
    public int getHuntKills(){
       return huntKills;
    }
}

class PlayerBag {
    ArrayList list;

    public PlayerBag(){
        list = new ArrayList();
    }

    public PlayerBag( String string ){
        this();
        add( string );
    }

    public void clear(){
        list.clear();
    }    
    
    public void add( String string ){
        list.add( string );
    }
    
    public List getList(){
        return (List)list;
    }

    public String grab(){
    	if( isEmpty() ){
            return null;
        } else {
            int i = random( list.size() );
            String grabbed;
            
            grabbed =(String)list.get( i ) ;
            list.remove( i );
            return grabbed;
        }
    }
    
    public int size(){
        return list.size();
    }
    
    private int random( int maximum ){
        return (int)(Math.random()*maximum);        
    }
    
    public boolean isEmpty(){
        return list.isEmpty();
    }

    public String toString(){
        return grab();
    }
}