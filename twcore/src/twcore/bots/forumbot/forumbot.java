package twcore.bots.forumbot;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.OperatorList;
import twcore.core.SubspaceBot;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.util.Tools;

/**
 * ForumBot is a bot that can !activate a forum account. 
 * If a player has the same nickname as his forum account, he can activate it using a simple !activate. 
 * If he's on a different nickname, he can specify the name and password to activate his forum account.
 * Furthermore it's possible to retrieve information on a specific forum account, even by Forum Administrators (after logging in).
 * 
 * @author Maverick
 */
public class forumbot extends SubspaceBot {
    private BotSettings m_botSettings;
    private BotAction m_botAction;
    private OperatorList m_opList;
    
    // Constants
    private static final String DATABASE = "forums";
    private static final String UNIQUECONNECTIONID = "forumbot";
    private static final int USERGROUP_REGISTERED = 2;
    private static final int USERGROUP_ADMINISTRATORS = 6;
    private static final int USERGROUP_AWAITING_EMAIL_CONFIRMATION = 3;
    private static final int USERGROUP_AWAITING_MODERATION_USERGROUP_ID = 4;
    
    
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int MAX_LOGIN_ATTEMPT_EXPIRE = 15; // minutes
    
    // Prepared Statements
    private PreparedStatement psUserGroupID, psActivateUser, psInfoUser, psValidateUser;
    
    // Keeps track of login attempts
    private HashMap<String, String> loginAttempts = new HashMap<String, String>();
    //             <username, amount of login attempts + ":" + currentTimeMillis>
    
    // Keeps track of logged in forum administrators
    private ArrayList<String> forumAdmins = new ArrayList<String>();

    private CleanLoginAttempts cleanLoginAttemptsTask = new CleanLoginAttempts();
    
    public forumbot(BotAction botAction) {
        super(botAction);
        m_botAction = BotAction.getBotAction();
        m_opList = m_botAction.getOperatorList();
        m_botSettings = m_botAction.getBotSettings();
        m_botAction.getEventRequester().request( EventRequester.MESSAGE );
        
        // Prepared statements
        psUserGroupID = m_botAction.createPreparedStatement(DATABASE, UNIQUECONNECTIONID, "SELECT usergroupid FROM user WHERE username = ? LIMIT 0,1");
        psActivateUser = m_botAction.createPreparedStatement(DATABASE, UNIQUECONNECTIONID, "UPDATE user SET usergroupid = ? WHERE username = ?");
        psInfoUser = m_botAction.createPreparedStatement(DATABASE, UNIQUECONNECTIONID, "SELECT u.userid, ug.title, u.username, u.email, u.posts, u.birthday, u.ipaddress FROM user u, usergroup ug WHERE u.usergroupid = ug.usergroupid AND u.username = ?");
        psValidateUser = m_botAction.createPreparedStatement(DATABASE, UNIQUECONNECTIONID, "SELECT username, password, salt FROM user WHERE username = ?");
        
        // Schedule the timertask to clear out the expired login attempts
        m_botAction.scheduleTaskAtFixedRate(cleanLoginAttemptsTask, 10 * Tools.TimeInMillis.MINUTE, Tools.TimeInMillis.MINUTE);
    }


    public void handleEvent(Message event) {
        int messageType = event.getMessageType();
        
        // Handle !commands
        if((Message.PRIVATE_MESSAGE == messageType || Message.REMOTE_PRIVATE_MESSAGE == messageType) && event.getMessage() != null) {
            String name = event.getMessager()==null ? m_botAction.getPlayerName(event.getPlayerID()) : event.getMessager();
            String message = event.getMessage().toLowerCase();
            boolean isModerator = m_opList.isModerator(name);
            boolean isForumAdmin = forumAdmins.contains(name);
            boolean isSMod = m_opList.isSmod(name);
            
            if(message.startsWith("!activate")) {
                cmdActivate(name, message);
            } else if(message.startsWith("!login")) {
                cmdLogin(name, message);
            } else if(message.startsWith("!forceactivate") && (isModerator || isForumAdmin)) {
                cmdForceActivate(name, message);
            } else if(message.startsWith("!info") && (isModerator || isForumAdmin)) {
                cmdInfo(name, message);
            } else if(message.startsWith("!go") && isSMod) {
                cmdGo(name, message);
            } else if(message.startsWith("!die") && isSMod) {
                cmdDie(name, message);
            } else {
                cmdHelp(name, message);
            }
        }
    }


