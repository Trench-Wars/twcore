package twcore.bots.elimbot.tasks;

import java.util.TimerTask;

import twcore.bots.elimbot.ElimState;
import twcore.bots.elimbot.elimbot;

public class NewGame extends TimerTask {
	
	private elimbot elimbot;

	public NewGame(elimbot elimbot) {
		this.elimbot = elimbot;
	}
	
	public void run() {
		elimbot.state = ElimState.IDLE;
		elimbot.step();
	}
}