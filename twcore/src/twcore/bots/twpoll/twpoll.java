package twcore.bots.twpoll;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.Vector;

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

	private static final int SPAM_INTERVAL_MINUTE = 30;

	private static final String DB_NAME = "website";

	private HashMap<Integer,Poll> polls;
	private HashMap<Integer,HashSet<Integer>> votes;
	private HashMap<Integer, PlayerVotes> pVotes;
	private HashMap<String,Integer> userIds;
	private HashMap<Integer,Integer> openPolls;
	private HashMap<Integer,Integer> lastPolls;
	private HashMap<Integer,Long> lastSpam;
	private HashSet<String> ignore;

	private Vector<String> players;

    private BotSettings m_botSettings;

    private SpamTask spamTask;

    public twpoll(BotAction botAction) {
        super(botAction);
        requestEvents();
        m_botSettings = m_botAction.getBotSettings();
        polls = new HashMap<Integer,Poll>();
        votes = new HashMap<Integer,HashSet<Integer>>();
        pVotes = new HashMap<Integer, PlayerVotes>();
        userIds = new HashMap<String,Integer>();
        openPolls = new HashMap<Integer,Integer>();
        lastPolls = new HashMap<Integer,Integer>();
        lastSpam = new HashMap<Integer,Long>();
        players = new Vector<String>();
        ignore = new HashSet<String>();
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

    public void handleEvent(PlayerEntered event) {
		if (!players.contains(event.getPlayerName()))
			players.add(event.getPlayerName());
    }

    public void handleEvent(Message event) {

    	String name = event.getMessager() != null ? event.getMessager() : m_botAction.getPlayerName(event.getPlayerID());
        if (name == null) name = "-anonymous-";
        if (event.getMessageType() == Message.PRIVATE_MESSAGE && event.getMessage().equalsIgnoreCase("!die")) {
            try { Thread.sleep(50); } catch (Exception e) {};
            m_botAction.die();
        }

        String message = event.getMessage();

        if (event.getMessageType() == Message.PRIVATE_MESSAGE || event.getMessageType() == Message.REMOTE_PRIVATE_MESSAGE) {

	        if (message.startsWith("!poll ")) {
	        	try {
	        		int pollId = Integer.parseInt(message.substring(6).trim());
	        		showPoll(name, pollId);
	        	} catch(NumberFormatException e) { }
	        }
	        else if (message.startsWith("!ignore")) {
	        	if (ignore.contains(name)) {
	        		m_botAction.sendSmartPrivateMessage(name, "You are already on my ignore list.");
	        	} else {
		        	ignore.add(name);
		        	m_botAction.sendSmartPrivateMessage(name, "You have been added to my ignore list. I won't bother you until I reset.");
		        	m_botAction.sendSmartPrivateMessage(name, "HINT: Vote for each polls and you won't get notified again.");
	        	}
	        }
	        else if (message.startsWith("!polls")) {
	        	showPolls(name);
	        }
	        else if (message.startsWith("!next")) {
	        	showNextPoll(name);
	        }
	        else if (message.startsWith("!help")) {
	        	showHelp(name);
	        }
	        //else if (message.startsWith("!undo")) {
	        //	undo(name);
	        //}
	        else if (message.startsWith("!spam") && m_botAction.getOperatorList().isER(name)) {
	        	spamTask.run();
	        }
	        else if (message.startsWith("!ignored") && m_botAction.getOperatorList().isER(name)) {
	        	String names = "";
	        	for(String n: ignore) {
	        		names += ", " + n;
	        	}
	        	if (ignore.size() > 0) {
	        		m_botAction.sendSmartPrivateMessage(name, "Total: " + ignore.size());
	        		m_botAction.sendSmartPrivateMessage(name, names.substring(2));
	        	} else {
	        		m_botAction.sendSmartPrivateMessage(name, "Ignore list empty.");
		        }
	        }
	        else if (message.startsWith("!queue") && m_botAction.getOperatorList().isER(name)) {
	        	m_botAction.sendSmartPrivateMessage(name, "Queue: " + players.size());
	        }
	        else if (message.startsWith("!reload") && m_botAction.getOperatorList().isER(name)) {
	        	loadPolls();
	        	loadVotes();
	        	m_botAction.sendSmartPrivateMessage(name, "Polls and votes reloaded.");
	        }
	        else {
	        	Integer userId = getUserID(name);
	        	 Integer choice = -1;
	        	 String comment = "none";
	        	try {
	        	    String[] splitCmd;	        	    
	        	            if (message.trim().contains(":")) {
	        	                    splitCmd = message.split(":");
    	        	                    if(splitCmd[0].length() >= 1)
    	        	                         choice = Integer.parseInt(message.trim());
    	        	                    if(splitCmd[1].length() >= 1)
    	        	                         comment = splitCmd[1];
	        	            } else
	        	                choice = Integer.parseInt(message.trim());
	        		if (userId != null) {
	        			if (openPolls.get(userId) != null) {
	        				vote(openPolls.get(userId), name, choice, comment);
	        				return;
	        			} else {
	        				showHelp(name);
	        			}
	        		}
	        	} catch(NumberFormatException e) {
	        		if (openPolls.get(userId) != null) {
	        			showPoll(name, openPolls.get(userId));
	        		} else {
	        			showPolls(name);
	        		}
	        	}
	        }

        }

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
    	if (spamTask != null) {
    		m_botAction.cancelTask(spamTask);
    	}
    	spamTask = new SpamTask();
    	m_botAction.scheduleTaskAtFixedRate(spamTask, SPAM_INTERVAL_MINUTE * Tools.TimeInMillis.MINUTE, SPAM_INTERVAL_MINUTE * Tools.TimeInMillis.MINUTE );
    }

    public void handleEvent(LoggedOn event) {
        m_botAction.joinArena(m_botSettings.getString("InitialArena"));
        m_botAction.requestArenaList();
        loadPolls();
        loadVotes();
    }

    private void giveMoney(String playerName, int money) {
    	m_botAction.sendSmartPrivateMessage("TW-Pub1", "!addmoney " + playerName + ":" + money);
    }

    private int getUserID(String playerName) {

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

    private int getPollCount(String playerName) {
    	int count = 0;
    	int userId = getUserID(playerName);
    	for(int pollId: polls.keySet()) {
    		Poll poll = polls.get(pollId);
    		if (!votes.containsKey(pollId) ||  (votes.containsKey(pollId) && !votes.get(pollId).contains(userId))) {
    			count++;
    		}
    	}
    	return count;
    }

    private void showHelp(String playerName) {
    	int polls = getPollCount(playerName);
    	String[] spam = {
    			"[Polls]",
    			"- !polls      Show current polls.",
    			"- !next       Get the next poll. (avalaible: " + polls + ")",
    			"- !ignore     Put you on my ignore list, I won't bother you again."
    			//"- !undo       Undo your last vote.",
    	};
    	m_botAction.privateMessageSpam(playerName, spam);
    }

    private void showPoll(String playerName, int pollId) {

       	if (polls.isEmpty()) {
    		m_botAction.sendSmartPrivateMessage(playerName, "There is no poll at the moment.");
    	}
    	else {

    		ArrayList<String> spam = new ArrayList();

        	int userId = getUserID(playerName);
        	Poll poll = polls.get(pollId);
        	if (poll == null) {
        		spam.add("Poll not found.");
        	} else if (votes.containsKey(pollId) && votes.get(pollId).contains(userId)) {
        		spam.add("You have already voted.");
        	} else {
    			openPolls.put(userId, poll.id);
    			spam.add("(#." + poll.id + ") " + poll.question);
    			int i=0;
    			for(PollOption option: poll.options) {
    				String pad = Tools.rightString("", ("(#" + poll.id + ") ").length(), ' ');
    				spam.add(pad + (++i) + ". " + option.option);
    			}
    			spam.add(" ");
    			spam.add("HELP: To vote, pm me your choice. You can follow up your choice with a comment by typing <choice>:<comment>");
        	}

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

    private void showPolls(String playerName) {

    	if (polls.isEmpty()) {
    		m_botAction.sendSmartPrivateMessage(playerName, "There is no poll at the moment.");
    	}
    	else {

    		ArrayList<String> intro = new ArrayList();
    		ArrayList<String> spam = new ArrayList();

    		intro.add("[Polls]");

        	int userId = getUserID(playerName);
        	boolean pollExist = false;
        	for(int pollId: polls.keySet()) {
        		Poll poll = polls.get(pollId);
        		if (!votes.containsKey(pollId) ||  (votes.containsKey(pollId) && !votes.get(pollId).contains(userId))) {
        			spam.add("(#" + poll.id + ") " + poll.question);
        			int i=0;
        			for(PollOption option: poll.options) {
        				String pad = Tools.rightString("", ("(#" + poll.id + ") ").length(), ' ');
        				spam.add(pad + (++i) + ". " + option.option);
        			}
        			spam.add(" ");
        			pollExist = true;
        		}
        	}
        	if (pollExist) {
        		intro.add("Earn up to $1000 by voting.");
        		intro.add(" ");
        		spam.add("To SELECT a poll, pm !poll <number>.");
        		spam.add("To VOTE, select a poll and pm your choice. You can follow up your choice with a comment by typing <choice>:<comment>");
        	} else {
        		intro.clear();
        		spam.add("There is no poll for you at the moment.");
        	}

        	intro.addAll(spam);

        	m_botAction.smartPrivateMessageSpam(playerName, intro.toArray(new String[intro.size()]));
    	}

    }



    private boolean undo(String playerName) {

    	int userId = getUserID(playerName);
    	if (userId == 0) {
    		return false;
    	}

    	Integer pollId = lastPolls.get(userId);
    	if (pollId == null) {
    		m_botAction.sendSmartPrivateMessage(playerName, "There is nothing to undo.");
    		return false;
    	}

    	try {
			m_botAction.SQLQueryAndClose(DB_NAME, "DELETE FROM tblPollVote WHERE fnPollID = " + pollId + " AND fnUserID = " + userId);
	    	votes.get(pollId).remove(userId);
	    	lastPolls.remove(userId);
	    	m_botAction.sendSmartPrivateMessage(playerName, "Your last vote has been removed.");
	    	return true;
    	} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

    }

    private boolean vote(int pollId, String playerName, int optionId, String comment) {

    	int userId = getUserID(playerName);
    	if (userId == 0 || hasVotedAlready(pollId, userId)) {
    		return false;
    	}

    	PollOption pollOption = null;

    	try {
    		Poll poll = polls.get(pollId);
    		pollOption = poll.options.get(optionId-1);
    	} catch(Exception e) { return false; }

    	try {
			m_botAction.SQLQueryAndClose(DB_NAME, "" +
				"INSERT INTO tblPollVote " +
				"VALUES (null, "+pollId+","+pollOption.id+","+userId+","+Tools.addSlashes(comment)+ ", NOW())"
			);

			// If no duplicate found, we can proceed, else SQLException
			lastPolls.put(userId, pollId);
			//m_botAction.sendSmartPrivateMessage(playerName, "Your vote has been counted (!undo to undo).");
			m_botAction.sendSmartPrivateMessage(playerName, "Your vote has been counted.");
			giveMoney(playerName, ((int)(Math.random()*1000)));
			m_botAction.sendSmartPrivateMessage(playerName, "Type !next to answer another poll.");

		} catch (SQLException e) {

		}

		HashSet<Integer> users = votes.get(pollId);
		if (users == null) {
			users = new HashSet<Integer>();
		}

	    if(!pVotes.containsKey(userId)) {
            pVotes.put(userId, new PlayerVotes(userId));
            }
            pVotes.get(userId).addVote(pollId, pollOption.id, comment);
            
		users.add(userId);
		votes.put(pollId, users);
		openPolls.remove(userId);
		return true;

    }

    private boolean hasVotedAlready(int pollId, int userId) {

		if (votes.get(pollId) != null && votes.get(pollId).contains(userId)) {
			return true;
		} else {
			return false;
		}
    }

    private void loadPolls() {

    	polls = new HashMap<Integer,Poll>();

    	try {
			ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
				"SELECT fnPollID, fcQuestion, fbMultiSelect, fdBegin, fdEnd, fdCreated, fnUserPosterID, fcUserName, fnPollOptionID, fcOption, fnOrder " +
				"FROM tblPoll p " +
				"JOIN tblPollOptions po USING (fnPollID) " +
				"JOIN tblUser u ON p.fnUserPosterID = u.fnUserID " +
				"WHERE NOW() BETWEEN fdBegin AND fdEnd " +
				"ORDER BY fnPollID,fnOrder"
			);

			Poll currentPoll = null;
			while(rs.next()) {
				if (currentPoll==null || currentPoll.id != rs.getInt("fnPollID")) {
					currentPoll = new Poll();
					currentPoll.id = rs.getInt("fnPollID");
					currentPoll.userPoster = rs.getString("fcUserName");
					currentPoll.userPosterId = rs.getInt("fnUserPosterID");
					currentPoll.question = rs.getString("fcQuestion");
					currentPoll.multi = rs.getBoolean("fbMultiSelect");
					currentPoll.begin = rs.getDate("fdBegin");
					currentPoll.end = rs.getDate("fdEnd");
					currentPoll.created = rs.getDate("fdCreated");
					currentPoll.options = new ArrayList<PollOption>();
					currentPoll.options.add(new PollOption(rs.getInt("fnPollOptionID"),rs.getString("fcOption")));
					polls.put(currentPoll.id, currentPoll);
				} else {
					currentPoll.options.add(new PollOption(rs.getInt("fnPollOptionID"),rs.getString("fcOption")));
					polls.put(currentPoll.id, currentPoll);
				}
			}
			rs.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

    }

    private void loadVotes() {

        votes = new HashMap<Integer,HashSet<Integer>>();

    	String pollList = "";
    	for(int pollId: polls.keySet()) {
    		pollList += "," + pollId;
    	}

    	if (!pollList.isEmpty())
    	{
	    	pollList = pollList.substring(1);
	    	try {
				ResultSet rs = m_botAction.SQLQuery(DB_NAME, "" +
					"SELECT fnPollID, fnUserID,fnPollOptionID, fcComment " +
					"FROM tblPollVote " +
					"WHERE fnPollID IN (" + pollList + ")"
				);

				while(rs.next()) {
					HashSet<Integer> users = votes.get(rs.getInt("fnPollID"));
			        int userID = rs.getInt("fnUserID");
			        int pollID =  rs.getInt("fnPollID");
			        int optionID = rs.getInt("fnPollOptionID");
			        String comments = rs.getString("fcComment");
					if (users == null) {
						users = new HashSet<Integer>();
					}
					if(!pVotes.containsKey(userID)) {
					pVotes.put(userID, new PlayerVotes(userID));
					}
					pVotes.get(userID).addVote(pollID, optionID, comments);
					
					users.add(userID);
					votes.put(pollID, users);
				}

				rs.close();

			} catch (SQLException e) {
				e.printStackTrace();
			}
	    }
    }

    private class SpamTask extends TimerTask {
    	public void run() {
        	Runnable r = new Runnable() {
				public void run() {
					Tools.printLog("TW-Poll: running spamTask with " + players.size() + " players.");
					Iterator<String> it = ((Vector)players.clone()).iterator();
					while(it.hasNext()) {
						String p = it.next();
		        		if (m_botAction.getOperatorList().isBotExact(p))
		        			continue;
		        		if (ignore.contains(p))
		        			continue;
		        		if (p.startsWith("TW-") || p.startsWith("TWCore") || p.startsWith("RoboBot"))
		        			continue;
		            	int userId = getUserID(p);
		            	if (userId == 0)
		            		continue;
		            	boolean next = false;
		            	for(int pollId: polls.keySet()) {
		            		if (next)
		            			continue;
		            		if (!votes.containsKey(pollId) ||  (votes.containsKey(pollId) && !votes.get(pollId).contains(userId))) {
		            			m_botAction.sendSmartPrivateMessage(p, "There is at least 1 poll you have not voted yet.");
		            			m_botAction.sendSmartPrivateMessage(p, " ");
		            			showPoll(p, pollId);
		            			m_botAction.sendSmartPrivateMessage(p, "      To stop getting notified, type !ignore.");
		            			try { Thread.sleep(3000); } catch (InterruptedException e) { }
		            			next = true;
		            			lastSpam.put(userId, System.currentTimeMillis());
		            		}
		            	}
		            	if (!next) {
		            		players.remove(p);
		            	}
		        	}
					if (players.size() > 140) {
						players.clear();
					}
				}
			};
			Thread t = new Thread(r);
			t.start();
        }
    }
    
    public class PlayerVotes {
        int userID;
        private HashMap<Integer,Votes> playerVotes;

        
        public PlayerVotes(int userID) {
            this.userID = userID;
            playerVotes = new HashMap<Integer, Votes>();
        }
        
        public int getUserID() {
            return userID;
        }
        
        public void addVote(int pollID, int optionID, String comment) {
            playerVotes.put(pollID, new Votes( pollID,optionID, comment));
        }
        
        private class Votes {
            int pollID;
            int optionID;
            String comment;
            
            public Votes(int pollID, int optionID, String comment) {
                this.pollID = pollID;
                this.optionID = optionID;
                this.comment = comment;
            }
            
            public int getPollID() {
                return this.pollID;
            }
        
            public int getOptionID() {
                return this.optionID;
            }
            
            public String getComment() { 
                return this.comment;
            }
        }
    }
    

    private class Poll {
    	public int id;
    	public int userPosterId;
    	public String userPoster;
    	public String question;
    	public boolean multi;
    	public Date begin;
    	public Date end;
    	public Date created;
    	public ArrayList<PollOption> options;
    }

    private class PollOption {
    	public int id;
    	public String option;
    	public PollOption(int id, String option) {
    		this.id = id;
    		this.option = option;
    	}
    }

}