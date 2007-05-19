//////////////////////////////////////////////////////
//
// Filename:
// InterpretHistory.java
//
// Description:
// Chronological log of incidents
//
// Usage:
//
//
//
//////////////////////////////////////////////////////

package twcore.bots.ballbot;


class IncidentHistory
{
	public static final int HISTORY_LENGTH = 20;
	Incident[] m_history = new Incident[ HISTORY_LENGTH ];
	int m_writePtr = 0;

	//////////////////////////////////////////////////////////////////////
	// History buffer access
	//////////////////////////////////////////////////////////////////////
	public Incident GetLastIncident()
	{
		return GetIncident( 0 );
	}

	public Incident GetIncident( int age )
	{
		int readLastPtr = (m_writePtr + HISTORY_LENGTH - (1 + age)) % HISTORY_LENGTH;
		return m_history[readLastPtr];
	}

	public void AppendIncident( Incident incident )
	{
		m_history[ m_writePtr ] = incident;
		m_writePtr++;
		m_writePtr = m_writePtr % HISTORY_LENGTH;
	}
}