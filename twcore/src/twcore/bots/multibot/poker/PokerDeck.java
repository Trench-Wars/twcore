package twcore.bots.multibot.poker;

import java.util.ArrayList;
import java.util.Random;

public class PokerDeck {
	public static void main(String args[]) { PokerDeck d = new PokerDeck(2, true); }
	Queue<PokerCard> cards;
	Random rand;
	int decks;

	public PokerDeck() {
		decks = 1;
		cards = new Queue<PokerCard>();
		rand = new Random();
		addDeck(false);
		for(int k = 0;k < 8;k++)
			shuffleDeck();
		testing();
	}

	public PokerDeck(int decks) {
		this.decks = decks;
		cards = new Queue<PokerCard>();
		rand = new Random();
		for(int k = 0;k < decks;k++)
			addDeck(false);
		for(int k = 0;k < 8;k++)
			shuffleDeck();
		testing();
	}

	public PokerDeck(int decks, boolean jokers) {
		this.decks = decks;
		cards = new Queue<PokerCard>();
		rand = new Random();
		for(int k = 0;k < decks;k++)
			addDeck(true);
		for(int k = 0;k < 8;k++)
			shuffleDeck();
		testing();
	}

	public void addDeck(boolean jokers) {
		ArrayList<String> suits = new ArrayList<String>();
		suits.add("Diamond");
		suits.add("Heart");
		suits.add("Club");
		suits.add("Spade");
		ArrayList<String> values = new ArrayList<String>();
		values.add("Ace");
		values.add("2");
		values.add("3");
		values.add("4");
		values.add("5");
		values.add("6");
		values.add("7");
		values.add("8");
		values.add("9");
		values.add("10");
		values.add("Jack");
		values.add("Queen");
		values.add("King");

		for(int k = 0;k < suits.size();k++) {
			String suit = suits.get(k);
			for(int i = 0;i < values.size();i++) {
				String value = values.get(i);
				PokerCard c = new PokerCard(suit, value);
				cards.add(c);
			}
		}

		if(jokers) {
			cards.add(new PokerCard("", "Joker"));
			cards.add(new PokerCard("", "Joker"));
		}
	}

	public void shuffleDeck() {
		Queue<PokerCard> split1 = new Queue<PokerCard>();
		Queue<PokerCard> split2 = new Queue<PokerCard>();
		int numCards = cards.size();
		int cutAt = cards.size() / 2 + rand.nextInt(10 * decks) - 5 * decks;
		for(int k = 0;k < cutAt;k++)
			split1.add(cards.next());
		while(cards.size() > 0)
			split2.add(cards.next());
		boolean s1 = true;
		while(cards.size() != numCards) {
			int thisTime = rand.nextInt(3) + 1;
			for(int k = 0;k < thisTime && cards.size() != numCards;k++) {
				if(s1 && !split1.isEmpty())  cards.add(split1.next());
				if(!s1 && !split2.isEmpty()) cards.add(split2.next());
			}
			s1 = !s1;
		}
	}

	public String drawCard() {
		if(cards.isEmpty()) return "";
		PokerCard c = cards.next();
		return c.toString();
	}

	public String[] drawCards(int num) {
		String draws[] = new String[num];
		for(int k = 0;k < num && !cards.isEmpty();k++) {
			draws[k] = cards.next().toString();
		}
		return draws;
	}

	public void testing() {
		while(!cards.isEmpty())
			System.out.println(cards.next());
	}
}

class Queue<E>
{
	ArrayList<E> objects;

	public Queue()
	{
		objects = new ArrayList<E>();
	}

	public E next()
	{
		E obj = objects.get(0);
		objects.remove(0);
		return obj;
	}

	public void add(E obj)
	{
		objects.add(obj);
	}

	public int size()
	{
		return objects.size();
	}

	public boolean isEmpty()
	{
		return objects.size() == 0;
	}
}