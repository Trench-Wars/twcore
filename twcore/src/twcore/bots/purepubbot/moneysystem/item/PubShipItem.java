package twcore.bots.purepubbot.moneysystem.item;

public class PubShipItem extends PubItem {

	private int shipNumber;

	public PubShipItem(String name, String displayName, int price, int shipNumber) {
		super(name, displayName, price);
		this.shipNumber = shipNumber;
	}
	
	public int getShipNumber() {
		return shipNumber;
	}

}
