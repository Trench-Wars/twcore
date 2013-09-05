package twcore.bots.twpoll;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.Vector;

import javax.print.PrintException;

import twcore.core.BotAction;
import twcore.core.BotSettings;
import twcore.core.EventRequester;
import twcore.core.SubspaceBot;
import twcore.core.events.ArenaJoined;
import twcore.core.events.ArenaList;
import twcore.core.events.LoggedOn;
import twcore.core.events.Message;
import twcore.core.events.PlayerEntered;
import twcore.core.util.Tools;

/**
 * A DB-based poll system
 *
 * Players will get notified if there is an active poll.
 *
 * @author  Arobas+
 */
public class twpoll extends SubspaceBot {

	public static final int SPAM_INTERVAL_MINUTE = 30;

	public static final String DB_NAME = "website";

    private static final String poll = null;

	public TreeMap<Integer,Poll> polls;
	public TreeMap<Integer,Updates> updates;
	public TreeMap<Integer,TreeSet<Integer>> votes;
	public TreeMap<Integer,PlayerData> playerdata;
	public TreeMap<String,Integer> userIds;
	public TreeMap<Integer,Integer> openPolls;
	public TreeMap<Integer,Integer> lastPolls;
	public TreeMap<Integer,Long> lastSpam;
	public TreeSet<String> ignore;

	public Vector<String> players;

	public BotSettings m_botSettings;

    public twpoll(BotAction botAction) {
        super(botAction);
        requestEvents();
        m_botSettings = m_botAction.getBotSettings();
        updates = new TreeMap<Integer,Updates>();
        playerdata = new TreeMap<Integer,PlayerData>(); 
        polls = new TreeMap<Integer,Poll>();
        votes = new TreeMap<Integer,TreeSet<Integer>>();
        userIds = new TreeMap<String,Integer>();
        openPolls = new TreeMap<Integer,Integer>();
        lastPolls = new TreeMap<Integer,Integer>();
        lastSpam = new TreeMap<Integer,Long>();
        players = new Vector<String>();
        ignore = new TreeSet<String>();
    }    

