package twcore.core;

public class Message extends SubspaceEvent
{

	String m_message;
	String m_messager;
	int m_playerID;
	int m_soundCode;
	int m_chatNumber;
	int m_messageType;
	String m_alertCommandType;

	public static final int ARENA_MESSAGE = 0x01;
	public static final int PUBLIC_MACRO_MESSAGE = 0x02;
	public static final int PUBLIC_MESSAGE = 0x04;
	public static final int TEAM_MESSAGE = 0x08;
	public static final int OPPOSING_TEAM_MESSAGE = 0x10;
	public static final int PRIVATE_MESSAGE = 0x20;
	public static final int WARNING_MESSAGE = 0x40;
	public static final int REMOTE_PRIVATE_MESSAGE = 0x80;
	public static final int SERVER_ERROR = 0x100;
	public static final int CHAT_MESSAGE = 0x200;
	public static final int ALERT_MESSAGE = 0x400;
	//public static int CHEATER_MESSAGE = 0x800;
	//public static int ADVERT_MESSAGE = 0x1000;

	public static String[] alertCommands = null;

	public Message(ByteArray array)
	{
		m_eventType = EventRequester.MESSAGE; //sets the event type in the superclass
		
		int nameEnding;
		int nameBeginning;

		m_chatNumber = 0;
		m_messager = null;
		m_messageType = (1 << (int) array.readByte(1));
		m_soundCode = (int) array.readByte(2);
		m_playerID = (int) array.readLittleEndianShort(3);
		m_message = array.readString(5, array.size() - 6);

		if (m_messageType == Message.ARENA_MESSAGE && m_message.startsWith("misc:alertcommand:"))
		{
			alertCommands = m_message.substring(18).split(",");
		}
		else
		{
			// The parsing of the name from the remote private message, private message, and chat message
			// could possibly inaccurate.  The message isn't meant to have the name parsed out of it.
			if (m_messageType == REMOTE_PRIVATE_MESSAGE)
			{
				if (alertCommands != null)
				{
					for (int i = 0; i < alertCommands.length; i++)
					{
						if (m_message.startsWith(alertCommands[i] + ':'))
						{
							m_messageType = ALERT_MESSAGE;
							m_alertCommandType = alertCommands[i];
							break;
						}
					}
				}

				if (m_messageType == REMOTE_PRIVATE_MESSAGE)
				{
					nameBeginning = m_message.indexOf('(');
					nameEnding = m_message.indexOf(")>");
					m_messager = m_message.substring(nameBeginning + 1, nameEnding);
					m_message = m_message.substring(nameEnding + 2);
				}
				else
				{
					nameBeginning = m_message.indexOf('(');
					nameEnding = m_message.indexOf(") (");
					m_messager = m_message.substring(nameBeginning + 1, nameEnding);
					m_message = m_message.substring(nameEnding + 2);
				}
			}
			else if (m_messageType == CHAT_MESSAGE)
			{
				m_chatNumber = Integer.valueOf(m_message.substring(0, 1)).intValue();
				nameBeginning = 2;
				nameEnding = m_message.indexOf("> ");
				if (nameEnding == -1)
				{
					m_messager = m_message.substring(nameBeginning);
					m_message = "";
				}
				else
				{
					m_messager = m_message.substring(nameBeginning, nameEnding);
					m_message = m_message.substring(nameEnding + 2);
				}
			}
		}
	}

	public int getPlayerID()
	{

		return m_playerID;
	}

	public int getSoundCode()
	{

		return m_soundCode;
	}

	public int getMessageType()
	{

		return m_messageType;
	}

	public String getMessage()
	{

		return m_message;
	}

	public String getMessager()
	{

		return m_messager;
	}

	public String getAlertCommandType()
	{

		return m_alertCommandType;
	}

	public int getChatNumber()
	{

		return m_chatNumber;
	}
}
