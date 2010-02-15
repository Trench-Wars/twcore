package twcore.bots.multibot.twar;
/*
 * Author: Dexter
 * match .java to twar
*/
public class shipSettings implements Comparable<shipSettings>{ //Uses comparable to sort the list by the number of kills to promotions
	
	private int ship;
	private int kill;
	private String shipName = "";
	public int getShip() {
		return ship;
	}
	
	public String toString()
	{
		return "be ship #"+ship+" with "+kill+" kills";
	}
	
	public int compareTo(shipSettings ship)
	{ 
	
		if(getKill() > ship.getKill()) return 1;
		if(getKill() < ship.getKill()) return -1;
		if(getKill() == ship.getKill()) return 0;
		return 0;
	}
	
	public void setShip(int ship) {
		
		if(ship == 1)
			this.shipName = "warbird";
		else if(ship == 2)
			this.shipName = "javelin";
		else if(ship == 3)
			this.shipName = "spider";
		else if(ship == 7)
			this.shipName = "lancaster";
		else if(ship == 4)
			this.shipName = "levi";
		else if(ship == 5)
			this.shipName = "terrier";
		else if(ship == 6)
			this.shipName = "weasel";
		else if(ship == 8)
			this.shipName = "shark";
		
		this.ship = ship;
		
	}
	public String getShipName(){
		return this.shipName;
	}
	public int getKill() {
		return kill;
	}

	public void setKill(Integer kill) {
		this.kill = kill;
	}
}
