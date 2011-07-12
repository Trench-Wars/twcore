package twcore.bots.multibot.lasertag;

import static twcore.core.EventRequester.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.Map.Entry;

import twcore.bots.MultiModule;
import twcore.core.events.*;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

/**
 * Woohoo lasertag!
 * 
 * @author JoyRider
 */
public class lasertag extends MultiModule {
    private int[]                   m_totalScores;
    private int[]                   m_totalFlags;
    private HashMap<Short, Short>   m_flagsOwned;
    private HashMap<Short, Integer> m_playerScores;
    private boolean                 m_running;
    private boolean                 m_arenaLocked;
    private int                     m_goalPoints;
    private int                     m_killPoints;
    private int                     m_gameTime;
    private int                     m_ballTimestamp;
    private short                   m_ballCarrier;
    private String[]                m_rules;

    final private String[]          m_playerHelp = { 
            "!rules       - Shows the rules of Lasertag.",
            "!help        - Shows this message.", 
            "!score       - Shows the score of the current game.", 
    };
    
    final private String[]          m_modHelp    = { 
            "!start       - Starts a game of Lasertag.",
            "!stop        - Stops a game currently in progress.", 
    };

    public void init() {
        m_totalScores = new int[2];
        m_totalFlags = new int[2];
        m_flagsOwned = new HashMap<Short, Short>();
        m_playerScores = new HashMap<Short, Integer>();
        m_running = false;
        m_arenaLocked = false;
        m_ballCarrier = -1;

        Integer set = moduleSettings.getInteger("GoalPoints");
        if( set == null ) {
            Tools.printLog("lasertag.cfg does not contain a 'GoalPoints' paramater, using default.");
            m_goalPoints = 5;
        } else {
            m_goalPoints = set;
        }

        set = moduleSettings.getInteger("KillPointsPerFlag");
        if( set == null ) {
            Tools.printLog("lasertag.cfg does not contain a 'KillPointsPerFlag' paramater, using default.");
            m_killPoints = 1;
        } else {
            m_killPoints = set;
        }
        
        set = moduleSettings.getInteger("GameTime");
        if( set == null ) {
            Tools.printLog("lasertag.cfg does not contain a 'GameTime' paramater, using default.");
            m_gameTime = 15;
        } else {
            m_gameTime = set;
        }
        
        m_rules = new String[] {
                "+---------------------------[ The Rules of Lasertag ]---------------------------+",
                "| Two teams fight for control of flags. There is one flag on each team's turf.",
                "| For each kill you make you will receive " + m_killPoints + " point" + ((m_killPoints > 1) ? "s" : "") + " per flag owned.",
                "| Ball scores count for " + m_goalPoints + " point" + ((m_goalPoints > 1) ? "s" : "") + ".",
                "| The team with the most points at the end of " + m_gameTime + " minutes is the winner.",
                "+-------------------------------------------------------------------------------+",
        };       
        
        int winLen = m_rules[0].length();
        for(int i = 1; i < m_rules.length - 1; ++i) {
            for(int j = m_rules[i].length(); j < winLen - 1; ++j) {
                m_rules[i] += " ";
            }
            m_rules[i] += "|";
        }

        TimerTask lockTestTask = new TimerTask() {
            public void run() {
                if( m_arenaLocked ) {
                    m_botAction.toggleLocked();
                }
            }
        };

        m_botAction.setDoors(0x00);
        m_botAction.toggleLocked();
        m_botAction.scheduleTask(lockTestTask, 1000);
    }

    public void requestEvents( ModuleEventRequester events ) {
        events.request(this, PLAYER_DEATH);
        events.request(this, FLAG_CLAIMED);
        events.request(this, SOCCER_GOAL);
        events.request(this, BALL_POSITION);
    }

    public void handleEvent( Message event ) {
        String msg = event.getMessage().toLowerCase();
        
        if( event.getMessageType() == Message.ARENA_MESSAGE ) {
            if( msg.matches("^arena locked$") ) {
                m_arenaLocked = true;
            } else if( msg.matches("^arena unlocked$") ) {
                m_arenaLocked = false;
            } else if( msg.matches("^notice: game over$") ) {
                doFinishGame();
            }
        }

        if( event.getMessageType() != Message.PRIVATE_MESSAGE )
            return;

        String name = m_botAction.getPlayerName(event.getPlayerID());

        if( m_botAction.getOperatorList().isER(name) ) {
            doModCommand(name, msg);
        } else {
            doPlayerCommand(name, msg);
        }
    }

    public void doModCommand( String name, String msg ) {
        if( msg.startsWith("!start") ) {
            doPrepareGame();
        } else if( msg.startsWith("!stop") ) {
            doStopGame();
        } else if( msg.startsWith("!rules") ) {
            doArenaRulesMessage();
        } else {
            doPlayerCommand(name, msg);
        }
    }

    public void doPlayerCommand( String name, String msg ) {
        if( msg.startsWith("!rules") ) {
            doRulesMessage(name);
        } else if( msg.startsWith("!help") ) {
            doHelpMessage(name);
        } else if( msg.startsWith("!score") ) {
            doScoreMessage(name);
        } else {
            m_botAction.sendPrivateMessage(name, "Invalid command, try !help");
        }
    }

