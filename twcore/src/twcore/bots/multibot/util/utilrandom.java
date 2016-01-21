package twcore.bots.multibot.util;

import java.util.Iterator;
import java.util.Random;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.game.Player;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.StringBag;

/**
    Random module

    Based on Golden which was based on conquer module
    And I bloody stole a function from zombies.
    PS: bloody stole function from hunt too.

    @author  Kyace
*/
public class utilrandom extends MultiUtil {

    Random rand = new Random();

    String Magic8Ball[] = {"Signs point to yes.",
                           "Yes.",
                           "Reply hazy, try again.",
                           "Without a doubt.",
                           "My sources say no.",
                           "As I see it, yes.",
                           "You may rely on it.",
                           "Concentrate and ask again.",
                           "Outlook not so good.",
                           "It is decidedly so.",
                           "Better not tell you now.",
                           "Very doubtful.",
                           "Yes - definitely.",
                           "It is certain.",
                           "Cannot predict now.",
                           "Most likely.",
                           "Ask again later.",
                           "My reply is no.",
                           "Outlook good.",
                           "Don't count on it."
                          };

    public void init() {
    }

    /**
        Requests events.
    */
    public void requestEvents( ModuleEventRequester modEventReq ) {
    }

    /**
        This method checks message for power and sends to handlecommand.

        @param event is the message event.
    */
    public void handleEvent( Message event ) {
        String message = event.getMessage();

        if( event.getMessageType() == Message.PRIVATE_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );

            if( m_opList.isER( name )) handleCommand( name, message );
        } else if( event.getMessageType() == Message.PUBLIC_MESSAGE ) {
            String name = m_botAction.getPlayerName( event.getPlayerID() );
            handlePublic( name, message );
        }
    }

    /**
        This method does actions depending what you PM bot with and your powers.

        @param name is the person that messaged the bot.
        @param message is text they sent.
    */
    public void handleCommand( String name, String message ) {
        if( message.startsWith( "!randomplayer" )) {
            Player p;
            String addPlayerName;
            StringBag randomPlayerBag = new StringBag();
            Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

            if( i == null ) return;

            while( i.hasNext() ) {
                p = (Player)i.next();
                addPlayerName = p.getPlayerName();
                //HuntPlayer temp;
                randomPlayerBag.add(addPlayerName);
            }

            addPlayerName = randomPlayerBag.grabAndRemove();
            m_botAction.sendPrivateMessage(name, "Random player: " + addPlayerName );

        }
        else if( message.toLowerCase().startsWith( "!rteams " )) {
            if( getInteger( message.substring( 8 )) < 1 ) {
                m_botAction.sendPrivateMessage( name, "Numbers cannot be less then 1" );
            } else {
                int freqsMax = getInteger( message.substring( 8 ));
                Player p;
                String addPlayerName;
                StringBag randomPlayerBag = new StringBag();
                Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

                if( i == null ) return;

                while( i.hasNext() ) {
                    p = (Player)i.next();
                    addPlayerName = p.getPlayerName();
                    //HuntPlayer temp;
                    randomPlayerBag.add(addPlayerName);
                }

                for (int currFreq = 0; !randomPlayerBag.isEmpty(); currFreq++ ) {
                    if (currFreq == freqsMax)
                        currFreq = 0;

                    addPlayerName = randomPlayerBag.grabAndRemove();
                    m_botAction.setFreq(addPlayerName, currFreq);
                }

                m_botAction.sendPrivateMessage( name, "Made " + freqsMax + " random freqs of players.");
            }
        }
    }

    public void handlePublic( String name, String message ) {
        if( message.startsWith( "!sayrandomplayer" )) {
            Player p;
            String addPlayerName;
            StringBag randomPlayerBag = new StringBag();
            Iterator<Player> i = m_botAction.getPlayingPlayerIterator();

            if( i == null ) return;

            while( i.hasNext() ) {
                p = (Player)i.next();
                addPlayerName = p.getPlayerName();
                //HuntPlayer temp;
                randomPlayerBag.add(addPlayerName);
            }

            addPlayerName = randomPlayerBag.grabAndRemove();
            m_botAction.sendPublicMessage("Random player: " + addPlayerName );

        }
        else if( message.toLowerCase().startsWith( "!magic8ball " )) {
            int die = rand.nextInt(Magic8Ball.length);
            m_botAction.sendPublicMessage("Answer: " + Magic8Ball[die] );
        } else if( message.toLowerCase().startsWith( "!rolldice" )) {
            int die = 1 + rand.nextInt(6);
            m_botAction.sendPublicMessage("You rolled a " + die + ".");
        } else if( message.toLowerCase().startsWith( "!tosscoin" )) {
            if (rand.nextInt(2) == 1 ) {
                m_botAction.sendPublicMessage("Heads!");
            } else {
                m_botAction.sendPublicMessage("Tails!");
            }
        }
    }

    /**
        This method returns the help.

        @return String the helps for this module
    */
    public String[] getHelpMessages() {
        String[] help = {
            "    Public commands:",
            "!sayrandomplayer - Says in public a random player's name",
            "!rolldice        - Says in public a random number from 1 to 6",
            "!tosscoin        - Says 'heads' or 'tails' in public randomly",
            "!magic8ball <q>  - Answers q in public randomly",
            "    PM commands:",
            "!randomplayer    - PMs you with name of random player",
            "!rteams <#>      - Creates # teams randomly using stringbag"
        };
        return help;
    }

    /**
        This method converts a string into an int
        I am bloody lazy so I stole this from hunt module.

        @param input is an string to be converted into an int.
        @return the int value of the string
    */
    public int getInteger( String input ) {
        try {
            return Integer.parseInt( input.trim() );
        } catch( Exception e ) {
            return 1;
        }
    }

    public void cancel() {
    }

}