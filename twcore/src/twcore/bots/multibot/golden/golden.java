package twcore.bots.multibot.golden;

import java.util.Iterator;
import java.util.TimerTask;

import twcore.bots.MultiModule;
import twcore.core.EventRequester;
import twcore.core.util.ModuleEventRequester;
import twcore.core.events.Message;
import twcore.core.events.PlayerDeath;
import twcore.core.game.Player;
import twcore.core.util.StringBag;
import twcore.core.util.Tools;

/**
 * Golden Gun module
 *
 * Based on conquer module
 * And I bloody stole a function from zombies.
 * PS: bloody stole function from hunt too.
 *
 * @author  Kyace
 */
public class golden extends MultiModule {

   boolean isRunning = false;
   String hasGun = "";
   String killMessage = " has got the Golden Gun! Run!";
   int gunShip = 1;
   int gunFreq = 1;
   int humanShip = 1;
   int humanFreq = 0;
   int specPlayers = 25;
   int resetDelay = 5;
   TimerTask resetPlayer;
   TimerTask goldenPrizes;
   
   public void init() {
   }

   public void requestEvents(ModuleEventRequester events) {
       events.request(this, EventRequester.PLAYER_DEATH);
   }

   /**
    * This method switchs ships and freqs and specs depending
    * on if guy with gun died and limit and such.
    *
    * @param event is the player death event.
    */
   public void handleEvent( PlayerDeath event ){
       if( !isRunning ) return;
       
       Player killer = m_botAction.getPlayer( event.getKillerID() );
       if( killer == null )
           return;

       String killeename = m_botAction.getPlayerName( event.getKilleeID() );
       String killername = m_botAction.getPlayerName( event.getKillerID() );
       Player deathGuy = m_botAction.getPlayer( event.getKilleeID() );
       Player killerGuy = m_botAction.getPlayer( event.getKillerID() );
       //If the person dead has the golden gun then switch ships.
//Old check       if ((killeename.compareToIgnoreCase( hasGun) == 0) && (deathGuy.getFrequency() == gunFreq) && (deathGuy.isShip(gunShip)))
       if ((deathGuy.getFrequency() == gunFreq) && (deathGuy.isShip(gunShip)))
           {if (m_botAction.getPlayer( event.getKilleeID() ).isPlaying() )
               {m_botAction.setShip(killeename, humanShip);
               //Sets old Golden Gunner to human ship
               m_botAction.setFreq(killeename, humanFreq);
               //Sets old Golden Gunner to human freq
               }
           if (m_botAction.getPlayer( event.getKilleeID() ).isPlaying() )
               {m_botAction.setFreq(killername, gunFreq);
               //Sets new golden Gunner to gun freq
               m_botAction.setShip(killername, gunShip);
               //Sets new golden Gunner to gun ship
               m_botAction.specificPrize(killername, 17);
               //Handles prizes for new Golden Gunner
               final String newGun = killername;
               goldenPrizes = new TimerTask() {
            	   // @Override
            	   	public void run() {
            	   		m_botAction.specificPrize(newGun, Tools.Prize.SUPER);
					
				   }   
               };
               m_botAction.scheduleTask( goldenPrizes, Tools.TimeInMillis.SECOND * 1, Tools.TimeInMillis.SECOND * 5 );
               }
           final String oldGun = killeename;
            resetPlayer = new TimerTask() {
            	// @Override
               public void run() {
                    m_botAction.shipReset( oldGun );
                    
                    }
               };
           m_botAction.scheduleTask( resetPlayer, resetDelay * Tools.TimeInMillis.SECOND );
           //Remembers new gunner  //No it doesnt? that code would perform a timertask thats only function is to shipreset 5 seconds a
           m_botAction.sendArenaMessage( killername + killMessage); 
           //Announces new gunner.
           }
       if ( deathGuy.getLosses() >= specPlayers ){
           m_botAction.spec( event.getKilleeID() );
           m_botAction.spec( event.getKilleeID() );
           m_botAction.sendArenaMessage( killeename + " is out with " + deathGuy.getWins() + " kills, " + deathGuy.getLosses() + " losses." );
           }

       deathGuy = m_botAction.getPlayer( event.getKillerID() );
       if ( deathGuy.getLosses() >= specPlayers ){
           m_botAction.spec( event.getKillerID() );
           m_botAction.spec( event.getKillerID() );
           m_botAction.sendArenaMessage( killername + " is out with " + deathGuy.getWins() + " kills, " + deathGuy.getLosses() + " losses." );
           }

   }

