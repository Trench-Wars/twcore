package twcore.bots.multibot.gravbomber;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.events.WatchDamage;
import twcore.core.events.WeaponFired;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

public class gravbomber extends MultiModule {
    BotSettings         m_botSettings;
    HashMap<String, GBPlayer> m_playerData;
    HashMap<String, TurnTask> m_turnTasks;
    LinkedList<String>   m_playerList;
    LinkedList<GBWeapon> m_weapons;
    Player              m_currentPlayer;
    GBPlayer            m_currentGBPlayer;
    int                 m_currentWeapon;
    int                 m_currentTurnDamage;
    int                 m_currentPlayerIndex;
    int                 m_turnTime;
    boolean             m_gameStarted;
    boolean             m_boughtWeapon;
    Timer               m_timer;
    final int           WEAP_ROCKET=0;
    final int           WEAP_L1BOMB=1;
    final int           WEAP_TRACER=2;
    final int           WEAP_THRUSTBOMB=3;
    final int           WEAP_L1BULLET=4;
    final int           WEAP_L3BOMB=5;
    final int           WEAP_BOUNCEBOMB=6;
    final int           WEAP_BURST=7;
    final int           WEAP_SHRAPBOMB=8;
    final int           WEAP_THOR=9;
    final int           WEAP_REPEL=10;
    boolean             m_debug = false;

    /* Initialization Code */
    public void init() {
        m_botSettings = moduleSettings;
        setupWeapons();
        m_botAction.sendUnfilteredPublicMessage( "?chat=" + m_botAction.getGeneralSettings().getString( "Chat Name" ) + ",spamchat" );
        m_botAction.sendUnfilteredPublicMessage( "*relkills 1" );
        
        int time = m_botSettings.getInt("TurnTime");
        if( time < 5000 ) {
            m_botAction.sendChatMessage("Invalid turn time set for GravBomber (<5000ms)!  Ensure CFG is set up properly.  Dying...");
            m_botAction.die();
        } else {
            m_turnTime = time;
        }
    }

    public void requestEvents(ModuleEventRequester req) {
		req.request( this, EventRequester.WEAPON_FIRED );
		req.request( this, EventRequester.WATCH_DAMAGE );
		req.request( this, EventRequester.PLAYER_LEFT );
		req.request( this, EventRequester.FREQUENCY_SHIP_CHANGE );
		req.request( this, EventRequester.PLAYER_DEATH );
	}

    public long getRequestedEvents(){
        return ( EventRequester.MESSAGE | EventRequester.FREQUENCY_SHIP_CHANGE | EventRequester.PLAYER_LEFT | EventRequester.PLAYER_DEATH | EventRequester.WATCH_DAMAGE | EventRequester.WEAPON_FIRED );
    }

    public void handleEvent( Message event ){
        if( event == null )
            return;
        Player p = m_botAction.getPlayer( event.getPlayerID() );
        if( p == null )
            return;
    	String name = p.getPlayerName();
        String message = event.getMessage();

        if( message.startsWith("!") ) {
            if( event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.PUBLIC_MESSAGE || event.getMessageType() == Message.PUBLIC_MACRO_MESSAGE ){
                handleCommand( name, message, event.getMessageType() );
            }
        } else if( event.getMessageType() == Message.ARENA_MESSAGE ) {
            handleArena( message );
        }
    }

