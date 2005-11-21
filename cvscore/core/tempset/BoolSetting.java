package twcore.core.tempset;


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