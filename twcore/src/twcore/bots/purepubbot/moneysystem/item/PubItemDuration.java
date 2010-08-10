package twcore.bots.purepubbot.moneysystem.item;


public class PubItemDuration {

	private int deaths = -1;
	private int seconds = -1;
	

	public void setDeaths(int number) {
		this.deaths = number;
	}
	
	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}
	
	public void setMinutes(int minutes) {
		this.seconds = minutes*60;
	}
	
	public boolean hasDeaths() {
		return deaths!=-1;
	}
	
	public boolean hasTime() {
		return seconds!=-1;
	}
	
	public int getDeaths() {
		return deaths;
	}
	
	public int getSeconds() {
		return seconds;
	}
	
}
