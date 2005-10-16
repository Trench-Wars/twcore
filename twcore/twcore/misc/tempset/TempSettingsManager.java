package twcore.misc.tempset;

import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twcore.core.BotAction;
import twcore.core.CommandInterpreter;
import twcore.core.Message;
import twcore.core.OperatorList;
import twcore.core.Tools;

/**
 * This class provides an easy way to allow an op to modify temporary settings.
 * The TempSettingsManager interfaces with the bot and keeps track of all the
 * TempSettings. Settings are identified by name and are case-sensitive, so
 * don't make duplicates in the same case. It registers !get and !set with
 * the Command Interpreter so all the bot writer has to do is add whatever
 * settings they want. Just make sure there are no other commands using !get/!set.
 * @author D1st0rt
 */
public class TempSettingsManager
{
	private BotAction m_botAction;
	private OperatorList m_opList;
	private int m_opLevel = OperatorList.ER_LEVEL;
	private HashMap<String, TempSetting> m_settings;
	private boolean m_locked;
	private Vector<TSChangeListener> m_listeners;

	/**
	 * @param botAction The BotAction event to send chat messages with
	 * @param cmd The CommandInterpreter to register commands with
	 * @param opLevel The minimum Operator Level to access the settings
	 */
	public TempSettingsManager(BotAction botAction, CommandInterpreter cmd, int opLevel)
	{
		m_botAction = botAction;
		m_opLevel = opLevel;
		m_opList = botAction.getOperatorList();
		m_settings = new HashMap<String, TempSetting>();
		m_locked = false;
		m_listeners = new Vector<TSChangeListener>();
		registerCommands(cmd);
	}

	/**
	 * Registers the commands !get and !set with the provided CommandInterpreter
	 * @param cmd the CommandInterpreter to use
	 */
	private void registerCommands(CommandInterpreter cmd)
	{
		cmd.registerCommand("!get", Message.PRIVATE_MESSAGE, this, "c_Get", m_opLevel);
		cmd.registerCommand("!set", Message.PRIVATE_MESSAGE, this, "c_Set", m_opLevel);
	}

	/**
	 * Adds a setting into the list, enabling it for use
	 * @param type The type of setting to be added
	 * @param name The name of the setting to be added
	 * @param defval The default value of this setting (what it will be initially)
	 */
	public void addSetting(SType type, String name, String defval)
	{
		switch(type)
		{
			case STRING:
				StringSetting sset = new StringSetting(name, defval);
				m_settings.put(name, sset);
			break;

			case INT:
				int idefault = Integer.parseInt(defval);
				IntSetting iset = new IntSetting(name, idefault);
				m_settings.put(name, iset);
			break;

			case DOUBLE:
				double ddefault = Double.parseDouble(defval);
				DoubleSetting dset = new DoubleSetting(name, ddefault);
				m_settings.put(name, dset);
			break;

			case BOOLEAN:
				String arg = defval.toLowerCase();
				boolean bdefault = (arg.equals("true")) || arg.equals("t") || arg.equals("on") || arg.equals("yes") || arg.equals("y");
				BoolSetting bset = new BoolSetting(name, bdefault);
				m_settings.put(name, bset);
			break;

			default:
				Tools.printLog("Could not add setting "+ name +" (unknown type)");
		}
	}

	/**
	 * This is used for when you want to restrict an integer setting within a certain range of numbers
	 * @param name The name of the setting to restrict
	 * @param min The minimum value to allow
	 * @param max The maximum value to allow
	 */
	public void restrictSetting(String name, int min, int max)
	{
		TempSetting t = m_settings.get(name);
		if(t == null)
			Tools.printLog("TempSet: Could not restrict setting "+name +" (doesn't exist)");
		else if(! (t instanceof IntSetting))
			Tools.printLog("TempSet: Could not restrict setting "+ name +" (not an int setting)");
		else
		{
			IntSetting iset = (IntSetting)t;
			iset.restrict(min, max);
		}
	}

