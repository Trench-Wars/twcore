package twcore.bots.elimbot.elimstate;

import twcore.core.events.FrequencyShipChange;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;

public class ElimStateVoting
        extends ElimState {
    
    private int [] votes = new int[9];
    public ElimStateVoting() {
        // TODO Auto-generated constructor stub
        for(int i = 0; i < votes.length ; i++)
            votes[i] = 0; //O(n)
    }
    
    @Override
    public void handleEvent(PlayerDeath event) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleEvent(Message event) {
        // TODO Auto-generated method stub
        int messageType = event.getMessageType();
        if(messageType == Message.PUBLIC_MESSAGE ){
            String publicMessage = event.getMessage();
            System.out.println("Msg: "+publicMessage);
            handleVotingMessage(publicMessage);
        }
    }


    private void handleVotingMessage(String publicMessage) {
        // TODO Auto-generated method stub
        if( publicMessage.length() > 2 )
            return ;
       
        try{
            int voteNumber = Integer.parseInt(publicMessage);
            votes[voteNumber]++;
            System.out.println(voteNumber);
            
        }catch(NumberFormatException e){
            e.printStackTrace();
            System.out.println("Message '" +publicMessage+"'  is not a voting number");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Voting State";
    }

    @Override
    public int[] getVotes() {
        // TODO Auto-generated method stub
        return votes;
    }

}