    public void handleEvent( PlayerDeath event ) {
        if( !m_running )
            return;

        short killer = event.getKillerID();
        Short freq = m_botAction.getPlayer(killer).getFrequency();
        m_totalScores[freq] += m_totalFlags[freq] * m_killPoints;

        Integer score = m_playerScores.get(killer);
        if(score == null) 
            score = 0;
        m_playerScores.put(killer, score + m_goalPoints);
    }

    public void handleEvent( FlagClaimed event ) {
        if( !m_running )
            return;

        Short freq = m_botAction.getPlayer(event.getPlayerID()).getFrequency();

        if( freq > 1 )
            return;

        Short flagId = event.getFlagID();
        Short oldFreq = m_flagsOwned.get(flagId);

        if( oldFreq == null ) {
            m_totalFlags[freq]++;
        } else if( oldFreq != freq ) {
            m_totalFlags[freq]++;
            m_totalFlags[oldFreq]--;
        }

        m_flagsOwned.put(flagId, freq);
    }

    public void handleEvent( SoccerGoal event ) {
        if( !m_running )
            return;

        Short freq = event.getFrequency();
        m_totalScores[freq] += m_goalPoints;
        
        Integer score = m_playerScores.get(m_ballCarrier);
        if(score == null) 
            score = 0;
        m_playerScores.put(m_ballCarrier, score + m_goalPoints);
    }   
    
    public void handleEvent( BallPosition event ) {
        m_ballTimestamp = event.getTimeStamp();
        if( event.getCarrier() != -1 )
            m_ballCarrier = event.getCarrier();
    }

    public boolean isUnloadable() {
        return !m_running;
    }

    public void cancel() {
        if( m_running )
            doStopGame();
    }

    @Override
    public String[] getModHelpMessage() {
        List<String> help = new ArrayList<String>();
        help.addAll(Arrays.asList(m_modHelp));
        help.addAll(Arrays.asList(m_playerHelp));
        return (String[])help.toArray(new String[m_modHelp.length + m_playerHelp.length]);
    }

    public void doPrepareGame() {
        if( m_running )
            return;
        
        TimerTask startTask = new TimerTask() {
            public void run() {
                doStartGame();
            }
        };

        if( m_arenaLocked )
            m_botAction.toggleLocked();

        m_botAction.setDoors(0xff);
        m_botAction.shipResetAll();
        m_botAction.warpAllRandomly();
        m_botAction.sendArenaMessage("The game will start in 30 seconds, enter to play.");
        m_botAction.scheduleTask(startTask, 30 * 1000);
    }

    public void doStartGame() {
        if( !m_arenaLocked )
            m_botAction.toggleLocked();
        
        m_totalScores[0] = 0;
        m_totalScores[1] = 0;
        m_botAction.shipResetAll();
        m_botAction.scoreResetAll();
        m_botAction.resetFlagGame();
        m_botAction.warpAllRandomly();
        
        /*reset ball*/
        m_botAction.getShip().setShip(0);
        m_botAction.getShip().move(512 << 4, 512 << 4);
        m_botAction.getBall((byte)0, m_ballTimestamp);
        m_botAction.getShip().setShip(8);

        m_botAction.setDoors(0x00);
        m_botAction.setTimer(m_gameTime);
        m_botAction.sendArenaMessage("The game has begun!", 104);
        m_running = true;
    }

    public void doStopGame() {
        m_botAction.cancelTasks();

        if( m_running ) {
            m_botAction.sendArenaMessage("The game has been killed.");
        }

        if( m_arenaLocked )
            m_botAction.toggleLocked();
        
        m_botAction.setDoors(0x00);
        m_running = false;
    }

    public void doFinishGame() {
        if( !m_running )
            return;

        m_running = false;
        
        if( m_totalScores[0] > m_totalScores[1] ) {
            m_botAction.sendArenaMessage("Freq 0 wins the game: " + m_totalScores[0] + "-" + m_totalScores[1]);
        } else if( m_totalScores[0] < m_totalScores[1] ) {
            m_botAction.sendArenaMessage("Freq 1 wins the game: " + m_totalScores[0] + "-" + m_totalScores[1]);
        } else {
            m_botAction.sendArenaMessage("The game is a draw: " + m_totalScores[0] + "-" + m_totalScores[1]);
        }
        
        short mvpId = 0;
        int maxScore = -1;
        
        for (Entry<Short, Integer> entry : m_playerScores.entrySet()) {
            if(entry.getValue() > maxScore) {
                mvpId = entry.getKey();
                maxScore = entry.getValue();
            }
        }

        if( maxScore != -1 )
            m_botAction.sendArenaMessage("MVP: " + m_botAction.getPlayerName(mvpId) + " (" + maxScore + ")");

        doStopGame();
    }

    public void doRulesMessage( String name ) {
        m_botAction.privateMessageSpam(name, m_rules);
    }

    public void doArenaRulesMessage() {
        m_botAction.arenaMessageSpam(m_rules);
        m_botAction.sendArenaMessage("", 1);
    }

    public void doHelpMessage( String name ) {
        m_botAction.privateMessageSpam(name, m_playerHelp);
    }
    
    public void doScoreMessage( String name ) {
        if( !m_running ) {
            m_botAction.sendPrivateMessage(name, "The game hasn't started yet.");
        } else {
            m_botAction.sendPrivateMessage(name, "Score: " + m_totalScores[0] + "-" + m_totalScores[1]);
        }
    }
}