    public void handleCommand( String name, String message, int messageType ){
    	//Host Commands
    	if( opList.isER(name) && messageType == Message.PRIVATE_MESSAGE ){
            if( message.toLowerCase().equals("!die") ){
                if(!m_gameStarted){
                    m_botAction.sendPrivateMessage( name, "Unloading..." );
                    m_botAction.die();
                } else {
                    m_botAction.sendPrivateMessage( name, "There is currently a game in progress. Please !stop it first" );
                }
            } else if( message.toLowerCase().equals("!start") ){
                startGame( name );
            } else if( message.toLowerCase().equals("!stop") ){
                if(m_gameStarted){
                  stopGame();
    	          m_botAction.sendArenaMessage("GravBomber deactivated by " + name );
    	        } else {
    	          m_botAction.sendPrivateMessage( name, "There is not an active game" );
    	        }
            } else if( message.toLowerCase().equals("!skip") ){
                endTurn( m_currentPlayer.getPlayerName(), true, true );
            } else if( message.toLowerCase().equals("!debug") ){
                m_debug = !m_debug;
                m_botAction.sendPrivateMessage( name, "Debug: " + m_debug );
            } else if( message.toLowerCase().startsWith( "!go " ) )
            	m_botAction.joinArena( message.substring( 4 ) );
        }

        //User Commands (bot will listen to pub chat)
        if( messageType == Message.PRIVATE_MESSAGE || messageType == Message.PUBLIC_MESSAGE || messageType == Message.PUBLIC_MACRO_MESSAGE ){
            if( message.toLowerCase().equals("!help") ){
                handleHelp( name );
            } else if( message.toLowerCase().equals("!end") ){
                if( m_currentPlayer.getPlayerName().equals( name ) ){
                    endTurn( name, true, true );
                } else {
                    m_botAction.sendPrivateMessage( name, "It is not your turn" );
                }
            } else if( message.toLowerCase().startsWith("!power ") ){
                if(m_gameStarted){
                    setPower( name, getInteger( message.substring(7), 500 ) );
                }
            } else if( message.toLowerCase().startsWith("!pow ") ){
                if(m_gameStarted){
                    setPower( name, getInteger( message.substring(5), 500 ) );
                }
            } else if( message.toLowerCase().equals("!liston") ){
                toggleList( name, true );
            } else if( message.toLowerCase().equals("!listoff") ){
                toggleList( name, false );
            } else if( message.toLowerCase().startsWith("!buy ") ){
                if(m_gameStarted){
                    if( m_currentPlayer.getPlayerName().toLowerCase().equals( name.toLowerCase() ) ){
                        buyWeapon( name, getInteger( message.substring(5), -1 ) );
                    } else if ( m_playerList.indexOf( name ) != -1 ) {
                        m_botAction.sendPrivateMessage( name, "You must wait until it is your turn before buying a weapon" );
                    }
                }
            } else if( message.toLowerCase().equals("!quit") ){
            	Player player = m_botAction.getPlayer( name );
            	if( !player.isPlaying() ) return;

                if(m_gameStarted){
                    if( m_playerList.indexOf(name) != -1 ){
                        remPlayer( name, true, true );
                        m_botAction.sendPrivateMessage( name, "You have been removed from the game" );
                    } else {
                        m_botAction.spec( name );
                        m_botAction.spec( name );
                    }
                } else {
                    m_botAction.spec( name );
                    m_botAction.spec( name );
                }
            }
        }
    }

    public void handleEvent( WeaponFired event ){
    	if(!m_gameStarted) return;
    	String playerName =  m_botAction.getPlayerName( event.getPlayerID() );

    	if( m_currentPlayer.getPlayerName().equals( playerName ) ){
    	    if( m_currentWeapon != WEAP_TRACER ){
    	        ((TimerTask)m_turnTasks.get( playerName )).cancel();

                m_turnTasks.put( playerName, new TurnTask(playerName) );

                if( m_currentWeapon == WEAP_L1BULLET || m_currentWeapon == WEAP_BURST ){
        	    m_timer.schedule( (TimerTask)m_turnTasks.get(playerName), m_botSettings.getInt("BulletAliveTime") );
        	    //debug
                    if(m_debug){
                        m_botAction.sendChatMessage( 2, "Timer schedule for " + playerName + " Interval: " + m_botSettings.getInt("BulletAliveTime") );
                    }
        	} else if( m_currentWeapon == WEAP_REPEL ) {
        	    m_timer.schedule( (TimerTask)m_turnTasks.get(playerName), m_botSettings.getInt("RepelAliveTime") );
        	    //debug
                    if(m_debug){
                        m_botAction.sendChatMessage( 2, "Timer schedule for " + playerName + " Interval: " + m_botSettings.getInt("RepelAliveTime") );
                    }
        	} else {
        	    m_timer.schedule( (TimerTask)m_turnTasks.get(playerName), m_botSettings.getInt("BombAliveTime") );
        	    //debug
                    if(m_debug){
                        m_botAction.sendChatMessage( 2, "Timer schedule for " + playerName + " Interval: " + m_botSettings.getInt("BombAliveTime") );
                    }
        	}
            }
    	}
    }

