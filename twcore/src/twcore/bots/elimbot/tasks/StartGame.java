package twcore.bots.elimbot.tasks;

import java.util.TimerTask;

import twcore.bots.elimbot.ElimState;
import twcore.bots.elimbot.elimbot;

public class StartGame extends TimerTask {
	
	private elimbot elimbot;

	public StartGame(elimbot elimbot) {
		this.elimbot = elimbot;
	}
	
	public void run() {
		elimbot.state = ElimState.RUNNING;
		elimbot.m_botAction.scoreResetAll();
		elimbot.m_botAction.shipResetAll();
		elimbot.m_botAction.sendArenaMessage("Go Go Go!",104);
		elimbot.m_botAction.scoreResetAll();
		elimbot.startTime = System.currentTimeMillis();
	}
}