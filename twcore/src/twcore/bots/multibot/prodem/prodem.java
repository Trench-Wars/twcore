package twcore.bots.multibot.prodem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerEntered;
import twcore.core.game.Player;
import twcore.core.stats.PlayerProfile;

/**
 * Prodem: promotion-demotion.
 * 
 * @author 2dragons 11.05.02
 */
public class prodem extends MultiModule {

    TimerTask		timerUpdate;
    TimerTask		startGame;

    int				curTime = 0,lastMessage = 0;
    int				gameProgress = -1;
    private HashMap<String, PlayerProfile> playerMap;
    final String[]	ranks = { "",
    "General",
    "Lt. General",
    "Colonel",
    "Lt. Colonel",
    "Major",
    "Captain",
    "1st Lieutenant",
    "2nd Lieutenant" };
    //Data values:
    //0 - kills on lower level
    //1 - kills on same level
    //2 - kills on higher level
    //3 - highest score
    //4 - lowest score
    int[] records = { 0, 0, 0, 0, 10000, 0, 0 };


    ///*** Constructor ///***
    public void init() {
        //Other setup
        playerMap = new HashMap<String, PlayerProfile>();
    }

    public void requestEvents(ModuleEventRequester events) {
		events.request(this, EventRequester.PLAYER_DEATH);
		events.request(this, EventRequester.PLAYER_ENTERED);
	}

    public void setupTimerTasks(){
        //Timer setup.
        timerUpdate = new TimerTask() {
            public void run() {
                if(gameProgress == 1) lastMessage += 30;
                if(lastMessage >= 60) {
                    m_botAction.sendTeamMessage("Lagout or want in? PM me with !prolag -" + m_botAction.getBotName() );
                    lastMessage = 0;
                }
            }
        };
        m_botAction.scheduleTaskAtFixedRate( timerUpdate, 0, 30000 );

    }

    public void handleCommand( String name, String message ) {
        if( message.toLowerCase().startsWith( "!prodem on" )) botStart();
        else if( message.toLowerCase().startsWith( "!prodem off" ))  botStop( name );
        else if( message.toLowerCase().startsWith( "!prolag" )) botPlayerIn( name, message );
    }