    public void handleEvent( WatchDamage event ){
        if(!m_gameStarted) return;

        String playerName = m_botAction.getPlayerName( event.getAttacker() );

        //debug
        if(m_debug) m_botAction.sendChatMessage( 2, "Hit watchdamage for: " + m_botAction.getPlayerName( event.getVictim() ) + "(" + event.getVictim() + ") Attacker: " + playerName + "(" + event.getAttacker() + ") Damage: " + event.getEnergyLost() );

        if( m_playerList.indexOf( playerName ) != -1 && !playerName.equals( m_botAction.getPlayerName( event.getVictim() ) ) && event.getEnergyLost() > 0 ){
            m_botAction.sendUnfilteredPrivateMessage( playerName, "*points " + event.getEnergyLost() );
            m_currentTurnDamage += event.getEnergyLost();
        }

        if( m_currentPlayer.getPlayerName().equals( playerName ) ) {
            if( m_currentWeapon != WEAP_TRACER && m_currentWeapon != WEAP_BURST ){
            	((TimerTask)m_turnTasks.get( playerName )).cancel();
            	m_turnTasks.put( playerName, new TurnTask(playerName) );
            	m_timer.schedule( (TimerTask)m_turnTasks.get(playerName), 1500 );

                //endTurn( playerName, true );
            }
        }
    }

    public void handleArena( String message ){
    	if(!m_gameStarted) return;

        //debug
        if(m_debug) m_botAction.sendChatMessage( 2, message );

        if ( message.indexOf(" picked up ") != -1 ){
            String playerName;
            playerName = message.substring( 0, message.lastIndexOf(" picked up ") );
            int points = 0;

            if( m_playerList.indexOf( playerName ) != -1 ){
                if( message.endsWith(" Energy") ){
                    points = m_botSettings.getInt("EnergyPoints");
                } else if( message.endsWith(" Rotation") ){
                    points = m_botSettings.getInt("RotationPoints");
                } else if( message.endsWith(" Top Speed") ){
                    points = m_botSettings.getInt("TopSpeedPoints");
                }

                if(points != 0){
                    m_botAction.sendArenaMessage( playerName + " has picked up a bonus " + points + " points!" );
                    m_botAction.sendUnfilteredPrivateMessage( playerName, "*points " + points);
                }
            }
        }
    }

    public void handleEvent( PlayerLeft event ){
        if(m_gameStarted){
            remPlayer( m_botAction.getPlayerName( event.getPlayerID() ), false, true );
        }
    }

    public void handleEvent( FrequencyShipChange event ){
        if (m_gameStarted){
            if( event.getShipType() == 0 ){
                remPlayer( m_botAction.getPlayerName( event.getPlayerID() ), true, true );
            }
        }
    }

    public void handleEvent( PlayerDeath event ){
   	if(!m_gameStarted) return;

    	Player playerKilled = m_botAction.getPlayer( event.getKilleeID() );
    	Player playerKiller = m_botAction.getPlayer( event.getKillerID() );

    	//debug
    	if(m_debug){m_botAction.sendChatMessage( 2, playerKilled.getPlayerName() + " killed by " + playerKiller.getPlayerName() );}

    	m_botAction.sendUnfilteredPrivateMessage( playerKiller.getPlayerName(), "*points " + m_botSettings.getInt("KillBonusPoints") );
    	m_botAction.sendArenaMessage( playerKilled.getPlayerName() + " eliminated by " + playerKiller.getPlayerName() + ". " + m_botSettings.getInt("KillBonusPoints") + " bonus points awarded!" );
    	remPlayer( playerKilled.getPlayerName(), true, true );
    }

