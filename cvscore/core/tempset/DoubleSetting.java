package twcore.core.tempset;


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

	public Result setValue(String arg)
	{
		double val;
		Result r = new Result();

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
		return r;
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