   /**
    * This method checks message for power and sends to handlecommand.
    *
    * @param event is the message event.
    */
   public void handleEvent( Message event ){
       String message = event.getMessage();
       if( event.getMessageType() == Message.PRIVATE_MESSAGE ){
           String name = m_botAction.getPlayerName( event.getPlayerID() );
           if( opList.isER( name )) handleCommand( name, message );
       }
   }

   /**
    * This method does actions depending what you PM bot with and your powers.
    *
    * @param name is the person that messaged the bot.
    * @param message is text they sent.
    */
   public void handleCommand( String name, String message ){
       if( message.startsWith( "!start " )){
           if( isRunning )
              {m_botAction.sendPrivateMessage(name, "Golden Gun is already running, cannot start.");
               return;}
           m_botAction.sendPrivateMessage(name, "Golden Gun activated");
           //Announces new game.
           hasGun = message.substring( 7 );
           //Shortens command to part refering to name.
           hasGun = m_botAction.getFuzzyPlayerName(hasGun);
           //Uses fuzzy name to allow first part of name instead of requiring full.
           m_botAction.changeAllShips( humanShip );
           //Sets all to human ship.
           m_botAction.setAlltoFreq( humanFreq );
           //Sets all to human freq.
           m_botAction.setShip( hasGun, gunShip);
           //sets gunner to gunner ship
           m_botAction.setFreq( hasGun, gunFreq);
           //sets gunner to gunner freq.
           m_botAction.sendArenaMessage( hasGun + killMessage);
           //Announces new gunner.
           isRunning = true;
           //Will now check upon deaths.
           }
       if( message.startsWith( "!startrandom" )){
           if( isRunning )
               {m_botAction.sendPrivateMessage(name, "Golden Gun is already running, cannot start.");
               return;}
           Player p;
           String addPlayerName;
           StringBag randomPlayerBag = new StringBag();
           Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
           if( i == null ) return;
           while( i.hasNext() ){
               p = (Player)i.next();
               addPlayerName = p.getPlayerName();
           //HuntPlayer temp;
           randomPlayerBag.add(addPlayerName);
           }
           hasGun = randomPlayerBag.grabAndRemove();
           m_botAction.sendPrivateMessage(name, "Golden Gun activated");
           m_botAction.changeAllShips( humanShip );
           //Sets all to human ship.
           m_botAction.setAlltoFreq( humanFreq );
           //Sets all to human freq.
           m_botAction.setShip( hasGun, gunShip);
           //sets gunner to gunner ship
           m_botAction.setFreq( hasGun, gunFreq);
           //sets gunner to gunner freq.
           m_botAction.sendArenaMessage( hasGun + killMessage);
           //Announces new gunner.
           isRunning = true;
           //Will now check upon deaths
       }
       if( message.startsWith( "!randomplayer" )){
           Player p;
           String addPlayerName;
           StringBag randomPlayerBag = new StringBag();
           Iterator<Player> i = m_botAction.getPlayingPlayerIterator();
           if( i == null ) return;
           while( i.hasNext() ){
               p = (Player)i.next();
               addPlayerName = p.getPlayerName();
           //HuntPlayer temp;
           randomPlayerBag.add(addPlayerName);
           }
           addPlayerName = randomPlayerBag.grabAndRemove();
           m_botAction.sendPrivateMessage(name,"Random player: " + addPlayerName );

       }
       else if( message.startsWith( "!stop" )){
           if( !isRunning ) {
               m_botAction.sendPrivateMessage(name, "Golden Gun is already stopped, cannot stop.");
               return;
           }
           m_botAction.sendPrivateMessage(name, "Golden Gun deactivated");
           isRunning = false;
           try {
               resetPlayer.cancel();
           } catch (Exception e) {}
           //Stops checking at death.
       }
       else if( message.startsWith( "!setgun " )){
           //Pretty much same code as !start but doesn't announce new game and doesn't set isRunning true.
            if( !isRunning )
               {m_botAction.sendPrivateMessage(name, "Golden Gun is stopped, just start with that person.");
               return;}
/*           m_botAction.setShip(hasGun, humanShip);
           //Sets old Golden Gunner to human ship
           m_botAction.setFreq(hasGun, humanFreq);
           //Sets old Golden Gunner to human freq*/
           hasGun = message.substring( 8 );
           hasGun = m_botAction.getFuzzyPlayerName(hasGun);
           m_botAction.sendArenaMessage( hasGun + killMessage);
           m_botAction.setShip( hasGun, gunShip);
           m_botAction.setFreq( hasGun, gunFreq);

       }
       else if( message.startsWith( "!setmessage " )){

           killMessage = " " + message.substring( 12 );
           m_botAction.sendPrivateMessage(name, "New message: <name>" + killMessage);

       }
       else if( message.startsWith( "!status" )){
           //This was a handy testing function that I left in because I like it.
//           m_botAction.sendPrivateMessage(name, hasGun + " has the gun.");
           if( isRunning )
           m_botAction.sendPrivateMessage(name, "Golden Gun is running.");
           if( !isRunning )
           m_botAction.sendPrivateMessage(name, "Golden Gun is NOT running.");
           m_botAction.sendPrivateMessage(name, "Humans: Freq " + humanFreq + " Ship " + humanShip);
           m_botAction.sendPrivateMessage(name, "Gunner: Freq " + gunFreq + " Ship " + gunShip);
           m_botAction.sendPrivateMessage(name, "Kill message: <name>" + killMessage);
           m_botAction.sendPrivateMessage(name, "Death limit set to: " + specPlayers );
           m_botAction.sendPrivateMessage(name, "Shipreset delay (seconds) set to " + resetDelay);
       }
       else if( message.startsWith( "!game " )){
           //This will change the freqs and ships for both gunner and human.
           if( isRunning )
               {m_botAction.sendPrivateMessage(name, "Golden Gun is already running, cannot change ships.");
               return;}
           String[] parameters = Tools.stringChopper( message.substring( 6 ), ' ' );
           game( name, parameters );
       }
       else if( message.toLowerCase().startsWith( "!goldspec " )){
       if(isRunning){
           m_botAction.sendPrivateMessage( name, "Cannot change !goldspec while a game is in progress" );
           } else {
               if( getInteger( message.substring( 10 )) < 1 ){
                   m_botAction.sendPrivateMessage( name, "The !goldspec cannot be less then 1" );
               } else {
                   specPlayers = getInteger( message.substring( 10 ));
                   m_botAction.sendPrivateMessage( name, "Death limit set to: " + specPlayers );
               }
           }
       }
              else if( message.toLowerCase().startsWith( "!resetdelay " )){
           if( getInteger( message.substring( 12 )) < 1 ){
               m_botAction.sendPrivateMessage( name, "The !resetdelay cannot be less then 1" );
           } else {
               resetDelay = getInteger( message.substring( 12 ));
               m_botAction.sendPrivateMessage( name, "Shipreset delay (seconds) set to: " + resetDelay );
           }
       }
   }


