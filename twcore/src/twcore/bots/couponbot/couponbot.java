package twcore.bots.couponbot;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import twcore.core.BotAction;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.InterProcessEvent;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.SQLResultEvent;
import twcore.core.util.Tools;
import twcore.core.util.ipc.IPCMessage;

/**
 * 
 * @author Arobas+
 *
 */
public class couponbot extends SubspaceBot {

	private final static String IPC_CHANNEL = "pubmoney";
	
	private String arena;
	private String database;
	
	private HashSet<String> operators;
	
	private HashMap<String,Long> history; 

	public couponbot(BotAction botAction) {
		super(botAction);
		requestEvents();
		history = new HashMap<String,Long>();
	}

    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.LOGGED_ON);

    }
    
    public void handleEvent(LoggedOn event) {
    	operators = new HashSet<String>();
    	database = m_botAction.getBotSettings().getString("Database");
    	arena = m_botAction.getBotSettings().getString("Arena");
    	if (m_botAction.getBotSettings().getString("Operators") != null) {
    		List<String> list = Arrays.asList(m_botAction.getBotSettings().getString("Operators").split("\\s*,\\s*"));
    		for(String name: list) {
    			operators.add(name.toLowerCase());
    		}
    	}
		m_botAction.joinArena(arena);
		m_botAction.ipcSubscribe(IPC_CHANNEL);
    }
	
    
    public void handleEvent(Message event) {
    	
        String sender = getSender(event);
        String message = event.getMessage().trim();

        if (message == null || sender == null)
            return;

        if (event.getMessageType() == Message.PRIVATE_MESSAGE 
        		|| event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {
        	
        	handleCommand(sender, message, event.getMessageType());
        }
    }
    
    public void handleEvent(SQLResultEvent event) {
    	
    	// ResultSet without identifier.. close it!
    	if (event.getIdentifier() == null || event.getIdentifier().equals("")) {
    		try {
				event.getResultSet().close();
			} catch (SQLException e) { 
				Tools.printStackTrace(e);
			}
    	}
    }
    
    public void handleEvent(InterProcessEvent event) {
    	if (event.getObject() instanceof IPCMessage) {
    		
    		IPCMessage ipc = (IPCMessage) event.getObject();
    		if (ipc.getRecipient() == null)
    			return;
    		if (!ipc.getRecipient().equals("couponbot"))
    			return;

    		if (ipc.getMessage().startsWith("couponsuccess:")) {
    			
    			String[] pieces = ipc.getMessage().split(":");
    			String codeString = pieces[1];
    			String name = pieces[2];
    			
    			if (history.containsKey(name+":"+codeString)) {
    				long time = history.get(name+":"+codeString);
    				if (System.currentTimeMillis()-time < 2*Tools.TimeInMillis.SECOND) {
    					// The bot get spammed by the sender with the same message
    					// It happens on TW but not TW Dev for some reason..
    					// Weird!!
    					return;
    				}
    			}

    			CouponCode code = getCode(codeString);
    			
    			// +1 used
    			code.setUsed(code.getUsed()+1);
    			if (code.getUsed() == code.getMaxUsed())
    				code.setEnabled(false);
    			
    			if (updateDB(code)) {
    				updateHistoryDB(name, code);
    				m_botAction.sendSmartPrivateMessage(name, "$" + code.getMoney() + " has been added to your account.");
 
    			} else {
    				m_botAction.sendSmartPrivateMessage(name, "A problem has occured. Please contact someone from the staff by using ?help");
    			}
    			
    			history.put(name+":"+codeString, System.currentTimeMillis());
    			
    		} else if (ipc.getMessage().startsWith("couponerror")) {
    			
    			String[] pieces = ipc.getMessage().split(":");
    			String codeString = pieces[1];
    			String name = pieces[2];
    			
    			m_botAction.sendSmartPrivateMessage(name, "A problem has occured. Please contact someone from the staff by using ?help");
    		}
    	}
    }

    private void handleHelpCommand(String sender) {
    	
    	String general[] = new String[] {
    		"[General]",
    		"- !coupon <code>             -- Redeem your <code>."
    	};
    	
    	String generation[] = new String[] {
    		"[Code Generation]",
    		"- !create <money>            -- Create a random code for <money>. Use !limituse/!expiredate for more options.",
    		"- !create <code>:<money>     -- Create a custom code for <money>. Max of 32 characters.",
    		"- !limituse <code>:<max>     -- Set how many players <max> can get this <code>.",
    		"- !expiredate <code>:<date>  -- Set an expiration <date> (format: yyyy/mm/dd) for <code>.",
    	};
    	
    	String maintenance[] = new String[] {
    		"[Maintenance]",
    		"- !info <code>               -- Information about this <code>.",
    		"- !users <code>              -- Who used this code.",
    		"- !enable <code>             -- Enable a <code> previously disabled.",
    		"- !disable <code>            -- Disable a <code>.",
    	};
    	
    	String bot[] = new String[] {
    		"[Bot] (Smod+)",
    		"- !addop <name>              -- Add an operator (an operator can generate a code to be used).",
    		"- !listops                   -- List of operators.",
    		"- !go <arena>                -- Move this bot to <arena>.",
    		"- !die                       -- Kill this bot.",
    	};
    	
    	List<String> lines = new ArrayList<String>();
    	if (m_botAction.getOperatorList().isSmod(sender)) {
    		lines.addAll(Arrays.asList(general));
    		lines.add(" ");
    		lines.addAll(Arrays.asList(generation));
    		lines.add(" ");
    		lines.addAll(Arrays.asList(maintenance));
    		lines.add(" ");
    		lines.addAll(Arrays.asList(bot));
    		
    	} else if (operators.contains(sender.toLowerCase())) {
    		lines.addAll(Arrays.asList(general));
    		lines.add(" ");
    		lines.addAll(Arrays.asList(generation));
    		lines.add(" ");
    		lines.addAll(Arrays.asList(maintenance));
    		
    	} else {
    		lines.addAll(Arrays.asList(general));
    	}
    	
    	m_botAction.smartPrivateMessageSpam(sender, lines.toArray(new String[lines.size()]));
    	
    }
    
    private void handleCommand(String sender, String command, int messageType) {
    	
    	boolean operator = operators.contains(sender.toLowerCase());
    	boolean smod = m_botAction.getOperatorList().isSmod(sender);

    	try {
    	
	    	if (command.equals("!help")) {
	    		handleHelpCommand(sender);
	    		
	    	} else  if (command.startsWith("!coupon")) {
	    		doCmdCoupon(sender, command.substring(8).trim());
	    	
	    	} else if (operator || smod) {
	    		
	    		// OPERATORS functions
	    		if (command.startsWith("!create ")) {
	    			doCmdCreate(sender, command.substring(8).trim());
	    		} else if (command.startsWith("!limituse ")) {
	    			doCmdLimitUse(sender, command.substring(10).trim());
	    		} else if (command.startsWith("!expiredate ")) {
	    			doCmdExpireDate(sender, command.substring(12).trim());
	    		} else if (command.startsWith("!info ")) {
	    			doCmdInfo(sender, command.substring(6).trim());
	    		} else if (command.startsWith("!users ")) {
	    			doCmdUsers(sender, command.substring(7).trim());
	    		} else if (command.startsWith("!enable ")) {
	    			doCmdEnable(sender, command.substring(8).trim());
	    		} else if (command.startsWith("!disable ")) {
	    			doCmdDisable(sender, command.substring(9).trim());
	    		}
	    		
	    		// SMOD functions
	    		if (smod && command.startsWith("!addop ")) {
	    			doCmdAddOp(sender, command.substring(7).trim());
	    		} else if (smod && command.equals("!listops")) {
	    			doCmdListOps(sender);
	    		} else if (smod && command.startsWith("!go ")) {
	    			doCmdGo(sender, command.substring(4).trim());
	    		} else if (smod && command.equals("!die")) {
	    			m_botAction.die();
	    		}
	    		
	    	}
    	
    	} catch (Exception e) {
    		Tools.printStackTrace(e);
    	}
    	
    }
    
    private void doCmdGo(String sender, String command) {
		m_botAction.changeArena(command);
	}

	private void doCmdListOps(String sender) {
		
		List<String> lines = new ArrayList<String>();
		lines.add("List of Operators:");
		for(String name: operators) {
			lines.add("- " + name);
		}
		m_botAction.smartPrivateMessageSpam(sender, lines.toArray(new String[lines.size()]));
	}

	private void doCmdAddOp(String sender, String name) {
		
		if (!operators.contains(name.toLowerCase())) {
			operators.add(name.toLowerCase());
			m_botAction.sendSmartPrivateMessage(sender, name + " is now an operator (temporary until the bot respawn).");
			
			/*
			String operatorsString = "";
			for(String operator: operators) {
				operatorsString += "," + operator;
			}

			m_botAction.getBotSettings().put("Operators", operatorsString.substring(1));
			m_botAction.getBotSettings().save();
			*/
			
		} else {
			m_botAction.sendSmartPrivateMessage(sender, name + " is already an operator.");
		}
	}

	private void doCmdDisable(String sender, String codeString) {
		
		CouponCode code = getCode(codeString);
		if (code == null) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' not found.");
		}
		else if (!code.isEnabled()) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' is already disabled.");
		} 
		else if (!code.isValid()) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' is not valid anymore, useless.");
		} 
		else {
			code.setEnabled(false);
			if (updateDB(code)) {
				m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' updated.");
			} else {
				m_botAction.sendSmartPrivateMessage(sender, "A problem has occured while updating the code '" + codeString + "'.");
			}
		}
		
	}

	private void doCmdEnable(String sender, String codeString) {
		
		CouponCode code = getCode(codeString);
		if (code == null) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' not found.");
		}
		else if (code.isEnabled()) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' is already enabled.");
		} 
		else if (!code.isValid()) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' is not valid anymore, useless.");
		} 
		else {
			code.setEnabled(true);
			if (updateDB(code)) {
				m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' updated.");
			} else {
				m_botAction.sendSmartPrivateMessage(sender, "A problem has occured while updating the code '" + codeString + "'.");
			}
		}
	}

	private void doCmdInfo(String sender, String codeString) {
		
		CouponCode code = getCode(codeString);
		if (code == null) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' not found.");
		}
		else {
	    	String generation[] = new String[] {
        		"Code: " + codeString.toUpperCase() + "  (Generated by " + code.getCreatedBy() + ", " + new SimpleDateFormat("yyyy-MM-dd").format(code.getCreatedAt()) + ")",
        		" - Valid: " + (code.isValid()? "Yes" : "No (Reason: " + code.getInvalidReason() + ")"),
        		" - Money: $" + code.getMoney(),
        		" - " + (code.getUsed() > 0 ? "Used: " + code.getUsed() + " time(s)" : "Not used yet"),
        		"[Limitation]",
        		" - Maximum of use: " + code.getMaxUsed(),
        		" - Start date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(code.getStartAt()),
        		" - Expiration date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(code.getEndAt()),
        	};
	    	
	    	m_botAction.smartPrivateMessageSpam(sender, generation);
		}
	}
	
	private void doCmdUsers(String sender, String codeString) {
		
		CouponCode code = getCode(codeString);
		if (code == null) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' not found.");
		}
		else {
	    	
			try {
				
				ResultSet rs = m_botAction.SQLQuery(database, "SELECT * FROM tblMoneyCodeUsed WHERE fnMoneyCodeId = '" + code.getId() + "'");

				int count = 0;
				while (rs.next()) {
					count++;
					String name = count + ". " + rs.getString("fcName");
					String date = new SimpleDateFormat("yyyy-MM-dd").format(rs.getDate("fdCreated"));
					String message = Tools.formatString(name, 23, " ");
					message += " " + date;
					m_botAction.sendSmartPrivateMessage(sender, message);
				}
				rs.close();

				if (count == 0) {
					m_botAction.sendSmartPrivateMessage(sender, "This code has not been used yet.");
				}
				
			} catch (SQLException e) {
				Tools.printStackTrace(e);
				m_botAction.sendSmartPrivateMessage(sender, "An error has occured.");
			}

		}
	}

	private void doCmdExpireDate(String sender, String command) {
		
		String[] pieces = command.split(":");
		if (pieces.length != 2) {
			m_botAction.sendSmartPrivateMessage(sender, "Bad argument");
			return;
		}
		
		String codeString = pieces[0];
		String dateString = pieces[1];
		
		CouponCode code = getCode(codeString);
		if (code == null) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' not found.");
		}
		else {
	    	
			DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
			java.util.Date date;
			try {
				date = df.parse(dateString);
			} catch (ParseException e) {
				m_botAction.sendSmartPrivateMessage(sender, "Bad date");
				return;
			}  
			
			code.setEndAt(date);
			if (updateDB(code)) {
				m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' updated.");
			} else {
				m_botAction.sendSmartPrivateMessage(sender, "A problem has occured while updating the code '" + codeString + "'.");
			}
			
		}
	}

	private void doCmdLimitUse(String sender, String command) {
		
		String[] pieces = command.split(":");
		if (pieces.length != 2) {
			m_botAction.sendSmartPrivateMessage(sender, "Bad argument");
			return;
		}
		
		String codeString = pieces[0];
		String limitString = pieces[1];
		int limit;
		try {
			limit = Integer.valueOf(limitString);
		} catch (NumberFormatException e) {
			m_botAction.sendSmartPrivateMessage(sender, "Bad number");
			return;
		}
		
		CouponCode code = getCode(codeString);
		if (code == null) {
			m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' not found.");
		}
		else {
			if (limit > 0) {
				code.setMaxUsed(limit);
				if (updateDB(code)) {
					m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' updated.");
				} else {
					m_botAction.sendSmartPrivateMessage(sender, "A problem has occured while updating the code '" + codeString + "'.");
				}
			} else {
				m_botAction.sendSmartPrivateMessage(sender, "Must be a number higher than 0.");
			}
			
		}
	}

	private void doCmdCreate(String sender, String command) {
		
		String[] pieces = command.split("\\s*:\\s*");
		
		// Automatic code
		if (pieces.length == 1) {
		
			int money;
			try {
				money = Integer.parseInt(pieces[0]);
			} catch (NumberFormatException e) {
				m_botAction.sendSmartPrivateMessage(sender, "Bad number.");
				return;
			}
			
			String codeString = null;
			
			while (codeString == null || getCode(codeString) != null) {
				// Genereate a random code using the date and md5
				String s = (new java.util.Date()).toString();
				MessageDigest m;
				try {
					m = MessageDigest.getInstance("MD5");
					m.update(s.getBytes(), 0, s.length());
					String codeTemp = new BigInteger(1, m.digest()).toString(16);
					
					if (getCode(codeTemp) == null)
						codeString = codeTemp.substring(0,8).toUpperCase();
					
				} catch (NoSuchAlgorithmException e) {
					return;
				}
			}
			
			CouponCode code = new CouponCode(codeString, money, sender);
			if (insertDB(code)) {
				m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' created.");
			} else {
				m_botAction.sendSmartPrivateMessage(sender, "A problem has occured while creating the code '" + codeString + "'.");
			}
			
		// Custom code
		} else if (pieces.length == 2) {
			
			String codeString = pieces[0];
			
			int money;
			try {
				money = Integer.parseInt(pieces[1]);
			} catch (NumberFormatException e) {
				m_botAction.sendSmartPrivateMessage(sender, "Bad number.");
				return;
			}
			
			if (getCode(codeString) != null) {
				m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' already exists.");
				return;
			}
			
			CouponCode code = new CouponCode(codeString, money, sender);
			if (insertDB(code)) {
				m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' created.");
			} else {
				m_botAction.sendSmartPrivateMessage(sender, "A problem has occured while creating the code '" + codeString + "'.");
			}
			
		} else {
			m_botAction.sendSmartPrivateMessage(sender, "Bad argument.");
			return;
		}
	}

	private void doCmdCoupon(String sender, String codeString) {

		CouponCode code = getCode(codeString);
		if (code == null) {
			// no feedback to avoid bruteforce!!
			// m_botAction.sendSmartPrivateMessage(sender, "Code '" + codeString + "' not found.");
		} 
		else if (isPlayerRedeemAlready(sender, code)) {
			m_botAction.sendSmartPrivateMessage(sender, "You have already used this code.");
			return;
		} 
		else if (!code.isValid()) {
			// no feedback to avoid bruteforce!!
			return;
		}
		else {

			// We send an IPC Message to pubsystem
			// pubsystem need to answer back with a success or error
			// if success, we send a PM to the player and update the DB
			sendIPCMoney(sender, code);
			
		}
	}
	
	private boolean isPlayerRedeemAlready(String playerName, CouponCode code) {
		
		ResultSet rs;
		try {
			rs = m_botAction.SQLQuery(database, "SELECT * FROM tblMoneyCodeUsed WHERE fnMoneyCodeId = '" + code.getId() + "' AND fcName = '" + Tools.addSlashes(playerName) + "'");
			if (rs.first()) {
				rs.close();
				return true;
			} else {
				rs.close();
				return false;
			}
			
		} catch (SQLException e) {
			Tools.printStackTrace(e);
			return false;
		}
		
	}
	
	private boolean insertDB(CouponCode code) {
		
		String startAtString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(code.getStartAt());
		String endAtString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(code.getEndAt());
		
		try {
			m_botAction.SQLQueryAndClose(database,
					"INSERT INTO tblMoneyCode " +
					"(fcCode, fcDescription, fnMoney, fcCreatedBy, fnUsed, fnMaxUsed, fdStartAt, fdEndAt, fbEnabled, fdCreated) " +
					"VALUES (" +
					"'" + code.getCode() + "'," +
					"'" + Tools.addSlashes(code.getDescription()) + "'," +
					"'" + code.getMoney() + "'," +
					"'" + Tools.addSlashes(code.getCreatedBy()) + "'," +
					"'" + code.getUsed() + "'," +
					"'" + code.getMaxUsed() + "'," +
					"'" + startAtString + "'," +
					"'" + endAtString + "'," +
					"" + (code.isEnabled() ? 1 : 0) + "," +
					"NOW()" +
					")");
			
		} catch (SQLException e) {
			Tools.printStackTrace(e);
			return false;
		}
		return true;
		
	}
	
	private boolean updateDB(CouponCode code) {
		
		String startAtString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(code.getStartAt());
		String endAtString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(code.getEndAt());
		
		try {
			m_botAction.SQLQueryAndClose(database,
					"UPDATE tblMoneyCode SET " +
					"fcDescription = '" + Tools.addSlashes(code.getDescription()) + "', " +
					"fnUsed = " + code.getUsed() + ", " +
					"fnMaxUsed = " + code.getMaxUsed() + ", " +
					"fdStartAt = '" + startAtString + "', " +
					"fdEndAt = '" + endAtString + "', " +
					"fbEnabled = " + (code.isEnabled() ? 1 : 0) + " " +
					"WHERE fnMoneyCodeId='" + code.getId() + "'");
			
		} catch (SQLException e) {
			Tools.printStackTrace(e);
			return false;
		}
		return true;
		
	}

	private CouponCode getCode(String codeString) {
    	
		try {
			
			ResultSet rs = m_botAction.SQLQuery(database, "SELECT * FROM tblMoneyCode WHERE fcCode = '" + Tools.addSlashes(codeString) + "'");

			if (rs.next()) {
	
				int id = rs.getInt("fnMoneyCodeId");
				String description = rs.getString("fcDescription");
				String createdBy = rs.getString("fcCreatedBy");
				int money = rs.getInt("fnMoney");
				int used = rs.getInt("fnUsed");
				int maxUsed = rs.getInt("fnMaxUsed");
				boolean enabled = rs.getBoolean("fbEnabled");
				Date startAt = rs.getDate("fdStartAt");
				Date endAt = rs.getDate("fdEndAt");
				Date createdAt = rs.getDate("fdCreated");
				
				CouponCode code = new CouponCode(codeString, money, createdAt, createdBy);
				code.setId(id);
				code.setDescription(description);
				code.setEnabled(enabled);
				code.setMaxUsed(maxUsed);
				code.setStartAt(startAt);
				code.setEndAt(endAt);
				code.setUsed(used);
				rs.close();
				return code;
			}
			else {
				rs.close();
				return null;
			}
			
		} catch (SQLException e) {
			Tools.printStackTrace(e);
			return null;
		}

    	
    }
    
    private void updateHistoryDB(String playerName, CouponCode code) {

		// Record this endorsement
		try {
			m_botAction.SQLQueryAndClose(database, "INSERT INTO tblMoneyCodeUsed "
					+ "(fnMoneyCodeId, fcName, fdCreated) "
					+ "VALUES ('" + code.getId() + "', '" + Tools.addSlashes(playerName) + "', NOW())");
		} catch (SQLException e) {
			Tools.printStackTrace(e);
		}
				
    }
    
    private void sendIPCMoney(String playerName, CouponCode code) {
    	
    	// Protocol>   coupon:<code>:<name>:<money>
    	m_botAction.sendSmartPrivateMessage(playerName, "Please wait..");
    	String message = "coupon:" + code.getCode() + ":" + playerName + ":" + code.getMoney();
    	m_botAction.ipcSendMessage(IPC_CHANNEL, message, "pubsystem", "couponbot");

    }
    
    private String getSender(Message event)
    {
        if(event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE)
            return event.getMessager();
        return m_botAction.getPlayerName(event.getPlayerID());
    }
    
    public void handleDisconnect() {
    	m_botAction.ipcUnSubscribe(IPC_CHANNEL);
    }

}
