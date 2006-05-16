package twcore.bots.multibot.poker;

import java.util.*;
import java.sql.*;
import twcore.core.*;
import twcore.misc.multibot.*;

public class poker extends MultiModule
{
	HashMap seats;
	ArrayList playerSeat;
	String[] onTable;
	boolean isRunning;
	String turn;
	PokerDeck d;
	int status;
	int dealer;
	int curBet;
	int maxBet;
	int thisBet;
	int pot;
	TimerTask nextPlayer, warning;

	public void init()
	{
		seats = new HashMap();
		d = new PokerDeck();
		playerSeat = new ArrayList();
		isRunning = true;
		dealer = 0;
		curBet = 0;
		maxBet = 0;
		thisBet = 0;
		pot = 0;
		status = -1;
		turn = "";
	}

	public boolean isUnloadable()
	{
		return !isRunning;
	}

	public String[] getModHelpMessage()
	{
		return new String[0];
	}

	public void requestEvents(EventRequester events)
	{
		events.request(events.MESSAGE);
		events.request(events.LOGGED_ON);
		events.request(events.PLAYER_LEFT);
	}

	/*public pokerbot(BotAction botAction) {
		super(botAction);

		seats = new HashMap();

		EventRequester events = m_botAction.getEventRequester();
		events.request(events.MESSAGE);
		events.request(events.LOGGED_ON);
		events.request(events.PLAYER_LEFT);
		d = new Deck();
		playerSeat = new ArrayList();
		isRunning = true;
		dealer = 0;
		curBet = 0;
		maxBet = 0;
		thisBet = 0;
		pot = 0;
		status = -1;
		turn = "";
	}*/

	/*public void handleEvent(LoggedOn event) {
		m_botAction.joinArena(m_botAction.getBotSettings().getString("Arena"));
	}*/

	public void handleEvent(PlayerLeft event) {
		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(playerSeat.indexOf(name.toLowerCase()) >= 0) {
			PokerPlayer pPlayer = (PokerPlayer)seats.get(name.toLowerCase());
			pPlayer.fold();
			m_botAction.sendArenaMessage(name + " folds because he left the arena.");
			if(isTurn(name)) { playerDidSomething(); nextPlayer(); }
			playerSeat.remove(name.toLowerCase());
			setChips(name, pPlayer.getChips());
			seats.remove(name.toLowerCase());
		}
	}

	public void handleEvent(Message event) {
		String name = event.getMessager();
		if(name == null) name = m_botAction.getPlayerName( event.getPlayerID() );
		String message = event.getMessage();
		if(m_botAction.getOperatorList().isModerator(name))
			handleModCommand(name, message);
		else
			handleCommand(name, message);
	}

	public void handleModCommand(String name, String message) {
		if(message.toLowerCase().startsWith("!die"))
			m_botAction.die();
		else if(message.toLowerCase().startsWith("!stop") && isRunning) {
			m_botAction.sendPrivateMessage(name, "Ok, stopping after this hand.");
			isRunning = false;
		} else if(message.toLowerCase().startsWith("!start") && !isRunning) {
			m_botAction.sendPrivateMessage(name, "Bot started.");
			isRunning = true;
		} else handleCommand(name, message);
	}

	public void handleCommand(String name, String message) {
		if(message.toLowerCase().startsWith("!hand")) {
			myHand(name);
		} else if(message.toLowerCase().startsWith("!play")) {
			addPlayer(name);
		} else if(message.toLowerCase().startsWith("!quit")) {
			playerQuit(name);
		} else if(message.toLowerCase().startsWith("!bet ")) {
			betChips(name, message, false);
		} else if(message.toLowerCase().startsWith("!call")) {
			betChips(name, message, true);
		} else if(message.toLowerCase().startsWith("!fold")) {
			foldPlayer(name);
		} else if(message.toLowerCase().startsWith("!check")) {
			checkPlayer(name);
		} else if(message.toLowerCase().startsWith("!pot")) {
			m_botAction.sendPrivateMessage(name, "Current pot: " + pot);
		} else if(message.toLowerCase().startsWith("!help")) {
			handleHelp(name);
		}
	}

