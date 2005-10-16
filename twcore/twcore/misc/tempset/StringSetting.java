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