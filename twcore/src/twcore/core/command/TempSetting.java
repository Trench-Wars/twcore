package twcore.core.command;

import twcore.core.BotAction;
import twcore.core.game.Player;

/**
 * Abstract TempSetting all specific types of settings must extend. Provides
 * name storage and individual locking functionality.
 *
 * @author D1st0rt
 * @version 06.05.24
 */
abstract class TempSetting
{
	/** The name of the setting */
	protected String m_name;
	/** The setting's locked status */
	protected boolean m_locked;

	/**
     * Creates a new TempSetting
     * @param name the name of the setting
     */
	public TempSetting(String name)
	{
		m_name = name;
		m_locked = false;
	}

	/**
	 * Gets the setting's name
	 * @return the name of the setting
	 */
	public String getName()
	{
		return m_name;
	}

	/**
	 * Sets the status of the setting as locked or unlocked
	 * @param locked the new locked status
	 */
	public void setLocked(boolean locked)
	{
		m_locked = locked;
	}

	/**
     * Gets the status of the setting as locked or unlocked
     * @return the locked status of the setting
     */
	public boolean getLocked()
	{
		return m_locked;
	}

	/**
     * Attempts to change the value of this setting
     * @param arg the new intended value
     * @return the Result of the value change
     */
	public Result setValue(String arg)
	{
		Result r = null;
		if(m_locked)
		{
			r = new Result();
			r.response = "That setting is currently locked.";
		}

		return r;
	}

	/**
     * Gets the setting value. All subclasses must define this method.
     * @return the value of this setting as an Object
     */
	public abstract Object getValue();

	/**
	 * Gets information about the setting to be displayed in !set help.
	 * @return various details about the nature of the setting
	 */
	public abstract String getInfo();

}

/**
 * Simple storage class to record the result of an attempt to change a setting
 *
 * @author D1st0rt
 * @version 06.05.24
 */
class Result
{
	/** The response message from the attempt */
	public String response;
	/** Whether the value was actually changed or not */
	public boolean changed;

	/**
     * Creates a new Result defaulting to no changes made
     */
	public Result()
	{
		response = "No change to made to setting.";
		changed = false;
	}
}

/**
 * This class represents a setting that stores an integer value
 *
 * @author D1st0rt
 * @version 06.06.19
 */
class IntSetting extends TempSetting
{
	/** The current value of the setting */
	private int m_value;
	/** The minimum allowed value of the setting */
	private int m_min;
	/** The maximum allowed value of the setting */
	private int m_max;
	/** Whether the setting has a restricted range or not */
	private boolean m_restricted;

	/**
     * Creates a new IntSetting
     * @param name the name of the setting
     * @param defval the setting's initial value
     */
	public IntSetting(String name, int defval)
	{
		super(name);
		m_value = defval;
		m_min = 0;
		m_max = 0;
		m_restricted = false;
	}

	/**
     * Attempts to change the value of this setting
     * @param arg the new intended value
     * @return the Result of the value change
     */
	public Result setValue(String arg)
	{
		int val;
		Result r = super.setValue(arg);

		if(r == null)
		{
			r = new Result();
			try{
			val = Integer.parseInt(arg);
			}catch(Exception e)
			{
				r.response = "Value for "+ m_name +" must be a valid integer.";
				return r;
			}
			if(m_restricted)
				if(val < m_min || val > m_max)
				{
					r.response = "Value for "+ m_name +" must be between "+ m_min +" and "+ m_max;
					return r;
				}

			m_value = val;
			r.changed = true;
			r.response = "Value for "+ m_name +" set to "+ val;
		}
		return r;
	}

	/**
	 * Gets the current value of this setting
	 * @return the value as an Object casted Integer
	 */
	public Object getValue()
	{
		return new Integer(m_value);
	}

	/**
     * Restricts the range of allowed setting values
     * @param min the smallest allowed value
     * @param max the largest allowed value
     */
	public void restrict(int min, int max)
	{
		m_min = min;
		m_max = max;
		m_restricted = true;
	}

