package twcore.bots.multibot.util;

import static twcore.core.OperatorList.ER_LEVEL;
import static twcore.core.command.SType.BOOLEAN;
import static twcore.core.command.SType.INT;
import static twcore.core.command.SType.STRING;
import twcore.bots.MultiUtil;
import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.command.TSChangeListener;
import twcore.core.command.TempSettingsManager;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.util.ModuleEventRequester;
import twcore.core.util.Tools;

import java.util.HashMap;

/**
 * Extended Safes Module (Based on twbotsafes by 2dragons)
 * This was a modification requested by crazi, with the added ability to change
 * a player's ship and/or frequency when they fly over a safety tile as well
 * as put them into spectator mode.
 *
 * @author D1st0rt
 * @version 08.07.27
 */
public class utilsafes extends MultiUtil implements TSChangeListener
{
	/** The TempSettingsManager used to keep track of the settings */
	private TempSettingsManager m_tsm;

	/** The status of the module's reactions to players flying over safes */
	private boolean	m_active;

	/** Keeps track of when a player entered safe for delays */
	private HashMap<String, Long> m_entryTimes;

	/** The help message to be sent to bot operators */
	private final String helpMessage[] =
	{
		"+------------------Extended Safes Module---------------------+",
		"|  Release 1.8 [08/07/27] - http://d1st0rt.sscentral.com     |",
		"+------------------------------------------------------------+",
		"! !safeson  - Turn on safe watching                          |",
		"| !safesoff - Turn off safe watching                         |",
		"|                                                            |",
		"| !set SpecPlayer=on/off, Whether to spec player             |",
		"| !set SpeccedMsg=\"text\", Arena message when player spec'd   |",
		"| !set ChangeShip=on/off, Whether to change player's ship    |",
		"| !set TargetShip=1 - 8 , ship to change player to           |",
		"| !set ShipChgMsg=\"text\", Arena message when ship changed    |",
		"| !set ChangeFreq=on/off, Whether to change player's freq    |",
		"| !set TargetFreq=0-9999, freq to change player to           |",
		"| !set FreqChgMsg=\"text\", Arena message when freq changed    |",
		"|      (Set the msg's to \"none\" for no message displayed)    |",
		"| !set DelaySeconds=0-1000, How long to wait before acting   |",
		"| !set EnableMS=on/off, Use MS instead of seconds for delay  |",
		"|                                                            |",
		"| !get <Value>- Get the value for one of the above settings  |",
		"|                                                            |",
		"| !default <Arena>- Applies default arena settings.          |",
		"|       Blank resets to original values.                     |",
		"|       Currently supported arenas: brickman, pipe           |",
		"+------------------------------------------------------------+"
	};


	// Cached game settings to increase speed
	private boolean specPlayer = false, changeShip = false, changeFreq = false;
	private int targetFreq = 1, targetShip = Tools.Ship.SPIDER, delaySeconds = 0;
	private String speccedMsg = "none", shipChgMsg = "none", freqChgMsg= "none";
	private int speccedSound = 0, shipSound = 0, freqSound = 0;
	private boolean enableMS = false;
	
	/**
	 * Creates a new instance of the Extended Safes Module
	 */
	public void init()
	{
		m_active = false;
		m_entryTimes = new HashMap<String, Long>();
		m_tsm = BotAction.getBotAction().getTSM();
		m_tsm.setOperatorLevel(ER_LEVEL);
		registerSettings();
		m_tsm.addTSChangeListener(this);
        m_botAction.setPlayerPositionUpdating(500);
	}

    /**
     * Requests needed events.
     */
    public void requestEvents( ModuleEventRequester modEventReq ) {
        modEventReq.request(this, EventRequester.PLAYER_POSITION );
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
		setting = setting.toLowerCase();
		if(setting.equals("specplayer"))
			specPlayer = (Boolean)value;
		else if(setting.equals("speccedmsg")) {
		    Pair msg = parseText((String)value);
			speccedMsg = msg.getKey();
			speccedSound = msg.getValue();
		} else if(setting.equals("changeship"))
			changeShip = (Boolean)value;
		else if(setting.equals("targetship"))
			targetShip = (Integer)value;
		else if(setting.equals("shipchgmsg")) {
		    Pair msg = parseText((String)value);
			shipChgMsg = msg.getKey();
			shipSound = msg.getValue();
		} else if(setting.equals("changefreq"))
			changeFreq = (Boolean)value;
		else if(setting.equals("targetfreq"))
			targetFreq = (Integer)value;
		else if(setting.equals("freqchgmsg")) {
		    Pair msg = parseText((String)value);
			freqChgMsg = msg.getKey();
			freqSound = msg.getValue();
		} else if(setting.equals("delayseconds"))
			delaySeconds = (Integer)value;
		else if(setting.equals("enablems"))
		    enableMS = (Boolean)value;
	}

