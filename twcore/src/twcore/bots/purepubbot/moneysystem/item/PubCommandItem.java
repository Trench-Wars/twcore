package twcore.bots.purepubbot.moneysystem.item;

public class PubCommandItem extends PubItem {

	private String command;

	public PubCommandItem(String name, String displayName, int price, String command) {
		super(name, displayName, price);
		this.command = command;
	}
	
	public String getCommand() {
		return command;
	}

}