	/**
	 * Gets information about the setting to be displayed in !set help.
	 * @return various details about the nature of the setting
	 */
	public String getInfo()
	{
		String info = "[INTEGER] " + m_name;

		if(m_restricted)
		{
			info += " (Range: "+ m_min;
			info += "-"+ m_max;
			info += ")";
		}

		if(m_locked)
			info = "*"+ info;
		return info;
	}
}

/**
 * This class represents a setting that stores a string value
 *
 * @author D1st0rt
 * @version 06.05.24
 */
class StringSetting extends TempSetting
{
	/** The current value of the setting */
	private String m_value;

	/**
     * Creates a new StringSetting
     * @param name the name of the setting
     * @param defval the setting's initial value
     */
	public StringSetting(String name, String defval)
	{
		super(name);
		m_value = defval;
	}

	/**
     * Attempts to change the value of this setting
     * @param arg the new intended value
     * @return the Result of the value change
     */
	public Result setValue(String arg)
	{
		Result r = super.setValue(arg);
		if(r == null)
		{
			r = new Result();
			if(!m_value.equals(arg))
			{
				m_value = arg;

				r.changed = true;
				r.response ="Value for "+ m_name +" set to "+ arg;
			}
		}
		return r;
	}

	/**
	 * Gets the current value of this setting
	 * @return the value as an Object casted Integer
	 */
	public Object getValue()
	{
		return m_value;
	}

	/**
	 * Gets information about the setting to be displayed in !set help.
	 * @return various details about the nature of the setting
	 */
	public String getInfo()
	{
		String info = "[STRING ] "+ m_name;
		if(m_locked)
			info = "*"+ info;
		return info;
	}
}

/**
 * This class represents a setting that stores a boolean value
 *
 * @author D1st0rt
 * @version 06.05.24
 */
class BoolSetting extends TempSetting
{
	/** The current value of the setting */
	private boolean m_value;

	/**
     * Creates a new BoolSetting
     * @param name the name of the setting
     * @param defval the setting's initial value
     */
	public BoolSetting(String name, boolean defval)
	{
		super(name);
		m_value = defval;
	}

	/**
     * Attempts to change the value of this setting
     * @param arg the new intended value
     * @return the Result of the value change
     */
	public Result setValue(String arg)
	{
		arg = arg.toLowerCase();
		Result r = super.setValue(arg);
		if(r == null)
		{
			r = new Result();
			boolean val = (arg.equals("true")) || arg.equals("t") || arg.equals("on") || arg.equals("yes") || arg.equals("y");
			if(m_value != val)
			{
				r.changed = true;
				m_value = val;
				r.response = "Value for "+ m_name +" set to "+ m_value;
			}
		}
		return r;
	}

	/**
	 * Gets the current value of this setting
	 * @return the value as an Object casted Boolean
	 */
	public Object getValue()
	{
		return new Boolean(m_value);
	}

	/**
	 * Gets information about the setting to be displayed in !set help.
	 * @return various details about the nature of the setting
	 */
	public String getInfo()
	{
		String info = "[BOOLEAN] "+ m_name;
		if(m_locked)
			info = "*"+ info;
		return info;
	}
}

/**
 * This class represents a setting that stores a floating point value
 *
 * @author D1st0rt
 * @version 06.05.24
 */
class DoubleSetting extends TempSetting
{
	/** The current value of the setting */
	private double m_value;
	/** The minimum allowed value of the setting */
	private double m_min;
	/** The maximum allowed value of the setting */
	private double m_max;
	/** Whether the setting has a restricted range or not */
	private boolean m_restricted;

	/**
     * Creates a new DoubleSetting
     * @param name the name of the setting
     * @param defval the setting's initial value
     */
	public DoubleSetting(String name, double defval)
	{
		super(name);
		m_value = defval;
		m_min = 0.0;
		m_max = 0.0;
		m_restricted = false;
	}