	public Pair parseText(String msg) {
        int soundPos = msg.indexOf('%');
        int soundCode = 0;

        if( soundPos != -1){
            try{
                soundCode = Integer.parseInt(msg.substring(soundPos + 1));
            } catch( Exception e ){
                soundCode = 0;
            }
            if(soundCode == 12) {soundCode = 1;} //no naughty sounds
        }
        return new Pair(msg, soundCode);
	}
	/**
	 * Event: Message
	 * Check and pass to appropriate commands
	 */
	public void handleEvent(Message event)
	{
		m_tsm.handleEvent(event);
		
		String msg = event.getMessage().toLowerCase();

		String name = m_botAction.getPlayerName(event.getPlayerID());
		if(event.getMessageType() == Message.PRIVATE_MESSAGE && m_opList.isER(name))
		{
			if(msg.equals("!safeson"))
			{
				c_Activate(name, true);
			}
			else if(msg.equals("!safesoff"))
			{
				c_Activate(name, false);
				m_entryTimes.clear();
			} else if(msg.startsWith("!default")) {
			    cmd_default(name, msg);
			}
		}
	}

	/**
	 * Event: PlayerPosition
	 * Check to see if player is in a safety zone and respond accordingly
	 */
	public void handleEvent(PlayerPosition event)
	{
		if(m_active)
		{
			String name = m_botAction.getPlayerName(event.getPlayerID());
			if(event.isInSafe())
			{
				int ship = m_botAction.getPlayer(event.getPlayerID()).getShipType();
				int freq = m_botAction.getPlayer(event.getPlayerID()).getFrequency();
				boolean delayExceeded = true;

				if(delaySeconds > 0)
				{
					long currentTime = System.currentTimeMillis();
					if(m_entryTimes.containsKey(name))
					{
						long entryTime = m_entryTimes.get(name);
						int delta = (int)(currentTime - entryTime);
						if(!enableMS) {
						    // Do the delay in seconds.
						    delta /= Tools.TimeInMillis.SECOND;
						}
						if(delta < delaySeconds)
						{
							delayExceeded = false;
						}
						else
						{
							m_entryTimes.remove(name);
						}
					}
					else
					{
						m_entryTimes.put(name, currentTime);
						delayExceeded = false;
					}
				}

				if(delayExceeded)
				{
					if(specPlayer)
					{
						m_botAction.spec(event.getPlayerID());
						m_botAction.spec(event.getPlayerID());
						if(!speccedMsg.equalsIgnoreCase("none")) {
                            if(speccedSound != 0)
                                m_botAction.sendArenaMessage(name + " " + speccedMsg, speccedSound);
                            else
                                m_botAction.sendArenaMessage(name + " " + speccedMsg);
						}
					}

					if(changeShip && ship != targetShip)
					{
					    m_botAction.sendArenaMessage("Current ship: " + ship + " ; New ship: " + targetShip);
						m_botAction.setShip(event.getPlayerID(), targetShip);
						
						if(!shipChgMsg.equalsIgnoreCase("none")) {
						    if(shipSound != 0)
						        m_botAction.sendArenaMessage(name + " " + shipChgMsg, shipSound);
						    else
						        m_botAction.sendArenaMessage(name + " " + shipChgMsg);
						}
							
					}

					if(changeFreq && freq != targetFreq)
					{
					    m_botAction.sendArenaMessage("Current freq: " + freq + " ; New freq: " + targetFreq);
						m_botAction.setFreq(event.getPlayerID(), targetFreq);
						if(!freqChgMsg.equalsIgnoreCase("none")) {
                            if(freqSound != 0)
                                m_botAction.sendArenaMessage(name + " " + freqChgMsg, freqSound);
                            else
                                m_botAction.sendArenaMessage(name + " " + freqChgMsg);
                        }
					}
				}
			}
			else if(delaySeconds > 0)
			{
				m_entryTimes.remove(name);
			}
		}
	}

