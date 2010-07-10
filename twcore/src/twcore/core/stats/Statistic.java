package twcore.core.stats;

/**
 * @author FoN
 * 
 */
public abstract class Statistic
{
	protected boolean m_derived = false;
	protected String m_shortForm = "";
	protected int m_statisticType;
	protected int m_intValue = 0;
	protected double m_doubleValue = 0.0;
	protected double m_weight = 1.0;

	protected int m_variableType = 0;	//either INT or DOUBLE can add more if needed
	private final int INT = 0;
	private final int DOUBLE = 1;
	
	//constructors	
	public Statistic(int statType, String shortForm)
	{
		m_shortForm = shortForm;
		m_statisticType = statType;
		m_variableType = INT;
	}
	
	public Statistic(int statType, String shortForm, boolean derived)
	{
		m_shortForm = shortForm;
		m_statisticType = statType;
		m_variableType = INT;
		m_derived = derived;
	}
	
	public Statistic(int statType, int initialValue, String shortForm)
	{
		m_shortForm = shortForm;
		m_statisticType = statType;
		m_intValue = initialValue;
		m_variableType = INT;		
	}
	
	public Statistic(int statType, double initialValue, String shortForm)
	{
		m_shortForm = shortForm;
		m_statisticType = statType;
		m_doubleValue = initialValue;
		m_variableType = DOUBLE;
	}
	
	public Statistic(int statType, double initialValue, String shortForm, boolean derived)
	{
		m_shortForm = shortForm;
		m_statisticType = statType;
		m_doubleValue = initialValue;
		m_derived = derived;
		m_variableType = DOUBLE;
	}
	
	/**
	 * @return String The printout of the stat
	 */
	public String toString()
	{
		if (m_variableType == INT)
			return m_shortForm + ":" + getIntValue() + " ";
		else
			return m_shortForm + ":" + getDoubleValue() + " ";
	}
	
	/**
	 * @return m_statType Stat type corresponding to Statistics.java
	 */
	public int getStatType()
	{
		return m_statisticType;
	}
	
	/**
	 * @return implemented in the superclass
	 */
	public int derivedInt()
	{
		return 0;
	}
	
	/**
	 * @return implemented in the superclass
	 */
	public double derivedDouble()
	{
		return 0.0;
	}
	
	/**
	 * @return int get stored value
	 */
	public int getIntValue()
	{

		if (m_variableType == INT)
		{
			//if derived
			if (m_derived)
			{
				synchronized(this)
				{
					return derivedInt() + m_intValue;
				}
			}
			else
				return m_intValue;
		}
		else
		{
			//if derived
			if (m_derived)
			{
				synchronized(this)
				{
					return derivedInt() + (int) m_doubleValue;
				}
			}
			else
				return (int) m_doubleValue;
		}
	}
	
	/**
	 * @return double get stored value
	 */
	public double getDoubleValue()
	{
		if (m_variableType == INT)
		{
			//if derived
			if (m_derived)
			{
				synchronized(this)
				{
					return derivedDouble() + (double)m_intValue;
				}
			}
			else
				return (double)m_intValue;
		}
		else
		{
			if (m_derived)
			{
				synchronized(this)
				{
					return derivedDouble() + m_doubleValue;
				}
			}
			else
				return m_doubleValue;
		}
	}
	
	/**
	 * @return m_variableType 0 for INT, 1 for DOUBLE
	 */
	public int getVariableType()
	{
		return m_variableType;
	}

	/**
	 * Returns the m_weight.
	 * @return int
	 */
	public double getWeight()
	{
		return m_weight;
	}

	/**
	 * Sets the m_weight.
	 * @param weight The m_weight to set
	 */
	public synchronized void setWeight(double weight)
	{
		m_weight = weight;
	}

	/**
	 * Adds one to intvalue
	 */
	public synchronized void setValue()
	{
		if (m_variableType == INT)
			m_intValue++;		
		else
			m_doubleValue++;
	}
	
	/**
	 * Adds to existing intValue
	 * @param value The value to add to existing variable
	 */
	public synchronized void setValue(int value)
	{
		if (m_variableType == INT)
			m_intValue += value;	
		else
			m_doubleValue += (double)value;
	}
	
	public synchronized void setValue(double value)
	{
		if (m_variableType == INT)
			m_intValue += (int)value;	
		else
			m_doubleValue += value;						
	}

	/**
	 * @param value Changes internal stat to specified value
	 */	
	public synchronized void changeValue(int value)
	{
		if (m_variableType == INT)
			m_intValue = value;	
		else
			m_doubleValue = (double)value;						
	}
	
	public synchronized void changeValue(double value)
	{
		if (m_variableType == INT)
			m_intValue = (int)value;	
		else
			m_doubleValue = value;						
	}

	/**
	 * decrement value by 1
	 */	
	public synchronized void decrement()
	{
		if (m_variableType == INT)
			m_intValue--;	
		else
			m_doubleValue--;						
	}
	
	/**
	 * @return double weighted value of the stat
	 */
	public double getWeightedValue()
	{
		if (m_variableType == INT)
			return m_intValue * m_weight;
		else
			return m_doubleValue * m_weight;
	}
}


