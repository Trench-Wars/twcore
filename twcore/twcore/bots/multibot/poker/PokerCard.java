package twcore.bots.multibot.poker;


class PokerCard
{
	String type;
	String value;

	public PokerCard(String t, String v) {
		type = t;
		value = v;
	}

	public String toString() {
		String tv;
		if(value.equals("Joker")) tv = "Joker";
		else if(value.length() > 1) tv = value.charAt(0) + "" + type.charAt(0);
		else tv = value + type.charAt(0);
		return tv;
	}
}