	public void playerQuit(String name) {
		if(playerSeat.indexOf(name.toLowerCase()) >= 0) {
			PokerPlayer pPlayer = (PokerPlayer)seats.get(name.toLowerCase());
			pPlayer.fold();
			m_botAction.sendArenaMessage(name + " folds because he left the game.");
			if(isTurn(name)) { playerDidSomething(); nextPlayer(); }
			playerSeat.remove(name.toLowerCase());
			setChips(name, pPlayer.getChips());
			seats.remove(name.toLowerCase());
		}
	}

	public void handleHelp(String name) {
		if(m_botAction.getOperatorList().isModerator(name)) {
		m_botAction.sendPrivateMessage(name, "!die           -Kills bot.");
		m_botAction.sendPrivateMessage(name, "!stop          -Stops bot.");
		m_botAction.sendPrivateMessage(name, "!start         -Starts bot.");
		}
		m_botAction.sendPrivateMessage(name, "!play          -Adds you to the game.");
		m_botAction.sendPrivateMessage(name, "!quit          -Removes you from the game.");
		m_botAction.sendPrivateMessage(name, "!hand          -PM's you your hand.");
		m_botAction.sendPrivateMessage(name, "!bet <#>       -Bets <#> chips.");
		m_botAction.sendPrivateMessage(name, "!call          -Calls the current bet.");
		m_botAction.sendPrivateMessage(name, "!fold          -Folds your hand.");
		m_botAction.sendPrivateMessage(name, "!check         -Checks.");
		m_botAction.sendPrivateMessage(name, "!pot           -PM's you current pot.");
		m_botAction.sendPrivateMessage(name, "!help          -Sends you this message.");
	}


	public void checkPlayer(String name) {
		if(playerSeat.indexOf(name.toLowerCase()) < 0) {
			m_botAction.sendPrivateMessage(name, "You are not in this game.");
			return;
		}
		if(!isTurn(name)) {
			m_botAction.sendPrivateMessage(name, "It is not your turn.");
			return;
		}
		PokerPlayer pPlayer = (PokerPlayer)seats.get(name.toLowerCase());
		if(pPlayer.getBet() >= curBet) {
			playerDidSomething();
			nextPlayer();
			m_botAction.sendArenaMessage(name + " checks.");
		} else
			m_botAction.sendPrivateMessage(name, "You must bet, call, or fold.");
	}

	public void foldPlayer(String name) {
		if(playerSeat.indexOf(name.toLowerCase()) < 0) {
			m_botAction.sendPrivateMessage(name, "You are not in this game.");
			return;
		}
		if(!isTurn(name)) {
			m_botAction.sendPrivateMessage(name, "It is not your turn.");
			return;
		}
		PokerPlayer pPlayer = (PokerPlayer)seats.get(name.toLowerCase());
		pPlayer.fold();
		m_botAction.sendArenaMessage(name + " folds.");
		playerDidSomething();
		nextPlayer();
	}


	public void betChips(String name, String message, boolean call) {
		if(playerSeat.indexOf(name.toLowerCase()) < 0) {
			m_botAction.sendPrivateMessage(name, "You are not in this game.");
			return;
		}
		if(!isTurn(name)) {
			m_botAction.sendPrivateMessage(name, "It is not your turn.");
			return;
		}
		PokerPlayer pPlayer = (PokerPlayer)seats.get(name.toLowerCase());
		if(call) {
			int bet = curBet - pPlayer.getBet();
			if(pPlayer.bet(bet)) {
				pot += bet;
				m_botAction.sendArenaMessage(name + " calls.");
				playerDidSomething();
				nextPlayer();
			}
		} else {
			int bet = 0;
			try {
				bet = Integer.parseInt(message.substring(5));
			} catch(Exception e) { m_botAction.sendPrivateMessage(name, "Invalid bet."); return; }

			if((pPlayer.getBet() + bet) > maxBet) {
				m_botAction.sendPrivateMessage(name, "Sorry, you can only bet " + (maxBet - pPlayer.getBet()));
				return;
			}

			if(curBet == pPlayer.getBet())
				m_botAction.sendArenaMessage(name + " bets " + bet + ".");
			else
				m_botAction.sendArenaMessage(name + " raises " + bet + ".");
			curBet += bet;
			pot += bet;
			pPlayer.bet(bet);
			playerDidSomething();
			nextPlayer();
		}
	}

