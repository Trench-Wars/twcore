package twcore.bots.multibot.victim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.StringBag;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.game.Player;

public class victim extends MultiModule {

    public void init() {
    }

    public void requestEvents(ModuleEventRequester events)  {
        events.request(this, EventRequester.PLAYER_DEATH);
        events.request(this, EventRequester.PLAYER_POSITION);
        events.request(this, EventRequester.PLAYER_LEFT);
        events.request(this, EventRequester.FREQUENCY_CHANGE);
        events.request(this, EventRequester.FREQUENCY_SHIP_CHANGE);
    }
    
    private HashMap<String, vPlayer> m_player;
    BotSettings m_botSettings = moduleSettings;
    
    int m_freqOne;
    int m_freqTwo;
    int m_killCount;
    int m_time;    // in seconds
    String bombPlayer;

    boolean isRunning = false;
    boolean modeSet = false;
    
    public void setMode( int _freqOne, int _freqTwo, int _killCount, int _time ){
        m_freqOne = _freqOne;
        m_freqTwo = _freqTwo;
        m_killCount = _killCount;
        m_time = _time;
        modeSet = true;
    }
    
    public void handleEvent( PlayerLeft event ){
        if(isRunning) {
            Player p = m_botAction.getPlayer( event.getPlayerID() );
            if( p != null ) {
                if( p.getShipType() == 0 ) {
                    if( m_botAction.getNumPlayers() == 1 ) {
                        gameOver();
                    }
                }
            }
        }
    }

    public void handleEvent( FrequencyShipChange event ) {
        if(isRunning) {
            Player p = m_botAction.getPlayer( event.getPlayerID() );
            if( p != null ) {
                if( p.getShipType() == 0 ) {
                    if( m_botAction.getNumPlayers() == 1 ) {
                        gameOver();
                    }
                }
            }
        }
    }
    
    public void handleEvent( Message event ) {
        int messageType = event.getMessageType();
        String message = event.getMessage().toLowerCase().trim();
        String name = m_botAction.getPlayerName(event.getPlayerID());

        if(opList.isER(name))
            if(messageType == Message.PRIVATE_MESSAGE)
                handleModCommand(name, message);
    }
    
    public void startGame( String name, String[] params ) {
        try{
            if( params.length == 4 ){
                int _freqOne = Integer.parseInt(params[0]);
                int _freqTwo = Integer.parseInt(params[1]);
                int _killCount = Integer.parseInt(params[2]);
                int _time = Integer.parseInt(params[3]);
                if(_time < 10)
                    _time = 10;
                if(_killCount <= 0)
                    _killCount = 1;
                setMode( _freqOne, _freqTwo, _killCount, _time );
                isRunning = true;
                modeSet = true;                
            }
            else {
                m_botAction.sendPrivateMessage( name, "Sorry, you made a mistake; please try again." );
                isRunning = false;
                modeSet = false;
            }
                
        } catch( Exception e ){
            m_botAction.sendPrivateMessage( name, "Sorry, you made a mistake; please try again." );
            isRunning = false;
            modeSet = false;
        }
    }
    
    //TODO make so that they cannot do !start again if it has started.
    public void handleModCommand( String name, String message ) {
        try {
            if( message.startsWith("!start ") ) {
                String[] params = message.substring(7).split(" ");
                startGame(name, params);
                doPreGame(name);
            } else if( message.equals("!start") ) {
                setMode( 0, 1, 1, 15 );
                doPreGame(name);
            } else if( message.equals("!stop") ) {
                m_botAction.sendPrivateMessage( name, "Victim mode stopped" );
                isRunning = false;
                //TODO stopGame();
            }
                
        } catch(Exception e) {}
    }
    
    public void doPreGame( String name ) {
        if(!checkEnoughPlayers()) {
            m_botAction.sendSmartPrivateMessage(name, "Must have atleast two players playing!");
            return;
        }
        m_botAction.scoreResetAll();
        m_botAction.setAlltoFreq(m_freqOne);
        m_botAction.arenaMessageSpam(displayIntro());
        m_botAction.sendArenaMessage("-- Victim needs: " + Integer.toString(m_killCount) + " kill(s) in " + 
                                        Integer.toString(m_time) + " seconds.");
        m_botAction.sendArenaMessage("-- This event will begin in 10 seconds...", 2);
        m_botAction.sendUnfilteredPublicMessage("*lock");
        
        TimerTask timer = new TimerTask() {
            public void run() {
                isRunning = true;
                modeSet = true;
                m_botAction.scoreResetAll();
                m_botAction.shipResetAll();
                //TODO Get random player and assign bomb to player.
                getRandomPlayer();
                if(isRunning) {
                    m_botAction.sendSmartPrivateMessage(bombPlayer, "You have " + m_time + " seconds to get rid of the bomb!");
                    m_botAction.sendArenaMessage("-- " + bombPlayer + " has the bomb!", 104);
                    m_botAction.prizeAll(7);
                }
            }
        }; m_botAction.scheduleTask(timer, 10000);
    }
    