	/**
	 * This is used for when you want to restrict a double setting within a certain range of numbers
	 * @param name The name of the setting to restrict
	 * @param min The minimum value to allow
	 * @param max The maximum value to allow
	 */
	public void restrictSetting(String name, double min, double max)
	{
		TempSetting t = m_settings.get(name);
		if(t == null)
			Tools.printLog("TempSet: Could not restrict setting "+name +" (doesn't exist)");
		else if(!(t instanceof DoubleSetting))
			Tools.printLog("TempSet: Could not restrict setting "+ name +" (not a double setting)");
		else
		{
			DoubleSetting dset = (DoubleSetting)t;
			dset.restrict(min, max);
		}
	}

	/**
	 * Gets a setting by name, this is for when you need to access the value in the code of your bot.
	 * @param name The name of the setting to retrieve
	 * @return The setting contained in an Object, ready for casting :D
	 */
	public Object getSetting(String name)
	{
		TempSetting t = m_settings.get(name);
		if(t == null)
			Tools.printLog("TempSet: Could not retrieve setting "+name +" (doesn't exist)");
		return m_settings.get(name).getValue();
	}

	/**
	 * This changes the value of a setting, with an optional message being returned
	 * @param name The name of the setting to change
	 * @param arg The new value you want for the setting 
	 * @return A message describing the success or failure of the set attempt
	 */
	public String setValue(String name, String arg)
	{
		TempSetting t = m_settings.get(name);
		if(t == null)
			return "Setting "+ name +" does not exist";
		
		Result r = t.setValue(arg);
		if(r.changed)
			for(TSChangeListener l : m_listeners)
				l.settingChanged(name, arg);
		
		return r.response;
	}

	/**
	 * This is the command function registered with the CommandInterpreter under !set
	 * @param name The name of the player that sent the message
	 * @param message The parameters sent along after "!set"
	 */
	public void c_Set(String name, String message)
	{
		if(m_opList.getAccessLevel(name) >= m_opLevel)
		{
			if(message.equalsIgnoreCase("help"))
			{
				String[] help = new String[]{
					"Use the !set command to change temporary bot settings",
					"Syntax: !set <name1>=<value1> <name2>=<value2> ...",
					"-----Modifiable Settings:-----"};
				String[] sets = m_settings.keySet().toArray(new String[]{});

				m_botAction.privateMessageSpam(name, help);
				m_botAction.privateMessageSpam(name, sets);
			}
			else if(!m_locked)
			{
				Matcher regex;
				regex = Pattern.compile("(\\w+)=(\\w+)").matcher(message);
				while(regex.find())
				{
					String old = ""+ getSetting(regex.group(1));
					m_botAction.sendPrivateMessage(name, setValue(regex.group(1), regex.group(2)));
					if(old.equals(regex.group(2)))
						for(TSChangeListener l: m_listeners)
							l.settingChanged(regex.group(1), regex.group(2));
				}
			}
			else
				m_botAction.sendPrivateMessage(name, "Settings are currently locked.");
		}
	}

	/**
	 * This is the command function registered with the CommandInterpreter under !get
	 * @param name The name of the player that sent the message
	 * @param message The parameters sent along after "!get"
	 */
	public void c_Get(String name, String message)
	{
		if(m_opList.getAccessLevel(name) >= m_opLevel)
		{
			message = message.trim();
			TempSetting t = m_settings.get(message);
			if(t == null)
				m_botAction.sendPrivateMessage(name, "Setting "+ message +" does not exist");
			else
				m_botAction.sendPrivateMessage(name, t.getName() + "=" + t.getValue());
		}
	}

	/**
	 * This locks/unlocks settings from being modified through the command interface (!set)
	 * @param locked Whether settings should be locked or unlocked
	 */
	public void setLocked(boolean locked)
	{
		m_locked = locked;
	}
	
	public void addTSChangeListener(TSChangeListener t)
	{
		if(!m_listeners.contains(t))
			m_listeners.add(t);
	}
}