package twcore.bots.multibot.util;

import twcore.bots.MultiUtil;
import twcore.core.events.Message;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Spy;

/**
 * Created for MultiBot's to detect racism using the Spy class (twcore.core.util).
 * @author Pio
 * @version 1.0
 * Last updated: 1/14/07
 */
public class utilracism extends MultiUtil{
	private Spy watcher;
	@Override
	public String[] getHelpMessages() {
		String[] message =
	    {
	      "Racism utility will report a person who uses racist language via ?cheater. The words are in data/racism.cfg.",
	      "This utility is automaticly active once you load it. Once unloaded it will be deactivated."
	    };

	    return message;
	}

	@Override
	public void init() {
		watcher = new Spy(m_botAction);
	}

	@Override
	public void requestEvents(ModuleEventRequester modEventReq) {
		// required :o
	}
	
	public void handleEvent(Message event)
	{
		int type = event.getMessageType();
		if(type == Message.PUBLIC_MESSAGE        ||
		   type == Message.TEAM_MESSAGE          ||
		   type == Message.OPPOSING_TEAM_MESSAGE ||
		   type == Message.PUBLIC_MACRO_MESSAGE)
			watcher.handleEvent(event);
	}
	
	public void cancel() {
	}

}
