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

	public Result setValue(String arg)
	{
		int val;
		Result r = new Result();

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
		return r;
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