    public void handleHelp( String name ){
    	String[] helpText;

        helpText = new String[] {
            "Player Commands: (can be sent publicly or privatly)",
            "!end                - Ends your turn if you don't want to wait for the timer to run out",
            "!liston/!listoff    - Toggle the display of the shopping list window",
            "!buy (num)          - Will purchase that weapon if it is your turn and you have enough points",
            "!power/!pow (num)   - Adjust the power of your shot",
            "!quit               - Use this command if you wish to leave the arena/game and do not have enough energy"
        };

        m_botAction.privateMessageSpam( name, helpText );
    }

    public  String[] getModHelpMessage() {
    	String[] helpText = new String[] {
            	"Host Commands:",
                "!start              - Starts the game",
                "!stop               - Stops the game",
                "!skip               - Used for the host to skip another player's turn if they are taking too long",
                "!die                - Unload bot"
            };
        return helpText;
    }

    public boolean isUnloadable() {
    	return true;
    }
    
    public void cancel() {
    	stopGame();
    }

    /* Game Code */
    public void setupWeapons(){
    	m_weapons = new LinkedList<GBWeapon>();
        m_weapons.add( WEAP_ROCKET, new GBWeapon("Grav Inverter", m_botSettings.getInt("RocketPrice") ) );
        m_weapons.add( WEAP_L1BOMB, new GBWeapon("Normal Bomb", m_botSettings.getInt("L1BombPrice") ) );
        m_weapons.add( WEAP_TRACER, new GBWeapon("Tracer Orbs", m_botSettings.getInt("TracerPrice") ) );
        m_weapons.add( WEAP_THRUSTBOMB, new GBWeapon("Inverter Shell", m_botSettings.getInt("ThrustBombPrice") ) );
        m_weapons.add( WEAP_L1BULLET, new GBWeapon("Anti-Grav Pellet", m_botSettings.getInt("L1BulletPrice") ) );
        m_weapons.add( WEAP_L3BOMB, new GBWeapon("Mega Bomb", m_botSettings.getInt("L3BombPrice") ) );
        m_weapons.add( WEAP_BOUNCEBOMB, new GBWeapon("Elasto Bomb", m_botSettings.getInt("BounceBombPrice") ) );
        m_weapons.add( WEAP_BURST, new GBWeapon("Star Burst", m_botSettings.getInt("BurstPrice") ) );
        m_weapons.add( WEAP_SHRAPBOMB, new GBWeapon("Shatter Bomb", m_botSettings.getInt("ShrapBombPrice") ) );
        m_weapons.add( WEAP_THOR, new GBWeapon("Planet Burrower", m_botSettings.getInt("ThorPrice") ) );
        m_weapons.add( WEAP_REPEL, new GBWeapon("Star Quake", m_botSettings.getInt("RepelPrice") ) );
    }

    public void setupSettings(){
        m_botAction.sendUnfilteredPublicMessage( "?set Bomb:JitterTime:32" );
        m_botAction.sendUnfilteredPublicMessage( "?set All:InitialRecharge:0" );
    	m_botAction.sendUnfilteredPublicMessage( "?set All:EmpBomb:0" );
    	m_botAction.sendUnfilteredPublicMessage( "?set All:BombFireDelay:3000" );
        m_botAction.sendUnfilteredPublicMessage( "?set All:BombBounceCount:0" );
    }

