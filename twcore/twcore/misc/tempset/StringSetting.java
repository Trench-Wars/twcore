package twcore.misc.tempset;

class StringSetting extends TempSetting
{
	private String m_value, m_default;

	public StringSetting(String name, String defval)
	{
		super(name);
		m_default = defval;
		m_value = defval;
	}

	public String setValue(String arg, boolean changed)
	{
		m_value = arg;
		changed = true;
		return "Value for "+ m_name +" set to "+ arg;
	}

	public Object getValue()
	{
		return m_value;
	}
}