	/**
	 * Command: !activate
	 * Toggles the module doing anything when a player flies over a safety tile
	 */
	private void c_Activate(String name, boolean activate) {
		if(m_active = activate) {
			m_botAction.sendSmartPrivateMessage(name, "Reacting to players who fly over safety tiles.");
		} else {
			m_botAction.sendSmartPrivateMessage(name, "NOT Reacting to players who fly over safety tiles.");
		}
	}

	/**
	 * Command: !default
	 * <p>
	 * Changes the settings to a predetermined set for a specific arena.
	 * @param name Sender
	 * @param msg Message
	 */
	private void cmd_default(String name, String msg) {
	    String arenaName = "";
	    
	    if(msg.length() > 9) {
	        arenaName = msg.substring(9);
	    }
	    
	    if(!arenaName.isEmpty()) {
	        if(arenaName.equals("pipe")) {
	            // Arena: Pipe
	            specPlayer = false;
	            changeShip = true;
	            changeFreq = true;
	            targetFreq = 7;
	            targetShip = Tools.Ship.SHARK;
	            delaySeconds = 100;
	            enableMS = true;
	            speccedMsg = "none";
	            speccedSound = 0;
	            shipChgMsg = "slipped and fell into a safe!";
	            shipSound = Tools.Sound.CROWD_AWW;
	            freqChgMsg = "none";
	            freqSound = 0;
	            m_botAction.sendSmartPrivateMessage(name, "Arenasettings applied for pipe.");
	            return;
	        } else if(arenaName.equals("brickman")) {
	            // Arena: Brickman
                specPlayer = true;
                changeShip = false;
                changeFreq = false;
                targetFreq = 2;
                targetShip = Tools.Ship.SPIDER;
                delaySeconds = 100;
                enableMS = true;
                speccedMsg = "slipped and fell into a safe!";
                speccedSound = Tools.Sound.CROWD_AWW;
                shipChgMsg = "none";
                shipSound = 0;
                freqChgMsg = "none";
                freqSound = 0;
                m_botAction.sendSmartPrivateMessage(name, "Arenasettings applied for brickman.");
                return;
	        }
	    }
	    
	    // Default arena settings.
	    specPlayer = false;
	    changeShip = false;
	    changeFreq = false;
	    targetFreq = 1;
	    targetShip = Tools.Ship.SPIDER;
	    delaySeconds = 0;
	    enableMS = false;
	    speccedMsg = "none";
	    speccedSound = 0;
	    shipChgMsg = "none";
	    shipSound = 0;
	    freqChgMsg = "none";
	    freqSound = 0;
	    if(!arenaName.isEmpty())
	        m_botAction.sendSmartPrivateMessage(name, "Arena " + arenaName + " not found. Reverting to default settings.");
	    else
	        m_botAction.sendSmartPrivateMessage(name, "Reverting to default settings.");
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

		m_tsm.addSetting(INT,     "DelaySeconds", "0");
		m_tsm.addSetting(BOOLEAN, "EnableMS", "off");

		m_tsm.restrictSetting("TargetShip", 1, 8);
		m_tsm.restrictSetting("TargetFreq", 0, 9999);
		m_tsm.restrictSetting("DelaySeconds", 0, 1000);
	}

	public void cancel()
	{
		m_tsm.removeTSChangeListener(this);
		m_tsm.removeSetting("SpecPlayer");
		m_tsm.removeSetting("SpeccedMsg");
		m_tsm.removeSetting("ChangeShip");
		m_tsm.removeSetting("TargetShip");
		m_tsm.removeSetting("ShipChgMsg");
		m_tsm.removeSetting("ChangeFreq");
		m_tsm.removeSetting("TargetFreq");
		m_tsm.removeSetting("FreqChgMsg");
		m_tsm.removeSetting("TargetShip");
		m_tsm.removeSetting("TargetFreq");
		m_tsm.removeSetting("DelaySeconds");
		m_tsm.removeSetting("EnableMS");
        m_botAction.resetReliablePositionUpdating();
	}
	
	public class Pair {
	    private String key;
	    private Integer value;
	    
	    public Pair(String key, Integer value) {
	        this.key = key;
	        this.value = value;
	    }
	    
	    public String getKey() {
	        return this.key;
	    }
	    
	    public Integer getValue() {
	        return this.value;
	    }
	}
}