    public void startGame( String hostName ){
    	if(m_gameStarted){
    	    m_botAction.sendPrivateMessage( hostName, "Cannot start game, there is one currently in progress" );
    	    return;
    	}

        m_boughtWeapon = false;
        m_timer = new Timer( true );
    	Player pTemp;
    	m_playerData = new HashMap<String, GBPlayer>();
    	m_turnTasks = new HashMap<String, TurnTask>();
        m_playerList = new LinkedList<String>();
    	PlayerBag GBPlayerBag = new PlayerBag();
        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

    	if( i == null ) return;
        while( i.hasNext() ){
       	    pTemp = i.next();
            GBPlayerBag.add( pTemp.getPlayerName() );
        }

        if( GBPlayerBag.size() < 2 ){
            m_botAction.sendPrivateMessage( hostName,  "Cannot start game with less than 2 people in play" );
            return;
        }

        setupSettings();
        m_botAction.sendUnfilteredPublicMessage( "*scorereset" );

        int dataSize = GBPlayerBag.size(); //need to take a snapshot since we alter it
        int points = m_botSettings.getInt("InitialPoints");
        for( int x = 0; x < dataSize; x++ ){
            String playerName = GBPlayerBag.grab();
            m_playerData.put( playerName, new GBPlayer(playerName) );
            m_playerList.addLast( playerName );
            m_botAction.sendUnfilteredPrivateMessage( playerName, "*watchdamage" );
            m_botAction.sendUnfilteredPrivateMessage( playerName, "*watchgreen" );
            m_botAction.sendUnfilteredPrivateMessage( playerName, "*points " + points );

            //debug
            if(m_debug){
                m_botAction.sendChatMessage( 2, "Added " + playerName + " ID: " + Integer.toString(m_botAction.getPlayerID( playerName )) );
            }
        }

        m_botAction.sendUnfilteredPublicMessage( "*shipreset" );
        m_gameStarted = true;
        m_botAction.sendArenaMessage("GravBomber activated by " + hostName + ". Type !help for command information");
        startTurn( m_playerList.getFirst().toString() );
    }

    public void stopGame(){
    	if(m_gameStarted){
    	    m_timer.cancel();

    	    m_gameStarted = false;

    	    for( int i = 0; i <= m_playerList.size(); i++ ){
    	        remPlayer( m_playerList.getFirst(), false, false );
    	    }

    	    m_botAction.sendUnfilteredPublicMessage( "?set All:InitialRecharge:5000" );
    	    m_botAction.sendUnfilteredPublicMessage( "*shipreset");
    	}
    }

    public void gameOver(){
    	if(!m_gameStarted) return;

    	GBPlayer winner = m_playerData.get( m_playerList.getFirst() );
    	stopGame();

    	m_botAction.sendArenaMessage("GAME OVER! Winner is " + winner.getName(), 6 );
    	m_botAction.sendUnfilteredPublicMessage("*objon 5");
    	m_botAction.sendUnfilteredPrivateMessage( winner.getName(), "*objon 4" );
        m_botAction.sendPrivateMessage( winner.getName(), "You Have Won!", 5 );
    }

    public void startTurn( String playerName ){
    	if( playerName == null ) return;
    	//if( m_playerList.size() == 1 && m_gameStarted ){
    	//    gameOver();
    	//}

        try{
            m_currentPlayer = m_botAction.getPlayer( playerName );
            m_currentGBPlayer = m_playerData.get( playerName );
        } catch( NullPointerException e) {
            //player left or lagged out before bot removed him from the list
            m_currentPlayer = m_botAction.getPlayer( getNextPlayer( true, false ) );
            m_currentGBPlayer = m_playerData.get( playerName );
        } catch( Exception e ) {
            Tools.printStackTrace( e );
        }

        m_botAction.sendArenaMessage( playerName + "'s Turn" );
        m_botAction.sendUnfilteredPublicMessage( "?set All:bombspeed:" + m_currentGBPlayer.getBombPower() );
        //In case "stop" is still showing, won't overlap
        m_botAction.sendUnfilteredPrivateMessage( playerName, "*oboff 3" );
        m_botAction.sendUnfilteredPrivateMessage( playerName, "*objon 2" );
        m_botAction.sendPrivateMessage( playerName, "It is your turn, please !buy a weapon - Shot power set to: " + m_currentGBPlayer.getBombPower(), 3 );
        toggleList( playerName, true );

        m_turnTasks.put( playerName, new TurnTask(playerName) );

    	m_timer.schedule( (TimerTask)m_turnTasks.get(playerName), m_turnTime );

    	//debug
    	if(m_debug){
    	    m_botAction.sendChatMessage( 2, "Timer schedule for " + playerName + " Interval: " + m_turnTime );
    	}

    	if( m_playerList.size() > 2 ){
    	    m_botAction.sendPrivateMessage( m_botAction.getPlayer( getNextPlayer( false, false ) ).getPlayerName(), "Your turn is next... Get ready!" );
    	}
    }

