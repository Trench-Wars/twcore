/*
* portabotTestModule.java
*
* Created on March 21, 2002, 4:14 PM
*/

/**
*
* @author  harvey
*/
package twcore.bots.twbot;

import twcore.bots.shared.TWBotExtension;
import twcore.core.Player;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;

public class twbotkillrace extends TWBotExtension {
   /** Creates a new instance of portabotTestModule */
   public twbotkillrace() {
   }

   private boolean isRunning = false;
   private int KillsNeeded = 50;
   private int Evens_Score = 0;
   private int Odds_Score = 0;

   public void handleEvent( PlayerDeath event ){
       if( !isRunning ) return;
       Player killer = m_botAction.getPlayer( event.getKillerID() );
       int team = killer.getFrequency();
       team = team % 2;
       if( team == 1 )
         {Odds_Score++;
         if (Odds_Score >= KillsNeeded)
           goWin(1);}
       if( team == 0 )
         {Evens_Score++;
         if (Evens_Score >= KillsNeeded )
           goWin(0);}
   }

   private void goWin(int winners){
       //Displays the end of game info and sets isRunning back to false
       if (winners == 1)
         m_botAction.sendArenaMessage( "The Odds Frequency has won! Odds: " + Odds_Score + " Evens: " + Evens_Score );
       if (winners == 0)
         m_botAction.sendArenaMessage( "The Evens Frequency has won! Evens: " + Evens_Score +" Odds: " + Odds_Score);
   }

   public void handleEvent( Message event ){

       String message = event.getMessage();
       if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
           String name = m_botAction.getPlayerName( event.getPlayerID() );
           if( m_opList.isER( name )) handleCommand( name, message );
           if(message.startsWith ("!score"))
             m_botAction.sendPrivateMessage(name, "Score is currently: Evens: " + Evens_Score +" Odds: " + Odds_Score);
       }
   }

   public void handleCommand( String name, String message ){
       if( message.startsWith( "!start" )){
           m_botAction.sendArenaMessage( "Kill race mode activated by " + name );
           isRunning = true;
           Evens_Score = 0;
           Odds_Score = 0;
           m_botAction.scoreResetAll();
       } else if( message.startsWith( "!stop" )){
           m_botAction.sendArenaMessage( "Kill race mode deactivated by " + name );
           m_botAction.sendArenaMessage( "Score: Evens: " + Evens_Score +" Odds: " + Odds_Score );
           isRunning = false;
       } else if( message.startsWith( "!setx " )){
               KillsNeeded = getInteger( message.substring( 6 ));
/*              Integer tempgetInteger( message.substring( 6 ));//(message.substring( 6 ));
           KillsNeeded = temp.intValue();*/
           m_botAction.sendArenaMessage( "Kills needed to win set to " + KillsNeeded );
       }
   }

   public String[] getHelpMessages() {
       String[] help = {
           "Kill Race - First team to X kills wins (default 50)",
           "            teams are evens vs odds (freqs).",
           "!setx #   - sets X (number of kills) to the number",
           "!start    - starts kill race mode  (Will not *lock)",
           "!stop     - stops kill race mode"
       };
       return help;
   }

   public void cancel() {
   }

   public int getInteger( String input ){
   //Blandently stolen from hunt module.
       try{
           return Integer.parseInt( input.trim() );
       } catch( Exception e ){
           return 1;
       }
   }
}