	public void myHand(String name) {
		if(playerSeat.indexOf(name.toLowerCase()) < 0) {
			m_botAction.sendPrivateMessage(name, "You are not in this game.");
			return;
		}
		PokerPlayer pPlayer = (PokerPlayer)seats.get(name.toLowerCase());
		String[] hand = pPlayer.getHand();
		String m_hand = "";
		for(int k = 0;k < hand.length;k++)
			m_hand += hand[k] + " ";
		m_botAction.sendPrivateMessage(name, m_hand);
	}

	public void addPlayer(String name) {
		if(!(playerSeat.size() < 10)) {
			m_botAction.sendPrivateMessage(name, "This game is full right now, please try again later.");
			return;
		}
		if(playerSeat.indexOf(name.toLowerCase()) >= 0) {
			m_botAction.sendPrivateMessage(name, "You are already in this game.");
			return;
		}
		if(!isRunning) {
			m_botAction.sendPrivateMessage(name, "The bot is currently disabled, please try again later.");
			return;
		}

		int chips = getChips(name);
		if(chips < 5) {
			m_botAction.sendPrivateMessage(name, "Sorry, there is a minimum buy-in of 5 chips.");
			return;
		}

		PokerPlayer pPlayer = new PokerPlayer(name, chips);
		seats.put(name.toLowerCase(), pPlayer);
		playerSeat.add(name.toLowerCase());
		m_botAction.sendPrivateMessage(name, "You have been added to the game, you will start next hand.");
		if(playerSeat.size() > 4) startBot();
	}

	public int getChips(String name) {
		String query = "SELECT * FROM tblPlayerPoints WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"'";
		try {
			ResultSet results = m_botAction.SQLQuery("local", query);
			if(results == null || !results.next())
				return -10;
			else
				return results.getInt("fnPoints");
		} catch(Exception e) {}
		return -10;
	}

	public void setChips(String name, int chips) {
		String query = "UPDATE tblPlayerPoints SET fnPoints = " + chips + " WHERE fcName = '"+Tools.addSlashesToString(name.toLowerCase())+"'";
		try {
			m_botAction.SQLQuery("local", query);
		} catch(Exception e) {}
	}

	public boolean isTurn(String name) {
		return turn.equals(name.toLowerCase());
	}

	public void nextPlayer() {
		int index = playerSeat.indexOf(turn) + 1;
		if(index >= playerSeat.size()) index = 0;
		turn = (String)playerSeat.get(index);
		PokerPlayer pPlayer = (PokerPlayer)seats.get(turn);
		/*if(!pPlayer.isPlaying()) { nextPlayer(); return; }
		if(nextRound()) {
			for(int k = 0;k < playerSeat.size();k++) {
				String name = (String)playerSeat.get(k);
				myHand(name);
			}
		}*/
		m_botAction.sendPrivateMessage(turn, "It's your turn.");
		schedule(turn.toLowerCase());

	}

	public void startBot() {
		status = -1;
		startNextRound();
	}

	public void schedule(String name) {
		final String player = name;
		warning = new TimerTask() {
			public void run() {
				m_botAction.sendPrivateMessage(player, "You have 15 seconds to play before getting removed from the game.");
			}
		};
		nextPlayer = new TimerTask() {
			public void run() {
				nextPlayer();
				removePlayer(player);
			}
		};
		m_botAction.scheduleTask(warning, 10 * 1000);
		m_botAction.scheduleTask(nextPlayer, 25 * 1000);
	}