    public void endTurn( String endPlayerName, boolean visuals, boolean startNext ){
    	//if(!m_gameStarted) return;

    	String playerName = m_currentPlayer.getPlayerName();

    	if(!playerName.equals( endPlayerName )) {
    	    //debug
    	    if(m_debug) m_botAction.sendChatMessage( 2, playerName + " does not equal " + endPlayerName );
    	    return;
    	}

        ((TimerTask)m_turnTasks.get(playerName)).cancel();
    	unloadWeapon( playerName, m_currentWeapon );
    	m_boughtWeapon = false;

    	if(!m_boughtWeapon){
    	    toggleList( playerName, false );
    	}

    	if(visuals){
            //In case Start is still showing from begenning of turn
    	    m_botAction.sendUnfilteredPrivateMessage( playerName, "*objoff 2" );
            m_botAction.sendUnfilteredPrivateMessage( playerName, "*objon 3" );
            m_botAction.sendArenaMessage( playerName + "'s turn has ended. Points awarded for damage this turn: " + m_currentTurnDamage );
            m_botAction.sendPrivateMessage( playerName, "Your turn is over", 4 );
        }

        m_currentTurnDamage = 0;

        GBPlayer gbplayer = m_playerData.get( playerName );
        gbplayer.decrementTurnsTillNextRocket();

        if(startNext && m_gameStarted){
            startTurn( getNextPlayer( true, false ) );
        }
    }

    public String getNextPlayer( boolean increment, boolean wasRemoved ){
    	if(!m_gameStarted) return null;

    	/*int i = m_playerList.indexOf( currentPlayerName );
    	if( i == -1 ) return null; //not found
    	int last = (m_playerList.size() - 1);

        if (i == last){
    	    return m_playerList.getFirst().toString();
    	} else if (i < last ){
            return m_playerList.get(++i).toString();
    	} else {
    	    return null;
    	}*/

    	int pos = m_currentPlayerIndex;
    	int last = (m_playerList.size() - 1);

    	if(!wasRemoved){
            if (pos == last){
    	        pos = m_playerList.indexOf(m_playerList.getFirst());
            } else if (m_currentPlayerIndex < last ){
                pos++;
            }
        } else {
            if (pos > (m_playerList.size() - 1)) {
                pos = m_playerList.indexOf(m_playerList.getFirst());
            }
        }

        if( increment ){ m_currentPlayerIndex = pos; }
        return m_playerList.get(pos).toString();
    }

    public void remPlayer( String playerName, boolean spec, boolean checkLast ){
    	if( m_playerList.indexOf( playerName ) == -1) return;

        if(spec){
            Player player = m_botAction.getPlayer( playerName );
            if( player.isPlaying() ){ //Spec them if they're still in a ship
                m_botAction.spec( playerName );
                m_botAction.spec( playerName );
            }
        }

        if( playerName.equals( m_currentPlayer.getPlayerName() ) ){
    	    endTurn( playerName, false, false );
    	}

    	m_playerList.remove( playerName );
        m_botAction.sendUnfilteredPrivateMessage( playerName, "*watchdamage" );
        m_botAction.sendUnfilteredPrivateMessage( playerName, "*watchgreen" );

        if( m_gameStarted && checkLast && m_playerList.size() == 1 ){
            gameOver();
        } else {
            if(m_gameStarted){
              startTurn( getNextPlayer( true, true ) );
            }
        }
    }