    public void handleEvent(LoggedOn event) {
        if(     psUserGroupID == null ||
                psActivateUser == null ||
                psInfoUser == null ||
                psValidateUser == null) {
            Tools.printLog("forumbot: One or more PreparedStatements are null! Disconnecting bot.");
            m_botAction.die("Error while initializing PreparedStatements");
        }
        
        m_botAction.joinArena(m_botSettings.getString("arena"));
    }
    
    /*****************************************************************************
     *                          COMMAND HANDLERS
     *****************************************************************************/
    private void cmdHelp(String name, String message) {
    	// Public commands
    	String[] startCommands = 
    	{   "+-------------------------------------------------------------------------------+",
    		"|                                 FORUMBOT                                      |",  
    	    "|                                                                               |",
    	    "| I'm a bot that can active your forum account at http://forums.trenchwars.org. |",
    	    "| Please look below for the available commands.                                 |"
        };
    	String[] publicCommands = 
    	{	"|                                                                               |",
    		"| !activate                   - Activates your registered name that matches     |",
    		"|                               your current nickname on the forum.             |",
    		"| !activate <name>:<password> - Activates <name>. <password> is only used for   |",
    		"|                               verification that you registered the <name>.    |",
    		"| !login <name>:<password>    - Login for forum administrators only             |" };
    	String[] modCommands = 
    	{   "|-----------------------   MOD+ / Forum Administrator   ------------------------|",
    	    "| !forceactivate <name>       - Activates the given <name>. This command should |",
    	    "|                               be used with caution and only in rare situations|",
    	    "| !info <name>                - Displays information about forum user           |",
    	    };
    	String[] smodCommands = 
    	{   "|-------------------------------   SMOD+   -------------------------------------|",
    	    "| !go <arena>                 - Moves ForumBot to <arena>                       |",
    	    "| !die                        - Removes ForumBot from the zone                  |"    };
    	String[] endCommands =
    	{   "\\-------------------------------------------------------------------------------/"   };
    	
    	
    	m_botAction.smartPrivateMessageSpam(name, startCommands);
   	    m_botAction.smartPrivateMessageSpam(name, publicCommands);
    	
    	if(m_opList.isModerator(name) || forumAdmins.contains(name)) // MOD+ / Forum Administrators
    		m_botAction.smartPrivateMessageSpam(name, modCommands);
    	
    	if(m_opList.isSmod(name))  // SMOD+
    		m_botAction.smartPrivateMessageSpam(name, smodCommands);
    	
    	m_botAction.smartPrivateMessageSpam(name, endCommands);
    }
    
    private void cmdActivate(String name, String message) {
        message = message.substring(9);

        // !activate
        if(message.length() == 0) {
            activateForumAccount(name, name);
        } 
        
        // !activate <name>:<password>
        else {
            // First lets get the two parameters
            String user, password;
            
            if(!message.contains(":") || message.startsWith(":") || message.endsWith(":")) {
                m_botAction.sendSmartPrivateMessage(name, "Syntax error. Please specify the name and password; ::!activate <name>:<password>");
                return;
            }
            
            // Get the username & password from the parameters
            String[] pieces = message.split(":");
            user = pieces[0];
            password = pieces[1];
            
            // Check if the user is blocked from logging in
            if(loginAttempts.containsKey(name)) {
                int amount = Integer.parseInt(loginAttempts.get(name).split(":")[0]);
                
                if(amount >= 3) {
                    m_botAction.sendSmartPrivateMessage(name, "You've tried to activate your forum account using an incorrect username / password too many times.");
                    m_botAction.sendSmartPrivateMessage(name, "You can try again in "+MAX_LOGIN_ATTEMPT_EXPIRE+" minutes, starting now.");
                    return;
                }
            }
            
            // Check if the username & password is correct
            if(!checkForumUsernamePassword(user, password)) {
                // Invalid username & password
                
                // Check amount of login attempts
                if(loginAttempts.containsKey(name)) {
                    int amount = Integer.parseInt(loginAttempts.get(name).split(":")[0]);
                    amount++;
                    loginAttempts.put(name, amount + ":" + System.currentTimeMillis());
                } else {
                    loginAttempts.put(name, "1:" + System.currentTimeMillis());
                }
                
                // message user
                m_botAction.sendSmartPrivateMessage(name, "Invalid username / password combination. Please try again. (attempt "+loginAttempts.get(name).split(":")[0]+" of "+MAX_LOGIN_ATTEMPTS+")");
                m_botAction.sendSmartPrivateMessage(name, "Have you forgotten your username or password? Visit http://forums.trenchwars.org/login.php?do=lostpw .");
                
            } else {
                // Valid username & password
                m_botAction.sendSmartPrivateMessage(name, "Username / password combination successfully validated.");
                activateForumAccount(user, name);
            }
        }
    }
    
