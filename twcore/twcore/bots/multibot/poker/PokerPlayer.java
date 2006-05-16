package twcore.bots.multibot.poker;


class PokerPlayer {
	String name;
	int chips;
	int bet;
	String[] hand;
	boolean folded;
	boolean check;
	boolean isPlaying;

	public PokerPlayer(String name, int chips) {
		this.name = name;
		this.chips = chips;
		this.bet = 0;
		folded = false;
		check = false;
		isPlaying = false;
	}

	public int getChips() {
		return chips;
	}

	public void changeChips(int delta) {
		chips += delta;
	}

	public int getBet() {
		return bet;
	}

	public void reset() {
		bet = 0;
		folded = false;
		isPlaying = true;
		check = false;
	}

	public boolean bet(int bet) {
		chips -= bet;
		if(chips < 0) {
			chips += bet;
			return false;
		} else {
			this.bet += bet;
			return true;
		}
	}

	public void setHand(String[] cards) {
		hand = cards;
	}

	public void addCardToHand(String card) {
		String[] newHand = new String[hand.length + 1];
		for(int k = 0;k < hand.length;k++)
			newHand[k] = hand[k];

		newHand[hand.length] = card;
		hand = newHand;
	}

	public String[] getHand() {
		return hand;
	}

	public void fold() {
		folded = true;
	}

	public boolean hasFolded() {
		return folded;
	}

	public void check() {
		check = true;
	}

	public void uncheck() {
		check = false;
	}

	public boolean hasChecked() {
		return check;
	}
}
