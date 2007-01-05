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

import twcore.core.*;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerPosition;
import twcore.core.events.BallPosition;
import twcore.core.game.Ship;
import twcore.core.game.Player;
import java.util.*;

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