    public boolean checkEnoughPlayers() {
        if(m_botAction.getNumPlayers() < 2)
            return false;
        return true;
    }

    public void getRandomPlayer() {
        Player p;
        StringBag randomPlayerBag = new StringBag();
        //int freqOne_SIZE = m_botAction.getPlayingFrequencySize(freqOne);

        //if(freqOne_SIZE < 2) {
        //    m_botAction.sendArenaMessage("-- This game has been cancelled due to the lack of players.");
        //    isRunning = false;
        //    return;
        //}
        
        Iterator<Player> it = m_botAction.getFreqPlayerIterator(m_freqOne);
        if (it == null)
            return;
        while (it.hasNext()) {
            p = it.next();
            randomPlayerBag.add(p.getPlayerName());
        }
        bombPlayer = randomPlayerBag.grabAndRemove();
        
        m_player = new HashMap<String, vPlayer>();
        vPlayer player = m_player.get(bombPlayer);
        m_player.remove(bombPlayer);
        
        if(bombPlayer != null)
            m_botAction.cancelTasks();
        
        player = new vPlayer( bombPlayer, m_time );
        m_botAction.scheduleTaskAtFixedRate( player, 1000, 1000 );
        m_player.put( bombPlayer, player );
        
        if(bombPlayer.equalsIgnoreCase(m_botAction.getBotName()))
            getRandomPlayer();
        m_botAction.setFreq(bombPlayer, m_freqTwo);
    }
    
    public void pickPlayer( String _player ) {
        String prevPlayer = bombPlayer;
        bombPlayer = _player;
        m_botAction.setFreq(prevPlayer, m_freqOne);
        m_botAction.sendSmartPrivateMessage(bombPlayer, "You have " + m_time + " seconds to get rid of the bomb!");
        m_botAction.sendArenaMessage("-- " + prevPlayer + " has passed the bomb to " + bombPlayer + "!", 104);
        m_botAction.setFreq(bombPlayer, m_freqTwo);
        m_botAction.scoreResetAll();
        
        newPlayer();
    }
    
    public void newPlayer() {
        clearPlayer();
        
        vPlayer player = new vPlayer( bombPlayer, m_time );
        m_botAction.scheduleTaskAtFixedRate( player, 1000, 1000 );
        m_player.put( bombPlayer, player );
    }
    
    public void gameOver() {
        Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
        Player p = i.next();
        m_botAction.sendArenaMessage("-- NOTICE: Game over (" + p.getPlayerName() + ")");
        m_botAction.sendUnfilteredPublicMessage("*lock");
        clearPlayer();
        isRunning = false;
    }
    
    public void clearPlayer() {
        Iterator<vPlayer> i = m_player.values().iterator();
        vPlayer player = (vPlayer)i.next();
        m_botAction.cancelTask(player);
        m_player = new HashMap<String, vPlayer>();
    }
    
    public void handleEvent( PlayerDeath event ){
        if( modeSet && isRunning ){
            Player p = m_botAction.getPlayer( event.getKilleeID() );
            Player p2 = m_botAction.getPlayer( event.getKillerID() );
            if( p == null || p2 == null )
                return;
            try {
                if(p2.getPlayerName().equals(bombPlayer) && p2.getFrequency() == m_freqTwo && p2.getWins() >= m_killCount) {
                    pickPlayer(p.getPlayerName());
                }
                
            } catch (Exception e) { }
        }
    }
    
    public String[] getModHelpMessage() {
        String[] help = {
            "!Start                        -- Starts a standard game of victim.",
            "!Start <freqOne> <freqTwo> <kills> <timer>",
            "!Stop                         -- Stops a game of victim.",
        };
        return help;
    }
    
    public String[] displayIntro() {
        String[] intro = {
            "-- +- Victim Rules -------------------------------------------------------------+",
            "-- |   A randomly selected player will hold a bomb.                             |",
            "-- |   Player with bomb must get certain amount of kills before timer runs out. |",
            "-- |   If timer runs out and player does not have enough kills, he is out.      |",
            "-- |   Repeated until there is one person in the game!                          |",
            "-- +----------------------------------------------------------------------------+"
        };
        return intro;
    }
    
    public void cancel() {
    }
    
    public boolean isUnloadable()   {
        return true;
    }
    
    private class vPlayer extends TimerTask {
        
        private String name;
        private int time;
        private boolean isPlaying = true;
        
        public vPlayer( String _name, int _time ) {
            this.name = _name;
            this.time = _time;
        }
        
        public void removePlayer() {
            m_botAction.specWithoutLock( name );
        }

        public void run() {
            if(isRunning && isPlaying) {
                time--;

                if(time > 0 && time <= 5) {
                    m_botAction.sendPrivateMessage( name, "Time left: " + String.valueOf(time) );
                } else if(time <= 0) {
                    m_botAction.sendArenaMessage("-- " + name + " could not make it in time and blew to bits!");
                    removePlayer();
                    if(m_botAction.getNumPlayers() != 1)
                        getRandomPlayer();
                }
            }
        }
    }
}