    public void setPower( String playerName, int power ){
        if( m_gameStarted && m_playerList.indexOf(playerName) != -1 ){
            int min = m_botSettings.getInt("MinPower");
            int max = m_botSettings.getInt("MaxPower");

            if( power >= min && power <= max ){
            	GBPlayer gbplayer = m_playerData.get( playerName );

                if( m_currentPlayer.getPlayerName().equals( playerName ) ){
                    m_botAction.sendUnfilteredPublicMessage( "?set All:bombspeed:" + power );
                    m_botAction.sendPrivateMessage( playerName, "Power set to: " + power );
                } else {
                    m_botAction.sendPrivateMessage( playerName, "For your next turn, power will be set to: " + power );
                }

                gbplayer.setBombPower( power );
            } else {
                m_botAction.sendPrivateMessage( playerName, "Invalid power amount. Valid range is " + min + "-" + max );
            }
        }
    }

    public void buyWeapon( String playerName, int weaponID ){
    	if( m_boughtWeapon == true ){
    	    m_botAction.sendPrivateMessage( playerName, "You may only buy ONE weapon per turn" );
    	    return;
    	}

    	GBWeapon weapon = new GBWeapon();

    	try{
    	    weapon = m_weapons.get( weaponID );
    	} catch( IndexOutOfBoundsException e){
            m_botAction.sendPrivateMessage( playerName, "Invalid weapon number");
            return;
        } catch( Exception e ){
            Tools.printStackTrace( e );
        }

    	Player player = m_botAction.getPlayer( playerName );

    	if( player.getScore() >= weapon.getPrice() ){
            if( weaponID == WEAP_ROCKET ){
                GBPlayer gbplayer = m_playerData.get( playerName );
                if( gbplayer.getTurnsTillNextRocket() > 0 ){
                    m_botAction.sendPrivateMessage( playerName, "There is a turn limit for buying " + weapon.getName() + "s. Turns left to wait: " + gbplayer.getTurnsTillNextRocket() );
                    return;
                } else {
                    gbplayer.setTurnsTillNextRocket( m_botSettings.getInt( "RocketTurnLimit" ) );
                }
    	    }

            toggleList( playerName, false );
    	    loadWeapon( playerName, weaponID );

    	    if( weapon.getPrice() != 0 ){
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*points -" + weapon.getPrice() );
    	    }
    	    m_currentWeapon = weaponID;
    	    m_botAction.sendPrivateMessage( playerName, weapon.getName() + " loaded");
    	    m_boughtWeapon = true;
    	} else {
            m_botAction.sendPrivateMessage( playerName, "Insufficient funds. You must have " + weapon.getPrice() + " points to buy a " + weapon.getName() );
    	}
    }