    /**
     * This method requests event information from any events your bot wishes
     * to "know" about; if left commented your bot will simply ignore them.
     */
    public void requestEvents() {
        EventRequester req = m_botAction.getEventRequester();
        req.request(EventRequester.MESSAGE);
        req.request(EventRequester.ARENA_LIST);
        req.request(EventRequester.ARENA_JOINED);
        req.request(EventRequester.LOGGED_ON);
        req.request(EventRequester.PLAYER_ENTERED);
    }

    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("InitialArena"));
        m_botAction.requestArenaList();
        loadData();
    }
    
    public void loadData() {
        loadPolls();
        loadVotes();
        loadUpdates();
    }
    
    public void handleEvent(PlayerEntered event) {
        //TODO: checkNewPlayer()
        int userID = getUserID(event.getPlayerName());
        if(!playerdata.containsKey(userID))
                playerdata.put(userID, new PlayerData(userID, event.getPlayerName()));
        else {
            PlayerData p = playerdata.get(userID);
            p.sendMessage();
        }
        
		if (!players.contains(event.getPlayerName()))
			players.add(event.getPlayerName());
    }
   
    public void handleEvent(ArenaList event) {
    	String[] arenaNames = event.getArenaNames();
        Comparator <String>a = new Comparator<String>() {
            public int compare(String a, String b) {
                if (Tools.isAllDigits(a) && !a.equals("") ) {
                    if (Tools.isAllDigits(b) && !b.equals("") ) {
                        if (Integer.parseInt(a) < Integer.parseInt(b)) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        return -1;
                    }
                } else if (Tools.isAllDigits(b)) {
                    return 1;
                } else {
                    return a.compareToIgnoreCase(b);
				}
            };
        };
        Arrays.sort(arenaNames, a);
    	String arenaToJoin = arenaNames[0];// initialPub+1 if you spawn it in # arena
    	if(Tools.isAllDigits(arenaToJoin)) {
    		m_botAction.changeArena(arenaToJoin);
    	}
    }

    public void handleEvent(ArenaJoined event) {
    
    }
    
    public void handleEvent(Message event) {
        String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
        if (name == null) name = "-anonymous-";
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!die")) {
            try { Thread.sleep(50); } catch (Exception e) {};
            m_botAction.die();
        }
        String message = event.getMessage();
        
        if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE || event.getMessageType() == Message.PUBLIC_MESSAGE) {
            handleCommand(name, message);
        }
    }

    public void handleCommand(String name, String message) {
        message = message.toLowerCase();
        
           if (message.startsWith("!ignore")) { //TODO IGNORE COMMAND
               cmd_ignore(name);               
            }  else if (message.startsWith("!polls")) {
                showPollsMain(name, true);
            }  else if (message.startsWith("!updates")) {
                showUpdatesMain(name, true);
            } else if (message.startsWith("!help")) {
                //showHelp(name, 0);
                cmd_home(name);
            } else if (message.startsWith("!viewall")) {
                cmd_viewall(name);
            } else if (message.startsWith("!view ") && message.substring(6) != null) {
                cmd_view(name, message.substring(6));
            } else if (message.startsWith("!com ") && message.substring(5) != null) {
                cmd_com(name, message.substring(5));
            } else if (message.startsWith("!home")) {
                cmd_home(name);
            } else if (message.startsWith("!back")) {
                cmd_back(name);
            } else if (message.startsWith("!about")) {
                cmd_about(name);
            } else if (message.startsWith("!vote ") && message.substring(6) != null) {
                cmd_vote(name, message.substring(6));                
            }
           
           if (m_botAction.getOperatorList().isER(name)) {
               if (message.startsWith("!reload")) {
                   polls.clear();
                   updates.clear();
                   loadData();
                m_botAction.sendSmartPrivateMessage(name, "Polls and Updates locked and loaded. Lets play.");
            }
        }
    }
    
    public void cmd_about(String name) {
        m_botAction.sendSmartPrivateMessage(name, "Bot made by K A N E. Command under construction");
    }
    
    public void cmd_ignore(String name) {
        m_botAction.sendSmartPrivateMessage(name, "command under construction");
        //TODO TO DO THIS LATER
        
       /* if (ignore.contains(name)) {
            m_botAction.sendSmartPrivateMessage(name, "You are already on my ignore list.");
        } else {
            ignore.add(name);
            m_botAction.sendSmartPrivateMessage(name, "You have been added to my ignore list. I won't bother you until I reset.");
            m_botAction.sendSmartPrivateMessage(name, "HINT: Vote for each polls and you won't get notified again.");
        } */       
    }
    
    public void cmd_viewall(String name) {
        PlayerData p = playerdata.get(getUserID(name));
        int[] window = p.getWindow();
        
        if (window[0] >= 1 && window[0] <= 4) {
            showPollsMain(name, false);
        } else if (window[0] >= 5 && window[0] <= 8) {
            showUpdatesMain(name, false);
        }
    }

    public void cmd_view(String name, String message) {
        PlayerData p = playerdata.get(getUserID(name));
        int[] window = p.getWindow();
        int entryID = -1;
                
        if (Tools.isAllDigits(message))
            entryID = Integer.parseInt(message);
        else 
            return;
        
        if ((window[0] >= 1 && window[0] <= 4) && polls.containsKey(entryID)) {
            showPoll(name, entryID);
        } else if ((window[0] >= 5 && window[0] <= 8) && updates.containsKey(entryID)) {
            showUpdate(name, entryID);
        } else
            m_botAction.sendSmartPrivateMessage(name, "Invalid selection please try again");
    }
    
    public void cmd_com(String name, String message) {
        PlayerData p = playerdata.get(getUserID(name));
        int[] window = p.getWindow();
        String comment = message;
        
        if ((window[0] == 3) && polls.containsKey(window[1])) {
            p.setComments(window[1], comment, 1);
            showPoll(name, window[1]);
        } else if ((window[0] == 7) && updates.containsKey(window[1])) {
            p.setComments(window[1], comment, 2);
            showUpdate(name, window[1]);
        } 
    }
    
    public void cmd_home(String name) {
        ArrayList<String> spam = new ArrayList();
        
                spam.add(formatMessage("[Main Navigation]",0));
                spam.add(formatMessage("!polls",1) + formatMessage("Shows the active polls.",2));
                spam.add(formatMessage("!updates",1) + formatMessage("Shows active updates.",2));
                spam.add(formatMessage("!ignore",1) + formatMessage("Turns off automessages for you.",2));
                spam.add(formatMessage("!about",1) + formatMessage("Information about this bot.",2));
                spam.add(formatMessage("-",3));

        m_botAction.smartPrivateMessageSpam(name, spam.toArray(new String[spam.size()]));
    }
    
    public void cmd_back(String name) {
        PlayerData p = playerdata.get(getUserID(name));
        int[] window = p.getWindow();
        
        switch(window[0]) {
        case 3:
            showPollsMain(name,false);
            break;
        case 4:
            showPoll(name, window[1]);
            break;
        case 7:
            showUpdatesMain(name, false);
            break;
        case 8:
            showUpdate(name, window[1]);
            break;
        default:
                cmd_home(name);
                 break;
        }        
    }
    
    private void cmd_vote(String name, String message) {
        int vote = -1;
        PlayerData p = playerdata.get(getUserID(name));
        int[] window = p.getWindow();
        
        if (window[0] != 3 && window [0] != 7)
            return;
        
        if (Tools.isAllDigits(message))
            vote = Integer.parseInt(message);
        else 
            return;
        
        vote(window[1], name, vote);
    }
    
    private void loadPolls() {
        try {
            ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
                "SELECT fnPollID, fcQuestion, fbMultiSelect, fdBegin, fdEnd, fdCreated, fnUserPosterID, fcUserName, fnPollOptionID, fcOption, fnOrder " +
                "FROM tblPoll__Poll p " +
                "JOIN tblPoll__PollOptions po USING (fnPollID) " +
                "JOIN tblUser u ON p.fnUserPosterID = u.fnUserID " +
                "WHERE NOW() BETWEEN fdBegin AND fdEnd " +
                "ORDER BY fnPollID,fnOrder"
            );

            while(rs.next()) {
                int pollID = rs.getInt("fnPollID");
                if (!polls.containsKey(pollID)) {
                    polls.put(rs.getInt("fnPollID"), new Poll(rs.getInt("fnPollID"), rs.getInt("fnUserPosterID"), rs.getString("fcUserName"),
                                    rs.getString("fcQuestion"),rs.getBoolean("fbMultiSelect"), rs.getDate("fdBegin"), rs.getDate("fdEnd"), rs.getDate("fdCreated")));

                    polls.get(pollID).addOption(rs.getInt("fnPollOptionID"),rs.getString("fcOption"));
                } else {
                    polls.get(pollID).addOption(rs.getInt("fnPollOptionID"),rs.getString("fcOption"));
                }
            }
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadVotes() {

        votes = new TreeMap<Integer,TreeSet<Integer>>();

        String pollList = "";
        for(int pollId: polls.keySet()) {
            pollList += "," + pollId;
        }

        if (!pollList.isEmpty())
        {
            pollList = pollList.substring(1);
            try {
                ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
                    "SELECT pv.fnPollID, fcOption, fnUserID, pv.fnPollOptionID " +
                    "FROM tblPoll__PollVote AS pv " +
                    "JOIN tblPoll__PollOptions AS po ON pv.fnPollOptionID = po.fnPollOptionID " +
                    "WHERE pv.fnPollID IN (" + pollList + ")"
                );

                while(rs.next()) {
                    int userID = rs.getInt("fnUserID");
                    int pollID =  rs.getInt("fnPollID");
                    int optionID = rs.getInt("fnPollOptionID");
                    String option = rs.getString("fcOption");
                    Poll poll = polls.get(pollID);
                    
                    if (poll != null && !poll.pvotes.containsKey(userID)) {
                        poll.addVote(userID, optionID);
                    }
                }
                rs.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void loadUpdates() {
        try {
            ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
                "SELECT * " +
                "FROM tblPoll__Updates p " +
                "JOIN tblUser u ON p.fnPosterID = u.fnUserID " +
                "WHERE NOW() BETWEEN fdBegin AND fdEnd " +
                "ORDER BY fnUpdateID"
            );

            while(rs.next()) {
                int updateID = rs.getInt("fnUpdateID");
                if (!updates.containsKey(updateID)) {
                    updates.put(updateID, new Updates(updateID, rs.getInt("fnPosterID"), rs.getString("fcUserName"),
                                    rs.getString("fcHeader"),rs.getString("fcDetails"), rs.getDate("fdBegin"), rs.getDate("fdEnd")));
                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public int getUserID(String playerName) {

    	// Cache system
    	if (userIds.containsKey(playerName))
    		return userIds.get(playerName);

		try {
			// First, filter with UserAccount to avoid double (it happens)
			ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
		    	"SELECT fnUserID " +
		    	"FROM tblUser u " +
		    	"JOIN tblUserAccount USING (fnUserID) " +
		    	"WHERE fcUserName = '" + Tools.addSlashes(playerName) + "'"
			);

			if (rs.next()) {
				int userId = rs.getInt("fnUserID");
				userIds.put(playerName, userId);
				rs.close();
				return userId;
			}
			// If nothing, dont filter and get the last fnUserID found
			else {
				rs = m_botAction.SQLQuery(DB_NAME, "" +
			    	"SELECT fnUserID " +
			    	"FROM tblUser u " +
			    	"WHERE fcUserName = '" + Tools.addSlashes(playerName) + "'"
				);

				if (rs.last()) {
					int userId = rs.getInt("fnUserID");
					userIds.put(playerName, userId);
					rs.close();
					return userId;
				}
				else {
					rs.close();
					return 0;
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}

    }

    public int getCounts(String playerName, Boolean unread, int type) {
        //TODO count read and unread for polls and updates
        int count = 0;
        int userID = getUserID(playerName);
        PlayerData p = playerdata.get(userID);
        
        if (type == 1) {
        for(int pollId: polls.keySet()) {
                if (!unread && !p.oldPolls.isEmpty() && p.oldPolls.contains(pollId))
                    count++;
                
                if (unread && !p.oldPolls.isEmpty() && !p.oldPolls.contains(pollId)) 
                    count++;
           
                if (unread && p.oldPolls.isEmpty())
                    count++;     
        }
        } else {            
                for (int updateID: updates.keySet()) {
                    if (!unread && !p.oldUpdates.isEmpty() && p.oldUpdates.contains(updateID))
                            count++;
                    if (unread && !p.oldUpdates.contains(updateID))
                            count++;
                }
        }  
        return count;
    }
    
    /**
     * Formating the help text using a method. Do I look like a designer to you?
     * Input Types: 0 = Title, 1 = Command, 2 = Description, 3 = Footer
     * 
     * @param input
     * @param type
     * @return
     */
    private String formatMessage(String input, int type) {
        String outputString= " ";
        switch(type) {
            case 0:
                outputString = Tools.centerString(input, 50, '-'); //Header
                break;
            case 1:
                outputString = "| " + Tools.formatString(input, 13, " "); //Help Menu Left Text
                break;
            case 2:
                outputString = Tools.formatString(input, 33, " ") + " |"; //Help Menu Right Text
                break;
            case 3:
                outputString = Tools.centerString(input, 50, '-');  //Footer
                break;
            case 4:
                outputString = Tools.formatString(input, 5, " ");                
                break;
        }
        return outputString;
    }

    private void showPoll(String playerName, int pollId) {
        PlayerData p = playerdata.get(getUserID(playerName));
        p.setWindow(3, pollId);
        int userId = getUserID(playerName);
        ArrayList<String> spam = new ArrayList();
        
             	if (polls.isEmpty()) {
    		m_botAction.sendSmartPrivateMessage(playerName, "There is no poll at the moment.");
    	} else {
        Poll poll = polls.get(pollId);
                if (poll.pvotes.containsKey(userId))
                    spam.add("[Poll #" + pollId + "]" + "   Your Vote: " + poll.options.get(poll.pvotes.get(userId).getOptionID() - 1).option);
                else
                    spam.add("[Poll #" + pollId + "]");
                spam.add("(" + poll.id + ") " + poll.question);
                int i=0;
                for(PollOption option: poll.options) {
                    String pad = Tools.rightString("", ("(#" + poll.id + ") ").length(), ' ');
                    spam.add(pad + (++i) + ". " + option.option);
                }
                spam.add(" ");                
                
                if (p.isCommented(pollId,1)) {
                    spam.add("Your comments: " + p.getComment(pollId, 1));
                    spam.add(" ");
                    spam.add("To REPLACE a comment, use !com <your new comment>. To SELECT VOTE OPTION, use !vote <num>");
                } else {
                    spam.add(" ");
                    spam.add("To COMMENT on this, use !com <your comment>. To SELECT VOTE OPTION, use !vote <num>");
                }
                    spam.add("To RETURN HOME, use !home. To go BACK a menu, use !back");
                    m_botAction.smartPrivateMessageSpam(playerName, spam.toArray(new String[spam.size()]));
    	}
    }

    private void showNextPoll(String playerName) {

       	if (polls.isEmpty()) {
    		m_botAction.sendSmartPrivateMessage(playerName, "There is no poll at the moment.");
    	}
    	else {

    		ArrayList<String> spam = new ArrayList();

        	int userId = getUserID(playerName);
        	for(int pollId: polls.keySet()) {
        		Poll poll = polls.get(pollId);
        		if (!votes.containsKey(pollId) ||  (votes.containsKey(pollId) && !votes.get(pollId).contains(userId))) {
        			openPolls.put(userId, poll.id);
        			spam.add("(#" + poll.id + ") " + poll.question);
        			int i=0;
        			for(PollOption option: poll.options) {
        				String pad = Tools.rightString("", ("(#" + poll.id + ") ").length(), ' ');
        				spam.add(pad + (++i) + ". " + option.option);
        			}
        			spam.add(" ");
        			spam.add("HELP: To vote, pm me your choice. You can follow up your choice with a comment by typing <choice>:<comment>");
        			break;
        		}
        	}

        	if (spam.isEmpty()) {
        		spam.add("There is no poll for you at the moment.");
        	}

        	m_botAction.smartPrivateMessageSpam(playerName, spam.toArray(new String[spam.size()]));
    	}

    }

    private void showPollsMain(String name, Boolean showNew) {
        ArrayList<String> intro = new ArrayList<String>();
        ArrayList<String> spam = new ArrayList<String>();
        
        PlayerData p = playerdata.get(getUserID(name));
        
        if (p != null) {
            p.setWindow(2, 0);
        
    	if (polls.isEmpty()) {
    		m_botAction.sendSmartPrivateMessage(name, "There is no poll at the moment.");
    	} else {
    	     if (showNew) {
                 intro.add("[New Polls]");
                 int userId = getUserID(name);
                 int oldpolls = 0;
                 if (!p.oldPolls.isEmpty())
                     oldpolls = p.oldPolls.size(); 
                 for(int pollId: polls.keySet()) {
                     Poll poll = polls.get(pollId);
                 if (!p.oldPolls.contains(pollId)) {                
                     spam.add("(" + poll.id + ") " + poll.question);
                     p.addEntry(pollId, 1, "none");
                 }
             }                 
                 if (spam.isEmpty()) {
                     intro.clear();
                     showPollsMain(name,false);
                     return;
                 }
            spam.add(" ");
            if (oldpolls != 0)
                spam.add("[" + oldpolls + " Polls(s) not shown]");
            spam.add(" ");
            spam.add("to VOTE or COMMENT on a poll, select it. To SELECT an poll, use !view <number>.");
            spam.add("To VIEW ALL polls, use !viewall. To RETURN home, use !home");        	
        	intro.addAll(spam);
        	m_botAction.smartPrivateMessageSpam(name, intro.toArray(new String[intro.size()]));    	    
        } else {
                 intro.add("[All polls]");
                 
                 for(int pollId: polls.keySet()) {
                     Poll poll = polls.get(pollId);
                     spam.add("(" + poll.id + ") " + poll.question);
                 }
                 spam.add(" ");
                 spam.add("to VOTE or COMMENT on a poll, select it. To SELECT an poll, use !view <number>.");
                 spam.add("To RETURN home, use !home");
                 intro.addAll(spam);                
                 m_botAction.smartPrivateMessageSpam(name, intro.toArray(new String[intro.size()]));
             }
    	}
    	} else {
            playerdata.put(getUserID(name), new PlayerData(getUserID(name), name));
            showPollsMain(name, true);
            }
    }

    private void showUpdatesMain(String name, Boolean showNew) {
        ArrayList<String> intro = new ArrayList();
        ArrayList<String> spam = new ArrayList();
        PlayerData p = playerdata.get(getUserID(name));
        String msg = "empty";
        int totalLength = -1;
        int totalSections = -1;
        
        if (p != null) {
            p.setWindow(5, 0);            

            if (showNew) {
                int oldupdates = 0;
                
                if (!p.oldUpdates.isEmpty())
                    oldupdates = p.oldUpdates.size();                
                if (oldupdates != 0)
                    intro.add("Date    #  Update");
                else
                    intro.add("Date    #  Update   [" + oldupdates + " Update(s) not shown]");
                
                for(int updateID : updates.keySet()) {
                    if (!p.oldUpdates.contains(updateID)) {
                        msg = updates.get(updateID).getUpdateString(1);
                        totalLength = msg.length();
                        totalSections = totalLength / 60;
                        
                        for(int i=0;i <= totalSections;i++) {
                            if (i == 0) {
                                spam.add( Tools.formatString(updates.get(updateID).getStartingDateString() + "  " + msg.substring(0, msg.length()),65));
                            } else {
                                spam.add(Tools.rightString(msg.substring(i * 60, totalLength),totalLength - ((i*60)+9), ' '));
                            }
                        }
                        p.addEntry(updateID, 2, "none");           
                    }             
                }                
                if (spam.isEmpty()) {
                    intro.clear();
                    showUpdatesMain(name,false);
                    return;
                }                 
                spam.add(" ");
                spam.add(" ");
                spam.add("Use !view <number> for more details on a specific update.");
                spam.add("Use !viewall to show previously read updates.");
                intro.addAll(spam);                
                m_botAction.smartPrivateMessageSpam(name, intro.toArray(new String[intro.size()]));
                
            } else {
                intro.add("Date    #  Update");                
                for(int updateID : updates.keySet()) {
                     msg = updates.get(updateID).getUpdateString(1);
                     totalLength = msg.length();
                     totalSections = totalLength / 60;
                     
                    for(int i=0;i <= totalSections;i++) {
                        if (i == 0) {
                            spam.add( Tools.formatString(updates.get(updateID).getStartingDateString() + "  " + msg.substring(0, msg.length()),65));
                        } else {
                            spam.add(Tools.rightString(msg.substring(i * 60, totalLength),totalLength - ((i*60)+9), ' '));
                        }
                    }
                }
             
                if (spam.isEmpty()) {
                    m_botAction.sendSmartPrivateMessage(name, "No updates avaliable");
                    intro.clear();
                    return;
                }
                spam.add(" ");
                spam.add(" ");
                spam.add("Use !view <number> for more details on a specific update.");
                intro.addAll(spam);                
                m_botAction.smartPrivateMessageSpam(name, intro.toArray(new String[intro.size()]));
            }
        } else {
        playerdata.put(getUserID(name), new PlayerData(getUserID(name), name));
        showUpdatesMain(name, true);
        }
    }
    
    private void showUpdate(String name, int updateID) {
        PlayerData p = playerdata.get(getUserID(name));
        p.setWindow(7, updateID);
        ArrayList<String> spam = new ArrayList();
        
                spam.add("Update #" + updateID);
                spam.add(" " +updates.get(updateID).getUpdateString(2));
                spam.add(" ");                
                
                if (p.isCommented(updateID,2)) {
                    spam.add("Your comments: " + p.getComment(updateID, 2));
                    spam.add(" ");
                    spam.add("Replace your comment on this with !com <comment>.");
                } else {
                    spam.add(" ");
                    spam.add("Leave a comment on this with !com <comment>.");
                }
                    spam.add("Reply with !back to go back to Update List.");                                
                    m_botAction.smartPrivateMessageSpam(name, spam.toArray(new String[spam.size()]));
    }

    private boolean vote(int pollID, String playerName, int optionID) {

    	int userID = getUserID(playerName);

    	PollOption pollOption = null;

    	try {
    		Poll poll = polls.get(pollID);
    		pollOption = poll.options.get(optionID-1);
    	} catch(Exception e) { return false; }

        	try {
        	    if (hasVotedAlready(pollID, userID)) {
        	        m_botAction.SQLQueryAndClose(DB_NAME, "" +
        	                "UPDATE tblPoll__PollVote " +
        	                "SET fnPollOptionID  = '" + pollOption.id + "' " +
        	                "WHERE fnUserID = '" + userID + "' "  +
        	                "AND fnPollID = '" + pollID + "'"
        	            );
        	    } else {
    			m_botAction.SQLQueryAndClose(DB_NAME, "" +
    				"INSERT INTO tblPoll__PollVote " +
    				"VALUES (null, "+pollID+","+pollOption.id+","+userID+", NOW())"
    			);
    	    }
			m_botAction.sendSmartPrivateMessage(playerName, "Your vote has been counted.");
		} catch (SQLException e) {

		}

		TreeSet<Integer> users = votes.get(pollID);
		if (users == null) {
			users = new TreeSet<Integer>();
		}
            
            Poll poll = polls.get(pollID);
            
            if (poll != null) {
                poll.addVote(userID, optionID);
            }
            
		users.add(userID);
		votes.put(pollID, users);
		openPolls.remove(userID);
		showPoll(playerName, pollID);
		return true;

    }

    private boolean hasVotedAlready(int pollID, int userID) {
        Poll poll = polls.get(pollID);
        
        if (poll != null && poll.pvotes.containsKey(userID)) 
            return true;
        else 
            return false;
    }

    private void showHelp(String playerName, int type) {        
        switch(type) {
            case 0:
            //  int polls = getPollCount(playerName);
              
                break;
            case 1:
                String[] spamPolls = {
                        formatMessage("[Polls]",0),
                        formatMessage("!polls",1) + formatMessage("Shows the active polls.",2),
                        formatMessage("!updates",1) + formatMessage("Shows active updates.",2),
                        formatMessage("!ignore",1) + formatMessage("Turns off automessages for you.",2),
                        formatMessage("!about",1) + formatMessage("Information about this bot.",2),
                        formatMessage("-",3)        
                        //"- !undo       Undo your last vote.",
                };
                m_botAction.privateMessageSpam(playerName, spamPolls);
                break;
            case 2:
                String[] spamUpdates = {
                        formatMessage("[Updates]",0),
                        formatMessage("!view <num>",1) + formatMessage("Shows the active polls.",2),
                        formatMessage("!viewall",1) + formatMessage("Shows active updates.",2),
                        formatMessage("!ignore <area>",1) + formatMessage("Turns off automessages for you.",2),
                        formatMessage("!home",1) + formatMessage("Information about this bot.",2),
                        formatMessage("-",3)        
                        //"- !undo       Undo your last vote.",
                };
                m_botAction.privateMessageSpam(playerName, spamUpdates);
                break;
            case 3:
                String[] spamView = {
                        formatMessage("[View/Edit]",0),
                        formatMessage("!edit ",1) + formatMessage("Shows the active polls.",2),
                        formatMessage("!back",1) + formatMessage("Shows active updates.",2),
                        formatMessage("!home",1) + formatMessage("Turns off automessages for you.",2),
                        formatMessage("!next",1) + formatMessage("Information about this bot.",2),
                        formatMessage("-",3)        
                        //"- !undo       Undo your last vote.",
                };
                m_botAction.privateMessageSpam(playerName, spamView);
                break;
        }      
    }
    
    private class Updates {
        public int id;
        public int userPosterId;
        public String userPoster;
        public String header;
        public String detail;
        public Date begin;
        public Date end;
        
        public Updates(int updateID, int posterID, String poster, String header, String detail, Date begin, Date end) {
            this.id = updateID;
            this.userPosterId = posterID;
            this.userPoster = poster;
            this.header = header;
            this.detail = detail;
            this.begin = begin;
            this.end = end;
        }
        
        //Types: 1-header, 2-details
        public String getUpdateString(int type) {
            if (type == 1)
                return "(" + id + ") " + header;
            else 
                return "" + detail;
        }
        
        public String getStartingDateString() {
            String date = new SimpleDateFormat("MM/dd").format(begin);
            return date;
        }
    }

    private class Poll {
        public TreeMap<Integer, PlayerVotes> pvotes;
    	public int id;
    	public int userPosterId;
    	public String userPoster;
    	public String question;
    	public boolean multi;
    	public Date begin;
    	public Date end;
    	public Date created;
    	public ArrayList<PollOption> options;
    	
    	public Poll(int pollID, int posterID, String poster, String question, Boolean multi, Date begin, Date end, Date created) {
    	    this.id = pollID;
    	    this.userPosterId = posterID;
    	    this.userPoster = poster;
    	    this.question = question;
    	    this.multi = multi;
    	    this.begin = begin;
    	    this.end = end;
    	    this.created = created;
    	    pvotes = new TreeMap<Integer, PlayerVotes>();
    	    options = new ArrayList<PollOption>();
    	}
    	
        public void addVote(int userID, int optionID) {
                pvotes.put(userID, new PlayerVotes(userID,optionID));
        }
        
        public void addOption(int pollOptionID, String Option) {
            options.add(new PollOption(pollOptionID,Option));            
        }
    	
    	 public class PlayerVotes {
    	        int userID;
                int optionID;
    	        
    	        public PlayerVotes(int userID, int optionID) {
    	            this.userID = userID;
    	            this.optionID = optionID;
    	        }
    	        
    	        public int getUserID() {
    	            return userID;
    	        }
    	        
    	        public int getOptionID() {
                    return this.optionID;
                }  	        
    	    }    	
    }

    private class PollOption {
    	public int id;
    	public String option;
    	public PollOption(int id, String option) {
    		this.id = id;
    		this.option = option;
    	}
    }

    private class PlayerData {
        public TreeMap<Integer, String> pollComments;
        public TreeMap<Integer, String> updateComments;
        public TreeSet<Integer> oldPolls;
        public TreeSet<Integer> oldUpdates;
        private int userID;
        private int[] currentWindow = {0,0};
        private String userName;
        private TimerTask updateMessage;
        
        public PlayerData(int userID, String name) {
             updateComments = new TreeMap <Integer, String>();
             pollComments = new TreeMap <Integer, String>();
             oldPolls = new TreeSet<Integer>();
             oldUpdates = new TreeSet<Integer>();
             this.userID = userID;
             this.userName = name;
            setPlayerEntryData();
            sendMessage();
           // setComments();
            
        }
        
        public void sendMessage() {
            if (updateMessage == null) {
                updateMessage = new TimerTask() {   
                @Override
                public void run() {
                    try {
                        int newPolls = getCounts(userName, true, 1);
                        int newUpdates = getCounts(userName, true, 2);
                        String message1 = "Hello, ";
                        String message2 = "Type ";      
                         
                        
                        if (newPolls != 0 && newUpdates != 0) {
                            m_botAction.sendSmartPrivateMessage(userName,  message1 + newPolls + " Poll(s) and " + newUpdates + " Zone update(s) has been added since you last checked.");
                            m_botAction.sendSmartPrivateMessage(userName, message2 + "To view them, use !polls for polls and !updates for updates.");
                        } else if (newPolls == 0 && newUpdates != 0) {
                            m_botAction.sendSmartPrivateMessage(userName,  message1 +  newUpdates + " Zone Update(s) has been added since you last checked.");
                            m_botAction.sendSmartPrivateMessage(userName, message2 + "!updates to view the latest zone updates.");
                        } else if (newPolls  != 0 && newUpdates == 0) {
                            m_botAction.sendSmartPrivateMessage(userName, message1 + newPolls + " Poll(s) has been added since you last checked.");
                            m_botAction.sendSmartPrivateMessage(userName, message2 + "!polls to view the latest poll questions.");
                        }               
                    } catch (NullPointerException e) {Tools.printStackTrace(e);}
                } 
            }; m_botAction.scheduleTask(updateMessage, Tools.TimeInMillis.MINUTE);
        }
    }
        
        private void setPlayerEntryData() {
            String pollList = "";
            for(int pollId: polls.keySet()) 
                pollList += "," + pollId;
            
            for(int updateID: updates.keySet())
                pollList += "," + updateID;            

            if (!pollList.isEmpty())
            {
                pollList = pollList.substring(1);
                try {
                    // First, filter with UserAccount to avoid double (it happens)
                    ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
                        "SELECT * " +
                        "FROM tblPoll__Entry  " +
                        "WHERE fnUserID = " + userID + 
                        " AND fnEntryID IN (" + pollList + ")"
                    );
                    
                    while (rs.next()) {
                        int type = rs.getInt("fnType");
                        int entryID = rs.getInt("fnEntryID");
                        String comment = rs.getString("fcComment");
                        
                        if (type == 1) {
                            if (!comment.equals("none"))
                                    pollComments.put(entryID, comment);
                            oldPolls.add(entryID);
                        } else {
                            if (!comment.equals("none"))
                                updateComments.put(entryID, comment);
                            oldUpdates.add(entryID);
                        }

                    }
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        
        public void setComments(int ID, String comment, int type) {
            try {
                m_botAction.SQLQueryAndClose(DB_NAME, "UPDATE tblPoll__Entry SET fcComment = '"+Tools.addSlashesToString(comment)+"' WHERE fnUserID  = '" + userID +"' AND fnType = '" + type +"' AND fnEntryID = '" + ID + "'");
                if (type == 1)
                    pollComments.put(ID,  comment);
                else 
                    updateComments.put(ID,  comment);
} catch(Exception e) { Tools.printStackTrace( e );}   
        }
        
        public Boolean isCommented(int ID, int type) {
            if (type == 1 && pollComments.containsKey(ID))
                return true; 
            if (type == 2 && updateComments.containsKey(ID))
                return true;
            
            return false;
        }
        
        public String getComment(int ID, int type) {
            String comment = " ";
            try {
                // First, filter with UserAccount to avoid double (it happens)
                ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
                    "SELECT * " +
                    "FROM tblPoll__Entry  " +
                    "WHERE fnUserID = " + userID + 
                    " AND fnType = " + type + 
                    " AND fnEntryID = " + ID 
                );
                
                while (rs.next()) {                    
                    comment = rs.getString("fcComment");                    
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return comment;
        }
        
        public void addEntry(int ID, int type, String comment) {
            try {
                m_botAction.SQLQueryAndClose(DB_NAME, "" +
                    "INSERT INTO tblPoll__Entry " +
                    "VALUES (null, "+type+","+ID+","+this.userID+",'"+Tools.addSlashesToString(comment)+ "',1 , NOW())"
                );
                if(type == 1) {
                    if (!oldPolls.contains(ID))
                        oldPolls.add(ID);
                }
                if(type == 2) {
                    if (!oldUpdates.contains(ID))
                        oldUpdates.add(ID);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }            
        }
        
        /** 
         * @param window 0 - other
         * 1-polls, 2-allpolls, 3-singlepoll, 4-pollcomment
         * 5-updates, 6-allupdates, 7-singleupdate, 8-updatecomment
         * 
         * @param id 0 -other
         * anything above 0 will correspond with the window         * 
         */
        public void setWindow(int window, int id) {
            currentWindow[0] = window;
            currentWindow[1] = id;
        }
        
        public int[] getWindow() {
            return currentWindow;
        }
    }
}