   /**
    * This method sets game values for ship and freqs.
    * I am bloody lazy so I stole this from zombies module.
    *
    * @param sender is the person that messaged the bot.
    * @param params are the parameters in a string.
    */
   public void game( String name, String[] params ){
       try{
           if( params.length == 4 ){
               humanFreq = Integer.parseInt(params[0]);
               humanShip = Integer.parseInt(params[1]);
               gunFreq = Integer.parseInt(params[2]);
               gunShip = Integer.parseInt(params[3]);
           m_botAction.sendPrivateMessage(name, "Humans: Freq " + humanFreq + " Ship " + humanShip);
           m_botAction.sendPrivateMessage(name, "Gunner: Freq " + gunFreq + " Ship " + gunShip);
           }
       }catch( Exception e ){
           m_botAction.sendPrivateMessage( name, "Sorry, you made a mistake, please try again." );
       }
   }


   /**
    * This method converts a string into an int
    * I am bloody lazy so I stole this from hunt module.
    *
    * @param input is an string to be converted into an int.
    * @return the int value of the string
    */
   public int getInteger( String input ){
       try{
           return Integer.parseInt( input.trim() );
       } catch( Exception e ){
           return 1;
       }
   }

   public String[] getModHelpMessage() {
       String[] GoldenHelp = {
               "!startrandom     - starts Golden Gun with random gunner",
               "!start <name>    - starts Golden Gun with name as gunner",
               "!setgun <name>   - sets a new gunner",
               "!stop            - stops Golden Gun mode",
               "!status          - returns the status",
               "!game <human freq> <human ship> <gunner freq> <gunner ship>",
               "                 - sets freqs and ships for game.",
               "                 - default !game 0 1 1 1",
               "!randomplayer    - PMs you with name of random player" ,
               "!setmessage <message>",
               "                 - Changes the arena message when new golden gun.",
               "!goldspec <#>    - sets death limit.",
               "!resetdelay <#)  - changes deley between when goldengun dies and *shipreset"
       };
       return GoldenHelp;
   }

   public void cancel() {
       try {
           resetPlayer.cancel();
       } catch (Exception e) {}
       m_botAction.cancelTask(goldenPrizes)
   }

   public boolean isUnloadable()    {
        return true;
    }

}