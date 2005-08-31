package twcore.misc.tempset;

class IntSetting extends TempSetting
{
	private int m_value, m_default, m_min, m_max;
	private boolean m_restricted;

	public IntSetting(String name, int defval)
	{
		super(name);
		m_default = defval;
		m_value = defval;
		m_min = 0;
		m_max = 0;
		m_restricted = false;
	}

	public String setValue(String arg)
	{
		int val;

		try{
		val = Integer.parseInt(arg);
		}catch(Exception e)
		{
			return "Value for "+ m_name +" must be a valid integer.";
		}
		if(m_restricted)
			if(val < m_min || val > m_max)
				return "Value for "+ m_name +" must be between "+ m_min +" and "+ m_max;

		m_value = val;
		return "Value for "+ m_name +" set to "+ val;
	}

	public Object getValue()
	{
		return new Integer(m_value);
	}

	public void restrict(int min, int max)
	{
		m_min = min;
		m_max = max;
		m_restricted = true;
	}
}