	public void removePlayer(String name) {
		PokerPlayer pPlayer = (PokerPlayer)seats.get(name.toLowerCase());
		playerSeat.remove(playerSeat.indexOf(name.toLowerCase()));
		seats.remove(name.toLowerCase());
		m_botAction.sendArenaMessage(name + " thrown out of the game for failure to play in a timely manner.");
		setChips(name, pPlayer.getChips());
	}

	public void playerDidSomething() {
		m_botAction.cancelTasks();
	}

	public boolean nextRound() {
		boolean allChecked = true;
		boolean betsSame = true;
		for(int k = 0;k < playerSeat.size();k++) {
			String name = (String)playerSeat.get(k);
			PokerPlayer pPlayer = (PokerPlayer)seats.get(name);
			allChecked = allChecked && pPlayer.hasChecked();
			betsSame = betsSame && (pPlayer.getBet() == curBet);
		}
		if(thisBet == 0 && allChecked) {
			startNextRound();
			return true;
		} else if(betsSame && thisBet != 0) {
			startNextRound();
			return true;
		} else {
			return false;
		}
	}

	public void startNextRound() {
		status++;
		switch(status) {
			case 0:
				initial();
				break;
			case 1:
				flop();
				break;
			case 2:
				turnOrRiver();
				break;
			case 3:
				turnOrRiver();
				break;
			case 4:
				end();
			default:
				break;
		}
	}

	public void initial() {
		d = new PokerDeck();
		for(int k = 0;k < playerSeat.size();k++) {
			String name = (String)playerSeat.get(k);
			((PokerPlayer)seats.get(name)).reset();
		}
		String[][] hands = new String[playerSeat.size()][2];
		String[] cards = d.drawCards((playerSeat.size() * 2));
		for(int k = 0, count = 0;k < 2;k++) {
			for(int i = 0;i < playerSeat.size();i++) {
				hands[i][k] = cards[count];
				count++;
			}
		}
		for(int k = 0;k < playerSeat.size();k++) {
			String name = (String)playerSeat.get(k);
			PokerPlayer pPlayer = (PokerPlayer)seats.get(name);
			pPlayer.setHand(hands[k]);
		}
	}

	public void flop() {
		d.drawCard();
		String[] cards = d.drawCards(3);
		for(int k = 0;k < playerSeat.size();k++) {
			String name = (String)playerSeat.get(k);
			PokerPlayer pPlayer = (PokerPlayer)seats.get(name);
			pPlayer.addCardToHand(cards[0]);
			pPlayer.addCardToHand(cards[1]);
			pPlayer.addCardToHand(cards[2]);
		}
	}

	public void turnOrRiver() {
		d.drawCard();
		String card = d.drawCard();
		for(int k = 0;k < playerSeat.size();k++) {
			String name = (String)playerSeat.get(k);
			PokerPlayer pPlayer = (PokerPlayer)seats.get(name);
			pPlayer.addCardToHand(card);
		}
	}

	public void end() {
		HashMap scores = new HashMap();
		for(int k = 0;k < playerSeat.size();k++) {
			String name = (String)playerSeat.get(k);
			PokerPlayer pPlayer = (PokerPlayer)seats.get(name);
			int pScore = 0;//getScore(pPlayer.getHand());
			HashSet players;
			if(scores.containsKey(pScore)) {
				players = (HashSet)scores.get(pScore);
				players.add(name);
			} else {
				players = new HashSet();
				players.add(name);
			}
			scores.put(pScore, players);
		}
		HashSet winners = new HashSet();
		int highScore = 0;
		Iterator it = scores.keySet().iterator();
		while(it.hasNext()) {
			int score = (Integer)it.next();
			if(score > highScore) {
				highScore = score;
				winners = (HashSet)scores.get(score);
			}
		}

		int eachPlayer = pot / winners.size();
		it = winners.iterator();
		while(it.hasNext()) {
			String name = (String)it.next();
			//PokerPlayer pPlayer = (P
	}

}

}