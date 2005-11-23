package twcore.misc.tempset;

abstract class TempSetting
{
	protected String m_name;

	public TempSetting(String name)
	{
		m_name = name;
	}

	public String getName()
	{
		return m_name;
	}

	public abstract Object getValue();
	public abstract Result setValue(String arg);
}

class Result
{
	public String response;
	public boolean changed;
	
	public Result()
	{
		response = "No change to made to setting.";
		changed = false;
	}	
}

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

class StringSetting extends TempSetting
{
	private String m_value, m_default;

	public StringSetting(String name, String defval)
	{
		super(name);
		m_default = defval;
		m_value = defval;
	}

	public Result setValue(String arg)
	{
		Result r = new Result();
		if(!m_value.equals(arg))
		{	
			m_value = arg;
		
			r.changed = true;
			r.response ="Value for "+ m_name +" set to "+ arg;
		}
		return r;
	}

	public Object getValue()
	{
		return m_value;
	}
}

class BoolSetting extends TempSetting
{
	boolean m_value, m_default;

	public BoolSetting(String name, boolean defval)
	{
		super(name);
		m_default = defval;
		m_value = defval;
	}

	public Result setValue(String arg)
	{
		arg = arg.toLowerCase();
		Result r = new Result();
		boolean val = (arg.equals("true")) || arg.equals("t") || arg.equals("on") || arg.equals("yes") || arg.equals("y");
		if(m_value != val)
		{
			r.changed = true;
			m_value = val;
			r.response = "Value for "+ m_name +" set to "+ m_value;
		}
		return r;
	}

	public Object getValue()
	{
		return new Boolean(m_value);
	}
}

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