	/**
     * Attempts to change the value of this setting
     * @param arg the new intended value
     * @return the Result of the value change
     */
	public Result setValue(String arg)
	{
		double val;
		Result r = super.setValue(arg);

		if(r == null)
		{
			r = new Result();
			try{
			val = Double.parseDouble(arg);
			}catch(Exception e)
			{
				r.response = "Value for "+ m_name +" must be a valid number.";
				return r;
			}
			if(m_restricted)
				if(val < m_min || val > m_max)
				{
					r.response = "Value for "+ m_name +" must be between "+ m_min +" and "+ m_max;
					return r;
				}

			m_value = val;
			r.changed = true;
			r.response = "Value for "+ m_name +" set to "+ val;
		}
		return r;
	}

	/**
	 * Gets the current value of this setting
	 * @return the value as an Object casted Double
	 */
	public Object getValue()
	{
		return new Double(m_value);
	}

	/**
     * Restricts the range of allowed setting values
     * @param min the smallest allowed value
     * @param max the largest allowed value
     */
	public void restrict(double min, double max)
	{
		m_min = min;
		m_max = max;
		m_restricted = true;
	}

	/**
	 * Gets information about the setting to be displayed in !set help.
	 * @return various details about the nature of the setting
	 */
	public String getInfo()
	{
		String info = "[DOUBLE ] " + m_name;

		if(m_restricted)
		{
			info += " (Range: "+ m_min;
			info += "-"+ m_max;
			info += ")";
		}

		if(m_locked)
			info = "*"+ info;
		return info;
	}
}

/**
 * This class represents a setting that stores a string value that exists in a
 * predefined enumeration.
 *
 * @author D1st0rt
 * @version 06.05.24
 */
class EnumSetting extends TempSetting
{
	/** The set of possible values */
	private String[] m_values;

	/** The index of the current value */
	private int m_index;

	/**
     * Creates a new EnumSetting
     * @param name the name of the setting
     * @param defval the setting's initial value. This value is the only
     * 	      option available in the list until more are added.
     */
	public EnumSetting(String name, String defval)
	{
		super(name);
		m_values = new String[]{ defval };
		m_index = 0;
	}

	/**
     * Sets the list of possible values for this setting.
     * @param values a string array of valid options
     */
	public void setOptions(String[] values)
	{
		m_values = values;
		if(m_index >= m_values.length)
			m_index = 0;
	}

	/**
	 * Adds a new setting to the list of options
	 */
	public void addSetting(String option)
	{
		String[] newArray = new String[m_values.length + 1];
		System.arraycopy(m_values, 0, newArray, 0, m_values.length);
		m_values = newArray;
		m_values[m_values.length - 1] = option;
	}

	/**
     * Attempts to change the value of this setting
     * @param arg the new intended value
     * @return the Result of the value change
     */
	public Result setValue(String arg)
	{
		int index = -1;
		Result r = super.setValue(arg);

		if(r == null)
		{
			r = new Result();

			for(int x = 0; x < m_values.length; x++)
			{
				if(m_values[x].equalsIgnoreCase(arg))
					index = x;
			}

			if(index == -1)
			{
				r.response = "Value for "+ m_name +" must be contained in the list of options.";
				return r;
			}

			m_index = index;
			r.changed = true;
			r.response = "Value for "+ m_name +" set to "+ m_values[m_index];
		}
		return r;
	}

	/**
	 * Gets the current value of this setting
	 * @return the value as an Object casted String
	 */
	public Object getValue()
	{
		return m_values[m_index];
	}

	/**
	 * Gets information about the setting to be displayed in !set help.
	 * @return various details about the nature of the setting
	 */
	public String getInfo()
	{
		String info = "[ENUM   ] " + m_name;
		info += " (Values: ";

		for(String val : m_values)
		{
			info += val + ", ";
		}
		info += ")";

		if(m_locked)
			info = "*"+ info;
		return info.substring(0, info.length() - 2);
	}
}

/**
 * This class represents a setting that stores a Player value that is modified
 * through a fuzzy search on the specified player name.
 *
 * @author D1st0rt
 * @version 06.06.26
 */
