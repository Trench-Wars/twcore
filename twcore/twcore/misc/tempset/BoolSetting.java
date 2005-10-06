package twcore.misc.tempset;

class BoolSetting extends TempSetting
{
	boolean m_value, m_default;

	public BoolSetting(String name, boolean defval)
	{
		super(name);
		m_default = defval;
		m_value = defval;
	}

	public String setValue(String arg, boolean changed)
	{
		arg = arg.toLowerCase();
		m_value = (arg.equals("true")) || arg.equals("t") || arg.equals("on") || arg.equals("yes") || arg.equals("y");
		changed = true;
		return "Value for "+ m_name +" set to "+ m_value;
	}

	public Object getValue()
	{
		return new Boolean(m_value);
	}
}