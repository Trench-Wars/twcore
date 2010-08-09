package twcore.bots.purepubbot.moneysystem.item;

public class PubPrizeItem extends PubItem {

	private int prizeNumber;

	public PubPrizeItem(String name, String displayName, int price, int prizeNumber) {
		super(name, displayName, price);
		this.prizeNumber = prizeNumber;
	}
	
	public int getPrizeNumber() {
		return prizeNumber;
	}

}
