package twcore.misc.tempset;

/**
 * Abstract TempSetting all specific types of settings must extend. Provides
 * name storage and individual locking functionality.
 *
 * @author D1st0rt
 * @version 06.02.08
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

}

/**
 * Simple storage class to record the result of an attempt to change a setting
 *
 * @author D1st0rt
 * @version 06.02.08
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
 * @version 06.02.08
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
}

/**
 * This class represents a setting that stores a string value
 *
 * @author D1st0rt
 * @version 06.02.08
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
}

/**
 * This class represents a setting that stores a boolean value
 *
 * @author D1st0rt
 * @version 06.02.08
 */
class BoolSetting extends TempSetting
{
	/** The current value of the setting */
	boolean m_value;

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
}

/**
 * This class represents a setting that stores a floating point value
 *
 * @author D1st0rt
 * @version 06.02.08
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
}