    public void loadWeapon( String playerName, int weaponID ){

    	switch(weaponID){
    	    case WEAP_ROCKET:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #27" );
    	        break;
    	    case WEAP_L1BOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        break;
    	    case WEAP_TRACER:
    	        m_botAction.sendUnfilteredPublicMessage( "?set All:EmpBomb:1" );
    	        m_botAction.sendUnfilteredPublicMessage( "?set All:BombFireDelay:10" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        break;
    	    case WEAP_THRUSTBOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #27" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        break;
    	    case WEAP_L1BULLET:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #8" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #15" );
    	        break;
    	    case WEAP_L3BOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        break;
    	    case WEAP_BOUNCEBOMB:
    	        m_botAction.sendUnfilteredPublicMessage( "?set All:BombBounceCount:3" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        break;
    	    case WEAP_BURST:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #22" );
    	        break;
    	    case WEAP_THOR:
    	        m_botAction.sendUnfilteredPublicMessage( "?set Bomb:JitterTime:250" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #24" );
    	        break;
    	    case WEAP_SHRAPBOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #19" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #10" );
    	        break;
    	    case WEAP_REPEL:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #21" );
    	        break;
        }
    }

    public void unloadWeapon( String playerName, int weaponID ){

    	switch(weaponID){
    	    case WEAP_ROCKET:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-27" );
    	        break;
    	    case WEAP_L1BOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        break;
    	    case WEAP_TRACER:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        m_botAction.sendUnfilteredPublicMessage( "?set All:EmpBomb:0" );
    	        m_botAction.sendUnfilteredPublicMessage( "?set All:BombFireDelay:3000" );
    	        break;
    	    case WEAP_THRUSTBOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-27" );
    	        break;
    	    case WEAP_L1BULLET:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-15" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-8" );
    	        break;
    	    case WEAP_L3BOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        break;
    	    case WEAP_BOUNCEBOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        m_botAction.sendUnfilteredPublicMessage( "?set All:BombBounceCount:0" );
    	        break;
    	    case WEAP_BURST:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-22" );
    	        break;
    	    case WEAP_THOR:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-24" );
    	        m_botAction.sendUnfilteredPublicMessage( "?set Bomb:JitterTime:32" );
    	        break;
    	    case WEAP_SHRAPBOMB:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-9" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-19" );
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-10" );
    	        break;
    	    case WEAP_REPEL:
    	        m_botAction.sendUnfilteredPrivateMessage( playerName, "*prize #-21" );
    	        break;
        }
    }

    public void toggleList( String playerName, boolean visible ){
    	if(visible){
            m_botAction.sendUnfilteredPrivateMessage( playerName, "*objon 1" );
        } else {
            m_botAction.sendUnfilteredPrivateMessage( playerName, "*objoff 1" );
        }
    }

    public class TurnTask extends TimerTask{
        final String playerName;

        public TurnTask( String endPlayerName ){
            playerName = endPlayerName;
        }

    	public void run(){
    	    if(m_gameStarted) endTurn( playerName, true, true );
    	}
    }

    /* Misc Code */
    public int getInteger( String input, int def ){
        try{
            return Integer.parseInt( input.trim() );
        } catch( Exception e ){
            return def;
        }
    }

    class GBPlayer {
        String name;
        int bombPower = 2000;
        int turnsTillNextRocket = 0;

        public GBPlayer(){
        }

        public GBPlayer( String playerName ){
            name = playerName;
        }

        public String getName(){
            return name;
        }

        public void setBombPower( int power ){
            bombPower = power;
        }

        public int getBombPower(){
            return bombPower;
        }

        public int getTurnsTillNextRocket(){
            return turnsTillNextRocket;
        }

        public void setTurnsTillNextRocket( int newValue ){
            turnsTillNextRocket = newValue;
        }

        public void decrementTurnsTillNextRocket(){
            if( turnsTillNextRocket > 0 ) {
            	turnsTillNextRocket--;
            }
        }
    }

    class GBWeapon {
        String name;
        int price;

        public GBWeapon(){
        }

        public GBWeapon( String newName, int newPrice ){
            setName( newName );
            setPrice( newPrice );
        }

        public void setName( String newName ){
            name = newName;
        }

        public String getName(){
            return name;
        }

        public void setPrice( int newPrice ){
            price = newPrice;
        }

        public int getPrice(){
            return price;
        }
    }

    class PlayerBag {
        ArrayList<String> list;

        public PlayerBag(){
            list = new ArrayList<String>();
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

        public ArrayList<String> getList(){
            return list;
        }

        public String grab(){
        	if( isEmpty() ){
                return null;
            } else {
                int i = random( list.size() );
                String grabbed;

                grabbed = list.get( i ) ;
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
}
