package twcore.bots.elimbot.elimsystem;

import java.util.Timer;
import java.util.TimerTask;

import twcore.bots.elimbot.elimstate.ElimState;
import twcore.bots.elimbot.elimstate.ElimStateGameRunning;
import twcore.bots.elimbot.elimstate.ElimStateOff;
import twcore.bots.elimbot.elimstate.ElimStatePreRunning;
import twcore.bots.elimbot.elimstate.ElimStateVoting;
import twcore.core.BotAction;
import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
/**
 * @author quiles
 * @see divide and conquer algorithm to voting state
 * @see state pattern to elim system
 * @see timertasks to each state
 * */
public class ElimSystem {

    private ElimState state;
    private BotAction botAction;
    
    public ElimSystem() {
        // TODO Auto-generated constructor stub
    }
    
    public ElimSystem(BotAction botAction) {
        // TODO Auto-generated constructor stub
        this.state = new ElimStateVoting();
        this.botAction = botAction;
        startElim();
    }

    public void handleEvent(PlayerDeath event){
        this.state.handleEvent(event);
    }
    
    public void handleEvent(FrequencyShipChange event){
        
    }
    
    public void handleEvent(Message event){
        String playerName = botAction.getPlayerName(event.getPlayerID());
        this.botAction.sendPrivateMessage(playerName, "This is the new bot system under development.");
        state.handleEvent(event);
    }
    
    public void startElim(){
        state = new ElimStateVoting();
        System.out.println("Voting state");
        setStateToVoting(); //state = new ElimStatePreRunning(); after seconds
        
        //after 30 seconds
        state = new ElimStateGameRunning();
        
    }
    public void setStateToVoting(){
        int seconds = 30;
        state = new ElimStateVoting();
        new TimerNextStatePreRunning(new Timer(), seconds);
       
    }
    
    public void setStateToPreRunning(){
        int seconds = 15;
        int vectorVotes[] = state.getVotes();
        state = new ElimStatePreRunning();
        new TimerNextStateGameRunning(new Timer(), seconds);
        
    }
    
    /**
    * Its possible to find the max number of a vector and his position in it.
    * algorithm by quiles
    * divide and conquer
    * T(n) = T(n/2) + 1
    * By substitution we have that T(n) might be O(n)
    * So T(n) <= c.n
    * T(n/2) <= c(n/2)
    * Then T(n) <= c.(n/2) + 1
    * if the vector has 1 element, we do 0 comparisons
    * if the vector has 2 elements, we do 1 comparisons
    * if the vector has 3 elements, we do 2 comparisons
    * so f(3) = 2
    * then 2 <= c(3/2) + 1 <=> 1 <= c.(3/2) => c >= 2/3
    * then T(n) <= (2/3).(n/2) + 1 <=> T(n) <= n/3 + 1 comparisons
    * it proves too that T(n) = O(n).
    */
    public int[] getMax(int vector[], int begin, int end){
        
        //Base
        if( (end - begin) < 2 ){
            if( (end - begin) == 0){ //Base 1: one element, in case of odd size of vector
                int [] v = {begin, vector[begin]};
                return v;//vector[begin];
            }
            if( vector[begin] >= vector[end] ){ //Base 2: two elementos
                int [] v = {begin, vector[begin]};
                return v;//vector[begin];
            }
            
            int v[] = {end, vector[end]};
            return v;//vector[end];
        }
        
        int middlePosition = (int) (begin+end)/2;

        //Hipothesis
        int [] max1 = getMax(vector, begin, middlePosition);
        int [] max2 = getMax(vector, middlePosition+1, end);
        
        //Pass
        return max1[1] > max2[1]? max1:max2;
    }
    
    public void setStateToGameRunning(){
        state = new ElimStateGameRunning();
    }
    
    private class TimerNextStatePreRunning extends TimerTask{
        private Timer timer;
        
        private TimerNextStatePreRunning(Timer timer, int seconds) {
            // TODO Auto-generated method stub
            this.timer = timer;
            timer.schedule(this, seconds*1000);
            System.out.println("Timer set to "+seconds+"s.");
        }
        
        @Override
        public void run() {
            // TODO Auto-generated method stub
            setStateToPreRunning();
            System.out.println(state.toString()+" started");
            timer.cancel();
        }
        
    }
    
    private class TimerNextStateGameRunning extends TimerTask{
        private Timer timer;
        
        public TimerNextStateGameRunning(Timer timer, int seconds) {
            // TODO Auto-generated constructor stub
            this.timer = timer;
            timer.schedule(this, seconds*1000);
            System.out.println("Timer set to "+seconds+"s.");
        }
        
        @Override
        public void run() {
            // TODO Auto-generated method stub
            setStateToGameRunning();
            System.out.println(state.toString()+" started");
            timer.cancel();
        }
    
    }
    
}