    private void cmdLogin(String name, String message) {
        message = message.substring(7);
        
        String user, password;
        
        if(message.length() == 0 || !message.contains(":") || message.startsWith(":") || message.endsWith(":")) {
            m_botAction.sendSmartPrivateMessage(name, "Syntax error. Please specify the name and password; ::!login <name>:<password>");
            return;
        }
        
        // Get the username & password from the parameters
        String[] pieces = message.split(":");
        user = pieces[0];
        password = pieces[1];
        
        // Check if the user is blocked from logging in
        if(loginAttempts.containsKey(name)) {
            int amount = Integer.parseInt(loginAttempts.get(name).split(":")[0]);
            
            if(amount >= 3) {
                m_botAction.sendSmartPrivateMessage(name, "You've tried to activate your forum account using an incorrect username / password too many times.");
                m_botAction.sendSmartPrivateMessage(name, "You can try again in "+MAX_LOGIN_ATTEMPT_EXPIRE+" minutes, starting now.");
                return;
            }
        }
        
        if(!checkForumUsernamePassword(user, password)) {
            // Invalid username & password
            
            // Check amount of login attempts
            if(loginAttempts.containsKey(name)) {
                int amount = Integer.parseInt(loginAttempts.get(name).split(":")[0]);
                amount++;
                loginAttempts.put(name, amount + ":" + System.currentTimeMillis());
            } else {
                loginAttempts.put(name, "1:" + System.currentTimeMillis());
            }
            
            // message user
            m_botAction.sendSmartPrivateMessage(name, "Invalid username / password combination. Please try again. (attempt "+loginAttempts.get(name).split(":")[0]+" of "+MAX_LOGIN_ATTEMPTS+")");
            m_botAction.sendSmartPrivateMessage(name, "Have you forgotten your username or password? Visit http://forums.trenchwars.org/login.php?do=lostpw .");
            
        } else {
            // Valid username & password
            
            // Check if the user is in the correct user group
            int usergroupid = 0;
            try {
                psUserGroupID.setString(1, user);
                ResultSet rsUserGroupID = psUserGroupID.executeQuery();
                if(rsUserGroupID.next()) {
                    usergroupid = rsUserGroupID.getInt(1);
                } else {
                    m_botAction.sendSmartPrivateMessage(name, "I was unable to check your forum access level. Login failed.");
                    return;
                }
            } catch(SQLException sqle) {
                Tools.printLog("SQLException encountered while checking the usergroup id of forum user '"+user+"': "+sqle.getMessage());
                m_botAction.sendSmartPrivateMessage(name, "Oops! Something went wrong while checking your forum access level! Please try again.");
                m_botAction.sendSmartPrivateMessage(name, "If you still get this error, please contact a staff member with the following error message:");
                m_botAction.sendSmartPrivateMessage(name, sqle.getMessage());
            }
            
            if(usergroupid == USERGROUP_ADMINISTRATORS) {
                forumAdmins.add(name);
                m_botAction.sendSmartPrivateMessage(name, "Successfully logged in.");
            } else {
                m_botAction.sendSmartPrivateMessage(name, "You're not a forum administrator. The !login command is only meant for forum administrators.");
            }
            
        }
    }
    
    private void cmdForceActivate(String name, String message) {
        String user = message.substring(15);
        
        if(user.length() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "Syntax error. Please specify a forum username;  ::!forceactive <name>");
            return;
        }
        