    ///*** Incomming Messages ///***
public void handleEvent( Message event ) {
        String message = event.getMessage();
        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            if( opList.isER( name ) ) {
                handleCommand( name, message );
            }
            else if( message.startsWith( "!prolag" ) ) {
                botPlayerIn( name, message );
            }
        }
    }

    ///*** Starts the game. ///***
    public void botStart() {
        if( timerUpdate == null ){
            setupTimerTasks();
        }
        gameProgress = 0;
        lastMessage = 30;

        //Locks and setships.
        m_botAction.toggleLocked();
        m_botAction.scoreResetAll();
        m_botAction.changeAllShips(8);

        records[0] = 0;
        records[1] = 0;
        records[2] = 0;
        records[3] = 0;
        records[4] = 10000;
        records[5] = 0;
        records[6] = 0;

        //Stores players information.
        Iterator i = m_botAction.getPlayingPlayerIterator();
        if( i == null ) return;
        while( i.hasNext() ){
            Player tempP = (Player)i.next();
            String name = tempP.getPlayerName();
            playerMap.put( name, new PlayerProfile( name, 8, tempP.getFrequency() ) );
        }

        startGame = new TimerTask() {
            public void run() {
                if( gameProgress == 0 ) {
                    gameProgress = 1;
                    m_botAction.createRandomTeams(1);
                    m_botAction.sendArenaMessage( "ProDem begins!", 104 );
                    m_botAction.sendArenaMessage( "Lagout or want in just pm me with !prolag -" + m_botAction.getBotName() );
                }

            }
        };
        m_botAction.scheduleTask( startGame, 10000 );

        m_botAction.sendArenaMessage( "Rules: Kill people of the same ship as you or higher (warbird high, shark low) to get promoted. ");
        m_botAction.sendArenaMessage( "Get killed by someone lower and get demoted! First one up the ladder of ranks wins!" );
        m_botAction.sendArenaMessage( "Game starts in 10 seconds!", 2 );

    }

    ///*** Stops the Game. ///***
    public void botStop( String name ) {
        if( gameProgress != -1 ) {
            gameProgress = -1;
            playerMap.clear();
            for(int h=0;h>7;h++)
                records[h] = 0;
            records[4] = 10000;
            m_botAction.toggleLocked();
            m_botAction.sendArenaMessage( "Game has been stopped.", 103 );
        }
        else
            m_botAction.sendPrivateMessage( name, "There is no game in progress." );
    }

    public void botPlayerIn( String name, String message) {
        if(gameProgress == 1) {
            if(!playerMap.containsKey( name ) ) {
                int freq = playerMap.size();
                m_botAction.setShip( name, 8 );
                m_botAction.setFreq( name, freq );
                playerMap.put( name, new PlayerProfile( name, 8, freq ) );
            }
            else {
                PlayerProfile tempP;
                tempP = playerMap.get( name );
                m_botAction.setShip( name, tempP.getShip() );
                m_botAction.setFreq( name, tempP.getFreq() );
            }
        }
    }

    ///*** Player Death ///***
    public void handleEvent( PlayerDeath event ) {
        if( gameProgress == 1 ) {
            Player theKiller  = m_botAction.getPlayer( event.getKillerID() );
            Player theKillee  = m_botAction.getPlayer( event.getKilleeID() );
            if( theKiller == null || theKillee == null )
                return;
            String killer 	  = theKiller.getPlayerName();
            String killee 	  = theKillee.getPlayerName();
            if(playerMap.containsKey(killer)) {
                if(playerMap.containsKey(killee)) {
                    manageKill(killer, killee);
                }
            }
        }
    }

    ///*** Handles a kill ///***
    public void manageKill( String killer, String killee ) {
        PlayerProfile tempWin, tempLose;
        tempWin  = playerMap.get(killer);
        tempLose = playerMap.get(killee);
        tempWin.addKill();
        if( tempWin.getKills() > records[5] ) records[5] = tempWin.getKills();
        tempLose.addDeath();
        if( tempLose.getDeaths() > records[6] ) records[6] = tempLose.getDeaths();
        if( tempWin.getShip() == tempLose.getShip() ) {
            //Stores data.
            tempWin.incData(1);
            if( tempWin.getData(1) > records[1] )
                records[1] = tempWin.getData(1);

            if( tempWin.getShip() == 1 )
                botEnd( killer );
            else {
                tempWin.setShip( tempWin.getShip() - 1 );

                m_botAction.setShip( killer, tempWin.getShip() );
                m_botAction.sendSmartPrivateMessage( killer, "You have been promoted to " + ranks[tempWin.getShip()]  );
                if( tempWin.getShip() < 3 )
            	m_botAction.sendArenaMessage( killer + " has just been promoted to " + ranks[tempWin.getShip()] );

            }
        }
        else if( tempWin.getShip() > tempLose.getShip() ) {
            tempWin.setShip( tempWin.getShip() - 1 );
            m_botAction.setShip( killer, tempWin.getShip() );

            //Stores data.
            tempWin.incData(2);
            if( tempWin.getData(2) > records[2] )
                records[2] = tempWin.getData(2);

            tempLose.setShip( tempLose.getShip() + 1 );

            TimerTask   task;

            final String finalKillee = killee;
            final int    finalShip = tempLose.getShip();
            final String test = new String( "You have been demoted to "
                + ranks[tempLose.getShip()] + ".  " + killer + " promoted to "
                + ranks[tempWin.getShip()] );

            task = new TimerTask(){

                public void run() {
                    m_botAction.setShip( finalKillee, finalShip );
                    m_botAction.sendSmartPrivateMessage( finalKillee, test );
                }
            };

            m_botAction.scheduleTask( task, 5000 );

            m_botAction.sendSmartPrivateMessage( killer, "You have been promoted to "
            + ranks[tempWin.getShip()] + ".  " + killee + " demoted to "
            + ranks[tempLose.getShip()]);
            if( tempWin.getShip() < 3 )
            m_botAction.sendArenaMessage( killer + " has just been promoted to " + ranks[tempWin.getShip()] );
            if( tempLose.getShip() < 4 )
            m_botAction.sendArenaMessage( killee + " has just been demoted to " + ranks[tempLose.getShip()] );

        }
        else {
            tempWin.incData(0);
            if( tempWin.getData(0) > records[0] )
                records[0] = tempWin.getData(0);
        }
    }

    ///*** Declares the end of game. ***///
    public void botEnd( String winner ) {
        //Sets the score for players.
        Set set = playerMap.keySet();
        Iterator i = set.iterator();
        while (i.hasNext()) {
            String curPlayer = (String) i.next();
            PlayerProfile tempPlayer;
            tempPlayer = playerMap.get(curPlayer);
            //Formula for calculating scores:
            //(kh * 2) + (ks) - (kl) - (d) + ((8-cs)*2)
            int sc = (tempPlayer.getData(2) * 2) + tempPlayer.getData(1) - tempPlayer.getData(0) + ((8 - tempPlayer.getShip()) * 2);
            if( sc > records[3] ) records[3] = sc;
            if( sc < records[4] ) records[4] = sc;
            tempPlayer.setData(3, sc);
        }
        m_botAction.sendArenaMessage( winner + " has won the game!", 5);
        m_botAction.sendArenaMessage( "Best ProDem Player       :" + getHolder(3, records[3]) + "  (" + records[3] + " pts)");
        m_botAction.sendArenaMessage( "Worst ProDem Player      :" + getHolder(3, records[4]) + "  (" + records[4] + " pts)");
        m_botAction.sendArenaMessage( "Bravest Player           :" + getHolder(2, records[2]) + "  (" + records[2] + " kills)");
        m_botAction.sendArenaMessage( "Most Dishonorable Player :" + getHolder(0, records[0]) + "  (" + records[0] + " kills)");
        m_botAction.sendArenaMessage( "Safest Playing Player    :" + getHolder(1, records[1]) + "  (" + records[1] + " kills)");
        m_botAction.sendArenaMessage( "Player w/Best Aim        :" + getHolder("Kills", records[5]) + "  (" + records[5] + " kills)");
        m_botAction.sendArenaMessage( "Worst Dodger             :" + getHolder("Deaths", records[6]) + "  (" + records[6] + " deaths)");
        gameProgress = -1;
        playerMap.clear();
        for(int h=0;h>7;h++)
            records[h] = 0;
        records[4] = 10000;
        m_botAction.toggleLocked();
    }

    public String getHolder( int location, int score ) {
        Set set = playerMap.keySet();
        Iterator i = set.iterator();
        String recordH = "";
        while (i.hasNext()) {
            String curPlayer = (String) i.next();
            PlayerProfile tempPlayer;
            tempPlayer = playerMap.get(curPlayer);
            if( tempPlayer.getData(location) == score )
                recordH += "  " + curPlayer;
        }
        return trimFill( recordH );
    }

    public String getHolder( String location, int score ) {
        Set set = playerMap.keySet();
        Iterator i = set.iterator();
        String recordH = "";
        while (i.hasNext()) {
            String curPlayer = (String) i.next();
            PlayerProfile tempPlayer;
            tempPlayer = playerMap.get(curPlayer);
            int curScore;
            if(location.equals("Kills")) curScore = tempPlayer.getKills();
            else if(location.equals("Deaths")) curScore = tempPlayer.getDeaths();
            else curScore = 0;
            if( curScore == score )
                recordH += "  " + curPlayer;
        }
        return trimFill( recordH );
    }

    public String trimFill( String line ) {
        if(line.length() < 38) {
            for(int i=line.length();i<38;i++)
                line += " ";
        }
        return line;
    }

    public void handleEvent( PlayerEntered event ) {
        if(gameProgress == 1)
            m_botAction.sendPrivateMessage( event.getPlayerName(), "Lagout or want in just pm me with !prolag");
    }


    ///*** Help Messages ///***
    public String[] getModHelpMessage() {
        String[] messages = {
            "Prodem Commands - for a good game try running this module in arenas where all ship settings are the same.",
            "     ie: prodem, deathmatch, bomberman, cloaks, javs, etc...",
            "!prodem on     -- Starts ProDem.",
            "!prodem off    -- Cancles ProDem.",
            "!prolag        -- For lagouts or to enter game."
        };
        return messages;
    }

    public void cancel() {
    	m_botAction.cancelTasks();
    }

    public boolean isUnloadable()	{
		return true;
	}

}