package twcore.bots.purepubbot;

import twcore.core.BotAction;

public class LvzHandler {

    private BotAction botAction;
    
    public LvzHandler(BotAction botAction){
        this.botAction = botAction;
    }
    
    public PubPlayer handleLvzMoney(PubPlayer player, int playerId, String moneyCash, boolean gainedMoney){
        //objset.hideAllObjects(playerId);
       
        int number = Integer.parseInt(moneyCash);
        int vectorObjon[] = player.getObjon();
        //objset.hideAllObjects();
        for(int i = 0; i < moneyCash.length(); i++)
        {
            String playerName = botAction.getPlayerName(playerId);
            //resetDigits(playerName);
            int digit = number%10;
            number/=10;
            botAction.sendUnfilteredPrivateMessage(playerName, "*objoff "+502+i+vectorObjon[i]);
            // System.out.println("Objoff: "+502+i+vectorObjon[i]);
            //botAction.sendArenaMessage("Number: "+digit+" Objon: "+502+i+digit);
            //System.out.println("Numero: "+digit+" Objon: "+502+i+digit);
            //botAction.sendUnfilteredPrivateMessage(playerName, "*objoff "+502+i+vectorObjon[i]);
            botAction.sendUnfilteredPrivateMessage(playerName, "*objon "+502+i+digit);
            vectorObjon[i] = digit;
            if(gainedMoney)
                botAction.sendUnfilteredPrivateMessage(playerName, "*objon "+50100);
            else
                botAction.sendUnfilteredPrivateMessage(playerName, "*objon "+50101);
            //objset.showObject(502+i+digit);
            
            player.setObjon(vectorObjon);
        }
        return player;
       
    }
    
/*    public void resetDigits(String playerName){
        for(int i = 0; i < 7; i++){
           for(int j = 0; j < 7; j++){
            botAction.sendUnfilteredPrivateMessage(playerName, "*objoff "+502+i+j);
            System.out.println("Objoff: "+502+i+j);
           }
       }
    }*/
   /* public static void main(String args[]){
        LvzHandler lvzHandler = new LvzHandler();
        String str = "5444";
        lvzHandler.handleLvzMoney(0, str);
    }
    */
}
