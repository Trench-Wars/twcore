package twcore.bots.twbot;

import twcore.core.BotAction;
import twcore.core.CommandInterpreter;
import twcore.core.Message;
import twcore.core.PlayerPosition;
import twcore.misc.tempset.TempSettingsManager;
import static twcore.core.OperatorList.ER_LEVEL;
import static twcore.misc.tempset.SType.*;

/**
 * Extended Safes Module (Based on twbotsafes by 2dragons)
 * This was a modification requested by crazi, with the added ability to change
 * a player's ship and/or frequency when they fly over a safety tile as well
 * as put them into spectator mode.
 *
 * @author D1st0rt
 * @version 06.01.21
 */
public class twbotsafes2 extends TWBotExtension
{
	/** The TempSettingsManager used to keep track of the settings */
	private TempSettingsManager m_tsm;

	/** The status of the module's reactions to players flying over safes */
	private boolean	m_active;

	/** The help message to be sent to bot operators */
	private final String helpMessage[] =
	{
		"+------------------Extended Safes Module------------------+",
		"|  Release 1.2 [01/21/06] - http://d1st0rt.sscentral.com  |",
		"+---------------------------------------------------------+",
		"! !activate - Toggles the module doing anything when a    |",
		"|             player flies over a safety tile             |",
		"|                                                         |",
		"| !set      - Modify actions to take for safety'd players |",
		"|   |                                                     |",
		"|   +-SpecPlayer= on/off, Whether to spec player          |",
		"|   +-SpeccedMsg= on/off, Arena message when player spec'd|",
		"|   +-ChangeShip= on/off, Whether to change player's ship |",
		"|   +-TargetShip= 1 - 8 , ship to change player to        |",
		"|   +-ShipChgMsg= on/off, Arena message when ship changed |",
		"|   +-ChangeFreq= on/off, Whether to change player's freq |",
		"|   +-TargetFreq= 0-9999, freq to change player to        |",
		"|   +-FreqChgMsg= on/off, Arena message when freq changed |",
		"|                                                         |",
		"| !get      - Get the value for one of the above settings |",
		"+---------------------------------------------------------+"
	};

	/**
	 * Creates a new instance of the Extended Safes Module
	 */
	public twbotsafes2()
	{
		m_active = false;
		m_tsm = new TempSettingsManager(BotAction.getBotAction(), ER_LEVEL);
		registerSettings();
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
			int tship = (Integer)m_tsm.getSetting("TargetShip");
			int tfreq = (Integer)m_tsm.getSetting("TargetFreq");

			if((Boolean)m_tsm.getSetting("SpecPlayer"))
			{
				m_botAction.spec(event.getPlayerID());
				m_botAction.spec(event.getPlayerID());
				if((Boolean)m_tsm.getSetting("SpeccedMsg"))
					m_botAction.sendArenaMessage(name + " has been specced for going into a safe area.");
			}

			if((Boolean)m_tsm.getSetting("ChangeShip") && ship != tship)
			{
				m_botAction.setShip(event.getPlayerID(), tship);
				if((Boolean)m_tsm.getSetting("ShipChgMsg"))
					m_botAction.sendArenaMessage(name + " is now in ship "+ tship +" for going into a safe area.");
			}

			if((Boolean)m_tsm.getSetting("ChangeFreq") && freq != tfreq)
			{
				m_botAction.setFreq(event.getPlayerID(), tfreq);
				if((Boolean)m_tsm.getSetting("FreqChgMsg"))
					m_botAction.sendArenaMessage(name + " is now on freq "+ tfreq +" for going into a safe area.");
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
		m_tsm.addSetting(BOOLEAN, "SpeccedMsg", "off");

		m_tsm.addSetting(BOOLEAN, "ChangeShip", "off");
		m_tsm.addSetting(INT,     "TargetShip", "3");
		m_tsm.addSetting(BOOLEAN, "ShipChgMsg", "off");

		m_tsm.addSetting(BOOLEAN, "ChangeFreq", "off");
		m_tsm.addSetting(INT,     "TargetFreq", "1");
		m_tsm.addSetting(BOOLEAN, "FreqChgMsg", "off");

		m_tsm.restrictSetting("TargetShip", 1, 8);
		m_tsm.restrictSetting("TargetFreq", 0, 9999);
	}

	public void cancel(){ }
}
