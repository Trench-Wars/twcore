package twcore.core;

public class PlayerEntered extends SubspaceEvent
{

	int m_team;
	int m_wins;
	int m_losses;
	int m_hasKOTH;
	int m_shipType;
	String m_squadName;
	String m_playerName;
	int m_flagPoints;
	int m_killPoints;
	int m_playerID;
	int m_acceptsAudio;
	int m_flagsCarried;
	int m_identTurretee;

	public PlayerEntered(ByteArray array)
	{
		m_byteArray = array;
		m_eventType = EventRequester.PLAYER_ENTERED; //sets the event type in the superclass

		m_shipType = ((int) array.readByte(1) + 1) % 9;
		m_acceptsAudio = (int) array.readByte(2);
		m_playerName = array.readString(3, 20);
		m_squadName = array.readString(23, 20);
		m_flagPoints = array.readLittleEndianInt(43);
		m_killPoints = array.readLittleEndianInt(47);
		m_playerID = (int) array.readLittleEndianShort(51);
		m_team = (int) array.readLittleEndianShort(53);
		m_wins = (int) array.readLittleEndianShort(55);
		m_losses = (int) array.readLittleEndianShort(57);
		m_identTurretee = (int) array.readLittleEndianShort(59);
		m_flagsCarried = (int) array.readLittleEndianShort(61);
		m_hasKOTH = (int) array.readByte(63);
	}

	public int getTeam()
	{

		return m_team;
	}

	public int getWins()
	{

		return m_wins;
	}

	public int getLosses()
	{

		return m_losses;
	}

	public int getHasKOTH()
	{

		return m_hasKOTH;
	}

	public int getShipType()
	{

		return m_shipType;
	}

	public String getSquadName()
	{

		return m_squadName;
	}

	public String getPlayerName()
	{

		return m_playerName;
	}

	public int getFlagPoints()
	{

		return m_flagPoints;
	}

	public int getKillPoints()
	{

		return m_killPoints;
	}

	public int getPlayerID()
	{

		return m_playerID;
	}

	public int getAcceptsAudio()
	{

		return m_acceptsAudio;
	}

	public int getFlagsCarried()
	{

		return m_flagsCarried;
	}

	public int getIdentTurretee()
	{

		return m_identTurretee;
	}
}