        m_botAction.sendSmartPrivateMessage(name, "Attempting to activate forum account '"+user+"'... All messages directed to 'you' are meant for the forum account owner.");
        activateForumAccount(user, name);
    }
    
    private void cmdInfo(String name, String message) {
        String user = message.substring(6);
        
        if(user.length() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "Syntax error. Please specify a forum username;  ::!info <name>");
            return;
        }
        
        m_botAction.sendSmartPrivateMessage(name, "Retrieving forum account information of user '"+user+"'...");
        boolean isModerator = m_opList.isModerator(name);
        boolean isSModerator = m_opList.isSmod(name);
        boolean isForumAdmin = forumAdmins.contains(name);
        
        try {
            // SELECT u.userid, ug.title, u.username, u.email, u.posts, u.birthday, u.ipaddress 
            // FROM user u, usergroup ug 
            // WHERE u.usergroupid = ug.usergroupid AND u.username = ?
            psInfoUser.setString(1, user);
            ResultSet rsInfoUser = psInfoUser.executeQuery();
            if(rsInfoUser.next()) {
                m_botAction.sendSmartPrivateMessage(name, "Username [id]: "+rsInfoUser.getString("username")+" ["+rsInfoUser.getInt("userid")+"]");
                m_botAction.sendSmartPrivateMessage(name, "Usergroup:     "+rsInfoUser.getString("title"));
                // smod + forum admin
                m_botAction.sendSmartPrivateMessage(name, "E-mail:        "+((isSModerator || isForumAdmin) ? rsInfoUser.getString("email") : "[hidden]"));
                m_botAction.sendSmartPrivateMessage(name, "Nr. of posts:  "+rsInfoUser.getInt("posts"));
                // mod + forum admin
                m_botAction.sendSmartPrivateMessage(name, "Birthdate:     "+((isModerator || isForumAdmin) ? rsInfoUser.getString("birthday") : "[hidden]"));
                // smod + forum admin
                m_botAction.sendSmartPrivateMessage(name, "IP:            "+((isSModerator || isForumAdmin) ? rsInfoUser.getString("ipaddress") : "[hidden]"));
            } else {
                m_botAction.sendSmartPrivateMessage(name, "The specified forum name was not found.");
            }
        } catch(SQLException sqle) {
            Tools.printLog("SQLException encountered while retrieving info of forum user '"+user+"': "+sqle.getMessage());
            m_botAction.sendSmartPrivateMessage(name, "Oops! Something went wrong while retrieving info of the specified user. Please try again.");
            m_botAction.sendSmartPrivateMessage(name, "If you still get this error, please contact a staff member with the following error message:");
            m_botAction.sendSmartPrivateMessage(name, sqle.getMessage());
        }
    }
    
    private void cmdGo(String name, String message) {
        String arena = message.substring(4);
        
        if(arena.length() == 0) {
            m_botAction.sendSmartPrivateMessage(name, "Syntax error. Please specify an arena name;  ::!go <arena>");
            return;
        }
        if(Tools.isAllDigits(arena)) {
            m_botAction.sendSmartPrivateMessage(name, "Syntax error. Please specify a non-numeric arena name (sending the bot to public arenas is not allowed).   ::!go <arena>");
            return;
        }
        m_botAction.changeArena(arena);
        m_botAction.sendSmartPrivateMessage(name, m_botAction.getBotName() + " moving to '"+arena+"'...");
    }
    
    private void cmdDie(String name, String message) {
        if(m_opList.isSmod(name)) {
            m_botAction.cancelTask(cleanLoginAttemptsTask);
            m_botAction.closePreparedStatement(DATABASE, UNIQUECONNECTIONID, psUserGroupID);
            m_botAction.closePreparedStatement(DATABASE, UNIQUECONNECTIONID, psActivateUser);
            m_botAction.closePreparedStatement(DATABASE, UNIQUECONNECTIONID, psInfoUser);
            m_botAction.closePreparedStatement(DATABASE, UNIQUECONNECTIONID, psValidateUser);
            m_botAction.die();
        }
    }
    
    
    private boolean checkForumUsernamePassword(String name, String password) {
        // Passwords are stored in vBulletin forum's database as:
        // md5(md5(PASSWORD) + salt)
        
        try {
            psValidateUser.setString(1, name);
            ResultSet rsValidateUser = psValidateUser.executeQuery();
            if(rsValidateUser.next()) {
                String hashedPassword = rsValidateUser.getString("password");
                String salt = rsValidateUser.getString("salt");
                
                // Encrypt the plain-text password to the correct MD5 format
                String hash = md5(md5(password) + salt);
                
                if(hash.equals(hashedPassword)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // user not found
                return false;
            }
        } catch(SQLException sqle) {
            Tools.printLog("SQLException occured while validating user/password, user '"+name+"': "+sqle.getMessage());
        }
        
        return false;
    }
    
    /**
     * Activates the forum account (attempts to)
     * 
     * @param name
     */
    private void activateForumAccount(String name, String messager) {
        int usergroupID = 0;
        
        try {
            psUserGroupID.setString(1, name);
            ResultSet rsUserGroupID = psUserGroupID.executeQuery();
            
            // Get the current usergroup ID of the user and check if the user forum account exists
            if(rsUserGroupID.next()) {
                usergroupID = rsUserGroupID.getInt(1);
            } else {
                m_botAction.sendSmartPrivateMessage(messager, "I'm sorry, your forum account wasn't found. Have you registered?");
                m_botAction.sendSmartPrivateMessage(messager, "If you tried to !activate but your forum name doesn't match your current nickname, please use !activate <name>:<password>.");
                return;
            }
            
            // Check if the usergroup ID of the user is the correct usergroup after registration
            if(usergroupID == USERGROUP_AWAITING_EMAIL_CONFIRMATION) {
                m_botAction.sendSmartPrivateMessage(messager, "Your forum account needs to be activated by the link that's sent to you by e-mail.");
                m_botAction.sendSmartPrivateMessage(messager, "If you haven't received an e-mail, visit http://forums.trenchwars.org/login.php?do=emailpassword or contact a forum admin.");
            }
            else if(usergroupID != USERGROUP_AWAITING_MODERATION_USERGROUP_ID) {
                m_botAction.sendSmartPrivateMessage(messager, "Your forum account has already been activated. If you have a problem with your forum account, please contact a forum admin.");
            } else {
                // Activate the user
                psActivateUser.setInt(1, USERGROUP_REGISTERED);
                psActivateUser.setString(2, name);
                psActivateUser.execute();
                
                // Start the check if the usergroup ID has the expected value after the change (activation)
                psUserGroupID.setString(1, name);
                ResultSet rsCheckUserGroupID = psUserGroupID.executeQuery();
                if(rsCheckUserGroupID.next()) {
                    
                    if(rsCheckUserGroupID.getInt(1) == USERGROUP_REGISTERED) {
                        m_botAction.sendSmartPrivateMessage(messager, "Your forum account has been activated!");
                        m_botAction.sendSmartPrivateMessage(messager, "Visit http://forums.trenchwars.org and have fun posting!");
                    } else {
                        m_botAction.sendSmartPrivateMessage(messager, "The activation change didn't yield the expected result afterwards.");
                        m_botAction.sendSmartPrivateMessage(messager, "Please try again or contact a staff member / forum admin if this problem is repeated.");
                    }
                } else {
                    // Throwing SQLException so the catch() part below catches it and handles it properly
                    throw new SQLException("Check of userGroup change is correct FAILED");
                }
            }
            
            // Clean up
            rsUserGroupID.close();
            psUserGroupID.clearParameters();
            psActivateUser.clearParameters();
            
        } catch(SQLException sqle) {
            Tools.printLog("SQLException encountered on activating forum account of '"+name+"': "+sqle.getMessage());
            m_botAction.sendSmartPrivateMessage(messager, "Oops! Something went wrong while working on your activation! Please try again.");
            m_botAction.sendSmartPrivateMessage(messager, "If you still get this error, please contact a staff member with the following error message:");
            m_botAction.sendSmartPrivateMessage(messager, sqle.getMessage());
        }
    }
    
    /**
     * @param str
     * @return a MD5 hashed string
     */
    private String md5(String str)  {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            
            BigInteger bigInt = new BigInteger(1, md5.digest());
            String hashed = bigInt.toString(16);
            
            // Leading zeroes
            while (hashed.length() < 32) 
                hashed = "0" + hashed;
            
            return hashed;
            
        } catch(NoSuchAlgorithmException nsae) {
            Tools.printLog("NoSuchAlgorithmException thrown while hashing string in forumbot.md5(): "+nsae.getMessage());
            return null;
        }
    }
    
    
    /**
     * This TimerTask checks if there are loginAttempts registered that  
     * @author Maverick
     */
    class CleanLoginAttempts extends TimerTask {
        public void run() {
            Iterator<String> iter = loginAttempts.values().iterator();
            
            while(iter.hasNext()) {
                String amount = iter.next();
                long time = Long.parseLong(amount.split(":")[1]);
                long diff = System.currentTimeMillis() - time;
                
                if(diff > (Tools.TimeInMillis.MINUTE*MAX_LOGIN_ATTEMPT_EXPIRE)) {
                    iter.remove();
                }
            }
        }
    }
    
}