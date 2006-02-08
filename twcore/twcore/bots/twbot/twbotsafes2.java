package twcore.bots.twbot;

import twcore.core.BotAction;
import twcore.core.CommandInterpreter;
import twcore.core.Message;
import twcore.core.PlayerPosition;
import twcore.misc.tempset.TempSettingsManager;
import twcore.misc.tempset.TSChangeListener;
import static twcore.core.OperatorList.ER_LEVEL;
import static twcore.misc.tempset.SType.*;

/**
 * Extended Safes Module (Based on twbotsafes by 2dragons)
 * This was a modification requested by crazi, with the added ability to change
 * a player's ship and/or frequency when they fly over a safety tile as well
 * as put them into spectator mode.
 *
 * @author D1st0rt
 * @version 06.02.08
 */
public class twbotsafes2 extends TWBotExtension implements TSChangeListener
{
	/** The TempSettingsManager used to keep track of the settings */
	private TempSettingsManager m_tsm;

	/** The status of the module's reactions to players flying over safes */
	private boolean	m_active;

	/** The help message to be sent to bot operators */
	private final String helpMessage[] =
	{
		"+------------------Extended Safes Module-------------------+",
		"|  Release 1.5 [02/08/06] - http://d1st0rt.sscentral.com   |",
		"+----------------------------------------------------------+",
		"! !activate - Toggles the module doing anything when a     |",
		"|             player flies over a safety tile              |",
		"|                                                          |",
		"| !set      - Modify actions to take for safety'd players  |",
		"|   |                                                      |",
		"|   +- SpecPlayer= on/off, Whether to spec player          |",
		"|   +- SpeccedMsg= \"text\", Arena message when player spec'd|",
		"|   +- ChangeShip= on/off, Whether to change player's ship |",
		"|   +- TargetShip= 1 - 8 , ship to change player to        |",
		"|   +- ShipChgMsg= \"text\", Arena message when ship changed |",
		"|   +- ChangeFreq= on/off, Whether to change player's freq |",
		"|   +- TargetFreq= 0-9999, freq to change player to        |",
		"|   +- FreqChgMsg= \"text\", Arena message when freq changed |",
		"|      (Set the msg's to \"none\" for no message displayed)  |",
		"|                                                          |",
		"| !get      - Get the value for one of the above settings  |",
		"+----------------------------------------------------------+"
	};


	// Cached game settings to increase speed
	private boolean specPlayer = false, changeShip = false, changeFreq = false;
	private int targetFreq = 1, targetShip = 3;
	private String speccedMsg = "none", shipChgMsg = "none", freqChgMsg= "none";

	/**
	 * Creates a new instance of the Extended Safes Module
	 */
	public twbotsafes2()
	{
		m_active = false;
		m_tsm = new TempSettingsManager(BotAction.getBotAction(), ER_LEVEL);
		registerSettings();
		m_tsm.addTSChangeListener(this);
	}

	/**
	 * Gets the help message for this module
	 * @return A string array containing the help information
	 */
	public String[] getHelpMessages()
	{
		return helpMessage;
	}

	/**
	 * Fired when an op changes a setting with !set. Updates the cached value.
	 * @param setting the name of the setting that was changed
	 * @param value the text string value the bot op entered
	 */
	public void settingChanged(String setting, Object value)
	{
		if(setting.equals("specplayer"))
			specPlayer = (Boolean)value;
		else if(setting.equals("speccedmsg"))
			speccedMsg = (String)value;
		else if(setting.equals("changeship"))
			changeShip = (Boolean)value;
		else if(setting.equals("targetship"))
			targetShip = (Integer)value;
		else if(setting.equals("shipchgmsg"))
			shipChgMsg = (String)value;
		else if(setting.equals("changefreq"))
			changeFreq = (Boolean)value;
		else if(setting.equals("targetfreq"))
			targetFreq = (Integer)value;
		else if(setting.equals("freqchgmsg"))
			freqChgMsg = (String)value;
	}

	/**
	 * Event: Message
	 * Check and pass to appropriate commands
	 */
	public void handleEvent(Message event)
	{
		m_tsm.handleEvent(event);

		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(name))
			if(event.getMessage().equalsIgnoreCase("!activate"))
				c_Activate(name);
	}

	/**
	 * Event: PlayerPosition
	 * Check to see if player is in a safety zone and respond accordingly
	 */
	public void handleEvent(PlayerPosition event)
	{
		if(m_active && event.isInSafe())
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			int ship = m_botAction.getPlayer(event.getPlayerID()).getShipType();
			int freq = m_botAction.getPlayer(event.getPlayerID()).getFrequency();

			if(specPlayer)
			{
				m_botAction.spec(event.getPlayerID());
				m_botAction.spec(event.getPlayerID());
				if(!speccedMsg.equalsIgnoreCase("none"))
					m_botAction.sendArenaMessage(name + " " + speccedMsg);
			}

			if(changeShip && ship != targetShip)
			{
				m_botAction.setShip(event.getPlayerID(), targetShip);
				if(!shipChgMsg.equalsIgnoreCase("none"))
					m_botAction.sendArenaMessage(name + " " + shipChgMsg);
			}

			if(changeFreq && freq != targetFreq)
			{
				m_botAction.setFreq(event.getPlayerID(), targetFreq);
				if(!freqChgMsg.equalsIgnoreCase("none"))
					m_botAction.sendArenaMessage(name + " " + freqChgMsg);
			}
		}
	}

	/**
	 * Command: !activate
	 * Toggles the module doing anything when a player flies over a safety tile
	 */
	private void c_Activate(String name)
	{
		if(m_active = !m_active)
			m_botAction.sendSmartPrivateMessage(name, "Reacting to players who fly over safety tiles.");
		else
			m_botAction.sendSmartPrivateMessage(name, "NOT Reacting to players who fly over safety tiles.");
	}

	/**
	 * Establishes all of the settings with the TempSettings Manager
	 */
	private void registerSettings()
	{
		m_tsm.addSetting(BOOLEAN, "SpecPlayer", "off");
		m_tsm.addSetting(STRING,  "SpeccedMsg", "none");

		m_tsm.addSetting(BOOLEAN, "ChangeShip", "off");
		m_tsm.addSetting(INT,     "TargetShip", "3");
		m_tsm.addSetting(STRING,  "ShipChgMsg", "none");

		m_tsm.addSetting(BOOLEAN, "ChangeFreq", "off");
		m_tsm.addSetting(INT,     "TargetFreq", "1");
		m_tsm.addSetting(STRING,  "FreqChgMsg", "none");

		m_tsm.restrictSetting("TargetShip", 1, 8);
		m_tsm.restrictSetting("TargetFreq", 0, 9999);
	}

	public void cancel()
	{
		m_tsm.removeTSChangeListener(this);
	}
}
