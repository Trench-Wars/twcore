package twcore.bots.multibot.prez;

import java.util.HashMap;
import twcore.bots.MultiModule;
import twcore.core.command.CommandInterpreter;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.events.PlayerLeft;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Point;

/**
 * Multibot Module - President
 * 
 * Assists in hosting of president type games. 
 * @see http://twcore.org/ticket/739
 * 
 * @author SpookedOne, Magnetic <ER>
 */
public class prez extends MultiModule{

    private CommandInterpreter  m_commandInterpreter;
   
    private int numOfFreqs;
    private int citzShip;
    private int prezShip;
    private HashMap<Integer, Point> freqWarpPoints;
    private HashMap<Integer, String> freqPrezs;
    
    //game state least us know when to listen to message callbacks from server
    //  in handleEvent methods
    private enum State {off, on};
    private State state = State.off;
    
    @Override
    public void init()    {
        m_commandInterpreter = new CommandInterpreter(m_botAction);
        registerCommands();
    }

    @Override
    public void requestEvents(ModuleEventRequester events) {}

    @Override
    public boolean isUnloadable()   {
        return true;
    }
    
    /*
     * This method is called when this module is unloaded.
     */
    @Override
    public void cancel(){
    	m_botAction.cancelTasks();
    }

    /*
     * Register commands for staff using Prez module.
     */
    public void registerCommands()  {
        //only allow private messages
        int acceptedMessages = Message.PRIVATE_MESSAGE;
        //register our commands with appropriate handlers
        m_commandInterpreter.registerCommand( "!start", acceptedMessages, this, "doStartGame" );
        m_commandInterpreter.registerCommand( "!setfreqs", acceptedMessages, this, "doSetFreqs" );
        m_commandInterpreter.registerCommand( "!setcitzship", acceptedMessages, this, "doSetCitzShip" );
        m_commandInterpreter.registerCommand( "!setprezship", acceptedMessages, this, "doSetPrezShip" );
        m_commandInterpreter.registerCommand( "!setfreqwarp", acceptedMessages, this, "doSetFreqWarp" );
        m_commandInterpreter.registerCommand( "!stop", acceptedMessages, this, "doStopGame" );
        m_commandInterpreter.registerCommand( "!help", acceptedMessages, this, "doShowHelp" );
        m_commandInterpreter.registerCommand( "!rules", acceptedMessages, this, "doShowRules" );
    }

    /*
     * Command handler for !start
     */
    public void doStartGame(String name,String message) {
        //verify is staff member
        if( m_botAction.getOperatorList().isER(name)) {
            if (isAllSet(name)){
                //initiates the game
                initGame();
            }
        }
    }

    /*
     * Command handler for !stop
     */
    public void doStopGame(String name, String message) {
        //verify is staff member
        if(m_botAction.getOperatorList().isER( name )) {
        	gamereset();
            m_botAction.sendArenaMessage("This game has been slaughtered by: " + name);
        }
    }
    
    /*
     * Command handler for !setcitzship
     */
    public void doSetCitzShip(String name, String message) {
        if(m_botAction.getOperatorList().isER( name )) {
            citzShip = Integer.parseInt(message);
        }
    }
    
    /*
     * Command handler for !setprezship
     */
    public void doSetPrezShip(String name, String message) {
        if(m_botAction.getOperatorList().isER( name )) {
            prezShip = Integer.parseInt(message);
        }
    }
    
    /*
     * Command handler for !setfreqwarp 
     */
    public void doSetFreqWarp(String name, String message) {
        if(m_botAction.getOperatorList().isER( name )) {
            String[] split = message.split(":");
            int freq = Integer.parseInt(split[0]);
            String [] coord = split[1].split(",");
            int x = Integer.parseInt(coord[0]);
            //TODO finish parsing coord, create new Point, and place in freqWarpPoints
        }
    }
    
    /*
     * Verify all members set for module, pass name so that you may tell staffer
     * which members require setting before start.
     */
    public boolean isAllSet(String name) {
        boolean valid = true;
        //TODO verify number of freqs, if not set valid false and notify name
        //TODO verify warps set for each freq, if not set valid false and notify name
        //TODO verify citz ship set, "..."
        //TODO verify prez ship set, "..."
        return valid;
    }
    
    /*
     * Initiates the module to start game
     */
    public void initGame() {
        //TODO lock arena
        //TODO randomize players and assign to freqs (look for !increment command for example)
        //TODO set all players to citz ship 
        //TODO pick random person from each freq and set to prez ship 
        //TODO warp freqs to were they assigned
        state = State.on;   //set game state to on to start listening to messages
    }

    /*
     * Reset the module to it's default state
     */
    public void gamereset() {
    }

    /*
     * Notify arena of game over and announce winning prez/freq
     */
    public void gameOver(String prezName) {
    }

    @Override
    public String[] getModHelpMessage() {
        String[] help = {
                "PREZ (President) BOT COMMANDS",
                "!start       - Starts a game of Prez.",
                "!stop        - Stops a game currently in progress."
                //TODO fill in reset
        };
        return help;
    }

    public void doShowRules(String name,String message) {
        String[] help = {
                "PREZ (President) RULES:",
                "Each team has one president..."
                //TODO fill in rest
        };
        m_botAction.privateMessageSpam(name,help);
    }
    
    @Override
    public void handleEvent(Message event) {
        m_commandInterpreter.handleEvent(event);
    }

    @Override
    public void handleEvent(PlayerLeft event){
        if (state == State.on) {
            //TODO check if aprez left, assign new random player from same freq
            //TODO change player ship to prez, then warp player to point
        }
    }

    @Override
    public void handleEvent(PlayerDeath event){
        if (state == State.on) {
            //TODO check if president died, if so
            //TODO remove president from map, spec the entire freq
            //TODO check for count of remaining prezs, if only 1
            //TODO do gameover
        }
    }
}