class PlayerSetting extends TempSetting
{
	/** The current value of the setting */
	private Player m_player;
	/** The BotAction object used for player lookups */
	private BotAction m_botAction;
	/** Whether the player has a restricted ship type or not */
	private boolean m_restrictedShip;
	/** The allowed ships to grab a player from */
	private byte m_shipMask;
	/** Whether the player has a restricted freq range or not */
	private boolean m_restrictedFreq;
	/** The minimum allowed frequency to grab a player from */
	private int m_minFreq;
	/** The maximum allowed frequency to grab a player from */
	private int m_maxFreq;

	/**
     * Creates a new PlayerSetting
     * @param name the name of the setting
     * @param botAction the BotAction object to use for player lookups
     */
	public PlayerSetting(String name, BotAction botAction)
	{
		super(name);
		m_botAction = botAction;
		m_shipMask = 0x7F;
		m_minFreq = 0;
		m_maxFreq = 9999;
		m_restrictedFreq = false;
		m_restrictedShip = false;
	}

	/**
     * Attempts to change the value of this setting
     * @param arg the new intended value
     * @return the Result of the value change
     */
	public Result setValue(String arg)
	{
		Player temp;
		Player p = null;
		Result r = super.setValue(arg);

		if(r == null)
		{
			r = new Result();

			if(arg.equalsIgnoreCase("none"))
			{
				if(m_player != null)
				{
					r.response = "Value for "+ m_name +" set to none.";
					r.changed = true;
				}
			}
			else
			{
				p = m_botAction.getFuzzyPlayer(arg);

				if(p != null)
				{
					temp = p;
					r.changed = true;
					r.response = "Value for "+ m_name +" set to "+ p;

					if(m_restrictedShip)
					{
						if((p.getShipType() & m_shipMask) == 0)
						{
							r.changed = false;
							r.response = p +" is not in an allowed ship.";
							temp = m_player;
						}
					}

					if(m_restrictedFreq)
					{
						if(p.getFrequency() <= m_minFreq || p.getFrequency() >= m_maxFreq)
						{
							r.changed = false;
							r.response = p +" is not on an allowed freq.";
							temp = m_player;
						}
					}

					m_player = temp;
				}
				else
					r.response = "That player was not found in this arena.";

			}
		}
		return r;
	}

	/**
	 * Gets the current value of this setting
	 * @return the value as an Object casted Player. Be careful because this
	 * 		   value could possibly be null.
	 */
	public Object getValue()
	{
		return m_player;
	}

	/**
     * Sets whether or not this value can be set to a player in a particular
     * ship, where ship ranges from 0 (warbird) to 7 (shark).
     * @param ship the ship to change the value for
     * @param allowed whether the ship is allowed or not
     */
	public void setShipAllowed(int ship, boolean allowed)
	{
		if(ship < 8 && ship > -1)
		{
			m_restrictedShip = true;

			if(allowed)
				m_shipMask |= (1 << ship);
			else
				m_shipMask &= ~(1 << ship);
		}
	}

	/**
     * Restricts the range of allowed setting values
     * @param minFreq the smallest allowed value
     * @param maxFreq the largest allowed value
     */
	public void restrictFreq(int minFreq, int maxFreq)
	{
		if(minFreq > 0 && minFreq < 9999 && maxFreq > 0 && maxFreq < 9999)
		{
			m_restrictedFreq = true;

			m_minFreq = minFreq;
			m_maxFreq = maxFreq;
		}
	}

	/**
	 * Gets information about the setting to be displayed in !set help.
	 * @return various details about the nature of the setting
	 */
	public String getInfo()
	{
		String info = "[PLAYER ]";
		info += " "+ m_name;

		if(m_restrictedFreq)
		{

			info += " (Freq Range: "+ m_minFreq;
			info += "-"+ m_maxFreq + ")";
		}

		if(m_restrictedShip)
		{
			info += " (Allowed Ships: ";
			for(int ship = 0; ship < 8; ship++)
			{
				if((ship & m_shipMask) != 0)
				{
					info += (ship + 1);
				}
			}
			info += ")";
		}

		if(m_locked)
			info = "*"+ info;

		return info;
	}
}