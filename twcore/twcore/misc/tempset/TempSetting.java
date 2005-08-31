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
	public abstract String setValue(String arg);
}