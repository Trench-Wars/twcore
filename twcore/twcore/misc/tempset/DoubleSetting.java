package twcore.misc.tempset;

class DoubleSetting extends TempSetting
{
	private double m_value, m_default, m_min, m_max;
	private boolean m_restricted;

	public DoubleSetting(String name, double defval)
	{
		super(name);
		m_default = defval;
		m_value = defval;
		m_min = 0.0;
		m_max = 0.0;
		m_restricted = false;
	}

	public String setValue(String arg, boolean changed)
	{
		double val;

		try{
		val = Double.parseDouble(arg);
		}catch(Exception e)
		{
			return "Value for "+ m_name +" must be a valid number.";
		}
		if(m_restricted)
			if(val < m_min || val > m_max)
				return "Value for "+ m_name +" must be between "+ m_min +" and "+ m_max;

		m_value = val;
		changed = true;
		return "Value for "+ m_name +" set to "+ val;
	}

	public Object getValue()
	{
		return new Double(m_value);
	}

	public void restrict(double min, double max)
	{
		m_min = min;
		m_max = max;
		m_restricted = true;
	}
}