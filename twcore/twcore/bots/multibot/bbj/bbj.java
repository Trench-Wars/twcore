package twcore.bots.multibot.bbj;

import twcore.core.*;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.bots.MultiModule;
import twcore.bots.multibot.*;
import java.util.*;

public class bbj extends MultiModule
{

	HashMap playersHands;

	HashSet blocked;

	ArrayList cardsLeft;
	ArrayList thisRound;
	ArrayList playingBJ;
	ArrayList nextRound;
	ArrayList winners;

	TimerTask nextTimer;

	Random rand;

	int pot = 0;
	int thisHand;
	int highest = 0;

	String turn;

	boolean isRunning = false;
	boolean enabled = false;

	public void init()
	{
		playingBJ = new ArrayList();
		winners = new ArrayList();

		rand = new Random();

		blocked = new HashSet();
	}

	public void requestEvents(EventRequester events)	{
		events.request(EventRequester.MESSAGE);
		events.request(EventRequester.PLAYER_ENTERED);
	}

	public void handleEvent(PlayerEntered event)
	{
		if(enabled)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(m_botAction.getOperatorList().isZH(name) || !blocked.contains(name.toLowerCase()))
				return;
			else
				startCountDown(name);
		}
	}

	public void handleEvent(Message event)
	{
		String message = event.getMessage();
		String name = "";

		if(event.getMessageType() == Message.PRIVATE_MESSAGE)
			name = m_botAction.getPlayerName(event.getPlayerID());
		else if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
			name = event.getMessager();

		if(m_botAction.getOperatorList().isModerator(name) || name.equalsIgnoreCase("ikrit <er>"))
			handleCommand(name, message);

		handleNormalCommand(name, message);
	}

	public void handleCommand(String name, String message)
	{
		String command = message.toLowerCase();

		if(command.equals("!start"))
			startBlackJack();
		else if(command.equals("!scorereset"))
			scoreReset();
		else if(command.equals("!cancel"))
			stopBlackJack();
		else if(command.startsWith("!block "))
			addblocked(command);
		else if(command.startsWith("!remove "))
			removeblocked(command);
		else if(command.startsWith("!enable"))
			enabled = true;
		else if(command.startsWith("!disable"))
			enabled = false;
		else if(command.equals("!die"))
			m_botAction.die();
	}

	public void handleNormalCommand(String name, String message)
	{
		String command = message.toLowerCase();

		if(command.equals("!play") && playingBJ.indexOf(name) == -1 && !isRunning)
		{
			m_botAction.sendPrivateMessage(name, "You are now playing.");
			playingBJ.add(name);
		}
		else if(command.equals("!quit") && playingBJ.indexOf(name) != -1)
		{
			playingBJ.remove(name);
			m_botAction.sendPrivateMessage(name, "You have left the game.");
		}
		else if(command.equals("!score") && isRunning && (thisRound.contains(name) || nextRound.contains(name)))
			m_botAction.sendPrivateMessage(name, "Your score is currently: " + getScore(name));

		if(name.equals(turn))
		{
			if(command.equals("!stay"))
			{
				turn = "";
				stay(name);
				nextTurn();
				m_botAction.cancelTasks();
			}
			else if(command.equals("!hit"))
			{
				turn = "";
				hit(name);
				nextTurn();
				m_botAction.cancelTasks();
			}
		}

		if(command.equals("!help"))
			handleHelp(name);
	}

	public void addblocked(String message)
	{
		String pieces[] = message.split(" ", 2);
		blocked.add(pieces[1]);
	}

	public void removeblocked(String message)
	{
		String pieces[] = message.split(" ", 2);
		if(blocked.contains(pieces[1]))
			blocked.remove(pieces[1]);
	}

	public void scoreReset()
	{
		m_botAction.scoreResetAll();
		for(int k = 0;k < playingBJ.size();k++)
			m_botAction.sendUnfilteredPrivateMessage(String.valueOf(playingBJ.get(k)), "*points 200");
	}

	public void startBlackJack()
	{
		m_botAction.sendArenaMessage("A round of BlackJack is starting.");
		thisRound = playingBJ;
		pot += thisRound.size() * 25;
		playersHands = new HashMap();
		resetCards();
		nextRound = new ArrayList();
		for(int k = 0;k < playingBJ.size();k++)
			m_botAction.sendUnfilteredPrivateMessage(String.valueOf(playingBJ.get(k)), "*points -25");

		m_botAction.sendArenaMessage("First person to get a score of 21 wins the pot of " + pot + " points!!!");
		nextTurn();
	}

	public void stopBlackJack()
	{
		isRunning = false;
		m_botAction.cancelTasks();
		m_botAction.sendArenaMessage("Ok... this game is over... go home kiddies (unless someone starts it up again :)).", 5);

		if(winners.isEmpty())
			m_botAction.sendArenaMessage("haha... you all bust. Noone gets the pot.");
		else if(highest == 21)
		{
			pot /= winners.size();
			for(int k = 0;k < winners.size();k++)
			{
				m_botAction.sendUnfilteredPrivateMessage(String.valueOf(winners.get(k)), "*points " + pot);
				m_botAction.sendArenaMessage(String.valueOf(winners.get(k)) + " has recieved " + pot + " points.");
			}
			m_botAction.sendArenaMessage("Pot emptied because of those lucky people that get 21... :'(");
			pot = 0;
		}
		else
		{
			pot /= 2;
			int tempPot = pot / winners.size();
			for(int k = 0;k < winners.size();k++)
			{
				m_botAction.sendUnfilteredPrivateMessage(String.valueOf(winners.get(k)), "*points " + tempPot);
				m_botAction.sendArenaMessage(String.valueOf(winners.get(k)) + " has recieved " + tempPot + " points.");
			}
			m_botAction.sendArenaMessage("Half pot dished out to winner(s).");
		}
		highest = 0;
		winners.clear();

	}

	public void nextTurn()
	{

		if(thisRound.size() == 0)
		{
			if(nextRound.isEmpty())
			{
				stopBlackJack();
				return;
			}
			thisRound = nextRound;
			nextRound = new ArrayList();
		}
		turn = (String) thisRound.get(0);
		m_botAction.sendArenaMessage(turn + ", please choose to !hit or !stay.");
		startTimer();
	}

	public void startTimer()
	{
		nextTimer = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendArenaMessage(turn + ", sorry... you took too long and were forced to hit.");
				hit(turn);
				turn = "";
				nextTurn();
			}
		};

		m_botAction.scheduleTask(nextTimer, 10000);
	}

	public void hit(String name)
	{
		if(thisRound.contains(name))
		{
			nextRound.add(name);
			thisRound.remove(name);
		}
		int k = rand.nextInt(cardsLeft.size());
		int card = ((Integer)cardsLeft.get(k)).intValue();
		cardsLeft.remove(k);
		if(cardsLeft.isEmpty())
		{
			m_botAction.sendArenaMessage("Wow, lotsa people playin tonight... ::takes out another deck of cards::");
			resetCards();
		}
		if(!playersHands.containsKey(name))
			thisHand = 0;
		else
			thisHand = ((Integer)playersHands.get(name)).intValue();

		thisHand += card;
		playersHands.put(name, new Integer(thisHand));
		m_botAction.sendPrivateMessage(name, "You got a card worth " + card + " points and you are up to " + getScore(name) + ".");
		if(getScore(name) >= 21)
			stay(name);
	}

	public void stay(String name)
	{
		if(highest < getScore(name) && getScore(name) <= 21)
		{
			winners.clear();
			winners.add(name);
			highest = getScore(name);
		}
		else if(highest == getScore(name) && getScore(name) <= 21)
			winners.add(name);

		if(thisRound.contains(name))
			thisRound.remove(name);
		if(nextRound.contains(name))
			nextRound.remove(name);

		m_botAction.sendArenaMessage("The game is over for " + name + " and is outta here with a score of " + getScore(name));
		if(getScore(name) > 21)
			m_botAction.sendArenaMessage("AHAHA YOU BUSTED!!!", 21);
		else if(getScore(name) == 21)
			m_botAction.sendArenaMessage("Nice playin'... don't wanna see you in a casino ;)", 13);
		else if(getScore(name) < 21)
			m_botAction.sendArenaMessage("Don't feel like risking it... CHICKEN!!!", 24);
	}

	public void resetCards()
	{
		cardsLeft = new ArrayList();
		for(int k = 0;k < 8;k++)
			cardsLeft.add(new Integer(1));

		for(int k = 0;k < 8;k++)
			for(int k2 = 0;k2 < 4;k2++)
				cardsLeft.add(new Integer(k + 2));

		for(int k = 0;k < 16;k++)
			cardsLeft.add(new Integer(10));
	}

	public int getScore(String name)
	{
		if(playersHands.containsKey(name))
		{
			int score = ((Integer)playersHands.get(name)).intValue();
			return score;
		}
		else
			return 0;
	}

	public void startCountDown(String name)
	{
		final String person = name;

		Timer t = new Timer();

		TimerTask _10secs = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendPrivateMessage(person, "You have 10 seconds to leave before being kicked off...");
			}
		};

		TimerTask _5secs = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendPrivateMessage(person, "You have 5 seconds to leave before being kicked off...");
			}
		};

		TimerTask _3secs = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendPrivateMessage(person, "You have 3 seconds to leave before being kicked off...");
			}
		};

		TimerTask _2secs = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendPrivateMessage(person, "You have 2 seconds to leave before being kicked off...");
			}
		};

		TimerTask _1secs = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendPrivateMessage(person, "You have 1 second to leave before being kicked off...");
			}
		};

		TimerTask byebye = new TimerTask()
		{
			public void run()
			{
				m_botAction.sendUnfilteredPrivateMessage(person, "*kill");
			}
		};

		t.schedule(_10secs, 1000);
		t.schedule(_5secs, 6000);
		t.schedule(_3secs, 8000);
		t.schedule(_2secs, 9000);
		t.schedule(_1secs, 10000);
		t.schedule(byebye, 11000);
	}

	public void handleHelp(String name)
	{
		m_botAction.sendPrivateMessage(name, "!play              -Enters you into the blackjack game whenever it starts.");
		m_botAction.sendPrivateMessage(name, "!quit              -Takes you off the playing list.");
		m_botAction.sendPrivateMessage(name, "!stay              -Same as staying in blackjack...");
		m_botAction.sendPrivateMessage(name, "!hit               -Same as hitting in blackjack...");
	}

	public  String[] getModHelpMessage() {
    	String[] helpText = new String[] {
            	"Host Commands:",
                "!start             -Starts a game of blackjack.",
                "!cancel            -Ends the current game.",
                "!scorereset        -Sets up scores (points) for players if keeping track of points.",
                "!enable            -Enables the bouncerness of the bot.",
                "!disable           -Disables the bouncer side of the bot.",
                "!block <name>      -Gives a nonstaff player permission to enter arena.",
                "!remove <name>     -Removes that players permission to enter."
            };
        return helpText;
    }

    public boolean isUnloadable() {
    	return true;
    }
}